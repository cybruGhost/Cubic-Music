package app.cubic.android.lastfm

import android.content.Context
import androidx.core.content.edit
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.utils.InnertubeSearchClient
import app.it.fast4x.rimusic.utils.preferences
import app.kreate.android.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class LastFmResolvedArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscribersText: String?,
)

/**
 * Local, best-effort bridge between Last.fm taste signals and Cubic Music recommendations.
 * Playback never depends on this data, and a failed refresh keeps the last on-device snapshot.
 */
object LastFmTasteRepository {
    private const val songsKey = "lastFmTasteSongsJson"
    private const val usernameKey = "lastFmTasteSongsUsername"
    private const val refreshedAtKey = "lastFmTasteSongsRefreshedAt"
    private const val artistsKey = "lastFmTasteArtistsJson"
    private const val artistsUsernameKey = "lastFmTasteArtistsUsername"
    private const val artistsRefreshedAtKey = "lastFmTasteArtistsRefreshedAt"
    private const val cacheTtlMs = 12L * 60L * 60L * 1_000L
    private val refreshMutex = Mutex()
    private val artistRefreshMutex = Mutex()

    fun cached(context: Context, username: String): List<Song> {
        val preferences = context.preferences
        if (!preferences.getString(usernameKey, "").equals(username.trim(), ignoreCase = true)) {
            return emptyList()
        }
        val raw = preferences.getString(songsKey, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        Song(
                            id = id,
                            title = title,
                            artistsText = item.optString("artist").trim().ifBlank { null },
                            durationText = item.optString("duration").trim().ifBlank { null },
                            thumbnailUrl = item.optString("thumbnail").trim().ifBlank { null },
                            totalPlayTimeMs = 1L,
                        ),
                    )
                }
            }
        }.getOrElse {
            Timber.tag("CubicLastFm").w(it, "Ignoring invalid local taste cache")
            emptyList()
        }
    }

    suspend fun load(
        context: Context,
        username: String,
        forceRefresh: Boolean,
    ): List<Song> = refreshMutex.withLock {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank()) return@withLock emptyList()
        val cachedSongs = cached(context, normalizedUsername)
        val refreshedAt = context.preferences.getLong(refreshedAtKey, 0L)
        val isFresh = System.currentTimeMillis() - refreshedAt in 0 until cacheTtlMs
        if (!forceRefresh && isFresh && cachedSongs.isNotEmpty()) return@withLock cachedSongs

        CubicLastFmClient.initialize(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_API_SECRET)
        if (!CubicLastFmClient.isConfigured()) return@withLock cachedSongs

        val signals = CubicLastFmClient.getTasteRecommendations(normalizedUsername)
            .onFailure { Timber.tag("CubicLastFm").w(it, "Taste refresh failed") }
            .getOrNull()
            .orEmpty()
        if (signals.isEmpty()) return@withLock cachedSongs

        val resolvedSongs = coroutineScope {
            val resolved = mutableListOf<Song?>()
            for (group in signals.take(10).chunked(3)) {
                resolved += group.map { signal ->
                    async {
                        val matches = InnertubeSearchClient
                            .search("${signal.artist} ${signal.title}", type = "song")
                            .getOrNull()
                            .orEmpty()
                        val artistKey = signal.artist.normalizedMatchText()
                        val titleKey = signal.title.normalizedMatchText()
                        val match = matches.firstOrNull { result ->
                            val resultTitle = result.title.normalizedMatchText()
                            val resultArtist = result.author.normalizedMatchText()
                            (resultTitle == titleKey || resultTitle.contains(titleKey) || titleKey.contains(resultTitle)) &&
                                (resultArtist.contains(artistKey) || artistKey.contains(resultArtist))
                        } ?: matches.firstOrNull()
                        match?.let { result ->
                            Song(
                                id = result.id,
                                title = result.title,
                                artistsText = result.author.ifBlank { signal.artist },
                                durationText = result.durationText,
                                thumbnailUrl = result.thumbnailUrl,
                                totalPlayTimeMs = 1L,
                            )
                        }
                    }
                }.awaitAll()
            }
            resolved
        }
            .filterNotNull()
            .distinctBy(Song::id)
            .take(10)

        if (resolvedSongs.isEmpty()) return@withLock cachedSongs
        store(context, normalizedUsername, resolvedSongs)
        resolvedSongs
    }

    fun cachedArtists(context: Context, username: String): List<LastFmResolvedArtist> {
        val preferences = context.preferences
        if (!preferences.getString(artistsUsernameKey, "").equals(username.trim(), ignoreCase = true)) {
            return emptyList()
        }
        val raw = preferences.getString(artistsKey, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        LastFmResolvedArtist(
                            id = id,
                            name = name,
                            thumbnailUrl = item.optString("thumbnail").trim().ifBlank { null },
                            subscribersText = item.optString("subscribers").trim().ifBlank { null },
                        ),
                    )
                }
            }
        }.getOrElse {
            Timber.tag("CubicLastFm").w(it, "Ignoring invalid local artist taste cache")
            emptyList()
        }
    }

    suspend fun loadArtists(
        context: Context,
        username: String,
        forceRefresh: Boolean,
    ): List<LastFmResolvedArtist> = artistRefreshMutex.withLock {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank()) return@withLock emptyList()
        val cachedArtists = cachedArtists(context, normalizedUsername)
        val refreshedAt = context.preferences.getLong(artistsRefreshedAtKey, 0L)
        val isFresh = System.currentTimeMillis() - refreshedAt in 0 until cacheTtlMs
        if (!forceRefresh && isFresh && cachedArtists.isNotEmpty()) return@withLock cachedArtists

        CubicLastFmClient.initialize(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_API_SECRET)
        if (!CubicLastFmClient.isConfigured()) return@withLock cachedArtists

        val signals = CubicLastFmClient.getArtistRecommendations(normalizedUsername)
            .onFailure { Timber.tag("CubicLastFm").w(it, "Artist taste refresh failed") }
            .getOrNull()
            .orEmpty()
        if (signals.isEmpty()) return@withLock cachedArtists

        val resolvedArtists = coroutineScope {
            val resolved = mutableListOf<LastFmResolvedArtist?>()
            for (group in signals.take(10).chunked(3)) {
                resolved += group.map { signal ->
                    async {
                        val matches = withTimeoutOrNull(6_000L) {
                            InnertubeSearchClient
                                .search(signal.name, type = "artist")
                                .getOrNull()
                                .orEmpty()
                        }.orEmpty()
                        val nameKey = signal.name.normalizedMatchText()
                        val match = matches.firstOrNull { result ->
                            val resultName = result.title.normalizedMatchText()
                            resultName == nameKey || resultName.contains(nameKey) || nameKey.contains(resultName)
                        } ?: matches.firstOrNull()
                        match?.let { result ->
                            LastFmResolvedArtist(
                                id = result.id,
                                name = result.title.ifBlank { signal.name },
                                thumbnailUrl = result.thumbnailUrl,
                                subscribersText = result.viewCountText,
                            )
                        }
                    }
                }.awaitAll()
            }
            resolved
        }
            .filterNotNull()
            .distinctBy(LastFmResolvedArtist::id)
            .take(10)

        if (resolvedArtists.isEmpty()) return@withLock cachedArtists
        storeArtists(context, normalizedUsername, resolvedArtists)
        resolvedArtists
    }

    private fun store(context: Context, username: String, songs: List<Song>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artistsText.orEmpty())
                    .put("duration", song.durationText.orEmpty())
                    .put("thumbnail", song.thumbnailUrl.orEmpty()),
            )
        }
        context.preferences.edit {
            putString(songsKey, array.toString())
            putString(usernameKey, username)
            putLong(refreshedAtKey, System.currentTimeMillis())
        }
    }

    private fun storeArtists(
        context: Context,
        username: String,
        artists: List<LastFmResolvedArtist>,
    ) {
        val array = JSONArray()
        artists.forEach { artist ->
            array.put(
                JSONObject()
                    .put("id", artist.id)
                    .put("name", artist.name)
                    .put("thumbnail", artist.thumbnailUrl.orEmpty())
                    .put("subscribers", artist.subscribersText.orEmpty()),
            )
        }
        context.preferences.edit {
            putString(artistsKey, array.toString())
            putString(artistsUsernameKey, username)
            putLong(artistsRefreshedAtKey, System.currentTimeMillis())
        }
    }

    private fun String.normalizedMatchText(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

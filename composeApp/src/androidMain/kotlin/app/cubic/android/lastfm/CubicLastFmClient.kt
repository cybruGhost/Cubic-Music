package app.cubic.android.lastfm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class LastFmSession(
    val key: String,
    val username: String,
)

data class LastFmProfile(
    val username: String,
    val avatarUrl: String?,
    val playCount: Long,
)

data class LastFmTasteTrack(
    val artist: String,
    val title: String,
    val affinity: Double,
)

data class LastFmTasteArtist(
    val name: String,
    val affinity: Double,
)

class LastFmApiException(
    val errorCode: Int,
    message: String,
) : IOException(message)

/**
 * Small, playback-independent Last.fm client for Cubic Music.
 *
 * The API key and shared secret are injected through BuildConfig. Mobile authentication exchanges
 * the account credentials for the persistent on-device session used by the rest of the integration.
 */
object CubicLastFmClient {
    private const val endpoint = "https://ws.audioscrobbler.com/2.0/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var apiKey = ""

    @Volatile
    private var apiSecret = ""

    @Volatile
    var sessionKey: String? = null

    fun initialize(key: String, secret: String) {
        apiKey = key.trim()
        apiSecret = secret.trim()
    }

    fun isConfigured(): Boolean = apiKey.isNotBlank() && apiSecret.isNotBlank()

    suspend fun login(username: String, password: String): Result<LastFmSession> = apiCall {
        requireConfigured()
        val response = postSigned(
            mapOf(
                "method" to "auth.getMobileSession",
                "username" to username.trim(),
                "password" to password,
                "api_key" to apiKey,
            ),
        )
        val session = response.optJSONObject("session")
            ?: throw IOException("Last.fm returned an empty session")
        LastFmSession(
            key = session.optString("key").ifBlank {
                throw IOException("Last.fm returned an empty session key")
            },
            username = session.optString("name").ifBlank { username.trim() },
        )
    }

    suspend fun getProfile(username: String): Result<LastFmProfile> = apiCall {
        requireConfigured()
        val response = post(
            mapOf(
                "method" to "user.getInfo",
                "user" to username.trim(),
                "api_key" to apiKey,
                "format" to "json",
            ),
        )
        val user = response.optJSONObject("user")
            ?: throw IOException("Last.fm profile was unavailable")
        val images = user.optJSONArray("image")
        var avatarUrl: String? = null
        if (images != null) {
            for (index in 0 until images.length()) {
                images.optJSONObject(index)
                    ?.optString("#text")
                    ?.takeIf(String::isNotBlank)
                    ?.let { avatarUrl = it }
            }
        }
        LastFmProfile(
            username = user.optString("name").ifBlank { username.trim() },
            avatarUrl = avatarUrl,
            playCount = user.optLong("playcount", 0L),
        )
    }

    /**
     * Builds discovery signals from the listener's own Last.fm history. The returned tracks are
     * metadata only; Cubic Music resolves them through its normal catalog before showing them.
     */
    suspend fun getTasteRecommendations(username: String): Result<List<LastFmTasteTrack>> = apiCall {
        requireConfigured()
        val topTracksResponse = post(
            mapOf(
                "method" to "user.getTopTracks",
                "user" to username.trim(),
                "period" to "3month",
                "limit" to "8",
                "api_key" to apiKey,
                "format" to "json",
            ),
        )
        val topTracks = topTracksResponse
            .optJSONObject("toptracks")
            ?.optJSONArray("track")
            .toTasteTracks(defaultAffinity = 1.0)

        val discoveries = topTracks.take(3).flatMap { seed ->
            runCatching {
                post(
                    mapOf(
                        "method" to "track.getSimilar",
                        "artist" to seed.artist,
                        "track" to seed.title,
                        "autocorrect" to "1",
                        "limit" to "6",
                        "api_key" to apiKey,
                        "format" to "json",
                    ),
                )
                    .optJSONObject("similartracks")
                    ?.optJSONArray("track")
                    .toTasteTracks(defaultAffinity = 0.5)
            }.getOrDefault(emptyList())
        }

        (discoveries + topTracks)
            .filter { it.artist.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { "${it.artist.lowercase()}|${it.title.lowercase()}" }
            .sortedByDescending(LastFmTasteTrack::affinity)
            .take(18)
    }

    /**
     * Supplies artist-level taste signals. Cubic Music deliberately resolves these names through
     * InnerTube before displaying them so every card has a working browse id and catalog artwork.
     */
    suspend fun getArtistRecommendations(username: String): Result<List<LastFmTasteArtist>> = apiCall {
        requireConfigured()
        val topArtistsResponse = post(
            mapOf(
                "method" to "user.getTopArtists",
                "user" to username.trim(),
                "period" to "3month",
                "limit" to "8",
                "api_key" to apiKey,
                "format" to "json",
            ),
        )
        val topArtists = topArtistsResponse
            .optJSONObject("topartists")
            ?.optJSONArray("artist")
            .toTasteArtists(defaultAffinity = 1.0)

        val discoveries = topArtists.take(2).flatMap { seed ->
            runCatching {
                post(
                    mapOf(
                        "method" to "artist.getSimilar",
                        "artist" to seed.name,
                        "autocorrect" to "1",
                        "limit" to "8",
                        "api_key" to apiKey,
                        "format" to "json",
                    ),
                )
                    .optJSONObject("similarartists")
                    ?.optJSONArray("artist")
                    .toTasteArtists(defaultAffinity = 0.55)
            }.getOrDefault(emptyList())
        }

        (discoveries + topArtists)
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.lowercase() }
            .sortedByDescending(LastFmTasteArtist::affinity)
            .take(16)
    }

    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        album: String?,
        durationSeconds: Long,
    ): Result<Unit> = apiCall {
        val session = requireSession()
        val params = mutableMapOf(
            "method" to "track.updateNowPlaying",
            "artist" to artist,
            "track" to track,
            "api_key" to apiKey,
            "sk" to session,
        )
        album?.takeIf(String::isNotBlank)?.let { params["album"] = it }
        if (durationSeconds > 0L) params["duration"] = durationSeconds.toString()
        postSigned(params)
        Unit
    }

    suspend fun scrobble(
        artist: String,
        track: String,
        album: String?,
        startedAtEpochSeconds: Long,
        durationSeconds: Long,
    ): Result<Unit> = apiCall {
        val session = requireSession()
        val params = mutableMapOf(
            "method" to "track.scrobble",
            "artist[0]" to artist,
            "track[0]" to track,
            "timestamp[0]" to startedAtEpochSeconds.toString(),
            "api_key" to apiKey,
            "sk" to session,
        )
        album?.takeIf(String::isNotBlank)?.let { params["album[0]"] = it }
        if (durationSeconds > 0L) params["duration[0]"] = durationSeconds.toString()
        postSigned(params)
        Unit
    }

    private suspend fun <T> apiCall(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching(block)
    }

    private fun requireConfigured() {
        check(isConfigured()) { "Last.fm is not configured in this build" }
    }

    private fun requireSession(): String {
        requireConfigured()
        return sessionKey?.takeIf(String::isNotBlank)
            ?: throw LastFmApiException(9, "Last.fm account is not connected")
    }

    private fun postSigned(unsignedParams: Map<String, String>): JSONObject {
        val params = unsignedParams.toMutableMap()
        params["api_sig"] = signature(params)
        params["format"] = "json"
        return post(params)
    }

    private fun post(params: Map<String, String>): JSONObject {
        val form = FormBody.Builder().apply {
            params.forEach { (name, value) -> add(name, value) }
        }.build()
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("User-Agent", "Cubic-Music/${app.kreate.android.BuildConfig.VERSION_NAME}")
            .post(form)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val payload = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Last.fm HTTP ${response.code}")
            }
            val json = runCatching { JSONObject(payload) }
                .getOrElse { throw IOException("Last.fm returned an invalid response", it) }
            val errorCode = json.optInt("error", 0)
            if (errorCode != 0) {
                throw LastFmApiException(
                    errorCode = errorCode,
                    message = json.optString("message").ifBlank { "Last.fm request failed" },
                )
            }
            json
        }
    }

    private fun signature(params: Map<String, String>): String {
        val source = buildString {
            params.toSortedMap().forEach { (name, value) ->
                if (name != "format" && name != "api_sig") append(name).append(value)
            }
            append(apiSecret)
        }
        return MessageDigest.getInstance("MD5")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun org.json.JSONArray?.toTasteTracks(defaultAffinity: Double): List<LastFmTasteTrack> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val artist = when (val rawArtist = item.opt("artist")) {
                    is JSONObject -> rawArtist.optString("name")
                    is String -> rawArtist
                    else -> ""
                }.trim()
                val title = item.optString("name").trim()
                if (artist.isBlank() || title.isBlank()) continue
                add(
                    LastFmTasteTrack(
                        artist = artist,
                        title = title,
                        affinity = item.optString("match")
                            .toDoubleOrNull()
                            ?: defaultAffinity,
                    ),
                )
            }
        }
    }

    private fun org.json.JSONArray?.toTasteArtists(defaultAffinity: Double): List<LastFmTasteArtist> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                add(
                    LastFmTasteArtist(
                        name = name,
                        affinity = item.optString("match")
                            .toDoubleOrNull()
                            ?: defaultAffinity,
                    ),
                )
            }
        }
    }
}

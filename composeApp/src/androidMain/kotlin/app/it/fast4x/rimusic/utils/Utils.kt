package app.it.fast4x.rimusic.utils


import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import app.cubic.android.core.network.NetworkQualityHelper
import app.cubic.android.core.network.isNetworkAvailable
import app.cubic.android.core.network.isNetworkConnected
import app.cubic.android.core.network.isNetworkAvailableComposable
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import com.zionhuang.innertube.pages.LibraryPage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic.addToPlaylist
import it.fast4x.innertube.YtMusic.likeVideoOrSong
import it.fast4x.innertube.YtMusic.removelikeVideoOrSong
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import androidx.media3.session.MediaConstants.EXTRAS_KEY_IS_EXPLICIT
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.MODIFIED_PREFIX
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.context
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.Lyrics
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.ui.components.themed.NewVersionDialog
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import app.kreate.android.me.knighthat.utils.Toaster
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

const val EXPLICIT_BUNDLE_TAG = "is_explicit"
private const val SIMPMUSIC_LYRICS_API = "https://api-lyrics.simpmusic.org/v1"

data class SimpMusicLyricsResult(
    val syncedLyrics: String?,
    val richSyncLyrics: String? = null,
    val plainLyrics: String?,
    val translatedLyrics: String? = null,
    val translatedLanguage: String? = null,
)

fun resolveLocalMediaUri(id: String): Uri {
    val localId = id.substringAfter(LOCAL_KEY_PREFIX, missingDelimiterValue = id)
        .trim()
        .toLongOrNull()

    return when {
        localId != null -> ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            localId
        )
        id.startsWith("content://", true) || id.startsWith("file://", true) -> sanitizePlaybackUri(id)
        else -> sanitizePlaybackUri(id.removePrefix(LOCAL_KEY_PREFIX))
    }
}

private fun normalizeSimpMusicSyncedLyrics(raw: String?): String? {
    if (raw.isNullOrBlank()) return null

    val timestampedLineRegex = Regex("""^\[(\d{2}:\d{2}\.\d{2,3})](.*)$""")
    val taggedRichLineRegex = Regex("""^\[([a-zA-Z0-9]+):(.*)$""")
    val inlineTimestampRegex = Regex("""<(\d{2}:\d{2}\.\d{2,3})>""")

    val normalizedLines = raw.lines().mapNotNull { originalLine ->
        val line = originalLine.trim()
        if (line.isBlank()) return@mapNotNull null

        timestampedLineRegex.matchEntire(line)?.let { match ->
            val timestamp = match.groupValues[1]
            val content = match.groupValues[2]
                .replace(Regex("""^[a-zA-Z0-9]+:"""), "")
                .replace(inlineTimestampRegex, "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            return@mapNotNull if (content.isBlank()) null else "[$timestamp]$content"
        }

        taggedRichLineRegex.matchEntire(line)?.let { match ->
            val contentPortion = match.groupValues[2]
            val inlineTime = inlineTimestampRegex.find(contentPortion)?.groupValues?.getOrNull(1)
            val content = contentPortion
                .replace(inlineTimestampRegex, "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            return@mapNotNull when {
                inlineTime != null && content.isNotBlank() -> "[$inlineTime]$content"
                content.isNotBlank() -> content
                else -> null
            }
        }

        line
    }

    return normalizedLines.joinToString("\n").ifBlank { null }
}

private fun parseSimpMusicLyricsJson(raw: String): SimpMusicLyricsResult? {
    val root = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return null

    fun org.json.JSONObject.toLyricsResult(): SimpMusicLyricsResult? {
        val syncedLyrics = normalizeSimpMusicSyncedLyrics(
            optString("syncedLyrics").takeIf { it.isNotBlank() }
        )
        val richSyncLyrics = normalizeSimpMusicSyncedLyrics(
            optString("richSyncLyrics").takeIf { it.isNotBlank() }
        )
        val plainLyrics = optString("plainLyrics").takeIf { it.isNotBlank() }
            ?: optString("plainLyric").takeIf { it.isNotBlank() }
        val translatedLyrics = normalizeSimpMusicSyncedLyrics(
            optString("translatedLyrics").takeIf { it.isNotBlank() }
                ?: optString("translatedLyric").takeIf { it.isNotBlank() }
        )
        val translatedLanguage = optString("language").takeIf { it.isNotBlank() }

        if (syncedLyrics == null && richSyncLyrics == null && plainLyrics == null && translatedLyrics == null) return null

        return SimpMusicLyricsResult(
            syncedLyrics = syncedLyrics ?: richSyncLyrics,
            richSyncLyrics = richSyncLyrics,
            plainLyrics = plainLyrics,
            translatedLyrics = translatedLyrics,
            translatedLanguage = translatedLanguage,
        )
    }

    root.optJSONObject("data")?.toLyricsResult()?.let { return it }

    val directItems = root.optJSONArray("data")
    val wrappedData = root.optJSONObject("data")?.optJSONArray("data")
    val resultItems = root.optJSONArray("result")
    val candidates = directItems ?: wrappedData ?: resultItems ?: return null

    val parsedCandidates = (0 until candidates.length())
        .mapNotNull { index -> candidates.optJSONObject(index)?.toLyricsResult() }

    return parsedCandidates.maxByOrNull { candidate ->
        when {
            !candidate.translatedLyrics.isNullOrBlank() -> 4
            !candidate.syncedLyrics.isNullOrBlank() -> 3
            !candidate.richSyncLyrics.isNullOrBlank() -> 2
            !candidate.plainLyrics.isNullOrBlank() -> 1
            else -> 0
        }
    }
}

suspend fun fetchSimpMusicLyrics(
    videoId: String,
    translatedLanguage: String? = null,
    useTranslatedLyrics: Boolean = false,
): SimpMusicLyricsResult? {
    if (videoId.isBlank()) return null

    return withContext(Dispatchers.IO) {
        runCatching {
            fun fetchJson(url: String): String? {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SimpMusicLyrics/1.0")
                    setRequestProperty("Content-Type", "application/json")
                }

                return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            }

            val baseResult = fetchJson("$SIMPMUSIC_LYRICS_API/$videoId?limit=10")
                ?.let(::parseSimpMusicLyricsJson)
                ?: return@runCatching null

            if (!useTranslatedLyrics || translatedLanguage.isNullOrBlank()) {
                return@runCatching baseResult
            }

            val translatedResult = fetchJson("$SIMPMUSIC_LYRICS_API/translated/$videoId/${translatedLanguage.lowercase()}?limit=1")
                ?.let(::parseSimpMusicLyricsJson)

            if (translatedResult?.translatedLyrics.isNullOrBlank()) {
                return@runCatching baseResult
            }

            baseResult.copy(
                syncedLyrics = translatedResult?.translatedLyrics ?: baseResult.syncedLyrics,
                translatedLyrics = translatedResult?.translatedLyrics,
                translatedLanguage = translatedResult?.translatedLanguage ?: translatedLanguage.lowercase()
            )
        }
            .getOrNull()
    }
}

private fun filteredVideoAuthorNames(authors: List<Innertube.Info<*>>?): List<String> =
    authors
        ?.mapNotNull { author ->
            val name = author.name?.trim().orEmpty()
            when {
                name.isBlank() -> null
                author.endpoint != null -> name
                name.contains(" views", ignoreCase = true) -> null
                name.contains(" view", ignoreCase = true) -> null
                else -> name
            }
        }
        ?.distinct()
        .orEmpty()

fun sanitizePlaybackUri(raw: String): Uri {
    val trimmed = raw.trim().removeSurrounding("\"").removeSurrounding("'")
    if (
        trimmed.isBlank() ||
        trimmed.equals("null", ignoreCase = true) ||
        trimmed.equals("undefined", ignoreCase = true) ||
        trimmed.equals("about:blank", ignoreCase = true)
    ) {
        return Uri.EMPTY
    }

    val originalUri = runCatching { trimmed.toUri() }.getOrNull()
    if (
        originalUri != null &&
        !originalUri.scheme.isNullOrBlank()
    ) {
        val sanitized = originalUri.buildUpon()
            .clearQuery()
            .fragment(null)
            .build()
        if (
            (sanitized.scheme.equals("file", ignoreCase = true) ||
                sanitized.scheme.equals("content", ignoreCase = true)) &&
            sanitized.path.isNullOrBlank()
        ) {
            return Uri.EMPTY
        }
        return sanitized
    }

    val cleaned = trimmed
        .substringBefore(" ")
        .substringBefore("?")
        .substringBefore("#")
        .trim()

    if (
        cleaned.isBlank() ||
        cleaned.equals("null", ignoreCase = true) ||
        cleaned.equals("undefined", ignoreCase = true)
    ) {
        return Uri.EMPTY
    }

    return runCatching {
        val parsed = cleaned.toUri()
        when {
            parsed == Uri.EMPTY -> Uri.EMPTY
            parsed.scheme.isNullOrBlank() -> {
                if (cleaned.contains('/') || cleaned.contains('\\')) {
                    cleaned.replace('\\', '/').toUri()
                } else if (cleaned.any { it.isLetterOrDigit() }) {
                    cleaned.toUri()
                } else {
                    Uri.EMPTY
                }
            }
            else -> parsed.buildUpon()
                .clearQuery()
                .fragment(null)
                .build()
        }
    }.getOrElse { Uri.EMPTY }
}

val Innertube.AlbumItem.asAlbum: Album
    get() = Album (
        id = key,
        title = info?.name,
        thumbnailUrl = thumbnail?.url,
        year = year,
        authorsText = authors?.joinToString(", ") { it.name ?: "" },
        //shareUrl =
    )

val Innertube.Podcast.EpisodeItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(videoId)
        .setUri(sanitizePlaybackUri(videoId))
        .setCustomCacheKey(videoId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(author.toString())
                .setAlbumTitle(title)
                .setArtworkUri(thumbnail.firstOrNull()?.url?.toUri())
                .setExtras(
                    bundleOf(
                        //"albumId" to album?.endpoint?.browseId,
                        "durationText" to durationString,
                        "artistNames" to author,
                        EXTRAS_KEY_IS_EXPLICIT to false,
                        //"artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                    )
                )
                .build()
        )
        .build()

val Innertube.SongItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(sanitizePlaybackUri(key))
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.filter {it.name?.matches(Regex("\\s*([,&])\\s*")) == false }?.joinToString(", ") { it.name ?: "" })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "albumId" to album?.endpoint?.browseId,
                        "durationText" to durationText,
                        "artistNames" to authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name },
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        EXPLICIT_BUNDLE_TAG to explicit,
                        EXTRAS_KEY_IS_EXPLICIT to explicit,
                        "setVideoId" to setVideoId,
                    )
                )
                .build()
        )
        .build()

val Innertube.SongItem.asSong: Song
    get() = Song (
        id = key,
        title = (if( explicit ) EXPLICIT_PREFIX else "").plus( info?.name ?: "" ),
        artistsText = authors?.joinToString(", ") { it.name ?: "" },
        durationText = durationText,
        thumbnailUrl = thumbnail?.url
    )

val Innertube.VideoItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(sanitizePlaybackUri(key))
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(filteredVideoAuthorNames(authors).joinToString(", "))
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "artistNames" to filteredVideoAuthorNames(authors),
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        "isOfficialMusicVideo" to isOfficialMusicVideo,
                        "isUserGeneratedContent" to isUserGeneratedContent,
                        "isVideo" to true,
                        // "artistNames" to if (isOfficialMusicVideo) authors?.filter { it.endpoint != null }?.mapNotNull { it.name } else null,
                        // "artistIds" to if (isOfficialMusicVideo) authors?.mapNotNull { it.endpoint?.browseId } else null,
                    )
                )
                .build()
        )
        .build()


val Song.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(cleanPrefix(title))
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        EXPLICIT_BUNDLE_TAG to title.startsWith( EXPLICIT_PREFIX, true ),
                        EXTRAS_KEY_IS_EXPLICIT to title.startsWith( EXPLICIT_PREFIX, true )
                    )
                )
                 .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .setMediaId(id)
        .setUri(
            when {
                id.startsWith(LOCAL_KEY_PREFIX) -> resolveLocalMediaUri(id)
                id.startsWith("content://", true) || id.startsWith("file://", true) -> sanitizePlaybackUri(id)
                else -> sanitizePlaybackUri(id)
            }
        )
        .setCustomCacheKey(id)
        .build()

val Innertube.VideoItem.asSong: Song
    get() = Song (
        id = key,
        title = info?.name ?: "",
        artistsText = filteredVideoAuthorNames(authors).joinToString(", "),
        durationText = durationText,
        thumbnailUrl = thumbnail?.url
    )

val MediaItem.asSong: Song
    get() = Song (
        id = when {
            mediaId.startsWith(LOCAL_KEY_PREFIX) -> mediaId
            mediaId.startsWith("content://", true) -> mediaId
            mediaId.startsWith("file://", true) -> mediaId
            else -> mediaId.split("/").lastOrNull() ?: mediaId
        },
        title = cleanPrefix(mediaMetadata.title.toString()),
        artistsText = mediaMetadata.artist.toString(),
        durationText = mediaMetadata.extras?.getString("durationText"),
        thumbnailUrl = mediaMetadata.artworkUri
            ?.toString()
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    )

val MediaItem.cleaned: MediaItem
    get() {
        // Add more if needed
        val isTitleModified = mediaMetadata.title?.startsWith( MODIFIED_PREFIX )
        val isArtistModified = mediaMetadata.artist?.startsWith( MODIFIED_PREFIX )

        if( isTitleModified == false && isArtistModified == false )
            // Return as-is if no property is modified
            // Reduce conversion time significantly when
            // some (if not most) of media items are not modified.
            return this

        val newMetadata: MediaMetadata = mediaMetadata.buildUpon()
                                                      .setTitle( cleanPrefix(mediaMetadata.title.toString()) )
                                                      .setArtist( cleanPrefix(mediaMetadata.artist.toString()) )
                                                      .build()
        return buildUpon().setMediaMetadata( newMetadata ).build()
    }

val MediaItem.isVideo: Boolean
    get() = mediaMetadata.extras?.getBoolean("isVideo") == true

val MediaItem.isExplicit: Boolean
    get() {
        val isTitleContain = mediaMetadata.title?.startsWith( EXPLICIT_PREFIX, true )
        val isBundleContain = mediaMetadata.extras?.getBoolean( EXPLICIT_BUNDLE_TAG )
        val isStandardContain = mediaMetadata.extras?.getBoolean("androidx.media3.session.EXTRAS_KEY_IS_EXPLICIT")

        return isTitleContain == true || isBundleContain == true || isStandardContain == true
    }


fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")
fun durationToMillis(duration: String): Long {
    val parts = duration.split(":")
    if (parts.size == 3){
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val seconds = parts[2].toLong()
        return hours * 3600000 + minutes * 60000 + seconds * 1000
    } else {
        val minutes = parts[0].toLong()
        val seconds = parts[1].toLong()
        return minutes * 60000 + seconds * 1000
    }
}

fun durationTextToMillis(duration: String): Long {
    return try {
        durationToMillis(duration)
    } catch (e: Exception) {
        0L
    }
}

fun plainLyricsFromTimedText(timedLyrics: String?): String? =
    timedLyrics
        ?.lineSequence()
        ?.map { line -> line.replace(Regex("""\[[^\]]*]"""), "").trim() }
        ?.filter { it.isNotBlank() }
        ?.joinToString("\n")
        ?.ifBlank { null }

fun pickBestLrcLibTrack(
    tracks: List<it.fast4x.lrclib.models.Track>,
    title: String,
    durationMs: Long
): it.fast4x.lrclib.models.Track? {
    if (tracks.isEmpty()) return null

    val normalizedTitle = cleanPrefix(title).trim().lowercase()
    val durationSeconds = (durationMs / 1000L).coerceAtLeast(0L)

    return tracks.minByOrNull { track ->
        val normalizedTrackTitle = cleanPrefix(track.trackName).trim().lowercase()
        val durationDelta = (track.duration - durationSeconds).absoluteValue
        val titlePenalty = if (normalizedTrackTitle == normalizedTitle) 0L else 1_000L
        val syncedPenalty = if (track.syncedLyrics.isNullOrBlank()) 500L else 0L
        val plainPenalty = if (track.plainLyrics.isNullOrBlank()) 250L else 0L

        durationDelta + titlePenalty + syncedPenalty + plainPenalty
    }
}


fun formatAsTime(millis: Long): String {
    //if (millis == 0L) return ""
    val timePart1 = Duration.ofMillis(millis).toMinutes().minutes
    val timePart2 = Duration.ofMillis(millis).seconds % 60

    return "${timePart1} ${timePart2}s"
}

@JvmName("ResultInnertubeItemsPageCompleted")
suspend fun Result<Innertube.ItemsPage<Innertube.SongItem>?>.completed(
    maxDepth: Int =  Int.MAX_VALUE
): Result<Innertube.ItemsPage<Innertube.SongItem>?> = runCatching {
    val page = getOrThrow()
    val songs = page?.items.orEmpty().toMutableList()
    var continuation = page?.continuation

    var depth = 0
    var continuationsList = arrayOf<String>()
    //continuationsList += continuation.orEmpty()

    println("mediaItem playlist completed() continuation? $continuation")

    while (continuation != null && depth++ < maxDepth) {
        val newSongs = Innertube
            .playlistPage(
                body = ContinuationBody(continuation = continuation)
            )
            ?.getOrNull()
            ?.takeUnless { it.items.isNullOrEmpty() } ?: break

        newSongs.items?.let { songs += it.filter { it !in songs } }
        continuation = newSongs.continuation

        //println("mediaItem loop $depth continuation founded ${continuationsList.contains(continuation)} $continuation")
        if (continuationsList.contains(continuation)) break

        continuationsList += continuation.orEmpty()
        //println("mediaItem loop continuationList size ${continuationsList.size}")
    }

    page?.copy(items = songs, continuation = null)
}.also { it.exceptionOrNull()?.printStackTrace() }

@JvmName("ResultInnertubePlaylistOrAlbumPageCompleted")
suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(
    maxDepth: Int =  Int.MAX_VALUE
): Result<Innertube.PlaylistOrAlbumPage> = runCatching {
    val page = getOrThrow()
    val songsPage = runCatching {
        page.songsPage
    }.onFailure {
        println("Innertube songsPage PlaylistOrAlbumPage>.completed ${it.stackTraceToString()}")
    }
    val itemsPage = songsPage.completed(maxDepth).getOrThrow()
    page.copy(songsPage = itemsPage)
}.onFailure {
    println("Innertube PlaylistOrAlbumPage>.completed ${it.stackTraceToString()}")
}

//@JvmName("completedPlaylist")
suspend fun Result<LibraryPage?>.completed(): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page?.items?.toMutableList()
    var continuation = page?.continuation
    while (continuation != null) {
        val continuationPage = Innertube.libraryContinuation(continuation).getOrNull()
        if (continuationPage != null)
            if (items != null) {
                items += continuationPage.items
            }

        continuation = continuationPage?.continuation
    }
    LibraryPage(
        items = items ?: emptyList(),
        continuation = page?.continuation
    )
}


@Composable
fun CheckAvailableNewVersion(
    onDismiss: () -> Unit,
    updateAvailable: (Boolean) -> Unit
) {
    var updatedProductName = ""
    var updatedVersionName = ""
    var updatedVersionCode = 0
    val file = File(LocalContext.current.filesDir, "RiMusicUpdatedVersionCode.ver")
    if (file.exists()) {
        val dataText = file.readText().substring(0, file.readText().length - 1).split("-")
        updatedVersionCode =
            try {
                dataText.first().toInt()
            } catch (e: Exception) {
                0
            }
        updatedVersionName = if(dataText.size == 3) dataText[1] else ""
        updatedProductName =  if(dataText.size == 3) dataText[2] else ""
    }

    if (updatedVersionCode > getVersionCode()) {
        //if (updatedVersionCode > BuildConfig.VERSION_CODE)
        NewVersionDialog(
            updatedVersionName = updatedVersionName,
            updatedVersionCode = updatedVersionCode,
            updatedProductName = updatedProductName,
            onDismiss = onDismiss
        )
        updateAvailable(true)
    } else {
        updateAvailable(false)
        onDismiss()
    }
}

fun isNetworkConnected(context: Context): Boolean = context.isNetworkConnected

@Composable
fun isNetworkAvailableComposable(): Boolean {
    val context = LocalContext.current
    return context.isNetworkAvailableComposable.value
}


@Composable
fun getVersionName(): String {
    val context = LocalContext.current
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return pInfo.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun getLongVersionCode(): Long {
    val context = LocalContext.current
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return pInfo.longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return 0L
}


@Composable
fun getVersionCode(): Int {
    val context = LocalContext.current
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return pInfo.longVersionCode.toInt()
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return 0
}


inline val isAtLeastAndroid6
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

inline val isAtLeastAndroid7
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

inline val isAtLeastAndroid81
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

inline val isAtLeastAndroid10
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

inline val isAtLeastAndroid11
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

inline val isAtLeastAndroid14
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

fun Modifier.conditional(condition : Boolean, modifier : Modifier.() -> Modifier) : Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

suspend fun downloadSyncedLyrics( song: Song ) {
    val storedLyrics = Database.lyricsTable.findBySongId( song.id ).first()
    if( storedLyrics?.synced != null ) return

    var fetchedLyrics: Lyrics? = null
    LrcLib.lyrics(
        artist = song.cleanArtistsText(),
        title = song.cleanTitle(),
        duration = durationTextToMillis( song.durationText.orEmpty() ).milliseconds
    )?.onSuccess {
        fetchedLyrics = Lyrics(
            songId = song.id,
            fixed = storedLyrics?.fixed,
            synced = it?.text.orEmpty()
        )
    }?.onFailure {
        // Try out different source for lyrics
        KuGou.lyrics(
            artist = song.cleanArtistsText(),
            title = song.cleanTitle(),
            duration = durationTextToMillis( song.durationText.orEmpty() ) / 1000
        )?.onSuccess {
            fetchedLyrics = Lyrics(
                songId = song.id,
                fixed = storedLyrics?.fixed,
                synced = it?.value.orEmpty()
            )
        }?.onFailure {
            fetchSimpMusicLyrics(song.id)?.let { simpLyrics ->
                fetchedLyrics = Lyrics(
                    songId = song.id,
                    fixed = simpLyrics.plainLyrics ?: storedLyrics?.fixed,
                    synced = simpLyrics.syncedLyrics ?: storedLyrics?.synced
                )
            }
        }
    }

    if( fetchedLyrics != null )
        Database.asyncTransaction {
            lyricsTable.upsert( fetchedLyrics!! )
        }
}

suspend fun addToYtPlaylist(localPlaylistId: Long, position: Int, ytplaylistId: String, mediaItems: List<MediaItem>){
    val mediaItemsChunks = mediaItems.chunked(50)
    mediaItemsChunks.forEachIndexed { index, items ->
        if (mediaItems.size <= 50) {}
        else if (index == 0) {
            Toaster.i(
                "${mediaItems.size} "+appContext().resources.getString(R.string.songs_adding_in_yt)
            )
        } else {
            delay(2000)
        }
        addToPlaylist(ytplaylistId, items.map { it.mediaId })
            .onSuccess {
                Database.playlistTable
                        .findById( localPlaylistId )
                        .first()
                        ?.let {
                            Database.mapIgnore( it, *items.toTypedArray() )
                        }
                if (items.size == 50)
                    Toaster.i( "${mediaItems.size - (index + 1) * 50} Songs Remaining" )
            }
            .onFailure {
                println("YtMusic addToPlaylist (list of size ${items.size}) error: ${it.stackTraceToString()}")
                if(it is ClientRequestException && it.response.status == HttpStatusCode.BadRequest) {
                    Toaster.w( R.string.adding_yt_to_pl_failed )
                    items.forEach { item ->
                        delay(500)
                        addToPlaylist(ytplaylistId, item.mediaId).onFailure {
                            println("YtMusic addToPlaylist (list insert backup) error: ${it.stackTraceToString()}")
                                Toaster.e(
                                    appContext().resources.getString(R.string.songs_add_yt_failed)+"${item.mediaMetadata.title} - ${item.mediaMetadata.artist}"
                                )
                        }.onSuccess {
                            Database.playlistTable
                                    .findById( localPlaylistId )
                                    .first()
                                    ?.let { playlist ->
                                        Database.mapIgnore( playlist, *items.toTypedArray() )
                                    }
                            Toaster.n( "${items.size - (index + 1)} Songs Remaining" )
                        }
                    }
                }
            }
    }

    Toaster.n(
        "${mediaItems.size} "+ appContext().resources.getString(R.string.songs_added_in_yt)
    )
}

suspend fun addSongToYtPlaylist(localPlaylistId: Long, position: Int, ytplaylistId: String, mediaItem: MediaItem){
    if (isYouTubeSyncEnabled()) {
        addToPlaylist(ytplaylistId,mediaItem.mediaId)
            .onSuccess {
                Database.playlistTable.findById( localPlaylistId ).first()?.let {
                    Database.mapIgnore( it, mediaItem )
                }
                Toaster.s( R.string.songs_add_yt_success )
            }
            .onFailure {
                Toaster.e( R.string.songs_add_yt_failed )
            }

    }
}


@OptIn(UnstableApi::class)
suspend fun addToYtLikedSong(mediaItem: MediaItem) {
    if( !isYouTubeSyncEnabled() ) return

    val isSongLiked = withContext(Dispatchers.IO) {
        Database.insertIgnore(mediaItem)
        Database.songTable.isLiked(mediaItem.mediaId).first()
    }

    val isSuccess: Boolean =
        (if( isSongLiked ) likeVideoOrSong( mediaItem.mediaId ) else removelikeVideoOrSong( mediaItem.mediaId )).isSuccess

    val messageId = when {
        isSongLiked && isSuccess -> R.string.songs_liked_yt
        isSongLiked && !isSuccess -> R.string.songs_liked_yt_failed
        !isSongLiked && isSuccess -> R.string.song_unliked_yt
        !isSongLiked && !isSuccess -> R.string.songs_unliked_yt_failed
        else -> throw RuntimeException()
    }

    if( isSuccess ) {
        Database.songTable.toggleLike(mediaItem.mediaId)
        Toaster.s( messageId )
    } else
        Toaster.e( messageId)
}

@OptIn(UnstableApi::class)
suspend fun addToYtLikedSongs(mediaItems: List<MediaItem>){
    if( !isYouTubeSyncEnabled() ) return

    mediaItems.forEachIndexed { index, item ->
        delay(1000)

        likeVideoOrSong( item.mediaId )
            .onSuccess {
                Database.asyncTransaction {
                    insertIgnore( item )
                    songTable.likeState( item.mediaId, true )
                    MyDownloadHelper.autoDownloadWhenLiked(
                        context(),
                        item
                    )

                    Toaster.s(
                        "${index + 1}/${mediaItems.size} " + appContext().resources.getString(R.string.songs_liked_yt)
                    )
                }
            }
            .onFailure {
                Toaster.e( "${index + 1}/${mediaItems.size} " + appContext().resources.getString(R.string.songs_liked_yt_failed) )
            }
    }
}

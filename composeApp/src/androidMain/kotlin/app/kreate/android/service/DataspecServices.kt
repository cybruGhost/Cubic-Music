package app.kreate.android.service

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import app.kreate.android.R
import app.kreate.android.Threads
import app.cubic.android.core.network.Store
import app.kreate.android.utils.CharUtils
import com.google.gson.Gson
import com.grack.nanojson.JsonObject
import io.ktor.client.statement.bodyAsText
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.clients.YouTubeLocale
import app.it.fast4x.rimusic.models.Song
import it.fast4x.innertube.Innertube.createPoTokenChallenge
import it.fast4x.innertube.Innertube.SearchFilter
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.innertube.requests.searchPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.isConnectionMeteredEnabled
import app.it.fast4x.rimusic.models.Format
import app.it.fast4x.rimusic.service.LoginRequiredException
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.NoInternetException
import app.it.fast4x.rimusic.service.PlayableFormatNotFoundException
import app.it.fast4x.rimusic.service.UnknownException
import app.it.fast4x.rimusic.service.UnplayableException
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.isNetworkConnected
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.utils.isConnectionMetered
import app.it.fast4x.rimusic.utils.okHttpDataSourceFactory
import app.it.fast4x.rimusic.utils.getPipedSession
import app.it.fast4x.rimusic.utils.preferences
import app.cubic.android.core.network.NetworkClientFactory
import app.cubic.android.core.utils.cipher.CipherDeobfuscator
import app.cubic.android.core.utils.potoken.PoTokenGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import app.kreate.android.me.knighthat.utils.Toaster
import it.fast4x.invidious.Invidious
import it.fast4x.piped.Piped
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import org.json.JSONArray
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.utils.NewPipeUtils
import it.fast4x.innertube.utils.from
import timber.log.Timber

private const val CHUNK_LENGTH = 512 * 1024L     // 512Kb
private const val STREAM_RESOLVE_RETRIES = 3
private const val FORMAT_CACHE_EXPIRY_SAFETY_MS = 30_000L
private const val INNERTUBE_CLIENT_TIMEOUT_MS = 5_000L
private const val LAST_SUCCESSFUL_YT_CLIENT_AUTH_KEY = "last_successful_yt_client_auth"
private const val LAST_SUCCESSFUL_YT_CLIENT_NOAUTH_KEY = "last_successful_yt_client_noauth"

private val formatCache = mutableMapOf<String, Uri>()
private val formatCacheLock = Any()

private val FALLBACK_CLIENTS = listOf(
    YouTubeClient.ANDROID_VR_1_43_32,
    YouTubeClient.ANDROID_VR_1_61_48,
    YouTubeClient.ANDROID_VR_NO_AUTH,
    YouTubeClient.IOS,
    YouTubeClient.MOBILE,
    YouTubeClient.ANDROID_CREATOR,
    YouTubeClient.IPADOS,
    YouTubeClient.WEB_REMIX,
    YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
    YouTubeClient.TVHTML5,
    YouTubeClient.WEB,
    YouTubeClient.WEB_CREATOR
)

enum class PlaybackSourceKind(val label: String) {
    Unknown("Waiting"),
    Local("Local"),
    YouTubeAndroid("YouTube"),
    YouTubeIos("YouTube iOS"),
    YouTubeInnertube("YouTube Player"),
    Invidious("Invidious"),
    Piped("Piped")
}

data class PlaybackSourceStatus(
    val source: PlaybackSourceKind = PlaybackSourceKind.Unknown,
    val videoId: String = "",
    val isFallback: Boolean = false,
    val updatedAt: Long = 0L
)

object PlaybackSourceMonitor {
    private val _status = MutableStateFlow(PlaybackSourceStatus())
    val status: StateFlow<PlaybackSourceStatus> = _status.asStateFlow()

    fun report(source: PlaybackSourceKind, videoId: String, isFallback: Boolean = false) {
        val normalizedVideoId = videoId.trim()
        val current = _status.value
        if (
            current.source == source &&
            current.videoId == normalizedVideoId &&
            current.isFallback == isFallback
        ) return

        _status.value = PlaybackSourceStatus(
            source = source,
            videoId = normalizedVideoId,
            isFallback = isFallback,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Store id of song just added to the database.
 * This is created to reduce load to Room
 */
@set:Synchronized
private var justInserted: String = ""

/**
 * Reach out to `next` endpoint for song's information.
 *
 * Info includes:
 * - Titles
 * - Artist(s)
 * - Album
 * - Thumbnails
 * - Duration
 *
 * ### If song IS already inside database
 *
 * It'll replace unmodified columns with fetched data
 *
 * ### If song IS NOT already inside database
 *
 * New record will be created and insert into database
 *
 */
@Blocking
private fun upsertSongInfo( videoId: String ) = runBlocking {       // Use this to prevent suspension of thread while waiting for response from YT

    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return@runBlocking

    Innertube.nextPage( NextBody(videoId = videoId) )?.fold(
        onSuccess = { nextPage ->
            val songItem = nextPage.itemsPage?.items?.firstOrNull() ?: return@fold
            Database.upsert( songItem )
        },
        onFailure = {
            when( it ) {
                // [UnknownHostException] means no internet connection in most cases
                // Set [justInserted] to this video will skip subsequence calls
                is UnknownHostException -> justInserted = videoId
                else                    -> Toaster.e( R.string.failed_to_fetch_original_property )
            }
        }
    )
}

/**
 * Upsert provided format to the database
 */
@NonBlocking
private fun upsertSongFormat( videoId: String, format: PlayerResponse.StreamingData.Format ) {
    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return

    runCatching {
        val itag = format.itagValue ?: return@runCatching
        val bitrate = format.bitrateValue ?: 0

        Database.asyncTransaction {
                  // Ensure Song exists first to satisfy Foreign Key constraint
            songTable.insertIgnore(Song.makePlaceholder(videoId))

            formatTable.insertIgnore(Format(
                videoId,
                itag,
                format.mimeType,
                bitrate.toLong(),
                format.contentLengthValue,
                format.lastModifiedValue,
                format.loudnessDb?.toFloat()
            ))
        }

        // Format must be added successfully before setting variable
        justInserted = videoId
    }
}

//<editor-fold defaultstate="collapsed" desc="Extractors">
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        useArrayPolymorphism = true
        explicitNulls = false
    }

private val decimalThumbnailDimensionRegex =
    Regex("""("(?:height|width)"\s*:\s*)(\d+)\.0(?=[,}])""")

private fun normalizePlayerResponseJson(json: String): String =
    decimalThumbnailDimensionRegex.replace(json) { match ->
        "${match.groupValues[1]}${match.groupValues[2]}"
    }

@UnstableApi
private fun checkPlayability( playabilityStatus: PlayerResponse.PlayabilityStatus? ) {
    if( playabilityStatus?.status != "OK" )
        when( playabilityStatus?.status ) {
            "LOGIN_REQUIRED"    -> throw LoginRequiredException()
            "UNPLAYABLE"        -> throw UnplayableException()
            else                -> throw UnknownException()
        }
}

private fun PlayerResponse.hasPlayableAudioFormats(): Boolean =
    streamingData?.adaptiveFormats?.any { it.isAudio && (!it.url.isNullOrBlank() || !it.signatureCipher.isNullOrBlank()) } == true ||
        streamingData?.formats?.any { it.isAudio && (!it.url.isNullOrBlank() || !it.signatureCipher.isNullOrBlank()) } == true

private val knownAudioItags = setOf(139, 140, 141, 171, 249, 250, 251, 774)
private val preferredAudioItags = listOf(140, 251, 250, 249, 139, 171, 774, 141)
private val supportedAudioCodecHints = listOf("mp4a.", "opus")

private fun PlayerResponse.StreamingData.Format.hasStreamUrl(): Boolean =
    !url.isNullOrBlank() || !signatureCipher.isNullOrBlank()

private fun PlayerResponse.StreamingData.Format.isPlayableAudioCandidate(): Boolean {
    if (!isAudio || !hasStreamUrl()) return false

    val itag = itagValue
    if (itag != null) return itag in knownAudioItags

    val normalizedMimeType = mimeType.lowercase()
    val hasSupportedCodecHint = supportedAudioCodecHints.any { normalizedMimeType.contains(it) }
    if (!hasSupportedCodecHint) {
        Timber.w("Rejecting audio format with unknown itag and codec for mime=%s", mimeType)
    } else {
        Timber.d("Accepting audio format with missing itag via mime/codec fallback: %s", mimeType)
    }
    return hasSupportedCodecHint
}

private fun List<PlayerResponse.StreamingData.Format>.preferAudioItag(order: List<Int>): PlayerResponse.StreamingData.Format? =
    order.firstNotNullOfOrNull { preferredItag -> firstOrNull { it.itagValue == preferredItag } }

private fun extractFormat(
    streamingData: PlayerResponse.StreamingData?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): PlayerResponse.StreamingData.Format? {
    val audioFormats = buildList {
        streamingData?.adaptiveFormats
            ?.filter { it.isPlayableAudioCandidate() }
            ?.let(::addAll)
        streamingData?.formats
            ?.filter { it.isPlayableAudioCandidate() }
            ?.let(::addAll)
    }.distinctBy { it.itagValue ?: it.mimeType + it.url.orEmpty() + it.signatureCipher.orEmpty() }

    if (audioFormats.isEmpty()) return null

    return when (audioQualityFormat) {
        AudioQualityFormat.High ->
            audioFormats.preferAudioItag(listOf(140, 251, 141, 250, 249, 139, 171, 774))
                ?: audioFormats.maxByOrNull { it.bitrateValue ?: 0 }

        AudioQualityFormat.Medium ->
            audioFormats.preferAudioItag(listOf(140, 250, 251, 249, 139, 171, 774))
                ?: audioFormats.maxByOrNull { it.bitrateValue ?: 0 }

        AudioQualityFormat.Low ->
            audioFormats.preferAudioItag(listOf(139, 249, 250, 140, 251, 171, 774))
                ?: audioFormats.minByOrNull { it.bitrateValue ?: Int.MAX_VALUE }

        AudioQualityFormat.Auto ->
            if (connectionMetered && isConnectionMeteredEnabled()) {
                audioFormats.preferAudioItag(listOf(140, 250, 249, 139, 171, 251, 774))
                    ?: audioFormats.minByOrNull { it.bitrateValue ?: Int.MAX_VALUE }
            } else {
                audioFormats.preferAudioItag(preferredAudioItags)
                    ?: audioFormats.maxByOrNull { it.bitrateValue ?: 0 }
            }
    }
}

@UnstableApi
private fun getFormatUrl(
    videoId: String,
    cpn: String,
    responseJson: JsonObject,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean,
    streamingDataPoToken: String? = null,
    appendPlaybackParameters: Boolean = true,
): Uri {
    val jsonString = normalizePlayerResponseJson(Gson().toJson(responseJson))
    val playerResponse = jsonParser.decodeFromString<PlayerResponse>( jsonString )

    checkPlayability( playerResponse.playabilityStatus )

    val format = extractFormat( playerResponse.streamingData, audioQualityFormat, connectionMetered )
        ?: throw PlayableFormatNotFoundException()
    val formatUrl = runBlocking(Dispatchers.IO) {
        format.signatureCipher?.takeIf { it.isNotBlank() }?.let { signatureCipher ->
            CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
                ?: NewPipeUtils.decodeSignatureCipher(videoId, signatureCipher)
        } ?: NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure { Timber.w(it, "Failed to resolve YouTube stream URL for %s", videoId) }
            .getOrNull()
    } ?: throw PlayableFormatNotFoundException()

    Timber.d("Resolved audio stream for %s: itag=%s mime=%s bitrate=%s contentLength=%s isAudio=%s", videoId, format.itagValue, format.mimeType, format.bitrateValue, format.contentLengthValue, format.isAudio)
    CoroutineScope( Threads.DATASPEC_DISPATCHER ).launch { upsertSongFormat( videoId, format ) }

    val uri = formatUrl.toUri()
    if (!appendPlaybackParameters) return uri

    return uri.buildUpon()
        .appendQueryParameter( "cpn", cpn )
        .apply {
            streamingDataPoToken
                ?.takeIf { it.isNotBlank() }
                ?.let { appendQueryParameter("pot", it) }
        }
        .build()
}

@UnstableApi
suspend fun getAndroidReelFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val cpn = CharUtils.randomString( 16 )
    val response = YoutubeStreamHelper.getAndroidReelPlayerResponse( ContentCountry.DEFAULT, Localization.DEFAULT, videoId, cpn )
    return getFormatUrl( videoId, cpn, response, audioQualityFormat, connectionMetered )
}

private fun String.getPoToken(): String? =
    this.replace("[", "")
        .replace("]", "")
        .split(",")
        .findLast { it.contains("\"") }
        ?.replace("\"", "")

private suspend fun generateIosPoToken() =
    createPoTokenChallenge().bodyAsText()
        .let { challenge ->
            val listChallenge = jsonParser.decodeFromString<List<String?>>(challenge)
            listChallenge.filterIsInstance<String>().firstOrNull()
        }?.let { poTokenChallenge ->
            Innertube.generatePoToken(poTokenChallenge)
                .bodyAsText()
                .getPoToken()
        }

@UnstableApi
suspend fun getIosFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    if (isYouTubeLoggedIn()) {
        YouTubeSessionStore.applyCurrentSession()
        val authenticated = runCatching {
            val poToken = PoTokenGenerator().getWebClientPoToken(videoId, Store.getIosVisitorData())?.playerRequestPoToken
            val response = YouTubeRequestThrottler.run {
                Innertube.player(videoId = videoId, poToken = poToken)
            }?.getOrThrow() ?: throw IllegalStateException("Null player response")
            val jsonString = Gson().toJson(response)
            return@getIosFormatUrl getFormatUrl(
                videoId = videoId,
                cpn = CharUtils.randomString(16),
                responseJson = Gson().fromJson(jsonString, JsonObject::class.java),
                audioQualityFormat = audioQualityFormat,
                connectionMetered = connectionMetered
            )
        }

        authenticated.getOrElse {
            it.printStackTrace()
        }
    }

    val cpn = CharUtils.randomString( 16 )
    val visitorData = Store.getIosVisitorData()
    val playerRequestToken = generateIosPoToken().orEmpty()
    val poTokenResult = PoTokenResult(visitorData, playerRequestToken, null )
    val response = YoutubeStreamHelper.getIosPlayerResponse( ContentCountry.DEFAULT, Localization.DEFAULT, videoId, cpn, poTokenResult )
    return getFormatUrl( videoId, cpn, response, audioQualityFormat, connectionMetered )
}

@UnstableApi
suspend fun getInnertubePlayerFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    YouTubeSessionStore.applyCurrentSession()

    val locale = YouTubeLocale(
        gl = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US",
        hl = java.util.Locale.getDefault().language.takeIf { it.isNotEmpty() } ?: "en"
    )
    val visitorData = Store.getIosVisitorData().ifBlank { Innertube.visitorData.ifBlank { Innertube.DEFAULT_VISITOR_DATA } }
    val isLoggedIn = !Innertube.cookie.isNullOrBlank() && Innertube.cookie?.contains("SAPISID") == true
    val prefs = appContext().preferences
    val rememberedClientKey = if (isLoggedIn) LAST_SUCCESSFUL_YT_CLIENT_AUTH_KEY else LAST_SUCCESSFUL_YT_CLIENT_NOAUTH_KEY
    val rememberedClientName = prefs.getString(rememberedClientKey, null)
    val clientsToTry = rememberedClientName
        ?.let { clientName ->
            FALLBACK_CLIENTS.firstOrNull { it.clientName == clientName }
                ?.let { rememberedClient ->
                    Timber.d(
                        "Prioritizing remembered Innertube client: %s (%s)",
                        rememberedClient.clientName,
                        if (isLoggedIn) "auth" else "noauth"
                    )
                    listOf(rememberedClient) + FALLBACK_CLIENTS.filterNot { it.clientName == clientName }
                }
        }
        ?: FALLBACK_CLIENTS

    val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure { Timber.w(it, "Could not get signature timestamp for %s; using default", videoId) }
        .getOrNull()

    val poToken = runCatching {
        PoTokenGenerator().getWebClientPoToken(videoId, visitorData)
    }.onFailure {
        Timber.w(it, "Innertube PoToken generation failed for %s; continuing with fallback clients", videoId)
    }.getOrNull()

    var firstError: Throwable? = null
    var lastFailureReason: String? = null

    clientsToTry.forEachIndexed { index, ytClient ->
        if (ytClient.loginRequired && !isLoggedIn) {
            Timber.d("Skipping Innertube client %s for %s because login is required", ytClient.clientName, videoId)
            return@forEachIndexed
        }

        val context = ytClient.toContext(
            locale = locale,
            visitorData = visitorData,
            dataSyncId = if (isLoggedIn) Innertube.dataSyncId else null
        )

        val playerResponse = runCatching {
            Timber.d("Trying Innertube client (%d/%d): %s for %s", index + 1, clientsToTry.size, ytClient.clientName, videoId)
            withTimeout(INNERTUBE_CLIENT_TIMEOUT_MS) {
                YouTubeRequestThrottler.run {
                    Innertube.player(
                        videoId = videoId,
                        poToken = if (ytClient.useWebPoTokens) poToken?.playerRequestPoToken else null,
                        context = context,
                        signatureTimestamp = if (ytClient.useSignatureTimestamp) signatureTimestamp else null
                    )
                }?.getOrThrow() ?: throw IllegalStateException("Null Innertube player response")
            }
        }.onFailure { error ->
            if (firstError == null) firstError = error
            lastFailureReason = "${ytClient.clientName}: ${error::class.simpleName}: ${error.message}"
            Timber.w(error, "Innertube client %s failed for %s", ytClient.clientName, videoId)
        }.getOrNull() ?: return@forEachIndexed

        Timber.d(
            "Innertube client %s for %s returned status=%s audioFormats=%d",
            ytClient.clientName,
            videoId,
            playerResponse.playabilityStatus?.status,
            playerResponse.streamingData?.adaptiveFormats?.count { it.isAudio } ?: 0
        )

        if (playerResponse.playabilityStatus?.status != "OK") {
            lastFailureReason = "${ytClient.clientName}: status=${playerResponse.playabilityStatus?.status} reason=${playerResponse.playabilityStatus?.reason}"
            return@forEachIndexed
        }

        if (!playerResponse.hasPlayableAudioFormats()) {
            lastFailureReason = "${ytClient.clientName}: no playable audio formats"
            return@forEachIndexed
        }

        val cpn = CharUtils.randomString(16)
        val uri = runCatching {
            withTimeout(INNERTUBE_CLIENT_TIMEOUT_MS) {
                val jsonString = Gson().toJson(playerResponse)
                getFormatUrl(
                    videoId = videoId,
                    cpn = cpn,
                    responseJson = Gson().fromJson(jsonString, JsonObject::class.java),
                    audioQualityFormat = audioQualityFormat,
                    connectionMetered = connectionMetered,
                    streamingDataPoToken = if (ytClient.useWebPoTokens) poToken?.streamingDataPoToken else null,
                    appendPlaybackParameters = false
                )
            }
        }.onFailure { error ->
            if (firstError == null) firstError = error
            lastFailureReason = "${ytClient.clientName}: URL resolution failed: ${error.message}"
            Timber.w(error, "Innertube client %s URL resolution failed for %s", ytClient.clientName, videoId)
        }.getOrNull() ?: return@forEachIndexed

        val isValid = NetworkClientFactory.validateStreamUrl(uri.toString(), expectedContentTypePrefix = "audio/")
        if (!isValid) {
            lastFailureReason = "${ytClient.clientName}: stream URL validation failed"
            Timber.w("Innertube client %s stream URL validation failed for %s", ytClient.clientName, videoId)
            return@forEachIndexed
        }

        Timber.d("Innertube client %s stream resolved successfully for %s", ytClient.clientName, videoId)
        prefs.edit().putString(rememberedClientKey, ytClient.clientName).apply()
        return uri.buildUpon()
            .appendQueryParameter("cpn", cpn)
            .apply {
                if (ytClient.useWebPoTokens) {
                    poToken?.streamingDataPoToken
                        ?.takeIf { it.isNotBlank() }
                        ?.let { appendQueryParameter("pot", it) }
                }
            }
            .build()
    }

    Timber.e("Innertube playback failed for %s: %s", videoId, lastFailureReason)
    throw (firstError as? Exception ?: UnplayableException())
}
private suspend fun resolvePrimaryFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean,
    isFallback: Boolean = false
): Uri =
    getInnertubePlayerFormatUrl(videoId, audioQualityFormat, connectionMetered)
        .also { PlaybackSourceMonitor.report(PlaybackSourceKind.YouTubeInnertube, videoId, isFallback) }

private fun <T> pickPreferredFormat(
    high: T?,
    medium: T?,
    low: T?,
    auto: T?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): T? = when (audioQualityFormat) {
    AudioQualityFormat.High -> high
    AudioQualityFormat.Medium -> medium
    AudioQualityFormat.Low -> low
    AudioQualityFormat.Auto -> if (connectionMetered && isConnectionMeteredEnabled()) medium else auto
}

private suspend fun getInvidiousFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val response = Invidious.api.videos(videoId)?.getOrThrow()
        ?: throw IllegalStateException("Invidious response unavailable")
    val format = pickPreferredFormat(
        high = response.highestQualityFormat,
        medium = response.mediumQualityFormat,
        low = response.lowestQualityFormat,
        auto = response.autoMaxQualityFormat,
        audioQualityFormat = audioQualityFormat,
        connectionMetered = connectionMetered
    ) ?: throw IllegalStateException("No playable Invidious format found")

    return format.url?.toUri() ?: throw IllegalStateException("Invidious format URL unavailable")
}

private suspend fun getPipedFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val pipedSession = getPipedSession()
    if (pipedSession.token.isBlank() || pipedSession.apiBaseUrl.toString().isBlank()) {
        throw IllegalStateException("Piped session unavailable")
    }

    val streams = Piped.media.audioStreams(pipedSession.toApiSession(), videoId)?.getOrThrow()
        ?: throw IllegalStateException("Piped audio streams unavailable")
    val sortedStreams = streams
        .filter { !it.videoOnly && it.url.isNotBlank() }
        .sortedBy { it.bitrate }

    val format = pickPreferredFormat(
        high = sortedStreams.lastOrNull(),
        medium = sortedStreams.getOrNull(sortedStreams.lastIndex / 2),
        low = sortedStreams.firstOrNull(),
        auto = sortedStreams.lastOrNull(),
        audioQualityFormat = audioQualityFormat,
        connectionMetered = connectionMetered
    ) ?: throw IllegalStateException("No playable Piped format found")

    return format.url.toUri()
}

private suspend fun resolveAlternateProviderFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean,
    isFallback: Boolean = true
): Uri =
    runCatching {
        getInvidiousFormatUrl(videoId, audioQualityFormat, connectionMetered)
            .also { PlaybackSourceMonitor.report(PlaybackSourceKind.Invidious, videoId, isFallback) }
    }.recoverCatching {
        getPipedFormatUrl(videoId, audioQualityFormat, connectionMetered)
            .also { PlaybackSourceMonitor.report(PlaybackSourceKind.Piped, videoId, isFallback) }
    }.getOrThrow()

private suspend fun findReplacementVideoId(videoId: String): String? {
    val song = Database.songTable.findById(videoId).first() ?: return null
    val title = cleanPrefix(song.title).trim()
    if (title.isBlank()) return null

    val artists = song.artistsText
        ?.split(",")
        ?.map { cleanPrefix(it).trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()

    val queries = buildList {
        add(title)
        artists.firstOrNull()?.let { add("$title $it") }
        if (artists.size > 1) {
            add((listOf(title) + artists.take(2)).joinToString(" "))
        }
    }.distinct()

    fun normalizeMatchText(value: String): String =
        cleanPrefix(value)
            .lowercase()
            .replace(Regex("\\b(official|music video|video|audio|lyrics|visualizer|topic|vevo|hd|4k)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun scoreCandidate(candidateTitle: String, candidateArtist: String): Int {
        val expectedTitle = normalizeMatchText(title)
        val expectedArtists = artists.map(::normalizeMatchText).filter { it.isNotBlank() }
        val normalizedCandidateTitle = normalizeMatchText(candidateTitle)
        val normalizedCandidateArtist = normalizeMatchText(candidateArtist)

        if (normalizedCandidateTitle.isBlank()) return Int.MIN_VALUE
        if (
            normalizedCandidateTitle.contains("mix") ||
            normalizedCandidateTitle.contains("playlist") ||
            normalizedCandidateTitle.contains("full album")
        ) return Int.MIN_VALUE

        var score = 0
        if (normalizedCandidateTitle == expectedTitle) score += 120
        else if (
            normalizedCandidateTitle.contains(expectedTitle) ||
            expectedTitle.contains(normalizedCandidateTitle)
        ) score += 80
        else {
            val expectedTokens = expectedTitle.split(" ").filter { it.length > 1 }.toSet()
            val candidateTokens = normalizedCandidateTitle.split(" ").filter { it.length > 1 }.toSet()
            score += expectedTokens.intersect(candidateTokens).size * 18
        }

        expectedArtists.forEach { artist ->
            if (artist == normalizedCandidateArtist) score += 70
            else if (
                normalizedCandidateArtist.contains(artist) ||
                artist.contains(normalizedCandidateArtist)
            ) score += 45
            else {
                val artistTokens = artist.split(" ").filter { it.length > 1 }.toSet()
                val candidateArtistTokens = normalizedCandidateArtist.split(" ").filter { it.length > 1 }.toSet()
                score += artistTokens.intersect(candidateArtistTokens).size * 14
            }
        }

        return score
    }

    suspend fun searchReplacement(filter: SearchFilter): String? {
        var bestCandidate: Pair<String, Int>? = null

        for (query in queries) {
            val itemsPage = Innertube.searchPage(
                body = SearchBody(query = query, params = filter.value),
                fromMusicShelfRendererContent = { content ->
                    when (filter) {
                        SearchFilter.Song -> Innertube.SongItem.from(content)
                        SearchFilter.Video -> Innertube.VideoItem.from(content)
                        else -> null
                    }
                }
            )?.getOrNull()

            itemsPage?.items?.forEach { item ->
                if (item.key.isBlank() || item.key == videoId) return@forEach

                val candidateTitle = when (item) {
                    is Innertube.SongItem -> item.info?.name.orEmpty()
                    is Innertube.VideoItem -> item.info?.name.orEmpty()
                    else -> ""
                }
                val candidateArtist = when (item) {
                    is Innertube.SongItem -> item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                    is Innertube.VideoItem -> item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                    else -> ""
                }
                val score = scoreCandidate(candidateTitle, candidateArtist)
                if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                    bestCandidate = item.key to score
                }
            }
        }

        val minimumScore = if (filter == SearchFilter.Song) 80 else 65
        return bestCandidate?.takeIf { it.second >= minimumScore }?.first
    }

    fun searchOmadaReplacement(): String? {
        var bestCandidate: Pair<String, Int>? = null

        queries.forEach { query ->
            runCatching {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val connection = URL("${SecureApiConfig.resolveOmadaSearchApi()}?q=$encodedQuery")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val results = JSONArray(response)
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    if (item.optString("type") != "video") continue

                    val candidateVideoId = item.optString("videoId").trim()
                    if (candidateVideoId.isBlank() || candidateVideoId == videoId) continue

                    val score = scoreCandidate(
                        candidateTitle = item.optString("title"),
                        candidateArtist = item.optString("author")
                    )
                    if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                        bestCandidate = candidateVideoId to score
                    }
                }
            }
        }

        return bestCandidate?.takeIf { it.second >= 60 }?.first
    }

    return searchReplacement(SearchFilter.Song)
        ?: searchReplacement(SearchFilter.Video)
        ?: searchOmadaReplacement()
}
//</editor-fold>

private fun formatCacheKey(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): String = "$videoId:${audioQualityFormat.name}:$connectionMetered"

private fun Uri.isExpiredSoon(): Boolean {
    val expiresAt = getQueryParameter("expire")?.toLongOrNull()?.times(1000) ?: return false
    return System.currentTimeMillis() >= expiresAt - FORMAT_CACHE_EXPIRY_SAFETY_MS
}

@UnstableApi
fun DataSpec.process(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): DataSpec = runBlocking( Dispatchers.IO ) {
    if (!isNetworkConnected(appContext())) {
        throw NoInternetException()
    }

    val cacheKey = formatCacheKey(videoId, audioQualityFormat, connectionMetered)
    var formatUri = synchronized(formatCacheLock) {
        formatCache[cacheKey]?.takeUnless { cachedUri ->
            cachedUri.isExpiredSoon().also { expired ->
                if (expired) {
                    Timber.d("Cached stream URL expired/near-expired for %s; resolving again", videoId)
                    formatCache.remove(cacheKey)
                }
            }
        }
    }

    if (formatUri == null) {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < STREAM_RESOLVE_RETRIES && formatUri == null) {
            attempt++
            try {
                formatUri = resolvePrimaryFormatUrl(videoId, audioQualityFormat, connectionMetered, isFallback = false)
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Stream extraction failed on attempt %d/%d for %s", attempt, STREAM_RESOLVE_RETRIES, videoId)
                if (attempt < STREAM_RESOLVE_RETRIES) {
                    delay(500L * attempt)
                }
            }
        }

        val newlyResolvedUri = formatUri ?: throw (lastException ?: UnplayableException())
        synchronized(formatCacheLock) {
            formatCache[cacheKey] = newlyResolvedUri
        }
        formatUri = newlyResolvedUri
    } else {
        Timber.d("Using cached stream URL for %s", videoId)
    }

    val resolvedFormatUri = formatUri ?: throw UnplayableException()
    withUri( resolvedFormatUri ).subrange( uriPositionOffset )
}

//<editor-fold defaultstate="collapsed" desc="Data source factories">
@UnstableApi
fun PlayerServiceModern.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    // This factory resolves the Video ID to a real URL when needed (cache miss)
    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        val isLocal = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT || dataSpec.uri.scheme == ContentResolver.SCHEME_FILE

        if (isLocal) {
            PlaybackSourceMonitor.report(PlaybackSourceKind.Local, videoId)
            return@Factory dataSpec
        }

        // Only upsert info if we are actually resolving (cache miss)
        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        // Always resolve URL for non-local files and ensure key is set to videoId
        // This ensures CacheDataSource uses the correct key even if URI changes
        runCatching {
            dataSpec.process(videoId, audioQualityFormat, applicationContext.isConnectionMetered())
                .buildUpon()
                .setKey(videoId)
                .build()
        }.onFailure {
            Timber.e(it, "Failed to resolve playback DataSpec for %s.", videoId)
        }.getOrThrow()
    }

    // LRU Cache (Writable) - Upstream is the Resolver
    val lruCacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)

    // Download Cache (Read-Only during playback) - Upstream is LRU Cache
    return CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(lruCacheFactory)
        .setCacheWriteDataSinkFactory(null)
        .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
}

@UnstableApi
fun MyDownloadHelper.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        
        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        runCatching {
            dataSpec.process(videoId, audioQualityFormat, appContext().isConnectionMetered())
                .buildUpon()
                .setKey(videoId)
                .build()
        }.onFailure {
            Timber.e(it, "Failed to resolve download DataSpec for %s.", videoId)
        }.getOrThrow()
    }

    // Download Cache (Writable for downloads? No, CacheWriter handles writing)
    // We expose it as a source that can read from cache if available, or resolve upstream.
    return CacheDataSource.Factory()
        .setCache(getDownloadCache(appContext()))
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)
}
//</editor-fold>

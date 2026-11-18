package app.cubic.android.cast

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.utils.NewPipeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

internal data class CastStream(
    val url: String,
    val mimeType: String,
    val candidateId: String,
    val durationMs: Long,
    val contentLength: Long,
    val title: String,
    val artist: String,
    val headers: Map<String, String> = emptyMap(),
    val requiresRelay: Boolean = false,
) {
    override fun toString() = "CastStream(candidateId=$candidateId, mimeType=$mimeType)"
}

internal object CastStreamResolver {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    // Resolve the next candidate only after the receiver rejects the previous one.
    // Cubic's playback cache, client health, tokens, media IDs and queue stay untouched.
    suspend fun tryCandidates(
        videoId: String,
        skipPlaybackCache: Boolean = false,
        tryStream: suspend (CastStream) -> Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        require(Regex("[A-Za-z0-9_-]{11}").matches(videoId))
        val clients = listOf(
            YouTubeClient.VISIONOS,
            YouTubeClient.ANDROID_VR_1_65_10,
            YouTubeClient.IOS,
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.ANDROID_MUSIC,
        )
        if (!skipPlaybackCache) {
            app.kreate.android.service.cachedPlaybackStreamForOutput(videoId)?.toString()?.toHttpUrlOrNull()
                ?.takeIf { it.isHttps && it.host.endsWith(".googlevideo.com") }
                ?.let { cached ->
                    val mime = cached.queryParameter("mime")?.substringBefore(';')
                        ?: when (cached.queryParameter("itag")) {
                            "140", "139", "141" -> "audio/mp4"
                            "249", "250", "251" -> "audio/webm"
                            else -> null
                        }
                    val profile = clients.firstOrNull {
                        it.clientName == cached.queryParameter("c") && it.clientVersion == cached.queryParameter("cver")
                    }
                    val headers = if (profile != null && profile.clientName !in setOf("ANDROID_VR", "VISIONOS")) {
                        mapOf("User-Agent" to profile.userAgent)
                    } else emptyMap()
                    if (mime in setOf("audio/mp4", "audio/webm") &&
                        cached.queryParameter("clen")?.toLongOrNull()?.let { it > 0 } == true) {
                        // The phone is already playing this exact URL. Hand its bytes to
                        // the LAN relay immediately instead of making another player API call.
                        val stream = CastStream(
                            url = cached.toString(), mimeType = requireNotNull(mime), candidateId = "playing-stream",
                            durationMs = cached.queryParameter("dur")?.toDoubleOrNull()
                                ?.takeIf { it.isFinite() && it > 0 && it <= 604_800 }?.times(1000)?.toLong() ?: 0L,
                            contentLength = cached.queryParameter("clen")?.toLongOrNull() ?: 0L,
                            title = "", artist = "", headers = headers, requiresRelay = true,
                        )
                        if (withContext(Dispatchers.Main.immediate) { tryStream(stream) }) return@withContext true
                    }
                }
        }
        val visitorData = app.cubic.android.core.network.Store.getIosVisitorData()
            .trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: Innertube.DEFAULT_VISITOR_DATA
        val deviceLocale = java.util.Locale.getDefault()
        val locale = it.fast4x.innertube.clients.YouTubeLocale(
            gl = deviceLocale.country.ifBlank { "US" },
            hl = deviceLocale.language.ifBlank { "en" },
        )
        for (client in clients) {
            currentCoroutineContext().ensureActive()
            val response = try {
                withTimeoutOrNull(8_000L) {
                    val signatureTimestamp = if (client.useSignatureTimestamp) {
                        NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
                    } else null
                    Innertube.player(
                        videoId = videoId,
                        context = client.toContext(
                            locale,
                            visitorData,
                            null,
                        ),
                        signatureTimestamp = signatureTimestamp,
                        withLogin = false,
                    )?.getOrThrow()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val status = (error as? io.ktor.client.plugins.ResponseException)?.response?.status?.value
                Timber.w("Cast resolve id=%s client=%s http=%s failure=%s",
                    videoId, client.clientName, status ?: "none", error.javaClass.simpleName)
                null
            } ?: continue
            if (response.playabilityStatus?.status != "OK") {
                Timber.w("Cast resolve id=%s client=%s status=%s",
                    videoId, client.clientName, response.playabilityStatus?.status)
                continue
            }
            val details = response.videoDetails
            if (details?.videoId != null && details.videoId != videoId) continue
            val formats = response.streamingData?.adaptiveFormats.orEmpty()
                .filter { it.isAudio && it.mimeType.substringBefore(';') in setOf("audio/mp4", "audio/webm") }
                .sortedWith(compareBy({ if (it.itagValue == 140) 0 else 1 }, { -(it.bitrateValue ?: 0) }))
                .distinctBy { it.mimeType.substringBefore(';') }
                .sortedBy { if (it.mimeType.startsWith("audio/mp4")) 0 else 1 }

            if (formats.isEmpty()) Timber.w("Cast resolve id=%s client=%s: no supported audio", videoId, client.clientName)
            for (format in formats) {
                currentCoroutineContext().ensureActive()
                val candidateId = "${client.clientName}:${client.clientVersion}:${format.itagValue}"
                val url = try {
                    NewPipeUtils.getStreamUrl(format, videoId).getOrThrow().toHttpUrlOrNull()
                        ?.takeIf { it.isHttps && it.host.endsWith(".googlevideo.com") }
                        ?.newBuilder()
                        ?.apply {
                            if (build().queryParameter("cpn") == null) {
                                addQueryParameter("cpn", UUID.randomUUID().toString().replace("-", "").take(16))
                            }
                        }?.build()?.toString()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.w("Cast decipher id=%s candidate=%s failure=%s",
                        videoId, candidateId, error.javaClass.simpleName)
                    null
                } ?: continue
                val headers = if (client.clientName in setOf("ANDROID_VR", "VISIONOS")) emptyMap()
                    else mapOf("User-Agent" to client.userAgent)
                val direct = probe(url, videoId, candidateId)
                if (!direct && !probe(url, videoId, candidateId, headers)) continue
                val stream = CastStream(
                    url = url,
                    mimeType = format.mimeType.substringBefore(';'),
                    candidateId = candidateId,
                    durationMs = details?.lengthSeconds?.toLongOrNull()?.coerceIn(0L, 604_800L)?.times(1000L) ?: 0L,
                    contentLength = format.contentLengthValue ?: url.toHttpUrlOrNull()
                        ?.queryParameter("clen")?.toLongOrNull() ?: 0L,
                    title = details?.title.orEmpty(),
                    artist = details?.author.orEmpty(),
                    headers = headers,
                    requiresRelay = !direct,
                )
                if (withContext(Dispatchers.Main.immediate) { tryStream(stream) }) return@withContext true
            }
        }
        false
    }

    private fun probe(url: String, videoId: String, candidateId: String, headers: Map<String, String> = emptyMap()): Boolean {
        val length = url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()
        if (length != null && length <= 0L) return false
        return try {
            // A first-byte response alone does not prove that a receiver can seek or
            // continue past YouTube's initial media chunk.
            listOf(0L, 1024L * 1024L).filter { length == null || it < length }.all { offset ->
                // googlevideo URLs are more reliable when the requested byte window is
                // carried in the signed URL query. Several profiles return 403 when an
                // otherwise identical HTTP Range header is used by a TV or LAN relay.
                val rangedUrl = url.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.setQueryParameter("range", "$offset-$offset")
                    ?.build()
                    ?: return false
                val request = Request.Builder().url(rangedUrl)
                    .apply { headers.forEach { (key, value) -> header(key, value) } }
                    .header("Accept-Encoding", "identity").build()
                httpClient.newCall(request).execute().use { response ->
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    val rangeStart = response.header("Content-Range")
                        ?.substringAfter("bytes ", "")?.substringBefore('-')?.toLongOrNull()
                    val valid = (response.code == 200 ||
                        response.code == 206 && (rangeStart == null || rangeStart == offset)) &&
                        (contentType.startsWith("audio/") || contentType.startsWith("application/octet-stream")) &&
                        (response.body?.byteStream()?.read() ?: -1) >= 0
                    if (!valid) Timber.w("Cast probe id=%s candidate=%s http=%d offset=%d",
                        videoId, candidateId, response.code, offset)
                    valid
                }
            }
        } catch (error: java.io.IOException) {
            Timber.w("Cast probe id=%s candidate=%s failure=%s", videoId, candidateId, error.javaClass.simpleName)
            false
        }
    }
}

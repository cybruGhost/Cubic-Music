package app.it.fast4x.rimusic.net

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.utils.NewPipeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

/** Resolves a WEB_REMIX URL with player and streaming PoTokens bound to the same visitor session. */
internal suspend fun resolveCubicWebPoTokenStream(
    videoId: String,
    httpClient: OkHttpClient
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val client = YouTubeClient.WEB_REMIX
        val visitorData = Innertube.visitorData.ifBlank { Innertube.DEFAULT_VISITOR_DATA }
        val tokens = CubicDesktopPoTokenGenerator.get(videoId, visitorData)
            ?: error("Desktop PoToken generation failed")
        System.err.println("Cubic WEB_REMIX PoTokens: player=${tokens.playerRequestPoToken.length} streaming=${tokens.streamingDataPoToken.length}")
        val signatureTimestamp = CubicDesktopCipherResolver.signatureTimestamp()
        val context = client.toContext(
            locale = Innertube.locale,
            visitorData = visitorData,
            dataSyncId = null
        )
        val response = withTimeout(12_000L) {
            Innertube.player(
                videoId = videoId,
                poToken = tokens.playerRequestPoToken,
                context = context,
                signatureTimestamp = signatureTimestamp
            )?.getOrThrow() ?: error("WEB_REMIX player returned no response")
        }
        check(response.playabilityStatus?.status == "OK") {
            "WEB_REMIX player rejected ${response.playabilityStatus?.status}: ${response.playabilityStatus?.reason.orEmpty()}"
        }
        val formats = (response.streamingData?.adaptiveFormats.orEmpty() + response.streamingData?.formats.orEmpty())
            .filter { it.isAudio && (!it.url.isNullOrBlank() || !it.signatureCipher.isNullOrBlank()) }
            .distinctBy { it.itagValue ?: it.mimeType + it.url.orEmpty() + it.signatureCipher.orEmpty() }
            .sortedWith(
                compareByDescending<PlayerResponse.StreamingData.Format> {
                    when {
                        it.mimeType.contains("opus", ignoreCase = true) -> 2
                        it.mimeType.contains("mp4a", ignoreCase = true) -> 1
                        else -> 0
                    }
                }.thenByDescending { it.bitrateValue ?: 0 }
            )
        check(formats.isNotEmpty()) { "WEB_REMIX returned no audio formats" }
        System.err.println("Cubic WEB_REMIX formats: ${formats.size}")

        for (format in formats) {
            val transformedUrl = CubicDesktopCipherResolver.resolveStreamUrl(
                directUrl = format.url,
                signatureCipher = format.signatureCipher
            )
                ?.takeIf(String::isNotBlank)
                ?: continue
            val tokenizedUrl = transformedUrl.toHttpUrlOrNull()?.newBuilder()
                ?.setQueryParameter("pot", tokens.streamingDataPoToken)
                ?.build()
                ?.toString()
            if (tokenizedUrl == null) {
                System.err.println("Cubic WEB_REMIX itag ${format.itagValue} URL parse failed")
                continue
            }
            val identifiedUrl = attachCubicPlaybackIdentity(tokenizedUrl, client, cubicWebPlaybackCpn())
            if (identifiedUrl == null) {
                System.err.println("Cubic WEB_REMIX itag ${format.itagValue} identity failed")
                continue
            }
            CubicRangeTransfer.validate(httpClient, identifiedUrl)?.let { return@withContext it }
        }
        error("WEB_REMIX PoToken URLs failed byte-range validation")
    }.onFailure { error ->
        System.err.println("Cubic WEB_REMIX PoToken resolver failed: ${error.message}")
    }.getOrNull()
}

private fun cubicWebPlaybackCpn(): String = buildString(16) {
    val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    repeat(16) { append(alphabet.random()) }
}
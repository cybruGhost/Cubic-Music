package app.it.fast4x.rimusic.net

import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.models.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal data class CubicPlaybackHeaders(
    val userAgent: String,
    val origin: String? = null,
    val referer: String? = null
)

private val cubicVisionOsClient = YouTubeClient(
    clientName = "VISIONOS",
    clientVersion = "0.1",
    api_key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
    clientId = "101",
    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
    osName = "visionOS",
    osVersion = "1.3.21O771",
    deviceMake = "Apple",
    deviceModel = "RealityDevice14,1",
    xClientName = 101,
    loginSupported = false,
    useSignatureTimestamp = false,
)

internal val cubicDesktopPlaybackClients = listOf(
    cubicVisionOsClient,
    YouTubeClient.ANDROID_VR_NO_AUTH,
    YouTubeClient.ANDROID_VR_1_61_48,
    YouTubeClient.ANDROID_VR_1_43_32,
    YouTubeClient.MOBILE,
    YouTubeClient.IOS,
    YouTubeClient.ANDROID_MUSIC,
    YouTubeClient.IPADOS,
    YouTubeClient.ANDROID_CREATOR,
    YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
    YouTubeClient.TVHTML5,
    YouTubeClient.WEB_REMIX,
    YouTubeClient.WEB_CREATOR,
    YouTubeClient.WEB
)

internal fun attachCubicPlaybackIdentity(url: String, client: YouTubeClient, cpn: String): String? {
    val parsed = url.toHttpUrlOrNull() ?: return null
    val contentLength = parsed.queryParameter("clen")?.toLongOrNull()?.takeIf { it > 0L }
    return parsed.newBuilder()
        .setQueryParameter("c", client.clientName)
        .setQueryParameter("cver", client.clientVersion)
        .setQueryParameter("cpn", cpn)
        .apply { contentLength?.let { setQueryParameter("range", "0-${it - 1L}") } }
        .build()
        .toString()
}

internal fun cubicPlaybackHeaders(url: String): CubicPlaybackHeaders {
    val httpUrl = url.toHttpUrlOrNull()
    val clientName = httpUrl?.queryParameter("c")
    val clientVersion = httpUrl?.queryParameter("cver")
    val client = cubicDesktopPlaybackClients.firstOrNull {
        it.clientName == clientName && it.clientVersion == clientVersion
    } ?: cubicDesktopPlaybackClients.firstOrNull { it.clientName == clientName }

    val name = client?.clientName.orEmpty()
    return CubicPlaybackHeaders(
        userAgent = client?.userAgent ?: Context.USER_AGENT_WEB,
        origin = when {
            name.startsWith("WEB") -> "https://music.youtube.com"
            name.startsWith("TVHTML5") -> "https://www.youtube.com"
            else -> null
        },
        referer = when {
            name.startsWith("WEB") -> "https://music.youtube.com/"
            name.startsWith("TVHTML5") -> "https://www.youtube.com/tv"
            else -> null
        }
    )
}

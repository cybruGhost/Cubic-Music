package it.fast4x.innertube.models

import io.ktor.http.headers
import io.ktor.http.parameters
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.clients.YouTubeLocale
import it.fast4x.innertube.utils.LocalePreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
    private val request: Request = Request(),
    val user: User? = User()
) {

    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val platform: String? = null,
        val hl: String? = "en",
        val gl: String? = "US",
        val visitorData: String? = null, // = Innertube.DEFAULT_VISITOR_DATA,
        val androidSdkVersion: Int? = null,
        val userAgent: String? = null,
        val referer: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val acceptHeader: String? = null,
        val xClientName: Int? = null,
        @Transient
        val api_key: String? = null
    ) {
        fun toContext(locale: YouTubeLocale, visitorData: String) = Context(
            client = Client(
                clientName = clientName,
                clientVersion = clientVersion,
                gl = locale.gl,
                hl = locale.hl,
                visitorData = visitorData,
                androidSdkVersion = androidSdkVersion,
                userAgent = userAgent,
                referer = referer,
                deviceMake = deviceMake,
                deviceModel = deviceModel,
                osName = osName,
                osVersion = osVersion,
                acceptHeader = acceptHeader,
                api_key = api_key,
                platform = platform,
            )
        )
    }

    @Serializable
    data class ThirdParty(
        val embedUrl: String,
    )

    @Serializable
    data class User(
        val lockedSafetyMode: Boolean = false
    )

    @Serializable
    data class Request(
        val internalExperimentFlags: Array<String> = emptyArray(),
        val useSsl: Boolean = true,
    )

    fun apply() {
        client.userAgent

        headers {
            client.referer?.let { append("Referer", it) }
            append("X-Youtube-Bootstrap-Logged-In", "false")
            append("X-YouTube-Client-Name", client.clientName)
            append("X-YouTube-Client-Version", client.clientVersion)
            client.api_key?.let { append("X-Goog-Api-Key", it) }
        }

        parameters {
            client.api_key?.let { append("key", it) }
        }
    }



    companion object {

        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.3"
        private const val USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36"
        private const val USER_AGENT_ANDROID_MUSIC = "com.google.android.youtube/19.29.1  (Linux; U; Android 11) gzip"
        private const val USER_AGENT_PLAYSTATION = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        private const val USER_AGENT_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
        private const val USER_AGENT_IOS = "com.google.ios.youtube/20.03.02 (iPhone16,2; U; CPU iOS 18_2_1 like Mac OS X;)"

        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"
        private const val REFERER_YOUTUBE = "https://www.youtube.com/"

        val DefaultWeb = Context(
            client = Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20250407.01.00",
                platform = "DESKTOP",
                //clientVersion = "1.20220606.03.00",
                //clientVersion = "1.20230731.00.00",
                userAgent = USER_AGENT_WEB,
                referer = REFERER_YOUTUBE_MUSIC,
                visitorData = Innertube.visitorData,
                api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
                xClientName = 67
            )
        )

        val hl = LocalePreferences.preference?.hl

        val DefaultWebWithLocale = DefaultWeb.copy(
            client = DefaultWeb.client.copy(hl = hl)
        )

        val DefaultIOS = Context(
            client = Client(
                clientName = "IOS",
                clientVersion = "20.03.02",
                deviceMake = "Apple",
                deviceModel = "iPhone16,2",
                osName = "iOS",
                osVersion = "18.2.1.22C161",
                acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                userAgent = USER_AGENT_IOS,
                api_key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
                xClientName = 5
            )
        )
    }
}

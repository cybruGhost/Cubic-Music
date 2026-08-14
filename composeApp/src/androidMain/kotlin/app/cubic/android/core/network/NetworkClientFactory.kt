package app.cubic.android.core.network
 
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.net.URI
import java.net.Proxy
import java.util.concurrent.TimeUnit

object NetworkClientFactory {
    private const val CHROME_WINDOWS_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.3"

    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    private var ktorClient: HttpClient? = null

    @Volatile
    private var ktorCachelessClient: HttpClient? = null

    @Volatile
    private var ktorTranslatorClient: HttpClient? = null

    private fun String.redactedUrl(): String =
        runCatching {
            val parsed = URI(this)
            "${parsed.scheme}://${parsed.host}${parsed.rawPath.orEmpty()}"
        }.getOrDefault("<stream-url>")

    fun configure(
        proxy: Proxy?,
        cacheDir: File?,
        connectTimeout: Long = 30L,
        readTimeout: Long = 30L
    ) {
        synchronized(this) {
            val currentClient = client
            if (currentClient == null) {
                val builder = OkHttpClient.Builder()
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .proxy(proxy)
                
                if (cacheDir != null) {
                    builder.cache(Cache(cacheDir, 100L * 1024 * 1024)) // 100MB cache
                }
                
                client = builder.build()
            } else {
                client = currentClient.newBuilder()
                    .proxy(proxy)
                    .build()
            }
            // Reset Ktor clients to force reconfiguration with new proxy/settings
            ktorClient = null
            ktorCachelessClient = null
            ktorTranslatorClient = null
        }
    }

    private fun buildDefaultClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .build()
    }

    fun getClient(): OkHttpClient {
        return client ?: synchronized(this) {
            client ?: buildDefaultClient().also { client = it }
        }
    }

    fun getCachelessClient(): OkHttpClient {
        return getClient().newBuilder()
            .cache(null)
            .build()
    }

    fun getClientWithTimeout(connect: Long, read: Long): OkHttpClient {
        return getClient().newBuilder()
            .connectTimeout(connect, TimeUnit.SECONDS)
            .readTimeout(read, TimeUnit.SECONDS)
            .build()
    }

    fun getTranslatorClient(): OkHttpClient {
        return getClient().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        CHROME_WINDOWS_USER_AGENT
                    )
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun getTranslatorKtorClient(): HttpClient {
        return ktorTranslatorClient ?: synchronized(this) {
            ktorTranslatorClient ?: HttpClient(OkHttp) {
                engine {
                    preconfigured = getTranslatorClient()
                }
            }.also { ktorTranslatorClient = it }
        }
    }

    fun getKtorClient(cacheless: Boolean = true): HttpClient {
        return if (cacheless) {
            ktorCachelessClient ?: synchronized(this) {
                ktorCachelessClient ?: HttpClient(OkHttp) {
                    engine {
                        preconfigured = getCachelessClient()
                    }
                }.also { ktorCachelessClient = it }
            }
        } else {
            ktorClient ?: synchronized(this) {
                ktorClient ?: HttpClient(OkHttp) {
                    engine {
                        preconfigured = getClient()
                    }
                }.also { ktorClient = it }
            }
        }
    }

    fun validateStreamUrl(
        streamUrl: String,
        expectedContentTypePrefix: String? = null,
        userAgent: String = CHROME_WINDOWS_USER_AGENT,
        origin: String? = null,
        referer: String? = null
    ): Boolean {
        fun Request.Builder.applyPlaybackHeaders(): Request.Builder = apply {
            header("User-Agent", userAgent)
            header("Accept", "*/*")
            header("Accept-Encoding", "identity")
            origin?.let { header("Origin", it) }
            referer?.let { header("Referer", it) }
        }

        fun responseIsUsable(response: okhttp3.Response): Boolean {
            if (response.code == 416) return true
            if (response.code !in 200..399) return false

            val contentType = response.header("Content-Type").orEmpty()
            val contentTypeMatches = expectedContentTypePrefix.isNullOrBlank() ||
                contentType.isBlank() ||
                contentType.startsWith(expectedContentTypePrefix, ignoreCase = true) ||
                contentType.startsWith("application/octet-stream", ignoreCase = true) ||
                (expectedContentTypePrefix == "audio/" && contentType.contains("audio", ignoreCase = true)) ||
                (expectedContentTypePrefix == "video/" && contentType.contains("video", ignoreCase = true))
            return contentTypeMatches
        }

        fun queryParameter(name: String): String =
            runCatching {
                URI(streamUrl).rawQuery
                    ?.split("&")
                    ?.firstOrNull { it.substringBefore("=").equals(name, ignoreCase = true) }
                    ?.substringAfter("=", "")
                    .orEmpty()
            }.getOrDefault("")

        fun probeRanges(): List<String> {
            val clientName = queryParameter("c").uppercase()
            return if (clientName.startsWith("WEB")) {
                listOf("bytes=0-0", "bytes=262144-262145", "bytes=1048576-1048577")
            } else {
                listOf("bytes=0-0")
            }
        }

        return try {
            val client = getClientWithTimeout(3, 4)
            for (range in probeRanges()) {
                val rangeRequest = Request.Builder()
                    .url(streamUrl)
                    .get()
                    .header("Range", range)
                    .applyPlaybackHeaders()
                    .build()

                val isUsable = client.newCall(rangeRequest).execute().use { response ->
                    val usable = responseIsUsable(response)
                    if (!usable) {
                        Timber.w(
                            "Stream range validation failed code=%d contentType=%s expected=%s range=%s for URL: %s",
                            response.code,
                            response.header("Content-Type").orEmpty(),
                            expectedContentTypePrefix,
                            range,
                            streamUrl.redactedUrl()
                        )
                    }
                    usable
                }
                if (!isUsable) return false
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "validateStreamUrl exception for URL: %s", streamUrl.redactedUrl())
            false
        }
    }

    fun canReachYouTube(): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://www.youtube.com/generate_204")
                .head()
                .header("User-Agent", CHROME_WINDOWS_USER_AGENT)
                .build()

            getClientWithTimeout(3, 3).newCall(request).execute().use { response ->
                response.code in 200..399 || response.code == 405
            }
        } catch (e: Exception) {
            Timber.w(e, "YouTube reachability check failed")
            false
        }
    }
}

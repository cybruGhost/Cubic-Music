package app.cubic.android.core.network
 
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.net.Proxy
import java.util.concurrent.TimeUnit

object NetworkClientFactory {
    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    private var ktorClient: HttpClient? = null

    @Volatile
    private var ktorCachelessClient: HttpClient? = null

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
}
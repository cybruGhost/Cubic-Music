package app.cubic.android.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Session-only relay for links the phone can read but the Cast receiver cannot. */
internal class CastStreamRelay private constructor(private val host: String) : NanoHTTPD(host, 0) {
    private val streams = ConcurrentHashMap<String, CastStream>()
    private val textTracks = ConcurrentHashMap<String, String>()
    private val textTrackTokens = ConcurrentHashMap<String, String>()
    private val textTrackOrder = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private val registrationOrder = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private val openBodies = ConcurrentHashMap.newKeySet<java.io.InputStream>()
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS).build()

    fun add(stream: CastStream, currentUrl: String?): CastStream {
        val length = stream.contentLength.takeIf { it > 0L }
            ?: stream.url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()
        require(length != null && length > 0L) { "Cast relay requires a known media length" }
        // The queue only needs its current track and its immediate successor.
        if (streams.size >= 8) {
            val currentToken = currentUrl?.substringAfterLast('/')
            registrationOrder.firstOrNull { it != currentToken }?.let { token ->
                streams.remove(token)
                registrationOrder.remove(token)
            }
        }
        val token = UUID.randomUUID().toString()
        streams[token] = stream
        registrationOrder.add(token)
        return stream.copy(url = "http://$host:$listeningPort/$token", candidateId = stream.candidateId + ":relay", requiresRelay = false)
    }

    @Synchronized
    fun addLyrics(mediaId: String, webVtt: String): String {
        val token = textTrackTokens[mediaId] ?: run {
            while (textTracks.size >= 12) {
                val expiredMediaId = textTrackOrder.poll() ?: break
                textTrackTokens.remove(expiredMediaId)?.let(textTracks::remove)
            }
            val created = "lyrics-${UUID.randomUUID()}.vtt"
            textTrackTokens[mediaId] = created
            textTrackOrder.add(mediaId)
            created
        }
        textTracks[token] = webVtt
        return "http://$host:$listeningPort/$token"
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) return cors(newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, ""))
        if (session.method !in setOf(Method.GET, Method.HEAD)) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }
        val token = session.uri.removePrefix("/")
        textTracks[token]?.let { webVtt ->
            val bytes = webVtt.toByteArray(Charsets.UTF_8)
            return cors(
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/vtt; charset=utf-8",
                    ByteArrayInputStream(bytes),
                    bytes.size.toLong(),
                )
            ).apply {
                addHeader("Cache-Control", "no-store")
            }
        }
        val stream = streams[token]
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        val total = stream.contentLength.takeIf { it > 0L }
            ?: stream.url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "No media length")
        val range = parseRange(session.headers["range"], total)
            ?: return cors(newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid range")).apply {
                addHeader("Content-Range", "bytes */$total")
            }
        val (start, end) = range
        val status = if (session.headers["range"] != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        if (openBodies.size >= 6) return cors(newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Busy"))
        var opened: java.io.InputStream? = null
        try {
            // Cast needs one continuous body. The download data source deliberately reopens
            // 512 KiB ranges, which can make a receiver treat the first chunk as a finished song.
            val upstreamUrl = stream.url.toHttpUrlOrNull()
                ?.newBuilder()
                ?.setQueryParameter("range", "$start-$end")
                ?.build()
                ?: return cors(newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Invalid media URL"))
            val source = OkHttpDataSource.Factory(client).createDataSource()
            val spec = DataSpec.Builder()
                .setUri(Uri.parse(upstreamUrl.toString()))
                // The byte window is already in the googlevideo URL. A second HTTP
                // Range header is rejected by some clients with 403.
                .setPosition(0L)
                .setHttpRequestHeaders(stream.headers + ("Accept-Encoding" to "identity"))
                .build()
            val upstream = DataSourceInputStream(source, spec)
            val input = object : java.io.FilterInputStream(upstream) {
                override fun close() { try { super.close() } finally { openBodies.remove(this) } }
            }
            opened = input
            // HEAD must not start a media download, but still advertises the full representation.
            if (session.method != Method.HEAD) upstream.open()
            openBodies.add(input)
            return cors(newFixedLengthResponse(status, stream.mimeType, input, end - start + 1)).apply {
                addHeader("Accept-Ranges", "bytes")
                addHeader("Cache-Control", "no-store")
                if (status == Response.Status.PARTIAL_CONTENT) addHeader("Content-Range", "bytes $start-$end/$total")
            }
        } catch (_: Exception) {
            runCatching { opened?.close() }
            return cors(newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Media unavailable"))
        }
    }

    override fun stop() {
        super.stop()
        openBodies.toList().forEach { runCatching { it.close() } }
        streams.clear()
        textTracks.clear()
        textTrackTokens.clear()
        textTrackOrder.clear()
        registrationOrder.clear()
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
    }

    private fun cors(response: Response): Response = response.apply {
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        addHeader("Access-Control-Allow-Headers", "Range, Content-Type")
        addHeader("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
        addHeader("Access-Control-Allow-Private-Network", "true")
    }

    companion object {
        fun create(context: Context): CastStreamRelay {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = connectivity.allNetworks.firstOrNull {
                connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } ?: throw IOException("Cast relay needs Wi-Fi")
            val address = connectivity.getLinkProperties(wifi)?.linkAddresses?.map { it.address }
                ?.filterIsInstance<Inet4Address>()?.firstOrNull { !it.isLoopbackAddress }?.hostAddress
                ?: throw IOException("No local Wi-Fi address")
            return CastStreamRelay(address).apply { start(SOCKET_READ_TIMEOUT, true) }
        }

        internal fun parseRange(header: String?, total: Long): Pair<Long, Long>? {
            if (total <= 0) return null
            if (header == null) return 0L to total - 1L
            val match = Regex("bytes=(\\d*)-(\\d*)", RegexOption.IGNORE_CASE).matchEntire(header.trim()) ?: return null
            val first = match.groupValues[1]
            val last = match.groupValues[2]
            if (first.isEmpty()) {
                val suffix = last.toLongOrNull()?.takeIf { it > 0 } ?: return null
                return (total - suffix).coerceAtLeast(0L) to total - 1L
            }
            val start = first.toLongOrNull()?.takeIf { it in 0 until total } ?: return null
            val end = if (last.isEmpty()) total - 1 else last.toLongOrNull() ?: return null
            if (end < start) return null
            return start to end.coerceAtMost(total - 1)
        }
    }
}

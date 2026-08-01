package app.it.fast4x.rimusic.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.OutputStream
import kotlin.math.min

/** Uses Android's 512 KiB bounded transfer and client-identity request headers. */
internal object CubicRangeTransfer {
    private const val CHUNK_LENGTH = 512L * 1024L
    private val contentRangePattern = Regex("bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)", RegexOption.IGNORE_CASE)

    fun validate(client: OkHttpClient, url: String): String? = runCatching {
        val headers = cubicPlaybackHeaders(url)
        val ranges = buildList {
            add("bytes=0-${CHUNK_LENGTH - 1L}")
            add("bytes=1048576-1048577")
        }.distinct()
        ranges.forEach { range ->
            val request = Request.Builder()
                .url(url)
                .header("Range", range)
                .applyPlaybackHeaders(headers)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty()
                val usableType = contentType.isBlank() ||
                    contentType.startsWith("audio/", ignoreCase = true) ||
                    contentType.startsWith("application/octet-stream", ignoreCase = true) ||
                    contentType.contains("audio", ignoreCase = true)
                check((response.code in 200..399 || response.code == 416) && usableType) {
                    "Stream validation failed with HTTP ${response.code} for $range"
                }
            }
        }
        url
    }.onFailure { error ->
        System.err.println("Cubic stream validation rejected ${url.toHttpUrlOrNull()?.queryParameter("c").orEmpty()}: ${error.message}")
    }.getOrNull()

    fun copy(
        client: OkHttpClient,
        url: String,
        output: OutputStream,
        shouldContinue: () -> Boolean = { true },
        onProgress: (copied: Long, total: Long) -> Unit = { _, _ -> }
    ): Long {
        var copied = 0L
        var total = url.toHttpUrlOrNull()
            ?.queryParameter("clen")
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: -1L
        val buffer = ByteArray(64 * 1024)
        val headers = cubicPlaybackHeaders(url)

        while (shouldContinue() && (total < 0L || copied < total)) {
            val end = if (total > 0L) min(copied + CHUNK_LENGTH - 1L, total - 1L)
            else copied + CHUNK_LENGTH - 1L
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=$copied-$end")
                .applyPlaybackHeaders(headers)
                .get()
                .build()

            var emptyChunk = false
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Stream request failed with HTTP ${response.code} at bytes $copied-$end"
                }
                val body = response.body
                val parsedRange = response.header("Content-Range")?.let(contentRangePattern::find)
                parsedRange?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { returnedStart ->
                    check(returnedStart == copied) { "The stream returned an unexpected starting byte." }
                }
                parsedRange?.groupValues?.getOrNull(3)?.takeUnless { it == "*" }?.toLongOrNull()?.let {
                    total = it
                }
                if (total < 0L && response.code == 200) total = body.contentLength()

                var chunkCopied = 0L
                body.byteStream().use { input ->
                    while (shouldContinue()) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        chunkCopied += count
                        onProgress(copied, total)
                    }
                }
                emptyChunk = chunkCopied == 0L
                if (response.code == 200) total = copied
            }
            if (emptyChunk) break
        }
        output.flush()
        return copied
    }

    private fun Request.Builder.applyPlaybackHeaders(headers: CubicPlaybackHeaders): Request.Builder = apply {
        header("User-Agent", headers.userAgent)
        header("Accept", "*/*")
        header("Accept-Encoding", "identity")
        headers.origin?.let { header("Origin", it) }
        headers.referer?.let { header("Referer", it) }
    }
}

package app.it.fast4x.rimusic.net

import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Desktop YouTube cipher executor backed by Metrolist's validated player config table. */
internal object CubicDesktopCipherResolver {
    private const val CONFIG_URL =
        "https://raw.githubusercontent.com/MetrolistGroup/Metrolist/main/app/src/main/assets/player_configs.json"
    private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private val hashRegex = Regex("/s/player/([A-Za-z0-9_-]+)/")
    private val configHashRegex = Regex("^[a-f0-9]{8}$")
    private val signatureExpressionRegex = Regex("""^[A-Za-z0-9_]{1,8}\(\d+,\d+,INPUT\)$""")
    private val nClassRegex = Regex("""^[A-Za-z0-9_]{1,8}$""")
    private val lock = Mutex()
    private var runtime: CipherRuntime? = null
    private val controlClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    suspend fun signatureTimestamp(): Int? =
        runCatching { withTimeout(45_000L) { getRuntime().signatureTimestamp } }
            .onFailure { System.err.println("Cubic cipher preparation failed: ${it.message}") }
            .getOrNull()

    suspend fun resolveStreamUrl(
        directUrl: String?,
        signatureCipher: String?
    ): String? = runCatching {
        withTimeout(30_000L) {
            val activeRuntime = getRuntime()
            val signedUrl = directUrl?.takeIf(String::isNotBlank) ?: run {
                val params = parseQuery(signatureCipher ?: error("Missing signature cipher"))
                val obfuscatedSignature = params["s"] ?: error("Missing cipher signature")
                val signatureParameter = params["sp"] ?: "signature"
                val baseUrl = params["url"] ?: error("Missing cipher URL")
                val signature = activeRuntime.decipherSignature(obfuscatedSignature)
                baseUrl.toHttpUrlOrNull()?.newBuilder()
                    ?.setQueryParameter(signatureParameter, signature)
                    ?.build()
                    ?.toString()
                    ?: error("Invalid cipher URL")
            }

            val parsed = signedUrl.toHttpUrlOrNull() ?: error("Invalid stream URL")
            val nValue = parsed.queryParameter("n") ?: return@withTimeout signedUrl
            val transformedN = activeRuntime.transformN(nValue)
            parsed.newBuilder().setQueryParameter("n", transformedN).build().toString()
        }
    }.onFailure { System.err.println("Cubic desktop cipher failed: ${it.message}") }.getOrNull()

    private suspend fun getRuntime(): CipherRuntime = lock.withLock {
        runtime?.let { return@withLock it }
        val hash = fetchPlayerHash()
        runtime?.close()
        runtime = null

        val config = fetchPlayerConfig(hash)
        val playerJs = getText(
            "https://www.youtube.com/s/player/$hash/player_ias.vflset/en_GB/base.js"
        )
        CipherRuntime.create(hash, config, playerJs).also { runtime = it }
    }

    private fun fetchPlayerHash(): String {
        val iframeApi = getText(IFRAME_API_URL).replace("\\/", "/")
        return hashRegex.find(iframeApi)?.groupValues?.get(1)
            ?: error("Could not find YouTube player hash")
    }

    private fun fetchPlayerConfig(hash: String): PlayerConfig {
        val root = CIPHER_JSON.parseToJsonElement(getText(CONFIG_URL)).jsonObject
        check(root["schemaVersion"]?.jsonPrimitive?.content?.toIntOrNull() == 1) {
            "Unsupported cipher config schema"
        }
        val players = root["players"]?.jsonObject ?: error("Cipher config has no players")
        val entry = players[hash]?.jsonObject ?: players.values
            .mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .firstOrNull { candidate ->
                candidate["aliases"]?.jsonArray?.any { it.jsonPrimitive.content == hash } == true
            }
            ?: error("No validated cipher config for player $hash")
        check(configHashRegex.matches(hash)) { "Invalid player hash" }
        val signatureExpression = entry["sig"]?.jsonPrimitive?.content ?: error("Missing signature expression")
        val nClass = entry["nClass"]?.jsonPrimitive?.content ?: error("Missing n-transform class")
        val signatureTimestamp = entry["sts"]?.jsonPrimitive?.content?.toIntOrNull()
            ?.takeIf { it > 0 } ?: error("Invalid signature timestamp")
        check(signatureExpressionRegex.matches(signatureExpression)) { "Unsafe signature expression" }
        check(nClassRegex.matches(nClass)) { "Unsafe n-transform class" }
        return PlayerConfig(signatureExpression, nClass, signatureTimestamp)
    }

    private fun getText(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
        return controlClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $url" }
            response.body.string().takeIf(String::isNotBlank) ?: error("Empty response from $url")
        }
    }

    private fun parseQuery(query: String): Map<String, String> = buildMap {
        query.split('&').forEach { pair ->
            val separator = pair.indexOf('=')
            if (separator > 0) {
                put(
                    URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
                )
            }
        }
    }

    private data class PlayerConfig(
        val signatureExpression: String,
        val nClass: String,
        val signatureTimestamp: Int
    )

    private class CipherRuntime private constructor(
        val playerHash: String,
        val signatureTimestamp: Int,
        private val directory: Path,
        private val webView: WebView,
        private val engine: WebEngine
    ) {
        suspend fun decipherSignature(signature: String): String = evaluateString(
            "window._cubicSignature(${CIPHER_JSON.encodeToString(signature)})"
        )

        suspend fun transformN(value: String): String = evaluateString(
            "window._cubicN(${CIPHER_JSON.encodeToString(value)})"
        )

        private suspend fun evaluateString(script: String): String = runOnCipherFx {
            engine.executeScript(script)?.toString()?.takeIf(String::isNotBlank)
                ?: error("Cipher function returned no value")
        }

        fun close() {
            Platform.runLater { runCatching { engine.load("about:blank") } }
            runCatching {
                directory.toFile().walkBottomUp().forEach { file -> file.delete() }
            }
        }

        companion object {
            suspend fun create(hash: String, config: PlayerConfig, playerJs: String): CipherRuntime {
                ensureCipherFxStarted()
                val directory = Files.createTempDirectory("cubic-cipher-")
                val exportCode = buildString {
                    append(";window._cubicSignature=function(INPUT){return ")
                    append(config.signatureExpression)
                    append(";};")
                    append("window._cubicN=function(INPUT){return (function(n){try{var u=new g.")
                    append(config.nClass)
                    append("('https://x.googlevideo.com/videoplayback?n='+n,true);")
                    append("var t=u.get('n');return(t&&t!==n)?t:n;}catch(e){return n;}})(INPUT);};")
                }
                val closure = "})(_yt_player);"
                val injectionIndex = playerJs.lastIndexOf(closure)
                check(injectionIndex >= 0) { "Player JS export point was not found" }
                val modifiedJs = playerJs.substring(0, injectionIndex) + exportCode + playerJs.substring(injectionIndex)
                Files.writeString(directory.resolve("player.js"), modifiedJs, StandardCharsets.UTF_8)
                Files.writeString(
                    directory.resolve("index.html"),
                    """<!doctype html><html><head><meta charset="utf-8"><script src="player.js"></script></head><body></body></html>""",
                    StandardCharsets.UTF_8
                )

                val ready = CompletableDeferred<Unit>()
                val pair = runOnCipherFx {
                    val view = WebView()
                    val webEngine = view.engine
                    webEngine.isJavaScriptEnabled = true
                    webEngine.userAgent = USER_AGENT
                    webEngine.loadWorker.stateProperty().addListener { _, _, state ->
                        when (state) {
                            Worker.State.SUCCEEDED -> runCatching {
                                check(webEngine.executeScript("typeof window._cubicSignature") == "function") {
                                    "Signature function was not exported"
                                }
                                check(webEngine.executeScript("typeof window._cubicN") == "function") {
                                    "N-transform function was not exported"
                                }
                            }.fold(ready::complete, ready::completeExceptionally)
                            Worker.State.FAILED, Worker.State.CANCELLED ->
                                ready.completeExceptionally(IllegalStateException("Cipher WebView failed to load"))
                            else -> Unit
                        }
                    }
                    webEngine.load(directory.resolve("index.html").toUri().toString())
                    view to webEngine
                }
                withTimeout(30_000L) { ready.await() }
                return CipherRuntime(hash, config.signatureTimestamp, directory, pair.first, pair.second)
            }
        }
    }
}

private val CIPHER_JSON = Json { ignoreUnknownKeys = true }
private val cipherFxStarted = AtomicBoolean(false)

private fun ensureCipherFxStarted() {
    if (!cipherFxStarted.compareAndSet(false, true)) return
    val latch = CountDownLatch(1)
    try {
        Platform.startup {
            Platform.setImplicitExit(false)
            latch.countDown()
        }
    } catch (_: IllegalStateException) {
        latch.countDown()
    }
    latch.await()
}

private suspend fun <T> runOnCipherFx(block: () -> T): T = suspendCancellableCoroutine { continuation ->
    Platform.runLater {
        runCatching(block).fold(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )
    }
}
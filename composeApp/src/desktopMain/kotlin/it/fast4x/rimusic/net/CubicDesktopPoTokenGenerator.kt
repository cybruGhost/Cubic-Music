package app.it.fast4x.rimusic.net

import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import netscape.javascript.JSObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class CubicDesktopPoTokens(
    val playerRequestPoToken: String,
    val streamingDataPoToken: String
)

internal object CubicDesktopPoTokenGenerator {
    private const val MIN_TOKEN_LENGTH = 100
    private val lock = Mutex()
    private var runtime: CubicDesktopPoTokenRuntime? = null
    private var sessionId: String? = null
    private var streamingToken: String? = null

    suspend fun get(videoId: String, visitorData: String): CubicDesktopPoTokens? = runCatching {
        withTimeout(20_000L) {
            val (activeRuntime, activeStreamingToken) = lock.withLock {
                val current = runtime
                if (current == null || current.isExpired || sessionId != visitorData) {
                    current?.close()
                    val replacement = CubicDesktopPoTokenRuntime.create()
                    val firstToken = replacement.generate(visitorData)
                    check(firstToken.length >= MIN_TOKEN_LENGTH) { "Streaming PoToken was undersized" }
                    runtime = replacement
                    sessionId = visitorData
                    streamingToken = firstToken
                }
                runtime!! to streamingToken!!
            }
            val playerToken = activeRuntime.generate(videoId)
            check(playerToken.length >= MIN_TOKEN_LENGTH) { "Player PoToken was undersized" }
            CubicDesktopPoTokens(playerToken, activeStreamingToken)
        }
    }.onFailure { error ->
        System.err.println("Cubic desktop PoToken generation failed: ${error.message}")
        lock.withLock {
            runtime?.close()
            runtime = null
            sessionId = null
            streamingToken = null
        }
    }.getOrNull()
}

private class CubicDesktopPoTokenRuntime private constructor(
    private val html: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder().build()
    private val ready = CompletableDeferred<Unit>()
    private val tokenRequests = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private lateinit var engine: WebEngine
    private lateinit var webView: WebView
    private val bridge = Bridge()
    @Volatile private var expiresAt: Instant? = null

    val isExpired: Boolean
        get() = expiresAt?.let { !Instant.now().isBefore(it) } ?: true

    suspend fun start() {
        runOnFx {
            webView = WebView()
            engine = webView.engine
            engine.isJavaScriptEnabled = true
            engine.userAgent = USER_AGENT
            engine.loadWorker.stateProperty().addListener { _, _, state ->
                when (state) {
                    Worker.State.SUCCEEDED -> {
                        val window = engine.executeScript("window") as JSObject
                        window.setMember(JS_BRIDGE, bridge)
                        scope.launch { downloadAndRunBotGuard() }
                    }
                    Worker.State.FAILED, Worker.State.CANCELLED ->
                        failInitialization(IllegalStateException("BotGuard WebView failed to load"))
                    else -> Unit
                }
            }
            val page = html.replaceFirst("<head>", "<head><base href=\"https://www.youtube.com/\">")
            engine.loadContent(page, "text/html")
        }
        ready.await()
    }

    suspend fun generate(identifier: String): String {
        ready.await()
        val result = CompletableDeferred<String>()
        tokenRequests[identifier] = result
        val identifierJson = POT_JSON.encodeToString(identifier)
        val identifierBytes = newUint8Array(identifier.toByteArray())
        runOnFx {
            engine.executeScript(
                """try {
                    var identifier = $identifierJson;
                    var u8Identifier = $identifierBytes;
                    obtainPoToken(u8Identifier).then(function(poTokenU8) {
                        $JS_BRIDGE.onObtainPoTokenResult(identifier, Array.from(poTokenU8).join(','));
                    }).catch(function(error) {
                        $JS_BRIDGE.onObtainPoTokenError(identifier, error + '\n' + (error.stack || ''));
                    });
                } catch (error) {
                    $JS_BRIDGE.onObtainPoTokenError(identifier, error + '\n' + (error.stack || ''));
                }"""
            )
        }
        return withTimeout(10_000L) { result.await() }
    }

    private suspend fun downloadAndRunBotGuard() {
        runCatching {
            val challengeBody = post(
                "https://www.youtube.com/api/jnn/v1/Create",
                "[ \"$REQUEST_KEY\" ]"
            )
            val challengeData = parseChallengeData(challengeBody)
            runOnFx {
                engine.executeScript(
                    """try {
                        var data = $challengeData;
                        runBotGuard(data).then(function(result) {
                            window.webPoSignalOutput = result.webPoSignalOutput;
                            $JS_BRIDGE.onRunBotguardResult(result.botguardResponse);
                        }).catch(function(error) {
                            $JS_BRIDGE.onJsInitializationError(error + '\n' + (error.stack || ''));
                        });
                    } catch (error) {
                        $JS_BRIDGE.onJsInitializationError(error + '\n' + (error.stack || ''));
                    }"""
                )
            }
        }.onFailure(::failInitialization)
    }

    private suspend fun finishBotGuard(botguardResponse: String) {
        runCatching {
            val responseBody = post(
                "https://www.youtube.com/api/jnn/v1/GenerateIT",
                "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]"
            )
            val (integrityToken, expirationSeconds) = parseIntegrityTokenData(responseBody)
            expiresAt = Instant.now().plusSeconds((expirationSeconds - 600L).coerceAtLeast(60L))
            runOnFx {
                engine.executeScript(
                    """try {
                        window.integrityToken = $integrityToken;
                        createPoTokenMinter(window.webPoSignalOutput, window.integrityToken).then(function() {
                            $JS_BRIDGE.onMinterCreated();
                        }).catch(function(error) {
                            $JS_BRIDGE.onJsInitializationError(error + '\n' + (error.stack || ''));
                        });
                    } catch (error) {
                        $JS_BRIDGE.onJsInitializationError(error + '\n' + (error.stack || ''));
                    }"""
                )
            }
        }.onFailure(::failInitialization)
    }

    private suspend fun post(url: String, body: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .post(body.toRequestBody("application/json+protobuf".toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "BotGuard HTTP ${response.code}" }
            response.body.string()
        }
    }

    private fun failInitialization(error: Throwable) {
        ready.completeExceptionally(error)
        tokenRequests.values.forEach { it.completeExceptionally(error) }
        tokenRequests.clear()
    }

    fun close() {
        scope.cancel()
        tokenRequests.values.forEach { it.cancel() }
        tokenRequests.clear()
        if (::engine.isInitialized) {
            Platform.runLater { runCatching { engine.load("about:blank") } }
        }
    }

    inner class Bridge {
        fun onRunBotguardResult(response: String) {
            scope.launch { finishBotGuard(response) }
        }

        fun onJsInitializationError(error: String) {
            failInitialization(IllegalStateException(error))
        }

        fun onMinterCreated() {
            ready.complete(Unit)
        }

        fun onObtainPoTokenResult(identifier: String, bytes: String) {
            tokenRequests.remove(identifier)?.complete(u8ToBase64(bytes))
        }

        fun onObtainPoTokenError(identifier: String, error: String) {
            tokenRequests.remove(identifier)?.completeExceptionally(IllegalStateException(error))
        }
    }

    companion object {
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val JS_BRIDGE = "CubicPoTokenBridge"
        private val fxStarted = AtomicBoolean(false)

        suspend fun create(): CubicDesktopPoTokenRuntime {
            ensureFxStarted()
            val html = CubicDesktopPoTokenRuntime::class.java
                .getResourceAsStream("/po_token.html")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Missing desktop po_token.html")
            return CubicDesktopPoTokenRuntime(html).also { it.start() }
        }

        private fun ensureFxStarted() {
            if (!fxStarted.compareAndSet(false, true)) return
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
    }
}

private suspend fun <T> runOnFx(block: () -> T): T = suspendCancellableCoroutine { continuation ->
    Platform.runLater {
        runCatching(block).fold(
            onSuccess = { continuation.resume(it) },
            onFailure = { continuation.resumeWithException(it) }
        )
    }
}

private val POT_JSON = Json { ignoreUnknownKeys = true }

private fun parseChallengeData(raw: String): String {
    val scrambled = POT_JSON.parseToJsonElement(raw).jsonArray
    val challenge = if (scrambled.size > 1 && scrambled[1].jsonPrimitive.isString) {
        POT_JSON.parseToJsonElement(descramble(scrambled[1].jsonPrimitive.content)).jsonArray
    } else {
        scrambled[0].jsonArray
    }
    val safeScript = challenge[1].takeIf { it !is JsonNull }?.jsonArray?.find { it.jsonPrimitive.isString }
    val trustedResource = challenge[2].takeIf { it !is JsonNull }?.jsonArray?.find { it.jsonPrimitive.isString }
    return POT_JSON.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "messageId" to JsonPrimitive(challenge[0].jsonPrimitive.content),
                "interpreterJavascript" to JsonObject(
                    mapOf(
                        "privateDoNotAccessOrElseSafeScriptWrappedValue" to (safeScript ?: JsonNull),
                        "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to (trustedResource ?: JsonNull)
                    )
                ),
                "interpreterHash" to JsonPrimitive(challenge[3].jsonPrimitive.content),
                "program" to JsonPrimitive(challenge[4].jsonPrimitive.content),
                "globalName" to JsonPrimitive(challenge[5].jsonPrimitive.content),
                "clientExperimentsStateBlob" to JsonPrimitive(challenge[7].jsonPrimitive.content)
            )
        )
    )
}

private fun parseIntegrityTokenData(raw: String): Pair<String, Long> {
    val data = POT_JSON.parseToJsonElement(raw).jsonArray
    return newUint8Array(decodeYouTubeBase64(data[0].jsonPrimitive.content)) to data[1].jsonPrimitive.long
}

private fun descramble(value: String): String =
    decodeYouTubeBase64(value).map { (it + 97).toByte() }.toByteArray().decodeToString()

private fun decodeYouTubeBase64(value: String): ByteArray {
    var normalized = value.replace('-', '+').replace('_', '/').replace('.', '=')
    while (normalized.length % 4 != 0) normalized += "="
    return Base64.getDecoder().decode(normalized)
}

private fun newUint8Array(bytes: ByteArray): String =
    "new Uint8Array([${bytes.joinToString(",") { it.toUByte().toString() }}])"

private fun u8ToBase64(value: String): String {
    val bytes = value.split(',').filter(String::isNotBlank).map { it.trim().toUByte().toByte() }.toByteArray()
    return Base64.getEncoder().encodeToString(bytes).replace('+', '-').replace('/', '_')
}
package app.cubic.android.core.utils.potoken

import android.webkit.CookieManager
import android.os.SystemClock
import app.cubic.android.core.utils.cipher.CipherDeobfuscator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class PoTokenGenerator {
    private val TAG = "PoTokenGenerator"

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false // whether the system has a bad WebView implementation

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenSessionPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null
    @Volatile
    private var unavailableUntilElapsedMs = 0L

    fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        Timber.tag(TAG).d("getWebClientPoToken called: videoId=$videoId, sessionId=$sessionId")
        Timber.tag(TAG).d("WebView state: supported=$webViewSupported, badImpl=$webViewBadImpl")
        if (!webViewSupported || webViewBadImpl) {
            Timber.tag(TAG).d("WebView not available: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return null
        }

        if (SystemClock.elapsedRealtime() < unavailableUntilElapsedMs) {
            Timber.tag(TAG).d("PoToken WebView is cooling down; continuing with fallback clients")
            return null
        }

        return try {
            Timber.tag(TAG).d("Calling runBlocking to generate poToken (timeout=${POTOKEN_TIMEOUT_MS}ms)...")
            runBlocking {
                withTimeout(POTOKEN_TIMEOUT_MS) {
                    getWebClientPoToken(videoId, sessionId, forceRecreate = false)
                }
            }
        } catch (e: TimeoutCancellationException) {
            // The WebView's sandboxed process can be culled by the OS (storage pressure, low
            // memory, etc.) which leaves the PoToken WebView call hung indefinitely. Cap it so
            // playerResponseForPlayback can fall through to non-PoToken fallback clients (e.g.
            // ANDROID_VR) instead of blocking the entire playback path.
            Timber.tag(TAG).w("poToken generation timed out after ${POTOKEN_TIMEOUT_MS}ms; proceeding without PoToken")
            runBlocking {
                webPoTokenGenLock.withLock {
                    try {
                        withContext(Dispatchers.Main) {
                            webPoTokenGenerator?.close()
                        }
                    } catch (closeEx: Exception) {
                        Timber.tag(TAG).e(closeEx, "Exception closing PoTokenWebView during timeout cleanup")
                    }
                    webPoTokenGenerator = null
                    webPoTokenSessionPot = null
                    webPoTokenSessionId = null
                }
            }
            unavailableUntilElapsedMs = SystemClock.elapsedRealtime() + POTOKEN_FAILURE_COOLDOWN_MS
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "poToken generation exception: ${e.javaClass.simpleName}: ${e.message}")
            when (e) {
                is BadWebViewException -> {
                    Timber.tag(TAG).e(e, "Could not obtain poToken because WebView is broken")
                    webViewBadImpl = true
                    null
                }
                else -> {
                    unavailableUntilElapsedMs = SystemClock.elapsedRealtime() + POTOKEN_FAILURE_COOLDOWN_MS
                    null
                }
            }
        }
    }

    companion object {
        val shared: PoTokenGenerator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            PoTokenGenerator()
        }

        // Healthy cold-start (WebView spin-up + botguard JS + token gen) is ~2–5s in practice;
        // 8s leaves slack for a slow device without making the user wait too long before the
        // fallback chain (ANDROID_VR, etc.) takes over when the WebView hangs.
        private const val POTOKEN_TIMEOUT_MS = 8_000L
        private const val POTOKEN_FAILURE_COOLDOWN_MS = 60_000L
        private const val MIN_HEALTHY_POTOKEN_LENGTH = 100
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenWebView.generatePoToken] was called
     */
    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        Timber.tag(TAG).d("Web poToken requested: videoId=$videoId, sessionId=$sessionId")

        val (poTokenGenerator, sessionPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired || webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    Timber.tag(TAG).d("Creating new PoTokenWebView (forceRecreate=$forceRecreate)")

                    withContext(Dispatchers.Main) {
                        webPoTokenGenerator?.close()
                    }

                    // Clear committed state before the fallible creation/mint steps. A failed
                    // attempt must never leave a new session ID paired with a stale token.
                    webPoTokenGenerator = null
                    webPoTokenSessionPot = null
                    webPoTokenSessionId = null

                    val newGenerator = PoTokenWebView.getNewPoTokenGenerator(CipherDeobfuscator.appContext)
                    val newSessionPot = try {
                        // The session-bound token must be minted once before per-video tokens.
                        newGenerator.generatePoToken(sessionId)
                    } catch (throwable: Throwable) {
                        withContext(Dispatchers.Main) { newGenerator.close() }
                        throw throwable
                    }

                    webPoTokenGenerator = newGenerator
                    webPoTokenSessionPot = newSessionPot
                    webPoTokenSessionId = sessionId
                    Timber.tag(TAG).d("Session poToken generated for sessionId=${sessionId.take(20)}...")
                }

                Triple(webPoTokenGenerator!!, webPoTokenSessionPot!!, shouldRecreate)
            }

        val videoPot = try {
            poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                // the poTokenGenerator has just been recreated (and possibly this is already the
                // second time we try), so there is likely nothing we can do
                throw throwable
            } else {
                // retry, this time recreating the [webPoTokenGenerator] from scratch;
                // this might happen for example if the app goes in the background and the WebView
                // content is lost
                Timber.tag(TAG).e(throwable, "Failed to obtain poToken, retrying")
                return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
            }
        }

        if (videoPot.length < MIN_HEALTHY_POTOKEN_LENGTH || sessionPot.length < MIN_HEALTHY_POTOKEN_LENGTH) {
            Timber.tag(TAG).w(
                "Discarding undersized poToken: session=%d video=%d",
                sessionPot.length,
                videoPot.length
            )
            if (!hasBeenRecreated) {
                return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
            }
            throw PoTokenException("Undersized poToken after WebView recreation")
        }

        Timber.tag(TAG).d("poToken generated successfully: session=${sessionPot.take(20)}..., video=${videoPot.take(20)}...")
        unavailableUntilElapsedMs = 0L

        // The /player request accepts the visitor/session-bound token. The googlevideo URL's
        // pot= must be bound to this video ID; reversing them can play the first range and then
        // fail later requests with HTTP 403.
        return PoTokenResult(
            playerRequestPoToken = sessionPot,
            streamingDataPoToken = videoPot,
        )
    }
}

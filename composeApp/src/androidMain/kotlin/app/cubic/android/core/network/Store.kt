package app.cubic.android.core.network

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.Context as InnerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.Blocking
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import timber.log.Timber

/**
 * Centralized store for network tokens and cookies.
 */
object Store {

    private const val DEFAULT_COOKIE = "PREF=hl=en&tz=UTC; SOCS=CAI"
    private const val YT_WATCH_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&bpctr=9999999999&has_verified=1"
    private const val PLAYBACK_IDENTITY_PREFS = "cubic_playback_identity"
    private const val ANONYMOUS_VISITOR_KEY = "anonymous_visitor_data"

    private val fetchMutex = Mutex()
    private val visitorMutex = Mutex()

    private var ghostResponseHeaders: Headers? = null
    private var ghostResponseBody: String? = null
    private var cookie: String? = null

    @Volatile
    private var iosVisitorData: String? = null

    @Volatile
    private var playbackIdentityPreferences: SharedPreferences? = null

    /**
     * Restores Cubic's anonymous playback visitor without mixing it with the signed-in account
     * session. Keeping this identity stable avoids minting a new YouTube session on every process
     * start, while rejection recovery can still invalidate it explicitly.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (playbackIdentityPreferences == null) {
            playbackIdentityPreferences = context.applicationContext.getSharedPreferences(
                PLAYBACK_IDENTITY_PREFS,
                Context.MODE_PRIVATE,
            )
        }
        if (iosVisitorData.isNullOrBlank()) {
            iosVisitorData = playbackIdentityPreferences
                ?.getString(ANONYMOUS_VISITOR_KEY, null)
                ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        }
    }

    fun cachedIosVisitorData(): String? = iosVisitorData

    @Synchronized
    fun invalidatePlaybackIdentity() {
        ghostResponseHeaders = null
        ghostResponseBody = null
        cookie = null
        iosVisitorData = null
        playbackIdentityPreferences?.edit()?.remove(ANONYMOUS_VISITOR_KEY)?.apply()
        Innertube.visitorData = Innertube.DEFAULT_VISITOR_DATA
        Timber.w("Store: invalidated cached YouTube visitor/cookie identity")
    }

    /**
     * Drops only the anonymous visitor used for media resolution.
     * The signed-in cookie belongs to account browsing and must not be cleared or reused by
     * playback recovery.
     */
    @Synchronized
    fun invalidateAnonymousPlaybackIdentity() {
        iosVisitorData = null
        playbackIdentityPreferences?.edit()?.remove(ANONYMOUS_VISITOR_KEY)?.apply()
        Timber.w("Store: invalidated anonymous playback visitor identity")
    }

    @Blocking
    private suspend fun fetchIfNeeded() {
        if (ghostResponseBody != null && ghostResponseHeaders != null)
            return

        fetchMutex.withLock {
            // Double-check after acquiring lock
            if (ghostResponseBody != null && ghostResponseHeaders != null)
                return@withLock

            runCatching {
                Innertube.client.get(YT_WATCH_URL) {
                    headers {
                        append(HttpHeaders.Connection, "Close")
                        append(HttpHeaders.Host, "www.youtube.com")
                        append(HttpHeaders.Cookie, DEFAULT_COOKIE)
                        append(HttpHeaders.UserAgent, InnerContext.USER_AGENT_WEB)
                        append("Sec-Fetch-Mode", "navigate")
                    }
                }
            }.fold(
                onSuccess = {
                    // Cache for later use
                    ghostResponseHeaders = it.headers
                    ghostResponseBody = it.bodyAsText()
                },
                onFailure = {
                    Timber.e(it, "Store: Failed to fetch visitorData")
                }
            )
        }
    }

    /**
     * Retrieves visitor data for iOS client.
     */
    fun getIosVisitorData(): String = runBlocking(Dispatchers.IO) {
        ensureIosVisitorData()
    }

    /** Fetches the anonymous visitor on an IO coroutine and persists the successful identity. */
    suspend fun ensureIosVisitorData(): String {
        iosVisitorData?.let { return it }

        return visitorMutex.withLock {
            iosVisitorData?.let { return@withLock it }

            val currentLocale = java.util.Locale.getDefault()
            val localization = Localization(currentLocale.language.takeIf { it.isNotBlank() } ?: "en")
            val contentCountry = ContentCountry(currentLocale.country.takeIf { it.isNotBlank() } ?: "GB")

            val headers: MutableMap<String, List<String>> = mutableMapOf()
            headers["User-Agent"] = listOf(YoutubeParsingHelper.getIosUserAgent(localization))
            headers.putAll(YoutubeParsingHelper.getOriginReferrerHeaders("https://www.youtube.com"))

            val data = YoutubeParsingHelper.getVisitorDataFromInnertube(
                InnertubeClientRequestInfo.ofIosClient(),
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                null,
                false
            )
            iosVisitorData = data
            playbackIdentityPreferences
                ?.edit()
                ?.putString(ANONYMOUS_VISITOR_KEY, data)
                ?.apply()
            data
        }
    }

    /**
     * Retrieves the network cookie, fetching it if necessary.
     */
    @Blocking
    fun getCookie(): String {
        cookie?.let { return it }

        runBlocking(Dispatchers.IO) { fetchIfNeeded() }

        val headers = ghostResponseHeaders
        if (headers != null) {
            headers.getAll(HttpHeaders.SetCookie)
                .orEmpty()
                .joinToString("; ") {
                    it.split(";").first()
                }
                .let {
                    val finalCookie = "$DEFAULT_COOKIE; $it"
                    cookie = finalCookie
                    return finalCookie
                }
        }

        return DEFAULT_COOKIE
    }
}

package app.it.fast4x.rimusic.ui.screens.spotify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class LogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType {
    ERROR, SUCCESS, LOADING, INFO, WARNING
}

private fun canvasString(resId: Int, vararg args: Any): String = appContext().getString(resId, *args)

object SpotifyApiConfig {
    var CANVAS_API: String = SecureApiConfig.spotifyCanvasApi
    var MATCH_API: String = SecureApiConfig.spotifyMatchApi
    var MATCH_API_KEY: String = SecureApiConfig.spotifyMatchApiKey

    const val MIN_MATCH_SCORE = 80.0
    const val RETRY_DELAY_MS = 5000L

    var isConfigLoaded: Boolean = false

    private val canvasCache = mutableMapOf<String, String>()

    suspend fun loadConfigIfNeeded() {
        if (isConfigLoaded) return
        isConfigLoaded = true
    }

    fun getCanvasFromCache(trackId: String): String? = canvasCache[trackId]

    fun cacheCanvas(trackId: String, canvasUrl: String) {
        canvasCache[trackId] = canvasUrl
    }

    fun clearAllCache() {
        canvasCache.clear()
    }
}

private fun canvasLog(
    message: String,
    type: LogType = LogType.INFO,
    showLogs: Boolean = false,
    force: Boolean = false
) {
    if (force || showLogs) {
        SpotifyCanvasState.addLog(message, type)
    }
}

private object CanvasVideoCache {
    private const val DIR_NAME = "spotify_canvas_video"

    private fun dir(context: android.content.Context): File =
        File(context.cacheDir, DIR_NAME).also { it.mkdirs() }

    private fun safeName(mediaId: String): String =
        mediaId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(96).ifBlank { "canvas" }

    fun clearExcept(context: android.content.Context, mediaId: String? = null) {
        val keep = mediaId?.let { "${safeName(it)}.mp4" }
        dir(context).listFiles()?.forEach { file ->
            if (file.name != keep) runCatching { file.delete() }
        }
    }

    fun clearAll(context: android.content.Context) {
        clearExcept(context, null)
    }

    fun cachedUri(context: android.content.Context, mediaId: String): String? {
        val file = File(dir(context), "${safeName(mediaId)}.mp4")
        return file.takeIf { it.exists() && it.length() > 64 * 1024L }?.toURI()?.toString()
    }

    fun cache(
        context: android.content.Context,
        mediaId: String,
        canvasUrl: String,
        client: OkHttpClient
    ): String? {
        cachedUri(context, mediaId)?.let { return it }

        val cacheDir = dir(context)
        val target = File(cacheDir, "${safeName(mediaId)}.mp4")
        val pending = File(cacheDir, "${target.name}.pending")
        runCatching { pending.delete() }

        val response = client.newCall(Request.Builder().url(canvasUrl).build()).execute()
        response.use {
            if (!it.isSuccessful) return null
            val body = it.body ?: return null
            pending.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }

        if (pending.length() <= 64 * 1024L) {
            runCatching { pending.delete() }
            return null
        }

        runCatching { target.delete() }
        return if (pending.renameTo(target)) target.toURI().toString() else null
    }
}

private object SpotifySessionApi {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36"
    private val secretsUrl: String
        get() = SecureApiConfig.spotifySecretsUrl
    private val serverTimeUrl: String
        get() = SecureApiConfig.spotifyServerTimeUrl
    private val tokenUrl: String
        get() = SecureApiConfig.spotifyTokenUrl
    private val webAccessTokenUrl: String
        get() = SecureApiConfig.spotifyWebAccessTokenUrl
    private val searchUrl: String
        get() = SecureApiConfig.spotifySearchUrl
    private const val FETCH_INTERVAL_MS = 60L * 60L * 1000L

    private val fallbackSecret = listOf(
        99, 111, 47, 88, 49, 56, 118, 65, 52, 67, 50, 104, 117,
        101, 55, 94, 95, 75, 94, 49, 69, 36, 85, 64, 74, 60
    )

    private var cachedAccessToken: String? = null
    private var cachedTokenExpiryMs: Long = 0L
    private var currentTotpVersion: String = "19"
    private var currentSecretBytes: List<Int> = fallbackSecret
    private var lastSecretFetchTime: Long = 0L

    private fun buildCookieHeader(sessionCookie: String): String =
        if (sessionCookie.contains("=") && sessionCookie.contains(";")) {
            sessionCookie
        } else {
            "sp_dc=$sessionCookie"
        }

    fun buildSessionCookieHeader(sessionCookie: String): String = buildCookieHeader(sessionCookie)

    private fun extractSpDcValue(sessionCookie: String): String {
        if (!sessionCookie.contains("=")) return sessionCookie.trim()

        return sessionCookie
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("sp_dc=") }
            ?.removePrefix("sp_dc=")
            ?.trim()
            .orEmpty()
    }

    fun extractSpDcFromSessionCookie(sessionCookie: String): String = extractSpDcValue(sessionCookie)

    fun clearTokenCache() {
        cachedAccessToken = null
        cachedTokenExpiryMs = 0L
    }

    private fun ensureSecrets(client: OkHttpClient, showLogs: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastSecretFetchTime < FETCH_INTERVAL_MS) return

        try {
            val response = client.newCall(
                Request.Builder()
                    .url(secretsUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
            ).execute()

            if (!response.isSuccessful) throw IOException("Secret fetch HTTP ${response.code}")

            val json = response.body?.use { it.string() } ?: throw IOException("Empty secrets")
            val obj = JSONObject(json)
            val versions = obj.keys().asSequence().mapNotNull { it.toIntOrNull() }.toList()
            val newestVersion = versions.maxOrNull()?.toString() ?: throw IOException("No secrets")
            val newestSecret = obj.optJSONArray(newestVersion) ?: throw IOException("Missing secret")

            currentTotpVersion = newestVersion
            currentSecretBytes = List(newestSecret.length()) { index -> newestSecret.optInt(index) }
            lastSecretFetchTime = now

            if (showLogs) {
                SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_loaded_totp_secret, newestVersion), LogType.SUCCESS)
            }
        } catch (_: Exception) {
            currentTotpVersion = "19"
            currentSecretBytes = fallbackSecret
            if (showLogs) {
                SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_fallback_totp_secret), LogType.WARNING)
            }
        }
    }

    fun getSpotifyTokenWithTotp(
        client: OkHttpClient,
        sessionCookie: String,
        showLogs: Boolean
    ): String? {
        val now = System.currentTimeMillis()
        if (!cachedAccessToken.isNullOrBlank() && now < cachedTokenExpiryMs - 60_000L) {
            return cachedAccessToken
        }

        ensureSecrets(client, showLogs)

        val serverTime = getServerTime(client, sessionCookie)
        val attempts = listOf("transport", "init")

        for (reason in attempts) {
            val token = requestSpotifyToken(client, sessionCookie, reason, serverTime, showLogs)
            if (!token.isNullOrBlank()) {
                cachedAccessToken = token
                cachedTokenExpiryMs = now + 50L * 60L * 1000L
                if (showLogs) {
                    SpotifyCanvasState.addLog(
                        canvasString(R.string.cubic_canvas_auth_token_ready_via, reason),
                        LogType.SUCCESS
                    )
                }
                return token
            }
        }

        val fallbackToken = requestWebAccessToken(client, sessionCookie, showLogs)
        if (!fallbackToken.isNullOrBlank()) {
            cachedAccessToken = fallbackToken
            cachedTokenExpiryMs = now + 50L * 60L * 1000L
            if (showLogs) {
                SpotifyCanvasState.addLog(
                    canvasString(R.string.cubic_canvas_auth_token_web_fallback),
                    LogType.SUCCESS
                )
            }
        }
        return fallbackToken
    }

    private fun getServerTime(client: OkHttpClient, sessionCookie: String): Long {
        return try {
            val response = client.newCall(
                Request.Builder()
                    .url(serverTimeUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Origin", "https://open.spotify.com/")
                    .header("Referer", "https://open.spotify.com/")
                    .header("Cookie", buildCookieHeader(sessionCookie))
                    .build()
            ).execute()

            if (!response.isSuccessful) return System.currentTimeMillis()

            val json = response.body?.use { it.string() } ?: return System.currentTimeMillis()
            val seconds = JSONObject(json).optLong("serverTime")
            if (seconds > 0L) seconds * 1000L else System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun requestSpotifyToken(
        client: OkHttpClient,
        sessionCookie: String,
        reason: String,
        serverTime: Long,
        showLogs: Boolean
    ): String? {
        val otpValue = generateTotp(serverTime)
        val url = tokenUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("reason", reason)
            ?.addQueryParameter("productType", "mobile-web-player")
            ?.addQueryParameter("totp", otpValue)
            ?.addQueryParameter("totpVer", currentTotpVersion)
            ?.addQueryParameter("totpServer", otpValue)
            ?.build()
            ?: return null

        return try {
            val response = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("App-platform", "WebPlayer")
                    .header("Spotify-App-Version", "1.2.61.20.g3b4cd5b2")
                    .header("Accept", "application/json")
                    .header("Origin", "https://open.spotify.com/")
                    .header("Referer", "https://open.spotify.com/")
                    .header("Cookie", buildCookieHeader(sessionCookie))
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                if (showLogs) {
                    SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_spotify_token_http, response.code, reason), LogType.WARNING)
                }
                return null
            }

            val json = response.body?.use { it.string() } ?: return null
            JSONObject(json).optString("accessToken").takeIf { it.length >= 100 }
        } catch (_: Exception) {
            null
        }
    }

    private fun requestWebAccessToken(
        client: OkHttpClient,
        sessionCookie: String,
        showLogs: Boolean
    ): String? {
        val url = webAccessTokenUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("reason", "transport")
            ?.addQueryParameter("productType", "web_player")
            ?.build()
            ?: return null

        return try {
            val response = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Cookie", buildCookieHeader(sessionCookie))
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                if (showLogs) {
                    SpotifyCanvasState.addLog(
                        canvasString(R.string.cubic_canvas_fallback_token_http, response.code),
                        LogType.WARNING
                    )
                }
                return null
            }

            val json = response.body?.use { it.string() } ?: return null
            JSONObject(json).optString("accessToken").takeIf { it.length >= 100 }
        } catch (_: Exception) {
            null
        }
    }

    fun searchTrackIdViaMatcherApi(
        client: OkHttpClient,
        sessionCookie: String,
        title: String,
        artist: String,
        showLogs: Boolean
    ): Pair<String?, Double?> {
        val cleanTitle = title
            .replace(Regex("\\(official\\s*(music\\s*)?video\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(official\\s*audio\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(lyrics?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[official\\s*(music\\s*)?video\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[lyrics?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(visualizer\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\|.*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val cleanArtist = artist
            .replace(Regex("[-–]\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("Official$", RegexOption.IGNORE_CASE), "")
            .trim()

        val requestBody = JSONObject().apply {
            put("action", "match_spotify")
            put("spDc", extractSpDcValue(sessionCookie))
            put("videoTitle", cleanTitle.ifBlank { title })
            put("videoAuthor", cleanArtist.ifBlank { artist })
        }.toString()

        return try {
            canvasLog(
                message = canvasString(R.string.cubic_canvas_matcher_api_url, SpotifyApiConfig.MATCH_API),
                type = LogType.INFO,
                showLogs = showLogs,
                force = true
            )

            val response = client.newCall(
                Request.Builder()
                    .url(SpotifyApiConfig.MATCH_API)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("apikey", SpotifyApiConfig.MATCH_API_KEY)
                    .header("Authorization", "Bearer ${SpotifyApiConfig.MATCH_API_KEY}")
                    .header("User-Agent", USER_AGENT)
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.use { it.string() }.orEmpty().take(220)
                canvasLog(
                    message = canvasString(R.string.cubic_canvas_matcher_api_http, response.code, errorBody),
                    type = LogType.WARNING,
                    showLogs = showLogs,
                    force = true
                )
                return Pair(null, null)
            }

            val json = response.body?.use { it.string() } ?: return Pair(null, null)
            val root = JSONObject(json)
            val searchQuery = root.optString("searchQuery")
            if (searchQuery.isNotBlank()) {
                canvasLog(
                    message = canvasString(R.string.cubic_canvas_search_query, searchQuery),
                    type = LogType.INFO,
                    showLogs = showLogs,
                    force = true
                )
            }

            val tracks = root.optJSONArray("tracks") ?: return Pair(null, null)
            var bestTrackId: String? = null
            var bestScore = 0.0

            for (index in 0 until tracks.length()) {
                val track = tracks.optJSONObject(index) ?: continue
                val candidateId = track.optString("id").takeIf { it.isNotBlank() } ?: continue
                val candidateTitle = track.optString("name")
                val candidateArtists = track.optJSONArray("artists")
                    ?.let { artists ->
                        buildString {
                            for (artistIndex in 0 until artists.length()) {
                                if (isNotEmpty()) append(", ")
                                append(artists.optString(artistIndex))
                            }
                        }
                    }
                    .orEmpty()
                val score = computeMatchScore(title, artist, candidateTitle, candidateArtists)
                if (score > bestScore) {
                    bestScore = score
                    bestTrackId = candidateId
                }
            }

            if (!bestTrackId.isNullOrBlank()) {
                canvasLog(
                    message = canvasString(R.string.cubic_canvas_track_id_found_matcher),
                    type = LogType.SUCCESS,
                    showLogs = showLogs,
                    force = true
                )
            }
            Pair(bestTrackId, bestScore.takeIf { it > 0.0 })
        } catch (e: Exception) {
            canvasLog(
                message = canvasString(R.string.cubic_canvas_matcher_api_failed, e.message ?: ""),
                type = LogType.WARNING,
                showLogs = showLogs,
                force = true
            )
            Pair(null, null)
        }
    }

    fun searchTrackId(
        client: OkHttpClient,
        authToken: String,
        title: String,
        artist: String,
        showLogs: Boolean
    ): Pair<String?, Double?> {
        val queries = buildSearchQueries(title, artist)

        var bestTrackId: String? = null
        var bestScore = 0.0
        var bestQuery: String? = null

        for (query in queries) {
            if (showLogs) {
                SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_search_query, query), LogType.INFO)
            }
            val url = searchUrl.toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", query)
                ?.addQueryParameter("type", "track")
                ?.addQueryParameter("limit", "5")
                ?.build()
                ?: continue

            val response = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $authToken")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                if (showLogs) {
                    SpotifyCanvasState.addLog(
                        canvasString(R.string.cubic_canvas_spotify_search_http, response.code, query),
                        LogType.WARNING
                    )
                }
                continue
            }

            val json = response.body?.use { it.string() } ?: continue
            val items = JSONObject(json)
                .optJSONObject("tracks")
                ?.optJSONArray("items")
                ?: continue

            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue

                val candidateId = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val candidateTitle = item.optString("name")
                val candidateArtists = item.optJSONArray("artists")
                    ?.let { artists ->
                        buildString {
                            for (artistIndex in 0 until artists.length()) {
                                if (isNotEmpty()) append(", ")
                                append(
                                    artists.optJSONObject(artistIndex)
                                        ?.optString("name")
                                        .orEmpty()
                                )
                            }
                        }
                    }
                    .orEmpty()

                val score = computeMatchScore(title, artist, candidateTitle, candidateArtists)
                if (score > bestScore) {
                    bestScore = score
                    bestTrackId = candidateId
                    bestQuery = query
                }
            }
        }

        if (showLogs && !bestTrackId.isNullOrBlank()) {
            SpotifyCanvasState.addLog(
                canvasString(R.string.cubic_canvas_track_id_found_query, bestQuery.orEmpty()),
                LogType.SUCCESS
            )
        }

        return Pair(bestTrackId, bestScore.takeIf { it > 0.0 })
    }

    private fun buildSearchQueries(title: String, artist: String): List<String> {
        val cleanTitle = sanitizeMetadata(title)
        val cleanArtist = sanitizeMetadata(artist)
        val titleWithoutArtistPrefix = cleanTitle.removePrefix("$cleanArtist ").trim()
        val titleWithoutArtistSuffix = cleanTitle.removeSuffix(" $cleanArtist").trim()

        return listOf(
            "$cleanTitle $cleanArtist".trim(),
            "$cleanArtist $cleanTitle".trim(),
            titleWithoutArtistPrefix,
            titleWithoutArtistSuffix,
            cleanTitle,
            "$title $artist".trim(),
            "$artist - $title".trim()
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun generateTotp(timestamp: Long): String {
        val secretBytes = currentSecretBytes.mapIndexed { index, value ->
            value xor ((index % 33) + 9)
        }.joinToString("").toByteArray()

        val counter = timestamp / 30_000L
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(secretBytes, "HmacSHA1"))
        }
        val hash = mac.doFinal(data)
        val offset = hash.last().toInt() and 0x0F
        val binary = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
            )
        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun computeMatchScore(
        sourceTitle: String,
        sourceArtist: String,
        candidateTitle: String,
        candidateArtist: String
    ): Double {
        val titleScore = tokenOverlapScore(sourceTitle, candidateTitle)
        val artistScore = tokenOverlapScore(sourceArtist, candidateArtist)
        return ((titleScore * 0.7) + (artistScore * 0.3)) * 100.0
    }

    private fun tokenOverlapScore(left: String, right: String): Double {
        val leftTokens = normalizeForMatch(left).split(" ").filter { it.isNotBlank() }.toSet()
        val rightTokens = normalizeForMatch(right).split(" ").filter { it.isNotBlank() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        return leftTokens.intersect(rightTokens).size.toDouble() /
            leftTokens.union(rightTokens).size.toDouble()
    }

    private fun normalizeForMatch(value: String): String =
        value
            .lowercase(Locale.US)
            .replace(Regex("[-–]\\s*topic$"), " ")
            .replace(Regex("vevo$"), " ")
            .replace(Regex("official$"), " ")
            .replace(Regex("\\(official[^)]*\\)|\\[official[^]]*\\]"), " ")
            .replace(Regex("\\(lyrics?\\)|\\[lyrics?\\]"), " ")
            .replace(Regex("\\(visualizer\\)"), " ")
            .replace(Regex("\\|.*$"), " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun sanitizeMetadata(value: String): String = normalizeForMatch(value)
}

object SpotifyCanvasState {
    var currentCanvasUrl: String? by mutableStateOf(null)
    var isLoading: Boolean by mutableStateOf(false)
    var error: String? by mutableStateOf(null)
    var logEntries: MutableList<LogEntry> by mutableStateOf(mutableListOf())
    var currentTrackId: String? by mutableStateOf(null)
    var currentMediaItemId: String? by mutableStateOf(null)
    var currentSongTitle: String? by mutableStateOf(null)
    var currentArtist: String? by mutableStateOf(null)
    var isPlaying: Boolean by mutableStateOf(false)
    var lastFetchTime: Long by mutableStateOf(0L)
    var configSource: String by mutableStateOf(canvasString(R.string.cubic_canvas_loading))
    var lastProcessedMediaId: String? by mutableStateOf(null)
    var hasTriedFetching: Boolean by mutableStateOf(false)
    var shouldRetryFetch: Boolean by mutableStateOf(false)

    private const val MAX_LOG_ENTRIES = 20

    fun addLog(message: String, type: LogType = LogType.INFO) {
        synchronized(logEntries) {
            logEntries.add(LogEntry(message, type))
            if (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries = logEntries.takeLast(MAX_LOG_ENTRIES).toMutableList()
            }
        }
    }

    fun clearForNewSong(mediaId: String, title: String?, artist: String?) {
        currentCanvasUrl = null
        currentTrackId = null
        error = null
        isLoading = false
        currentMediaItemId = mediaId
        currentSongTitle = title
        currentArtist = artist
        lastProcessedMediaId = mediaId
        hasTriedFetching = false
        shouldRetryFetch = true
        addLog(canvasString(R.string.cubic_canvas_new_song_log, title ?: "", artist ?: ""), LogType.INFO)
    }

    fun matchesCurrentSong(mediaId: String, title: String?, artist: String?): Boolean =
        currentMediaItemId == mediaId &&
            currentSongTitle == title &&
            currentArtist == artist

    fun clearAll() {
        currentCanvasUrl = null
        currentTrackId = null
        currentMediaItemId = null
        currentSongTitle = null
        currentArtist = null
        error = null
        isLoading = false
        isPlaying = false
        lastFetchTime = 0L
        lastProcessedMediaId = null
        hasTriedFetching = false
        shouldRetryFetch = false
        SpotifySessionApi.clearTokenCache()
        addLog(canvasString(R.string.cubic_canvas_all_state_cleared), LogType.INFO)
    }

    fun clearLogs() {
        logEntries = mutableListOf()
    }

    fun markFetchAttempted() {
        hasTriedFetching = true
        lastFetchTime = System.currentTimeMillis()
    }

    fun scheduleRetry() {
        shouldRetryFetch = true
        addLog(canvasString(R.string.cubic_canvas_scheduled_retry), LogType.INFO)
    }

    fun updateConfigSource(source: String) {
        configSource = source
        addLog(canvasString(R.string.cubic_canvas_using_source, source), LogType.INFO)
    }

    fun shouldFetchForCurrentSong(mediaId: String): Boolean =
        mediaId == currentMediaItemId &&
            currentCanvasUrl == null &&
            !isLoading &&
            (!hasTriedFetching ||
                (shouldRetryFetch &&
                    System.currentTimeMillis() - lastFetchTime > SpotifyApiConfig.RETRY_DELAY_MS))
}

@Composable
fun SpotifyCanvasWorker() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

    val isCanvasEnabled by rememberPreference("spotifyCanvasEnabled", false)
    val showLogs by rememberPreference("showSpotifyCanvasLogs", false)
    val displayedMediaItem = binder.displayedMediaItem ?: binder.player.currentMediaItem

    LaunchedEffect(isCanvasEnabled) {
        if (isCanvasEnabled && !SpotifyApiConfig.isConfigLoaded) {
            withContext(Dispatchers.IO) { SpotifyApiConfig.loadConfigIfNeeded() }
            SpotifyCanvasState.updateConfigSource(canvasString(R.string.cubic_canvas_source_fixed_matcher))
        }
    }

    LaunchedEffect(isCanvasEnabled) {
        if (!isCanvasEnabled) {
            SpotifyCanvasState.clearAll()
            CanvasPlayerManager.stopAndClear()
            SpotifyApiConfig.clearAllCache()
            CanvasVideoCache.clearAll(context)
        }
    }

    LaunchedEffect(
        isCanvasEnabled,
        appRunningInBackground,
        displayedMediaItem?.mediaId,
        displayedMediaItem?.mediaMetadata?.title,
        displayedMediaItem?.mediaMetadata?.artist
    ) {
        if (!isCanvasEnabled) return@LaunchedEffect

        if (appRunningInBackground) {
            SpotifyCanvasState.isPlaying = false
            CanvasPlayerManager.pauseKeepingState()
            return@LaunchedEffect
        }

        val mediaItem = displayedMediaItem ?: return@LaunchedEffect
        val mediaId = mediaItem.mediaId
        val (title, artist) = resolveCanvasMetadata(
            mediaId = mediaId,
            title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
            artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
        )
        if (title.isBlank() || artist.isBlank()) return@LaunchedEffect

        val songChanged = !SpotifyCanvasState.matchesCurrentSong(mediaId, title, artist)
        if (songChanged) {
            CanvasPlayerManager.stopAndClearForNewSong()
            CanvasVideoCache.clearExcept(context, mediaId)
            SpotifyCanvasState.clearForNewSong(mediaId, title, artist)
        }

        val shouldPlay = binder.player.playWhenReady && binder.player.playbackState == Player.STATE_READY
        if (SpotifyCanvasState.currentCanvasUrl != null &&
            SpotifyCanvasState.matchesCurrentSong(mediaId, title, artist)
        ) {
            SpotifyCanvasState.isPlaying = shouldPlay
            CanvasPlayerManager.updatePlayState(shouldPlay)
            return@LaunchedEffect
        }

        if (!SpotifyCanvasState.shouldFetchForCurrentSong(mediaId)) return@LaunchedEffect

        SpotifyCanvasState.markFetchAttempted()
        if (showLogs) {
            SpotifyCanvasState.addLog(
                canvasString(
                    if (songChanged) R.string.cubic_canvas_initial_fetch else R.string.cubic_canvas_fetching_for,
                    title,
                    artist
                ),
                LogType.LOADING
            )
        }
        launch(Dispatchers.IO) {
            fetchCanvasForSong(
                context = context,
                title = title,
                artist = artist,
                showLogs = showLogs,
                mediaId = mediaId,
                shouldPlayWhenReady = shouldPlay
            )
        }
    }

    LaunchedEffect(binder.player, displayedMediaItem?.mediaId, appRunningInBackground) {

        snapshotFlow {
            Pair(binder.player.playbackState, binder.player.playWhenReady)
        }.collect { (playbackState, playWhenReady) ->
            if (appRunningInBackground) {
                SpotifyCanvasState.isPlaying = false
                CanvasPlayerManager.pauseKeepingState()
                return@collect
            }
            val mediaId = displayedMediaItem?.mediaId

            if (mediaId == SpotifyCanvasState.currentMediaItemId &&
                SpotifyCanvasState.currentCanvasUrl != null
            ) {
                val shouldPlay = playWhenReady && playbackState == Player.STATE_READY
                if (shouldPlay != SpotifyCanvasState.isPlaying) {
                    SpotifyCanvasState.isPlaying = shouldPlay
                    CanvasPlayerManager.updatePlayState(shouldPlay)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SpotifyCanvasState.isPlaying = false
            CanvasPlayerManager.pauseKeepingState()
        }
    }
}

private suspend fun resolveCanvasMetadata(
    mediaId: String,
    title: String,
    artist: String
): Pair<String, String> {
    val normalizedMediaId = mediaId.substringAfterLast("/")
    val dbSong = Database.songTable.findById(normalizedMediaId).first()

    val resolvedTitle = dbSong?.title?.takeIf { it.isNotBlank() } ?: title
    val resolvedArtist = dbSong?.artistsText?.takeIf { it.isNotBlank() } ?: artist

    return resolvedTitle.trim() to resolvedArtist.trim()
}

private suspend fun fetchCanvasForSong(
    context: android.content.Context,
    title: String,
    artist: String,
    showLogs: Boolean,
    mediaId: String,
    shouldPlayWhenReady: Boolean,
) {
    if (appRunningInBackground) return
    SpotifyCanvasState.isLoading = true

    try {
        canvasLog(
            message = canvasString(R.string.cubic_canvas_now_playing_metadata, artist, title),
            type = LogType.INFO,
            showLogs = showLogs,
            force = true
        )

        val canvasUrl = withTimeoutOrNull(15_000L) {
            withContext(Dispatchers.IO) {
                fetchSpotifyCanvas(context, title, artist, showLogs)
            }
        }

        if (mediaId == SpotifyCanvasState.currentMediaItemId) {
            if (canvasUrl != null) {
                val canvasClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
                val cachedCanvasUrl = CanvasVideoCache.cache(context, mediaId, canvasUrl, canvasClient)
                SpotifyCanvasState.currentCanvasUrl = cachedCanvasUrl ?: canvasUrl
                SpotifyCanvasState.isPlaying = shouldPlayWhenReady
                SpotifyCanvasState.error = null
                SpotifyCanvasState.shouldRetryFetch = false
                CanvasPlayerManager.updatePlayState(shouldPlayWhenReady)
                if (showLogs) {
                    SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_loaded_successfully), LogType.SUCCESS)
                }
            } else {
                SpotifyCanvasState.error = canvasString(R.string.cubic_canvas_no_canvas_available)
                SpotifyCanvasState.scheduleRetry()
                if (showLogs) {
                    SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_not_found_retry), LogType.WARNING)
                }
            }
        }
    } catch (e: Exception) {
        if (mediaId == SpotifyCanvasState.currentMediaItemId) {
            SpotifyCanvasState.error = canvasString(R.string.cubic_canvas_failed_to_load, e.message ?: "")
            SpotifyCanvasState.scheduleRetry()
            if (showLogs) {
                SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_error, e.message ?: ""), LogType.ERROR)
            }
        }
    } finally {
        SpotifyCanvasState.isLoading = false
    }
}

private fun fetchSpotifyCanvas(
    context: android.content.Context,
    title: String,
    artist: String,
    showLogs: Boolean
): String? {
    val cacheDir = File(context.cacheDir, "spotify_canvas").also { it.mkdirs() }
    val sessionCookie = getSpotifyCookieHeader(context)
        ?: renewSpotifySession(context).takeIf { it }?.let { getSpotifyCookieHeader(context) }
    val spDc = getSpDc(context)?.takeIf { it.isNotBlank() }
    if (sessionCookie.isNullOrBlank()) {
        canvasLog(
            message = canvasString(R.string.cubic_canvas_cookies_required),
            type = LogType.WARNING,
            showLogs = showLogs,
            force = true
        )
        return null
    }
    val resolvedSpDc = spDc ?: SpotifySessionApi.extractSpDcFromSessionCookie(sessionCookie)

    val client = OkHttpClient.Builder()
        .cache(Cache(cacheDir, 20 * 1024 * 1024))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    return try {
        canvasLog(
            message = canvasString(R.string.cubic_canvas_searching_track_id),
            type = LogType.LOADING,
            showLogs = showLogs,
            force = true
        )

        var authToken: String? = null
        var (trackId, matchScore) = SpotifySessionApi.searchTrackIdViaMatcherApi(
            client = client,
            sessionCookie = sessionCookie,
            title = title,
            artist = artist,
            showLogs = showLogs
        )

        val shouldFallbackToSpotifySearch =
            trackId.isNullOrBlank() ||
                (matchScore != null && matchScore < SpotifyApiConfig.MIN_MATCH_SCORE)

        if (shouldFallbackToSpotifySearch) {
            canvasLog(
                message = canvasString(R.string.cubic_canvas_searching_track_id),
                type = LogType.INFO,
                showLogs = showLogs,
                force = true
            )

            authToken = SpotifySessionApi.getSpotifyTokenWithTotp(
                client = client,
                sessionCookie = sessionCookie,
                showLogs = showLogs
            )

            if (!authToken.isNullOrBlank()) {
                val (fallbackTrackId, fallbackScore) = SpotifySessionApi.searchTrackId(
                    client = client,
                    authToken = authToken,
                    title = title,
                    artist = artist,
                    showLogs = showLogs
                )

                val fallbackWins =
                    !fallbackTrackId.isNullOrBlank() &&
                        (trackId.isNullOrBlank() || (fallbackScore ?: 0.0) >= (matchScore ?: 0.0))

                if (fallbackWins) {
                    trackId = fallbackTrackId
                    matchScore = fallbackScore
                }
            }
        }

        if (trackId == null) {
            canvasLog(
                message = canvasString(R.string.cubic_canvas_no_matching_track),
                type = LogType.WARNING,
                showLogs = showLogs,
                force = true
            )
            return null
        }

        if (matchScore != null && matchScore < SpotifyApiConfig.MIN_MATCH_SCORE && showLogs) {
            SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_low_match_confidence, matchScore.toInt()), LogType.WARNING)
        }

        SpotifyCanvasState.currentTrackId = trackId
        canvasLog(
            message = canvasString(R.string.cubic_canvas_using_track_id, trackId),
            type = LogType.SUCCESS,
            showLogs = showLogs,
            force = true
        )

        SpotifyApiConfig.getCanvasFromCache(trackId)?.let { cached ->
            if (showLogs) {
                SpotifyCanvasState.addLog(canvasString(R.string.cubic_canvas_using_cached_canvas), LogType.INFO)
            }
            return cached
        }

        val encodedTrackId = URLEncoder.encode(trackId, "UTF-8")
        canvasLog(
            message = canvasString(R.string.cubic_canvas_requesting_track, trackId),
            type = LogType.LOADING,
            showLogs = showLogs,
            force = true
        )
        val canvasResponse = client.newCall(
            Request.Builder()
                .url("${SpotifyApiConfig.CANVAS_API}?trackId=$encodedTrackId")
                .header("Accept", "application/json")
                .header("X-Sp-Dc", resolvedSpDc)
                .header("Cookie", SpotifySessionApi.buildSessionCookieHeader(sessionCookie))
                .apply {
                    authToken?.takeIf { it.isNotBlank() }?.let {
                        header("Authorization", "Bearer $it")
                    }
                }
                .build()
        ).execute()

        if (!canvasResponse.isSuccessful) {
            canvasLog(
                message = canvasString(R.string.cubic_canvas_api_error, canvasResponse.code),
                type = LogType.WARNING,
                showLogs = showLogs,
                force = true
            )
            return null
        }

        val canvasJson = canvasResponse.body?.use { it.string() } ?: return null
        val canvasUrl = parseCanvasUrl(canvasJson)
        if (canvasUrl != null) {
            SpotifyApiConfig.cacheCanvas(trackId, canvasUrl)
            canvasLog(
                message = canvasString(R.string.cubic_canvas_found_track, trackId.take(8)),
                type = LogType.SUCCESS,
                showLogs = showLogs,
                force = true
            )
        }
        canvasUrl
    } catch (e: IOException) {
        canvasLog(
            message = canvasString(R.string.cubic_canvas_network_error, e.message ?: ""),
            type = LogType.ERROR,
            showLogs = showLogs,
            force = true
        )
        null
    } catch (e: Exception) {
        canvasLog(
            message = canvasString(R.string.cubic_canvas_unexpected_error, e.message ?: ""),
            type = LogType.ERROR,
            showLogs = showLogs,
            force = true
        )
        null
    }
}

private fun parseCanvasUrl(json: String): String? {
    return try {
        val root = JSONObject(json)
        root.optJSONArray("canvasesList")
            ?.optJSONObject(0)
            ?.optString("canvasUrl")
            ?.takeIf { it.startsWith("https://") }
            ?: root.optJSONArray("canvases")
                ?.optJSONObject(0)
                ?.optString("canvas_url")
                ?.takeIf { it.startsWith("https://") }
    } catch (_: Exception) {
        null
    }
}

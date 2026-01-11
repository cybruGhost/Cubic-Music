package it.fast4x.rimusic.ui.screens.spotify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType {
    ERROR, SUCCESS, LOADING, INFO, WARNING
}

// API Configuration
object SpotifyApiConfig {
    // Configuration URL  waa niko na jokes ate car😂ol 😂 hio json wacha tu ..nikubokeka tu
    private const val CONFIG_URL = "https://v0-spotify-playlist-csv.vercel.app/carolwillbemywife_config.json"
    
    // Default URLs tuseme fallback and quick fetching
    private const val DEFAULT_MATCH_API = "https://v0-spotifyapishit.vercel.app/api/spotify/match"
    private const val DEFAULT_CANVAS_API = "https://v0-spotifyapishit.vercel.app/api/canvas"
    
    // Actual URLs used
    var MATCH_API: String = DEFAULT_MATCH_API
    var CANVAS_API: String = DEFAULT_CANVAS_API
    
    // Thresholds for detection
    const val MIN_MATCH_SCORE = 80.0
    const val RETRY_DELAY_MS = 5000L // 5 seconds for retry
    
    // Configuration state
    var isConfigLoaded: Boolean = false
    var usingJsonConfig: Boolean = false
    
    // Track cache for canvas URLs
    private val canvasCache = mutableMapOf<String, String>() // trackId -> canvasUrl
    
    suspend fun loadConfigIfNeeded(context: android.content.Context) {
        if (isConfigLoaded) return
        
        try {
            val config = fetchConfigFromUrl(context)
            if (config != null) {
                try {
                    val endpoints = config.optJSONObject("api_endpoints")
                    if (endpoints != null) {
                        val matchApi = endpoints.optString("match_api", DEFAULT_MATCH_API)
                        val canvasApi = endpoints.optString("canvas_api", DEFAULT_CANVAS_API)
                        
                        if (matchApi.isNotBlank() && canvasApi.isNotBlank()) {
                            MATCH_API = matchApi
                            CANVAS_API = canvasApi
                            usingJsonConfig = true
                        }
                    }
                } catch (e: Exception) {
                    usingJsonConfig = false
                }
            }
        } catch (e: Exception) {
            usingJsonConfig = false
        } finally {
            isConfigLoaded = true
        }
    }
    
    private fun fetchConfigFromUrl(context: android.content.Context): JSONObject? {
        val cacheDir = File(context.cacheDir, "spotify_config")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        
        val client = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 1 * 1024 * 1024))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        
        return try {
            val request = Request.Builder()
                .url(CONFIG_URL)
                .header("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return null
            }
            
            val json = response.body?.use { it.string() } ?: return null
            JSONObject(json)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCanvasFromCache(trackId: String): String? {
        return canvasCache[trackId]
    }
    
    fun cacheCanvas(trackId: String, canvasUrl: String) {
        canvasCache[trackId] = canvasUrl
    }
    
    fun clearCacheForTrack(trackId: String) {
        canvasCache.remove(trackId)
    }
    
    fun clearAllCache() {
        canvasCache.clear()
    }
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
    var configSource: String by mutableStateOf("Loading...")
    var lastProcessedMediaId: String? by mutableStateOf(null)
    var hasTriedFetching: Boolean by mutableStateOf(false) // Track if we've tried fetching for current song
    var shouldRetryFetch: Boolean by mutableStateOf(false) // Track if we should retry
    
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
        shouldRetryFetch = true  // FIX: Set to true so new song will fetch
        addLog("New song: $title - $artist", LogType.INFO)
    }
    
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
        addLog("All canvas state cleared", LogType.INFO)
    }
    
    fun markFetchAttempted() {
        hasTriedFetching = true
        lastFetchTime = System.currentTimeMillis()
    }
    
    fun scheduleRetry() {
        shouldRetryFetch = true
        addLog("Scheduled retry in 5 seconds", LogType.INFO)
    }
    
    fun updateConfigSource(source: String) {
        configSource = source
        addLog("Using API config: $source", LogType.INFO)
    }
    
    fun shouldFetchForCurrentSong(mediaId: String): Boolean {
        // Should fetch if:
        // 1. Same song
        // 2. No canvas yet
        // 3. Not currently loading
        // 4. Either never tried OR should retry AND 5 seconds have passed
        return mediaId == currentMediaItemId && 
               currentCanvasUrl == null && 
               !isLoading &&
               (!hasTriedFetching || 
                (shouldRetryFetch && 
                 System.currentTimeMillis() - lastFetchTime > SpotifyApiConfig.RETRY_DELAY_MS))
    }
}

@Composable
fun SpotifyCanvasWorker() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    
    val isCanvasEnabled by rememberPreference("spotifyCanvasEnabled", false)
    val showLogs by rememberPreference("showSpotifyCanvasLogs", false)
    
    // Load configuration once when enabled
    LaunchedEffect(isCanvasEnabled) {
        if (isCanvasEnabled && !SpotifyApiConfig.isConfigLoaded) {
            withContext(Dispatchers.IO) {
                SpotifyApiConfig.loadConfigIfNeeded(context)
            }
            
            if (SpotifyApiConfig.usingJsonConfig) {
                SpotifyCanvasState.updateConfigSource("JSON Config")
                if (showLogs) {
                    SpotifyCanvasState.addLog("Loaded APIs from JSON config", LogType.SUCCESS)
                }
            } else {
                SpotifyCanvasState.updateConfigSource("Default URLs")
                if (showLogs) {
                    SpotifyCanvasState.addLog("Using default API URLs", LogType.INFO)
                }
            }
        }
    }
    
    LaunchedEffect(isCanvasEnabled) {
        if (!isCanvasEnabled) {
            SpotifyCanvasState.clearAll()
            CanvasPlayerManager.stopAndClear()
            SpotifyApiConfig.clearAllCache()
        }
    }
    
    // FIX: Add initial fetch on first load
    LaunchedEffect(binder?.player?.currentMediaItem, isCanvasEnabled) {
        if (binder == null || !isCanvasEnabled) return@LaunchedEffect
        
        val mediaItem = binder.player.currentMediaItem
        val mediaId = mediaItem?.mediaId
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        
        if (mediaId != null && title.isNotBlank() && artist.isNotBlank()) {
            // If canvas is not loaded for current song, fetch it
            if (SpotifyCanvasState.currentMediaItemId != mediaId || 
                SpotifyCanvasState.currentCanvasUrl == null) {
                
                SpotifyCanvasState.clearForNewSong(mediaId, title, artist)
                
                if (showLogs) {
                    SpotifyCanvasState.addLog("Initial fetch for: $title - $artist", LogType.LOADING)
                }
                
                launch(Dispatchers.IO) {
                    fetchCanvasForSong(context, title, artist, showLogs, mediaId)
                }
            }
        }
    }
    
    // Monitor player state for song changes - FIXED VERSION
    LaunchedEffect(binder?.player, isCanvasEnabled) {
        if (binder == null || !isCanvasEnabled) return@LaunchedEffect
        
        snapshotFlow { 
            Triple(
                binder.player.currentMediaItem?.mediaId,
                binder.player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "",
                binder.player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: ""
            )
        }.collect { (mediaId, title, artist) ->
            if (mediaId == null || title.isBlank() || artist.isBlank()) {
                return@collect
            }
            
            // Check if song changed OR if canvas was cleared
            val songChanged = mediaId != SpotifyCanvasState.lastProcessedMediaId
            val canvasWasCleared = SpotifyCanvasState.currentCanvasUrl == null
            
            if (songChanged) {
                if (showLogs) {
                    SpotifyCanvasState.addLog("Song changed to: $title - $artist", LogType.INFO)
                }
                
                // Clear everything for the new song
                SpotifyCanvasState.clearForNewSong(mediaId, title, artist)
                CanvasPlayerManager.stopAndClearForNewSong()
                
                // FIX: Immediately schedule fetch for new song
                SpotifyCanvasState.shouldRetryFetch = true
                SpotifyCanvasState.hasTriedFetching = false
            }
            
            // FIX: Check if we should fetch canvas for current song
            // Either song changed OR canvas was cleared and we should retry
            val shouldFetch = isCanvasEnabled && (
                songChanged || 
                (SpotifyCanvasState.shouldFetchForCurrentSong(mediaId) && canvasWasCleared) ||
                (!SpotifyCanvasState.hasTriedFetching && mediaId == SpotifyCanvasState.currentMediaItemId)
            )
            
            if (shouldFetch) {
                if (showLogs) {
                    SpotifyCanvasState.addLog("Fetching canvas for: $title - $artist", LogType.LOADING)
                }
                
                SpotifyCanvasState.markFetchAttempted()
                
                // Fetch canvas for current song
                launch(Dispatchers.IO) {
                    fetchCanvasForSong(context, title, artist, showLogs, mediaId)
                }
            }
        }
    }
    
    // FIXED: Monitor playback state
    LaunchedEffect(binder?.player) {
        if (binder == null) return@LaunchedEffect
        
        snapshotFlow { 
            Pair(
                binder.player.playbackState,
                binder.player.playWhenReady
            )
        }.collect { (playbackState, playWhenReady) ->
            val mediaId = binder.player.currentMediaItem?.mediaId
            
            // When song ends, prepare for next song
            if (playbackState == Player.STATE_ENDED) {
                // Stop looping but keep player for next song
                CanvasPlayerManager.stopLooping()
                SpotifyCanvasState.isPlaying = false
                
                // FIX: Clear current canvas so next song can fetch
                if (mediaId == SpotifyCanvasState.currentMediaItemId) {
                    SpotifyCanvasState.currentCanvasUrl = null
                    SpotifyCanvasState.shouldRetryFetch = true
                }
                
                if (showLogs) {
                    SpotifyCanvasState.addLog("Song ended, clearing canvas for next song", LogType.INFO)
                }
            }
            
            // Only update play state if canvas is for current song
            if (mediaId == SpotifyCanvasState.currentMediaItemId && 
                SpotifyCanvasState.currentCanvasUrl != null) {
                val shouldPlay = playWhenReady && playbackState == Player.STATE_READY
                if (shouldPlay != SpotifyCanvasState.isPlaying) {
                    SpotifyCanvasState.isPlaying = shouldPlay
                    CanvasPlayerManager.updatePlayState(shouldPlay)
                    
                    if (showLogs) {
                        if (shouldPlay) {
                            SpotifyCanvasState.addLog("Canvas playing", LogType.INFO)
                        } else {
                            SpotifyCanvasState.addLog("Canvas paused", LogType.INFO)
                        }
                    }
                }
            }
            
            // FIX: When song starts, ensure canvas is playing
            if (playbackState == Player.STATE_READY && playWhenReady) {
                if (mediaId == SpotifyCanvasState.currentMediaItemId && 
                    SpotifyCanvasState.currentCanvasUrl != null &&
                    !SpotifyCanvasState.isPlaying) {
                    
                    SpotifyCanvasState.isPlaying = true
                    CanvasPlayerManager.updatePlayState(true)
                    
                    if (showLogs) {
                        SpotifyCanvasState.addLog("Canvas sync with song playback", LogType.INFO)
                    }
                }
            }
        }
    }
        
    DisposableEffect(Unit) {
        onDispose {
            SpotifyCanvasState.clearAll()
            CanvasPlayerManager.stopAndClear()
        }
    }
}

private suspend fun fetchCanvasForSong(
    context: android.content.Context,
    title: String,
    artist: String,
    showLogs: Boolean,
    mediaId: String
) {
    SpotifyCanvasState.isLoading = true
    
    try {
        val canvasUrl = withTimeoutOrNull(10000) {
            withContext(Dispatchers.IO) {
                fetchSpotifyCanvas(context, title, artist, showLogs)
            }
        }
        
        // Only update if still the same song
        if (mediaId == SpotifyCanvasState.currentMediaItemId) {
            if (canvasUrl != null) {
                SpotifyCanvasState.currentCanvasUrl = canvasUrl
                SpotifyCanvasState.isPlaying = true
                SpotifyCanvasState.error = null
                SpotifyCanvasState.shouldRetryFetch = false
                
                if (showLogs) {
                    SpotifyCanvasState.addLog("Canvas loaded successfully", LogType.SUCCESS)
                }
            } else {
                SpotifyCanvasState.error = "No canvas available"
                SpotifyCanvasState.scheduleRetry() // Schedule retry after 5 seconds
                
                if (showLogs) {
                    SpotifyCanvasState.addLog("No canvas found, will retry in 5 seconds", LogType.WARNING)
                }
            }
        } else {
            if (showLogs) {
                SpotifyCanvasState.addLog("Canvas fetch completed but song changed", LogType.INFO)
            }
        }
    } catch (e: Exception) {
        // Only show error if still same song
        if (mediaId == SpotifyCanvasState.currentMediaItemId) {
            SpotifyCanvasState.error = "Failed to load canvas: ${e.message}"
            SpotifyCanvasState.scheduleRetry() // Schedule retry after 5 seconds
            
            if (showLogs) {
                SpotifyCanvasState.addLog("Error: ${e.message}, will retry in 5 seconds", LogType.ERROR)
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
    val cacheDir = File(context.cacheDir, "spotify_canvas")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    
    val client = OkHttpClient.Builder()
        .cache(Cache(cacheDir, 20 * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .header("Cache-Control", "public, max-age=600")
                .build()
        }
        .build()
    
    try {
        // Step 1: Match YouTube video to Spotify track
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        
        val matchUrl = "${SpotifyApiConfig.MATCH_API}?videoTitle=$encodedTitle&author=$encodedArtist&authorVerified=true"
        
        if (showLogs) {
            SpotifyCanvasState.addLog("Finding Spotify track...", LogType.LOADING)
        }
        
        val matchRequest = Request.Builder()
            .url(matchUrl)
            .header("Accept", "application/json")
            .header("User-Agent", "MusicApp/1.0")
            .build()
        
        val matchResponse = client.newCall(matchRequest).execute()
        
        if (!matchResponse.isSuccessful) {
            if (showLogs) {
                SpotifyCanvasState.addLog("Match API error: ${matchResponse.code}", LogType.WARNING)
            }
            return null
        }
        
        val matchJson = matchResponse.body?.use { it.string() } ?: ""
        val (trackId, matchScore) = parseMatchResponse(matchJson)
        
        if (trackId == null) {
            if (showLogs) {
                SpotifyCanvasState.addLog("No matching track found", LogType.WARNING)
            }
            return null
        }
        
        if (matchScore != null && matchScore < SpotifyApiConfig.MIN_MATCH_SCORE) {
            if (showLogs) {
                SpotifyCanvasState.addLog("Low match confidence: $matchScore", LogType.WARNING)
            }
        }
        
        SpotifyCanvasState.currentTrackId = trackId
        
        // Check cache first
        val cachedCanvas = SpotifyApiConfig.getCanvasFromCache(trackId)
        if (cachedCanvas != null) {
            if (showLogs) {
                SpotifyCanvasState.addLog("Using cached canvas", LogType.INFO)
            }
            return cachedCanvas
        }
        
        // Step 2: Fetch Canvas URL
        val canvasUrl = "${SpotifyApiConfig.CANVAS_API}?trackId=$trackId"
        
        if (showLogs) {
            SpotifyCanvasState.addLog("Loading canvas video...", LogType.LOADING)
        }
        
        val canvasRequest = Request.Builder()
            .url(canvasUrl)
            .header("Accept", "application/json")
            .build()
        
        val canvasResponse = client.newCall(canvasRequest).execute()
        
        if (!canvasResponse.isSuccessful) {
            if (showLogs) {
                SpotifyCanvasState.addLog("Canvas API error: ${canvasResponse.code}", LogType.WARNING)
            }
            return null
        }
        
        val canvasJson = canvasResponse.body?.use { it.string() } ?: ""
        val canvasUrlResult = parseCanvasUrl(canvasJson)
        
        if (canvasUrlResult != null) {
            // Cache the result
            SpotifyApiConfig.cacheCanvas(trackId, canvasUrlResult)
            
            if (showLogs) {
                SpotifyCanvasState.addLog("Found track: ${trackId.take(8)}...", LogType.SUCCESS)
            }
        }
        
        return canvasUrlResult
        
    } catch (e: IOException) {
        if (showLogs) {
            SpotifyCanvasState.addLog("Network error: ${e.message}", LogType.ERROR)
        }
        return null
    } catch (e: Exception) {
        if (showLogs) {
            SpotifyCanvasState.addLog("Unexpected error: ${e.message}", LogType.ERROR)
        }
        return null
    }
}

private fun parseMatchResponse(json: String): Pair<String?, Double?> {
    return try {
        val obj = JSONObject(json)
        val trackId = obj.optString("trackId").takeIf { it.isNotBlank() }
        val matchScore = obj.optDouble("matchScore").takeIf { it > 0 }
        Pair(trackId, matchScore)
    } catch (e: Exception) {
        Pair(null, null)
    }
}

private fun parseCanvasUrl(json: String): String? {
    return try {
        val canvases = JSONObject(json).optJSONArray("canvasesList")
        canvases?.let {
            if (it.length() > 0) {
                it.getJSONObject(0).optString("canvasUrl").takeIf { url -> 
                    url.isNotBlank() && url.startsWith("https://")
                }
            } else null
        }
    } catch (e: Exception) {
        null
    }
}

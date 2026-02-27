package app.it.fast4x.rimusic.utils

import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap

// Make these classes accessible from other files
@Serializable
data class WaigweVideo(
    val videoId: String,
    val title: String,
    val artist: String? = null,
    val duration: Int? = null,
    val thumbnail: String? = null
)

@Serializable
data class WaigweSearchResponse(
    val videos: List<WaigweVideo>,
    val success: Boolean
)

object WaigweApi {
    private const val API_BASE = "https://yt.omada.cafe/api/v1"
    private val json = Json { ignoreUnknownKeys = true }
    
    // Cache to avoid repeated API calls
    private val searchCache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L // 6 hours cache
    private val streamUrlCache = ConcurrentHashMap<String, String>()
    
    // Retry mechanism
    private val retryCounts = ConcurrentHashMap<String, Int>()
    private const val MAX_RETRIES = 2
    private val retryDelays = longArrayOf(1000L, 3000L) // 1s, then 3s
    
    // Logging for debugging
    private val fallbackLogs = mutableListOf<String>()
    private const val MAX_LOGS = 100
    
    // Prevent infinite loops - track which songs are using waigwe
    private val waigweMediaIds = ConcurrentHashMap.newKeySet<String>()
    
    private data class CacheEntry(
        val response: WaigweSearchResponse,
        val timestamp: Long
    )
    
    suspend fun search(query: String): Result<WaigweSearchResponse> {
        // Check cache first
        val cached = searchCache[query]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
            addLog("Cache hit for: ${query.take(40)}...")
            return Result.success(cached.response)
        }
        
        // Check retry count
        val retryCount = retryCounts[query] ?: 0
        if (retryCount > MAX_RETRIES) {
            addLog("Max retries reached for: ${query.take(40)}...")
            return Result.failure(Exception("Max retries reached"))
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "$API_BASE/search?q=$encodedQuery"
                
                addLog("Searching: ${query.take(40)}... (attempt ${retryCount + 1})")
                
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val data = json.decodeFromString<WaigweSearchResponse>(response)
                    
                    if (data.success && data.videos.isNotEmpty()) {
                        // Cache successful result
                        searchCache[query] = CacheEntry(data, System.currentTimeMillis())
                        retryCounts.remove(query) // Reset retry count on success
                        addLog("Found ${data.videos.size} results for: ${query.take(40)}...")
                        Result.success(data)
                    } else {
                        addLog("No results for: ${query.take(40)}...")
                        Result.failure(Exception("No videos found"))
                    }
                } else {
                    // Increment retry count and retry with exponential backoff
                    retryCounts[query] = retryCount + 1
                    if (retryCount < MAX_RETRIES) {
                        addLog("Retrying ${query.take(40)}... in ${retryDelays[retryCount]}ms")
                        Result.failure(Exception("API returned ${connection.responseCode}, will retry"))
                    } else {
                        Result.failure(Exception("API returned ${connection.responseCode}"))
                    }
                }
            } catch (e: Exception) {
                // Increment retry count and retry with exponential backoff
                retryCounts[query] = retryCount + 1
                if (retryCount < MAX_RETRIES) {
                    addLog("Error, will retry: ${e.message?.take(50)}")
                    Result.failure(Exception("Network error, will retry"))
                } else {
                    addLog("Final error: ${e.message?.take(50)}")
                    Result.failure(e)
                }
            }
        }
    }
    
    fun getStreamUrl(videoId: String): String {
        // Cache stream URLs since they're deterministic
        return streamUrlCache.getOrPut(videoId) {
            "$API_BASE/stream/$videoId"
        }
    }
    
    // Helper to check if a URL is from waigwe (to prevent loops)
    fun isWaigweUrl(url: String?): Boolean {
        return url?.contains("yt.omada.cafe") == true
    }
    
    // Track which media items are using waigwe
    fun markAsWaigweMedia(mediaId: String) {
        waigweMediaIds.add(mediaId)
        addLog("Marked as waigwe: ${mediaId.take(20)}...")
    }
    
    fun isWaigweMedia(mediaId: String?): Boolean {
        return mediaId != null && waigweMediaIds.contains(mediaId)
    }
    
    // Clear cache (for settings)
    fun clearCache() {
        searchCache.clear()
        streamUrlCache.clear()
        retryCounts.clear()
        addLog("Cache cleared")
    }
    
    // Get logs for debugging
    fun getLogs(): List<String> {
        return fallbackLogs.toList()
    }
    
    // Clear logs
    fun clearLogs() {
        fallbackLogs.clear()
    }
    
    // Internal logging
    private fun addLog(message: String) {
        val timestamp = System.currentTimeMillis()
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        val logEntry = "[$time] $message"
        
        fallbackLogs.add(0, logEntry)
        
        // Keep only recent logs
        if (fallbackLogs.size > MAX_LOGS) {
            fallbackLogs.subList(MAX_LOGS, fallbackLogs.size).clear()
        }
    }
}
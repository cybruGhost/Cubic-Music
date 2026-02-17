package it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import androidx.media3.common.MediaItem
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.knighthat.utils.isNetworkAvailable
import timber.log.Timber
import it.fast4x.innertube.Innertube
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CubicJamManager(
    private val context: Context,
    private val getToken: () -> String?,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val BASE_URL = "https://dkbvirgavjojuyaazzun.supabase.co/functions/v1"
    }

    private var lastToken: String? = null
    private var lastMediaItem: MediaItem? = null
    private var isStopped = false
    private var refreshJob: Job? = null
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL = 15000L // 15 seconds
    
    // Flag to indicate if user has explicitly logged out
    private var isUserLoggedOut = false

    // Public method for explicit logout - this should be called when user clicks logout
    fun logout() {
        isUserLoggedOut = true
        val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        lastToken = null
        Timber.tag("CubicJam").d("User explicitly logged out, data cleared")
    }

    // Token refresh helper for manager - FIXED to NOT delete data on refresh failure
    private suspend fun ensureValidTokenForManager(): String? {
        // If user explicitly logged out, don't attempt anything
        if (isUserLoggedOut) {
            return null
        }
        
        val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
        val currentToken = prefs.getString("bearer_token", null)
        val refreshToken = prefs.getString("refresh_token", null)
        val refreshAt = prefs.getLong("refresh_at", 0L)
        val currentTime = System.currentTimeMillis() / 1000

        // Check if token needs refresh
        if (currentToken != null && refreshToken != null && currentTime >= refreshAt) {
            try {
                Timber.tag("CubicJam").d("Token expired or near expiry, refreshing...")
                val result = refreshToken(refreshToken, context)
                
                result.fold(
                    onSuccess = { authResponse ->
                        if (authResponse.success) {
                            lastToken = authResponse.session.access_token
                            Timber.tag("CubicJam").d("✅ Token refreshed successfully")
                            return authResponse.session.access_token
                        } else {
                            Timber.tag("CubicJam").w("Token refresh returned unsuccessful but not failed")
                            // Don't delete data, just return current token and hope it works
                            return currentToken
                        }
                    },
                    onFailure = { e ->
                        // LOG ONLY - DON'T DELETE DATA
                        Timber.tag("CubicJam").e(e, "Failed to refresh token, but keeping user data")
                        // Return the current token - it might still work
                        return currentToken
                    }
                )
            } catch (e: Exception) {
                // LOG ONLY - DON'T DELETE DATA
                Timber.tag("CubicJam").e(e, "Error refreshing token, but keeping user data")
                return currentToken
            }
        }
        
        return currentToken
    }

    // Helper to make authenticated requests with auto-retry on 401
    private suspend fun <T> makeAuthenticatedRequest(
        request: suspend (String) -> T
    ): Result<T> {
        return runCatching {
            // First ensure we have a valid token
            val token = ensureValidTokenForManager() ?: throw IllegalStateException("Not authenticated")
            
            // Try the request
            request(token)
        }.recoverCatching { originalError ->
            // If request failed with 401, try to refresh token and retry once
            if (originalError.message?.contains("401") == true || 
                originalError.message?.contains("unauthorized", ignoreCase = true) == true) {
                
                Timber.tag("CubicJam").d("Got 401, attempting token refresh...")
                val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
                val refreshToken = prefs.getString("refresh_token", null)
                
                if (refreshToken != null) {
                    try {
                        val refreshResult = refreshToken(refreshToken, context)
                        refreshResult.fold(
                            onSuccess = { authResponse ->
                                if (authResponse.success) {
                                    // Retry the original request with new token
                                    Timber.tag("CubicJam").d("Retrying request with new token")
                                    val newToken = authResponse.session.access_token
                                    return@recoverCatching request(newToken)
                                } else {
                                    Timber.tag("CubicJam").w("Token refresh unsuccessful, but keeping data")
                                    // Don't throw, just rethrow original error
                                }
                            },
                            onFailure = { e ->
                                // LOG ONLY - DON'T DELETE DATA
                                Timber.tag("CubicJam").e(e, "Failed to refresh token on 401, but keeping user data")
                            }
                        )
                    } catch (e: Exception) {
                        // LOG ONLY - DON'T DELETE DATA
                        Timber.tag("CubicJam").e(e, "Token refresh failed on 401, but keeping user data")
                    }
                }
            }
            throw originalError // Re-throw if we couldn't recover
        }
    }

    fun onPlayingStateChanged(
        mediaItem: MediaItem?,
        isPlaying: Boolean,
        position: Long = 0L,
        duration: Long = 0L,
        now: Long = System.currentTimeMillis(),
        getCurrentPosition: (() -> Long)? = null,
        isPlayingProvider: (() -> Boolean)? = null
    ) {
        if (isStopped) return
        
        if (!isNetworkAvailable(context)) {
            Timber.tag("CubicJam").d("No network available")
            return
        }

        refreshJob?.cancel()
        
        lastMediaItem = mediaItem
        
        if (mediaItem == null) {
            sendStoppedActivity()
            return
        }
        
        // Send initial update
        sendActivityUpdate(mediaItem, position, duration, isPlaying)
        
        // Start periodic updates if playing
        if (isPlaying) {
            startPeriodicUpdates(mediaItem, getCurrentPosition, duration, isPlayingProvider)
        }
    }

    private fun sendActivityUpdate(mediaItem: MediaItem, position: Long, duration: Long, isPlaying: Boolean) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle updates
        if (currentTime - lastUpdateTime < 5000 && isPlaying) {
            return
        }
        
        lastUpdateTime = currentTime
        
        externalScope.launch {
            try {
                val activityData = createActivityData(mediaItem, position, duration, isPlaying)
                
                Timber.tag("CubicJam").d("Sending activity: ${activityData.title} by ${activityData.artist}")
                
                makeAuthenticatedRequest { token ->
                    Innertube.client.post("$BASE_URL/update-activity") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(activityData)
                    }
                }.fold(
                    onSuccess = { response ->
                        if (response.status.value in 200..299) {
                            Timber.tag("CubicJam").d("✅ Activity updated successfully")
                        } else {
                            val error = try {
                                response.body<String>()
                            } catch (e: Exception) {
                                "Failed to parse error"
                            }
                            Timber.tag("CubicJam").e("❌ Activity update failed: ${response.status}, $error")
                        }
                    },
                    onFailure = { e ->
                        Timber.tag("CubicJam").e(e, "❌ Failed to update activity: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("CubicJam").e(e, "❌ Network error: ${e.message}")
            }
        }
    }

    private suspend fun createActivityData(
        mediaItem: MediaItem,
        position: Long,
        duration: Long,
        isPlaying: Boolean
    ): ActivityUpdate {
        val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown Title"
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
        val album = mediaItem.mediaMetadata.albumTitle?.toString()
        val artworkUri = mediaItem.mediaMetadata.artworkUri
        
        val trackId = mediaItem.mediaId ?: "unknown_${System.currentTimeMillis()}"
        
        val artworkUrl = if (artworkUri?.scheme?.startsWith("http") == true) {
            artworkUri.toString()
        } else {
            null
        }
        
        return ActivityUpdate(
            track_id = trackId,
            title = title,
            artist = artist,
            album = album,
            artwork_url = artworkUrl,
            duration_ms = duration,
            position_ms = position,
            is_playing = isPlaying
        )
    }

    private fun sendStoppedActivity() {
        externalScope.launch {
            Timber.tag("CubicJam").d("Media stopped, sending clear activity")
            
            try {
                makeAuthenticatedRequest { token ->
                    Innertube.client.post("$BASE_URL/clear-activity") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }
                }.fold(
                    onSuccess = { response ->
                        if (response.status.value in 200..299) {
                            Timber.tag("CubicJam").d("✅ Activity cleared successfully")
                        }
                    },
                    onFailure = { e ->
                        Timber.tag("CubicJam").e(e, "Failed to clear activity")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("CubicJam").e(e, "Error in sendStoppedActivity")
            }
        }
    }

    private fun startPeriodicUpdates(
        mediaItem: MediaItem,
        getCurrentPosition: (() -> Long)?,
        duration: Long,
        isPlayingProvider: (() -> Boolean)?
    ) {
        refreshJob = externalScope.launch {
            while (isActive && !isStopped) {
                delay(UPDATE_INTERVAL)
                if (!isNetworkAvailable(context)) {
                    continue
                }
                val isPlaying = isPlayingProvider?.invoke() ?: true
                val currentPosition = getCurrentPosition?.invoke() ?: 0L
                sendActivityUpdate(mediaItem, currentPosition, duration, isPlaying)
            }
        }
    }

    fun onStop() {
        isStopped = true
        refreshJob?.cancel()
        externalScope.launch {
            sendStoppedActivity()
        }
    }
}

// Authentication functions (keep your existing ones but fix them)
suspend fun login(email: String, password: String, context: Context): Result<AuthResponse> {
    return runCatching {
        val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/mobile-auth") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        
        val authResponse = response.body<AuthResponse>()
        
        if (authResponse.success) {
            val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("bearer_token", authResponse.session.access_token)
                putString("refresh_token", authResponse.session.refresh_token)
                putLong("expires_at", authResponse.session.expires_at)
                // Set refresh time to 5 minutes before expiry, or use server-provided refresh_at
                val refreshTime = authResponse.session.refresh_at ?: 
                    (authResponse.session.expires_at - 300) // 5 minutes before
                putLong("refresh_at", refreshTime)
                putString("user_id", authResponse.user?.id ?: "")
                putString("profile_id", authResponse.user?.profile_id ?: "")
                putString("username", authResponse.user?.username ?: "")
                putString("email", authResponse.user?.email ?: "")
                putString("display_name", authResponse.user?.display_name ?: "")
                putString("friend_code", authResponse.user?.friend_code ?: "")
                putBoolean("is_enabled", true)
                apply()
            }
            Timber.tag("CubicJam").d("✅ Login successful for ${authResponse.user?.email}")
        }
        
        authResponse
    }
}

suspend fun signup(email: String, password: String, username: String, context: Context): Result<AuthResponse> {
    return runCatching {
        val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/mobile-auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(SignupRequest(email, password, username))
        }
        
        val authResponse = response.body<AuthResponse>()
        
        if (authResponse.success) {
            val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("bearer_token", authResponse.session.access_token)
                putString("refresh_token", authResponse.session.refresh_token)
                putLong("expires_at", authResponse.session.expires_at)
                // Set refresh time to 5 minutes before expiry
                val refreshTime = authResponse.session.refresh_at ?: 
                    (authResponse.session.expires_at - 300) // 5 minutes before
                putLong("refresh_at", refreshTime)
                putString("user_id", authResponse.user?.id ?: "")
                putString("profile_id", authResponse.user?.profile_id ?: "")
                putString("username", authResponse.user?.username ?: "")
                putString("email", authResponse.user?.email ?: "")
                putString("display_name", authResponse.user?.display_name ?: "")
                putString("friend_code", authResponse.user?.friend_code ?: "")
                putBoolean("is_enabled", true)
                apply()
            }
            Timber.tag("CubicJam").d("✅ Signup successful for ${authResponse.user?.email}")
        }
        
        authResponse
    }
}

suspend fun refreshToken(refreshToken: String, context: Context): Result<AuthResponse> {
    return runCatching {
        val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/mobile-auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refresh_token" to refreshToken))
        }
        
        val authResponse = response.body<AuthResponse>()
        
        if (authResponse.success) {
            val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("bearer_token", authResponse.session.access_token)
                putString("refresh_token", authResponse.session.refresh_token)
                putLong("expires_at", authResponse.session.expires_at)
                // Use server-provided refresh_at or calculate it
                val refreshTime = authResponse.session.refresh_at ?: 
                    (authResponse.session.expires_at - 300) // 5 minutes before
                putLong("refresh_at", refreshTime)
                apply()
            }
            Timber.tag("CubicJam").d("✅ Token refreshed successfully")
        }
        
        authResponse
    }
}
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

@Serializable
data class AuthResponse(
    val success: Boolean,
    val user: UserInfo,
    val session: SessionInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val profile_id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val friend_code: String
)

@Serializable
data class SessionInfo(
    val access_token: String,
    val refresh_token: String,
    val expires_at: Long,
    val expires_in: Int
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class ActivityUpdate(
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artwork_url: String? = null,
    val duration_ms: Long,
    val position_ms: Long,
    val is_playing: Boolean
)

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

    fun onPlayingStateChanged(
        mediaItem: MediaItem?,
        isPlaying: Boolean,
        position: Long = 0L,
        duration: Long = 0L,
        now: Long = System.currentTimeMillis(), // Added this parameter
        getCurrentPosition: (() -> Long)? = null,
        isPlayingProvider: (() -> Boolean)? = null
    ) {
        if (isStopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return

        if (!isNetworkAvailable(context)) {
            Timber.tag("CubicJam").d("No network available")
            return
        }

        refreshJob?.cancel()
        
        if (token != lastToken) {
            lastToken = token
        }

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
        val token = lastToken ?: return
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
                
                val response = Innertube.client.post("$BASE_URL/update-activity") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(activityData)
                }

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
            Timber.tag("CubicJam").d("Media stopped, not sending update")
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
    }
}

// Authentication functions
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
                putString("user_id", authResponse.user.id)
                putString("profile_id", authResponse.user.profile_id)
                putString("username", authResponse.user.username)
                putString("email", authResponse.user.email)
                putString("display_name", authResponse.user.display_name)
                putString("friend_code", authResponse.user.friend_code)
                putBoolean("is_enabled", true)
                apply()
            }
            Timber.tag("CubicJam").d("✅ Login successful for ${authResponse.user.email}")
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
                putString("user_id", authResponse.user.id)
                putString("profile_id", authResponse.user.profile_id)
                putString("username", authResponse.user.username)
                putString("email", authResponse.user.email)
                putString("display_name", authResponse.user.display_name)
                putString("friend_code", authResponse.user.friend_code)
                putBoolean("is_enabled", true)
                apply()
            }
            Timber.tag("CubicJam").d("✅ Signup successful for ${authResponse.user.email}")
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
                apply()
            }
            Timber.tag("CubicJam").d("✅ Token refreshed successfully")
        }
        
        authResponse
    }
}
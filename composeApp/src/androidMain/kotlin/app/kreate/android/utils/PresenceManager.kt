package it.fast4x.rimusic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.media3.common.MediaItem
import it.fast4x.rimusic.models.SharedPlaybackState
import it.fast4x.rimusic.models.UserPresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


object PresenceManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _currentPresence = MutableStateFlow<UserPresence?>(null)
    val currentPresence: StateFlow<UserPresence?> = _currentPresence.asStateFlow()

    private val _friendsPresence = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val friendsPresence: StateFlow<Map<String, UserPresence>> = _friendsPresence.asStateFlow()

    private val _playbackState = MutableStateFlow<SharedPlaybackState?>(null)
    val playbackState: StateFlow<SharedPlaybackState?> = _playbackState.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun initialize(context: Context, userId: String) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Monitor network connectivity
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                updatePresenceStatus(userId, true)
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                updatePresenceStatus(userId, false)
            }
        }

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)

        checkInitialConnectivity()
    }

    private fun checkInitialConnectivity() {
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun updatePlaybackState(
        mediaItem: MediaItem,
        position: Long,
        duration: Long,
        isPlaying: Boolean,
        queuePosition: Int,
        artistName: String = "",
        albumId: String? = null
    ) {
        val state = SharedPlaybackState(
            mediaId = mediaItem.mediaId,
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = artistName.ifEmpty {
                mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
            },
            albumId = albumId,
            artworkUri = mediaItem.mediaMetadata.artworkUri?.toString(),
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            queuePosition = queuePosition
        )

        _playbackState.value = state

        scope.launch {
            updateCurrentPresence(state)
        }
    }

    private fun updateCurrentPresence(playbackState: SharedPlaybackState) {
        val currentUserId = _currentPresence.value?.userId ?: return

        val presence = UserPresence(
            userId = currentUserId,
            isOnline = _isOnline.value,
            currentSongId = playbackState.mediaId,
            currentSongTitle = playbackState.title,
            currentArtist = playbackState.artist,
            playlistId = null, // TODO: Add playlist tracking
            isPlaying = playbackState.isPlaying,
            timestamp = System.currentTimeMillis()
        )

        _currentPresence.value = presence

        sharePresenceWithFriends(presence)
    }

    private fun updatePresenceStatus(userId: String, isOnline: Boolean) {
        val current = _currentPresence.value
        if (current != null && current.userId == userId) {
            _currentPresence.value = current.copy(
                isOnline = isOnline,
                timestamp = System.currentTimeMillis()
            )
        } else {
            _currentPresence.value = UserPresence(
                userId = userId,
                isOnline = isOnline
            )
        }
    }

    private fun sharePresenceWithFriends(presence: UserPresence) {
        scope.launch {
            // FirebaseDatabase.reference.child("presence/${presence.userId}").setValue(presence)
            println("Sharing presence: $presence")
        }
    }

    fun updateFriendPresence(friendId: String, presence: UserPresence) {
        val current = _friendsPresence.value.toMutableMap()
        current[friendId] = presence
        _friendsPresence.value = current
    }

    fun removeFriendPresence(friendId: String) {
        val current = _friendsPresence.value.toMutableMap()
        current.remove(friendId)
        _friendsPresence.value = current
    }

    fun cleanup() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        scope.launch {
            val userId = _currentPresence.value?.userId
            if (userId != null) {
                updatePresenceStatus(userId, false)
            }
        }
    }
}
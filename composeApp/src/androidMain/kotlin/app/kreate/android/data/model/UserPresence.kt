package it.fast4x.rimusic.models

data class UserPresence(
    val userId: String,
    val isOnline: Boolean,
    val currentSongId: String? = null,
    val currentSongTitle: String? = null,
    val currentArtist: String? = null,
    val playlistId: String? = null,
    val isPlaying: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class SharedPlaybackState(
    val mediaId: String,
    val title: String,
    val artist: String,
    val albumId: String?,
    val artworkUri: String?,
    val position: Long,
    val duration: Long,
    val isPlaying: Boolean,
    val queuePosition: Int
)
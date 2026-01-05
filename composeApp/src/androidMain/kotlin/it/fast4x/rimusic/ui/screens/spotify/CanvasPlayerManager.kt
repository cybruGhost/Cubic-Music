
package it.fast4x.rimusic.ui.screens.spotify

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import timber.log.Timber

object CanvasPlayerManager {
    private var currentPlayer: ExoPlayer? = null
    private var currentPlayerView: PlayerView? = null
    private var currentCanvasUrl: String? = null
    private var currentMediaItemId: String? = null
    private var isPlayerActive = false
    private var lastSetupTime = 0L
    
    // Memory optimization
    private const val PLAYER_RECYCLE_THRESHOLD = 2000L // Reduced for faster switching
    
    fun getCurrentCanvasUrl(): String? = currentCanvasUrl
    fun getCurrentMediaItemId(): String? = currentMediaItemId
    
    fun isPlayingForMediaItem(mediaItemId: String?): Boolean {
        return currentMediaItemId == mediaItemId && currentCanvasUrl != null && isPlayerActive
    }
    
    fun setupPlayer(
        context: Context,
        canvasUrl: String,
        isPlaying: Boolean,
        mediaItemId: String? = null
    ): PlayerView {
        val now = System.currentTimeMillis()
        
        // Check if we can reuse existing player (same media and within threshold)
        val shouldReuse = currentCanvasUrl == canvasUrl && 
                         currentMediaItemId == mediaItemId &&
                         isPlayerActive &&
                         (now - lastSetupTime) < PLAYER_RECYCLE_THRESHOLD &&
                         currentPlayer != null
        
        if (shouldReuse && currentPlayerView != null) {
            Timber.d("CanvasPlayer: Reusing player for mediaId: ${mediaItemId?.take(8)}")
            currentPlayer?.playWhenReady = isPlaying
            currentPlayer?.repeatMode = if (isPlaying) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            return currentPlayerView!!
        }
        
        // Release old player if exists and not the same
        if (currentCanvasUrl != canvasUrl || currentMediaItemId != mediaItemId) {
            stopAndClear()
        }
        
        Timber.d("CanvasPlayer: Creating new player for mediaId: ${mediaItemId?.take(8)}")
        
        val player = ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(15000)
            .setSeekBackIncrementMs(5000)
            .build()
        
        val mediaItem = MediaItem.Builder()
            .setUri(canvasUrl)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        
        player.setMediaItem(mediaItem)
        player.playWhenReady = isPlaying
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        player.repeatMode = if (isPlaying) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        
        // Error listener
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Timber.e("CanvasPlayer error: ${error.message}")
                stopAndClear()
            }
        })
        
        val playerView = PlayerView(context).apply {
            this.player = player
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        
        // Prepare but don't auto-start if not playing
        player.prepare()
        if (!isPlaying) {
            player.pause()
        }
        
        currentPlayer = player
        currentPlayerView = playerView
        currentCanvasUrl = canvasUrl
        currentMediaItemId = mediaItemId
        isPlayerActive = true
        lastSetupTime = now
        
        Timber.d("CanvasPlayer: New player setup complete for: ${mediaItemId?.take(8)}")
        
        return playerView
    }
    
    fun stopAndClear() {
        Timber.d("CanvasPlayer: Stopping and clearing player")
        
        currentPlayer?.let { player ->
            player.stop()
            player.release()
        }
        
        currentPlayer = null
        currentPlayerView = null
        currentCanvasUrl = null
        currentMediaItemId = null
        isPlayerActive = false
        
        Timber.d("CanvasPlayer: Cleanup complete")
    }
    
    fun stopAndClearForNewSong() {
        Timber.d("CanvasPlayer: Clearing player for new song")
        
        currentPlayer?.let { player ->
            player.stop()
            player.release()
        }
        
        currentPlayer = null
        currentPlayerView = null
        currentCanvasUrl = null
        // Keep currentMediaItemId so we know which song we're fetching for
        isPlayerActive = false
        
        Timber.d("CanvasPlayer: Ready for new song")
    }
    
    fun releasePlayer() {
        stopAndClear()
    }
    
    fun updatePlayState(isPlaying: Boolean) {
        currentPlayer?.let { player ->
            player.playWhenReady = isPlaying
            player.repeatMode = if (isPlaying) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            
            if (isPlaying && !player.isPlaying) {
                player.play()
                Timber.d("CanvasPlayer: Started playing")
            } else if (!isPlaying) {
                player.pause()
                Timber.d("CanvasPlayer: Paused")
            }
        }
    }
    
    fun stopLooping() {
        currentPlayer?.let { player ->
            player.repeatMode = Player.REPEAT_MODE_OFF
            Timber.d("CanvasPlayer: Loop stopped")
        }
    }
    
    fun isActive(): Boolean = isPlayerActive
    
    fun forceCleanup() {
        stopAndClear()
        Timber.d("CanvasPlayer: Force cleanup complete")
    }
}


package app.it.fast4x.rimusic.service.modern

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.utils.mediaItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.reflect.Method
import kotlin.math.pow

data class CrossfadeUiState(
    val isEnabled: Boolean = false,
    val isActive: Boolean = false,
    val isHighlightActive: Boolean = false,
    val displayPosition: Long = 0L,
    val displayDuration: Long = 0L,
    val displayMediaItem: MediaItem? = null,
)

@UnstableApi
class CrossFadeMediaPlayer(
    private var currentPlayer: ExoPlayer,
    private var nextPlayer: ExoPlayer,
    private val targetVolumeProvider: () -> Float,
    private val onPlayersSwapped: (ExoPlayer, ExoPlayer) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val monitorIntervalMs = 100L
    private val preloadThreshold = 0.72f
    private val readyTimeoutMs = 2_000L
    private val crossfadeCompletionGraceMs = 250L
    private val outgoingCompletionThreshold = 0.03f
    private val earlyStartLeadInMs = 1_800L
    private val incomingStartVolumeRatio = 0.05f
    private val outgoingHoldPortion = 0f

    private var enabled = false
    private var crossfadeDurationMs = 0L
    private var preloadedIndex = C.INDEX_UNSET
    private var preloadedMediaId: String? = null
    private var waitingReadySinceMs = 0L
    private var skippedMediaId: String? = null
    private var isCrossfading = false
    private var crossfadeStartedAtMs = 0L
    private var activeCrossfadeDurationMs = 0L
    private var baseVolume = 1f
    private var isMonitoring = false
    private var pauseAtEndMethod: Method? = null
    private val _uiState = MutableStateFlow(CrossfadeUiState())
    val uiState: StateFlow<CrossfadeUiState> = _uiState
    var onDisplayItemChanged: ((MediaItem?) -> Unit)? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            tick()
            handler.postDelayed(this, monitorIntervalMs)
        }
    }

    init {
        nextPlayer.repeatMode = Player.REPEAT_MODE_OFF
        nextPlayer.playWhenReady = false
        nextPlayer.volume = 0f
        pauseAtEndMethod = runCatching {
            ExoPlayer::class.java.getMethod("setPauseAtEndOfMediaItems", Boolean::class.javaPrimitiveType)
        }.getOrNull()
    }

    fun updateConfig(
        enabled: Boolean,
        crossfadeDurationMs: Long,
    ) {
        this.enabled = enabled
        this.crossfadeDurationMs = crossfadeDurationMs.coerceAtLeast(0L)

        if (!enabled || this.crossfadeDurationMs <= 0L) {
            cancelCrossfade()
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    fun onPrimaryTimelineChanged() {
        val preloadedIndex = preloadedIndex
        if (preloadedIndex == C.INDEX_UNSET || preloadedIndex >= currentPlayer.mediaItemCount) {
            resetNextPlayer()
            return
        }

        val expectedMediaId = runCatching {
            currentPlayer.getMediaItemAt(preloadedIndex).mediaId
        }.getOrNull()
        if (expectedMediaId != preloadedMediaId) {
            resetNextPlayer()
        }
    }

    fun onPrimaryMediaItemTransition(mediaItem: MediaItem?) {
        skippedMediaId = null
        waitingReadySinceMs = 0L
        baseVolume = targetVolumeProvider()

        if (mediaItem == null) {
            resetNextPlayer()
            updateUiState()
            return
        }

        if (mediaItem.mediaId != preloadedMediaId && !isCrossfading) {
            resetNextPlayer()
        }

        updateUiState()
    }

    fun onPrimaryIsPlayingChanged(isPlaying: Boolean) {
        if (!isCrossfading) return

        if (isPlaying) {
            if (nextPlayer.playbackState == Player.STATE_READY) {
                nextPlayer.play()
            }
        } else {
            nextPlayer.pause()
        }
    }

    fun onPrimaryPlayWhenReadyChanged(playWhenReady: Boolean) {
        if (!isCrossfading) return

        if (playWhenReady) {
            if (nextPlayer.playbackState == Player.STATE_READY) {
                nextPlayer.play()
            }
        } else {
            nextPlayer.pause()
        }
    }

    fun pauseForUiInteraction() {
        nextPlayer.pause()
    }

    fun playForUiInteraction() {
        if (isCrossfading && nextPlayer.playbackState == Player.STATE_READY) {
            nextPlayer.play()
        }
    }

    fun onPrimaryPositionDiscontinuity(reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SKIP) {
            cancelCrossfade()
            if (currentPlayer.playWhenReady) {
                if (currentPlayer.playbackState == Player.STATE_IDLE) {
                    currentPlayer.prepare()
                }
                currentPlayer.play()
            }
        }
    }

    fun release() {
        stopMonitoring()
        nextPlayer.release()
    }

    private fun tick() {
        val currentMediaId = currentPlayer.currentMediaItem?.mediaId
        if (currentMediaId == null) {
            if (!isCrossfading) {
                resetNextPlayer()
            }
            updateUiState()
            return
        }

        if (!enabled || crossfadeDurationMs <= 0L || currentPlayer.mediaItemCount < 2) {
            cancelCrossfade()
            return
        }

        if (isCrossfading) {
            updateCrossfade()
            return
        }

        updateUiState()

        val nextIndex = currentPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) {
            resetNextPlayer()
            return
        }

        val duration = currentPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return

        val currentPosition = currentPlayer.currentPosition.coerceAtLeast(0L)
        val remaining = (duration - currentPosition).coerceAtLeast(0L)
        val preloadStartPosition = (duration * preloadThreshold).toLong()

        val shouldPreload = currentPosition >= preloadStartPosition
        if (shouldPreload) {
            ensureSecondaryPrepared(nextIndex)
        }

        val effectiveCrossfadeWindowMs = crossfadeDurationMs + earlyStartLeadInMs
        if (
            currentPosition < preloadStartPosition ||
            remaining > effectiveCrossfadeWindowMs ||
            skippedMediaId == currentMediaId
        ) {
            return
        }

        ensureSecondaryPrepared(nextIndex)

        if (nextPlayer.playbackState == Player.STATE_READY) {
            startCrossfade(effectiveCrossfadeWindowMs)
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (waitingReadySinceMs == 0L) {
            waitingReadySinceMs = now
        }

        if (now - waitingReadySinceMs >= readyTimeoutMs) {
            skippedMediaId = currentMediaId
            waitingReadySinceMs = 0L
        }
    }

    private fun ensureSecondaryPrepared(nextIndex: Int) {
        val mediaItem = currentPlayer.getMediaItemAt(nextIndex)
        if (preloadedIndex == nextIndex && preloadedMediaId == mediaItem.mediaId) return

        isCrossfading = false
        waitingReadySinceMs = 0L

        val queue = buildQueueSnapshot(currentPlayer)
        nextPlayer.stop()
        nextPlayer.clearMediaItems()
        nextPlayer.setMediaItems(queue, nextIndex, 0L)
        nextPlayer.prepare()
        nextPlayer.playWhenReady = false
        nextPlayer.volume = 0f
        nextPlayer.playbackParameters = currentPlayer.playbackParameters
        nextPlayer.repeatMode = currentPlayer.repeatMode
        nextPlayer.shuffleModeEnabled = currentPlayer.shuffleModeEnabled

        preloadedIndex = nextIndex
        preloadedMediaId = mediaItem.mediaId
    }

    private fun startCrossfade(durationMs: Long) {
        if (isCrossfading) return

        waitingReadySinceMs = 0L
        isCrossfading = true
        crossfadeStartedAtMs = SystemClock.elapsedRealtime()
        activeCrossfadeDurationMs = durationMs.coerceAtLeast(1L)
        baseVolume = targetVolumeProvider()
        currentPlayer.volume = baseVolume
        setPauseAtEndOfMediaItems(currentPlayer, true)

        nextPlayer.seekTo(preloadedIndex, 0L)
        nextPlayer.volume = 0f
        nextPlayer.playbackParameters = currentPlayer.playbackParameters

        if (currentPlayer.playWhenReady) {
            nextPlayer.play()
        } else {
            nextPlayer.pause()
        }
    }

    private fun updateCrossfade() {
        if (!isCrossfading) return

        if (!currentPlayer.playWhenReady) {
            nextPlayer.pause()
            return
        }

        if (!nextPlayer.isPlaying && nextPlayer.playbackState == Player.STATE_READY) {
            nextPlayer.play()
        }

        val elapsed = SystemClock.elapsedRealtime() - crossfadeStartedAtMs
        val fraction = (elapsed.toFloat() / activeCrossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
        val normalizedProgress = ((fraction - outgoingHoldPortion) / (1f - outgoingHoldPortion))
            .coerceIn(0f, 1f)
        val smoothProgress = normalizedProgress * normalizedProgress * (3f - 2f * normalizedProgress)
        val fadeOut = (1f - smoothProgress).pow(1.55f)
        val fadeIn = incomingStartVolumeRatio +
            ((1f - incomingStartVolumeRatio) * smoothProgress.pow(1.15f))
        val currentDuration = currentPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        val currentRemaining = currentDuration?.let { duration ->
            (duration - currentPlayer.currentPosition.coerceAtLeast(0L)).coerceAtLeast(0L)
        }
        val remainingFraction = currentDuration?.takeIf { it > 0L }?.let { duration ->
            currentRemaining?.toFloat()?.div(duration.toFloat())
        } ?: 1f

        currentPlayer.volume = (baseVolume * fadeOut).coerceIn(0f, baseVolume)
        nextPlayer.volume = (baseVolume * fadeIn).coerceIn(0f, baseVolume)
        updateUiState(elapsed)

        if (
            fraction >= 1f ||
            (currentRemaining != null && currentRemaining <= crossfadeCompletionGraceMs) ||
            remainingFraction <= outgoingCompletionThreshold ||
            currentPlayer.playbackState == Player.STATE_ENDED
        ) {
            completeCrossfade()
        }
    }

    private fun completeCrossfade() {
        val outgoingPlayer = currentPlayer
        val incomingPlayer = nextPlayer
        val shouldKeepPlaying = outgoingPlayer.playWhenReady || outgoingPlayer.isPlaying
        isCrossfading = false
        activeCrossfadeDurationMs = 0L

        incomingPlayer.volume = baseVolume
        incomingPlayer.repeatMode = outgoingPlayer.repeatMode
        incomingPlayer.shuffleModeEnabled = outgoingPlayer.shuffleModeEnabled
        incomingPlayer.playbackParameters = outgoingPlayer.playbackParameters
        incomingPlayer.playWhenReady = shouldKeepPlaying

        currentPlayer = incomingPlayer
        nextPlayer = outgoingPlayer
        setPauseAtEndOfMediaItems(currentPlayer, false)
        runCatching {
            if (currentPlayer.playbackState == Player.STATE_IDLE) {
                currentPlayer.prepare()
            }
            if (shouldKeepPlaying && !currentPlayer.isPlaying) {
                currentPlayer.play()
            }
        }
        runCatching {
            nextPlayer.pause()
            nextPlayer.stop()
            nextPlayer.clearMediaItems()
            nextPlayer.volume = 0f
            setPauseAtEndOfMediaItems(nextPlayer, false)
        }
        waitingReadySinceMs = 0L
        skippedMediaId = null
        preloadedIndex = C.INDEX_UNSET
        preloadedMediaId = null
        onPlayersSwapped(currentPlayer, nextPlayer)
        updateUiState()
    }

    private fun cancelCrossfade() {
        val shouldRestoreCurrentVolume = isCrossfading
        isCrossfading = false
        waitingReadySinceMs = 0L
        if (shouldRestoreCurrentVolume) {
            currentPlayer.volume = baseVolume
        }
        setPauseAtEndOfMediaItems(currentPlayer, false)
        setPauseAtEndOfMediaItems(nextPlayer, false)
        activeCrossfadeDurationMs = 0L
        resetNextPlayer()
        updateUiState()
    }

    private fun resetNextPlayer() {
        nextPlayer.pause()
        nextPlayer.stop()
        nextPlayer.clearMediaItems()
        nextPlayer.volume = 0f
        setPauseAtEndOfMediaItems(nextPlayer, false)
        preloadedIndex = C.INDEX_UNSET
        preloadedMediaId = null
    }

    private fun buildQueueSnapshot(player: ExoPlayer): List<MediaItem> =
        List(player.mediaItemCount) { index -> player.getMediaItemAt(index) }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        handler.post(monitorRunnable)
    }

    private fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }

    private fun updateUiState(crossfadeElapsedMs: Long = 0L) {
        if (!enabled) {
            _uiState.value = CrossfadeUiState()
            return
        }

        val displayPlayer = if (isCrossfading) nextPlayer else currentPlayer
        val fallbackPlayer = if (isCrossfading) currentPlayer else nextPlayer
        val rawDisplayMediaItem = displayPlayer.currentMediaItem ?: fallbackPlayer.currentMediaItem
        val normalizedDisplayMediaItem = normalizeDisplayMediaItem(rawDisplayMediaItem)
        val duration = (displayPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: fallbackPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: if (normalizedDisplayMediaItem != null) 1L else 0L)
        val position = displayPlayer.currentPosition
            .coerceAtLeast(0L)
            .coerceIn(0L, duration.coerceAtLeast(1L))

        val previousDisplayItem = _uiState.value.displayMediaItem
        val stableDisplayMediaItem = if (
            previousDisplayItem != null &&
            normalizedDisplayMediaItem != null &&
            previousDisplayItem.mediaId == normalizedDisplayMediaItem.mediaId
        ) {
            previousDisplayItem
        } else {
            normalizedDisplayMediaItem
        }
        _uiState.value = CrossfadeUiState(
            isEnabled = true,
            isActive = isCrossfading,
            isHighlightActive = isCrossfading && crossfadeElapsedMs <= activeCrossfadeDurationMs.coerceAtMost(10_000L),
            displayPosition = position,
            displayDuration = duration,
            displayMediaItem = stableDisplayMediaItem,
        )
        if (previousDisplayItem?.mediaId != stableDisplayMediaItem?.mediaId) {
            onDisplayItemChanged?.invoke(stableDisplayMediaItem)
        }
    }

    private fun normalizeDisplayMediaItem(mediaItem: MediaItem?): MediaItem? {
        if (mediaItem == null) return null

        val metadata = mediaItem.mediaMetadata
        val normalizedMetadata = metadata.buildUpon()
            .setTitle(cleanPrefix(cleanPrefix(metadata.title?.toString().orEmpty())))
            .setArtist(cleanPrefix(cleanPrefix(metadata.artist?.toString().orEmpty())))
            .setAlbumTitle(cleanPrefix(cleanPrefix(metadata.albumTitle?.toString().orEmpty())))
            .build()

        return mediaItem.buildUpon()
            .setMediaMetadata(normalizedMetadata)
            .build()
    }

    private fun setPauseAtEndOfMediaItems(player: ExoPlayer, enabled: Boolean) {
        runCatching {
            pauseAtEndMethod?.invoke(player, enabled)
        }
    }
}

package app.it.fast4x.rimusic.service.modern

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import app.it.fast4x.rimusic.service.PlaybackFailureCategory
import app.it.fast4x.rimusic.service.toPlaybackFailureInfo

class PlaybackRecoveryHelper {
    data class Snapshot(
        val mediaId: String?,
        val title: String?,
        val currentPositionMs: Long,
        val bufferedPositionMs: Long,
        val playWhenReady: Boolean,
        val isNetworkAvailable: Boolean,
        val hasNextMediaItem: Boolean,
        val skipOnErrorEnabled: Boolean,
        val lastPlaybackPositionMs: Long,
        val currentRetryCount: Int,
    )

    sealed interface Decision {
        data object None : Decision

        data class WaitForNetwork(
            val mediaId: String,
            val positionMs: Long,
            val message: String,
            val resumeWhenNetworkReturns: Boolean,
        ) : Decision

        data class RetryCurrent(
            val mediaId: String,
            val positionMs: Long,
            val delayMs: Long,
            val message: String,
        ) : Decision

        data class SkipNext(
            val message: String,
        ) : Decision

        data class Pause(
            val message: String,
        ) : Decision
    }

    fun onPlaybackStateChanged(
        playbackState: Int,
        snapshot: Snapshot,
    ): Decision {
        if (
            playbackState == Player.STATE_BUFFERING &&
            !snapshot.isNetworkAvailable &&
            !snapshot.mediaId.isNullOrBlank()
        ) {
            return Decision.WaitForNetwork(
                mediaId = snapshot.mediaId,
                positionMs = snapshot.lastPlaybackPositionMs.takeIf { it > 0L } ?: snapshot.currentPositionMs,
                message = "Connection lost. Cubic is waiting for network before it continues.",
                resumeWhenNetworkReturns = snapshot.playWhenReady,
            )
        }

        return Decision.None
    }

    fun onNetworkRestored(snapshot: Snapshot): Decision {
        val mediaId = snapshot.mediaId ?: return Decision.None
        return Decision.RetryCurrent(
            mediaId = mediaId,
            positionMs = snapshot.lastPlaybackPositionMs.takeIf { it > 0L } ?: snapshot.currentPositionMs,
            delayMs = 450L,
            message = "Connection restored. Retrying playback.",
        )
    }

    fun onPlayerError(
        error: PlaybackException,
        snapshot: Snapshot,
        maxRetries: Int,
    ): Decision {
        val mediaId = snapshot.mediaId
        val title = snapshot.title ?: "this song"
        val failure = error.toPlaybackFailureInfo()
        val resumePosition = snapshot.lastPlaybackPositionMs
            .takeIf { it > 0L }
            ?: snapshot.currentPositionMs

        if (
            !snapshot.isNetworkAvailable &&
            (failure.category == PlaybackFailureCategory.NETWORK || failure.isRecoverable)
        ) {
            if (mediaId.isNullOrBlank()) return Decision.Pause("Connection issue detected. Please retry playback.")
            return Decision.WaitForNetwork(
                mediaId = mediaId,
                positionMs = resumePosition,
                message = "Network problem while playing $title. Cubic will retry automatically.",
                resumeWhenNetworkReturns = snapshot.playWhenReady,
            )
        }

        if (failure.canRetryCurrent && !mediaId.isNullOrBlank() && snapshot.currentRetryCount < maxRetries) {
            return Decision.RetryCurrent(
                mediaId = mediaId,
                positionMs = resumePosition,
                delayMs = 500L,
                message = "Stream issue on $title. Retrying (${snapshot.currentRetryCount + 1}/$maxRetries).",
            )
        }

        if (
            failure.canRetryCurrent &&
            snapshot.currentRetryCount >= maxRetries &&
            snapshot.skipOnErrorEnabled &&
            snapshot.hasNextMediaItem &&
            snapshot.isNetworkAvailable
        ) {
            return Decision.SkipNext("Couldn't play $title after $maxRetries retries. Skipping to the next track.")
        }

        if (failure.canRetryCurrent && snapshot.currentRetryCount >= maxRetries) {
            return Decision.Pause("Couldn't play $title after $maxRetries retries. Cubic paused instead of looping.")
        }

        if (snapshot.skipOnErrorEnabled && snapshot.hasNextMediaItem) {
            return Decision.SkipNext("Couldn't play $title. Skipping to the next track.")
        }

        return Decision.Pause("Couldn't play $title right now. Check your connection or try another source.")
    }
}

package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import app.it.fast4x.rimusic.service.modern.CrossfadeUiState
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.positionAndDurationState

@Stable
data class PlayerDisplayState(
    val crossfadeUiState: CrossfadeUiState,
    val mediaItem: MediaItem?,
    val shouldBePlaying: Boolean,
    val isBuffering: Boolean,
    val position: Long,
    val duration: Long,
)

@Composable
fun rememberDisplayedPlayerState(
    binder: PlayerServiceModern.Binder,
): PlayerDisplayState {
    val crossfadeUiState by binder.crossfadeUiState.collectAsState()
    val positionAndDuration by binder.player.positionAndDurationState()
    var currentMediaItem by remember {
        mutableStateOf(binder.displayedMediaItem ?: binder.player.currentMediaItem)
    }
    var playbackStateValue by remember {
        mutableIntStateOf(binder.player.playbackState)
    }
    var playWhenReadyState by remember {
        mutableStateOf(binder.player.playWhenReady)
    }
    var isPlaying by remember {
        mutableStateOf(binder.player.isPlaying)
    }
    var playerError by remember {
        mutableStateOf<PlaybackException?>(binder.player.playerError)
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = binder.displayedMediaItem ?: mediaItem
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackStateValue = playbackState
                playerError = binder.player.playerError
                isPlaying = binder.player.isPlaying
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playWhenReadyState = playWhenReady
                isPlaying = binder.player.isPlaying
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = error
            }
        }
    }

    val mediaItem by remember(
        binder,
        crossfadeUiState,
        currentMediaItem,
    ) {
        derivedStateOf {
            if (crossfadeUiState.isEnabled) {
                crossfadeUiState.displayMediaItem
                    ?: binder.displayedMediaItem
                    ?: currentMediaItem
            } else {
                binder.displayedMediaItem ?: currentMediaItem
            }
        }
    }

    val shouldBePlaying by remember(
        binder,
        crossfadeUiState,
        playWhenReadyState,
        isPlaying,
        playerError,
        binder.sessionPlayer.playWhenReady,
        binder.sessionPlayer.isPlaying,
    ) {
        derivedStateOf {
            (binder.sessionPlayer.playWhenReady ||
                binder.sessionPlayer.isPlaying ||
                (playWhenReadyState && crossfadeUiState.isActive) ||
                isPlaying) &&
                playerError == null
        }
    }

    val isBuffering by remember(playbackStateValue) {
        derivedStateOf { playbackStateValue == Player.STATE_BUFFERING }
    }

    val displayedPositionAndDuration by remember(crossfadeUiState, positionAndDuration) {
        derivedStateOf {
            if (crossfadeUiState.isEnabled && crossfadeUiState.displayDuration > 0L) {
                val safeDuration = crossfadeUiState.displayDuration.coerceAtLeast(1L)
                val safePosition = crossfadeUiState.displayPosition.coerceIn(0L, safeDuration)
                safePosition to safeDuration
            } else {
                val safeDuration = positionAndDuration.second.coerceAtLeast(1L)
                val safePosition = positionAndDuration.first.coerceIn(0L, safeDuration)
                safePosition to safeDuration
            }
        }
    }

    return remember(
        crossfadeUiState,
        mediaItem,
        shouldBePlaying,
        isBuffering,
        displayedPositionAndDuration,
    ) {
        PlayerDisplayState(
            crossfadeUiState = crossfadeUiState,
            mediaItem = mediaItem,
            shouldBePlaying = shouldBePlaying,
            isBuffering = isBuffering,
            position = displayedPositionAndDuration.first,
            duration = displayedPositionAndDuration.second,
        )
    }
}

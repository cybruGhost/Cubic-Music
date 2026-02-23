package it.fast4x.rimusic.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.kreate.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.knighthat.utils.Toaster

@Composable
inline fun Player.DisposableListener(crossinline listenerProvider: () -> Player.Listener) {
    DisposableEffect(this) {
        val listener = listenerProvider()
        addListener(listener)
        onDispose { removeListener(listener) }
    }
}

@Composable
fun Player.positionAndDurationState(): State<Pair<Long, Long>> {
    val state = remember {
        mutableStateOf(currentPosition to duration)
    }

    LaunchedEffect(this) {
        var isSeeking = false

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isSeeking = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Use safe values to prevent negative numbers
                val currentPos = currentPosition.coerceAtLeast(0L)
                val currentDur = duration.coerceAtLeast(0L)
                state.value = currentPos to currentDur
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    isSeeking = true
                    // Use safe values to prevent negative numbers
                    val currentPos = currentPosition.coerceAtLeast(0L)
                    val currentDur = duration.coerceAtLeast(0L)
                    state.value = currentPos to currentDur
                }
            }
        }

        addListener(listener)

        val pollJob = launch {
            while (isActive) {
                delay(500)
                if (!isSeeking) {
                    // Use safe values to prevent negative numbers
                    val currentPos = currentPosition.coerceAtLeast(0L)
                    val currentDur = duration.coerceAtLeast(0L)
                    state.value = currentPos to currentDur
                }
            }
        }

        try {
            suspendCancellableCoroutine<Nothing> { }
        } finally {
            pollJob.cancel()
            removeListener(listener)
        }
    }

    return state
}

@Composable
fun rememberEqualizerLauncher(
    audioSessionId: () -> Int?,
    contentType: Int = AudioEffect.CONTENT_TYPE_MUSIC
): State<() -> Unit> {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    return rememberUpdatedState {
        try {
            val sessionId = audioSessionId()
            // Only launch if we have a valid audio session ID
            if (sessionId != null && sessionId != 0) {
                launcher.launch(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        replaceExtras(EqualizerIntentBundleAccessor.bundle {
                            audioSession = sessionId
                            packageName = context.packageName
                            this.contentType = contentType
                        })
                    }
                )
            } else {
                // Show error if no valid audio session
                Toaster.w(R.string.info_not_find_application_audio)
            }
        } catch (e: ActivityNotFoundException) {
            Toaster.w(R.string.info_not_find_application_audio)
        } catch (e: Exception) {
            // Catch any other exceptions to prevent crashes
            Toaster.w(R.string.info_not_find_application_audio)
        }
    }
}

@Composable
fun Player.playbackStateState(): State<Int> {
    val state = remember {
        mutableStateOf(playbackState)
    }

    DisposableListener {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.value = playbackState
            }
        }
    }

    return state
}

/**
 * Safe utility functions to prevent the "Out of range" IllegalArgumentException
 * These can be used throughout the app where player position/duration calculations are done
 */

/**
 * Safe version of getBufferedPercentage that handles edge cases and prevents negative values
 */
fun Player.getSafeBufferedPercentage(): Int {
    return try {
        val bufferedPosition = bufferedPosition.coerceAtLeast(0L)
        val currentDuration = duration.coerceAtLeast(0L)
        
        if (currentDuration <= 0L) return 0
        if (bufferedPosition <= 0L) return 0
        
        // Calculate percentage safely
        val percentage = (bufferedPosition * 100 / currentDuration.toDouble()).toInt()
        percentage.coerceIn(0, 100)
    } catch (e: Exception) {
        0 // Return 0% in case of any calculation errors
    }
}

/**
 * Safe version of getCurrentPosition that prevents negative values
 */
fun Player.getSafeCurrentPosition(): Long {
    return currentPosition.coerceAtLeast(0L)
}

/**
 * Safe version of getDuration that prevents negative values  
 */
fun Player.getSafeDuration(): Long {
    return duration.coerceAtLeast(0L)
}

/**
 * Safe seek function that validates parameters
 */
fun Player.safeSeekTo(positionMs: Long) {
    try {
        val safePosition = positionMs.coerceAtLeast(0L)
        seekTo(safePosition)
    } catch (e: Exception) {
        // Log error but don't crash
        android.util.Log.e("PlayerUtils", "Error seeking to position: ${e.message}")
    }
}

/**
 * Safe media item index validation
 */
fun Player.isValidMediaItemIndex(index: Int): Boolean {
    return index in 0 until mediaItemCount
}

/**
 * Get safe media item at index with bounds checking
 */
fun Player.getSafeMediaItemAt(index: Int): MediaItem? {
    return try {
        if (isValidMediaItemIndex(index)) {
            getMediaItemAt(index)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
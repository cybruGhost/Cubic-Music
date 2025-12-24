package it.fast4x.rimusic.utils

import android.animation.ValueAnimator
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode
import kotlin.time.Duration.Companion.seconds

var volume = 0f

private const val FADE_INTERVAL = 100L // ms between volume updates

fun audioFadeOut(player: ExoPlayer, duration: Int, context: Context) {
    val deviceVolume = getDeviceVolume(context)
    val handler = Handler(Looper.getMainLooper())
    
    handler.postDelayed({
        var timeRemaining = duration
        var currentVolume = deviceVolume

        val fadeOutRunnable: Runnable = object : Runnable {
            override fun run() {
                if (timeRemaining > 0) {
                    timeRemaining -= FADE_INTERVAL.toInt()
                    currentVolume = (deviceVolume * timeRemaining) / duration
                    player.volume = currentVolume.coerceAtLeast(0f)
                    //println("mediaItem audioFadeOut: volume $currentVolume $timeRemaining")
                    handler.postDelayed(this, FADE_INTERVAL)
                } else {
                    // Ensure volume is exactly 0 at the end
                    player.volume = 0f
                }
            }
        }
        handler.post(fadeOutRunnable)

    }, FADE_INTERVAL)
}

fun audioFadeIn(player: ExoPlayer, duration: Int, context: Context) {
    val handler = Handler(Looper.getMainLooper())
    
    handler.postDelayed({
        var elapsedTime = 0
        var targetVolume = 0f

        val fadeInRunnable: Runnable = object : Runnable {
            override fun run() {
                if (elapsedTime < duration) {
                    elapsedTime += FADE_INTERVAL.toInt()
                    targetVolume = (elapsedTime.toFloat() / duration)
                        .toBigDecimal()
                        .setScale(2, RoundingMode.UP)
                        .toFloat()
                        .coerceAtMost(1f)
                    
                    // Only increase volume if current volume is lower than target
                    if (player.volume < targetVolume) {
                        player.volume = targetVolume
                        //println("mediaItem audioFadeIn: player.volume ${player.volume} targetVolume $targetVolume elapsedTime $elapsedTime")
                    }

                    handler.postDelayed(this, FADE_INTERVAL)
                } else {
                    // Ensure volume is exactly 1 at the end
                    player.volume = 1f
                }
            }
        }
        handler.post(fadeInRunnable)

    }, FADE_INTERVAL)
}

fun getDeviceVolume(context: Context): Float {
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    val volumeLevel: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return if (maxVolume > 0) volumeLevel.toFloat() / maxVolume else 1f
}

fun setDeviceVolume(context: Context, volume: Float) {
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val targetVolume = (volume * maxVolume).toInt().coerceIn(0, maxVolume)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
}

@Composable
@OptIn(UnstableApi::class)
fun MedleyMode(binder: PlayerServiceModern.Binder?, seconds: Int) {
    if (seconds <= 0) return
    if (binder != null) {
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(binder, seconds) {
            while (isActive) {
                delay(seconds.seconds)
                withContext(Dispatchers.Main) {
                    if (binder.player.isPlaying) {
                        binder.player.playNext()
                    }
                }
            }
        }
    }
}

@MainThread
fun ExoPlayer.fadeInEffect(duration: Long) {
    if (isPlaying) return
    if (duration <= 0L) {
        if (playbackState == Player.STATE_IDLE) {
            prepare()
        }
        play()
        return
    }

    val animator = ValueAnimator.ofFloat(0f, getGlobalVolume())
    animator.duration = duration
    animator.addUpdateListener {
        volume = it.animatedValue as Float
    }
    animator.doOnStart {
        if (playbackState == Player.STATE_IDLE) {
            prepare()
        }
        play()
    }
    animator.start()
}

@MainThread
fun ExoPlayer.fadeOutEffect(duration: Long) {
    if (!isPlaying) return
    if (duration <= 0L) {
        pause()
        return
    }

    val animator = ValueAnimator.ofFloat(getGlobalVolume(), 0f)
    animator.duration = duration
    animator.addUpdateListener {
        volume = it.animatedValue as Float
    }
    animator.doOnEnd {
        pause()
        restoreGlobalVolume()
    }
    animator.start()
}

// Helper extension functions for global volume management
private fun ExoPlayer.getGlobalVolume(): Float {
    return volume
}

private fun ExoPlayer.restoreGlobalVolume() {
    volume = 1f // Restore to full volume
}
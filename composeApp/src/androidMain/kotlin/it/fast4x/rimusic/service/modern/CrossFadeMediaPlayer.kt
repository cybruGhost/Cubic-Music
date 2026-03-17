package it.fast4x.rimusic.service.modern

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

import android.content.Context

class CrossFadeMediaPlayer(private val context: Context) {

    private var currentPlayer: ExoPlayer? = null
    private var nextPlayer: ExoPlayer? = null

    private var crossFadeDuration = 15000L
    private var isTransitioning = false

    fun setCrossFadeDuration(durationMs: Long) {
        crossFadeDuration = durationMs
    }

    fun play(mediaItem: MediaItem) {
        if (currentPlayer == null) {

            currentPlayer = createPlayer()
            currentPlayer?.setMediaItem(mediaItem)
            currentPlayer?.prepare()
            currentPlayer?.play()
            startMonitoringForCrossFade()
        } else {

            nextPlayer = createPlayer()
            nextPlayer?.setMediaItem(mediaItem)
            nextPlayer?.prepare()
            nextPlayer?.volume = 0f
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            // Configure player
        }
    }

    private fun startMonitoringForCrossFade() {
        currentPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Monitor playback position
                checkForCrossFadeStart()
            }
        })
    }

    private fun checkForCrossFadeStart() {
        val player = currentPlayer ?: return
        val duration = player.duration
        val position = player.currentPosition

        if (duration - position <= crossFadeDuration && !isTransitioning) {
            startCrossFade()
        }
    }

    private fun startCrossFade() {
        if (nextPlayer == null) return

        isTransitioning = true
        nextPlayer?.play()

        // Animate volumes
        val fadeOutAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = crossFadeDuration
            addUpdateListener { animation ->
                currentPlayer?.volume = animation.animatedValue as Float
            }
        }

        val fadeInAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = crossFadeDuration
            addUpdateListener { animation ->
                nextPlayer?.volume = animation.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Crossfade complete
                    currentPlayer?.release()
                    currentPlayer = nextPlayer
                    nextPlayer = null
                    isTransitioning = false
                }
            })
        }

        fadeOutAnimator.start()
        fadeInAnimator.start()
    }

    fun release() {
        currentPlayer?.release()
        nextPlayer?.release()
    }
}
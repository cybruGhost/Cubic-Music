package app.it.fast4x.rimusic.utils

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer

class FadeAdjuster {
    var duration by mutableFloatStateOf(0f)
        private set
    
    var isActive by mutableStateOf(false)
        private set
    
    var fadeDirection by mutableStateOf(FadeDirection.NONE)
        private set
    
    private var animator: ValueAnimator? = null
    private var context: android.content.Context? = null
    
    enum class FadeDirection {
        IN, OUT, NONE
    }
    
    fun setContext(ctx: android.content.Context) {
        context = ctx
    }
    
    fun setDuration(durationMs: Int) {
        duration = durationMs.toFloat()
    }
    
    fun fadeOut(player: ExoPlayer, onComplete: (() -> Unit)? = null) {
        if (duration <= 0) {
            onComplete?.invoke()
            return
        }
        
        isActive = true
        fadeDirection = FadeDirection.OUT
        
        // Use the existing audioFadeOut function
        audioFadeOut(player, duration.toInt(), context ?: return)
        
        // Since audioFadeOut doesn't have a callback, we'll simulate it with a delayed runnable
        Handler(Looper.getMainLooper()).postDelayed({
            isActive = false
            fadeDirection = FadeDirection.NONE
            onComplete?.invoke()
        }, duration.toLong())
    }
    
    fun fadeIn(player: ExoPlayer, targetVolume: Float = player.volume, onComplete: (() -> Unit)? = null) {
        if (duration <= 0) {
            player.volume = targetVolume
            onComplete?.invoke()
            return
        }
        
        isActive = true
        fadeDirection = FadeDirection.IN
        
        // Use the existing audioFadeIn function
        audioFadeIn(player, duration.toInt(), context ?: return)
        
        // Since audioFadeIn doesn't have a callback, we'll simulate it with a delayed runnable
        Handler(Looper.getMainLooper()).postDelayed({
            isActive = false
            fadeDirection = FadeDirection.NONE
            onComplete?.invoke()
        }, duration.toLong())
    }
    
    fun cancel() {
        animator?.cancel()
        isActive = false
        fadeDirection = FadeDirection.NONE
    }
    
    @Composable
    fun Render() {
        // This follows the same pattern as BlurAdjuster.Render()
        // No implementation needed - just for consistency with the pattern
    }
}
package app.it.fast4x.rimusic.service.modern

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import app.it.fast4x.rimusic.cleanPrefix
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArraySet

@UnstableApi
class DelegatingExoPlayer(
    initialDelegate: ExoPlayer,
) : InvocationHandler {
    @Volatile
    private var delegate: ExoPlayer = initialDelegate
    @Volatile
    private var displayStateProvider: (() -> CrossfadeUiState?)? = null

    private val playerListeners = CopyOnWriteArraySet<Player.Listener>()
    private val analyticsListeners = CopyOnWriteArraySet<AnalyticsListener>()

    val player: ExoPlayer = Proxy.newProxyInstance(
        ExoPlayer::class.java.classLoader,
        arrayOf(ExoPlayer::class.java),
        this,
    ) as ExoPlayer

    fun updateDelegate(newDelegate: ExoPlayer) {
        if (delegate === newDelegate) return

        val oldDelegate = delegate
        playerListeners.forEach(oldDelegate::removeListener)
        analyticsListeners.forEach(oldDelegate::removeAnalyticsListener)

        delegate = newDelegate

        playerListeners.forEach(newDelegate::addListener)
        analyticsListeners.forEach(newDelegate::addAnalyticsListener)
        notifyStateRefresh(newDelegate)
    }

    fun setDisplayStateProvider(provider: () -> CrossfadeUiState?) {
        displayStateProvider = provider
    }

    fun refreshState() {
        notifyStateRefresh(delegate)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        return when (method.name) {
            "getCurrentMediaItem" -> reportedMediaItem()
            "getMediaMetadata" -> reportedMediaMetadata()
            "isPlaying" -> reportedIsPlaying()
            "getCurrentPosition" -> reportedCurrentPosition()
            "getContentPosition" -> reportedCurrentPosition()
            "getDuration" -> reportedDuration()
            "getBufferedPosition" -> reportedBufferedPosition()
            "getContentBufferedPosition" -> reportedBufferedPosition()
            "getBufferedPercentage" -> reportedBufferedPercentageValue()
            "getTotalBufferedDuration" -> reportedTotalBufferedDuration()
            "addListener" -> {
                val listener = args?.firstOrNull() as? Player.Listener
                if (listener != null) {
                    playerListeners += listener
                    delegate.addListener(listener)
                }
                Unit
            }

            "removeListener" -> {
                val listener = args?.firstOrNull() as? Player.Listener
                if (listener != null) {
                    playerListeners -= listener
                    delegate.removeListener(listener)
                }
                Unit
            }

            "addAnalyticsListener" -> {
                val listener = args?.firstOrNull() as? AnalyticsListener
                if (listener != null) {
                    analyticsListeners += listener
                    delegate.addAnalyticsListener(listener)
                }
                Unit
            }

            "removeAnalyticsListener" -> {
                val listener = args?.firstOrNull() as? AnalyticsListener
                if (listener != null) {
                    analyticsListeners -= listener
                    delegate.removeAnalyticsListener(listener)
                }
                Unit
            }

            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "DelegatingExoPlayer(delegate=$delegate)"
            else -> invokeOnDelegate(method, args)
        }
    }

    private fun invokeOnDelegate(method: Method, args: Array<out Any?>?): Any? {
        return try {
            method.invoke(delegate, *(args ?: emptyArray()))
        } catch (error: InvocationTargetException) {
            val target = error.targetException
            when (method.name) {
                "getBufferedPercentage" -> 0
                "getBufferedPosition", "getContentBufferedPosition", "getCurrentPosition", "getContentPosition" -> 0L
                "getTotalBufferedDuration" -> 0L
                "getDuration" -> C.TIME_UNSET
                else -> throw target
            }
        }
    }

    private fun currentDisplayState(): CrossfadeUiState? = displayStateProvider?.invoke()

    private fun shouldUseDisplayState(): Boolean {
        val state = currentDisplayState()
        return state?.isEnabled == true && state.isActive && state.displayMediaItem != null
    }

    private fun activeDisplayState(): CrossfadeUiState? =
        currentDisplayState()?.takeIf { shouldUseDisplayState() }

    private fun reportedMediaItem(): MediaItem? {
        if (!shouldUseDisplayState()) {
            return delegate.currentMediaItem
        }
        return activeDisplayState()?.displayMediaItem ?: delegate.currentMediaItem
    }

    private fun reportedMediaMetadata(): MediaMetadata {
        if (!shouldUseDisplayState()) {
            return delegate.mediaMetadata
        }

        val displayItem = activeDisplayState()?.displayMediaItem
        return if (displayItem != null) {
            displayItem.mediaMetadata.buildUpon()
                .setTitle(cleanPrefix(cleanPrefix(displayItem.mediaMetadata.title?.toString().orEmpty())))
                .setArtist(cleanPrefix(cleanPrefix(displayItem.mediaMetadata.artist?.toString().orEmpty())))
                .setAlbumTitle(cleanPrefix(cleanPrefix(displayItem.mediaMetadata.albumTitle?.toString().orEmpty())))
                .build()
        } else {
            delegate.mediaMetadata
        }
    }

    private fun reportedIsPlaying(): Boolean {
        if (!shouldUseDisplayState()) {
            return delegate.isPlaying
        }
        return true
    }

    private fun reportedDuration(): Long {
        if (!shouldUseDisplayState()) {
            val duration = runCatching { delegate.duration }.getOrDefault(C.TIME_UNSET)
            return if (duration == C.TIME_UNSET || duration <= 0L) C.TIME_UNSET else duration
        }
        val displayDuration = activeDisplayState()?.displayDuration
        if (displayDuration != null && displayDuration > 0L) return displayDuration
        val duration = runCatching { delegate.duration }.getOrDefault(C.TIME_UNSET)
        return if (duration == C.TIME_UNSET || duration <= 0L) C.TIME_UNSET else duration
    }

    private fun reportedCurrentPosition(): Long {
        if (!shouldUseDisplayState()) {
            return runCatching { delegate.currentPosition }.getOrDefault(0L).coerceAtLeast(0L)
        }
        val state = activeDisplayState()
        return state?.displayPosition?.coerceIn(0L, state.displayDuration.coerceAtLeast(1L))
            ?: runCatching { delegate.currentPosition }.getOrDefault(0L).coerceAtLeast(0L)
    }

    private fun reportedBufferedPosition(): Long {
        if (!shouldUseDisplayState()) {
            val duration = reportedDuration()
            val bufferedPosition = runCatching { delegate.bufferedPosition }.getOrDefault(0L).coerceAtLeast(0L)
            return if (duration == C.TIME_UNSET) bufferedPosition else bufferedPosition.coerceAtMost(duration)
        }
        val state = activeDisplayState()
        return state?.displayPosition?.coerceIn(0L, state.displayDuration.coerceAtLeast(1L))
            ?: runCatching {
                val duration = reportedDuration()
                val bufferedPosition = delegate.bufferedPosition.coerceAtLeast(0L)
                if (duration == C.TIME_UNSET) bufferedPosition else bufferedPosition.coerceAtMost(duration)
            }.getOrDefault(0L)
    }

    private fun reportedBufferedPercentageValue(): Int {
        val duration = reportedDuration()
        if (duration == C.TIME_UNSET || duration <= 0L) return 0

        val bufferedPosition = reportedBufferedPosition().coerceAtLeast(0L)
        val safeDuration = duration.coerceAtLeast(1L)
        val percent = ((bufferedPosition.coerceAtMost(safeDuration) * 100L) / safeDuration)
        return percent.coerceIn(0L, 100L).toInt()
    }

    private fun reportedTotalBufferedDuration(): Long =
        (reportedBufferedPosition().coerceAtLeast(0L) - reportedCurrentPosition().coerceAtLeast(0L))
            .coerceAtLeast(0L)

    private fun notifyStateRefresh(activeDelegate: ExoPlayer) {
        val useDisplayState = shouldUseDisplayState()
        val currentMediaItem = if (useDisplayState) {
            activeDisplayState()?.displayMediaItem
        } else {
            activeDelegate.currentMediaItem
        }
        val mediaMetadata = if (useDisplayState) {
            reportedMediaMetadata()
        } else {
            activeDelegate.mediaMetadata
        }
        val playWhenReady = activeDelegate.playWhenReady
        val playbackState = activeDelegate.playbackState
        val isPlaying = if (useDisplayState) true else activeDelegate.isPlaying
        val shouldEmitSyntheticTransition =
            useDisplayState && currentMediaItem?.mediaId != activeDelegate.currentMediaItem?.mediaId

        playerListeners.forEach { listener ->
            if (shouldEmitSyntheticTransition) {
                listener.onMediaItemTransition(
                    currentMediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
                )
            }
            listener.onMediaMetadataChanged(mediaMetadata)
            listener.onPlayWhenReadyChanged(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            listener.onPlaybackStateChanged(playbackState)
            listener.onIsPlayingChanged(isPlaying)
        }
    }
}

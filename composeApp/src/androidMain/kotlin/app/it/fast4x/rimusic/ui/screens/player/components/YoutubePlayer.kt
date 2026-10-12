package app.it.fast4x.rimusic.ui.screens.player.components

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.kreate.android.R
import app.kreate.android.service.getInnertubeVideoStream
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.okHttpDataSourceFactory
import app.it.fast4x.rimusic.utils.lastVideoIdKey
import app.it.fast4x.rimusic.utils.lastVideoSecondsKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun YoutubePlayer(
    ytVideoId: String,
    lifecycleOwner: LifecycleOwner,
    showPlayer: Boolean = true,
    syncPlayer: Player? = null,
    onCurrentSecond: (second: Float) -> Unit,
    onSwitchToAudioPlayer: () -> Unit
) {
    if (!showPlayer) return

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    if (ytVideoId != lastYTVideoId) lastYTVideoSeconds = 0f

    val context = LocalContext.current
    var videoPlaybackFailed by remember(ytVideoId) { mutableStateOf(false) }
    val resolvedVideoState by produceState<ResolvedYoutubeVideoState>(
        initialValue = ResolvedYoutubeVideoState.Loading,
        key1 = ytVideoId
    ) {
        value = withContext(Dispatchers.IO) {
            withTimeoutOrNull(12_000L) {
                resolveYoutubeVideo(ytVideoId)
            } ?: ResolvedYoutubeVideoState.Unavailable
        }
    }

    val player = remember(ytVideoId, resolvedVideoState) {
        (resolvedVideoState as? ResolvedYoutubeVideoState.Ready)?.video?.let { video ->
            ExoPlayer.Builder(context)
                .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setMediaSourceFactory(DefaultMediaSourceFactory(context.okHttpDataSourceFactory))
                .build()
                .apply {
                    volume = if (syncPlayer == null) 1f else 0f
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = syncPlayer?.isPlaying ?: true
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    setMediaItem(
                        MediaItem.Builder()
                            .setUri(video.url)
                            .setMimeType(video.mimeType)
                            .build()
                    )
                    val startPosition =
                        syncPlayer?.currentPosition ?: (lastYTVideoSeconds * 1000).toLong()
                    if (startPosition > 0L) {
                        seekTo(startPosition)
                    }
                    addListener(
                        object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                if (isPlaying) {
                                    lastYTVideoId = ytVideoId
                                }
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    syncPlayer?.let { audioPlayer ->
                                        val target = audioPlayer.currentPosition.coerceAtLeast(0L)
                                        if (kotlin.math.abs(currentPosition - target) > 500L) {
                                            seekTo(target)
                                        }
                                        playWhenReady = audioPlayer.isPlaying
                                    }
                                } else if (playbackState == Player.STATE_ENDED) {
                                    pause()
                                }
                            }

                            override fun onPlayWhenReadyChanged(
                                playWhenReady: Boolean,
                                reason: Int
                            ) {
                                if (
                                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST &&
                                    syncPlayer != null
                                ) {
                                    if (playWhenReady) syncPlayer.play() else syncPlayer.pause()
                                }
                            }

                            override fun onPositionDiscontinuity(
                                oldPosition: Player.PositionInfo,
                                newPosition: Player.PositionInfo,
                                reason: Int
                            ) {
                                if (
                                    reason == Player.DISCONTINUITY_REASON_SEEK &&
                                    syncPlayer != null &&
                                    kotlin.math.abs(syncPlayer.currentPosition - newPosition.positionMs) > 1_000L
                                ) {
                                    syncPlayer.seekTo(newPosition.positionMs)
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                videoPlaybackFailed = true
                            }
                        }
                    )
                    prepare()
                }
        }
    }

    LaunchedEffect(player) {
        var stalledForMs = 0L
        while (player != null) {
            syncPlayer?.let { audioPlayer ->
                val targetPosition = audioPlayer.currentPosition.coerceAtLeast(0L)
                if (
                    player.playbackState == Player.STATE_READY &&
                    kotlin.math.abs(player.currentPosition - targetPosition) > 1_500L
                ) {
                    player.seekTo(targetPosition)
                }
                player.playWhenReady = audioPlayer.isPlaying
            }

            stalledForMs = if (
                player.playWhenReady &&
                player.playbackState != Player.STATE_READY
            ) {
                stalledForMs + 500L
            } else {
                0L
            }
            if (stalledForMs >= 20_000L) {
                videoPlaybackFailed = true
                player.pause()
                break
            }

            val currentSecond = player.currentPosition / 1000f
            onCurrentSecond(currentSecond)
            lastYTVideoSeconds = currentSecond
            delay(500L)
        }
    }

    DisposableEffect(player, lifecycleOwner, syncPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> player?.playWhenReady = syncPlayer?.isPlaying ?: true
                Lifecycle.Event.ON_STOP -> player?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }
    Box {
        when {
            player != null && !videoPlaybackFailed -> {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .zIndex(2f),
                    factory = {
                        PlayerView(it).apply {
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setKeepContentOnPlayerReset(true)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                    }
                )
            }

            resolvedVideoState is ResolvedYoutubeVideoState.Loading -> {
                VideoStatus(
                    title = "Loading video...",
                    subtitle = "Fetching a direct stream that bypasses the blocked YouTube embed.",
                    isLoading = true
                )
            }

            else -> {
                VideoStatus(
                    title = "Video unavailable",
                    subtitle = "This video stream could not be resolved right now. Audio playback is still available.",
                    isLoading = false
                )
            }
        }
    }
}

@Composable
private fun VideoStatus(
    title: String,
    subtitle: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 130.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colorPalette().background0)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = colorPalette().accent
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = if (isLoading) 18.dp else 0.dp),
            style = typography().s.semiBold,
            color = colorPalette().text
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 8.dp),
            style = typography().xs,
            color = colorPalette().textSecondary,
        )
    }
}

private data class ResolvedYoutubeVideo(
    val url: String,
    val mimeType: String
)

private sealed interface ResolvedYoutubeVideoState {
    data object Loading : ResolvedYoutubeVideoState
    data class Ready(val video: ResolvedYoutubeVideo) : ResolvedYoutubeVideoState
    data object Unavailable : ResolvedYoutubeVideoState
}

private suspend fun resolveYoutubeVideo(videoId: String): ResolvedYoutubeVideoState {
    val stream = runCatching { getInnertubeVideoStream(videoId) }
        .getOrNull()
        ?: return ResolvedYoutubeVideoState.Unavailable

    return ResolvedYoutubeVideoState.Ready(
        ResolvedYoutubeVideo(
            url = stream.url,
            mimeType = stream.mimeType
        )
    )
}

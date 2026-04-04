package app.it.fast4x.rimusic.ui.screens.player.components

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.getStreamUrl
import app.it.fast4x.rimusic.utils.lastVideoIdKey
import app.it.fast4x.rimusic.utils.lastVideoSecondsKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import com.google.gson.Gson
import io.ktor.client.statement.bodyAsText
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.Innertube.createPoTokenChallenge
import it.fast4x.innertube.models.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.util.UUID

@Composable
fun YoutubePlayer(
    ytVideoId: String,
    lifecycleOwner: LifecycleOwner,
    showPlayer: Boolean = true,
    onCurrentSecond: (second: Float) -> Unit,
    onSwitchToAudioPlayer: () -> Unit
) {
    if (!showPlayer) return

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    if (ytVideoId != lastYTVideoId) lastYTVideoSeconds = 0f

    val context = LocalContext.current
    val resolvedVideoState by produceState<ResolvedYoutubeVideoState>(
        initialValue = ResolvedYoutubeVideoState.Loading,
        key1 = ytVideoId
    ) {
        value = withContext(Dispatchers.IO) {
            resolveYoutubeVideo(ytVideoId)
        }
    }

    val player = remember(ytVideoId, resolvedVideoState) {
        (resolvedVideoState as? ResolvedYoutubeVideoState.Ready)?.video?.let { video ->
            ExoPlayer.Builder(context).build().apply {
                volume = 1f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(video.url)
                        .setMimeType(video.mimeType)
                        .build()
                )
                prepare()
                if (lastYTVideoSeconds > 0f) {
                    seekTo((lastYTVideoSeconds * 1000).toLong())
                }
                addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                lastYTVideoId = ytVideoId
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                seekTo(0)
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(player) {
        while (player != null) {
            val currentSecond = player.currentPosition / 1000f
            onCurrentSecond(currentSecond)
            lastYTVideoSeconds = currentSecond
            withFrameMillis { }
        }
    }

    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }

    Box {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 12.dp)
                .background(
                    color = colorPalette().background0.copy(alpha = 0.9f),
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSwitchToAudioPlayer() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.musical_notes),
                contentDescription = stringResource(R.string.return_to_audio_mode),
                colorFilter = ColorFilter.tint(colorPalette().text),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = stringResource(R.string.return_to_audio_mode),
                style = typography().xs.semiBold,
                color = colorPalette().text
            )
        }

        when {
            player != null -> {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 130.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .zIndex(2f),
                    factory = {
                        PlayerView(it).apply {
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
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

private val playerResponseJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    useArrayPolymorphism = true
    explicitNulls = false
}

private suspend fun resolveYoutubeVideo(videoId: String): ResolvedYoutubeVideoState {
    val playerResponse = runCatching {
        getAndroidPlayerResponse(videoId)
    }.recoverCatching {
        getIosPlayerResponse(videoId)
    }.getOrNull() ?: return ResolvedYoutubeVideoState.Unavailable

    val playabilityStatus = playerResponse.playabilityStatus?.status
    if (playabilityStatus != null && playabilityStatus != "OK") {
        return ResolvedYoutubeVideoState.Unavailable
    }

    val selectedFormat = playerResponse.streamingData
        ?.formats
        ?.asSequence()
        ?.filter { !it.isAudio }
        ?.sortedWith(
            compareByDescending<PlayerResponse.StreamingData.Format> { it.height ?: 0 }
                .thenByDescending { it.bitrate }
        )
        ?.firstOrNull { it.mimeType.startsWith("video/mp4") }
        ?: playerResponse.streamingData
            ?.formats
            ?.firstOrNull { !it.isAudio }
        ?: return ResolvedYoutubeVideoState.Unavailable

    val streamUrl = getStreamUrl(selectedFormat, videoId)
        ?: return ResolvedYoutubeVideoState.Unavailable
    val mimeType = selectedFormat.mimeType.substringBefore(";")

    return ResolvedYoutubeVideoState.Ready(
        ResolvedYoutubeVideo(
            url = streamUrl,
            mimeType = mimeType
        )
    )
}

private fun getAndroidPlayerResponse(videoId: String): PlayerResponse {
    val cpn = randomCpn()
    val response = YoutubeStreamHelper.getAndroidReelPlayerResponse(
        ContentCountry.DEFAULT,
        Localization.DEFAULT,
        videoId,
        cpn
    )
    return Gson().toJson(response).let(playerResponseJson::decodeFromString)
}

private suspend fun getIosPlayerResponse(videoId: String): PlayerResponse {
    val cpn = randomCpn()
    val visitorData = app.kreate.android.network.innertube.Store.getIosVisitorData()
    val challenge = createPoTokenChallenge().bodyAsText()
    val challengeTokens = playerResponseJson.decodeFromString<List<String?>>(challenge)
    val challengeToken = challengeTokens.filterIsInstance<String>().firstOrNull().orEmpty()
    val poToken = Innertube.generatePoToken(challengeToken).bodyAsText()
        .replace("[", "")
        .replace("]", "")
        .split(",")
        .findLast { it.contains("\"") }
        ?.replace("\"", "")
        .orEmpty()

    val response = YoutubeStreamHelper.getIosPlayerResponse(
        ContentCountry.DEFAULT,
        Localization.DEFAULT,
        videoId,
        cpn,
        PoTokenResult(visitorData, poToken, null)
    )
    return Gson().toJson(response).let(playerResponseJson::decodeFromString)
}

private fun randomCpn(): String =
    UUID.randomUUID().toString().replace("-", "").take(16)

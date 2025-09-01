package it.fast4x.rimusic.ui.screens.player.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import app.kreate.android.R
import it.fast4x.rimusic.utils.lastVideoIdKey
import it.fast4x.rimusic.utils.lastVideoSecondsKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.semiBold


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

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 130.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .zIndex(2f),
            factory = {
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(1)
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        //println("mediaItem youtubePlayer onReady called lastYTVideoSeconds $lastYTVideoSeconds")
                        youTubePlayer.loadVideo(ytVideoId, lastYTVideoSeconds)
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        onCurrentSecond(second)
                        lastYTVideoSeconds = second
                        lastYTVideoId = ytVideoId
                    }

                }


                YouTubePlayerView(context = it).apply {
                    enableAutomaticInitialization = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    lifecycleOwner.lifecycle.addObserver(this)
                    initialize(listener, true, iFramePlayerOptions)
                }
            }
        )
    }

}

package app.it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.color
import app.kreate.android.R

@Composable
internal fun VoiceListeningOverlay(
    onCancel: () -> Unit
) {
    val palette = colorPalette()
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        )
    )
    val wave by rememberInfiniteTransition().animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(palette.background2.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f * wave)
                    .clip(RoundedCornerShape(50.dp))
                    .background(palette.accent.copy(alpha = 0.28f))
                    .padding(vertical = 3.dp)
            )

            BasicText(
                text = stringResource(R.string.voice_listening_title),
                style = typography().xl.color(Color.White)
            )
            BasicText(
                text = stringResource(R.string.voice_listening_subtitle),
                style = typography().s.color(Color.White.copy(alpha = 0.72f))
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onCancel() }) }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.cancel),
                    style = typography().s.color(Color.White)
                )
            }

            VoiceExampleChip(stringResource(R.string.voice_example_relaxing))
            VoiceExampleChip(stringResource(R.string.voice_example_artist))
            VoiceExampleChip(stringResource(R.string.voice_example_workout))
        }
    }
}

@Composable
private fun VoiceExampleChip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = "\"$text\"",
            style = typography().xs.color(Color.White.copy(alpha = 0.86f))
        )
    }
}
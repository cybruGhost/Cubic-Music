package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R

@Composable
fun RewindIntroCard(
    username: String,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_intro_title),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindRed,
        secondary = RewindPurple,
        onClick = onNext
    ) {
        Spacer(Modifier.height(34.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            RewindWaveform(
                accent = RewindRed.copy(alpha = 0.72f),
                secondary = RewindPink.copy(alpha = 0.62f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(RewindRed, RewindPink, RewindInk),
                            radius = 180f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.rewind_playback_cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_intro_headline),
                color = RewindCream,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.rewind_greeting, username),
                color = RewindRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rewind_card_intro_body, year),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        RewindShareButton(
            text = stringResource(R.string.rewind_start),
            accent = RewindRed,
            onClick = onNext
        )
        Text(
            text = stringResource(R.string.rewind_privacy_subtitle),
            color = RewindCream.copy(alpha = 0.48f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    }
}

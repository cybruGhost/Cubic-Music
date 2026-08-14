package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

@Composable
fun RewindListeningDaysCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_days_title),
        year = data.year,
        page = page,
        pageCount = pageCount,
        accent = RewindGreen,
        secondary = RewindBlue,
        onClick = onNext
    ) {
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.rewind_card_days_headline),
                color = RewindCream,
                fontSize = 28.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatRewindNumber(data.daysWithMusic.toLong()),
                color = RewindGreen,
                fontSize = 86.sp,
                lineHeight = 88.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.rewind_days_with_music).lowercase(),
                color = RewindCream,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(7) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < data.daysWithMusic.coerceAtMost(7)) RewindGreen.copy(alpha = 0.9f)
                            else RewindCream.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (index < data.daysWithMusic.coerceAtMost(7)) RewindInk else RewindCream.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        RewindMetricPill(
            value = data.stats.firstPlayDate ?: "-",
            label = stringResource(R.string.rewind_card_days_first_play),
            accent = RewindGreen,
            modifier = Modifier.fillMaxWidth()
        )
        RewindMetricPill(
            value = data.stats.lastPlayDate ?: "-",
            label = stringResource(R.string.rewind_card_days_last_play),
            accent = RewindBlue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

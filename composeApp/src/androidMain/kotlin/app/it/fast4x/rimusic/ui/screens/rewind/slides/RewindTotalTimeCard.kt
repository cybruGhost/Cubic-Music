package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

@Composable
fun RewindTotalTimeCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_total_title),
        year = data.year,
        page = page,
        pageCount = pageCount,
        accent = RewindOrange,
        secondary = RewindRed,
        onClick = onNext
    ) {
        Spacer(Modifier.height(22.dp))
        RewindRadialMeter(
            value = (data.stats.totalMinutes / 120_000f).coerceIn(0.08f, 1f),
            accent = RewindOrange,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.rewind_card_total_prefix),
                    color = RewindCream.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formatRewindMinutes(data.stats.totalMinutes),
                    color = RewindOrange,
                    fontSize = 56.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.rewind_minutes_label).lowercase(),
                    color = RewindCream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RewindMetricPill(
                value = formatRewindNumber(data.stats.totalPlays.toLong()),
                label = stringResource(R.string.rewind_total_plays),
                accent = RewindRed,
                modifier = Modifier.weight(1f)
            )
            RewindMetricPill(
                value = formatRewindNumber(data.daysWithMusic.toLong()),
                label = stringResource(R.string.rewind_days_with_music),
                accent = RewindGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.rewind_card_total_footer, formatRewindHours(data.stats.totalMinutes)),
            color = RewindCream.copy(alpha = 0.68f),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

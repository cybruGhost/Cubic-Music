package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.DailyStat
import app.it.fast4x.rimusic.ui.screens.rewind.HourlyStat
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

@Composable
fun RewindPeakTimeCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    val peakDay = remember(data.dailyStats) { data.dailyStats.maxByOrNull { it.plays } }
    val peakHour = remember(data.hourlyStats) { data.hourlyStats.maxByOrNull { it.plays } }
    val dayBars = remember(data.dailyStats) { normalizeDailyBars(data.dailyStats) }

    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_peak_title),
        year = data.year,
        page = page,
        pageCount = pageCount,
        accent = RewindYellow,
        secondary = RewindOrange,
        onClick = onNext
    ) {
        if (data.dailyStats.isEmpty() && data.hourlyStats.isEmpty()) {
            Spacer(Modifier.weight(1f))
            RewindEmptyState(
                title = stringResource(R.string.rewind_card_peak_no_day),
                body = stringResource(R.string.rewind_empty_line),
                accent = RewindYellow
            )
            Spacer(Modifier.weight(1f))
            return@RewindCanvasCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_peak_headline),
                color = RewindCream,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = peakDay?.dayOfWeek ?: stringResource(R.string.rewind_card_peak_no_day),
                color = RewindYellow,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = peakHour?.let { stringResource(R.string.rewind_card_peak_hour, it.hour) }
                    ?: stringResource(R.string.rewind_card_peak_no_hour),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            dayBars.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(day.normalized)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (day.isPeak) RewindYellow else RewindCream.copy(alpha = 0.18f))
                    )
                    Text(
                        text = day.label,
                        color = if (day.isPeak) RewindYellow else RewindCream.copy(alpha = 0.62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (day.isPeak) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RewindYellow)
                        )
                    }
                }
            }
        }
        RewindMetricPill(
            value = formatRewindNumber((peakDay?.plays ?: 0).toLong()),
            label = stringResource(R.string.rewind_card_peak_play_count),
            accent = RewindYellow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class DayBar(
    val label: String,
    val normalized: Float,
    val isPeak: Boolean
)

private fun normalizeDailyBars(stats: List<DailyStat>): List<DayBar> {
    val ordered = stats
    val peak = ordered.maxByOrNull { it.plays }
    val max = ordered.maxOfOrNull { it.plays }?.coerceAtLeast(1) ?: 1
    return ordered.take(7).map { stat ->
        DayBar(
            label = stat.dayOfWeek.take(3).uppercase(),
            normalized = (0.18f + (stat.plays.toFloat() / max.toFloat()) * 0.82f).coerceIn(0.18f, 1f),
            isPeak = peak != null && peak.dayOfWeek == stat.dayOfWeek && peak.plays > 0
        )
    }
}

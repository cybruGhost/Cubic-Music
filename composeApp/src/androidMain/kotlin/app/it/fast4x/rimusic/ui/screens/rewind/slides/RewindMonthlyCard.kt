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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.MonthlyStat
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

@Composable
fun RewindMonthlyCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    val months = remember(data.monthlyStats) { data.monthlyStats.filter { it.plays > 0 || it.minutes > 0 } }
    val bestMonth = remember(months) { months.maxWithOrNull(compareBy<MonthlyStat> { it.minutes }.thenBy { it.plays }) }
    val maxMinutes = remember(data.monthlyStats) { data.monthlyStats.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1 }

    RewindCanvasCard(
        title = stringResource(R.string.rewind_slide_months),
        year = data.year,
        page = page,
        pageCount = pageCount,
        accent = RewindRed,
        secondary = RewindYellow,
        onClick = onNext
    ) {
        if (data.monthlyStats.isEmpty()) {
            Spacer(Modifier.weight(1f))
            RewindEmptyState(
                title = stringResource(R.string.rewind_slide_months),
                body = stringResource(R.string.rewind_empty_line),
                accent = RewindRed
            )
            Spacer(Modifier.weight(1f))
            return@RewindCanvasCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_month_headline),
                color = RewindCream,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = bestMonth?.month ?: "-",
                color = RewindRed,
                fontSize = 46.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bestMonth?.let { stringResource(R.string.rewind_card_month_meta, compactMetaMinutes(it.minutes), it.plays) }
                    ?: stringResource(R.string.rewind_empty_line),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 15.sp,
                lineHeight = 19.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.monthlyStats.take(12).forEach { month ->
                val active = month.minutes > 0 || month.plays > 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (active) (month.minutes.toFloat() / maxMinutes).coerceIn(0.12f, 1f) else 0.04f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (month.month == bestMonth?.month) RewindRed
                                else RewindCream.copy(alpha = if (active) 0.28f else 0.10f)
                            )
                    )
                    Text(
                        text = month.month.take(1).uppercase(),
                        color = if (month.month == bestMonth?.month) RewindRed else RewindCream.copy(alpha = 0.58f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

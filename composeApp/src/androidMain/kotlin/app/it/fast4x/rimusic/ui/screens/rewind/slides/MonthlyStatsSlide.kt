package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.MonthlyStat
import app.it.fast4x.rimusic.ui.screens.rewind.components.getMonthName

@Composable
fun MonthlyStatsSlide(monthlyStats: List<MonthlyStat>, onNext: () -> Unit) {
    val accent = Color(0xFFE056D8)
    val total = monthlyStats.sumOf { it.minutes }.coerceAtLeast(1)
    RewindStoryFrame(header = "Monthly Breakdown", accent = accent) {
        Text(total.toString(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("minutes", color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth().height(210.dp).padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            monthlyStats.take(12).forEach {
                val fraction = (it.minutes.toFloat() / total).coerceIn(0.08f, 1f)
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.verticalGradient(listOf(accent, Color(0xFF4D37E8))))
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            monthlyStats.sortedByDescending { it.minutes }.take(3).forEachIndexed { index, stat ->
                RewindListRow(index + 1, getMonthName(stat.month), "${stat.plays} plays", "${stat.minutes} min", null, accent = accent)
            }
        }
        RewindAction("See full breakdown", accent, onNext)
    }
}

package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RewindPeakTimeCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val peakHour = data.stats.mostActiveHour?.hour?.substringBefore(':')?.toIntOrNull() ?: 0
    val hand = remember(peakHour) { Animatable(0f) }
    val bars = remember { Animatable(0f) }

    LaunchedEffect(active, peakHour) {
        if (!active) {
            hand.snapTo(0f)
            bars.snapTo(0f)
        } else {
            hand.snapTo(0f)
            bars.snapTo(0f)
            delay(360)
            hand.animateTo(1f, tween(1_050, easing = FastOutSlowInEasing))
            bars.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindPurple,
        progressColor = RewindCream,
        onNext = onNext
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val clockSize = if (compact) 210.dp else 250.dp

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("WHEN YOU LISTENED • ${data.year}", RewindLime)
                }

                Spacer(Modifier.height(11.dp))

                RewindReveal(active, 110, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOUR PEAK\nLISTENING TIME.",
                        color = RewindCream,
                        fontSize = if (compact) 35.sp else 41.sp,
                        lineHeight = if (compact) 33.sp else 38.sp,
                        letterSpacing = (-1.9).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RewindReveal(active, 260, scaleFrom = 0.76f) {
                        Box(
                            modifier = Modifier
                                .size(clockSize)
                                .background(RewindInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = size.minDimension / 2f
                                drawCircle(
                                    color = RewindCream.copy(alpha = 0.16f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                repeat(12) { index ->
                                    val angle = Math.toRadians(index * 30.0 - 90.0)
                                    val startR = radius * 0.82f
                                    val endR = radius * 0.94f
                                    drawLine(
                                        color = RewindCream.copy(alpha = 0.55f),
                                        start = Offset(
                                            center.x + cos(angle).toFloat() * startR,
                                            center.y + sin(angle).toFloat() * startR
                                        ),
                                        end = Offset(
                                            center.x + cos(angle).toFloat() * endR,
                                            center.y + sin(angle).toFloat() * endR
                                        ),
                                        strokeWidth = if (index % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                val targetDegrees = ((peakHour % 12) / 12f) * 360f - 90f
                                val currentDegrees = -90f + (targetDegrees + 90f) * hand.value
                                val currentAngle = Math.toRadians(currentDegrees.toDouble())
                                val handEnd = Offset(
                                    center.x + cos(currentAngle).toFloat() * radius * 0.64f,
                                    center.y + sin(currentAngle).toFloat() * radius * 0.64f
                                )
                                drawLine(
                                    color = RewindLime,
                                    start = center,
                                    end = handEnd,
                                    strokeWidth = 6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(RewindPink, 8.dp.toPx(), center)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(clockSize * 0.54f))
                                Text(
                                    text = formatHourLabel(data.stats.mostActiveHour?.hour),
                                    color = RewindCream,
                                    fontSize = if (compact) 26.sp else 31.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "PEAK HOUR",
                                    color = RewindLime,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.9.sp
                                )
                            }
                        }
                    }
                }

                RewindReveal(active, 1_100) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewindInk.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "LISTENING BY DAY",
                            color = RewindLime,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(9.dp))
                        DailyBars(data = data, progress = bars.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBars(
    data: RewindData,
    progress: Float
) {
    val stats = data.dailyStats.take(7)
    val max = stats.maxOfOrNull { it.minutes }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val gap = 7.dp.toPx()
            val barWidth = (size.width - gap * 6f) / 7f
            stats.forEachIndexed { index, stat ->
                val local = ((progress * 1.5f) - index * 0.08f).coerceIn(0f, 1f)
                val ratio = stat.minutes.toFloat() / max.toFloat()
                val height = size.height * ratio * local
                val left = index * (barWidth + gap)
                drawRoundRect(
                    color = if (stat == data.stats.mostActiveDay) RewindLime else RewindPink,
                    topLeft = Offset(left, size.height - height),
                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stats.forEach { stat ->
                Text(
                    text = stat.dayOfWeek.take(1).uppercase(),
                    color = RewindCream.copy(alpha = 0.55f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

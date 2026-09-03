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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import kotlinx.coroutines.delay
import java.time.Year
import kotlin.math.roundToInt

@Composable
fun RewindListeningDaysCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val daysInYear = Year.of(data.year).length()
    val ratio = (data.daysWithMusic.toFloat() / daysInYear.toFloat()).coerceIn(0f, 1f)
    val bar = remember(data.daysWithMusic, data.year) { Animatable(0f) }

    LaunchedEffect(active, ratio) {
        if (!active) {
            bar.snapTo(0f)
        } else {
            delay(700)
            bar.animateTo(ratio, tween(1_050, easing = FastOutSlowInEasing))
        }
    }

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindBlue,
        progressColor = RewindCream,
        onNext = onNext
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("DAYS WITH MUSIC • ${data.year}", RewindLime)
                }

                Spacer(Modifier.height(11.dp))

                RewindReveal(active, 110, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOU LISTENED ON",
                        color = RewindCream,
                        fontSize = if (compact) 34.sp else 40.sp,
                        lineHeight = if (compact) 32.sp else 37.sp,
                        letterSpacing = (-1.9).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        RewindAnimatedNumber(
                            value = data.daysWithMusic.toLong(),
                            active = active,
                            color = RewindCream,
                            fontSize = if (compact) 100 else 126,
                            delayMillis = 330,
                            durationMillis = 1_250
                        )
                        Text(
                            text = "DAYS",
                            color = RewindLime,
                            fontSize = 25.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp
                        )
                    }
                }

                RewindReveal(active, 760) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewindInk, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "YOU LISTENED ON",
                                color = RewindCream.copy(alpha = 0.55f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${(ratio * 100f).roundToInt()}% OF THE YEAR",
                                color = RewindLime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .background(RewindCream.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bar.value)
                                    .height(9.dp)
                                    .background(RewindLime, RoundedCornerShape(100.dp))
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${formatRewindNumber((daysInYear - data.daysWithMusic).coerceAtLeast(0).toLong())} days without listening.",
                            color = RewindCream.copy(alpha = 0.52f),
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

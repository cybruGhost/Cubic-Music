package app.it.fast4x.rimusic.ui.screens.rewind.slides

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData

@Composable
fun RewindTotalTimeCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindOrange,
        progressColor = RewindInk,
        onNext = onNext
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val discSize = if (compact) 235.dp else 282.dp
            val numberSize = when {
                data.stats.totalMinutes >= 100_000 -> if (compact) 48 else 58
                data.stats.totalMinutes >= 10_000 -> if (compact) 57 else 69
                else -> if (compact) 66 else 80
            }

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 50, direction = RewindRevealDirection.Left) {
                    RewindKicker("YOUR LISTENING TIME • ${data.year}", RewindLime)
                }

                Spacer(Modifier.height(11.dp))

                RewindReveal(active, 130, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOU LISTENED FOR",
                        color = RewindInk,
                        fontSize = if (compact) 33.sp else 39.sp,
                        lineHeight = if (compact) 31.sp else 36.sp,
                        letterSpacing = (-1.9).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.height(3.dp))

                RewindReveal(active, 260) {
                    Text(
                        text = "TOTAL LISTENING TIME",
                        color = RewindInk.copy(alpha = 0.67f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(discSize + 34.dp)) {
                        drawCircle(RewindLime.copy(alpha = 0.90f))
                    }

                    RewindReveal(active, 420, scaleFrom = 0.64f) {
                        Box(
                            modifier = Modifier
                                .size(discSize)
                                .background(RewindInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "YOU LISTENED FOR",
                                    color = RewindCream.copy(alpha = 0.58f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.0.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                RewindAnimatedNumber(
                                    value = data.stats.totalMinutes,
                                    active = active,
                                    color = RewindLime,
                                    fontSize = numberSize,
                                    delayMillis = 620,
                                    durationMillis = 1_300
                                )
                                Text(
                                    text = "MINUTES",
                                    color = RewindCream,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.8.sp
                                )
                            }
                        }
                    }
                }

                RewindReveal(active, 1_180) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        TotalMetric(
                            label = "PLAYS",
                            value = formatRewindNumber(data.stats.totalPlays.toLong()),
                            background = RewindInk,
                            foreground = RewindCream,
                            modifier = Modifier.weight(1f)
                        )
                        TotalMetric(
                            label = "DAYS",
                            value = formatRewindNumber(data.daysWithMusic.toLong()),
                            background = RewindLime,
                            foreground = RewindInk,
                            modifier = Modifier.weight(1f)
                        )
                        TotalMetric(
                            label = "HOURS",
                            value = formatRewindNumber(data.stats.totalMinutes / 60L),
                            background = RewindCream,
                            foreground = RewindInk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalMetric(
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            color = foreground,
            fontSize = 20.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = label,
            color = foreground.copy(alpha = 0.65f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp
        )
    }
}

package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import kotlinx.coroutines.delay

@Composable
fun RewindIntroCard(
    data: RewindData,
    username: String,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    var revealComplete by remember { mutableStateOf(false) }

    LaunchedEffect(active) {
        revealComplete = false
        if (active) {
            delay(2_950)
            revealComplete = true
        }
    }

    val displayName = username.trim().ifBlank { "Music Fan" }

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindPurple,
        progressColor = RewindCream,
        onNext = if (revealComplete) onNext else null,
        backgroundArt = {
            Canvas(Modifier.fillMaxSize()) {
                // Deliberately angular. No records, wheels or circular "fingerprint" artwork.
                val orange = Path().apply {
                    moveTo(size.width * 0.76f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.48f)
                    lineTo(size.width * 0.91f, size.height * 0.40f)
                    close()
                }
                drawPath(orange, RewindOrange)

                val pink = Path().apply {
                    moveTo(0f, size.height * 0.73f)
                    lineTo(size.width * 0.48f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(pink, RewindPink)

                val inkSlash = Path().apply {
                    moveTo(size.width * 0.88f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.25f)
                    lineTo(size.width * 0.94f, size.height * 0.20f)
                    close()
                }
                drawPath(inkSlash, RewindInk)
            }
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val nameSize = when {
                displayName.length > 18 -> if (compact) 30.sp else 34.sp
                displayName.length > 12 -> if (compact) 34.sp else 39.sp
                else -> if (compact) 39.sp else 45.sp
            }

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 50, direction = RewindRevealDirection.Left, distance = 18.dp) {
                    RewindKicker("CUBIC MUSIC • ${data.year}", RewindLime)
                }

                Spacer(Modifier.height(if (compact) 14.dp else 18.dp))

                RewindReveal(active, 220, direction = RewindRevealDirection.Left, distance = 30.dp) {
                    Text(
                        text = "$displayName,",
                        color = RewindLime,
                        fontSize = nameSize,
                        lineHeight = nameSize,
                        letterSpacing = (-1.8).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.90f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                RewindReveal(active, 430, direction = RewindRevealDirection.Left, distance = 36.dp) {
                    Text(
                        text = "THIS IS YOUR",
                        color = RewindCream,
                        fontSize = if (compact) 37.sp else 44.sp,
                        lineHeight = if (compact) 36.sp else 42.sp,
                        letterSpacing = (-2.1).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                RewindReveal(active, 650, direction = RewindRevealDirection.Right, distance = 46.dp) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = data.year.toString(),
                            color = RewindCream,
                            fontSize = if (compact) 78.sp else 94.sp,
                            lineHeight = if (compact) 70.sp else 84.sp,
                            letterSpacing = (-6.2).sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer {
                                scaleX = 0.88f
                            }
                        )
                        Text(
                            text = ".",
                            color = RewindPink,
                            fontSize = if (compact) 66.sp else 78.sp,
                            lineHeight = if (compact) 64.sp else 76.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                RewindReveal(active, 850, direction = RewindRevealDirection.Left, distance = 32.dp) {
                    Text(
                        text = "REWIND.",
                        color = RewindInk,
                        fontSize = if (compact) 48.sp else 57.sp,
                        lineHeight = if (compact) 45.sp else 53.sp,
                        letterSpacing = (-3.0).sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(RewindOrange, RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(if (compact) 16.dp else 24.dp))

                RewindListeningSignature(
                    data = data,
                    active = active,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 130.dp else 164.dp)
                )

                Spacer(Modifier.weight(1f))

                RewindReveal(active, 1_900, direction = RewindRevealDirection.Up) {
                    Text(
                        text = "A year of music. Yours.",
                        color = RewindCream,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(14.dp))

                RewindReveal(active, 2_180, scaleFrom = 0.94f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "START  →",
                            color = RewindInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(RewindLime, RoundedCornerShape(100.dp))
                                .padding(horizontal = 26.dp, vertical = 12.dp)
                        )

                        Text(
                            text = if (revealComplete) "TAP TO BEGIN" else "OPENING…",
                            color = RewindCream.copy(alpha = 0.52f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/**
 * Personal non-spoiler signature. Bars are seeded by the listener's totals but expose no winners
 * and no exact values. They rise once with the intro and then stay still.
 */
@Composable
private fun RewindListeningSignature(
    data: RewindData,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val reveal = remember(data.year) { Animatable(0f) }

    LaunchedEffect(active, data.year) {
        reveal.snapTo(0f)
        if (active) {
            delay(1_050)
            reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    val seed = (
        data.stats.totalMinutes +
            data.stats.totalPlays.toLong() * 13L +
            data.totalUniqueSongs.toLong() * 29L +
            data.daysWithMusic.toLong() * 47L
        ).coerceAtLeast(1L)

    Canvas(modifier) {
        val count = 23
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        val baseline = size.height * 0.90f

        repeat(count) { index ->
            val mixed = (seed + index * 97L + index * index * 31L)
            val fraction = 0.18f + ((mixed % 73L).toFloat() / 100f)
            val localDelay = (index % 7) * 0.045f
            val localProgress = ((reveal.value - localDelay) / (1f - localDelay)).coerceIn(0f, 1f)
            val height = size.height * fraction * localProgress
            val x = index * (barWidth + gap)
            val color = when (index % 5) {
                0 -> RewindLime
                1 -> RewindPink
                2 -> RewindCream
                3 -> RewindOrange
                else -> RewindInk
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, baseline - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height.coerceAtLeast(1f)),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

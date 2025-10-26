package it.fast4x.rimusic.ui.components.themed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette
import kotlinx.coroutines.delay

@Composable
fun Loader(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val rotation = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val baseColor = colorPalette().text

    // Rotation animation
    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    // Pulse animation for the ring width
    LaunchedEffect(Unit) {
        pulse.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    // Glow intensity animation
    LaunchedEffect(Unit) {
        while (true) {
            glow.animateTo(1f, tween(800))
            glow.animateTo(0f, tween(800))
        }
    }

    Box(
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size)
        ) {
            val centerX = size.toPx() / 2
            val centerY = size.toPx() / 2
            val radius = size.toPx() * 0.4f
            
            // Calculate dynamic values
            val ringWidth = 4f + (pulse.value * 2f) // Pulsing width between 4-6px
            val glowIntensity = 0.3f + (glow.value * 0.4f) // Glow alpha between 0.3-0.7
            
            // Create gradient colors for the ring
            val gradientColors = listOf(
                baseColor.copy(alpha = 0.8f),
                baseColor.copy(alpha = 1f),
                baseColor.copy(alpha = 0.8f),
                baseColor.copy(alpha = 0.5f)
            )
            
            val sweepGradient = Brush.sweepGradient(
                colors = gradientColors,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            rotate(rotation.value) {
                // Draw the main ring with gradient
                drawCircle(
                    brush = sweepGradient,
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ringWidth
                    )
                )

                // Draw glow effect
                drawCircle(
                    color = baseColor.copy(alpha = glowIntensity * 0.3f),
                    radius = radius + ringWidth,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ringWidth * 2f
                    )
                )

                // Draw inner subtle glow
                drawCircle(
                    color = baseColor.copy(alpha = glowIntensity * 0.1f),
                    radius = radius - ringWidth,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ringWidth * 1.5f
                    )
                )

                // Draw a brighter leading edge
                val leadingAngle = rotation.value % 360
                
                drawArc(
                    color = baseColor.copy(alpha = 1f),
                    startAngle = leadingAngle - 10,
                    sweepAngle = 20f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - radius, centerY - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ringWidth * 1.2f
                    )
                )
            }

            // Draw center dot
            drawCircle(
                color = baseColor.copy(alpha = 0.6f + (pulse.value * 0.4f)),
                radius = 2f + pulse.value,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}

// Alternative simpler version with just rotation and glow
@Composable
fun SimpleGlowingLoader(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val rotation = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val baseColor = colorPalette().text

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            glow.animateTo(1f, tween(1000))
            glow.animateTo(0f, tween(1000))
        }
    }

    Box(
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size)
        ) {
            val centerX = size.toPx() / 2
            val centerY = size.toPx() / 2
            val radius = size.toPx() * 0.35f
            
            rotate(rotation.value) {
                // Main ring
                drawCircle(
                    color = baseColor,
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
                
                // Glow effect
                drawCircle(
                    color = baseColor.copy(alpha = 0.2f + (glow.value * 0.3f)),
                    radius = radius + 6f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                )
            }
            
            // Center dot
            drawCircle(
                color = baseColor,
                radius = 3f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}
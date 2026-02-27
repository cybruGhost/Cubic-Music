package app.it.fast4x.rimusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

// Correct imports based on your project structure
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX

@Composable
fun SeekBarWaved(
    value: Long,  // Changed to match SeekBarColored's pattern
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 6.dp,
    showTooltip: Boolean = true
) {
    val currentValue = value

    val isDragging = remember {
        MutableTransitionState(false)
    }

    var seekBarWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }

    var draggingValue by remember { mutableLongStateOf(currentValue) }

    LaunchedEffect(currentValue) {
        if (!isDragging.targetState) {
            draggingValue = currentValue
        }
    }

    // Get appearance from LocalAppearance
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val typography = appearance.typography

    // Get binder for media info
    val binder = LocalPlayerServiceBinder.current
    val mediaItem = binder?.player?.currentMediaItem
    
    // Check if media is local (using the constant from your service)
    val isLocal = mediaItem?.mediaId?.startsWith(LOCAL_KEY_PREFIX) == true
    
    // Format time - updates in realtime when dragging
    val timeText = remember(draggingValue, isLocal) { 
        formatAsDuration(if (isLocal) {
            draggingValue
        } else {
            draggingValue * 1000
        })
    }
    
    // Generate wave parameters based on current media - updates when media changes
    val waveParams = remember(mediaItem?.mediaId) { 
        generateSinusoidalParams(mediaItem?.mediaId ?: "default") 
    }

    val density = LocalDensity.current
    val scrubberRadiusPx = with(density) { scrubberRadius.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> seekBarWidth = coordinates.size.width }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging.targetState = true
                        val newValue = (it.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue.coerceIn(minimumValue, maximumValue)
                        onDragStart(draggingValue)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val currentX = change.position.x
                        val newValue = (currentX / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue.coerceIn(minimumValue, maximumValue)

                        acc += dragAmount / size.width * (maximumValue - minimumValue)

                        if (acc !in -1f..1f) {
                            onDrag(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging.targetState = false
                        acc = 0F
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging.targetState = false
                        acc = 0F
                        onDragEnd()
                    }
                )
            }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        val newValue = (offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        draggingValue = newValue
                        onDragStart(newValue)
                    },
                    onTap = {
                        onDragEnd()
                    }
                )
            }
            .padding(horizontal = scrubberRadius)
            .height(scrubberRadius + 44.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(scrubberRadius + 44.dp)
                .align(Alignment.TopCenter)
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (width > 0) {
                val path = Path()
                val step = 2f

                // Generate wave path
                for (x in 0..width.toInt() step step.toInt()) {
                    val y = centerY + (height * 0.5f * waveParams.amplitude *
                            sin(x * 0.02f * waveParams.frequency + waveParams.phase))

                    if (x == 0) path.moveTo(x.toFloat(), y)
                    else path.lineTo(x.toFloat(), y)
                }

                val fraction = if (maximumValue > minimumValue) {
                    ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                } else 0f
                val progressWidth = width * fraction

                // Draw background wave
                drawPath(
                    path = path,
                    color = backgroundColor,
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round
                    )
                )

                // Draw current progress wave
                withTransform({
                    clipRect(left = 0f, top = 0f, right = progressWidth, bottom = height)
                }) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        // Scrubber handle - shows when dragging
        AnimatedVisibility(
            visible = isDragging.targetState && showTooltip,
            enter = fadeIn(), 
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val fraction = if (maximumValue > minimumValue) {
                            ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                        } else 0f
                        val xPos = if (seekBarWidth > 0) {
                            (seekBarWidth * fraction) - scrubberRadiusPx
                        } else 0
                        IntOffset(x = xPos.toInt(), y = 0)
                    }
                    .size(scrubberRadius * 2)
                    .clip(CircleShape)
                    .background(scrubberColor)
            )
        }

        // Tooltip - shows when dragging with realtime time updates
        AnimatedVisibility(
            visible = isDragging.targetState && showTooltip,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val fraction = if (maximumValue > minimumValue) {
                        ((draggingValue.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val xPos = if (seekBarWidth > 0) {
                        (seekBarWidth * fraction) - (tooltipWidth / 2)
                    } else {
                        0
                    }

                    IntOffset(x = xPos.toInt(), y = -20)
                }
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> tooltipWidth = coordinates.size.width }
                    .background(
                        color = colorPalette.text,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                BasicText(
                    text = timeText,
                    style = typography.xs.copy(
                        color = colorPalette.background0,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

data class SinusoidalWaveParams(
    val frequency: Float,
    val amplitude: Float,
    val phase: Float
)

fun generateSinusoidalParams(seed: String): SinusoidalWaveParams {
    val random = Random(seed.hashCode())
    return SinusoidalWaveParams(
        frequency = random.nextFloat() * 4f + 2f,
        amplitude = random.nextFloat() * 0.6f + 0.3f,
        phase = random.nextFloat() * PI.toFloat() * 2
    )
}
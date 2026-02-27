package app.it.fast4x.rimusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun SeekBarOcean(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    scrubberRadius: Dp = 6.dp,
    shape: Shape = RectangleShape,
) {
    val minimumValueFloat = minimumValue.toFloat()
    val maximumValueFloat = maximumValue.toFloat()
    val currentValueFloat = value.toFloat()

    val isDragging = remember {
        MutableTransitionState(false)
    }

    val transition = rememberTransition(transitionState = isDragging, label = null)

    val currentAmplitude by transition.animateDp(label = "") { if (it) 0.dp else 2.dp }
    val currentScrubberHeight by transition.animateDp(label = "") {
        if (it) 20.dp else 15.dp
    }

    Box(modifier = modifier
        .pointerInput(minimumValueFloat, maximumValueFloat) {
            if (maximumValueFloat < minimumValueFloat) return@pointerInput

            var acc = 0f

            detectHorizontalDragGestures(
                onDragStart = {
                    isDragging.targetState = true
                },
                onHorizontalDrag = { _, delta ->
                    acc += delta / size.width * (maximumValueFloat - minimumValueFloat)

                    if (acc !in -1f..1f) {
                        onDrag(acc.toLong())
                        acc -= acc.toLong()
                    }
                },
                onDragEnd = {
                    isDragging.targetState = false
                    acc = 0f
                    onDragEnd()
                },
                onDragCancel = {
                    isDragging.targetState = false
                    acc = 0f
                    onDragEnd()
                }
            )
        }
        .pointerInput(minimumValueFloat, maximumValueFloat) {
            if (maximumValueFloat < minimumValueFloat) return@pointerInput

            detectTapGestures(
                onPress = { offset ->
                    val newValue = (offset.x / size.width * (maximumValueFloat - minimumValueFloat) + minimumValueFloat).roundToLong()
                    onDragStart(newValue)
                },
                onTap = {
                    onDragEnd()
                }
            )
        }
        .padding(horizontal = scrubberRadius)
        .drawWithContent {
            drawContent()
            drawOceanScrubber(value, minimumValue, maximumValue, color, currentScrubberHeight)
        }
    ) {
        OceanContent(
            backgroundColor = backgroundColor,
            amplitude = { currentAmplitude },
            value = value,
            minimumValue = minimumValue,
            maximumValue = maximumValue,
            color = color,
            shape = shape
        )
    }
}

private fun ContentDrawScope.drawOceanScrubber(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    color: Color,
    height: Dp
) {
    val scrubberPosition = if (maximumValue < minimumValue) {
        0f
    } else {
        (value.toFloat() - minimumValue) / (maximumValue - minimumValue) * size.width
    }

    drawRoundRect(
        color,
        topLeft = Offset(scrubberPosition - 5f, (size.height - height.toPx()) / 2),
        size = Size(10f, height.toPx()),
        cornerRadius = CornerRadius(5f)
    )
}

@Composable
private fun OceanContent(
    backgroundColor: Color,
    amplitude: () -> Dp,
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    color: Color,
    shape: Shape
) {
    val fraction = (value.toFloat() - minimumValue) / (maximumValue - minimumValue)
    val progress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f - fraction)
                .background(color = backgroundColor, shape = shape)
                .align(Alignment.CenterEnd)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(amplitude())
                .align(Alignment.CenterStart)
        ) {
            drawPath(
                path = oceanWavePath(size.copy(height = size.height * 2), progress),
                color = color,
                style = Stroke(width = 15f)
            )
        }
    }
}

private fun oceanWavePath(size: Size, progress: Float): Path {
    fun yFromX(x: Float) = (sin(x / 15f + progress * 2 * PI.toFloat()) + 1) * size.height / 2
    
    return Path().apply {
        moveTo(0f, yFromX(0f))
        var currentX = 0f
        while (currentX < size.width) {
            lineTo(currentX, yFromX(currentX))
            lineTo(currentX - 5, yFromX(currentX) - 5)
            currentX += 1
        }
    }
}
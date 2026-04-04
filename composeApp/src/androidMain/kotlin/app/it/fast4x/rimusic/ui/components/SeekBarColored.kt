package app.it.fast4x.rimusic.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import kotlin.math.roundToLong

@Composable
fun SeekBarColored(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 6.dp,
    shape: Shape = RectangleShape,
    crossfadePalette: List<Color>? = null,
) {

    val (colorPalette, typography) = LocalAppearance.current
    val isDragging = remember {
        MutableTransitionState(false)
    }

    val transition = rememberTransition(transitionState = isDragging, label = null)

    val currentBarHeight by transition.animateDp(label = "") { if (it) scrubberRadius else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "") { if (it) 0.dp else scrubberRadius }

    val progressFraction = if (maximumValue > minimumValue) {
        ((value.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val barColor = if (!crossfadePalette.isNullOrEmpty()) {
        val paletteIndex = ((crossfadePalette.size - 1) * progressFraction).toInt().coerceIn(0, crossfadePalette.lastIndex)
        crossfadePalette[paletteIndex]
    } else when {
        value >= 0 && value <= maximumValue / 5 -> colorPalette.text
        value >= maximumValue / 5 && value <= maximumValue / 4 -> colorPalette.background0
        value >= maximumValue / 4 && value <= maximumValue / 3 -> colorPalette.textDisabled
        value >= maximumValue / 3 && value <= maximumValue / 2 -> colorPalette.background2
        value >= maximumValue / 2 && value <= maximumValue -> colorPalette.accent
        else -> colorPalette.text
    }
    val crossfadeBrush = if (!crossfadePalette.isNullOrEmpty()) {
        Brush.horizontalGradient(crossfadePalette)
    } else {
        null
    }

    Box(
        modifier = modifier
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging.targetState = true
                    },
                    onHorizontalDrag = { _, delta ->
                        acc += delta / size.width * (maximumValue - minimumValue)

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
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        onDragStart((offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong())
                    },
                    onTap = {
                        onDragEnd()
                    }
                )
            }
            .padding(horizontal = scrubberRadius)
            .drawWithContent {
                drawContent()

                val scrubberPosition = if (maximumValue < minimumValue) {
                    0f
                } else {
                    (value.toFloat() - minimumValue) / (maximumValue - minimumValue) * size.width
                }

                drawCircle(
                    color = barColor,
                    radius = currentScrubberRadius.toPx(),
                    center = center.copy(x = scrubberPosition)
                )

            }
            .height(scrubberRadius)
    ) {

        Spacer(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth()
                .background(color = backgroundColor, shape = shape)
                .align(Alignment.Center)
        )

        if (crossfadeBrush != null) {
            Spacer(
                modifier = Modifier
                    .height(currentBarHeight)
                    .fillMaxWidth(progressFraction)
                    .background(brush = crossfadeBrush, shape = shape)
                    .align(Alignment.CenterStart)
            )
        } else {
            Spacer(
                modifier = Modifier
                    .height(currentBarHeight)
                    .fillMaxWidth(progressFraction)
                    .background(color = barColor, shape = shape)
                    .align(Alignment.CenterStart)
            )
        }
    }


}

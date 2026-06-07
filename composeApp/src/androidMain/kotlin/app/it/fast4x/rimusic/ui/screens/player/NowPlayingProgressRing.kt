package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C

@Composable
fun NowPlayingProgressOutline(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
    contentPadding: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = if (duration <= 0L || duration == C.TIME_UNSET) {
        0f
    } else {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content
        )
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val corner = cornerRadius.toPx().coerceIn(0f, (right - left).coerceAtMost(bottom - top) / 2f)
            val perimeter = ((right - left - corner * 2f) + (bottom - top - corner * 2f)) * 2f
            val active = perimeter * progress
            val brush = Brush.linearGradient(
                listOf(
                    Color(0xFF7C3AED),
                    Color(0xFFEC4899),
                    Color(0xFF22D3EE),
                    Color(0xFF34D399)
                ),
                start = Offset(left, top),
                end = Offset(right, bottom)
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(strokePx),
                alpha = 0.14f
            )
            drawProgressEdge(left, top, right, bottom, corner, active, brush, strokePx, alpha = 0.98f)
        }
    }
}

private fun DrawScope.drawProgressEdge(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    corner: Float,
    distance: Float,
    brush: Brush,
    strokePx: Float,
    alpha: Float
) {
    var remaining = distance.coerceAtLeast(0f)

    fun drawSegment(start: Offset, end: Offset, length: Float) {
        if (remaining <= 0f) return
        val drawLength = remaining.coerceAtMost(length)
        val fraction = if (length == 0f) 0f else drawLength / length
        drawLine(
            brush = brush,
            start = start,
            end = Offset(
                x = start.x + (end.x - start.x) * fraction,
                y = start.y + (end.y - start.y) * fraction
            ),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
            alpha = alpha
        )
        remaining -= drawLength
    }

    drawSegment(Offset(left + corner, top), Offset(right - corner, top), right - left - corner * 2f)
    drawSegment(Offset(right, top + corner), Offset(right, bottom - corner), bottom - top - corner * 2f)
    drawSegment(Offset(right - corner, bottom), Offset(left + corner, bottom), right - left - corner * 2f)
    drawSegment(Offset(left, bottom - corner), Offset(left, top + corner), bottom - top - corner * 2f)
}

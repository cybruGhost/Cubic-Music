package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import app.kreate.android.R

@Composable
internal fun RingPlayerHeader(
    nowPlayingFrom: String,
    foreground: Color,
    onDismiss: () -> Unit,
    onAppearance: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RingActionButton(
            icon = R.drawable.chevron_down,
            tint = foreground,
            iconSize = 28,
            onClick = onDismiss,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.now_playing),
                color = foreground,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.9f),
                        offset = Offset(0f, 1f),
                        blurRadius = 4f,
                    )
                ),
            )
            if (nowPlayingFrom.isNotBlank()) {
                Text(
                    text = nowPlayingFrom,
                    color = foreground.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                )
            }
        }
        RingActionButton(
            icon = R.drawable.color_palette,
            tint = foreground,
            iconSize = 24,
            onClick = onAppearance,
        )
    }
}

@Composable
internal fun RingLinearSeekBar(
    mediaId: String,
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    trackColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val playbackFraction = if (duration > 0L) {
        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var dragFraction by remember(mediaId) { mutableStateOf<Float?>(null) }
    val shownFraction = dragFraction ?: playbackFraction

    fun fractionAt(x: Float, width: Int): Float =
        if (width <= 0) 0f else (x / width.toFloat()).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .height(22.dp)
            .pointerInput(mediaId, duration) {
                detectTapGestures { position ->
                    if (duration > 0L) {
                        onSeek((fractionAt(position.x, size.width) * duration).toLong())
                    }
                }
            }
            .pointerInput(mediaId, duration) {
                detectDragGestures(
                    onDragStart = { position ->
                        if (duration > 0L) dragFraction = fractionAt(position.x, size.width)
                    },
                    onDrag = { change, _ ->
                        if (duration > 0L) {
                            dragFraction = fractionAt(change.position.x, size.width)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeek((it * duration).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                )
            },
    ) {
        val centerY = size.height / 2f
        val endX = size.width * shownFraction.coerceIn(0f, 1f)
        val stroke = 3.dp.toPx()
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(endX, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = activeColor,
            radius = if (dragFraction == null) 4.dp.toPx() else 6.dp.toPx(),
            center = Offset(endX, centerY),
        )
    }
}

@Composable
internal fun RingBottomActions(
    foreground: Color,
    accent: Color,
    crossfadeEnabled: Boolean,
    onSleepTimer: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
    ) {
        RingActionButton(
            icon = R.drawable.sleep,
            tint = foreground,
            iconSize = 24,
            onClick = onSleepTimer,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (crossfadeEnabled) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                contentColor = accent,
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = stringResource(R.string.crossfade_active_badge),
                    color = accent,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
        RingActionButton(
            icon = R.drawable.ellipsis_horizontal,
            tint = foreground,
            iconSize = 24,
            onClick = onMore,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun RingActionButton(
    icon: Int,
    tint: Color,
    iconSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

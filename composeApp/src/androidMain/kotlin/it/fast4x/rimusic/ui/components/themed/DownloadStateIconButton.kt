@file:kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
package it.fast4x.rimusic.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.utils.DOWNLOAD_INDICATOR_SIZE_NORMAL
import it.fast4x.rimusic.utils.getDownloadProgress

@UnstableApi
@Composable
fun DownloadStateIconButton(
    onClick: () -> Unit,
    onCancelButtonClicked: () -> Unit,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = null,
    downloadState: Int,
    mediaId: String? = null
) {
    val progress = if (mediaId != null) getDownloadProgress(mediaId) else 0f

    if (downloadState == Download.STATE_DOWNLOADING
                || downloadState == Download.STATE_QUEUED
                || downloadState == Download.STATE_RESTARTING
                ) {
        Box(
            modifier = Modifier
                .clickable(
                    indication = indication ?: ripple(bounded = false, radius = 24.dp),
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = enabled,
                    onClick = onCancelButtonClicked
                )
                .then(modifier)
                .size(DOWNLOAD_INDICATOR_SIZE_NORMAL.dp),
            contentAlignment = Alignment.Center
        ) {
            // Minimal Ring Progress Indicator - Clean and Elegant
            MinimalRingProgress(
                progress = progress,
                color = colorPalette().accent,
                trackColor = colorPalette().textDisabled,
                showPercentage = true,
                modifier = Modifier.size(DOWNLOAD_INDICATOR_SIZE_NORMAL.dp)
            )
        }
    } else {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                .clickable(
                    indication = indication ?: ripple(bounded = false),
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = enabled,
                    onClick = onClick
                )
                .then(modifier)
                .size(DOWNLOAD_INDICATOR_SIZE_NORMAL.dp)
        )
    }
}

@Composable
fun MinimalRingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = colorPalette().accent,
    trackColor: Color = colorPalette().textDisabled,
    showPercentage: Boolean = true,
    strokeWidth: Dp = 2.5.dp,
    textColor: Color = colorPalette().accent
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Canvas for the ring progress
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Calculate radius (subtract half stroke width to keep it inside bounds)
            val radius = (minOf(canvasWidth, canvasHeight) - strokeWidthPx) / 2
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

            // Draw track circle (background)
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )

            // Draw progress arc (foreground)
            val sweepAngle = 360f * progress.coerceIn(0f, 1f)
            
            // Rotate -90 degrees so progress starts from top
            rotate(degrees = -90f, pivot = center) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        x = center.x - radius,
                        y = center.y - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = radius * 2,
                        height = radius * 2
                    )
                )
            }
        }
        
        // Percentage text in the center
        if (showPercentage && progress > 0.01f) {
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

// Optional: Alternative version without percentage (for when you want a cleaner look)
@Composable
fun MinimalRingProgressNoPercentage(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = colorPalette().accent,
    trackColor: Color = colorPalette().textDisabled
) {
    MinimalRingProgress(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        showPercentage = false
    )
}
package app.it.fast4x.rimusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Song
import app.it.fast4x.rimusic.utils.LoadImage
import player.PlayerController
import player.frame.FramePlayer
import player.frame.FrameRenderer

// ── Palette ───────────────────────────────────────────────────────────────────
private object MPColor {
    val bg          = Color(0xF00D1117)
    val surface     = Color(0xCC161B22)
    val glass       = Color(0x881E2D3D)
    val border      = Color(0xFF21303F)
    val accent      = Color(0xFF1DB954)
    val accentDim   = Color(0xFF14532D)
    val textPrimary = Color(0xFFEEF2FF)
    val textSecond  = Color(0xFF8899AA)
    val textDim     = Color(0xFF445566)
}

/**
 * Glassy bottom mini-player bar.
 *
 * Layout:
 *  [  Album art + track info  |  FramePlayer (video preview + controls)  ]
 */
@Composable
fun MiniPlayer(
    frameController : PlayerController,
    frameRenderer   : FrameRenderer,
    url             : String?,
    song            : Song?,
    onExpandAction  : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MPColor.bg)
            .drawBehind {
                // Top border glow
                drawLine(MPColor.border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                // Accent glow when playing
                if (url != null) {
                    drawLine(
                        brush       = Brush.horizontalGradient(
                            listOf(Color.Transparent, MPColor.accent.copy(0.5f), Color.Transparent)
                        ),
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Track info panel ─────────────────────────────────────────────
            TrackInfoPanel(
                song           = song,
                onExpandAction = onExpandAction,
                modifier       = Modifier.width(280.dp)
            )

            // Thin divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MPColor.border)
            )

            // ── Frame player (video + transport controls) ─────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                FramePlayer(
                    modifier        = Modifier.fillMaxWidth(),
                    url             = url ?: "",
                    size            = frameRenderer.size.collectAsState(0 to 0).value.let { (width: Int, height: Int) ->
                        if (width > 0 && height > 0) IntSize(width, height) else IntSize.Zero
                    },
                    bytes           = frameRenderer.bytes.collectAsState(null).value,
                    controller      = frameController,
                    showControls    = true
                )
            }
        }
    }
}

// ── Track info panel ──────────────────────────────────────────────────────────
@Composable
private fun TrackInfoPanel(
    song           : Song?,
    onExpandAction : () -> Unit,
    modifier       : Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered         by interactionSource.collectIsHoveredAsState()
    val bgAlpha           by animateFloatAsState(if (isHovered) 1f else 0f, tween(150))

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MPColor.glass.copy(alpha = bgAlpha))
            .hoverable(interactionSource)
            .clickable(interactionSource, indication = null) { onExpandAction() }
            .padding(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MPColor.accentDim),
            contentAlignment = Alignment.Center
        ) {
            if (song?.thumbnailUrl != null) {
                LoadImage(song.thumbnailUrl)
            } else {
                Text(
                    text     = if (song != null) "♫" else "♩",
                    color    = MPColor.accent,
                    fontSize = 22.sp
                )
            }
        }

        // Title + artist
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text       = song?.title ?: "No track selected",
                color      = if (song != null) MPColor.textPrimary else MPColor.textDim,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = song?.artistsText ?: "—",
                color    = MPColor.textSecond,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song?.durationText != null) {
                Text(
                    text     = song.durationText,
                    color    = MPColor.textDim,
                    fontSize = 10.sp
                )
            }
        }

        // Expand chevron
        if (song != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MPColor.glass)
                    .clickable { onExpandAction() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌃", color = MPColor.textSecond, fontSize = 14.sp)
            }
        }
    }
}

package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Song
import player.PlayerController

@Composable
internal fun CubicMiniPlayer(
    controller: PlayerController,
    song: Song?,
    isResolving: Boolean,
    canGoNext: Boolean,
    onNext: () -> Unit,
    onRestore: () -> Unit
) {
    val state by controller.state.collectAsState()
    val progress = if (state.duration > 0L) (state.timestamp.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
    val glassShape = RoundedCornerShape(25.dp)

    Row(
        Modifier.fillMaxSize().padding(6.dp).clip(glassShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xE82A3324), Color(0xF21B1D1A), Color(0xE8242821))
                )
            )
            .border(1.dp, CubicColors.Accent.copy(alpha = 0.34f), glassShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CubicArtwork(song?.thumbnailUrl, Modifier.size(64.dp), 18.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(song?.title ?: "Cubic Music", color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(if (isResolving) "Preparing stream" else song?.artistsText ?: "Nothing playing", color = CubicColors.TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color = CubicColors.Accent,
                trackColor = CubicColors.TextMuted.copy(alpha = 0.24f)
            )
        }
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(CubicColors.Accent)
                .clickable(enabled = song != null && !isResolving) { if (state.isPlaying) controller.pause() else controller.play() },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "Pause" else "Play", tint = CubicColors.Background, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.SkipNext, "Next", tint = if (canGoNext) CubicColors.TextSecondary else CubicColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = controller::toggleSound, enabled = song != null, modifier = Modifier.size(32.dp)) {
            Icon(if (state.isMuted || state.volume == 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, "Mute", tint = CubicColors.TextSecondary, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.OpenInFull, "Restore Cubic Music", tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
        }
    }
}

package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import database.entities.Song
import database.entities.SongEntity
import extension.formatTimestamp
import player.PlayerController
import kotlin.math.roundToLong

@Composable
internal fun CubicNowPlayingPanel(
    currentSong: Song?,
    isResolving: Boolean,
    playbackMessage: String?,
    recentSongs: List<SongEntity>,
    onToggleFavorite: () -> Unit,
    onSongClick: (Song) -> Unit,
    onOpenPlayer: () -> Unit
) {
    Column(
        modifier = Modifier.width(318.dp).fillMaxHeight().background(CubicColors.Panel).padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Text("Now Playing", color = CubicColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CubicColors.PanelRaised)
                .clickable(enabled = currentSong != null, onClick = onOpenPlayer)
                .padding(16.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CubicArtwork(currentSong?.thumbnailUrl, Modifier.size(190.dp), 20.dp)
                if (isResolving) {
                    Box(Modifier.size(190.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CubicColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(currentSong?.title ?: "Choose a song", color = CubicColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            isResolving -> "Preparing stream"
                            !playbackMessage.isNullOrBlank() && playbackMessage != "Playing" -> playbackMessage
                            currentSong != null -> currentSong.artistsText.orEmpty()
                            else -> "Browse the live catalog"
                        },
                        color = if (!playbackMessage.isNullOrBlank() && playbackMessage != "Playing") CubicColors.Danger else CubicColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (currentSong != null) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp)) {
                        Icon(
                            if (currentSong.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "Favorite",
                            tint = if (currentSong.isLiked) CubicColors.Accent else CubicColors.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.QueueMusic, null, tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
            Text("Recently played", color = CubicColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))

        if (recentSongs.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Your listening history will appear here.", color = CubicColors.TextMuted, fontSize = 11.sp)
            }
        } else {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                recentSongs.take(5).forEach { entity ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable { onSongClick(entity.song) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CubicArtwork(entity.song.thumbnailUrl, Modifier.size(42.dp), 10.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(entity.song.title, color = if (entity.song.id == currentSong?.id) CubicColors.Accent else CubicColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entity.song.artistsText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CubicPlayerRail(
    controller: PlayerController,
    currentSong: Song?,
    isResolving: Boolean,
    playbackMessage: String?,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(86.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF242522), Color(0xFF181916), Color(0xFF242522))))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CubicArtwork(currentSong?.thumbnailUrl, Modifier.size(58.dp), 29.dp)
        Column(
            modifier = Modifier.width(210.dp).clickable(enabled = currentSong != null, onClick = onOpenPlayer),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(currentSong?.title ?: "No song selected", color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    isResolving -> "Preparing stream"
                    !playbackMessage.isNullOrBlank() && playbackMessage != "Playing" -> playbackMessage
                    currentSong != null -> currentSong.artistsText.orEmpty()
                    else -> "Choose music from Browse"
                },
                color = if (!playbackMessage.isNullOrBlank() && playbackMessage != "Playing") CubicColors.Danger else CubicColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CubicPlaybackControls(controller, enabled = currentSong != null && !isResolving, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CubicPlaybackControls(controller: PlayerController, enabled: Boolean, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val position by animateFloatAsState(state.timestamp.toFloat())

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(48.dp).background(if (enabled) CubicColors.Accent else CubicColors.Selection, CircleShape).clickable(enabled = enabled) {
                if (state.isPlaying) controller.pause() else controller.play()
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "Pause" else "Play", tint = CubicColors.Background, modifier = Modifier.size(25.dp))
        }
        Text(state.timestamp.formatTimestamp(), color = CubicColors.TextMuted, fontSize = 10.sp)
        Slider(
            value = position.coerceIn(0f, state.duration.toFloat().coerceAtLeast(1f)),
            onValueChange = { controller.seekTo(it.roundToLong()) },
            enabled = enabled,
            valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = CubicColors.Accent,
                activeTrackColor = CubicColors.Accent,
                inactiveTrackColor = CubicColors.TextMuted.copy(alpha = 0.42f),
                disabledThumbColor = CubicColors.TextMuted,
                disabledActiveTrackColor = CubicColors.TextMuted
            ),
            modifier = Modifier.weight(1f)
        )
        Text(state.duration.formatTimestamp(), color = CubicColors.TextMuted, fontSize = 10.sp)
        IconButton(onClick = controller::toggleSound, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(if (state.isMuted || state.volume == 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, "Volume", tint = CubicColors.TextSecondary, modifier = Modifier.size(19.dp))
        }
        Slider(
            value = state.volume,
            onValueChange = controller::setVolume,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = CubicColors.Text, activeTrackColor = CubicColors.Text, inactiveTrackColor = CubicColors.TextMuted.copy(alpha = 0.4f)),
            modifier = Modifier.width(94.dp)
        )
    }
}

@Composable
internal fun CubicExpandedPlayerDialog(
    controller: PlayerController,
    currentSong: Song,
    isResolving: Boolean,
    playbackMessage: String?,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.76f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight(0.72f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(listOf(CubicColors.PanelRaised, CubicColors.Window)))
                    .clickable(enabled = false) {}
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("NOW PLAYING", color = CubicColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = CubicColors.TextSecondary) }
                }
                CubicArtwork(currentSong.thumbnailUrl, Modifier.weight(1f).fillMaxWidth(0.64f), 24.dp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(currentSong.title, color = CubicColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (isResolving) "Preparing stream" else playbackMessage?.takeUnless { it == "Playing" } ?: currentSong.artistsText.orEmpty(),
                            color = if (!playbackMessage.isNullOrBlank() && playbackMessage != "Playing") CubicColors.Danger else CubicColors.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(if (currentSong.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (currentSong.isLiked) CubicColors.Accent else CubicColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                CubicPlaybackControls(controller, enabled = !isResolving, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

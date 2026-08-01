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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import database.entities.Song
import extension.formatTimestamp
import player.PlayerController
import kotlin.math.roundToLong

private enum class CubicPlayerTab { UpNext, Lyrics }

@Composable
internal fun CubicNowPlayingPanelV2(
    currentSong: Song?,
    isResolving: Boolean,
    playbackMessage: String?,
    queue: List<Song>,
    currentQueueIndex: Int,
    syncedLyrics: String?,
    plainLyrics: String?,
    lyricsTimestampMs: Long,
    lyricsLoading: Boolean,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    onToggleFavorite: () -> Unit,
    playlistNames: List<String>,
    onSongClick: (Int) -> Unit,
    onAddToPlaylist: (Song, String) -> Unit,
    onDownload: (Song) -> Unit,
    onOpenPlayer: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CubicPlayerTab.UpNext) }
    Column(Modifier.width(326.dp).fillMaxHeight().background(CubicColors.Panel).padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Now Playing", color = CubicColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CubicColors.PanelRaised)
                .clickable(enabled = currentSong != null, onClick = onOpenPlayer).padding(15.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CubicArtwork(currentSong?.thumbnailUrl, Modifier.size(184.dp), 19.dp)
                if (isResolving) Box(Modifier.size(184.dp).background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CubicColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(29.dp))
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(currentSong?.title ?: "Choose a song", color = CubicColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(playerSubtitle(currentSong, isResolving, playbackMessage), color = playerSubtitleColor(playbackMessage), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                currentSong?.let { song ->
                    DownloadButton(song, downloadedIds, downloadProgress, onDownload)
                    PlaylistAddButton(song, playlistNames, onAddToPlaylist)
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (song.isLiked) CubicColors.Accent else CubicColors.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        CubicPlayerTabs(selectedTab, onSelected = { selectedTab = it })
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            CubicPlayerTab.UpNext -> CubicQueue(queue, currentQueueIndex, downloadedIds, downloadProgress, onSongClick, onDownload, Modifier.weight(1f))
            CubicPlayerTab.Lyrics -> CubicSynchronizedLyricsPane(syncedLyrics, plainLyrics, lyricsTimestampMs, lyricsLoading, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CubicPlayerTabs(selected: CubicPlayerTab, onSelected: (CubicPlayerTab) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(CubicColors.Selection).padding(3.dp)) {
        CubicTab("Up next", Icons.Rounded.QueueMusic, selected == CubicPlayerTab.UpNext, { onSelected(CubicPlayerTab.UpNext) }, Modifier.weight(1f))
        CubicTab("Lyrics", Icons.Rounded.Lyrics, selected == CubicPlayerTab.Lyrics, { onSelected(CubicPlayerTab.Lyrics) }, Modifier.weight(1f))
    }
}

@Composable
private fun CubicTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) CubicColors.PanelRaised else Color.Transparent).clickable(onClick = onClick).padding(vertical = 9.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected) CubicColors.Accent else CubicColors.TextMuted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = if (selected) CubicColors.Text else CubicColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CubicQueue(
    queue: List<Song>, currentQueueIndex: Int, downloadedIds: Set<String>, downloadProgress: Map<String, Float>,
    onSongClick: (Int) -> Unit, onDownload: (Song) -> Unit, modifier: Modifier
) {
    val upcoming = queue.withIndex().filter { it.index > currentQueueIndex }
    if (upcoming.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Discovering more radio tracks...", color = CubicColors.TextMuted, fontSize = 11.sp)
        }
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(upcoming, key = { _, entry -> "${entry.index}-${entry.value.id}" }) { _, entry ->
            val index = entry.index
            val song = entry.value
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.Transparent)
                    .clickable { onSongClick(index) }.padding(horizontal = 7.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                CubicArtwork(song.thumbnailUrl, Modifier.size(42.dp), 9.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(song.title, color = CubicColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistsText.orEmpty(), color = CubicColors.TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DownloadButton(song, downloadedIds, downloadProgress, onDownload)
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun CubicLyrics(lyrics: String?, loading: Boolean, modifier: Modifier) {
    when {
        loading -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CubicColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp)) }
        lyrics.isNullOrBlank() -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Lyrics are not available for this track.", color = CubicColors.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center) }
        else -> LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lyrics.cleanLyricsLines().size) { index ->
                Text(lyrics.cleanLyricsLines()[index], color = CubicColors.TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun DownloadButton(song: Song, downloadedIds: Set<String>, downloadProgress: Map<String, Float>, onDownload: (Song) -> Unit) {
    val progress = downloadProgress[song.id]
    when {
        progress != null && progress < 1f -> Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { progress }, color = CubicColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
        song.id in downloadedIds -> Icon(Icons.Rounded.DownloadDone, "Downloaded", tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
        else -> IconButton(onClick = { onDownload(song) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Download, "Download", tint = CubicColors.TextSecondary, modifier = Modifier.size(19.dp)) }
    }
}

@Composable
private fun PlaylistAddButton(song: Song, playlistNames: List<String>, onAdd: (Song, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.PlaylistAdd, "Add to playlist", tint = CubicColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (playlistNames.isEmpty()) {
                DropdownMenuItem(text = { Text("Create a playlist first") }, onClick = {}, enabled = false)
            } else playlistNames.forEach { playlist ->
                DropdownMenuItem(
                    text = { Text(playlist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onAdd(song, playlist); expanded = false }
                )
            }
        }
    }
}

@Composable
internal fun CubicPlayerRailV2(
    controller: PlayerController,
    currentSong: Song?,
    isResolving: Boolean,
    playbackMessage: String?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.height(86.dp).shadow(24.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF242522), Color(0xFF181916), Color(0xFF242522)))).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        CubicArtwork(currentSong?.thumbnailUrl, Modifier.size(58.dp), 16.dp)
        Column(Modifier.width(185.dp).clickable(enabled = currentSong != null, onClick = onOpenPlayer), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(currentSong?.title ?: "No song selected", color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(playerSubtitle(currentSong, isResolving, playbackMessage), color = playerSubtitleColor(playbackMessage), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CubicPlaybackControlsV2(controller, currentSong != null && !isResolving, canGoPrevious, canGoNext, onPrevious, onNext, Modifier.weight(1f))
    }
}

@Composable
private fun CubicPlaybackControlsV2(
    controller: PlayerController, enabled: Boolean, canGoPrevious: Boolean, canGoNext: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val position by animateFloatAsState(state.timestamp.toFloat())
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious, modifier = Modifier.size(33.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = if (canGoPrevious) CubicColors.TextSecondary else CubicColors.TextMuted, modifier = Modifier.size(21.dp)) }
        Box(Modifier.size(46.dp).background(if (enabled) CubicColors.Accent else CubicColors.Selection, CircleShape).clickable(enabled = enabled) { if (state.isPlaying) controller.pause() else controller.play() }, contentAlignment = Alignment.Center) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "Pause" else "Play", tint = CubicColors.Background, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(33.dp)) { Icon(Icons.Rounded.SkipNext, "Next", tint = if (canGoNext) CubicColors.TextSecondary else CubicColors.TextMuted, modifier = Modifier.size(21.dp)) }
        Text(state.timestamp.formatTimestamp(), color = CubicColors.TextMuted, fontSize = 9.sp)
        Slider(
            value = position.coerceIn(0f, state.duration.toFloat().coerceAtLeast(1f)), onValueChange = { controller.seekTo(it.roundToLong()) }, enabled = enabled,
            valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = CubicColors.Accent, activeTrackColor = CubicColors.Accent, inactiveTrackColor = CubicColors.TextMuted.copy(alpha = 0.42f), disabledThumbColor = CubicColors.TextMuted, disabledActiveTrackColor = CubicColors.TextMuted),
            modifier = Modifier.weight(1f)
        )
        Text(state.duration.formatTimestamp(), color = CubicColors.TextMuted, fontSize = 9.sp)
        IconButton(onClick = controller::toggleSound, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(if (state.isMuted || state.volume == 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, "Mute", tint = CubicColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Slider(value = state.volume.coerceIn(0f, 1f), onValueChange = controller::setVolume, enabled = enabled, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = CubicColors.Text, activeTrackColor = CubicColors.Text, inactiveTrackColor = CubicColors.TextMuted.copy(alpha = 0.4f)), modifier = Modifier.width(92.dp))
    }
}

@Composable
internal fun CubicExpandedPlayerDialogV2(
    controller: PlayerController,
    currentSong: Song,
    isResolving: Boolean,
    playbackMessage: String?,
    queue: List<Song>,
    currentQueueIndex: Int,
    syncedLyrics: String?,
    plainLyrics: String?,
    lyricsTimestampMs: Long,
    lyricsLoading: Boolean,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    onToggleFavorite: () -> Unit,
    playlistNames: List<String>,
    onQueueSongClick: (Int) -> Unit,
    onAddToPlaylist: (Song, String) -> Unit,
    onDownload: (Song) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CubicPlayerTab.UpNext) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Row(
                Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.76f).clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(listOf(CubicColors.PanelRaised, CubicColors.Window))).clickable(enabled = false) {}.padding(25.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                Column(Modifier.weight(1.1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", color = CubicColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(16.dp))
                    CubicArtwork(currentSong.thumbnailUrl, Modifier.weight(1f).fillMaxWidth(), 24.dp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(currentSong.title, color = CubicColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(playerSubtitle(currentSong, isResolving, playbackMessage), color = playerSubtitleColor(playbackMessage), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DownloadButton(currentSong, downloadedIds, downloadProgress, onDownload)
                        IconButton(onClick = onToggleFavorite) { Icon(if (currentSong.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (currentSong.isLiked) CubicColors.Accent else CubicColors.TextSecondary) }
                        PlaylistAddButton(currentSong, playlistNames, onAddToPlaylist)
                    }
                    CubicPlaybackControlsV2(controller, !isResolving, currentQueueIndex > 0, currentQueueIndex in 0 until queue.lastIndex, onPrevious, onNext, Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(0.9f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = CubicColors.TextSecondary) } }
                    CubicPlayerTabs(selectedTab, onSelected = { selectedTab = it })
                    Spacer(Modifier.height(10.dp))
                    when (selectedTab) {
                        CubicPlayerTab.UpNext -> CubicQueue(queue, currentQueueIndex, downloadedIds, downloadProgress, onQueueSongClick, onDownload, Modifier.weight(1f))
                        CubicPlayerTab.Lyrics -> CubicSynchronizedLyricsPane(syncedLyrics, plainLyrics, lyricsTimestampMs, lyricsLoading, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun playerSubtitle(song: Song?, resolving: Boolean, message: String?): String = when {
    resolving -> "Preparing stream"
    !message.isNullOrBlank() && message != "Playing" -> message
    song != null -> song.artistsText.orEmpty()
    else -> "Choose music from Browse"
}

private fun playerSubtitleColor(message: String?): Color = when {
    message.isNullOrBlank() || message == "Playing" -> CubicColors.TextSecondary
    message.contains("fail", ignoreCase = true) ||
        message.contains("error", ignoreCase = true) ||
        message.contains("unavailable", ignoreCase = true) ||
        message.contains("timed out", ignoreCase = true) -> CubicColors.Danger
    message.startsWith("Added to ", ignoreCase = true) ||
        message.startsWith("Downloaded", ignoreCase = true) ||
        message == "Desktop data cleared" -> CubicColors.Success
    else -> CubicColors.TextSecondary
}

private fun String.cleanLyricsLines(): List<String> = lineSequence()
    .map { it.replace(Regex("^\\[[0-9:.]+]\\s*"), "").trim() }
    .filter(String::isNotBlank)
    .toList()

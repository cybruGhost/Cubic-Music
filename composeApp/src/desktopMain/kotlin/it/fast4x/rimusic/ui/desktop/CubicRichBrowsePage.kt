package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.utils.asSong
import database.entities.Song
import it.fast4x.innertube.Innertube

@Composable
internal fun CubicRichBrowsePage(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoodClick: (Innertube.Mood.Item) -> Unit
) {
    when {
        data == null && isLoading -> CubicLoadingState("Building your music feed")
        data == null && error != null -> CubicErrorState(error, onRetry)
        data == null -> CubicEmptyState("Nothing to show yet", "Refresh to load the live catalog.")
        else -> {
            val related = data.related?.songs.orEmpty().filter { it.key.isNotBlank() }
            val topSongs = data.charts?.songs.orEmpty().filter { it.key.isNotBlank() }
            val trending = data.charts?.trending.orEmpty().filter { it.key.isNotBlank() }
            val quickPicks = remember(data) {
                buildList {
                    val widest = maxOf(related.size, topSongs.size, trending.size)
                    repeat(widest) { index ->
                        trending.getOrNull(index)?.let(::add)
                        topSongs.getOrNull(index)?.let(::add)
                        related.getOrNull(index)?.let(::add)
                    }
                }.distinctBy { it.key }
            }
            val allSongs = (quickPicks + topSongs + trending + related).distinctBy { it.key }
            val longListens = allSongs.filter { it.durationText.cubicDurationSeconds() >= 300 }.take(12)
            val releases = data.discover?.newReleaseAlbums.orEmpty().distinctBy { it.key }
            val moods = data.discover?.moods.orEmpty().distinctBy { it.title }
            val chartPlaylists = data.charts?.playlists.orEmpty().distinctBy { it.key }
            val globalCharts = chartPlaylists.filter { playlist ->
                playlist.title.orEmpty().contains("global", true) || playlist.title.orEmpty().contains("50", true)
            }.ifEmpty { chartPlaylists }
            val videos = data.charts?.videos.orEmpty().filter { it.key.isNotBlank() }.distinctBy { it.key }
            val artists = data.charts?.artists.orEmpty().plus(data.related?.artists.orEmpty()).distinctBy { it.key }

            val listState = rememberLazyListState()
            val scrollScope = rememberCoroutineScope()
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).cubicKeyboardScroll(listState, scrollScope)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Browse", color = CubicColors.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Charts, new releases and music that keeps moving", color = CubicColors.TextSecondary, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (allSongs.isNotEmpty()) CubicTextAction("Play all", Icons.Rounded.PlayCircle) { onPlayAll(allSongs.map { it.asSong }) }
                            CubicTextAction("Refresh", Icons.Rounded.Refresh, onRetry)
                        }
                    }
                }

                if (quickPicks.isNotEmpty()) {
                    item {
                        CubicSectionTitle("Quick picks", "A mixed seed from charts, trending and your live radio")
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            quickPicks.take(8).chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { song -> RichQuickPick(song, Modifier.weight(1f)) { onSongClick(song.asSong) } }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (moods.isNotEmpty()) item {
                    Spacer(Modifier.height(24.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(moods.take(14), key = { it.title }) { mood ->
                            val stripe = Color(mood.stripeColor)
                            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(stripe.copy(alpha = .2f)).clickable { onMoodClick(mood) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(7.dp).background(stripe, CircleShape))
                                Text(mood.title, color = CubicColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (releases.isNotEmpty()) item {
                    RichSpacerTitle("New releases", "Albums landing now")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(releases.take(16), key = { it.key }) { album ->
                            CubicMediaCard(album.title.orEmpty(), album.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, album.thumbnail?.url, { onAlbumClick(album.key) })
                        }
                    }
                }

                if (topSongs.isNotEmpty() || chartPlaylists.isNotEmpty()) item {
                    RichSpacerTitle("Charts", "What listeners are playing right now")
                }

                if (topSongs.isNotEmpty()) {
                    item { CubicSectionTitle("Top songs", "Ranked from the live chart"); Spacer(Modifier.height(9.dp)) }
                    itemsIndexed(topSongs.take(10), key = { _, item -> "top-${item.key}" }) { index, song ->
                        RichRankedSong(index + 1, song, onSongClick)
                    }
                }

                if (trending.isNotEmpty()) item {
                    RichSpacerTitle("Trending", "Songs climbing fastest")
                    RichSongShelf(trending.take(14), onSongClick)
                }

                if (globalCharts.isNotEmpty()) item {
                    RichSpacerTitle("Global Top 50", "Open a complete chart")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(globalCharts.take(10), key = { it.key }) { playlist ->
                            CubicMediaCard(playlist.title.orEmpty(), playlist.channel?.name ?: playlist.songCount?.let { "$it songs" }.orEmpty(), playlist.thumbnail?.url, { onPlaylistClick(playlist.key) })
                        }
                    }
                }

                if (trending.isNotEmpty()) {
                    item { RichSpacerTitle("Trending now", "A ranked view of today's momentum") }
                    itemsIndexed(trending.take(10), key = { _, item -> "trend-${item.key}" }) { index, song ->
                        RichRankedSong(index + 1, song, onSongClick, Icons.Rounded.TrendingUp)
                    }
                }

                if (longListens.isNotEmpty()) item {
                    RichSpacerTitle("Long listens", "Five minutes and beyond")
                    RichSongShelf(longListens, onSongClick)
                }

                if (videos.isNotEmpty()) item {
                    RichSpacerTitle("Music videos", "Watch-worthy tracks from the live charts")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(videos.take(14), key = { it.key }) { video ->
                            Column(Modifier.width(190.dp).clip(RoundedCornerShape(16.dp)).clickable { onSongClick(video.cubicAsSong()) }.padding(bottom = 5.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CubicArtwork(video.thumbnail?.url, Modifier.width(190.dp).height(112.dp), 15.dp)
                                Text(video.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(video.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                if (artists.isNotEmpty()) item {
                    RichSpacerTitle("Artists in rotation", "Open a full artist page")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(artists.take(14), key = { it.key }) { artist -> CubicArtistCard(artist) { onArtistClick(artist.key) } }
                    }
                }
                item { Spacer(Modifier.height(42.dp)) }
            }
        }
    }
}

@Composable
private fun RichQuickPick(song: Innertube.SongItem, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.height(65.dp).clip(RoundedCornerShape(15.dp)).background(CubicColors.PanelRaised).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CubicArtwork(song.thumbnail?.url, Modifier.size(49.dp), 10.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RichRankedSong(index: Int, song: Innertube.SongItem, onSongClick: (Song) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable { onSongClick(song.asSong) }.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(25.dp), contentAlignment = Alignment.Center) {
            if (icon == null) Text(index.toString().padStart(2, '0'), color = CubicColors.TextMuted, fontSize = 10.sp)
            else Icon(icon, null, tint = CubicColors.Accent, modifier = Modifier.size(17.dp))
        }
        CubicArtwork(song.thumbnail?.url, Modifier.size(46.dp), 10.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp)
        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RichSongShelf(songs: List<Innertube.SongItem>, onSongClick: (Song) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        items(songs, key = { it.key }) { song ->
            Column(Modifier.width(154.dp).clip(RoundedCornerShape(16.dp)).clickable { onSongClick(song.asSong) }.padding(bottom = 5.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CubicArtwork(song.thumbnail?.url, Modifier.size(154.dp), 17.dp)
                Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RichSpacerTitle(title: String, subtitle: String) {
    Spacer(Modifier.height(29.dp))
    CubicSectionTitle(title, subtitle)
    Spacer(Modifier.height(12.dp))
}

private fun String?.cubicDurationSeconds(): Int {
    val parts = this?.split(':')?.mapNotNull(String::toIntOrNull) ?: return 0
    return parts.fold(0) { total, part -> total * 60 + part }
}

private fun Innertube.VideoItem.cubicAsSong() = Song(
    id = key,
    title = title.orEmpty(),
    artistsText = authors.orEmpty().joinToString(", ") { it.name.orEmpty() },
    durationText = durationText,
    thumbnailUrl = thumbnail?.url
)

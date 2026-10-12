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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Song
import it.fast4x.innertube.Innertube
import app.it.fast4x.rimusic.utils.asSong

@Composable
internal fun CubicBrowsePageV2(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoodClick: (Innertube.Mood.Item) -> Unit
) {
    when {
        data == null && isLoading -> CubicLoadingState("Finding music for you")
        data == null && error != null -> CubicErrorState(error, onRetry)
        data == null -> CubicEmptyState("Nothing to show yet", "Refresh to load the live catalog.")
        else -> {
            val songs = data.related?.songs.orEmpty().distinctBy { it.key }.filter { it.key.isNotBlank() }
            val albums = data.discover?.newReleaseAlbums.orEmpty().distinctBy { it.key }
            val artists = data.related?.artists.orEmpty().distinctBy { it.key }
            val playlists = data.related?.playlists.orEmpty().distinctBy { it.key }
            val moods = data.discover?.moods.orEmpty().distinctBy { it.title }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Made for you", color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Fresh picks from the live music catalog", color = CubicColors.TextSecondary, fontSize = 13.sp)
                        }
                        CubicTextAction("Refresh", Icons.Rounded.Refresh, onRetry)
                    }
                }

                if (songs.isNotEmpty()) {
                    item {
                        CubicSectionTitle("Quick picks", "Start listening")
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            songs.take(6).chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { song ->
                                        BrowseSongCard(song, Modifier.weight(1f)) { onSongClick(song.asSong) }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (moods.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(moods.take(12), key = { it.title }) { mood ->
                                val stripe = Color(mood.stripeColor)
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(stripe.copy(alpha = 0.22f)).clickable { onMoodClick(mood) }.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(Modifier.size(7.dp).background(stripe, CircleShape))
                                    Text(mood.title, color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(28.dp))
                        CubicSectionTitle("New releases", "Albums landing now")
                        Spacer(Modifier.height(13.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(albums.take(12), key = { it.key }) { album ->
                                CubicMediaCard(
                                    title = album.title.orEmpty(),
                                    subtitle = album.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty(),
                                    thumbnailUrl = album.thumbnail?.url,
                                    onClick = { onAlbumClick(album.key) }
                                )
                            }
                        }
                    }
                }

                if (artists.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(28.dp))
                        CubicSectionTitle("Artists to explore", "Based on what is moving now")
                        Spacer(Modifier.height(13.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            items(artists.take(12), key = { it.key }) { artist ->
                                CubicArtistCard(artist) { onArtistClick(artist.key) }
                            }
                        }
                    }
                }

                if (playlists.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(28.dp))
                        CubicSectionTitle("Playlists worth opening", "Real collections from the catalog")
                        Spacer(Modifier.height(13.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(playlists.take(12), key = { it.key }) { playlist ->
                                CubicMediaCard(
                                    title = playlist.title.orEmpty(),
                                    subtitle = playlist.channel?.name ?: playlist.songCount?.let { "$it songs" }.orEmpty(),
                                    thumbnailUrl = playlist.thumbnail?.url,
                                    onClick = { onPlaylistClick(playlist.key) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(34.dp)) }
            }
        }
    }
}

@Composable
private fun BrowseSongCard(song: Innertube.SongItem, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.height(66.dp).clip(RoundedCornerShape(15.dp)).background(CubicColors.PanelRaised).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        CubicArtwork(song.thumbnail?.url, Modifier.size(50.dp), 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty(), color = CubicColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(21.dp))
    }
}

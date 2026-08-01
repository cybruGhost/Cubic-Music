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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.artistPage
import app.it.fast4x.rimusic.utils.asSong

@Composable
internal fun CubicArtistDetailPage(
    browseId: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var artist by remember(browseId) { mutableStateOf<Innertube.ArtistInfoPage?>(null) }
    var error by remember(browseId) { mutableStateOf<String?>(null) }
    var retry by remember(browseId) { mutableIntStateOf(0) }

    LaunchedEffect(browseId, retry) {
        artist = null
        error = null
        if (browseId.isBlank()) {
            error = "No artist was selected."
            return@LaunchedEffect
        }
        val result = Innertube.artistPage(BrowseBody(browseId = browseId))
        result?.onSuccess { page ->
            if (page != null) artist = page else error = "The artist page was empty."
        }?.onFailure { error = it.message ?: "Could not load this artist." }
            ?: run { error = "Could not load this artist." }
    }

    val page = artist
    when {
        error != null -> CubicErrorState(error.orEmpty()) { retry++ }
        page == null -> CubicLoadingState("Loading artist")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    CubicArtwork(page.thumbnail?.url, Modifier.size(178.dp), 89.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ARTIST", color = CubicColors.Accent, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                        Text(page.name.orEmpty(), color = CubicColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        page.subscriberCountText?.let { Text(it, color = CubicColors.TextSecondary, fontSize = 12.sp) }
                        page.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = CubicColors.TextMuted, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            page.songs?.takeIf { it.isNotEmpty() }?.let { songs ->
                item {
                    CubicSectionTitle("Popular songs", "Tap a track to play")
                    Spacer(Modifier.height(10.dp))
                }
                items(songs.take(12), key = { "artist-song-${it.key}" }) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onSongClick(song.asSong) }.padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CubicArtwork(song.thumbnail?.url, Modifier.size(48.dp), 11.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty(), color = CubicColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp)
                        Box(Modifier.size(32.dp).background(CubicColors.AccentSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }

            page.albums?.takeIf { it.isNotEmpty() }?.let { albums ->
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Albums")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(albums, key = { it.key }) { album ->
                            CubicMediaCard(
                                title = album.title.orEmpty(),
                                subtitle = album.year.orEmpty(),
                                thumbnailUrl = album.thumbnail?.url,
                                onClick = { onAlbumClick(album.key) }
                            )
                        }
                    }
                }
            }

            page.singles?.takeIf { it.isNotEmpty() }?.let { singles ->
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Singles")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(singles, key = { it.key }) { single ->
                            CubicMediaCard(
                                title = single.title.orEmpty(),
                                subtitle = single.year.orEmpty(),
                                thumbnailUrl = single.thumbnail?.url,
                                onClick = { onAlbumClick(single.key) }
                            )
                        }
                    }
                }
            }

            page.playlists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Playlists")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(playlists, key = { it.key }) { playlist ->
                            CubicMediaCard(
                                title = playlist.title.orEmpty(),
                                subtitle = playlist.channel?.name.orEmpty(),
                                thumbnailUrl = playlist.thumbnail?.url,
                                onClick = { onPlaylistClick(playlist.key) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import app.it.fast4x.rimusic.utils.asSong
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private data class CubicSearchResults(
    val songs: List<Innertube.SongItem>,
    val albums: List<Innertube.AlbumItem>,
    val artists: List<Innertube.ArtistItem>,
    val playlists: List<Innertube.PlaylistItem>
) {
    val isEmpty get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

@Composable
internal fun CubicSearchPage(
    query: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var results by remember(query) { mutableStateOf<CubicSearchResults?>(null) }
    var error by remember(query) { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        results = null
        error = null
        runCatching {
            coroutineScope {
                val songs = async {
                    Innertube.searchPage<Innertube.SongItem>(
                        SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                        Innertube.SongItem::from
                    )?.getOrNull()?.items.orEmpty()
                }
                val albums = async {
                    Innertube.searchPage<Innertube.AlbumItem>(
                        SearchBody(query = query, params = Innertube.SearchFilter.Album.value),
                        Innertube.AlbumItem::from
                    )?.getOrNull()?.items.orEmpty()
                }
                val artists = async {
                    Innertube.searchPage<Innertube.ArtistItem>(
                        SearchBody(query = query, params = Innertube.SearchFilter.Artist.value),
                        Innertube.ArtistItem::from
                    )?.getOrNull()?.items.orEmpty()
                }
                val playlists = async {
                    Innertube.searchPage<Innertube.PlaylistItem>(
                        SearchBody(query = query, params = Innertube.SearchFilter.CommunityPlaylist.value),
                        Innertube.PlaylistItem::from
                    )?.getOrNull()?.items.orEmpty()
                }
                CubicSearchResults(
                    songs = songs.await().distinctBy { it.key }.filter { it.key.isNotBlank() },
                    albums = albums.await().distinctBy { it.key },
                    artists = artists.await().distinctBy { it.key },
                    playlists = playlists.await().distinctBy { it.key }
                )
            }
        }.onSuccess { results = it }
            .onFailure { error = it.message ?: "Search failed." }
    }

    val searchResults = results
    when {
        error != null -> CubicErrorState(error.orEmpty()) { results = null }
        searchResults == null -> CubicLoadingState("Searching the live catalog")
        searchResults.isEmpty -> CubicEmptyState("No results for “$query”", "Try another artist, album, song, or playlist.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.padding(top = 22.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Search", color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Results for “$query”", color = CubicColors.TextSecondary, fontSize = 12.sp)
                }
            }

            if (searchResults.songs.isNotEmpty()) {
                item { CubicSectionTitle("Songs", "Play directly from the live results") }
                items(searchResults.songs.take(20), key = { "song-${it.key}" }) { song ->
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
                        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (searchResults.albums.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Albums")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResults.albums.take(12), key = { it.key }) { album ->
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

            if (searchResults.artists.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Artists")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(searchResults.artists.take(12), key = { it.key }) { artist ->
                            CubicArtistCard(artist) { onArtistClick(artist.key) }
                        }
                    }
                }
            }

            if (searchResults.playlists.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    CubicSectionTitle("Playlists")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResults.playlists.take(12), key = { it.key }) { playlist ->
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
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

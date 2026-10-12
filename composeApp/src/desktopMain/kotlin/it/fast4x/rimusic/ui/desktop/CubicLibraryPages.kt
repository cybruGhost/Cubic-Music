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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Album
import database.entities.Song
import database.entities.SongEntity

internal enum class CubicSongCollection {
    All,
    Recent,
    Favorites,
    Cached
}

@Composable
internal fun CubicSongsPage(
    songs: List<SongEntity>,
    collection: CubicSongCollection,
    currentSongId: String?,
    onSongClick: (Song) -> Unit
) {
    val visibleSongs = when (collection) {
        CubicSongCollection.All -> songs.sortedBy { it.song.title.lowercase() }
        CubicSongCollection.Recent -> songs
        CubicSongCollection.Favorites -> songs.filter { it.song.isLiked }.sortedByDescending { it.song.likedAt }
        CubicSongCollection.Cached -> songs.sortedBy { it.song.title.lowercase() }
    }
    val (title, subtitle, emptyTitle, emptyMessage) = when (collection) {
        CubicSongCollection.All -> listOf("Songs", "Your desktop library", "Your library is empty", "Songs you play or save will appear here.")
        CubicSongCollection.Recent -> listOf("Recently played", "Your listening history", "No recent plays yet", "Play something from Browse to start your history.")
        CubicSongCollection.Favorites -> listOf("Favorite songs", "Tracks you marked with a heart", "No favorites yet", "Use the heart in Now Playing to save a track.")
        CubicSongCollection.Cached -> listOf("Cached songs", "Available without a connection", "No downloads yet", "Use the download button in Now Playing or Up next.")
    }

    if (visibleSongs.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            CubicPageHeader(title, subtitle, 0)
            Box(Modifier.weight(1f).fillMaxWidth()) { CubicEmptyState(emptyTitle, emptyMessage) }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        item { CubicPageHeader(title, subtitle, visibleSongs.size) }
        itemsIndexed(visibleSongs, key = { _, entity -> entity.song.id }) { index, entity ->
            CubicLibrarySongRow(
                index = index + 1,
                entity = entity,
                isPlaying = entity.song.id == currentSongId,
                onClick = { onSongClick(entity.song) }
            )
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun CubicLibrarySongRow(index: Int, entity: SongEntity, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) CubicColors.AccentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
            else Text(index.toString().padStart(2, '0'), color = CubicColors.TextMuted, fontSize = 10.sp)
        }
        CubicArtwork(entity.song.thumbnailUrl, Modifier.size(48.dp), 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entity.song.title, color = if (isPlaying) CubicColors.Accent else CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entity.song.artistsText.orEmpty(), color = CubicColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        entity.albumTitle?.let {
            Text(it, color = CubicColors.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.5f))
        }
        if (entity.song.isLiked) Icon(Icons.Rounded.Favorite, null, tint = CubicColors.Accent, modifier = Modifier.size(17.dp))
        Text(entity.song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun CubicAlbumsPage(albums: List<Album>, onAlbumClick: (String) -> Unit) {
    if (albums.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            CubicPageHeader("Albums", "Saved in your desktop library", 0)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CubicEmptyState("No saved albums", "Albums added to the desktop library will appear here.")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        CubicPageHeader("Albums", "Saved in your desktop library", albums.size)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums.sortedBy { it.title.orEmpty().lowercase() }, key = { it.id }) { album ->
                CubicMediaCard(
                    title = album.title.orEmpty(),
                    subtitle = listOfNotNull(album.authorsText, album.year).joinToString(" • "),
                    thumbnailUrl = album.thumbnailUrl,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun CubicArtistsPage(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val artists = data?.related?.artists.orEmpty().distinctBy { it.key }
    when {
        data == null && isLoading -> CubicLoadingState("Loading artists")
        artists.isEmpty() && error != null -> CubicErrorState(error, onRetry)
        artists.isEmpty() -> CubicEmptyState("No artists returned", "Refresh the live catalog to try again.")
        else -> Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            CubicPageHeader("Artists", "Voices to put in rotation", artists.size)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(artists, key = { it.key }) { artist ->
                    CubicArtistCard(artist) { onArtistClick(artist.key) }
                }
            }
        }
    }
}

@Composable
internal fun CubicPlaylistsPage(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val playlists = data?.related?.playlists.orEmpty().distinctBy { it.key }
    when {
        data == null && isLoading -> CubicLoadingState("Loading playlists")
        playlists.isEmpty() && error != null -> CubicErrorState(error, onRetry)
        playlists.isEmpty() -> CubicEmptyState("No playlists returned", "Refresh the live catalog to try again.")
        else -> Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            CubicPageHeader("Playlists", "Collections from the live catalog", playlists.size)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(158.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.key }) { playlist ->
                    CubicMediaCard(
                        title = playlist.title.orEmpty(),
                        subtitle = playlist.channel?.name ?: playlist.songCount?.let { "$it songs" }.orEmpty(),
                        thumbnailUrl = playlist.thumbnail?.url,
                        onClick = { onPlaylistClick(playlist.key) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CubicPageHeader(title: String, subtitle: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = CubicColors.TextSecondary, fontSize = 12.sp)
        }
        Text("$count ${if (count == 1) "item" else "items"}", color = CubicColors.TextMuted, fontSize = 11.sp)
    }
}

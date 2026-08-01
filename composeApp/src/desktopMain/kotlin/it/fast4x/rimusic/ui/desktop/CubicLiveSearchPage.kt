package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.utils.asSong
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CubicSearchKind(val label: String) { All("All"), Songs("Songs"), Videos("Videos") }

private data class CubicLiveSearchResults(
    val songs: List<Innertube.SongItem> = emptyList(),
    val videos: List<Innertube.VideoItem> = emptyList(),
    val albums: List<Innertube.AlbumItem> = emptyList(),
    val artists: List<Innertube.ArtistItem> = emptyList(),
    val playlists: List<Innertube.PlaylistItem> = emptyList()
) {
    val isEmpty get() = songs.isEmpty() && videos.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

@Composable
internal fun CubicLiveSearchPage(
    query: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var kind by remember { mutableStateOf(CubicSearchKind.All) }
    var results by remember(query, kind) { mutableStateOf(CubicLiveSearchResults()) }
    var error by remember(query, kind) { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableStateOf(0) }
    var requestGeneration by remember { mutableStateOf(0L) }
    var primaryLoading by remember { mutableStateOf(false) }
    val state = remember(query, kind) { LazyListState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query, kind, retryNonce) {
        requestGeneration += 1L
        val generation = requestGeneration
        state.scrollToItem(0)
        results = CubicLiveSearchResults()
        error = null
        if (query.trim().length < 2) {
            primaryLoading = false
            return@LaunchedEffect
        }
        delay(220)
        primaryLoading = true
        val requestQuery = query.trim()
        var completedPrimary = 0
        val primaryCount = if (kind == CubicSearchKind.All) 2 else 1

        fun publishPrimary(update: (CubicLiveSearchResults) -> CubicLiveSearchResults, hasItems: Boolean) {
            if (generation != requestGeneration) return
            results = update(results)
            completedPrimary++
            if (hasItems || completedPrimary >= primaryCount) primaryLoading = false
        }

        fun publishExtra(update: (CubicLiveSearchResults) -> CubicLiveSearchResults) {
            if (generation != requestGeneration) return
            results = update(results)
            if (!results.isEmpty) error = null
        }

        if (kind != CubicSearchKind.Videos) scope.launch {
            val items = Innertube.searchPage<Innertube.SongItem>(
                SearchBody(query = requestQuery, params = Innertube.SearchFilter.Song.value), Innertube.SongItem::from
            )?.getOrNull()?.items.orEmpty().filter { it.key.isNotBlank() }.distinctBy { it.key }
            publishPrimary({ it.copy(songs = items) }, items.isNotEmpty())
        }
        if (kind != CubicSearchKind.Songs) scope.launch {
            val items = Innertube.searchPage<Innertube.VideoItem>(
                SearchBody(query = requestQuery, params = Innertube.SearchFilter.Video.value), Innertube.VideoItem::from
            )?.getOrNull()?.items.orEmpty().filter { it.key.isNotBlank() }.distinctBy { it.key }
            publishPrimary({ it.copy(videos = items) }, items.isNotEmpty())
        }
        if (kind == CubicSearchKind.All) {
            scope.launch {
                val items = Innertube.searchPage<Innertube.AlbumItem>(
                    SearchBody(query = requestQuery, params = Innertube.SearchFilter.Album.value), Innertube.AlbumItem::from
                )?.getOrNull()?.items.orEmpty().distinctBy { it.key }
                publishExtra { it.copy(albums = items) }
            }
            scope.launch {
                val items = Innertube.searchPage<Innertube.ArtistItem>(
                    SearchBody(query = requestQuery, params = Innertube.SearchFilter.Artist.value), Innertube.ArtistItem::from
                )?.getOrNull()?.items.orEmpty().distinctBy { it.key }
                publishExtra { it.copy(artists = items) }
            }
            scope.launch {
                val items = Innertube.searchPage<Innertube.PlaylistItem>(
                    SearchBody(query = requestQuery, params = Innertube.SearchFilter.CommunityPlaylist.value), Innertube.PlaylistItem::from
                )?.getOrNull()?.items.orEmpty().distinctBy { it.key }
                publishExtra { it.copy(playlists = items) }
            }
        }
        scope.launch {
            delay(8_000L)
            if (generation == requestGeneration && primaryLoading) {
                primaryLoading = false
                if (results.isEmpty) error = "Search is taking too long. Retry or refine the title."
            }
        }
    }

    val found = results
    if (error != null && found.isEmpty) return CubicErrorState(error.orEmpty()) {
        error = null
        results = CubicLiveSearchResults()
        retryNonce++
    }

    val hoverSource = remember { MutableInteractionSource() }
    val pointerInside by hoverSource.collectIsHoveredAsState()
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (pointerInside || state.isScrollInProgress) 0.84f else 0f,
        label = "searchScrollbarAlpha"
    )
    val scrollbarStyle = LocalScrollbarStyle.current.copy(
        minimalHeight = 44.dp,
        thickness = 6.dp,
        shape = RoundedCornerShape(50),
        hoverDurationMillis = 140,
        unhoverColor = CubicColors.TextMuted.copy(alpha = 0.34f),
        hoverColor = CubicColors.Accent.copy(alpha = 0.76f)
    )

    Box(Modifier.fillMaxSize().hoverable(hoverSource)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).cubicKeyboardScroll(state, scope)
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (query.isBlank()) "Search" else "Results for \"$query\"", color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Suggestions refresh while you type", color = CubicColors.TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CubicSearchKind.entries.forEach { option ->
                            FilterChip(
                                selected = kind == option,
                                onClick = { kind = option },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CubicColors.AccentSoft,
                                    selectedLabelColor = CubicColors.Accent,
                                    labelColor = CubicColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }
            if (query.trim().length < 2) item { CubicEmptyState("Type to search", "Enter at least two characters for live suggestions.") }
            else if (primaryLoading && found.isEmpty) item {
                Text("Searching songs and videos...", color = CubicColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 22.dp))
            }
            else if (found.isEmpty) item { CubicEmptyState("No results for \"$query\"", "Try another title or artist.") }
            if (found.songs.isNotEmpty()) {
                item { CubicSectionTitle("Songs", "Play directly from live results"); Spacer(Modifier.height(8.dp)) }
                items(found.songs.take(24), key = { "search-song-${it.key}" }) { song -> CubicSearchSongRow(song.asSong, onSongClick) }
            }
            if (found.videos.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)); CubicSectionTitle("Videos", "Audio playback from music-video results"); Spacer(Modifier.height(8.dp)) }
                items(found.videos.take(20), key = { "search-video-${it.key}" }) { video -> CubicSearchSongRow(video.cubicSearchSong(), onSongClick) }
            }
            if (found.albums.isNotEmpty()) item {
                Spacer(Modifier.height(24.dp)); CubicSectionTitle("Albums"); Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(found.albums.take(12), key = { it.key }) { album ->
                        CubicMediaCard(album.title.orEmpty(), album.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, album.thumbnail?.url, { onAlbumClick(album.key) })
                    }
                }
            }
            if (found.artists.isNotEmpty()) item {
                Spacer(Modifier.height(24.dp)); CubicSectionTitle("Artists"); Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(found.artists.take(12), key = { it.key }) { artist -> CubicArtistCard(artist) { onArtistClick(artist.key) } }
                }
            }
            if (found.playlists.isNotEmpty()) item {
                Spacer(Modifier.height(24.dp)); CubicSectionTitle("Playlists"); Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(found.playlists.take(12), key = { it.key }) { playlist ->
                        CubicMediaCard(playlist.title.orEmpty(), playlist.channel?.name.orEmpty(), playlist.thumbnail?.url, { onPlaylistClick(playlist.key) })
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                .padding(top = 18.dp, bottom = 18.dp, end = 7.dp).width(7.dp).alpha(scrollbarAlpha),
            style = scrollbarStyle
        )
    }
}

@Composable
private fun CubicSearchSongRow(song: Song, onSongClick: (Song) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onSongClick(song) }.padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CubicArtwork(song.thumbnailUrl, Modifier.size(48.dp), 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(song.title, color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artistsText.orEmpty(), color = CubicColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp)
        Icon(Icons.Rounded.PlayArrow, "Play", tint = CubicColors.Accent, modifier = Modifier.size(20.dp))
    }
}

private fun Innertube.VideoItem.cubicSearchSong() = Song(
    id = key,
    title = title.orEmpty(),
    artistsText = authors.orEmpty().joinToString(", ") { it.name.orEmpty() },
    durationText = durationText,
    thumbnailUrl = thumbnail?.url
)

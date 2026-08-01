package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.utils.asSong
import database.entities.Song
import database.entities.SongEntity
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun CubicSongsDiscoveryPage(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    librarySongs: List<SongEntity>,
    onRetry: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayAll: (List<Song>) -> Unit
) {
    val madeForYou = (data?.related?.songs.orEmpty() + data?.charts?.trending.orEmpty() + data?.charts?.songs.orEmpty())
        .filter { it.key.isNotBlank() }.distinctBy { it.key }
    val artists = (data?.related?.artists.orEmpty() + data?.charts?.artists.orEmpty()).distinctBy { it.key }
    if (data == null && isLoading) return CubicLoadingState("Building your songs page")
    if (madeForYou.isEmpty() && librarySongs.isEmpty() && error != null) return CubicErrorState(error, onRetry)

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).cubicKeyboardScroll(listState, scope)
    ) {
        item {
            CubicCatalogHeader("Songs", "Fresh picks, artists and charts in one place") {
                if (madeForYou.isNotEmpty()) CubicTextAction("Play all", Icons.Rounded.PlayCircle) { onPlayAll(madeForYou.map { it.asSong }) }
            }
        }
        if (madeForYou.isNotEmpty()) item {
            CubicSectionTitle("Made for you", "A varied radio mix from the live catalog")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                items(madeForYou.take(14), key = { "made-${it.key}" }) { song ->
                    CubicSongCard(song) { onSongClick(song.asSong) }
                }
            }
        }
        if (artists.isNotEmpty()) item {
            Spacer(Modifier.height(26.dp))
            CubicSectionTitle("Artists for you", "Open an artist to play their songs")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(artists.take(8), key = { "for-${it.key}" }) { artist -> CubicArtistCard(artist) { onArtistClick(artist.key) } }
            }
        }
        if (artists.size > 4) item {
            Spacer(Modifier.height(26.dp))
            CubicSectionTitle("Popular artists", "More voices worth exploring")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(artists.drop(4).take(10), key = { "popular-${it.key}" }) { artist -> CubicArtistCard(artist) { onArtistClick(artist.key) } }
            }
        }
        if (madeForYou.isNotEmpty()) {
            item { Spacer(Modifier.height(28.dp)); CubicSectionTitle("Trending songs", "Tap any row to start playback"); Spacer(Modifier.height(8.dp)) }
            itemsIndexed(madeForYou.take(30), key = { _, song -> "rank-${song.key}" }) { index, song ->
                CubicCatalogSongRow(index + 1, song, onSongClick)
            }
        }
        if (librarySongs.isNotEmpty()) {
            item { Spacer(Modifier.height(28.dp)); CubicSectionTitle("Your library", "Songs saved on this desktop"); Spacer(Modifier.height(8.dp)) }
            itemsIndexed(librarySongs.take(30), key = { _, entity -> "local-${entity.song.id}" }) { index, entity ->
                CubicLocalSongRow(index + 1, entity.song) { onSongClick(entity.song) }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
internal fun CubicNewReleasesPage(
    data: CubicDiscoveryData?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val releases = data?.discover?.newReleaseAlbums.orEmpty().distinctBy { it.key }
    if (data == null && isLoading) return CubicLoadingState("Loading new releases")
    if (releases.isEmpty() && error != null) return CubicErrorState(error, onRetry)
    if (releases.isEmpty()) return CubicEmptyState("No new releases returned", "Refresh the catalog to try again.")

    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).cubicKeyboardScroll(state, scope)) {
        item { CubicCatalogHeader("New Release Albums", "Latest releases handpicked for you") { } }
        items(releases.chunked(4), key = { row -> row.joinToString { it.key } }) { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { album ->
                    CubicMediaCard(
                        title = album.title.orEmpty(),
                        subtitle = album.authors.orEmpty().joinToString(", ") { it.name.orEmpty() },
                        thumbnailUrl = album.thumbnail?.url,
                        onClick = { onAlbumClick(album.key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
internal fun CubicUserPlaylistsPage(
    data: CubicDiscoveryData?,
    playlists: List<String>,
    librarySongs: List<SongEntity>,
    onCreate: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var selectedPlaylist by remember(playlists) { mutableStateOf(playlists.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    val discover = data?.related?.playlists.orEmpty().distinctBy { it.key }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).cubicKeyboardScroll(state, scope)) {
        item {
            CubicCatalogHeader("Your Playlists", "${playlists.size} ${if (playlists.size == 1) "playlist" else "playlists"}") { }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("New Playlist Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CubicColors.Text, unfocusedTextColor = CubicColors.Text,
                        focusedBorderColor = CubicColors.Accent, unfocusedBorderColor = CubicColors.TextMuted,
                        focusedLabelColor = CubicColors.Accent, unfocusedLabelColor = CubicColors.TextMuted
                    )
                )
                Button(
                    onClick = {
                        val clean = name.trim().replace(Regex("\\s+"), " ")
                        if (clean.isNotEmpty() && playlists.none { it.equals(clean, true) }) {
                            onCreate(clean)
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CubicColors.Accent, contentColor = CubicColors.Background)
                ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Create", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (playlists.isEmpty()) item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CubicColors.PanelRaised).padding(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("You haven't created any playlists yet.", color = CubicColors.Text, fontWeight = FontWeight.Bold)
                    Text("Use the Create button above to start.", color = CubicColors.TextSecondary, fontSize = 12.sp)
                }
            }
        } else items(playlists, key = { "mine-$it" }) { playlist ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).clip(RoundedCornerShape(14.dp)).background(if (selectedPlaylist == playlist) CubicColors.AccentSoft else CubicColors.PanelRaised).clickable { selectedPlaylist = playlist }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(CubicColors.AccentSoft, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent) }
                Column(Modifier.padding(start = 12.dp)) { Text(playlist, color = CubicColors.Text, fontWeight = FontWeight.Bold); Text("${CubicPlaylistStore.songIds(playlist).size} songs", color = CubicColors.TextMuted, fontSize = 11.sp) }
            }
        }
        selectedPlaylist?.let { playlist ->
            val songs = CubicPlaylistStore.songIds(playlist).mapNotNull { id -> librarySongs.firstOrNull { it.song.id == id }?.song }
            item {
                Spacer(Modifier.height(24.dp))
                CubicSectionTitle(playlist, if (songs.isEmpty()) "No songs yet ? use + in Now Playing" else "${songs.size} saved songs")
                Spacer(Modifier.height(8.dp))
            }
            if (songs.isEmpty()) item {
                Text("Play a song, press +, and choose this playlist.", color = CubicColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
            } else itemsIndexed(songs, key = { _, song -> "playlist-song-${song.id}" }) { index, song ->
                CubicLocalSongRow(index + 1, song) { onSongClick(song) }
            }
        }
        if (discover.isNotEmpty()) {
            item { Spacer(Modifier.height(28.dp)); CubicSectionTitle("Discover playlists", "Live collections you can open"); Spacer(Modifier.height(12.dp)) }
            items(discover.chunked(4), key = { row -> row.joinToString { it.key } }) { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { playlist ->
                        CubicMediaCard(playlist.title.orEmpty(), playlist.channel?.name.orEmpty(), playlist.thumbnail?.url, { onPlaylistClick(playlist.key) }, Modifier.weight(1f))
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun CubicCatalogHeader(title: String, subtitle: String, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = CubicColors.TextSecondary, fontSize = 12.sp)
        }
        action()
    }
}

@Composable
private fun CubicSongCard(song: Innertube.SongItem, onClick: () -> Unit) {
    Column(Modifier.width(154.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CubicArtwork(song.thumbnail?.url, Modifier.size(154.dp), 17.dp)
        Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CubicCatalogSongRow(index: Int, song: Innertube.SongItem, onSongClick: (Song) -> Unit) =
    CubicLocalSongRow(index, song.asSong) { onSongClick(song.asSong) }

@Composable
private fun CubicLocalSongRow(index: Int, song: Song, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(index.toString().padStart(2, '0'), color = CubicColors.TextMuted, fontSize = 10.sp, modifier = Modifier.width(24.dp))
        CubicArtwork(song.thumbnailUrl, Modifier.size(45.dp), 10.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.title, color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artistsText.orEmpty(), color = CubicColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp)
        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(18.dp))
    }
}

internal fun Modifier.cubicKeyboardScroll(state: LazyListState, scope: CoroutineScope): Modifier =
    focusable().onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val action: (suspend () -> Unit)? = when (event.key) {
            Key.DirectionDown -> ({ state.animateScrollBy(170f) })
            Key.DirectionUp -> ({ state.animateScrollBy(-170f) })
            Key.PageDown -> ({ state.animateScrollBy(620f) })
            Key.PageUp -> ({ state.animateScrollBy(-620f) })
            Key.MoveHome -> ({ state.animateScrollToItem(0) })
            Key.MoveEnd -> ({ state.animateScrollToItem((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) })
            else -> null
        }
        action?.let { scope.launch { it() } }
        action != null
    }

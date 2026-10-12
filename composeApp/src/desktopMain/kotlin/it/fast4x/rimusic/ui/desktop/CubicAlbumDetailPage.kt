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
import androidx.compose.foundation.lazy.itemsIndexed
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
import app.it.fast4x.rimusic.utils.asSong
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.albumPage

@Composable
internal fun CubicAlbumDetailPage(browseId: String, onAlbumSongClick: (List<Song>, Int) -> Unit, onAlbumClick: (String) -> Unit) {
    var album by remember(browseId) { mutableStateOf<Innertube.PlaylistOrAlbumPage?>(null) }
    var error by remember(browseId) { mutableStateOf<String?>(null) }
    var retry by remember(browseId) { mutableIntStateOf(0) }

    LaunchedEffect(browseId, retry) {
        album = null
        error = null
        if (browseId.isBlank()) {
            error = "No album was selected."
            return@LaunchedEffect
        }
        Innertube.albumPage(BrowseBody(browseId = browseId))
            ?.onSuccess { page -> if (page != null) album = page else error = "The album page was empty." }
            ?.onFailure { error = it.message ?: "Could not load this album." }
            ?: run { error = "Could not load this album." }
    }

    val page = album
    when {
        error != null -> CubicErrorState(error.orEmpty()) { retry++ }
        page == null -> CubicLoadingState("Loading album")
        else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    CubicArtwork(page.thumbnail?.url, Modifier.size(180.dp), 22.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("ALBUM", color = CubicColors.Accent, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                        Text(page.title.orEmpty(), color = CubicColors.Text, fontSize = 31.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(page.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextSecondary, fontSize = 13.sp)
                        Text(listOfNotNull(page.year, page.otherInfo).joinToString(" • "), color = CubicColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }
            val songs = page.songsPage?.items.orEmpty()
            if (songs.isNotEmpty()) {
            val albumQueue = songs.map { it.asSong }
                item { CubicSectionTitle("Tracks", "${songs.size} songs"); Spacer(Modifier.height(10.dp)) }
                itemsIndexed(songs, key = { _, item -> "album-song-${item.key}" }) { index, song ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onAlbumSongClick(albumQueue, index) }.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CubicArtwork(song.thumbnail?.url ?: page.thumbnail?.url, Modifier.size(47.dp), 10.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.authors.orEmpty().joinToString(", ") { it.name.orEmpty() }, color = CubicColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(song.durationText.orEmpty(), color = CubicColors.TextMuted, fontSize = 10.sp)
                        Box(Modifier.size(31.dp).background(CubicColors.AccentSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(18.dp)) }
                    }
                }
            } else {
                item { CubicEmptyState("No tracks returned", "Try refreshing this album.") }
            }
            page.otherVersions.orEmpty().takeIf { it.isNotEmpty() }?.let { versions ->
                item {
                    Spacer(Modifier.height(25.dp)); CubicSectionTitle("Other versions"); Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(versions, key = { it.key }) { version ->
                            CubicMediaCard(version.title.orEmpty(), version.year.orEmpty(), version.thumbnail?.url, { onAlbumClick(version.key) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(34.dp)) }
        }
    }
}

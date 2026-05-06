package app.it.fast4x.rimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import database.DB
import database.entities.Song
import app.it.fast4x.rimusic.styling.Dimensions.layoutColumnBottomSpacer
import app.it.fast4x.rimusic.ui.components.Loader
import app.it.fast4x.rimusic.ui.components.Title
import app.it.fast4x.rimusic.utils.LoadImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    query: String,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var songs by remember(query) { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember(query) { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@LaunchedEffect
        isLoading = true
        songs = withContext(Dispatchers.IO) {
            DB.songsByTitleAsc().first()
                .map { it.song }
                .filter { song ->
                    song.title.contains(trimmedQuery, ignoreCase = true) ||
                        (song.artistsText?.contains(trimmedQuery, ignoreCase = true) == true)
                }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Title(title = "Search")
        Text(text = query, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Loader()
            return@Column
        }

        if (songs.isNotEmpty()) {
            Title(title = "Songs")
            songs.forEach { song ->
                SearchSongRow(
                    song = song,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSongClick(song) }
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (songs.isEmpty()) {
            Text("No results found in your desktop library yet.")
        }

        Spacer(Modifier.height(layoutColumnBottomSpacer))
    }
}

@Composable
private fun SearchSongRow(
    song: Song,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(52.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            song.thumbnailUrl?.let { thumbnailUrl ->
                LoadImage(thumbnailUrl)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall
            )
            song.artistsText?.takeIf { it.isNotBlank() }?.let { artists ->
                Text(
                    text = artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        song.durationText?.takeIf { it.isNotBlank() }?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

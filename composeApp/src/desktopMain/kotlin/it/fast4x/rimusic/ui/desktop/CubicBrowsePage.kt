package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.chartsPageComplete
import it.fast4x.innertube.requests.discoverPageNewAlbumsComplete
import it.fast4x.innertube.requests.relatedPage
import app.it.fast4x.rimusic.utils.asSong
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class CubicDiscoveryData(
    val related: Innertube.RelatedPage?,
    val discover: Innertube.DiscoverPage?,
    val charts: Innertube.ChartsPage?
)

internal object CubicDiscoveryRepository {
    private var cached: CubicDiscoveryData? = null

    suspend fun load(force: Boolean = false, seedVideoId: String = "HZnNt9nnEhw"): Result<CubicDiscoveryData> = runCatching {
        if (!force) cached?.let { return@runCatching it }
        coroutineScope {
            val related = async { Innertube.relatedPage(NextBody(videoId = seedVideoId))?.getOrNull() }
            val discover = async { Innertube.discoverPage().getOrNull() }
            val releaseFallback = async { Innertube.discoverPageNewAlbumsComplete().getOrNull()?.newReleaseAlbums.orEmpty() }
            val charts = async { Innertube.chartsPageComplete().getOrNull() }
            val relatedPage = related.await()
            val discoverPage = discover.await()
            val newReleases = (discoverPage?.newReleaseAlbums.orEmpty() + releaseFallback.await()).distinctBy { it.key }
            val mergedDiscover = when {
                discoverPage != null -> discoverPage.copy(newReleaseAlbums = newReleases)
                newReleases.isNotEmpty() -> Innertube.DiscoverPage(newReleaseAlbums = newReleases, moods = emptyList())
                else -> null
            }
            CubicDiscoveryData(relatedPage, mergedDiscover, charts.await()).also { result ->
                check(result.related != null || result.discover != null || result.charts != null) { "The music catalog did not return any sections." }
                cached = result
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CubicBrowsePage(
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

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Made for you", color = CubicColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Fresh picks from the live music catalog", color = CubicColors.TextSecondary, fontSize = 13.sp)
                    }
                    CubicTextAction("Refresh", Icons.Rounded.Refresh, onRetry)
                }

                if (songs.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    CubicSectionTitle("Quick picks", "Start listening")
                    Spacer(Modifier.height(13.dp))
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height(146.dp)
                    ) {
                        items(songs.take(14), key = { it.key }) { item ->
                            CubicSongTile(item, onClick = { onSongClick(item.asSong) })
                        }
                    }
                }

                if (moods.isNotEmpty()) {
                    Spacer(Modifier.height(26.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(moods.take(10), key = { it.title }) { mood ->
                            val stripe = Color(mood.stripeColor)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(stripe.copy(alpha = 0.22f))
                                    .clickable { onMoodClick(mood) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.size(7.dp).background(stripe, CircleShape))
                                Text(mood.title, color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    Spacer(Modifier.height(30.dp))
                    CubicSectionTitle("New releases", "Albums landing now")
                    Spacer(Modifier.height(14.dp))
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

                if (artists.isNotEmpty()) {
                    Spacer(Modifier.height(30.dp))
                    CubicSectionTitle("Artists to explore", "Based on what is moving now")
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(artists.take(12), key = { it.key }) { artist ->
                            CubicArtistCard(artist, onClick = { onArtistClick(artist.key) })
                        }
                    }
                }

                if (playlists.isNotEmpty()) {
                    Spacer(Modifier.height(30.dp))
                    CubicSectionTitle("Playlists worth opening", "Real collections from the catalog")
                    Spacer(Modifier.height(14.dp))
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
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun CubicSongTile(song: Innertube.SongItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(316.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(CubicColors.PanelRaised)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        CubicArtwork(song.thumbnail?.url, Modifier.size(52.dp), 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(song.title.orEmpty(), color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty(), color = CubicColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.PlayArrow, null, tint = CubicColors.Accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun CubicMediaCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(158.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CubicArtwork(thumbnailUrl, Modifier.size(158.dp), 18.dp)
        Text(title, color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = CubicColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CubicArtistCard(artist: Innertube.ArtistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(126.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(bottom = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        CubicArtwork(artist.thumbnail?.url, Modifier.size(118.dp), 59.dp)
        Text(artist.title.orEmpty(), color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        artist.subscribersCountText?.let {
            Text(it, color = CubicColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun CubicArtwork(url: String?, modifier: Modifier, radius: androidx.compose.ui.unit.Dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(CubicColors.Selection), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = CubicColors.TextMuted, modifier = Modifier.size(28.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url.cubicHighResolutionArtwork(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun CubicSectionTitle(title: String, subtitle: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = CubicColors.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = CubicColors.TextMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
internal fun CubicTextAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CubicColors.PanelRaised).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, null, tint = CubicColors.Accent, modifier = Modifier.size(17.dp))
        Text(label, color = CubicColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CubicLoadingState(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = CubicColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
            Text(label, color = CubicColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun CubicErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Could not load this page", color = CubicColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(message, color = CubicColors.TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            CubicTextAction("Try again", Icons.Rounded.Refresh, onRetry)
        }
    }
}

@Composable
internal fun CubicEmptyState(title: String, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(56.dp).background(CubicColors.PanelRaised, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, tint = CubicColors.Accent, modifier = Modifier.size(25.dp))
            }
            Text(title, color = CubicColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(message, color = CubicColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

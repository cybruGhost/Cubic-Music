package app.it.fast4x.rimusic.utils

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.typography
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import kotlinx.coroutines.delay

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun SearchYoutubeEntity(
    navController: NavController,
    onDismiss: () -> Unit,
    query: String,
    disableScrollingText: Boolean
) {
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val normalizedQuery = query.trim()
    val tabs = remember {
        listOf(
            YoutubeSearchTab(R.string.content_type_all, R.drawable.discover, YoutubeResultGroup.All),
            YoutubeSearchTab(R.string.songs, R.drawable.musical_notes, YoutubeResultGroup.Songs),
            YoutubeSearchTab(R.string.videos, R.drawable.video, YoutubeResultGroup.Videos),
            YoutubeSearchTab(R.string.artists, R.drawable.artist, YoutubeResultGroup.Artists),
            YoutubeSearchTab(R.string.playlists, R.drawable.playlist, YoutubeResultGroup.Playlists)
        )
    }
    var searchState by remember(normalizedQuery) { mutableStateOf<YoutubeSearchState>(YoutubeSearchState.Idle) }

    LaunchedEffect(normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            searchState = YoutubeSearchState.Idle
            return@LaunchedEffect
        }

        delay(300)
        searchState = YoutubeSearchState.Loading
        searchState = OmadaSearchClient.search(normalizedQuery)
            .fold(
                onSuccess = { YoutubeSearchState.Success(it) },
                onFailure = { YoutubeSearchState.Error }
            )
    }

    val filteredItems by remember(searchState, selectedTab) {
        derivedStateOf {
            val items = (searchState as? YoutubeSearchState.Success)?.items.orEmpty()
            items.filter { tabs[selectedTab].group.matches(it) }
        }
    }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .padding(WindowInsets.systemBars.asPaddingValues())
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                YoutubeSearchTabButton(
                    tab = tab,
                    selected = selectedTab == index,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = index }
                )
            }
        }

        when {
            normalizedQuery.isBlank() -> CenterText(stringResource(R.string.search_youtube_hint))
            searchState is YoutubeSearchState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorPalette().accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            searchState is YoutubeSearchState.Error -> CenterText(stringResource(R.string.searches_no_suggestions))
            filteredItems.isEmpty() -> CenterText(stringResource(R.string.no_results_found))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredItems,
                    key = { "${it.type}:${it.id}" }
                ) { item ->
                    YoutubeResultRow(
                        item = item,
                        navController = navController,
                        onDismiss = onDismiss,
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }
    }
}

private sealed interface YoutubeSearchState {
    data object Idle : YoutubeSearchState
    data object Loading : YoutubeSearchState
    data object Error : YoutubeSearchState
    data class Success(val items: List<OmadaSearchResult>) : YoutubeSearchState
}

private enum class YoutubeResultGroup {
    All,
    Songs,
    Videos,
    Artists,
    Playlists;

    fun matches(item: OmadaSearchResult): Boolean = when (this) {
        All -> true
        Songs -> item.type in setOf("song", "music", "track")
        Videos -> item.type == "video"
        Artists -> item.type.contains("artist") || item.type.contains("channel")
        Playlists -> item.type.contains("playlist")
    }
}

private data class YoutubeSearchTab(
    val titleRes: Int,
    val iconRes: Int,
    val group: YoutubeResultGroup
)

@Composable
private fun YoutubeSearchTabButton(
    tab: YoutubeSearchTab,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) colorPalette().accent.copy(alpha = 0.16f) else colorPalette().background1
    val foreground = if (selected) colorPalette().accent else colorPalette().textSecondary

    Column(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(tab.titleRes),
            style = typography().xxs,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CenterText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography().s,
            color = colorPalette().textSecondary
        )
    }
}

@Composable
private fun YoutubeResultRow(
    item: OmadaSearchResult,
    navController: NavController,
    onDismiss: () -> Unit,
    disableScrollingText: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val playable = item.type in setOf("video", "song", "music", "track")
    val thumbnailUrl = item.thumbnailUrl ?: if (playable) "https://i.ytimg.com/vi/${item.id}/hqdefault.jpg" else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    playable -> {
                        binder?.player?.forcePlay(item.asMediaItem(thumbnailUrl))
                        onDismiss()
                    }
                    item.type.contains("playlist") -> navController.navigate("${NavRoutes.playlist.name}/${item.id}")
                    item.type.contains("artist") ->
                        navController.navigate("${NavRoutes.artist.name}/${item.id}")
                    item.type.contains("channel") ->
                        navController.navigate("${NavRoutes.artist.name}/${item.id}")
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (thumbnailUrl != null) {
            ImageCacheFactory.Thumbnail(
                thumbnailUrl = thumbnailUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = if (playable) 64.dp else 46.dp, height = 46.dp)
                    .clip(thumbnailShape())
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(thumbnailShape())
                    .background(colorPalette().background1),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.logo_youtube),
                    contentDescription = null,
                    tint = colorPalette().textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                style = typography().s,
                color = colorPalette().text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.conditional(!disableScrollingText) { horizontalFadingEdge() }
            )
            val secondary = listOfNotNull(
                item.author.takeIf { it.isNotBlank() },
                item.description?.takeIf { item.type.contains("channel") && it.startsWith("@") },
                item.durationText,
                item.viewCountText
            ).joinToString(" • ")
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = typography().xxs,
                    color = colorPalette().textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun OmadaSearchResult.asMediaItem(thumbnailUrl: String?): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(author)
                .setArtworkUri(thumbnailUrl?.toUri())
                .build()
        )
        .build()

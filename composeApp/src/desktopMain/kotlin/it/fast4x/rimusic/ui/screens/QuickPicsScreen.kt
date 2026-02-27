package app.it.fast4x.rimusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerDefaults.windowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.relatedPage
import app.it.fast4x.rimusic.items.AlbumItem
import app.it.fast4x.rimusic.items.ArtistItem
import app.it.fast4x.rimusic.items.MoodItemColored
import app.it.fast4x.rimusic.items.PlaylistItem
import app.it.fast4x.rimusic.items.SongItem
import app.it.fast4x.rimusic.styling.Dimensions.albumThumbnailSize
import app.it.fast4x.rimusic.styling.Dimensions.artistThumbnailSize
import app.it.fast4x.rimusic.styling.Dimensions.itemInHorizontalGridWidth
import app.it.fast4x.rimusic.styling.Dimensions.itemsVerticalPadding
import app.it.fast4x.rimusic.styling.Dimensions.playlistThumbnailSize
import app.it.fast4x.rimusic.ui.components.Loader
import app.it.fast4x.rimusic.ui.components.Title
import app.it.fast4x.rimusic.ui.components.Title2Actions
import app.it.fast4x.rimusic.utils.asSong
import org.jetbrains.compose.resources.stringResource
import rimusic.composeapp.generated.resources.Res
import rimusic.composeapp.generated.resources.moods_and_genres
import rimusic.composeapp.generated.resources.new_albums
import rimusic.composeapp.generated.resources.play
import rimusic.composeapp.generated.resources.playlists_you_might_like
import rimusic.composeapp.generated.resources.related_albums
import rimusic.composeapp.generated.resources.similar_artists

//rewind
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import kotlin.random.Random
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicsScreen(
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoodClick: (Innertube.Mood.Item) -> Unit
) {

    Title2Actions(
        title = "For You",
        onClick1 = {},
        icon2 = Res.drawable.play,
        onClick2 = {}
    )

    val quickPicksLazyGridState = rememberLazyGridState()
    val moodAngGenresLazyGridState = rememberLazyGridState()
    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()
    val related = remember { mutableStateOf<Innertube.RelatedPage?>(null) }
    var relatedPageResult by remember { mutableStateOf<Result<Innertube.RelatedPage?>?>(null) }
    var discoverPageResult by remember { mutableStateOf<Result<Innertube.DiscoverPage?>?>(null) }
    var discover = remember { mutableStateOf<Innertube.DiscoverPage?>(null) }

    LaunchedEffect(Unit) {
        relatedPageResult = Innertube.relatedPage(
            NextBody(
                videoId = "HZnNt9nnEhw"
            )
        )

        discoverPageResult = Innertube.discoverPage()
    }
    relatedPageResult?.getOrNull().also { related.value = it }
    discoverPageResult?.getOrNull().also { discover.value = it }

    LazyHorizontalGrid(
        state = quickPicksLazyGridState,
        rows = GridCells.Fixed(if (related.value != null) 3 else 1),
        flingBehavior = ScrollableDefaults.flingBehavior(),
        contentPadding = endPaddingValues,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (related.value != null) itemsVerticalPadding * 3 * 9 else itemsVerticalPadding * 9)
    ) {
        related.value?.let{
            items(
                items = related.value!!.songs?.distinctBy { it.key }
                //?.dropLast(if (trending == null) 0 else 1)
                    ?: emptyList(),
                key = Innertube.SongItem::key
            ) { song ->

                SongItem(
                    song = song,
                    isDownloaded = false,
                    onDownloadClick = {},
                    //thumbnailSizeDp = 50.dp,
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {},
                            onClick = {
                                onSongClick(song.asSong)
                            }
                        )
                        .animateItemPlacement()
                        .width(itemInHorizontalGridWidth)
                )
            }
        } ?:
        item(span = { GridItemSpan(maxLineSpan) }){
            Loader()
        }

    }

    discover.let { page ->
        val showNewAlbums = true
        if (showNewAlbums) {
            Title(
                title = stringResource(Res.string.new_albums),
                onClick = {},
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyRow(contentPadding = endPaddingValues) {
                page.value?.newReleaseAlbums?.let {
                    items(items = it.distinctBy { it.key }, key = { it.key }) {
                        AlbumItem(
                            album = it,
                            thumbnailSizeDp = albumThumbnailSize,
                            alternative = true,
                            modifier = Modifier.clickable(onClick = {
                                onAlbumClick(it.key)
                            })
                        )
                    }
                }
            }
        }
    }

    related.value?.albums?.let { albums ->
        val showRelatedAlbums = true
        if (showRelatedAlbums) {
            Title(
                title = stringResource(Res.string.related_albums),
                onClick = {},
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(items = albums.distinctBy { it.key }, key = { it.key }) {
                    AlbumItem(
                        album = it,
                        thumbnailSizeDp = albumThumbnailSize,
                        alternative = true,
                        modifier = Modifier.clickable(onClick = {
                            onAlbumClick(it.key)
                        })
                    )
                }
            }
        }
    }

    related.value?.artists?.let { artists ->
        val showSimilarArtists = true
        if (showSimilarArtists) {
            Title(
                title = stringResource(Res.string.similar_artists),
                onClick = {},
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(items = artists.distinctBy { it.key }, key = { it.key }) {
                    ArtistItem(
                        artist = it,
                        thumbnailSizeDp = artistThumbnailSize,
                        alternative = true,
                        modifier = Modifier.clickable(onClick = {
                            onArtistClick(it.key)
                        })
                    )

                }
            }
        }
    }

    related.value?.playlists?.let { playlists ->
        val showPlaylistMightLike = true
        if (showPlaylistMightLike) {
            Title(
                title = stringResource(Res.string.playlists_you_might_like),
                onClick = {},
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(items = playlists.distinctBy { it.key }, key = { it.key }) {
                    PlaylistItem(
                        playlist = it,
                        thumbnailSizeDp = playlistThumbnailSize,
                        alternative = true,
                        showSongsCount = false,
                        modifier = Modifier.clickable(onClick = {
                            onPlaylistClick(it.key)
                        })
                    )

                }
            }
        }
    }

    discover.let { page ->
        val showNewAlbums = true
        if (showNewAlbums) {
            Title(
                title = stringResource(Res.string.moods_and_genres),
                onClick = {},
                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            LazyHorizontalGrid(
                state = moodAngGenresLazyGridState,
                rows = GridCells.Fixed(4),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                contentPadding = endPaddingValues,
                modifier = Modifier
                    //.fillMaxWidth()
                    .height(itemsVerticalPadding * 4 * 8)
            ) {
                page.value?.moods?.let {
                    items(
                        items = it.sortedBy { it.title },
                        key = { it.endpoint.params ?: it.title }
                    ) {
                        MoodItemColored(
                            mood = it,
                            onClick = {
                                it.endpoint.browseId?.let { _ -> onMoodClick(it) }
                            },
                            modifier = Modifier
                                //.width(itemWidth)
                                .padding(4.dp)
                        )
                    }
                }
            }

        }
    }

}
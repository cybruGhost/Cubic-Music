package it.fast4x.rimusic.ui.screens.searchresult

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import it.fast4x.rimusic.enums.ContentType
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.items.AlbumItemListPlaceholder
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.conditional
import me.knighthat.coil.ImageCacheFactory
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.playVideo
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.items.ArtistItemPlaceholder
import it.fast4x.rimusic.ui.items.SongItemPlaceholder
import it.fast4x.rimusic.ui.items.VideoItem
import it.fast4x.rimusic.ui.items.VideoItemPlaceholder
import me.knighthat.component.SongItem
import me.knighthat.component.menu.video.VideoItemMenu
import me.knighthat.utils.Toaster
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.showButtonPlayerVideoKey

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnlineSearchList(
    query: String,
    tabIndex: Int,
    filterContentType: ContentType,
    navController: NavController,
    disableScrollingText: Boolean,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    emptyItemsText: String
) {
    val thumbnailSizeDp = when (tabIndex) {
        1 -> Dimensions.thumbnails.album + 8.dp
        2 -> 64.dp
        else -> Dimensions.thumbnails.playlist + 8.dp
    }

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val isVideoEnabled = remember { context.preferences.getBoolean(showButtonPlayerVideoKey, false) }

    val itemContent: @Composable LazyItemScope.(Innertube.Item) -> Unit = { item ->
        when (item) {
            is Innertube.SongItem -> {
                val isDownloaded = isDownloadedSong(item.asMediaItem.mediaId)
                SwipeablePlaylistItem(
                    mediaItem = item.asMediaItem,
                    onPlayNext = { binder?.player?.addNext(item.asMediaItem) },
                    onDownload = {
                        binder?.cache?.removeResource(item.asMediaItem.mediaId ?: "")
                        Database.asyncTransaction {
                            Database.formatTable.updateContentLengthOf(item.key ?: "")
                        }
                        manageDownload(context, item.asMediaItem, isDownloaded)
                    },
                    onEnqueue = { binder?.player?.enqueue(item.asMediaItem) }
                ) {
                    SongItem(
                        song = item.asSong,
                        navController = navController,
                        modifier = Modifier,
                        onClick = {
                            binder?.startRadio(item.asMediaItem, false, item.info?.endpoint)
                        }
                    )
                }
            }
            is Innertube.VideoItem -> {
                SwipeablePlaylistItem(
                    mediaItem = item.asMediaItem,
                    onPlayNext = { binder?.player?.addNext(item.asMediaItem) },
                    onDownload = { Toaster.w(R.string.downloading_videos_not_supported) },
                    onEnqueue = { binder?.player?.enqueue(item.asMediaItem) }
                ) {
                    VideoItem(
                        video = item,
                        thumbnailWidthDp = 128.dp,
                        thumbnailHeightDp = 72.dp,
                        modifier = Modifier
                            .background(colorPalette().background0)
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        VideoItemMenu(
                                            navController = navController,
                                            song = item.asMediaItem.asSong ?: Song.makePlaceholder("")
                                        ).MenuComponent()
                                    }
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onClick = {
                                    binder?.stopRadio()
                                    if (isVideoEnabled) binder?.player?.playVideo(item.asMediaItem)
                                    else binder?.player?.forcePlay(item.asMediaItem)
                                }
                            ),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (tabIndex) {
                                1 -> navController.navigate("${NavRoutes.album.name}/${item.key}")
                                2 -> navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                4, 5 -> navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                6 -> navController.navigate("${NavRoutes.podcast.name}/${item.key}")
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        ImageCacheFactory.Thumbnail(
                            thumbnailUrl = when (item) {
                                is Innertube.AlbumItem -> item.thumbnail?.url
                                is Innertube.PlaylistItem -> item.thumbnail?.url
                                is Innertube.ArtistItem -> item.thumbnail?.url
                                else -> null
                            },
                            contentScale = if (tabIndex == 1 || tabIndex == 5 || tabIndex == 6) ContentScale.FillWidth else ContentScale.Crop,
                            modifier = Modifier
                                .size(thumbnailSizeDp - 8.dp)
                                .clip(thumbnailShape())
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        BasicText(
                            text = when (item) {
                                is Innertube.AlbumItem -> item.title ?: ""
                                is Innertube.PlaylistItem -> item.title ?: ""
                                is Innertube.ArtistItem -> item.title ?: ""
                                else -> ""
                            },
                            style = typography().s,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                        )
                        if (item is Innertube.AlbumItem) {
                            item.authors?.joinToString(", ") { it.name ?: "" }?.let { authors ->
                                if (authors.isNotBlank()) {
                                    BasicText(
                                        text = authors,
                                        style = typography().xs.copy(color = Color.Gray),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                    )
                                }
                            }
                            item.year?.let { year ->
                                if (year.isNotBlank()) {
                                    BasicText(
                                        text = year,
                                        style = typography().xs.copy(color = Color.Gray),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                    )
                                }
                            }
                            item.description?.split(" • ")?.forEach { segment ->
                                if (segment.isNotBlank()) {
                                    BasicText(
                                        text = segment,
                                        style = typography().xs.copy(color = Color.Gray),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                    )
                                }
                            }
                        }
                        if (item is Innertube.ArtistItem) {
                            item.description?.split(" • ")?.forEach { segment ->
                                if (segment.isNotBlank()) {
                                    BasicText(
                                        text = segment,
                                        style = typography().xs.copy(color = Color.Gray),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                    )
                                }
                            }
                        }
                        if (item is Innertube.PlaylistItem) {
                            val channelMetadata = item.channel?.name
                            if (channelMetadata?.isNotBlank() == true) {
                                BasicText(
                                    text = channelMetadata,
                                    style = typography().xs.copy(color = Color.Gray),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                )
                            }
                            if (tabIndex != 5) {
                                item.description?.split(" • ")?.forEach { segment ->
                                    if (segment.isNotBlank()) {
                                        BasicText(
                                            text = segment,
                                            style = typography().xs.copy(color = Color.Gray),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ItemsPage(
        filterContentType = filterContentType,
        tag = "searchResults/$query/${getTabName(tabIndex)}",
        itemsPageProvider = { continuation ->
            if (continuation == null) {
                Innertube.searchPage(
                    body = SearchBody(
                        query = query,
                        params = getSearchParams(tabIndex)
                    ),
                    fromMusicShelfRendererContent = getItemFrom(tabIndex)
                )
            } else {
                Innertube.searchPage(
                    body = ContinuationBody(continuation = continuation),
                    fromMusicShelfRendererContent = getItemFrom(tabIndex)
                )
            }
        },
        emptyItemsText = emptyItemsText,
        headerContent = headerContent,
        itemContent = { itemContent(it) },
        itemPlaceholderContent = {
            when (tabIndex) {
                0 -> Column { repeat(8) { SongItemPlaceholder() } }
                2 -> Column { repeat(8) { ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp) } }
                3 -> VideoItemPlaceholder(thumbnailWidthDp = 128.dp, thumbnailHeightDp = 72.dp)
                else -> AlbumItemListPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
            }
        }
    )
}

private fun getTabName(tabIndex: Int): String = when (tabIndex) {
    0 -> "songs"
    1 -> "albums"
    2 -> "artists"
    3 -> "videos"
    4 -> "playlists"
    5 -> "featured"
    6 -> "podcasts"
    else -> ""
}

private fun getSearchParams(tabIndex: Int): String = when (tabIndex) {
    0 -> Innertube.SearchFilter.Song.value
    1 -> Innertube.SearchFilter.Album.value
    2 -> Innertube.SearchFilter.Artist.value
    3 -> Innertube.SearchFilter.Video.value
    4 -> Innertube.SearchFilter.CommunityPlaylist.value
    5 -> Innertube.SearchFilter.FeaturedPlaylist.value
    6 -> Innertube.SearchFilter.Podcast.value
    else -> ""
}

private fun getItemFrom(tabIndex: Int): (it.fast4x.innertube.models.MusicShelfRenderer.Content) -> Innertube.Item? = when (tabIndex) {
    0 -> Innertube.SongItem.Companion::from
    1 -> Innertube.AlbumItem.Companion::from
    2 -> Innertube.ArtistItem.Companion::from
    3 -> Innertube.VideoItem.Companion::from
    else -> Innertube.PlaylistItem.Companion::from
}
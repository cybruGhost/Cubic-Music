package app.kreate.android.themed.rimusic.screen.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.Skeleton
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.Title
import it.fast4x.rimusic.ui.items.VideoItem
import it.fast4x.rimusic.ui.items.VideoItemPlaceholder
import it.fast4x.rimusic.ui.screens.searchresult.ItemsPage
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.playVideo
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.showButtonPlayerVideoKey

@UnstableApi
@ExperimentalTextApi
@ExperimentalAnimationApi
@Composable
fun ArtistVideos(
    navController: NavController,
    browseId: String,
    params: String?,
    miniPlayer: @Composable () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val isVideoEnabled = context.preferences.getBoolean(showButtonPlayerVideoKey, false)

    val thumbnailHeightDp = 72.dp
    val thumbnailWidthDp = 128.dp

    Skeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        navBarContent = {}
    ) {
        ItemsPage(
            tag = "artist/$browseId/videos",
            headerContent = {
                Title(
                    title = stringResource(R.string.videos),
                    verticalPadding = 4.dp,
                    modifier = Modifier.statusBarsPadding()
                )
            },
            itemContent = { item ->
                val video = item as? Innertube.VideoItem
                val song = item as? Innertube.SongItem

                if (video != null || song != null) {
                    val mediaItem = video?.asMediaItem ?: song?.asMediaItem!!
                    SwipeablePlaylistItem(
                        mediaItem = mediaItem,
                        onPlayNext = {
                            binder?.player?.addNext(mediaItem)
                        },
                        onDownload = {
                            // Downloading videos not supported
                        },
                        onEnqueue = {
                            binder?.player?.enqueue(mediaItem)
                        }
                    ) {
                        VideoItem(
                            thumbnailUrl = video?.thumbnail?.url ?: song?.thumbnail?.url,
                            duration = video?.durationText ?: song?.durationText,
                            title = item.title,
                            uploader = (video?.authors ?: song?.authors)?.joinToString(", ") { it.name ?: "" },
                            views = video?.viewsText,
                            thumbnailWidthDp = thumbnailWidthDp,
                            thumbnailHeightDp = thumbnailHeightDp,
                            modifier = Modifier
                                .background(colorPalette().background0)
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                navController = navController,
                                                mediaItem = mediaItem,
                                                onDismiss = menuState::hide,
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        if (isVideoEnabled)
                                            binder?.player?.playVideo(mediaItem)
                                        else
                                            binder?.player?.forcePlay(mediaItem)
                                    }
                                ),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            },
            itemPlaceholderContent = {
                VideoItemPlaceholder(
                    thumbnailHeightDp = thumbnailHeightDp,
                    thumbnailWidthDp = thumbnailWidthDp
                )
            },
            itemsPageProvider = { continuation ->
                if (continuation == null) {
                    YtMusic.getArtistItemsPage(BrowseEndpoint(browseId, params)).map {
                        Innertube.ItemsPage(it.items, it.continuation)
                    }
                } else {
                    YtMusic.getArtistItemsContinuation(continuation).map { continuationPage ->
                        Innertube.ItemsPage(
                            continuationPage?.items,
                            continuationPage?.continuation
                        )
                    }
                }
            },
            emptyItemsText = stringResource(R.string.no_results_found)
        )
    }
}
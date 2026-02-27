package app.it.fast4x.rimusic.ui.screens.searchresult

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.Skeleton
import app.it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import app.it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import app.it.fast4x.rimusic.ui.components.themed.Title
import app.it.fast4x.rimusic.ui.items.ArtistItem
import app.it.fast4x.rimusic.ui.items.ArtistItemPlaceholder
import app.it.fast4x.rimusic.ui.items.SongItemPlaceholder
import app.it.fast4x.rimusic.ui.items.VideoItem
import app.it.fast4x.rimusic.ui.items.VideoItemPlaceholder
import app.kreate.android.me.knighthat.component.menu.video.VideoItemMenu
import app.it.fast4x.rimusic.ui.styling.Dimensions

import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.parentalControlEnabledKey
import app.it.fast4x.rimusic.utils.playVideo
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.searchResultScreenTabIndexKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.kreate.android.me.knighthat.component.SongItem
import app.kreate.android.me.knighthat.utils.Toaster
import androidx.compose.foundation.text.BasicText
import app.it.fast4x.rimusic.typography
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.utils.conditional
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import app.it.fast4x.rimusic.enums.ContentType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import app.it.fast4x.rimusic.thumbnailShape
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import androidx.compose.ui.text.style.TextOverflow


fun String.toBooleanArray(): BooleanArray = this.map { it == '1' }.toBooleanArray()
fun BooleanArray.toPrefString(): String = joinToString(separator = "") { if (it) "1" else "0" }

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchResultScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    query: String,
    onSearchAgain: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabIndexChanges) = rememberPreference(searchResultScreenTabIndexKey, 0)

    val hapticFeedback = LocalHapticFeedback.current

    val isVideoEnabled = LocalContext.current.preferences.getBoolean(showButtonPlayerVideoKey, false)
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val (filterContentType, onFilterContentTypeChanged) = rememberPreference(app.it.fast4x.rimusic.utils.filterContentTypeKey, ContentType.All)
    val showFilterMenu = remember { mutableStateOf(false) }

    val (gridStatesPref, onGridStatesPrefChanged) = rememberPreference(app.it.fast4x.rimusic.utils.Preference.SEARCH_RESULT_GRID_STATES.key, app.it.fast4x.rimusic.utils.Preference.SEARCH_RESULT_GRID_STATES.default)
    val gridStates = remember(gridStatesPref) { gridStatesPref.toBooleanArray() }
    val useGrid = gridStates[tabIndex]
    val setUseGrid: (Boolean) -> Unit = { newValue ->
        val arr = gridStates.copyOf()
        arr[tabIndex] = newValue
        onGridStatesPrefChanged(arr.toPrefString())
    }

    val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
        Title(
            title = stringResource(R.string.search_results_for),
            verticalPadding = 4.dp
        )
        Title(
            title = query,
            icon = R.drawable.pencil,
            onClick = {
                navController.navigate("${NavRoutes.search.name}?text=${Uri.encode(query)}")
            },
            verticalPadding = 4.dp,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showFilterMenu.value = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.discover),
                                contentDescription = "Filter",
                                tint = colorPalette().text
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu.value,
                            modifier = Modifier.background(colorPalette().background0),
                            onDismissRequest = { showFilterMenu.value = false }
                        ) {
                            ContentType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.textName, color = colorPalette().text) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = type.icon),
                                            contentDescription = null,
                                            tint = colorPalette().text
                                        )
                                    },
                                    onClick = {
                                        onFilterContentTypeChanged(type)
                                        showFilterMenu.value = false
                                    }
                                )
                            }
                        }
                    }
                    if (tabIndex in listOf(1, 4, 5, 6)) {
                        IconButton(onClick = { setUseGrid(!useGrid) }) {
                            Icon(
                                painter = painterResource(id = if (useGrid) R.drawable.sort_vertical else R.drawable.sort_grid),
                                contentDescription = "Switch Mode",
                                tint = colorPalette().text
                            )
                        }
                    }
                }
            }
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = filterContentType.icon),
                contentDescription = null,
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicText(
                text = filterContentType.textName,
                style = typography().xs.copy(color = colorPalette().textSecondary)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    val emptyItemsText = stringResource(R.string.no_results_found)

    Skeleton(
        navController,
        tabIndex,
        onTabIndexChanges,
        miniPlayer,
        navBarContent = { item ->
            item(0, stringResource(R.string.songs), R.drawable.musical_notes)
            item(1, stringResource(R.string.albums), R.drawable.album)
            item(2, stringResource(R.string.artists), R.drawable.artist)
            item(3, stringResource(R.string.videos), R.drawable.video)
            item(4, stringResource(R.string.playlists), R.drawable.playlist)
            item(5, stringResource(R.string.featured), R.drawable.featured_playlist)
            item(6, stringResource(R.string.podcasts), R.drawable.podcast)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0, 2, 3 -> {
                    OnlineSearchList(
                        query = query,
                        tabIndex = currentTabIndex,
                        filterContentType = filterContentType,
                        navController = navController,
                        disableScrollingText = disableScrollingText,
                        headerContent = headerContent,
                        emptyItemsText = emptyItemsText
                    )
                }

                1, 4, 5, 6 -> {
                    if (useGrid) {
                        OnlineSearchGrid(
                            query = query,
                            tabIndex = currentTabIndex,
                            filterContentType = filterContentType,
                            navController = navController,
                            disableScrollingText = disableScrollingText,
                            headerContent = headerContent,
                            emptyItemsText = emptyItemsText
                        )
                    } else {
                        OnlineSearchList(
                            query = query,
                            tabIndex = currentTabIndex,
                            filterContentType = filterContentType,
                            navController = navController,
                            disableScrollingText = disableScrollingText,
                            headerContent = headerContent,
                            emptyItemsText = emptyItemsText
                        )
                    }
                }
            }
        }
    }
}
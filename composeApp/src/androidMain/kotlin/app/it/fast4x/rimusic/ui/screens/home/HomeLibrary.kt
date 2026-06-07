package app.it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.compose.persist.persistList
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.PINNED_PREFIX
import app.it.fast4x.rimusic.PIPED_PREFIX
import app.it.fast4x.rimusic.YTP_PREFIX
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.PlaylistsType
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.PlaylistPreview
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.ItemSize
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.CShareDialog
import app.it.fast4x.rimusic.ui.components.themed.CShareImportDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.CheckMonthlyPlaylist
import app.it.fast4x.rimusic.utils.Preference.HOME_LIBRARY_ITEM_SIZE
import app.it.fast4x.rimusic.utils.Preference.HOME_LIBRARY_SORT_BY
import app.it.fast4x.rimusic.utils.Preference.HOME_LIBRARY_SORT_ORDER
import app.it.fast4x.rimusic.utils.autoSyncToolbutton
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.enableCreateMonthlyPlaylistsKey
import app.it.fast4x.rimusic.utils.playlistTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showMonthlyPlaylistsKey
import app.it.fast4x.rimusic.utils.showPinnedPlaylistsKey
import app.it.fast4x.rimusic.utils.showPipedPlaylistsKey
import app.it.fast4x.rimusic.utils.syncSelectedYtmAccountData
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.component.Sort
import app.kreate.android.me.knighthat.component.playlist.NewPlaylistDialog
import app.kreate.android.me.knighthat.component.tab.CsvImportConversionHost
import app.kreate.android.me.knighthat.component.tab.ImportSongsFromCSV
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.component.tab.SongShuffler
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Randomizer
import app.kreate.android.me.knighthat.utils.Toaster

private sealed class LibraryChip {
    data class PlaylistFilter(val type: PlaylistsType) : LibraryChip()
    data object CShare : LibraryChip()
}

@ExperimentalMaterial3Api
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeLibrary(
    onPlaylistClick: (Playlist) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val lazyGridState = rememberLazyGridState()
    val syncScope = rememberCoroutineScope()

    suspend fun repairYtmScope() {
        YouTubeSessionStore.applyCurrentSession()
            ?.let { YtmSessionApi.ensureScopedSession(it) }
    }

    // Non-vital
    var playlistType by rememberPreference(playlistTypeKey, PlaylistsType.Playlist)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var items by persistList<PlaylistPreview>("home/playlists/items")

    var itemsOnDisplay by persistList<PlaylistPreview>("home/playlists/on_display")

    val search = Search(lazyGridState)

    val sort = Sort( HOME_LIBRARY_SORT_BY, HOME_LIBRARY_SORT_ORDER )
    val itemSize = ItemSize.init( HOME_LIBRARY_ITEM_SIZE )

    //<editor-fold desc="Songs shuffler">
    /**
     * Previous implementation calls this every time shuffle button is clicked.
     * It is extremely slow since the database needs some time to look for and
     * sort songs before it can go through and start playing.
     *
     * This implementation will make sure that new list is fetched when [PlaylistsType]
     * is changed, but this process happens in the background, therefore, there's no
     * visible penalty. Furthermore, this will reduce load time significantly.
     */
    val shuffle = SongShuffler(
        databaseCall = when( playlistType ) {
            PlaylistsType.Playlist          -> Database.playlistTable::allSongs
            PlaylistsType.PinnedPlaylist    -> Database.playlistTable::allPinnedSongs
            PlaylistsType.MonthlyPlaylist   -> Database.playlistTable::allMonthlySongs
            PlaylistsType.PipedPlaylist     -> Database.playlistTable::allPipedSongs
            PlaylistsType.YTPlaylist        -> Database.playlistTable::allYTPlaylistSongs
        },
        key = arrayOf( playlistType )
    )
    //</editor-fold>
    //<editor-fold desc="New playlist dialog">
    val newPlaylistDialog = NewPlaylistDialog()
    //</editor-fold>
    val importPlaylistDialog = ImportSongsFromCSV()
    var showCShareImport by remember { mutableStateOf(false) }
    if (showCShareImport) CShareImportDialog(onDismiss = { showCShareImport = false })
    var showCShareLibrary by remember { mutableStateOf(false) }

    LaunchedEffect( sort.sortBy, sort.sortOrder ) {
        Database.playlistTable
                .sortPreviews( sort.sortBy, sort.sortOrder )
                .distinctUntilChanged()
                .collect { items = it }
    }
    LaunchedEffect( items, search.inputValue ) {
        itemsOnDisplay = items.filter {
            it.playlist.name.contains( search.inputValue, true )
        }
    }

    // START: Additional playlists
    val showPinnedPlaylists by rememberPreference(showPinnedPlaylistsKey, true)
    val showMonthlyPlaylists by rememberPreference(showMonthlyPlaylistsKey, true)
    val showPipedPlaylists by rememberPreference(showPipedPlaylistsKey, true)

    val buttonsList = mutableListOf<Pair<LibraryChip, String>>(
        LibraryChip.PlaylistFilter(PlaylistsType.Playlist) to stringResource(R.string.playlists)
    )
    buttonsList += LibraryChip.PlaylistFilter(PlaylistsType.YTPlaylist) to stringResource(R.string.yt_playlists)
    buttonsList += LibraryChip.CShare to stringResource(R.string.cshare_title)
    if (showPipedPlaylists) buttonsList +=
        LibraryChip.PlaylistFilter(PlaylistsType.PipedPlaylist) to stringResource(R.string.piped_playlists)
    if (showPinnedPlaylists) buttonsList +=
        LibraryChip.PlaylistFilter(PlaylistsType.PinnedPlaylist) to stringResource(R.string.pinned_playlists)
    if (showMonthlyPlaylists) buttonsList +=
        LibraryChip.PlaylistFilter(PlaylistsType.MonthlyPlaylist) to stringResource(R.string.monthly_playlists)
    // END - Additional playlists

    LaunchedEffect(showPinnedPlaylists, showMonthlyPlaylists, showPipedPlaylists) {
        if (!showPinnedPlaylists && playlistType == PlaylistsType.PinnedPlaylist) playlistType = PlaylistsType.Playlist
        if (!showMonthlyPlaylists && playlistType == PlaylistsType.MonthlyPlaylist) playlistType = PlaylistsType.Playlist
        if (!showPipedPlaylists && playlistType == PlaylistsType.PipedPlaylist) playlistType = PlaylistsType.Playlist
    }

    LaunchedEffect(playlistType) {
        if (playlistType == PlaylistsType.YTPlaylist) {
            repairYtmScope()
            syncSelectedYtmAccountData()
        }
    }
    // START - New playlist
    newPlaylistDialog.Render()
    // END - New playlist

    // START - Monthly playlist
    val enableCreateMonthlyPlaylists by rememberPreference(enableCreateMonthlyPlaylistsKey, true)
    if (enableCreateMonthlyPlaylists)
        CheckMonthlyPlaylist()
    // END - Monthly playlist

    // FIX: Changed from Playlist to PlaylistPreview to match the actual type
    val randomizer = object: Randomizer<PlaylistPreview> {
        override fun getItems(): List<PlaylistPreview> = itemsOnDisplay
        override fun onClick(index: Int) = onPlaylistClick( itemsOnDisplay[index].playlist )
    }

    val sync = autoSyncToolbutton(R.string.sync) {
        syncScope.launch {
            repairYtmScope()
            val synced = syncSelectedYtmAccountData()
            if (!synced) Toaster.i("No new YouTube Music changes were synced")
        }
    }

    val listPrefix =
        when( playlistType ) {
            PlaylistsType.Playlist -> ""
            PlaylistsType.PinnedPlaylist -> PINNED_PREFIX
            PlaylistsType.MonthlyPlaylist -> MONTHLY_PREFIX
            PlaylistsType.PipedPlaylist -> PIPED_PREFIX
            PlaylistsType.YTPlaylist -> YTP_PREFIX
        }
    val condition: (PlaylistPreview) -> Boolean = {
        when (playlistType) {
            PlaylistsType.YTPlaylist -> it.playlist.isYoutubePlaylist
            PlaylistsType.Playlist -> {
                val isMonthly = it.playlist.name.startsWith(MONTHLY_PREFIX, true)
                val isPinned = it.playlist.name.startsWith(PINNED_PREFIX, true)
                val isPiped = it.playlist.name.startsWith(PIPED_PREFIX, true)

                (!isMonthly || showMonthlyPlaylists) &&
                    (!isPinned || showPinnedPlaylists) &&
                    (!isPiped || showPipedPlaylists)
            }
            else -> it.playlist.name.startsWith(listPrefix, true)
        }
    }
    val visiblePlaylists = itemsOnDisplay.filter(condition)
    if (showCShareLibrary) {
        CShareLibraryDialog(
            playlists = visiblePlaylists,
            onDismiss = { showCShareLibrary = false },
            onImportClick = {
                showCShareLibrary = false
                showCShareImport = true
            }
        )
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxSize()
        ) {
            Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
                TabHeader( R.string.playlists ) {
                    HeaderInfo( items.size.toString(), R.drawable.playlist )
                }

                // Sticky tab's tool bar
                TabToolBar.Buttons( sort, search, sync, shuffle, newPlaylistDialog, randomizer, importPlaylistDialog, itemSize )

                // Sticky search bar
                search.SearchBar( this )

                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive( itemSize.size.dp ),
                    modifier = Modifier
                        .background(colorPalette().background0)
                        .fillMaxSize(),
                    contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer )
                ) {
                    item(
                        key = "separator",
                        contentType = 0,
                        span = { GridItemSpan(maxLineSpan) }) {

                         Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Box {
                                ButtonsRow(
                                    chips = buttonsList,
                                    currentValue = LibraryChip.PlaylistFilter(playlistType),
                                    onValueUpdate = {
                                        when (it) {
                                            LibraryChip.CShare -> showCShareLibrary = true
                                            is LibraryChip.PlaylistFilter -> playlistType = it.type
                                        }
                                    },
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        }
                    }
                    

                    items(
                        items = visiblePlaylists,
                        key = { it.playlist.id }
                    ) { preview ->
                        PlaylistItem(
                            playlist = preview,
                            thumbnailSizeDp = itemSize.size.dp,
                            thumbnailSizePx = itemSize.size.px,
                            alternative = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .clickable(onClick = {
                                    search.hideIfEmpty()
                                    onPlaylistClick(preview.playlist)
                                }),
                            disableScrollingText = disableScrollingText,
                            isYoutubePlaylist = preview.playlist.isYoutubePlaylist,
                            isEditable = preview.playlist.isEditable
                        )
                    }

                }
            }

            FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

            val showFloaticon by rememberPreference(showFloatingIconKey, false)
            if (UiType.ViMusic.isCurrent() && showFloaticon)
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
        }

        CsvImportConversionHost()
    }
}

@Composable
private fun CShareLibraryDialog(
    playlists: List<PlaylistPreview>,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
) {
    var selectedPlaylist by remember { mutableStateOf<PlaylistPreview?>(null) }
    selectedPlaylist?.let {
        CShareDialog(
            playlistPreview = it,
            onDismiss = {
                selectedPlaylist = null
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette().background1.copy(alpha = 0.98f),
        titleContentColor = colorPalette().text,
        textContentColor = colorPalette().textSecondary,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.cshare_title)) },
        text = {
            Column {
                BasicText(
                    text = stringResource(R.string.cshare_library_description),
                    style = typography().xs.copy(color = colorPalette().textSecondary)
                )
                Surface(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onImportClick),
                    color = colorPalette().accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            tint = colorPalette().accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = stringResource(R.string.cshare_paste_code),
                            style = typography().xs.semiBold.copy(color = colorPalette().text)
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .heightIn(max = 300.dp)
                ) {
                    items(playlists, key = { it.playlist.id }) { preview ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPlaylist = preview },
                            color = colorPalette().background2.copy(alpha = 0.74f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.playlist),
                                    contentDescription = null,
                                    tint = colorPalette().accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = preview.playlist.name,
                                        style = typography().s.semiBold.copy(color = colorPalette().text)
                                    )
                                    BasicText(
                                        text = stringResource(R.string.cshare_song_count, preview.songCount),
                                        style = typography().xxs.copy(color = colorPalette().textSecondary)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.share_social),
                                    contentDescription = null,
                                    tint = colorPalette().textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

package app.kreate.android.themed.rimusic.screen.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.me.knighthat.component.dialog.RestartAppDialog
import app.kreate.android.me.knighthat.component.ResetCache
import app.kreate.android.me.knighthat.component.tab.ImportSongsFromCSV
import app.kreate.android.me.knighthat.component.tab.ItemSelector
import app.kreate.android.me.knighthat.component.tab.LikeComponent
import app.kreate.android.me.knighthat.component.tab.Locator
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.component.tab.SmartShuffle
import app.kreate.android.me.knighthat.component.tab.SongShuffler
import app.kreate.android.me.knighthat.utils.Toaster
import app.kreate.android.themed.rimusic.screen.home.onDevice.OnDeviceSong
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.BuiltInPlaylist
import app.it.fast4x.rimusic.enums.CacheType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.RecommendationsNumber
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.themed.CacheSpaceIndicator
import app.it.fast4x.rimusic.ui.components.themed.Enqueue
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.components.themed.PlayNext
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.builtInPlaylistKey
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.recommendationsNumberKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showMyTopPlaylistKey
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

private data class DownloadStorageTarget(
    val treeUri: Uri,
    val displayPath: String,
)

private data class DownloadStorageOperationState(
    val title: String,
    val message: String,
    val currentItem: String = "",
    val progress: Float? = null,
)

private data class ExportSummary(
    val exportedCount: Int,
    val skippedCount: Int,
)

@UnstableApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSongsScreen(navController: NavController) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val lazyListState = rememberLazyListState()
    val customDownloadUri by rememberPreference(MyDownloadHelper.CUSTOM_DOWNLOAD_URI_KEY, "")

    var builtInPlaylist by rememberPreference(builtInPlaylistKey, BuiltInPlaylist.Favorites)
    var isRecommendationEnabled by remember { mutableStateOf(false) }
    val recommendationsNumber by rememberPreference(recommendationsNumberKey, RecommendationsNumber.Adaptive)
    var recommendationCount by remember { mutableStateOf(0) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }

    val itemsOnDisplayState = remember { mutableStateListOf<Song>() }

    val itemSelector = ItemSelector<Song>()
    fun getSongs() = itemSelector.ifEmpty { itemsOnDisplayState }.toList()
    fun getMediaItems() = getSongs().map(Song::asMediaItem)

    val search = Search(lazyListState)
    val locator = Locator(lazyListState, ::getSongs)
    val import = ImportSongsFromCSV()
    val shuffle = SongShuffler(::getSongs)
    val smartShuffle = SmartShuffle(
        isRecommendationEnabled = { isRecommendationEnabled },
        isRecommendationsLoading = { isRecommendationsLoading },
        onToggleRecommendation = { isRecommendationEnabled = !isRecommendationEnabled }
    )
    val playNext = PlayNext {
        binder?.player?.addNext(getMediaItems(), appContext())
        itemSelector.isActive = false
    }
    val enqueue = Enqueue {
        binder?.player?.enqueue(getMediaItems(), appContext())
        itemSelector.isActive = false
    }
    val addToFavorite = LikeComponent(::getSongs)
    val addToPlaylist = PlaylistsMenu.init(
        navController = navController,
        mediaItems = { _ -> getMediaItems() },
        onFailure = { throwable, preview ->
            Timber.e("Failed to add songs to playlist ${preview.playlist.name} on HomeSongs")
            throwable.printStackTrace()
        },
        finalAction = {
            itemSelector.isActive = false
        }
    )
    val resetCache = ResetCache(::getSongs)

    val buttons = remember(builtInPlaylist) {
        itemSelector.isActive = false
        mutableStateListOf<Button>().apply {
            add(search)
            add(locator)
            add(shuffle)
            add(smartShuffle)
            add(itemSelector)
            add(playNext)
            add(enqueue)
            add(addToFavorite)
            add(addToPlaylist)
            add(import)
            if (builtInPlaylist != BuiltInPlaylist.OnDevice) {
                add(resetCache)
            }
        }
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent()) {
                    Dimensions.contentWidthRightBar
                } else {
                    1f
                }
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(R.string.songs) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderInfo(itemsOnDisplayState.size.toString(), R.drawable.musical_notes)
                    }
                    if (isRecommendationEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.smart_shuffle),
                                contentDescription = null,
                                tint = colorPalette().textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            if (isRecommendationsLoading) {
                                Spacer(modifier = Modifier.width(4.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = colorPalette().textSecondary
                                )
                            } else if (recommendationCount > 0) {
                                BasicText(
                                    text = recommendationCount.toString(),
                                    style = TextStyle(
                                        color = colorPalette().textSecondary,
                                        fontStyle = typography().xxxs.semiBold.fontStyle,
                                        fontWeight = typography().xxxs.semiBold.fontWeight,
                                        fontSize = typography().xxxs.semiBold.fontSize
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background0)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicText(
                        text = "Filter:",
                        style = typography().xs.semiBold.copy(color = colorPalette().textSecondary),
                        maxLines = 1
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        val showFavoritesPlaylist by rememberPreference(showFavoritesPlaylistKey, true)
                        val showCachedPlaylist by rememberPreference(showCachedPlaylistKey, true)
                        val showMyTopPlaylist by rememberPreference(showMyTopPlaylistKey, true)
                        val showDownloadedPlaylist by rememberPreference(showDownloadedPlaylistKey, true)
                        val showOnDeviceChip by rememberPreference(showOnDevicePlaylistKey, true)

                        val chips = remember(
                            showFavoritesPlaylist,
                            showCachedPlaylist,
                            showMyTopPlaylist,
                            showDownloadedPlaylist,
                            showOnDeviceChip
                        ) {
                            buildList {
                                add(BuiltInPlaylist.All)
                                if (showFavoritesPlaylist) add(BuiltInPlaylist.Favorites)
                                if (showCachedPlaylist) add(BuiltInPlaylist.Offline)
                                if (showDownloadedPlaylist) add(BuiltInPlaylist.Downloaded)
                                if (showMyTopPlaylist) add(BuiltInPlaylist.Top)
                                if (showOnDeviceChip) add(BuiltInPlaylist.OnDevice)
                            }
                        }

                        ButtonsRow(
                            chips = chips,
                            currentValue = builtInPlaylist,
                            onValueUpdate = { builtInPlaylist = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                when (builtInPlaylist) {
                    BuiltInPlaylist.Downloaded, BuiltInPlaylist.Offline -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        CacheSpaceIndicator(
                            cacheType = when (builtInPlaylist) {
                                BuiltInPlaylist.Downloaded -> CacheType.DownloadedSongs
                                BuiltInPlaylist.Offline -> CacheType.CachedSongs
                                else -> CacheType.CachedSongs
                            }
                        )
                        if (builtInPlaylist == BuiltInPlaylist.Downloaded && customDownloadUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DownloadStorageStatusCard(
                                context = context,
                                downloadedSongsCount = MyDownloadHelper.getDownloadedSongsCount()
                            )
                        }
                    }

                    else -> Unit
                }
            }

            TabToolBar.Buttons(buttons)
            search.SearchBar(this)

            when (builtInPlaylist) {
                BuiltInPlaylist.OnDevice -> OnDeviceSong(
                    navController,
                    lazyListState,
                    itemSelector,
                    search,
                    buttons,
                    itemsOnDisplayState,
                    ::getSongs
                )

                else -> HomeSongs(
                    navController = navController,
                    builtInPlaylist = builtInPlaylist,
                    lazyListState = lazyListState,
                    itemSelector = itemSelector,
                    search = search,
                    buttons = buttons,
                    itemsOnDisplay = itemsOnDisplayState,
                    getSongs = ::getSongs,
                    onRecommendationCountChange = { count -> recommendationCount = count },
                    onRecommendationsLoadingChange = { loading -> isRecommendationsLoading = loading },
                    isRecommendationEnabled = isRecommendationEnabled
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

        val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
        if (UiType.ViMusic.isCurrent() && showFloatingIcon) {
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = { search.onShortClick() },
                onClickSettings = { navController.navigate(NavRoutes.settings.name) },
                onClickSearch = {
                    if (!search.isVisible) {
                        search.onShortClick()
                    } else {
                        search.isFocused = true
                    }
                }
            )
        }

        if (builtInPlaylist == BuiltInPlaylist.Downloaded) {
            DownloadedPlaylistFloatingTool(
                context = context,
                binder = binder,
                songs = itemsOnDisplayState.toList()
            )
        }

        RestartAppDialog.Render()
    }
}

@UnstableApi
@Composable
private fun DownloadedPlaylistFloatingTool(
    context: Context,
    binder: PlayerServiceModern.Binder?,
    songs: List<Song>,
) {
    val coroutineScope = rememberCoroutineScope()
    val customDownloadUri by rememberPreference(MyDownloadHelper.CUSTOM_DOWNLOAD_URI_KEY, "")
    val customDownloadPath by rememberPreference(MyDownloadHelper.CUSTOM_DOWNLOAD_PATH_KEY, "")
    var hasCustomStorage by remember { mutableStateOf(MyDownloadHelper.hasCustomDownloadStorage(context)) }
    var customFolderLabel by remember { mutableStateOf(MyDownloadHelper.getCustomDownloadFolderLabel(context)) }
    var showActions by remember { mutableStateOf(false) }
    var showMigrateWarning by remember { mutableStateOf(false) }
    var pendingStorageTarget by remember { mutableStateOf<DownloadStorageTarget?>(null) }
    var showMoveExistingPrompt by remember { mutableStateOf(false) }
    var showDisableStoragePrompt by remember { mutableStateOf(false) }

    // ── KEY FIX: use MutableStateFlow so background-thread updates are
    //            delivered atomically to the Compose main-thread collector.
    //            Plain `mutableStateOf` assigned from a coroutine on IO can
    //            race with recomposition and cause the dialog to flash in/out.
    val operationStateFlow = remember { MutableStateFlow<DownloadStorageOperationState?>(null) }
    val operationState by operationStateFlow.asStateFlow().collectAsState()

    var offsetX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var offsetY by rememberSaveable { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(customDownloadUri, customDownloadPath) {
        hasCustomStorage = customDownloadUri.isNotBlank()
        customFolderLabel = customDownloadPath
    }

    // Helper: post a state update safely onto the main thread regardless of
    // which dispatcher the caller is on, so Compose always sees a consistent value.
    suspend fun postOperationState(state: DownloadStorageOperationState?) =
        withContext(Dispatchers.Main.immediate) {
            operationStateFlow.value = state
        }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        val downloadedSongs = songs.fastDistinctBy(Song::id)
        if (downloadedSongs.isEmpty()) {
            Toaster.i(R.string.downloaded_tools_no_songs)
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            postOperationState(
                DownloadStorageOperationState(
                    title = context.getString(R.string.downloaded_tools_export_title),
                    message = context.getString(R.string.downloaded_tools_export_preparing),
                    progress = 0f,
                )
            )

            val result = runCatching {
                exportDownloadedSongsToTree(
                    context = context,
                    binder = binder,
                    songs = downloadedSongs,
                    treeUri = uri
                ) { current, total, title ->
                    // Progress updates from IO thread — postOperationState
                    // switches to Main.immediate before writing the flow.
                    coroutineScope.launch {
                        postOperationState(
                            DownloadStorageOperationState(
                                title = context.getString(R.string.downloaded_tools_export_title),
                                message = context.getString(R.string.downloaded_tools_export_copying),
                                currentItem = title,
                                progress = if (total <= 0) null
                                            else current.toFloat() / total.toFloat(),
                            )
                        )
                    }
                }
            }

            // Clear the dialog only after the operation is fully done.
            postOperationState(null)

            result.onSuccess { summary ->
                when {
                    summary.exportedCount > 0 -> Toaster.s(R.string.downloaded_tools_export_completed)
                    summary.skippedCount > 0  -> Toaster.i(R.string.downloaded_tools_export_none_available)
                    else                      -> Toaster.i(R.string.downloaded_tools_no_songs)
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed exporting downloaded songs")
                Toaster.e(throwable.message ?: context.getString(R.string.downloaded_tools_export_failed))
            }
        }
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        pendingStorageTarget = DownloadStorageTarget(
            treeUri = uri,
            displayPath = resolveTreeUriToFile(context, uri)?.absolutePath ?: uri.toString()
        )
        showMoveExistingPrompt = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val density = LocalDensity.current
        val fabSizePx = with(density) { 64.dp.toPx() }
        val horizontalPaddingPx = with(density) { 16.dp.toPx() }
        val verticalPaddingPx = with(density) { 96.dp.toPx() }
        val maxX = (constraints.maxWidth - fabSizePx - horizontalPaddingPx).coerceAtLeast(horizontalPaddingPx)
        val maxY = (constraints.maxHeight - fabSizePx - verticalPaddingPx).coerceAtLeast(verticalPaddingPx)

        if (offsetX.isNaN()) offsetX = maxX
        if (offsetY.isNaN()) offsetY = maxY

        FloatingActionButton(
            onClick = { showActions = true },
            containerColor = colorPalette().background2,
            contentColor = colorPalette().text,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = offsetX.roundToInt(),
                        y = offsetY.roundToInt()
                    )
                }
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount.x).coerceIn(horizontalPaddingPx, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(verticalPaddingPx / 2f, maxY)
                        }
                    )
                }
        ) {
            Icon(
                painter = painterResource(R.drawable.downloaded),
                contentDescription = stringResource(R.string.downloaded_tools_title)
            )
        }
    }

    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            containerColor = colorPalette().background1,
            iconContentColor = colorPalette().accent,
            titleContentColor = colorPalette().text,
            textContentColor = colorPalette().textSecondary,
            title = {
                Text(
                    text = stringResource(R.string.downloaded_tools_title),
                    style = typography().m.semiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.downloaded_tools_subtitle),
                        style = typography().xs
                    )
                    if (hasCustomStorage && customFolderLabel.isNotBlank()) {
                        DownloadedSongsActionTile(
                            title = stringResource(R.string.downloaded_tools_active_title),
                            subtitle = stringResource(R.string.downloaded_tools_selected_folder, customFolderLabel),
                            onClick = {}
                        )
                    }
                    Text(
                        text = stringResource(R.string.downloaded_tools_default_recommendation),
                        style = typography().xxs,
                        color = colorPalette().textSecondary
                    )
                    DownloadedSongsActionTile(
                        title = stringResource(R.string.downloaded_tools_export_action),
                        subtitle = stringResource(R.string.downloaded_tools_export_action_subtitle)
                    ) {
                        showActions = false
                        showMigrateWarning = true
                    }
                    DownloadedSongsActionTile(
                        title = stringResource(
                            if (hasCustomStorage) R.string.downloaded_tools_change_storage_action
                            else R.string.downloaded_tools_set_storage_action
                        ),
                        subtitle = stringResource(
                            if (hasCustomStorage) R.string.downloaded_tools_change_storage_action_subtitle
                            else R.string.downloaded_tools_set_storage_action_subtitle
                        )
                    ) {
                        showActions = false
                        storageLauncher.launch(null)
                    }
                    if (hasCustomStorage) {
                        DownloadedSongsActionTile(
                            title = stringResource(R.string.downloaded_tools_disable_storage_action),
                            subtitle = stringResource(R.string.downloaded_tools_disable_storage_action_subtitle)
                        ) {
                            showActions = false
                            showDisableStoragePrompt = true
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActions = false }) {
                    Text(stringResource(R.string.close), color = colorPalette().text)
                }
            }
        )
    }

    if (showMigrateWarning) {
        AlertDialog(
            onDismissRequest = { showMigrateWarning = false },
            containerColor = colorPalette().background1,
            titleContentColor = colorPalette().text,
            textContentColor = colorPalette().textSecondary,
            title = {
                Text(
                    text = stringResource(R.string.downloaded_tools_export_warning_title),
                    style = typography().m.semiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.downloaded_tools_export_warning_message),
                    style = typography().xs
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMigrateWarning = false
                        exportLauncher.launch(null)
                    }
                ) {
                    Text(stringResource(R.string.downloaded_tools_pick_folder), color = colorPalette().accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMigrateWarning = false }) {
                    Text(stringResource(R.string.cancel), color = colorPalette().text)
                }
            }
        )
    }

    if (showDisableStoragePrompt) {
        AlertDialog(
            onDismissRequest = { showDisableStoragePrompt = false },
            containerColor = colorPalette().background1,
            titleContentColor = colorPalette().text,
            textContentColor = colorPalette().textSecondary,
            title = {
                Text(
                    text = stringResource(R.string.downloaded_tools_disable_storage_title),
                    style = typography().m.semiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.downloaded_tools_disable_storage_message),
                    style = typography().xs
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableStoragePrompt = false
                        coroutineScope.launch {
                            postOperationState(
                                DownloadStorageOperationState(
                                    title = context.getString(R.string.downloaded_tools_disable_storage_title),
                                    message = context.getString(R.string.downloaded_tools_disable_storage_progress)
                                )
                            )
                            withContext(Dispatchers.IO) {
                                MyDownloadHelper.clearCustomDownloadStorage(context)
                                MyDownloadHelper.reinitializeDownloadStorage(context)
                            }
                            postOperationState(null)
                            hasCustomStorage = false
                            customFolderLabel = ""
                            Toaster.s(R.string.downloaded_tools_disable_storage_done)
                            RestartAppDialog.showDialog()
                        }
                    }
                ) {
                    Text(stringResource(R.string.turn_off), color = colorPalette().accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableStoragePrompt = false }) {
                    Text(stringResource(R.string.cancel), color = colorPalette().text)
                }
            }
        )
    }

    if (showMoveExistingPrompt && pendingStorageTarget != null) {
        AlertDialog(
            onDismissRequest = {
                showMoveExistingPrompt = false
                pendingStorageTarget = null
            },
            containerColor = colorPalette().background1,
            titleContentColor = colorPalette().text,
            textContentColor = colorPalette().textSecondary,
            title = {
                Text(
                    text = stringResource(R.string.downloaded_tools_copy_existing_title),
                    style = typography().m.semiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            R.string.downloaded_tools_selected_folder,
                            pendingStorageTarget?.displayPath.orEmpty()
                        ),
                        style = typography().xxs,
                        color = colorPalette().text
                    )
                    Text(
                        text = stringResource(R.string.downloaded_tools_copy_existing_message),
                        style = typography().xs
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingStorageTarget ?: return@TextButton
                        showMoveExistingPrompt = false
                        pendingStorageTarget = null
                        coroutineScope.launch {
                            applyDownloadStorageSelection(
                                context = context,
                                target = target,
                                songs = songs.fastDistinctBy(Song::id),
                                moveExisting = true,
                                onProgress = { state -> postOperationState(state) }
                            )
                            postOperationState(null)
                            hasCustomStorage = MyDownloadHelper.hasCustomDownloadStorage(context)
                            customFolderLabel = MyDownloadHelper.getCustomDownloadFolderLabel(context)
                            RestartAppDialog.showDialog()
                        }
                    }
                ) {
                    Text(stringResource(R.string.downloaded_tools_copy_existing_confirm), color = colorPalette().accent)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val target = pendingStorageTarget ?: return@TextButton
                            showMoveExistingPrompt = false
                            pendingStorageTarget = null
                            coroutineScope.launch {
                                applyDownloadStorageSelection(
                                    context = context,
                                    target = target,
                                    songs = songs.fastDistinctBy(Song::id),
                                    moveExisting = false,
                                    onProgress = { state -> postOperationState(state) }
                                )
                                postOperationState(null)
                                hasCustomStorage = MyDownloadHelper.hasCustomDownloadStorage(context)
                                customFolderLabel = MyDownloadHelper.getCustomDownloadFolderLabel(context)
                                RestartAppDialog.showDialog()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.downloaded_tools_future_only), color = colorPalette().text)
                    }
                    TextButton(
                        onClick = {
                            showMoveExistingPrompt = false
                            pendingStorageTarget = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel), color = colorPalette().textSecondary)
                    }
                }
            }
        )
    }

    // Progress dialog — driven by the StateFlow so it never flickers.
    // onDismissRequest is intentionally empty: the operation is non-cancellable
    // once started, so tapping outside does nothing.
    val activeOperation = operationState
    if (activeOperation != null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colorPalette().background1,
            titleContentColor = colorPalette().text,
            textContentColor = colorPalette().textSecondary,
            title = {
                Text(
                    text = activeOperation.title,
                    style = typography().m.semiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = activeOperation.message, style = typography().xs)
                    if (activeOperation.progress != null) {
                        LinearProgressIndicator(
                            progress = { activeOperation.progress.coerceIn(0f, 1f) },
                            color = colorPalette().accent,
                            trackColor = colorPalette().background3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CircularProgressIndicator(color = colorPalette().accent)
                    }
                    if (activeOperation.currentItem.isNotBlank()) {
                        Text(
                            text = activeOperation.currentItem,
                            style = typography().xxs,
                            color = colorPalette().text
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun DownloadedSongsActionTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        tonalElevation = 4.dp,
        color = colorPalette().background2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = typography().xs.semiBold,
                color = colorPalette().text
            )
            Text(
                text = subtitle,
                style = typography().xxs,
                color = colorPalette().textSecondary
            )
        }
    }
}

@Composable
private fun DownloadStorageStatusCard(
    context: Context,
    downloadedSongsCount: Int,
) {
    var mirroredFileCount by remember { mutableStateOf(0) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val customFolderLabel = MyDownloadHelper.getCustomDownloadFolderLabel(context)

    LaunchedEffect(customFolderLabel, downloadedSongsCount) {
        mirroredFileCount = withContext(Dispatchers.IO) {
            MyDownloadHelper.getCustomDownloadFileCount(context)
        }
    }

    Surface(
        onClick = { isExpanded = !isExpanded },
        color = colorPalette().background1,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (isExpanded) R.string.downloaded_tools_status_title_expanded
                        else R.string.downloaded_tools_status_title_collapsed
                    ),
                    style = typography().xs.semiBold,
                    color = colorPalette().text
                )
                Text(
                    text = stringResource(
                        if (isExpanded) R.string.downloaded_tools_hide_details
                        else R.string.downloaded_tools_show_details
                    ),
                    style = typography().xxs,
                    color = colorPalette().accent
                )
            }
            Text(
                text = stringResource(R.string.downloaded_tools_status_summary, downloadedSongsCount, mirroredFileCount),
                style = typography().xxs,
                color = colorPalette().textSecondary
            )
            if (isExpanded) {
                Text(
                    text = stringResource(R.string.downloaded_tools_status_default_count, downloadedSongsCount),
                    style = typography().xxs,
                    color = colorPalette().textSecondary
                )
                Text(
                    text = stringResource(R.string.downloaded_tools_status_custom_count, mirroredFileCount),
                    style = typography().xxs,
                    color = colorPalette().textSecondary
                )
                if (customFolderLabel.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.downloaded_tools_selected_folder, customFolderLabel),
                        style = typography().xxs,
                        color = colorPalette().textSecondary
                    )
                }
            }
        }
    }
}

private suspend fun applyDownloadStorageSelection(
    context: Context,
    target: DownloadStorageTarget,
    songs: List<Song>,
    moveExisting: Boolean,
    onProgress: suspend (DownloadStorageOperationState) -> Unit,
) {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            if (moveExisting) {
                onProgress(
                    DownloadStorageOperationState(
                        title = context.getString(R.string.downloaded_tools_copy_existing_title),
                        message = context.getString(R.string.downloaded_tools_copy_existing_progress),
                        progress = 0f
                    )
                )
                exportDownloadedSongsToTree(
                    context = context,
                    binder = null,
                    songs = songs,
                    treeUri = target.treeUri
                ) { current, total, title ->
                    // onProgress already switches to Main.immediate via postOperationState
                    kotlinx.coroutines.runBlocking {
                        onProgress(
                            DownloadStorageOperationState(
                                title = context.getString(R.string.downloaded_tools_copy_existing_title),
                                message = context.getString(R.string.downloaded_tools_copy_existing_progress),
                                currentItem = title,
                                progress = if (total <= 0) null else current.toFloat() / total.toFloat()
                            )
                        )
                    }
                }
            }

            context.preferences.edit()
                .putString(MyDownloadHelper.CUSTOM_DOWNLOAD_URI_KEY, target.treeUri.toString())
                .putString(MyDownloadHelper.CUSTOM_DOWNLOAD_PATH_KEY, target.displayPath)
                .apply()

            MyDownloadHelper.reinitializeDownloadStorage(context)

            if (moveExisting) context.getString(R.string.downloaded_tools_storage_updated_with_copy)
            else context.getString(R.string.downloaded_tools_storage_updated_future_only)
        }
    }

    result.onSuccess { message -> Toaster.s(message) }
        .onFailure { throwable ->
            Timber.e(throwable, "Failed to update download storage")
            Toaster.e(throwable.message ?: context.getString(R.string.downloaded_tools_storage_update_failed))
        }
}

@UnstableApi
private suspend fun exportDownloadedSongsToTree(
    context: Context,
    binder: PlayerServiceModern.Binder?,
    songs: List<Song>,
    treeUri: Uri,
    onProgress: (current: Int, total: Int, title: String) -> Unit,
): ExportSummary = withContext(Dispatchers.IO) {
    val root = DocumentFile.fromTreeUri(context, treeUri)
        ?: throw IOException("Could not open the selected export folder")
    val downloadCache = binder?.downloadCache ?: MyDownloadHelper.getDownloadCache(context)
    val distinctSongs = songs.fastDistinctBy(Song::id)
    var exportedCount = 0
    var skippedCount = 0

    distinctSongs.forEachIndexed { index, song ->
        onProgress(index + 1, distinctSongs.size, song.title)

        val spans = downloadCache.getCachedSpans(song.id)
            .sortedBy(CacheSpan::position)
            .mapNotNull(CacheSpan::file)

        if (spans.isEmpty()) {
            skippedCount++
            return@forEachIndexed
        }

        val outputName = buildDownloadedExportFileName(song)
        root.findFile(outputName)?.delete()
        val outputFile = root.createFile("audio/mp4", outputName)
            ?: throw IOException("Could not create $outputName in the selected folder")

        context.contentResolver.openOutputStream(outputFile.uri, "w")?.use { outputStream ->
            spans.forEach { cacheFile ->
                cacheFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } ?: throw IOException("Could not open $outputName for writing")

        exportedCount++
    }

    ExportSummary(exportedCount = exportedCount, skippedCount = skippedCount)
}

private fun resolveTreeUriToFile(context: Context, treeUri: Uri): File? {
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        ?: return null
    val parts = treeDocumentId.split(":", limit = 2)
    val volume = parts.firstOrNull()?.lowercase().orEmpty()
    val relativePath = parts.getOrNull(1).orEmpty()

    return when (volume) {
        "primary" -> {
            val root = Environment.getExternalStorageDirectory()
            if (relativePath.isBlank()) root else root.resolve(relativePath)
        }
        "home" -> {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (relativePath.isBlank()) documentsDir else documentsDir.resolve(relativePath)
        }
        else -> {
            context.getExternalFilesDirs(null)
                .filterNotNull()
                .firstOrNull { file -> file.absolutePath.contains(volume, ignoreCase = true) }
                ?.let { scopedDir ->
                    val androidDataIndex = scopedDir.absolutePath.indexOf("/Android/", ignoreCase = true)
                    val storageRoot = if (androidDataIndex >= 0) {
                        File(scopedDir.absolutePath.substring(0, androidDataIndex))
                    } else {
                        scopedDir
                    }
                    if (relativePath.isBlank()) storageRoot else storageRoot.resolve(relativePath)
                }
        }
    }
}

private fun buildDownloadedExportFileName(song: Song): String {
    val rawName = buildString {
        append(song.title.ifBlank { song.id })
        val artists = song.cleanArtistsText()
        if (artists.isNotBlank()) {
            append(" - ")
            append(artists)
        }
    }
    val sanitized = rawName
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { song.id }
    return "$sanitized.m4a"
}
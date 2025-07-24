package app.kreate.android.themed.rimusic.screen.home.onDevice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.Search
import app.kreate.android.themed.rimusic.component.tab.Sort
import it.fast4x.rimusic.EXPLICIT_PREFIX
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.bold
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.isAtLeastAndroid13
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import me.knighthat.component.FolderItem
import me.knighthat.component.SongItem
import me.knighthat.utils.PathUtils
import me.knighthat.utils.Toaster
import me.knighthat.utils.getLocalSongs

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun OnDeviceSong(
    navController: NavController,
    lazyListState: LazyListState,
    itemSelector: ItemSelector<Song>,
    search: Search,
    buttons: MutableList<Button>,
    itemsOnDisplay: MutableList<Song>,
    getSongs: () -> List<Song>,
) {
    // Essentials
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    // Settings
    val parentalControlEnabled by Preferences.PARENTAL_CONTROL
    val showFolder4LocalSongs by Preferences.HOME_SONGS_ON_DEVICE_SHOW_FOLDERS

    var songsOnDevice by remember {
        mutableStateOf(emptyMap<Song, String>())
    }
    var currentPath by remember(songsOnDevice.values) {
        mutableStateOf(PathUtils.findCommonPath(songsOnDevice.values))
    }

    // Permission handling
    val permission = rememberSaveable {
        if (isAtLeastAndroid13) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isPermissionGranted = it }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isPermissionGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val odSort = remember {
        Sort(menuState, Preferences.HOME_ON_DEVICE_SONGS_SORT_BY, Preferences.HOME_SONGS_SORT_ORDER)
    }

    // Fixed coroutine scope handling
    LaunchedEffect(isPermissionGranted, odSort.sortBy, odSort.sortOrder) {
        if (!isPermissionGranted) return@LaunchedEffect

        try {
            val job = coroutineContext[Job] ?: return@LaunchedEffect
            
            context.getLocalSongs(odSort.sortBy, odSort.sortOrder)
                .distinctUntilChanged()
                .onEach { 
                    if (!job.isActive) return@onEach
                    lazyListState.scrollToItem(0, 0) 
                }
                .collect { songsMap ->
                    if (!job.isActive) return@collect
                    songsOnDevice = songsMap
                    if (songsMap.isEmpty()) {
                        Toaster.e("No songs found on device")
                    }
                }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Toaster.e("Error loading songs: ${e.message}")
            }
        }
    }

    // This effect doesn't need cancellation as it's synchronous
    LaunchedEffect(songsOnDevice, search.input, currentPath) {
        songsOnDevice.keys.filter { !parentalControlEnabled || !it.title.startsWith(EXPLICIT_PREFIX, true) }
            .filter {
                !showFolder4LocalSongs ||
                songsOnDevice[it]?.startsWith(currentPath) == true ||
                currentPath.isEmpty()
            }
            .filter {
                val containsTitle = search appearsIn it.cleanTitle()
                val containsArtist = search appearsIn it.cleanArtistsText()
                containsTitle || containsArtist
            }
            .let {
                itemsOnDisplay.clear()
                itemsOnDisplay.addAll(it)
            }
    }

    // This effect doesn't need cancellation as it's one-time
    LaunchedEffect(Unit) {
        buttons.add(0, odSort)

        if (!isPermissionGranted) {
            try {
                permissionLauncher.launch(permission)
            } catch (e: Exception) {
                e.message?.let { Toaster.e(it) }
            }
        }
    }

    // UI remains the same...
    if (!isPermissionGranted) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    tint = colorPalette().textDisabled,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(.4f)
                )

                BasicText(
                    text = stringResource(R.string.media_permission_required_please_grant),
                    style = typography().m.copy(color = colorPalette().textDisabled)
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    border = BorderStroke(2.dp, colorPalette().accent),
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color.Transparent),
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            settingsLauncher.launch(intent)
                        } catch (e: Exception) {
                            e.message?.let { Toaster.e(it) }
                        }
                    }
                ) {
                    BasicText(
                        text = stringResource(R.string.open_permission_settings),
                        style = typography().l.bold.copy(color = colorPalette().accent)
                    )
                }
            }
        }
        return
    }

    if (isPermissionGranted && songsOnDevice.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            BasicText(
                text = "No songs found on device",
                style = typography().m.copy(color = colorPalette().textDisabled)
            )
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = songsOnDevice.isNotEmpty(),
        contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
    ) {
        if (showFolder4LocalSongs && songsOnDevice.isNotEmpty()) {
            item("folder_paths") {
                PathUtils.AddressBar(
                    paths = songsOnDevice.values,
                    currentPath = currentPath,
                    onSpecificAddressClick = { path ->
                        currentPath = path
                    }
                )
            }

            val folders = songsOnDevice.values
                .map { path ->
                    val file = java.io.File(path)
                    file.parent ?: ""
                }
                .distinct()
                .filter { it.startsWith(currentPath) && it != currentPath }
                .sorted()

            items(folders) { folderPath ->
                val folderName = folderPath.substringAfterLast('/')
                FolderItem(
                    text = folderName,
                    onClick = {
                        currentPath = folderPath
                    }
                )
            }
        }

        itemsIndexed(
            items = itemsOnDisplay,
            key = { _, song -> song.id }
        ) { index, song ->
            val mediaItem = song.asMediaItem

            SwipeablePlaylistItem(
                mediaItem = mediaItem,
                onPlayNext = { binder?.player?.addNext(mediaItem) },
                onEnqueue = {
                    binder?.player?.enqueue(mediaItem)
                }
            ) {
                SongItem(
                    song = song,
                    itemSelector = itemSelector,
                    navController = navController,
                    modifier = Modifier.animateItem(),
                    onClick = {
                        search.hideIfEmpty()
                        val mediaItems = getSongs().fastMap(Song::asMediaItem)
                        binder?.player?.forcePlayAtIndex(mediaItems, index)
                    }
                )
            }
        }
    }
}
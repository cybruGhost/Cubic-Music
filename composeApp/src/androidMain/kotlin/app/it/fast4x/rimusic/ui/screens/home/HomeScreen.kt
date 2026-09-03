package app.it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.themed.rimusic.screen.home.HomeSongsScreen
import app.it.fast4x.compose.persist.PersistMapCleanup
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.enums.HomeScreenTabs
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.toUiMood
import app.it.fast4x.rimusic.ui.screens.home.apple.AppleHomeScreen
import app.it.fast4x.rimusic.ui.components.Skeleton
import app.it.fast4x.rimusic.ui.components.themed.Loader
import app.it.fast4x.rimusic.utils.enableQuickPicksPageKey
import app.it.fast4x.rimusic.utils.exportifyWebUrlKey
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.homeScreenTabIndexKey
import app.it.fast4x.rimusic.utils.indexNavigationTabKey
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.utils.Toaster
import kotlin.system.exitProcess
import android.net.ConnectivityManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebViewClient.*
import android.os.Environment
import java.io.File
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.content.Context

@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomeScreen(
    navController: NavController,
    onPlaylistUrl: (String) -> Unit,
    miniPlayer: @Composable () -> Unit = {},
    openTabFromShortcut: Int
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val preferences = LocalContext.current.preferences
    val enableQuickPicksPage by rememberPreference(enableQuickPicksPageKey, true)

//    PersistMapCleanup("home/")

    val openTabFromShortcut1 by remember { mutableIntStateOf(openTabFromShortcut) }

    var initialtabIndex =
        when (openTabFromShortcut1) {
            -1 -> when (preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.Default)) {
                HomeScreenTabs.Default -> HomeScreenTabs.QuickPics.index
                else -> preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.QuickPics).index
            }
            else -> openTabFromShortcut1
        }

    var (tabIndex, onTabChanged) = rememberPreference(homeScreenTabIndexKey, initialtabIndex)

    // Check if services are ready
    val binder = LocalPlayerServiceBinder.current
    var isReady by remember { mutableStateOf(false) }
    
    LaunchedEffect(binder) {
        delay(100) // Small delay to ensure services are initialized
        isReady = true
    }

    if (tabIndex == -2) navController.navigate(NavRoutes.search.name)

    if (!enableQuickPicksPage && tabIndex == 0) tabIndex = 1

    // Show loader while services are not ready
    if (!isReady) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Loader()
        }
        return
    }

    Skeleton(
        navController,
        tabIndex,
        onTabChanged,
        miniPlayer,
        navBarContent = { Item ->
            if (enableQuickPicksPage)
                Item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
            Item(1, stringResource(R.string.songs), R.drawable.musical_notes)
            Item(2, stringResource(R.string.artists), R.drawable.music_artist)
            Item(3, stringResource(R.string.albums), R.drawable.album)
            Item(4, stringResource(R.string.playlists), R.drawable.library)
            Item(5, stringResource(R.string.exportify), R.drawable.export_icon)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> {
                    val onAlbum: (String) -> Unit = { id ->
                        navController.navigate(route = "${NavRoutes.album.name}/$id")
                    }
                    val onArtist: (String) -> Unit = { rawId ->
                        rawId.trim().takeIf(String::isNotBlank)?.let { artistId ->
                            navController.navigate(route = "${NavRoutes.artist.name}/${Uri.encode(artistId)}")
                        }
                    }
                    val onPlaylist: (String) -> Unit = { id ->
                        navController.navigate(route = "${NavRoutes.playlist.name}/$id")
                    }

                    if (UiType.Apple.isCurrent()) {
                        AppleHomeScreen(
                            navController = navController,
                            onAlbumClick = onAlbum,
                            onArtistClick = onArtist,
                            onPlaylistClick = onPlaylist,
                        )
                    } else {
                        HomeQuickPicks(
                            onAlbumClick = onAlbum,
                            onArtistClick = onArtist,
                            onPlaylistClick = onPlaylist,
                            onSearchClick = {
                                navController.navigate(NavRoutes.search.name)
                            },
                            onMoodClick = { mood ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("mood", mood.toUiMood())
                                navController.navigate(NavRoutes.mood.name)
                            },
                            onSettingsClick = {
                                navController.navigate(NavRoutes.settings.name)
                            },
                            navController = navController
                        )
                    }
                }

                1 -> HomeSongsScreen(navController)

                2 -> HomeArtists(
                    onArtistClick = {
                        val artistId = it.id.trim()
                        if (artistId.isNotBlank()) {
                            navController.navigate(route = "${NavRoutes.artist.name}/${Uri.encode(artistId)}")
                        }
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                3 -> HomeAlbums(
                    navController = navController,
                    onAlbumClick = {
                        navController.navigate(route = "${NavRoutes.album.name}/${it.id}")
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                4 -> HomeLibrary(
                    onPlaylistClick = {
                        navController.navigate(route = "${NavRoutes.localPlaylist.name}/${it.id}")
                    },
                    onSearchClick = {
                        navController.navigate(NavRoutes.search.name)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.settings.name)
                    }
                )
                
                5 -> ExportifyScreen()
            }
        }
    }

    // Exit app when user uses back
    val context = LocalContext.current
    var confirmCount by remember { mutableIntStateOf(0) }
    BackHandler {
        if (NavRoutes.home.isNotHere(navController)) {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                navController.popBackStack()
            return@BackHandler
        }

        if (confirmCount == 0) {
            Toaster.i(R.string.press_once_again_to_exit)
            confirmCount++
            CoroutineScope(Dispatchers.Default).launch {
                delay(5000L)
                confirmCount = 0
            }
        } else {
            val activity = context as? Activity
            activity?.finishAffinity()
            exitProcess(0)
        }
    }
}

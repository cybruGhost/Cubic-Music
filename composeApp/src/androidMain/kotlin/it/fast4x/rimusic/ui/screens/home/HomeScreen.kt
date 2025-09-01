package it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.themed.rimusic.screen.home.HomeSongsScreen
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.rimusic.enums.HomeScreenTabs
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.toUiMood
import it.fast4x.rimusic.ui.components.Skeleton
import it.fast4x.rimusic.ui.components.themed.Loader
import it.fast4x.rimusic.utils.enableQuickPicksPageKey
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.homeScreenTabIndexKey
import it.fast4x.rimusic.utils.indexNavigationTabKey
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import kotlin.system.exitProcess
import it.fast4x.rimusic.LocalPlayerServiceBinder

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

    PersistMapCleanup("home/")

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
            Item(2, stringResource(R.string.artists), R.drawable.people)
            Item(3, stringResource(R.string.albums), R.drawable.album)
            Item(4, stringResource(R.string.playlists), R.drawable.library)
            Item(5, "Exportify", R.drawable.export_icon)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> HomeQuickPicks(
                    onAlbumClick = {
                        navController.navigate(route = "${NavRoutes.album.name}/$it")
                    },
                    onArtistClick = {
                        navController.navigate(route = "${NavRoutes.artist.name}/$it")
                    },
                    onPlaylistClick = {
                        navController.navigate(route = "${NavRoutes.playlist.name}/$it")
                    },
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

                1 -> HomeSongsScreen(navController)

                2 -> HomeArtists(
                    onArtistClick = {
                        navController.navigate(route = "${NavRoutes.artist.name}/${it.id}")
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
                
                5 -> ExportifyWebViewScreen()
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExportifyWebViewScreen() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    
                    // Enable JavaScript
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportMultipleWindows(true)
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    
                    // Enable cookies
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }
                    
                    webChromeClient = WebChromeClient()
                    
                    // Load the Exportify website
                    loadUrl("https://exportify.app/")
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Update the WebView if needed
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                Loader()
            }
        }
    }

    // Handle back navigation for WebView
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
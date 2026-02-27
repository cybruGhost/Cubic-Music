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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import app.it.fast4x.rimusic.models.toUiMood
import app.it.fast4x.rimusic.ui.components.Skeleton
import app.it.fast4x.rimusic.ui.components.themed.Loader
import app.it.fast4x.rimusic.utils.enableQuickPicksPageKey
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

class ExportifyWebInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun onDownloadRequested(url: String) {
        // Handle download requests from JavaScript
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ExportifyWebViewScreen() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var hasError by remember { mutableStateOf(false) }
    var isConnectedToInternet by remember { mutableStateOf(true) } // Assume connected initially
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }

    // Function to check internet connectivity
    fun checkInternetConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    // Update connection status on first composition
    LaunchedEffect(Unit) {
        isConnectedToInternet = checkInternetConnection()
    }

    // Handle download dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download CSV") },
            text = { Text("Do you want to download the Spotify CSV file?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        // Open download link in browser for proper download handling
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDownloadDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Only show WebView if connected to internet
        if (isConnectedToInternet) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webView = this
                        
                        // Enhanced WebView settings to mimic a legitimate browser
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportMultipleWindows(true)
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            javaScriptCanOpenWindowsAutomatically = true
                            allowContentAccess = true
                            allowFileAccess = true
                            setSupportZoom(true)
                            
                            // Set realistic desktop user agent to avoid detection
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        }
                        
                        // Enhanced cookie handling for persistent login
                        CookieManager.getInstance().setAcceptCookie(true)
                        
                        // Handle file downloads directly to device storage
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            downloadUrl = url
                            showDownloadDialog = true
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url.toString()
                                
                                // Handle Spotify authentication by opening in external browser
                                if (url.contains("accounts.spotify.com") || 
                                    url.contains("login.spotify.com")) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    return true
                                }
                                
                                // Handle CSV downloads through download listener
                                if (url.endsWith(".csv") || url.contains("download=true")) {
                                    // Let the download listener handle it
                                    return false
                                }
                                
                                return super.shouldOverrideUrlLoading(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                hasError = false
                                isConnectedToInternet = checkInternetConnection()
                                
                                // Inject JavaScript to handle authentication and downloads
                                url?.let { currentUrl ->
                                    if (currentUrl.contains("exportify.app", ignoreCase = true)) {
                                        view?.evaluateJavascript("""
                                            // Intercept download clicks
                                            document.addEventListener('click', function(e) {
                                                const target = e.target;
                                                if (target.tagName === 'A' && 
                                                    (target.href.includes('.csv') || 
                                                     target.href.includes('download') ||
                                                     target.download)) {
                                                    e.preventDefault();
                                                    // Let the native download listener handle it
                                                    window.location.href = target.href;
                                                }
                                            });
                                            
                                            // Monitor for authentication completion
                                            if (window.location.href.includes('exportify.app') && 
                                                !window.location.href.includes('login')) {
                                                console.log('Exportify authentication successful');
                                                
                                                // Store authentication state in localStorage for persistence
                                                localStorage.setItem('spotify_authenticated', 'true');
                                            }
                                            
                                            // Check if already authenticated
                                            if (localStorage.getItem('spotify_authenticated') === 'true') {
                                                console.log('User is already authenticated');
                                            }
                                        """.trimIndent(), null)
                                    }
                                }
                            }
                            
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                isLoading = false
                                hasError = true
                                
                                // Check if the error is due to no internet connection
                                val errorCode = error?.errorCode
                                if (errorCode == ERROR_HOST_LOOKUP || 
                                    errorCode == ERROR_CONNECT || 
                                    errorCode == ERROR_TIMEOUT) {
                                    isConnectedToInternet = false
                                }
                            }
                        }
                        
                        webChromeClient = WebChromeClient()
                        
                        // Add JavaScript interface for communication
                        addJavascriptInterface(ExportifyWebInterface(ctx), "Android")
                        
                        // Load the Exportify website
                        loadUrl("https://thecub4.netlify.app/spotifyfeature/")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Refresh if there was an error and we're connected
                    if (hasError && isConnectedToInternet) {
                        hasError = false
                        isLoading = true
                        view.loadUrl("https://thecub4.netlify.app/spotifyfeature/")
                    }
                }
            )
        }

        // Show loading indicator
        if (isLoading && isConnectedToInternet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                Loader()
            }
        }
        
        // Show error message if there's an error but still connected
        if (hasError && isConnectedToInternet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to load Exportify. Pull down to refresh.",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Show "Connect to Internet" message with butterfly if no connection
        if (!isConnectedToInternet) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {

                    // Butterfly Image inside Rounded Card
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.butterfly),
                            contentDescription = "Butterfly",
                            modifier = Modifier
                                .size(140.dp)
                                .padding(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Connect to Internet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4CAF50) // Nice green
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Please check your internet connection and try again",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isConnectedToInternet = checkInternetConnection()
                            if (isConnectedToInternet) {
                                isLoading = true
                                webView?.loadUrl("https://thecub4.netlify.app/spotifyfeature/")
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }

    // Handle back navigation for WebView
    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else if (hasError && isConnectedToInternet) {
            // Refresh on back press if there's an error
            hasError = false
            isLoading = true
            webView?.loadUrl("https://thecub4.netlify.app/spotifyfeature/")
        } else if (!isConnectedToInternet) {
            // Check connection again on back press
            isConnectedToInternet = checkInternetConnection()
        }
    }
}
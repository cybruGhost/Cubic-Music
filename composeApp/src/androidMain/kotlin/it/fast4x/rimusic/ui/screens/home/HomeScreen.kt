package it.fast4x.rimusic.ui.screens.home

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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebResourceError
import kotlin.math.roundToInt

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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

        // DRAGGABLE DJ BUTTON - Smaller and movable
        DraggableDJButton(
            onTap = {
                try {
                    // Try multiple navigation approaches
                    navController.navigate("DjVeda")
                } catch (e: Exception) {
                    // If DjVeda route doesn't work, try alternative
                    try {
                        navController.navigate("djveda") // lowercase
                    } catch (e2: Exception) {
                        android.util.Log.e("HomeScreen", "DJ navigation failed: ${e.message}")
                        // Fallback to settings for testing
                        navController.navigate(NavRoutes.settings.name)
                    }
                }
            },
            modifier = Modifier.zIndex(100f)
        )
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

@Composable
fun DraggableDJButton(
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Draggable state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // Multiple animations for amazing effects
    val infiniteTransition = rememberInfiniteTransition(label = "dj_button_animations")
    
    // Pulsing scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f, // Smaller pulse
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000), // Slower rotation
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Glow opacity animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f, // Softer glow
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .offset { 
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt()
                )
            }
            .size(44.dp) // Smaller size
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                rotationZ = rotation
            }
            .drawWithCache {
                // Create gradient brush for the button
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6200EE),
                        Color(0xFF9C27B0),
                        Color(0xFFE91E63)
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 1.5f
                )
                
                // Create glow brush
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF00FF).copy(alpha = glowAlpha),
                        Color(0xFF00FFFF).copy(alpha = glowAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width * 1.1f
                )
                
                onDrawWithContent {
                    // Draw glow effect
                    drawCircle(
                        brush = glowBrush,
                        radius = size.width / 1.8f,
                        blendMode = BlendMode.Plus
                    )
                    
                    // Draw main button with gradient
                    drawCircle(
                        brush = gradientBrush,
                        radius = size.width / 2 - 3.dp.toPx()
                    )
                    
                    // Draw outer ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = size.width / 2,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    // Draw content (icon)
                    drawContent()
                }
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
    ) {
        FloatingActionButton(
            onClick = onTap,
            modifier = Modifier
                .size(44.dp)
                .scale(pulseScale),
            shape = CircleShape,
            containerColor = Color.Transparent, // Using custom draw instead
            contentColor = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Rotating icon with multiple effects
                Icon(
                    painter = painterResource(id = R.drawable.medical),
                    contentDescription = "DJ Veda - Drag to move, Tap to open",
                    modifier = Modifier
                        .size(20.dp) // Smaller icon
                        .rotate(rotation * -0.3f), // Less counter-rotation
                    tint = Color.White
                )
                
                // Tiny pulsing dot in center
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            scaleX = 1f + (pulseScale - 1f) * 1.5f
                            scaleY = 1f + (pulseScale - 1f) * 1.5f
                            alpha = glowAlpha
                        }
                        .drawWithCache {
                            onDrawBehind {
                                drawCircle(
                                    color = Color.Yellow.copy(alpha = glowAlpha),
                                    radius = size.width / 2
                                )
                            }
                        }
                )
            }
        }
    }
}

// Rest of the file remains the same (ExportifyWebViewScreen, etc.)
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
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }

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
                        
                        // FIXED: Updated deprecated onReceivedError method
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            isLoading = false
                            hasError = true
                        }
                        
                        // For older Android versions, keep the deprecated method but suppress warning
                        @Suppress("DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?, 
                            errorCode: Int, 
                            description: String?, 
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            isLoading = false
                            hasError = true
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
                // Refresh if there was an error
                if (hasError) {
                    hasError = false
                    isLoading = true
                    view.loadUrl("https://thecub4.netlify.app/spotifyfeature/")
                }
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
        
        if (hasError) {
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
    }

    // Handle back navigation for WebView
    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else if (hasError) {
            // Refresh on back press if there's an error
            hasError = false
            isLoading = true
            webView?.loadUrl("https://thecub4.netlify.app/spotifyfeature/")
        }
    }
}


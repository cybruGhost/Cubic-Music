package it.fast4x.rimusic.extensions.youtubelogin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.kreate.android.R
import it.fast4x.rimusic.ui.components.themed.Title
import it.fast4x.rimusic.utils.ytVisitorDataKey
import it.fast4x.rimusic.utils.ytCookieKey
import it.fast4x.rimusic.utils.ytAccountNameKey
import it.fast4x.rimusic.utils.ytAccountEmailKey
import it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.ytAccountThumbnailKey
import it.fast4x.rimusic.utils.ytDataSyncIdKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    onLogin: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = Innertube.DEFAULT_VISITOR_DATA)
    var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
    var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
    var accountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
    var accountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
    var accountChannelHandle by rememberPreference(key = ytAccountChannelHandleKey, defaultValue = "")
    var accountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")

    var webView: WebView? = null
    var hasLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf<String?>(null) }

    // Check if we're already logged in when composable starts
    LaunchedEffect(Unit) {
        if (cookie.isNotEmpty() && cookie.contains("SAPISID") && !hasLoggedIn) {
            Timber.d("YouTubeLogin: Already have cookie with SAPISID")
            hasLoggedIn = true
            onLogin(cookie)
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title(
            title = "Login to YouTube Music",
            icon = R.drawable.chevron_down,
            onClick = {
                // Check if we have valid cookies before dismissing
                if (cookie.contains("SAPISID")) {
                    onLogin(cookie)
                } else {
                    android.widget.Toast.makeText(context, "Please complete login first", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            currentUrl = url
                            Timber.d("YouTubeLogin: Page started: $url")
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            Timber.d("YouTubeLogin: Page finished: $url")
                            currentUrl = url

                            // Get all cookies from all domains
                            val cookieManager = CookieManager.getInstance()
                            val allCookies = cookieManager.getCookie("https://accounts.google.com") ?: ""
                            val youtubeCookies = cookieManager.getCookie("https://youtube.com") ?: ""
                            val musicCookies = cookieManager.getCookie("https://music.youtube.com") ?: ""
                            
                            // Combine all cookies
                            val combinedCookies = buildString {
                                if (allCookies.isNotEmpty()) append("$allCookies; ")
                                if (youtubeCookies.isNotEmpty()) append("$youtubeCookies; ")
                                if (musicCookies.isNotEmpty()) append("$musicCookies; ")
                            }.trim().removeSuffix(";")
                            
                            Timber.d("YouTubeLogin: Combined cookies: $combinedCookies")
                            
                            if (combinedCookies.isNotEmpty()) {
                                // Check for SAPISID cookie
                                if (combinedCookies.contains("SAPISID") && !hasLoggedIn) {
                                    cookie = combinedCookies
                                    Timber.d("YouTubeLogin: Saved cookie with SAPISID: ${cookie.contains("SAPISID")}")
                                    
                                    // Try to get VISITOR_DATA and DATASYNC_ID from JavaScript
                                    loadUrl("javascript:(function() {" +
                                            "try {" +
                                            "  console.log('Trying to get YouTube config...');" +
                                            "  console.log('yt object:', window.yt);" +
                                            "  console.log('yt.config_:', window.yt?.config_);" +
                                            "  var visitorData = window.yt?.config_?.VISITOR_DATA || window.ytcfg?.data_?.VISITOR_DATA;" +
                                            "  var dataSyncId = window.yt?.config_?.DATASYNC_ID || window.ytcfg?.data_?.DATASYNC_ID;" +
                                            "  console.log('visitorData:', visitorData);" +
                                            "  console.log('dataSyncId:', dataSyncId);" +
                                            "  if (visitorData) Android.onRetrieveVisitorData(visitorData);" +
                                            "  if (dataSyncId) Android.onRetrieveDataSyncId(dataSyncId);" +
                                            "} catch(e) {" +
                                            "  console.log('Error getting YouTube config:', e);" +
                                            "}" +
                                            "})()")
                                    
                                    // Wait a bit and then try to fetch account info
                                    scope.launch {
                                        delay(2000) // Give time for cookies to be fully set
                                        
                                        try {
                                            isLoading = true
                                            val accountInfo = Innertube.accountInfo().getOrNull()
                                            Timber.d("YouTubeLogin: Account info result: $accountInfo")
                                            
                                            accountInfo?.let {
                                                accountName = it.name.orEmpty()
                                                accountEmail = it.email.orEmpty()
                                                accountChannelHandle = it.channelHandle.orEmpty()
                                                accountThumbnail = it.thumbnailUrl.orEmpty()
                                                
                                                android.widget.Toast.makeText(context, "Logged in as ${it.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                
                                                // Trigger login callback
                                                onLogin(cookie)
                                            } ?: run {
                                                // Even if we can't get account info, if we have SAPISID, we're logged in
                                                if (cookie.contains("SAPISID")) {
                                                    android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                    hasLoggedIn = true
                                                    isLoading = false
                                                    onLogin(cookie)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e("YouTubeLogin: Error fetching account info: ${e.message}")
                                            // Even if account info fails, if we have cookies, we're logged in
                                            if (cookie.contains("SAPISID")) {
                                                android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                onLogin(cookie)
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                            
                            // If we're on music.youtube.com and not logged in yet, check for cookies again
                            if (url?.contains("music.youtube.com") == true && !hasLoggedIn) {
                                Timber.d("YouTubeLogin: On music.youtube.com but not logged in, checking cookies...")
                                loadUrl("javascript:(function() {" +
                                        "try {" +
                                        "  var visitorData = window.yt?.config_?.VISITOR_DATA || window.ytcfg?.data_?.VISITOR_DATA;" +
                                        "  var dataSyncId = window.yt?.config_?.DATASYNC_ID || window.ytcfg?.data_?.DATASYNC_ID;" +
                                        "  if (visitorData) Android.onRetrieveVisitorData(visitorData);" +
                                        "  if (dataSyncId) Android.onRetrieveDataSyncId(dataSyncId);" +
                                        "} catch(e) {}" +
                                        "})()")
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                            Timber.d("YouTubeLogin: Navigating to: $url")
                            // Allow navigation to continue
                            return false
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            super.onLoadResource(view, url)
                            // Debug: log resource loading
                            if (url?.contains("youtube") == true || url?.contains("google") == true) {
                                Timber.d("YouTubeLogin: Loading resource: $url")
                            }
                        }
                    }
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportMultipleWindows(false)
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        blockNetworkLoads = false
                    }
                    
                    // Enable cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    // Note: setAcceptFileSchemeCookies is deprecated and not needed for HTTP/HTTPS cookies
                    
                    // Add JavaScript interface
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            newVisitorData?.let {
                                Timber.d("YouTubeLogin: Received VISITOR_DATA: $it")
                                visitorData = it
                            }
                        }
                        
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            newDataSyncId?.let {
                                Timber.d("YouTubeLogin: Received DATASYNC_ID: $it")
                                dataSyncId = it
                            }
                        }
                    }, "Android")
                    
                    webView = this
                    
                    // Clear any existing cookies before starting login
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()
                    WebStorage.getInstance().deleteAllData()
                    
                    // Load the login page with proper headers
                    val headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Safari/537.36",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                        "Accept-Language" to "en-US,en;q=0.5",
                        "Accept-Encoding" to "gzip, deflate, br",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1"
                    )
                    
                    // Start with music.youtube.com directly - it will redirect to login if needed
                    loadUrl("https://music.youtube.com", headers)
                    
                    // Alternative: Use the Google login URL that redirects to music.youtube.com
                    // loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1&hl=en", headers)
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }

        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        }
    }
}
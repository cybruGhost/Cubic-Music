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
import it.fast4x.rimusic.extensions.youtubelogin.AccountInfoFetcher
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    onLogin: (YoutubeSession) -> Unit
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
    var fetchedAccountInfo by remember { mutableStateOf(false) }

    // Check if we're already logged in when composable starts
    LaunchedEffect(Unit) {
        if (cookie.isNotEmpty() && cookie.contains("SAPISID") && !hasLoggedIn) {
            Timber.d("YouTubeLogin: Already have cookie with SAPISID")
            // Try to fetch account info
            isLoading = true
            val accountInfo = AccountInfoFetcher.fetchAccountInfo()
            if (accountInfo != null) {
                accountName = accountInfo.name.orEmpty()
                accountEmail = accountInfo.email.orEmpty()
                accountChannelHandle = accountInfo.channelHandle.orEmpty()
                accountThumbnail = accountInfo.thumbnailUrl.orEmpty()
            }
            hasLoggedIn = true
            isLoading = false
            onLogin(YoutubeSession(cookie, visitorData, accountName, accountEmail, accountChannelHandle))
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
                    val session = YoutubeSession(
                        cookie = cookie,
                        visitorData = visitorData,
                        accountName = accountName,
                        accountEmail = accountEmail,
                        accountChannelHandle = accountChannelHandle
                    )
                    onLogin(session)
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
                                if (combinedCookies.contains("SAPISID") && !hasLoggedIn && !fetchedAccountInfo) {
                                    cookie = combinedCookies
                                    Timber.d("YouTubeLogin: Saved cookie with SAPISID: ${cookie.contains("SAPISID")}")
                                    
                                    // Try to get VISITOR_DATA and DATASYNC_ID from JavaScript
                                    loadUrl("javascript:(function() {" +
                                            "try {" +
                                            "  var visitorData = window.yt?.config_?.VISITOR_DATA || window.ytcfg?.data_?.VISITOR_DATA;" +
                                            "  var dataSyncId = window.yt?.config_?.DATASYNC_ID || window.ytcfg?.data_?.DATASYNC_ID;" +
                                            "  if (visitorData) Android.onRetrieveVisitorData(visitorData);" +
                                            "  if (dataSyncId) Android.onRetrieveDataSyncId(dataSyncId);" +
                                            "} catch(e) {" +
                                            "}" +
                                            "})()")
                                    
                                    // Set flag to prevent multiple fetches
                                    fetchedAccountInfo = true
                                    
                                    // Wait a bit and then try to fetch account info
                                    scope.launch {
                                        delay(2000) // Give time for cookies to be fully set
                                        
                                        try {
                                            isLoading = true
                                            val accountInfo = AccountInfoFetcher.fetchAccountInfo()
                                            
                                            accountInfo?.let {
                                                accountName = it.name.orEmpty()
                                                accountEmail = it.email.orEmpty()
                                                accountChannelHandle = it.channelHandle.orEmpty()
                                                accountThumbnail = it.thumbnailUrl.orEmpty()
                                                
                                                Timber.d("YouTubeLogin: Saved account info:")
                                                Timber.d("  Name: $accountName")
                                                Timber.d("  Email: $accountEmail")
                                                Timber.d("  Channel: $accountChannelHandle")
                                                
                                                // Create session with all data
                                                val session = YoutubeSession(
                                                    cookie = cookie,
                                                    visitorData = visitorData,
                                                    accountName = accountName,
                                                    accountEmail = accountEmail,
                                                    accountChannelHandle = accountChannelHandle
                                                )
                                                
                                                android.widget.Toast.makeText(context, "Logged in as ${it.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                
                                                // Trigger login callback with complete session
                                                onLogin(session)
                                            } ?: run {
                                                // Even if we can't get account info, if we have SAPISID, we're logged in
                                                if (cookie.contains("SAPISID")) {
                                                    Timber.d("YouTubeLogin: Cookie has SAPISID but couldn't fetch account info")
                                                    
                                                    // Create session with basic data
                                                    val session = YoutubeSession(
                                                        cookie = cookie,
                                                        visitorData = visitorData,
                                                        accountName = accountName,
                                                        accountEmail = accountEmail,
                                                        accountChannelHandle = accountChannelHandle
                                                    )
                                                    
                                                    android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                    hasLoggedIn = true
                                                    isLoading = false
                                                    onLogin(session)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e("YouTubeLogin: Error fetching account info: ${e.message}")
                                            // Even if account info fails, if we have cookies, we're logged in
                                            if (cookie.contains("SAPISID")) {
                                                // Create session with basic data
                                                val session = YoutubeSession(
                                                    cookie = cookie,
                                                    visitorData = visitorData,
                                                    accountName = accountName,
                                                    accountEmail = accountEmail,
                                                    accountChannelHandle = accountChannelHandle
                                                )
                                                
                                                android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                onLogin(session)
                                            }
                                        } finally {
                                            isLoading = false
                                            fetchedAccountInfo = false
                                        }
                                    }
                                }
                            }
                            
                            // If we're on music.youtube.com and not logged in yet, check for cookies again
                            if (url?.contains("music.youtube.com") == true && !hasLoggedIn && !fetchedAccountInfo) {
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
                            return false
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
                    
                    // Start with music.youtube.com directly
                    loadUrl("https://music.youtube.com", headers)
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
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
import it.fast4x.rimusic.extensions.youtubelogin.AccountInfoFetcher 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.utils.parseCookieString
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.kreate.android.R
import it.fast4x.rimusic.ui.components.themed.Title
import it.fast4x.rimusic.utils.rememberPreference
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

    // Use string literals for preference keys to avoid conflicts
    var visitorData by rememberPreference(key = "yt_visitor_data", defaultValue = Innertube.DEFAULT_VISITOR_DATA)
    var dataSyncId by rememberPreference(key = "yt_data_sync_id", defaultValue = "")
    var cookie by rememberPreference(key = "yt_cookie", defaultValue = "")
    var accountName by rememberPreference(key = "yt_account_name", defaultValue = "")
    var accountEmail by rememberPreference(key = "yt_account_email", defaultValue = "")
    var accountChannelHandle by rememberPreference(key = "yt_account_channel_handle", defaultValue = "")
    var accountThumbnail by rememberPreference(key = "yt_account_thumbnail", defaultValue = "")

    var webView: WebView? = null
    var hasLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var loginAttempted by remember { mutableStateOf(false) }

    // Check if we're already logged in when composable starts
    LaunchedEffect(Unit) {
        if (cookie.isNotEmpty() && parseCookieString(cookie).containsKey("SAPISID") && !hasLoggedIn && !loginAttempted) {
            Timber.d("YouTubeLogin: Already have cookie with SAPISID")
            
            // Try to fetch fresh account info
            isLoading = true
            try {
                // PASS THE COOKIE TO FETCHER
                val accountInfo = AccountInfoFetcher.fetchAccountInfo(cookie)
                accountInfo?.let {
                    accountName = it.name.orEmpty()
                    accountEmail = it.email.orEmpty()
                    accountChannelHandle = it.channelHandle.orEmpty()
                    accountThumbnail = it.thumbnailUrl.orEmpty()
                    Timber.d("YouTubeLogin: Updated account info from existing login")
                }
            } catch (e: Exception) {
                Timber.e("YouTubeLogin: Error fetching account info: ${e.message}")
            }
            
            hasLoggedIn = true
            isLoading = false
            loginAttempted = true
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
                if (parseCookieString(cookie).containsKey("SAPISID")) {
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
                                cookie = combinedCookies
                                
                                // Check for SAPISID cookie
                                if (parseCookieString(combinedCookies).containsKey("SAPISID") && !hasLoggedIn && !loginAttempted) {
                                    Timber.d("YouTubeLogin: Found SAPISID cookie, attempting login!")
                                    
                                    // Try to get VISITOR_DATA and DATASYNC_ID from JavaScript
                                    loadUrl("javascript:(function() {" +
                                            "try {" +
                                            "  var visitorData = window.yt?.config_?.VISITOR_DATA || window.ytcfg?.data_?.VISITOR_DATA;" +
                                            "  var dataSyncId = window.yt?.config_?.DATASYNC_ID || window.ytcfg?.data_?.DATASYNC_ID;" +
                                            "  if (visitorData) Android.onRetrieveVisitorData(visitorData);" +
                                            "  if (dataSyncId) Android.onRetrieveDataSyncId(dataSyncId);" +
                                            "} catch(e) {" +
                                            "  console.log('Error getting JS data:', e);" +
                                            "}" +
                                            "})()")
                                    
                                    // Try to fetch account info using AccountInfoFetcher
                                    scope.launch {
                                        try {
                                            isLoading = true
                                            loginAttempted = true
                                            delay(2000) // Give time for cookies to be fully set
                                            
                                            // Use AccountInfoFetcher to get account info - PASS THE COOKIE!
                                            val accountInfo = AccountInfoFetcher.fetchAccountInfo(cookie)
                                            
                                            accountInfo?.let {
                                                accountName = it.name.orEmpty()
                                                accountEmail = it.email.orEmpty()
                                                accountChannelHandle = it.channelHandle.orEmpty()
                                                accountThumbnail = it.thumbnailUrl.orEmpty()
                                                
                                                Timber.d("YouTubeLogin: Saved account info from AccountInfoFetcher:")
                                                Timber.d("  Name: $accountName")
                                                Timber.d("  Email: $accountEmail")
                                                Timber.d("  Channel: $accountChannelHandle")
                                                Timber.d("  Thumbnail: $accountThumbnail")
                                                
                                                android.widget.Toast.makeText(context, "Logged in as ${it.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                onLogin(cookie)
                                            } ?: run {
                                                // Even if account info fails, if we have cookies, we're logged in
                                                if (parseCookieString(cookie).containsKey("SAPISID")) {
                                                    Timber.d("YouTubeLogin: Cookie has SAPISID but AccountInfoFetcher returned null")
                                                    
                                                    android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                    hasLoggedIn = true
                                                    isLoading = false
                                                    onLogin(cookie)
                                                } else {
                                                    // No SAPISID, not logged in
                                                    isLoading = false
                                                    loginAttempted = false
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e("YouTubeLogin: Error fetching account info: ${e.message}")
                                            // Even if account info fails, if we have cookies, we're logged in
                                            if (parseCookieString(cookie).containsKey("SAPISID")) {
                                                android.widget.Toast.makeText(context, "Logged in successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                hasLoggedIn = true
                                                isLoading = false
                                                onLogin(cookie)
                                            } else {
                                                // No SAPISID, not logged in
                                                isLoading = false
                                                loginAttempted = false
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                            
                            // Always try to get visitor data and data sync ID
                            loadUrl("javascript:(function() {" +
                                    "try {" +
                                    "  var visitorData = window.yt?.config_?.VISITOR_DATA || window.ytcfg?.data_?.VISITOR_DATA;" +
                                    "  var dataSyncId = window.yt?.config_?.DATASYNC_ID || window.ytcfg?.data_?.DATASYNC_ID;" +
                                    "  if (visitorData) Android.onRetrieveVisitorData(visitorData);" +
                                    "  if (dataSyncId) Android.onRetrieveDataSyncId(dataSyncId);" +
                                    "} catch(e) {}" +
                                    "})()")
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
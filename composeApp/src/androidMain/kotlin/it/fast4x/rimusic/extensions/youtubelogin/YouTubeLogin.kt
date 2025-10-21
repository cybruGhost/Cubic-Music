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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    onLogin: (String) -> Unit
) {

    val scope = rememberCoroutineScope()

    var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = Innertube.DEFAULT_VISITOR_DATA)
    var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
    var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
    var accountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
    var accountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
    var accountChannelHandle by rememberPreference(key = ytAccountChannelHandleKey, defaultValue = "")
    var accountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")

    var hasLoggedIn by remember { mutableStateOf(false) }
    var currentAccount by remember { mutableStateOf("") }
    var webView: WebView? = null

    Column (
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title(
            "Login to YouTube Music",
            icon = R.drawable.chevron_down,
            onClick = { onLogin(cookie) }
        )

        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                            Timber.d("YouTubeLogin: URL changed to $url")
                            
                            if (url.startsWith("https://music.youtube.com") && !hasLoggedIn) {
                                hasLoggedIn = true
                                
                                // Get cookies first
                                val cookieManager = CookieManager.getInstance()
                                val allCookies = cookieManager.getCookie(url)
                                cookie = allCookies.orEmpty()
                                
                                Timber.d("YouTubeLogin: Cookies retrieved, length: ${cookie.length}")
                                Timber.d("YouTubeLogin: VisitorData: $visitorData")
                                Timber.d("YouTubeLogin: DataSyncId: $dataSyncId")
                                
                                // Now fetch account info to verify login and get account details
                                scope.launch {
                                    try {
                                        Timber.d("YouTubeLogin: Fetching account info...")
                                        val accountInfo = Innertube.accountInfo().getOrNull()
                                        Timber.d("YouTubeLogin: Account info received: $accountInfo")
                                        
                                        if (accountInfo != null) {
                                            val newAccountName = accountInfo.name.orEmpty()
                                            val newAccountEmail = accountInfo.email.orEmpty()
                                            
                                            // Check if this is a different account
                                            val isDifferentAccount = newAccountEmail.isNotBlank() && 
                                                                     newAccountEmail != accountEmail &&
                                                                     accountEmail.isNotBlank()
                                            
                                            if (isDifferentAccount) {
                                                Timber.d("YouTubeLogin: Switching to different account: $newAccountEmail")
                                                // Clear previous artist data when switching accounts
                                                // This ensures we fetch fresh data for the new account
                                            }
                                            
                                            accountName = newAccountName
                                            accountEmail = newAccountEmail
                                            accountChannelHandle = accountInfo.channelHandle.orEmpty()
                                            accountThumbnail = accountInfo.thumbnailUrl.orEmpty()
                                            currentAccount = newAccountEmail
                                            
                                            Timber.d("YouTubeLogin: Login successful - Account: $accountName ($accountEmail)")
                                            
                                            // Call onLogin with the cookie to complete the process
                                            onLogin(cookie)
                                        } else {
                                            Timber.e("YouTubeLogin: Account info is null - login may have failed")
                                            // Still proceed if we have cookies
                                            if (cookie.isNotBlank()) {
                                                onLogin(cookie)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Timber.e("YouTubeLogin: Error fetching account info: ${e.message}")
                                        e.printStackTrace()
                                        // Even if account info fails, proceed if we have cookies
                                        if (cookie.isNotBlank()) {
                                            onLogin(cookie)
                                        }
                                    }
                                }
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            Timber.d("YouTubeLogin: Page finished loading: $url")
                            
                            // Try to extract visitor data and data sync ID from multiple possible locations
                            view.loadUrl("""
                                javascript:(function() {
                                    try {
                                        if (window.yt && window.yt.config_ && window.yt.config_.VISITOR_DATA) {
                                            Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA);
                                        }
                                    } catch(e) {}
                                    
                                    try {
                                        if (window.yt && window.yt.config_ && window.yt.config_.DATASYNC_ID) {
                                            Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID);
                                        }
                                    } catch(e) {}
                                    
                                    try {
                                        if (window.ytcfg && window.ytcfg.data_ && window.ytcfg.data_.VISITOR_DATA) {
                                            Android.onRetrieveVisitorData(ytcfg.data_.VISITOR_DATA);
                                        }
                                    } catch(e) {}
                                })();
                            """.trimIndent())
                        }
                    }
                    
                    settings.apply {
                        javaScriptEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        domStorageEnabled = true
                        allowContentAccess = true
                        allowFileAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                    }
                    
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null && newVisitorData.isNotBlank() && newVisitorData != "null") {
                                visitorData = newVisitorData
                                Timber.d("YouTubeLogin: Visitor data updated: ${newVisitorData.take(20)}...")
                            }
                        }
                        
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null && newDataSyncId.isNotBlank() && newDataSyncId != "null") {
                                dataSyncId = newDataSyncId
                                Timber.d("YouTubeLogin: DataSync ID updated: ${newDataSyncId.take(20)}...")
                            }
                        }
                    }, "Android")
                    
                    webView = this
                    
                    // Clear previous session to ensure fresh login
                    clearCache(true)
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    
                    loadUrl("https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F")
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
    
    // Reset login state when entering the login screen
    LaunchedEffect(Unit) {
        hasLoggedIn = false
        Timber.d("YouTubeLogin: Login screen opened")
    }
}
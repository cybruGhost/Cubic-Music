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

    // Persistent storage
    var visitorData by rememberPreference("yt_visitor_data", "")
    var dataSyncId by rememberPreference("yt_data_sync_id", "")
    var cookie by rememberPreference("yt_cookie", "")

    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasLoggedIn by remember { mutableStateOf(false) }

    // Restore cookies and data before WebView loads
    LaunchedEffect(Unit) {
        if (cookie.isNotEmpty()) {
            Timber.d("Restoring saved cookies")
            val cm = CookieManager.getInstance()
            cookie.split(";").forEach { cm.setCookie("https://youtube.com", it.trim()) }
            cookie.split(";").forEach { cm.setCookie("https://music.youtube.com", it.trim()) }
            cm.flush()
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title(
            title = "Login to YouTube Music",
            icon = app.kreate.android.R.drawable.chevron_down,
            onClick = {
                if (cookie.contains("SAPISID")) {
                    onLogin(cookie)
                } else {
                    android.widget.Toast.makeText(context, "Please complete login first", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            scope.launch {
                                delay(1500) // Wait for cookies and JS to set

                                val cm = CookieManager.getInstance()
                                val ytCookies = cm.getCookie("https://youtube.com") ?: ""
                                val ytmCookies = cm.getCookie("https://music.youtube.com") ?: ""
                                val combinedCookies = listOf(ytCookies, ytmCookies)
                                    .filter { it.isNotBlank() }
                                    .joinToString("; ")
                                    .replace(";;", ";")
                                    .trim()
                                cookie = combinedCookies

                                if (combinedCookies.contains("SAPISID") && !hasLoggedIn) {
                                    Timber.d("Auto-login detected with SAPISID!")

                                    // Inject saved VISITOR_DATA & DATASYNC_ID
                                    if (visitorData.isNotEmpty() && dataSyncId.isNotEmpty()) {
                                        loadUrl(
                                            "javascript:ytcfg.set('VISITOR_DATA','$visitorData');" +
                                                    "ytcfg.set('DATASYNC_ID','$dataSyncId');"
                                        )
                                        delay(500)
                                    }

                                    // Fetch account info
                                    try {
                                        isLoading = true
                                        val accountInfo = AccountInfoFetcher.fetchAccountInfo(cookie)
                                        accountInfo?.let {
                                            android.widget.Toast.makeText(context, "Logged in as ${it.name}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Timber.e("Error fetching account info: ${e.message}")
                                    } finally {
                                        isLoading = false
                                        hasLoggedIn = true
                                        onLogin(cookie)
                                    }
                                }

                                // Always retrieve VISITOR_DATA & DATASYNC_ID
                                loadUrl(
                                    "javascript:(function() {" +
                                            "try {" +
                                            "  var ytcfg = window.ytcfg;" +
                                            "  if (ytcfg && ytcfg.get) {" +
                                            "    Android.onRetrieveVisitorData(ytcfg.get('VISITOR_DATA'));" +
                                            "    Android.onRetrieveDataSyncId(ytcfg.get('DATASYNC_ID'));" +
                                            "  }" +
                                            "} catch(e) {}" +
                                            "})()"
                                )
                            }
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            newVisitorData?.let { visitorData = it }
                        }

                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            newDataSyncId?.let { dataSyncId = it }
                        }
                    }, "Android")

                    webView = this
                    loadUrl("https://music.youtube.com")
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )
        }
    }
}
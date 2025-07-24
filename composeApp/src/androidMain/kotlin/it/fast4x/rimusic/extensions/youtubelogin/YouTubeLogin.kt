package it.fast4x.rimusic.extensions.youtubelogin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.kreate.android.Preferences
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.ui.components.themed.Title
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    onLogin: (String) -> Unit
) {
    var visitorData by Preferences.YOUTUBE_VISITOR_DATA
    var dataSyncId by Preferences.YOUTUBE_SYNC_ID
    var cookie by Preferences.YOUTUBE_COOKIES
    var accountName by Preferences.YOUTUBE_ACCOUNT_NAME
    var accountEmail by Preferences.YOUTUBE_ACCOUNT_EMAIL
    var accountChannelHandle by Preferences.YOUTUBE_SELF_CHANNEL_HANDLE
    var accountThumbnail by Preferences.YOUTUBE_ACCOUNT_AVATAR

    var webView: WebView? = null

    Column (
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title("Login to YouTube Music",
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
                        override fun onPageFinished(view: WebView, url: String?) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            if ( url?.startsWith("https://music.youtube.com") == false ) return

                            cookie = CookieManager.getInstance().getCookie( url )
                            GlobalScope.launch {
                                Innertube.accountInfo().onSuccess {
                                    accountName = it?.name.orEmpty()
                                    accountEmail = it?.email.orEmpty()
                                    accountChannelHandle = it?.channelHandle.orEmpty()
                                    accountThumbnail = it?.thumbnailUrl.orEmpty()
                                    onLogin(cookie)
                                }.onFailure {
                                    it.printStackTrace()
                                    it.message?.let( Toaster::e )
                                }
                            }
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                    }
                    addJavascriptInterface(object {
                        @Suppress("unused")     // Suppress to prevent accidental deletion
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorData = newVisitorData
                            }
                        }
                        @Suppress("unused")     // Suppress to prevent accidental deletion
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null) {
                                dataSyncId = newDataSyncId
                            }
                        }
                    }, "Android")
                    webView = this
                    loadUrl( "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com" )
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
}

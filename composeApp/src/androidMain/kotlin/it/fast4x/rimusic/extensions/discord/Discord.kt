package it.fast4x.rimusic.extensions.discord

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.ui.components.themed.IconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.view.View
import androidx.compose.ui.platform.LocalContext
import android.content.Context

private const val JS_SNIPPET = "javascript:(function(){var i=document.createElement('iframe');document.body.appendChild(i);alert(i.contentWindow.localStorage.token.slice(1,-1))})()"
private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginAndGetToken(
    navController: NavController,
    onGetToken: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
                        webView.stopLoading()
                        if (url.endsWith("/app")) {
                            webView.loadUrl(JS_SNIPPET)
                            webView.visibility = View.GONE
                        }
                        return false
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        if (url.contains("/app")) {
                            view.loadUrl(JS_SNIPPET)
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult,
                    ): Boolean {
                        scope.launch(Dispatchers.Main) {
                            onGetToken(message)
                            navController.navigateUp()
                        }
                        this@apply.visibility = View.GONE
                        result.confirm()
                        return true
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                if (android.os.Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                    settings.userAgentString = SAMSUNG_USER_AGENT
                }
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                WebStorage.getInstance().deleteAllData()
                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text("Login to Discord") },
        navigationIcon = {
            IconButton(
                icon = R.drawable.chevron_back,
                onClick = navController::navigateUp,
                color = Color.White
            )
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
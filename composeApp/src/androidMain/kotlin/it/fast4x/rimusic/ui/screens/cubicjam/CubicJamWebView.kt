package it.fast4x.rimusic.ui.screens.cubicjam

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import app.kreate.android.R
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.ui.components.themed.Title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CubicJamWebView(
    navController: NavController,
    initialUrl: String = "https://swipes.lovable.app"
) {
    val scope = rememberCoroutineScope()
    var webView: WebView? = null

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        // Header
        Title(
            title = "Cubic Jam Web",
            onClick = { navController.navigateUp() }
        )

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
                            return false
                        }
                        
                        override fun onPageFinished(view: WebView, url: String) {
                            // Check if user is already logged in via token
                            val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
                            val token = prefs.getString("bearer_token", null)
                            
                            if (token != null && url.contains("lovable.app")) {
                                // Inject token into localStorage if needed
                                val js = """
                                    localStorage.setItem('supabase.auth.token', JSON.stringify({
                                        access_token: '$token',
                                        token_type: 'bearer',
                                        expires_in: 3600,
                                        refresh_token: '',
                                        user: null
                                    }));
                                    console.log('Cubic Jam token injected');
                                """.trimIndent()
                                evaluateJavascript(js, null)
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        // Handle any JavaScript dialogs if needed
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    
                    if (android.os.Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                        settings.userAgentString = SAMSUNG_USER_AGENT
                    }
                    
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    webView = this
                    
                    // Load Cubic Jam website
                    loadUrl(initialUrl)
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
}
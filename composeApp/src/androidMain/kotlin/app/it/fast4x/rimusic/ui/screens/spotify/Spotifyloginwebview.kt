package app.it.fast4x.rimusic.ui.screens.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography

private const val PREFS_NAME = "spotify_prefs"
private const val KEY_SP_DC = "spotify_sp_dc"
private const val KEY_LOGGED_IN = "spotify_logged_in"
private const val KEY_ACCOUNT_NAME = "spotify_account_name"
private const val KEY_SAVED_AT = "spotify_saved_at"
private const val KEY_EXPIRES_AT = "spotify_expires_at"
private const val KEY_COOKIE_HEADER = "spotify_cookie_header"

private const val SESSION_ESTIMATE_MS = 7L * 24L * 60L * 60L * 1000L

data class SpotifySessionInfo(
    val spDc: String,
    val cookieHeader: String,
    val accountName: String,
    val savedAt: Long,
    val expiresAt: Long
)

fun saveSpotifySession(
    context: Context,
    spDc: String,
    accountName: String = "",
    cookieHeader: String = "sp_dc=$spDc"
) {
    val now = System.currentTimeMillis()
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SP_DC, spDc)
        .putString(KEY_COOKIE_HEADER, cookieHeader)
        .putBoolean(KEY_LOGGED_IN, true)
        .putString(KEY_ACCOUNT_NAME, accountName)
        .putLong(KEY_SAVED_AT, now)
        .putLong(KEY_EXPIRES_AT, now + SESSION_ESTIMATE_MS)
        .apply()
}

fun getSpDc(context: Context): String? =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SP_DC, null)
        ?.takeIf { it.isNotBlank() }

fun isSpotifyLoggedIn(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_LOGGED_IN, false) &&
        !getSpDc(context).isNullOrBlank()

fun getSpotifyAccountName(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_ACCOUNT_NAME, "")
        .orEmpty()

fun getSpotifyCookieHeader(context: Context): String? =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_COOKIE_HEADER, null)
        ?.takeIf { it.isNotBlank() }
        ?: getSpDc(context)?.let { "sp_dc=$it" }

fun getSpotifySessionInfo(context: Context): SpotifySessionInfo? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val spDc = prefs.getString(KEY_SP_DC, null)?.takeIf { it.isNotBlank() } ?: return null
    return SpotifySessionInfo(
        spDc = spDc,
        cookieHeader = prefs.getString(KEY_COOKIE_HEADER, null).orEmpty().ifBlank { "sp_dc=$spDc" },
        accountName = prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty(),
        savedAt = prefs.getLong(KEY_SAVED_AT, 0L),
        expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
    )
}

fun getMaskedSpDc(context: Context): String {
    val value = getSpDc(context) ?: return "Not available"
    return if (value.length <= 12) value else "${value.take(6)}...${value.takeLast(6)}"
}

fun renewSpotifySession(context: Context): Boolean {
    val spDc = extractSpDcCookie() ?: return false
    val cookieHeader = extractSpotifyCookieHeader() ?: "sp_dc=$spDc"
    saveSpotifySession(context, spDc, getSpotifyAccountName(context), cookieHeader)
    return true
}

fun clearSpotifySession(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_SP_DC)
        .remove(KEY_ACCOUNT_NAME)
        .remove(KEY_SAVED_AT)
        .remove(KEY_EXPIRES_AT)
        .remove(KEY_COOKIE_HEADER)
        .putBoolean(KEY_LOGGED_IN, false)
        .apply()

    val cookieManager = CookieManager.getInstance()
    expireSpotifyCookies(cookieManager, "https://open.spotify.com")
    expireSpotifyCookies(cookieManager, "https://accounts.spotify.com")
    expireSpotifyCookies(cookieManager, "https://spotify.com")
    cookieManager.flush()
}

private fun expireSpotifyCookies(cookieManager: CookieManager, url: String) {
    val cookies = cookieManager.getCookie(url).orEmpty()
    if (cookies.isBlank()) return

    cookies.split(";")
        .map { it.trim() }
        .mapNotNull { entry -> entry.substringBefore("=", "").takeIf { it.isNotBlank() } }
        .distinct()
        .forEach { name ->
            cookieManager.setCookie(
                url,
                "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
            )
        }
}

private fun extractSpDcCookie(): String? {
    val cookieManager = CookieManager.getInstance()
    val cookieString = cookieManager.getCookie("https://open.spotify.com")
        ?: cookieManager.getCookie("https://accounts.spotify.com")
        ?: return null

    return cookieString
        .split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("sp_dc=") }
        ?.removePrefix("sp_dc=")
        ?.trim()
        ?.takeIf { it.length > 50 }
}

private fun extractSpotifyCookieHeader(): String? {
    val cookieManager = CookieManager.getInstance()
    return cookieManager.getCookie("https://open.spotify.com")
        ?: cookieManager.getCookie("https://accounts.spotify.com")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginWebView(
    onLoginSuccess: (spDc: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isPageLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var loginDetected by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler { onDismiss() }

    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/127.0.0.0 Safari/537.36"
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadProgress = newProgress / 100f
                    isPageLoading = newProgress < 100
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageLoading = false

                    if (loginDetected) return

                    val spDc = extractSpDcCookie() ?: return
                    val cookieHeader = extractSpotifyCookieHeader() ?: "sp_dc=$spDc"
                    loginDetected = true
                    saveSpotifySession(context, spDc, view?.title.orEmpty(), cookieHeader)
                    onLoginSuccess(spDc)
                }
            }

            loadUrl(
                "https://accounts.spotify.com/en/login" +
                    "?continue=https%3A%2F%2Fopen.spotify.com%2F"
            )
        }
    }

    LaunchedEffect(webView) {
        webViewRef = webView
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.text.BasicText(
                text = "Log in to Spotify",
                style = typography().m.copy(color = colorPalette().text),
                modifier = Modifier.align(Alignment.CenterStart)
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(text = "Cancel", color = colorPalette().accent)
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isPageLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF1DB954),
                trackColor = colorPalette().background2
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isPageLoading && loadProgress < 0.15f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorPalette().background0)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF1DB954))
                        androidx.compose.foundation.text.BasicText(
                            text = "Loading Spotify...",
                            style = typography().s.copy(color = colorPalette().textSecondary),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

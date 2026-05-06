package app.it.fast4x.rimusic.extensions.youtubelogin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
import app.it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.it.fast4x.rimusic.ui.components.themed.Title
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin(
    autoCompleteExistingSession: Boolean = true,
    onLogin: (YoutubeSession) -> Unit
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
    var baselineCookie by remember { mutableStateOf("") }
    var baselineDataSyncId by remember { mutableStateOf("") }

    // Restore cookies and data before WebView loads
    LaunchedEffect(Unit) {
        val restoredSession = YouTubeSessionStore.applyCurrentSession(context)
        if (restoredSession != null) {
            cookie = restoredSession.cookie
            visitorData = restoredSession.visitorData
            dataSyncId = restoredSession.dataSyncId
            baselineCookie = restoredSession.cookie
            baselineDataSyncId = restoredSession.dataSyncId
        }

        if (cookie.isNotEmpty()) {
            Timber.d("Restoring saved cookies")
            val cm = CookieManager.getInstance()
            cookie.split(";").forEach { cm.setCookie("https://youtube.com", it.trim()) }
            cookie.split(";").forEach { cm.setCookie("https://music.youtube.com", it.trim()) }
            cm.flush()
        }
    }

    suspend fun saveScopedSession(cookies: String): YoutubeSession {
        val accountInfo = YtmSessionApi.fetchAccountInfo(cookies).getOrThrow()
        val linkedAccounts = YtmSessionApi.listAccounts(cookies).getOrDefault(emptyList())
        val selectedAccount = linkedAccounts.firstOrNull { it.isSelected }
            ?: linkedAccounts.firstOrNull()
        val switched = selectedAccount?.let { account ->
            account.authUser.takeIf { it.isNotBlank() }?.let { authUser ->
                runCatching {
                    YtmSessionApi.switchAccount(
                        cookies = cookies,
                        authUser = authUser,
                        pageId = account.pageId.ifBlank { null }
                    ).getOrThrow()
                }.getOrNull()
            }
        }

        return YouTubeSessionStore.saveSession(
            context = context,
            session = YoutubeSession(
                cookie = switched?.cookie?.ifBlank { cookies } ?: cookies,
                visitorData = switched?.visitorData?.ifBlank { visitorData } ?: visitorData,
                dataSyncId = switched?.dataSyncId?.ifBlank { dataSyncId } ?: dataSyncId,
                authUser = switched?.authUser?.ifBlank { selectedAccount?.authUser.orEmpty() }
                    ?: selectedAccount?.authUser.orEmpty(),
                pageId = switched?.pageId?.ifBlank { selectedAccount?.pageId.orEmpty() }
                    ?: selectedAccount?.pageId.orEmpty(),
                accountName = switched?.accountName?.ifBlank { accountInfo.accountName }
                    ?: accountInfo.accountName,
                accountEmail = switched?.accountEmail?.ifBlank { accountInfo.accountEmail }
                    ?: accountInfo.accountEmail,
                accountChannelHandle = switched?.accountChannelHandle?.ifBlank { accountInfo.accountChannelHandle }
                    ?: accountInfo.accountChannelHandle,
                accountThumbnail = switched?.accountThumbnail?.ifBlank { accountInfo.accountThumbnail }
                    ?: accountInfo.accountThumbnail,
                lastAccountRefreshAt = if (accountInfo.hasSession) {
                    System.currentTimeMillis()
                } else {
                    0L
                }
            ),
            makePreferred = true
        )
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        Title(
            title = "Connect YouTube Music",
            icon = app.kreate.android.R.drawable.chevron_down,
            onClick = {
                if (YouTubeSessionStore.hasAuthCookies(cookie)) {
                    scope.launch {
                        isLoading = true
                        runCatching {
                            saveScopedSession(cookie)
                        }.onSuccess { session ->
                            onLogin(session)
                        }.onFailure {
                            val session = YouTubeSessionStore.saveSession(
                                context = context,
                                session = YoutubeSession(
                                    cookie = cookie,
                                    visitorData = visitorData,
                                    dataSyncId = dataSyncId
                                ),
                                makePreferred = true
                            )
                            onLogin(session)
                        }
                        isLoading = false
                    }
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
                                delay(400)

                                val cm = CookieManager.getInstance()
                                val ytCookies = cm.getCookie("https://youtube.com") ?: ""
                                val ytmCookies = cm.getCookie("https://music.youtube.com") ?: ""
                                val combinedCookies = YouTubeSessionStore.mergeCookieStrings(
                                    primary = ytmCookies,
                                    secondary = ytCookies
                                )
                                cookie = combinedCookies

                                val sessionChanged = combinedCookies.isNotBlank() && (
                                    combinedCookies != baselineCookie ||
                                        dataSyncId != baselineDataSyncId
                                    )
                                if (YouTubeSessionStore.hasAuthCookies(combinedCookies) &&
                                    !hasLoggedIn &&
                                    (autoCompleteExistingSession || sessionChanged)
                                ) {
                                    Timber.d("Auto-login detected with authenticated YouTube cookies")

                                    try {
                                        isLoading = true
                                        val savedSession = saveScopedSession(combinedCookies)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Logged in as ${savedSession.accountName.ifBlank { "YouTube Music" }}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        baselineCookie = savedSession.cookie
                                        baselineDataSyncId = savedSession.dataSyncId
                                        onLogin(savedSession)
                                    } catch (e: Exception) {
                                        Timber.e("Error fetching account info: ${e.message}")
                                        val savedSession = YouTubeSessionStore.saveSession(
                                            context = context,
                                            session = YoutubeSession(
                                                cookie = combinedCookies,
                                                visitorData = visitorData,
                                                dataSyncId = dataSyncId
                                            ),
                                            makePreferred = true
                                        )
                                        baselineCookie = savedSession.cookie
                                        baselineDataSyncId = savedSession.dataSyncId
                                        onLogin(savedSession)
                                    } finally {
                                        isLoading = false
                                        hasLoggedIn = true
                                    }
                                }
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

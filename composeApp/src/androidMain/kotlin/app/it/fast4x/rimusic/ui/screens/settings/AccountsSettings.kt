package app.it.fast4x.rimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.ktor.http.Url
import app.it.fast4x.compose.persist.persistList
import it.fast4x.innertube.utils.parseCookieString
import it.fast4x.piped.Piped
import it.fast4x.piped.models.Instance
import it.fast4x.piped.models.Session
import app.it.fast4x.rimusic.appContext
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.extensions.discord.DiscordLoginAndGetToken
import app.it.fast4x.rimusic.extensions.discord.DiscordPresenceManager
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeLogin
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmLinkedAccount
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.extensions.youtubelogin.YoutubeSession
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.DefaultDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.Menu
import app.it.fast4x.rimusic.ui.components.themed.MenuEntry

import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.discordPersonalAccessTokenKey
import app.it.fast4x.rimusic.utils.enableYouTubeLoginKey
import app.it.fast4x.rimusic.utils.isAtLeastAndroid7
import app.it.fast4x.rimusic.utils.isAtLeastAndroid81
import app.it.fast4x.rimusic.utils.isDiscordPresenceEnabledKey
import app.it.fast4x.rimusic.utils.isPipedCustomEnabledKey
import app.it.fast4x.rimusic.utils.isPipedEnabledKey
import app.it.fast4x.rimusic.utils.pipedApiBaseUrlKey
import app.it.fast4x.rimusic.utils.pipedApiTokenKey
import app.it.fast4x.rimusic.utils.pipedInstanceNameKey
import app.it.fast4x.rimusic.utils.pipedPasswordKey
import app.it.fast4x.rimusic.utils.pipedUsernameKey
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.rememberEncryptedPreference
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.restartActivityKey
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey
import app.it.fast4x.rimusic.utils.syncSelectedYtmAccountData
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytAccountEmailKey
import app.it.fast4x.rimusic.utils.ytAccountNameKey
import app.it.fast4x.rimusic.utils.ytAccountThumbnailKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.ytDataSyncIdKey
import app.it.fast4x.rimusic.utils.ytVisitorDataKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.kreate.android.R
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.IconButton

import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import app.it.fast4x.rimusic.typography
import app.kreate.android.me.knighthat.component.dialog.RestartAppDialog

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun AccountsSettings() {
    val context = LocalContext.current
    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    val scope = rememberCoroutineScope()

    var restartActivity by rememberPreference(restartActivityKey, false)
    var restartService by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
            .verticalScroll(rememberScrollState())
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.tab_accounts),
            iconId = R.drawable.person,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.accounts_settings_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 

        Spacer(modifier = Modifier.height(16.dp))

// YouTube Music Section
AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = tween(600)) + scaleIn(
        animationSpec = tween(600),
        initialScale = 0.9f
    )
) {
    SettingsSectionCard(
        title = "YOUTUBE MUSIC",
        icon = R.drawable.ytmusic,
         content = {
        // ⚠️ Warning message
        Text(
            text = "Connect once to load your YouTube Music account details, saved sessions, and sync options. Raw cookies are kept out of this screen.",
            color = colorPalette().textSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

            var isYouTubeLoginEnabled by rememberPreference(enableYouTubeLoginKey, false)
            var loginYouTube by remember { mutableStateOf(false) }
            var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
            var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = "")
            var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
            var storedAccountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
            var storedAccountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
            var storedAccountChannelHandle by rememberPreference(key = ytAccountChannelHandleKey, defaultValue = "")
            var storedAccountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")
            var isRefreshingAccountInfo by remember { mutableStateOf(false) }
            var savedSessions by remember { mutableStateOf(YouTubeSessionStore.getSessions(context)) }
            var linkedAccounts by remember { mutableStateOf(emptyList<YtmLinkedAccount>()) }

            var accountName by remember { mutableStateOf(storedAccountName) }
            var accountEmail by remember { mutableStateOf(storedAccountEmail) }
            var accountChannelHandle by remember { mutableStateOf(storedAccountChannelHandle) }
            var accountThumbnail by remember { mutableStateOf(storedAccountThumbnail) }
            var currentSessionId by remember {
                mutableStateOf(YouTubeSessionStore.getCurrentSession(context)?.sessionId.orEmpty())
            }
            
            var isLoggedIn = remember(cookie) {
                YouTubeSessionStore.hasAuthCookies(cookie)
            }

            fun applySession(session: YoutubeSession?) {
                cookie = session?.cookie.orEmpty()
                visitorData = session?.visitorData.orEmpty()
                dataSyncId = session?.dataSyncId.orEmpty()
                accountName = session?.accountName.orEmpty()
                accountEmail = session?.accountEmail.orEmpty()
                accountChannelHandle = session?.accountChannelHandle.orEmpty()
                accountThumbnail = session?.accountThumbnail.orEmpty()
                storedAccountName = accountName
                storedAccountEmail = accountEmail
                storedAccountChannelHandle = accountChannelHandle
                storedAccountThumbnail = accountThumbnail
                currentSessionId = session?.sessionId.orEmpty()
                savedSessions = YouTubeSessionStore.getSessions(context)
            }

            fun clearAccountScopedUiCaches() {
                appContext().preferences.edit()
                    .putString("quickPicsHomePageSessionId", "")
                    .putString("quickPicsHomePageChipTitle", "")
                    .putString("quickPicsHomePageChipParams", "")
                    .apply()
            }

            suspend fun refreshSessionFromApi(showToast: Boolean = false): Boolean {
                if (!YouTubeSessionStore.hasAuthCookies(cookie)) {
                    linkedAccounts = emptyList()
                    if (showToast) Toaster.i("Connect YouTube Music first")
                    return false
                }
                isRefreshingAccountInfo = true
                return try {
                    val accountInfo = YtmSessionApi.fetchAccountInfo(cookie).getOrThrow()
                    val accounts = YtmSessionApi.listAccounts(cookie).getOrDefault(emptyList())
                    val selectedAccount = accounts.firstOrNull { it.isSelected }

                    applySession(
                        YouTubeSessionStore.saveSession(
                            context = context,
                            session = YoutubeSession(
                                cookie = cookie,
                                visitorData = visitorData,
                                dataSyncId = dataSyncId,
                                authUser = selectedAccount?.authUser.orEmpty(),
                                pageId = selectedAccount?.pageId.orEmpty(),
                                accountName = accountInfo.accountName,
                                accountEmail = accountInfo.accountEmail,
                                accountChannelHandle = accountInfo.accountChannelHandle,
                                accountThumbnail = accountInfo.accountThumbnail,
                                lastAccountRefreshAt = if (accountInfo.hasSession) {
                                    System.currentTimeMillis()
                                } else {
                                    0L
                                }
                            ),
                            makePreferred = true
                        )
                    )

                    linkedAccounts = accounts
                    if (showToast) Toaster.i("YouTube Music account refreshed")
                    true
                } catch (e: Exception) {
                    if (showToast) Toaster.e(e.message ?: "Failed to query YouTube Music account")
                    false
                } finally {
                    isRefreshingAccountInfo = false
                }
            }

            suspend fun switchLinkedAccount(linkedAccount: YtmLinkedAccount) {
                if (!YouTubeSessionStore.hasAuthCookies(cookie)) {
                    Toaster.i("Connect YouTube Music first")
                    return
                }

                isRefreshingAccountInfo = true
                runCatching {
                    val switched = YtmSessionApi.switchAccount(
                        cookies = cookie,
                        authUser = linkedAccount.authUser,
                        pageId = linkedAccount.pageId.ifBlank { null }
                    ).getOrThrow()

                    val nextSession = YouTubeSessionStore.saveSession(
                        context = context,
                        session = YoutubeSession(
                            cookie = switched.cookie.ifBlank { cookie },
                            visitorData = switched.visitorData.ifBlank { visitorData },
                            dataSyncId = switched.dataSyncId.ifBlank { dataSyncId },
                            authUser = switched.authUser.ifBlank { linkedAccount.authUser },
                            pageId = switched.pageId.ifBlank { linkedAccount.pageId },
                            accountName = switched.accountName.ifBlank { linkedAccount.accountName },
                            accountEmail = switched.accountEmail.ifBlank { linkedAccount.accountEmail },
                            accountChannelHandle = switched.accountChannelHandle.ifBlank { linkedAccount.channelHandle },
                            accountThumbnail = switched.accountThumbnail.ifBlank { linkedAccount.accountThumbnail },
                            lastAccountRefreshAt = System.currentTimeMillis()
                        ),
                        makePreferred = true
                    )

                    applySession(nextSession)
                    clearAccountScopedUiCaches()
                    linkedAccounts = YtmSessionApi.listAccounts(nextSession.cookie).getOrDefault(emptyList())
                    Toaster.i("${linkedAccount.accountName.ifBlank { "YouTube account" }} is now active")
                    scope.launch(Dispatchers.IO) {
                        syncSelectedYtmAccountData()
                    }
                    restartService = true
                }.onFailure {
                    Timber.w(it, "AccountsSettings: switch_account failed")
                    Toaster.e(it.message ?: "Failed to switch YouTube Music account")
                }
                isRefreshingAccountInfo = false
            }

            LaunchedEffect(Unit) {
                applySession(YouTubeSessionStore.applyCurrentSession(context))
            }

            LaunchedEffect(
                storedAccountName,
                storedAccountEmail,
                storedAccountChannelHandle,
                storedAccountThumbnail
            ) {
                val session = YouTubeSessionStore.getCurrentSession(context)
                if (session == null) {
                    accountName = storedAccountName
                    accountEmail = storedAccountEmail
                    accountChannelHandle = storedAccountChannelHandle
                    accountThumbnail = storedAccountThumbnail
                } else {
                    applySession(session)
                }
            }

            // Auto-fetch account info when logged in
            LaunchedEffect(isLoggedIn) {
                val currentSession = YouTubeSessionStore.applyCurrentSession(context)
                applySession(currentSession)
                if (isLoggedIn && YouTubeSessionStore.shouldRefreshAccount(currentSession)) {
                    scope.launch {
                        refreshSessionFromApi(showToast = false)
                    }
                } else if (isLoggedIn && linkedAccounts.isEmpty()) {
                    scope.launch {
                        linkedAccounts = YtmSessionApi.listAccounts(cookie).getOrDefault(emptyList())
                    }
                } else if (!isLoggedIn) {
                    linkedAccounts = emptyList()
                }
            }


                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_youtube_music_login),
                        text = "",
                        isChecked = isYouTubeLoginEnabled,
                        onCheckedChange = {
                            isYouTubeLoginEnabled = it
                            if (!it) {
                                YouTubeSessionStore.clearAllSessions(context)
                                visitorData = ""
                                dataSyncId = ""
                                cookie = ""
                                accountName = ""
                                accountChannelHandle = ""
                                accountEmail = ""
                                accountThumbnail = ""
                                savedSessions = emptyList()
                                currentSessionId = ""
                            }
                        },
                        icon = R.drawable.ytmusic
                    )

                    AnimatedVisibility(visible = isYouTubeLoginEnabled) {
                        Column {
                            OtherSettingsEntry(
                                title = if (isLoggedIn) "Query account details from API" else stringResource(R.string.youtube_connect),
                                text = if (isLoggedIn) {
                                    "Use the captured YouTube Music cookies to fetch your active account and linked accounts"
                                } else {
                                    "Log in once with the YouTube Music WebView to capture your session cookies"
                                },
                                icon = R.drawable.person,
                                onClick = {
                                    if (isLoggedIn) {
                                        scope.launch {
                                            refreshSessionFromApi(showToast = true)
                                        }
                                    } else {
                                        loginYouTube = true
                                    }
                                }
                            )

                            // Display account info card when logged in
                            if (isLoggedIn) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorPalette().background1,
                                        contentColor = colorPalette().text
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Profile picture
                                            if (accountThumbnail.isNotEmpty()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(accountThumbnail)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Profile picture",
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else if (accountName.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(colorPalette().accent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = accountName.take(2).uppercase(),
                                                        style = typography().s.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                        color = colorPalette().text
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(colorPalette().accent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.person),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = colorPalette().text
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.size(12.dp))

                                            // Account details
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = accountName.ifEmpty { "YouTube Account" },
                                                    style = typography().s.copy(fontWeight = FontWeight.Bold),
                                                    color = colorPalette().text,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                if (accountEmail.isNotEmpty()) {
                                                    Text(
                                                        text = accountEmail,
                                                        style = typography().xs,
                                                        color = colorPalette().textSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (accountChannelHandle.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ytmusic),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = colorPalette().textSecondary
                                                        )
                                                        Spacer(modifier = Modifier.size(4.dp))
                                                        Text(
                                                            text = accountChannelHandle,
                                                            style = typography().xxs,
                                                            color = colorPalette().textSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            // Refresh button
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        refreshSessionFromApi(showToast = true)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp),
                                                enabled = !isRefreshingAccountInfo
                                            ) {
                                                if (isRefreshingAccountInfo) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.refresh),
                                                        contentDescription = "Refresh",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = colorPalette().textSecondary
                                                    )
                                                }
                                            }
                                        }

                                        // Refresh indicator
                                        if (isRefreshingAccountInfo) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Refreshing account info...",
                                                style = typography().xxs,
                                                color = colorPalette().textDisabled
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Session connected securely on this device",
                                            style = typography().xxs,
                                            color = colorPalette().textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            linkedAccounts.forEach { linkedAccount ->
                                    OtherSettingsEntry(
                                        title = buildString {
                                            append(linkedAccount.accountName.ifBlank { "YouTube account" })
                                            if (linkedAccount.isSelected) append(" (active)")
                                        },
                                        text = listOf(
                                            linkedAccount.accountEmail,
                                            linkedAccount.channelHandle,
                                            linkedAccount.subscribers
                                        )
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • ")
                                            .ifBlank {
                                                if (linkedAccount.pageId.isNotBlank()) {
                                                    "Brand account available in this session"
                                                } else {
                                                    "Linked account available in this session"
                                                }
                                            },
                                        icon = R.drawable.person,
                                        onClick = {
                                            if (!linkedAccount.isSelected) {
                                                scope.launch {
                                                    switchLinkedAccount(linkedAccount)
                                                }
                                            }
                                        }
                                    )
                                }

                            savedSessions
                                .filter { it.sessionId != currentSessionId }
                                .forEach { savedSession ->
                                    OtherSettingsEntry(
                                        title = "Reuse ${savedSession.accountName.ifBlank { "saved session" }}",
                                        text = listOf(savedSession.accountEmail, savedSession.accountChannelHandle)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • "),
                                        icon = R.drawable.person,
                                        onClick = {
                                            applySession(
                                                YouTubeSessionStore.switchToSession(
                                                    context = context,
                                                    sessionId = savedSession.sessionId
                                                )
                                            )
                                            Toaster.i("Reused saved YouTube session")
                                        }
                                    )
                                }

                            // Login/Logout button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isLoggedIn && accountThumbnail != "")
                                    ImageCacheFactory.AsyncImage(
                                        thumbnailUrl = accountThumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )

                                Column {
                                    OtherSettingsEntry(
                                        title = if (isLoggedIn) "Reconnect YouTube Music" else stringResource(R.string.youtube_connect),
                                        text = "Use the YouTube web login once, then reuse that saved session everywhere in the app",
                                        icon = R.drawable.person,
                                        onClick = {
                                            loginYouTube = true
                                        }
                                    )

                                    if (isLoggedIn) {
                                        OtherSettingsEntry(
                                            title = stringResource(R.string.youtube_disconnect),
                                            text = "Disconnect the current session and stop reusing it until you choose a saved one",
                                            icon = R.drawable.logout,
                                            onClick = {
                                                YouTubeSessionStore.clearCurrentSession(context, removeStored = true)
                                                applySession(null)
                                                loginYouTube = false
                                                val cookieManager = CookieManager.getInstance()
                                                cookieManager.removeAllCookies(null)
                                                cookieManager.flush()
                                                WebStorage.getInstance().deleteAllData()
                                                linkedAccounts = emptyList()
                                                restartService = true
                                            }
                                        )

                                        OtherSettingsEntry(
                                            title = "Clear all saved YouTube sessions",
                                            text = "Wipe all stored cookies, saved sessions, and current YouTube login data",
                                            icon = R.drawable.trash,
                                            onClick = {
                                                YouTubeSessionStore.clearAllSessions(context)
                                                applySession(null)
                                                loginYouTube = false
                                                val cookieManager = CookieManager.getInstance()
                                                cookieManager.removeAllCookies(null)
                                                cookieManager.flush()
                                                WebStorage.getInstance().deleteAllData()
                                                savedSessions = emptyList()
                                                linkedAccounts = emptyList()
                                                currentSessionId = ""
                                                restartService = true
                                                Toaster.i("Cleared all saved YouTube sessions")
                                            }
                                        )
                                    }

                                    CustomModalBottomSheet(
                                        showSheet = loginYouTube,
                                        onDismissRequest = {
                                            loginYouTube = false
                                        },
                                        containerColor = colorPalette().background0,
                                        contentColor = colorPalette().background0,
                                        modifier = Modifier.fillMaxWidth(),
                                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                        dragHandle = {
                                            Surface(
                                                modifier = Modifier.padding(vertical = 0.dp),
                                                color = colorPalette().background0,
                                                shape = thumbnailShape()
                                            ) {}
                                        },
                                        shape = thumbnailRoundness.shape
                                    ) {
                                        YouTubeLogin(
                                            autoCompleteExistingSession = !isLoggedIn,
                                            onLogin = { session ->
                                                if (YouTubeSessionStore.hasAuthCookies(session.cookie)) {
                                                    applySession(session)
                                                    scope.launch {
                                                        linkedAccounts = YtmSessionApi.listAccounts(session.cookie)
                                                            .getOrDefault(emptyList())
                                                    }
                                                    loginYouTube = false
                                                    Toaster.i( context.getString(R.string.youtube_login_successful) )
                                                    restartService = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Piped Section
        if (isAtLeastAndroid7) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                    animationSpec = tween(800),
                    initialScale = 0.9f
                )
            ) {
                SettingsSectionCard(
                    title = stringResource(R.string.piped_account),
                    icon = R.drawable.piped_logo,
                    content = {
                        // rememberEncryptedPreference only works correct with API 24 and up
                        var isPipedEnabled by rememberPreference(isPipedEnabledKey, false)
                        var isPipedCustomEnabled by rememberPreference(isPipedCustomEnabledKey, false)
                        var pipedUsername by rememberEncryptedPreference(pipedUsernameKey, "")
                        var pipedPassword by rememberEncryptedPreference(pipedPasswordKey, "")
                        var pipedInstanceName by rememberEncryptedPreference(pipedInstanceNameKey, "")
                        var pipedApiBaseUrl by rememberEncryptedPreference(pipedApiBaseUrlKey, "")
                        var pipedApiToken by rememberEncryptedPreference(pipedApiTokenKey, "")

                        var loadInstances by remember { mutableStateOf(false) }
                        var isLoading by remember { mutableStateOf(false) }
                        var instances by persistList<Instance>(tag = "otherSettings/pipedInstances")
                        var noInstances by remember { mutableStateOf(false) }
                        var executeLogin by remember { mutableStateOf(false) }
                        var showInstances by remember { mutableStateOf(false) }
                        var session by remember {
                            mutableStateOf<Result<Session>?>(
                                null
                            )
                        }

                        val menuState = LocalMenuState.current
                        val coroutineScope = rememberCoroutineScope()

                        if (isLoading)
                            DefaultDialog(
                                onDismiss = {
                                    isLoading = false
                                }
                            ) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            }

                        if (loadInstances) {
                            LaunchedEffect(Unit) {
                                isLoading = true
                                Piped.getInstances()?.getOrNull()?.let {
                                    instances = it
                                } ?: run { noInstances = true }
                                isLoading = false
                                showInstances = true
                            }
                        }
                        if (noInstances)
                            Toaster.i( context.getString(R.string.no_instances_found) )

                        if (executeLogin) {
                            LaunchedEffect(Unit) {
                                coroutineScope.launch {
                                    isLoading = true
                                    session = Piped.login(
                                        apiBaseUrl = Url(pipedApiBaseUrl),
                                        username = pipedUsername,
                                        password = pipedPassword
                                    )?.onFailure {
                                        Timber.e("Failed piped login ${it.stackTraceToString()}")
                                        isLoading = false
                                        Toaster.e( context.getString(R.string.piped_login_failed) )
                                        loadInstances = false
                                        session = null
                                        executeLogin = false
                                    }
                                    if (session?.isSuccess == false)
                                        return@launch

                                    Toaster.s( context.getString(R.string.piped_login_successful) )
                                    Timber.i("Piped login successful")

                                    session.let {
                                        it?.getOrNull()?.token?.let { it1 ->
                                            pipedApiToken = it1
                                            pipedApiBaseUrl = it.getOrNull()!!.apiBaseUrl.toString()
                                        }
                                    }

                                    isLoading = false
                                    loadInstances = false
                                    executeLogin = false
                                }
                            }
                        }

                        if (showInstances && instances.isNotEmpty()) {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.chevron_back,
                                        text = stringResource(R.string.cancel),
                                        onClick = {
                                            loadInstances = false
                                            showInstances = false
                                            menuState.hide()
                                        }
                                    )
                                    instances.forEach {
                                        MenuEntry(
                                            icon = R.drawable.server,
                                            text = it.name,
                                            secondaryText = "${it.locationsFormatted} Users: ${it.userCount}",
                                            onClick = {
                                                menuState.hide()
                                                pipedApiBaseUrl = it.apiBaseUrl.toString()
                                                pipedInstanceName = it.name
                                                loadInstances = false
                                                showInstances = false
                                            }
                                        )
                                    }
                                    MenuEntry(
                                        icon = R.drawable.chevron_back,
                                        text = stringResource(R.string.cancel),
                                        onClick = {
                                            loadInstances = false
                                            showInstances = false
                                            menuState.hide()
                                        }
                                    )
                                }
                            }
                        }

                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.enable_piped_syncronization),
                            text = "",
                            isChecked = isPipedEnabled,
                            onCheckedChange = { isPipedEnabled = it },
                            icon = R.drawable.piped_logo
                        )

                        AnimatedVisibility(visible = isPipedEnabled) {
                            Column {
                                OtherSwitchSettingEntry(
                                    title = stringResource(R.string.piped_custom_instance),
                                    text = "",
                                    isChecked = isPipedCustomEnabled,
                                    onCheckedChange = { isPipedCustomEnabled = it },
                                    icon = R.drawable.server
                                )
                                
                                AnimatedVisibility(visible = isPipedCustomEnabled) {
                                    Column {
                                        var showCustomInstanceDialog by remember { mutableStateOf(false) }
                                        OtherSettingsEntry(
                                            title = stringResource(R.string.piped_custom_instance),
                                            text = pipedApiBaseUrl,
                                            icon = R.drawable.server,
                                            onClick = { showCustomInstanceDialog = true }
                                        )
                                        
                                        if (showCustomInstanceDialog) {
                                            app.it.fast4x.rimusic.ui.components.themed.InputTextDialog(
                                                title = stringResource(R.string.piped_custom_instance),
                                                value = pipedApiBaseUrl,
                                                placeholder = stringResource(R.string.piped_custom_instance),
                                                onDismiss = { showCustomInstanceDialog = false },
                                                setValue = { pipedApiBaseUrl = it }
                                            )
                                        }
                                    }
                                }
                                
                                AnimatedVisibility(visible = !isPipedCustomEnabled) {
                                    OtherSettingsEntry(
                                        title = stringResource(R.string.piped_change_instance),
                                        text = pipedInstanceName,
                                        icon = R.drawable.open,
                                        onClick = {
                                            loadInstances = true
                                        }
                                    )
                                }

                                var showUsernameDialog by remember { mutableStateOf(false) }
                                OtherSettingsEntry(
                                    title = stringResource(R.string.piped_username),
                                    text = pipedUsername,
                                    icon = R.drawable.person,
                                    onClick = { showUsernameDialog = true }
                                )
                                
                                if (showUsernameDialog) {
                                    app.it.fast4x.rimusic.ui.components.themed.InputTextDialog(
                                        title = stringResource(R.string.piped_username),
                                        value = pipedUsername,
                                        placeholder = stringResource(R.string.piped_username),
                                        onDismiss = { showUsernameDialog = false },
                                        setValue = { pipedUsername = it }
                                    )
                                }

                                var showPasswordDialog by remember { mutableStateOf(false) }
                                OtherSettingsEntry(
                                    title = stringResource(R.string.piped_password),
                                    text = if (pipedPassword.isNotEmpty()) "********" else "",
                                    icon = R.drawable.locked,
                                    onClick = { showPasswordDialog = true },
                                    modifier = Modifier.semantics { password() }
                                )
                                
                                if (showPasswordDialog) {
                                    app.it.fast4x.rimusic.ui.components.themed.InputTextDialog(
                                        title = stringResource(R.string.piped_password),
                                        value = pipedPassword,
                                        placeholder = stringResource(R.string.piped_password),
                                        onDismiss = { showPasswordDialog = false },
                                        setValue = { pipedPassword = it }
                                    )
                                }

                                OtherSettingsEntry(
                                    title = if (pipedApiToken.isNotEmpty()) stringResource(R.string.piped_disconnect) else stringResource(R.string.piped_connect),
                                    text = if (pipedApiToken.isNotEmpty()) stringResource(R.string.piped_connected_to_s).format(pipedInstanceName) else "",
                                    icon = R.drawable.piped_logo,
                                    onClick = {
                                        if (pipedApiToken.isNotEmpty()) {
                                            pipedApiToken = ""
                                            executeLogin = false
                                        } else executeLogin = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discord Section
        if (isAtLeastAndroid7) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                    animationSpec = tween(1000),
                    initialScale = 0.9f
                )
            ) {
                SettingsSectionCard(
                    title = stringResource(R.string.social_discord) + " " + stringResource(R.string.beta_title),
                    icon = R.drawable.logo_discord,
                    content = {
                        // rememberEncryptedPreference only works correct with API 24 and up
                        var isDiscordPresenceEnabled by rememberPreference(isDiscordPresenceEnabledKey, false)
                        var loginDiscord by remember { mutableStateOf(false) }
                        var discordPersonalAccessToken by rememberEncryptedPreference(
                            key = discordPersonalAccessTokenKey,
                            defaultValue = ""
                        )
                        var discordAvatar by rememberEncryptedPreference(
                            key = "discord_avatar",
                            defaultValue = ""
                        )
                        var discordUsername by rememberEncryptedPreference(
                            key = "discord_username",
                            defaultValue = ""
                        )
                        var isTokenValid by remember { mutableStateOf(true) }
                        var showTokenError by remember { mutableStateOf(false) }

                        LaunchedEffect(discordPersonalAccessToken) {
                            if (discordPersonalAccessToken.isNotEmpty()) {
                                val presenceManager = DiscordPresenceManager(context, { discordPersonalAccessToken })
                                when (presenceManager.validateToken(discordPersonalAccessToken)) {
                                    true -> {
                                        isTokenValid = true
                                        showTokenError = false
                                    }
                                    false -> {
                                        isTokenValid = false
                                        showTokenError = true
                                        discordPersonalAccessToken = ""
                                        discordUsername = ""
                                        discordAvatar = ""
                                        Toaster.e(R.string.discord_token_text_invalid)
                                    }
                                    null -> { // Network error
                                        isTokenValid = false
                                        showTokenError = false
                                    }
                                }
                            }
                        }

                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.discord_enable_rich_presence),
                            text = stringResource(R.string.beta_text),
                            isChecked = isDiscordPresenceEnabled,
                            onCheckedChange = { 
                                isDiscordPresenceEnabled = it
                                if (!it) {
                                    RestartAppDialog.showDialog()
                                }
                            },
                            icon = R.drawable.musical_notes
                        )

                        AnimatedVisibility(visible = isDiscordPresenceEnabled) {
                            Column {
                                if (showTokenError) {
                                    Text(
                                        text = stringResource(R.string.discord_token_text_invalid),
                                        color = colorPalette().red,
                                        style = typography().s,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                if (discordPersonalAccessToken.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.account_info),
                                                color = colorPalette().text,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 5.dp),
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (discordAvatar.isNotEmpty()) {
                                                    ImageCacheFactory.AsyncImage(
                                                        thumbnailUrl = discordAvatar,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .padding(start = 5.dp, top = 8.dp, bottom = 8.dp)
                                                            .size(50.dp)
                                                            .clip(thumbnailShape())
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.person),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .padding(start = 5.dp, top = 8.dp, bottom = 8.dp)
                                                            .size(50.dp)
                                                            .clip(thumbnailShape()),
                                                        tint = colorPalette().textSecondary
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 8.dp)
                                                        .height(50.dp)
                                                        .padding(top = 8.dp, bottom = 8.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = discordUsername,
                                                        color = colorPalette().textSecondary,
                                                        modifier = Modifier.padding(start = 5.dp),
                                                        style = typography().m
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                OtherSettingsEntry(
                                    title = if (discordPersonalAccessToken.isNotEmpty()) stringResource(R.string.discord_disconnect) else stringResource(R.string.discord_connect),
                                    text = if (discordPersonalAccessToken.isNotEmpty()) stringResource(R.string.discord_connected_to_discord_account) else "",
                                    icon = R.drawable.logout,
                                    onClick = {
                                        if (discordPersonalAccessToken.isNotEmpty()) {
                                            discordPersonalAccessToken = ""
                                            discordUsername = ""
                                            discordAvatar = ""
                                            showTokenError = false
                                            RestartAppDialog.showDialog()
                                        } else
                                            loginDiscord = true
                                    }
                                )

                                CustomModalBottomSheet(
                                    showSheet = loginDiscord,
                                    onDismissRequest = {
                                        loginDiscord = false
                                    },
                                    containerColor = colorPalette().background0,
                                    contentColor = colorPalette().background0,
                                    modifier = Modifier.fillMaxWidth(),
                                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                    dragHandle = {
                                        Surface(
                                            modifier = Modifier.padding(vertical = 0.dp),
                                            color = colorPalette().background0,
                                            shape = thumbnailShape()
                                        ) {}
                                    },
                                    shape = thumbnailRoundness.shape
                                ) {
                                    DiscordLoginAndGetToken(
                                        navController = rememberNavController(),
                                        onGetToken = { token, username, avatar ->
                                            loginDiscord = false
                                            discordPersonalAccessToken = token
                                            discordUsername = username
                                            discordAvatar = avatar
                                            Toaster.i(context.getString(R.string.discord_connected_to_discord_account))
                                            RestartAppDialog.showDialog()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}

fun isYouTubeLoginEnabled(): Boolean {
    val isYouTubeLoginEnabled = appContext().preferences.getBoolean(enableYouTubeLoginKey, false)
    return isYouTubeLoginEnabled
}

fun isYouTubeSyncEnabled(): Boolean {
    return isYouTubeLoggedIn() && isYouTubeLoginEnabled()
}

fun isYouTubeLoggedIn(): Boolean {
    val session = YouTubeSessionStore.applyCurrentSession()
    return YouTubeSessionStore.hasAuthCookies(session?.cookie)
}

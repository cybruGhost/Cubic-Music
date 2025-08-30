package app.kreate.android.themed.common.screens.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import app.kreate.android.themed.common.component.settings.section
import coil.compose.AsyncImage
import io.ktor.http.Url
import it.fast4x.compose.persist.persistList
import it.fast4x.innertube.utils.parseCookieString
import it.fast4x.piped.Piped
import it.fast4x.piped.models.Instance
import it.fast4x.piped.models.Session
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.rimusic.extensions.youtubelogin.YouTubeLogin
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.DefaultDialog
import it.fast4x.rimusic.ui.components.themed.Menu
import it.fast4x.rimusic.ui.components.themed.MenuEntry
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.RestartPlayerService
import it.fast4x.rimusic.utils.isAtLeastAndroid7
import it.fast4x.rimusic.utils.isAtLeastAndroid81
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.dialog.InputDialogConstraints
import me.knighthat.utils.Toaster
import timber.log.Timber

@ExperimentalMaterial3Api
@Composable
fun AccountSettings(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val scrollState = rememberLazyListState()

    val search = remember {
        SettingEntrySearch(scrollState, R.string.tab_accounts, R.drawable.person)
    }
    val (restartService, onRestartServiceChange) = rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(colorPalette().background0)
            .padding(paddingValues)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        search.ToolBarButton()

        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
        ) {
            section("YOUTUBE MUSIC") {
                var visitorData by remember { mutableStateOf(Preferences.YOUTUBE_VISITOR_DATA.value) }
                var dataSyncId by remember { mutableStateOf(Preferences.YOUTUBE_SYNC_ID.value) }
                var cookie by remember { mutableStateOf(Preferences.YOUTUBE_COOKIES.value) }
                var accountName by remember { mutableStateOf(Preferences.YOUTUBE_ACCOUNT_NAME.value) }
                var accountEmail by remember { mutableStateOf(Preferences.YOUTUBE_ACCOUNT_EMAIL.value) }
                var accountChannelHandle by remember { mutableStateOf(Preferences.YOUTUBE_SELF_CHANNEL_HANDLE.value) }

                if (search appearsIn "YOUTUBE MUSIC")
                    SettingComponents.BooleanEntry(
                        Preferences.YOUTUBE_LOGIN,
                        "Enable YouTube Music Login"
                    ) {
                        if (it) return@BooleanEntry

                        Preferences.YOUTUBE_VISITOR_DATA.reset()
                        Preferences.YOUTUBE_SYNC_ID.reset()
                        Preferences.YOUTUBE_COOKIES.reset()
                        Preferences.YOUTUBE_ACCOUNT_NAME.reset()
                        Preferences.YOUTUBE_ACCOUNT_EMAIL.reset()
                        Preferences.YOUTUBE_SELF_CHANNEL_HANDLE.reset()
                    }

                AnimatedVisibility(Preferences.YOUTUBE_LOGIN.value) {
                    var accountThumbnail by remember { mutableStateOf(Preferences.YOUTUBE_ACCOUNT_AVATAR.value) }
                    var loginYouTube by remember { mutableStateOf(false) }
                    var loginError by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(false) }

                    var isLoggedIn = remember(cookie) {
                        "SAPISID" in parseCookieString(cookie)
                    }

                    Column(Modifier.padding(start = 25.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isLoggedIn && accountThumbnail.isNotEmpty())
                                AsyncImage(
                                    model = accountThumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(50.dp)
                                        .clip(thumbnailShape())
                                )

                            Column {
                                val (title, subtitle) = remember(isLoggedIn, accountName, accountChannelHandle) {
                                    if (isLoggedIn)
                                        "Disconnect" to "$accountName $accountChannelHandle"
                                    else
                                        "Connect" to ""
                                }

                                if (search appearsIn title)
                                    SettingComponents.Text(
                                        title = title,
                                        subtitle = subtitle,
                                        onClick = {
                                            if (isLoggedIn) {
                                                Preferences.YOUTUBE_VISITOR_DATA.reset()
                                                Preferences.YOUTUBE_SYNC_ID.reset()
                                                Preferences.YOUTUBE_COOKIES.reset()
                                                Preferences.YOUTUBE_ACCOUNT_NAME.reset()
                                                Preferences.YOUTUBE_ACCOUNT_EMAIL.reset()
                                                Preferences.YOUTUBE_SELF_CHANNEL_HANDLE.reset()
                                                Preferences.YOUTUBE_ACCOUNT_AVATAR.reset()
                                                loginYouTube = false
                                                // Delete cookies after logout
                                                val cookieManager = CookieManager.getInstance()
                                                cookieManager.removeAllCookies(null)
                                                cookieManager.flush()
                                                WebStorage.getInstance().deleteAllData()
                                                onRestartServiceChange(true)
                                            } else {
                                                loginYouTube = true
                                                loginError = null
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ytmusic),
                                            contentDescription = title,
                                            tint = colorPalette().text,
                                        )
                                    }

                                loginError?.let { error ->
                                    Text(
                                        text = error,
                                        color = Color.Red,
                                        modifier = Modifier.padding(horizontal = 16.dp)
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
                                    shape = Preferences.THUMBNAIL_BORDER_RADIUS.value.shape
                                ) {
                                    YouTubeLogin(
                                        onLogin = { cookieRetrieved ->
                                            if (cookieRetrieved.contains("SAPISID")) {
                                                isLoggedIn = true
                                                loginYouTube = false
                                                Toaster.i("Login successful")
                                                onRestartServiceChange(true)
                                            } else {
                                                loginError = "Login failed - missing required cookies"
                                            }
                                        }
                                    )
                                }

                                var restartActivity by remember { mutableStateOf(Preferences.RESTART_ACTIVITY.value) }
                                RestartPlayerService(restartService, onRestart = {
                                    onRestartServiceChange(false)
                                    restartActivity = !restartActivity
                                })
                            }
                        }

                        if (search appearsIn "Sync data with YTM account")
                            SettingComponents.BooleanEntry(
                                Preferences.YOUTUBE_PLAYLISTS_SYNC,
                                "Sync data with YTM account",
                                subtitle = "Playlists, albums, artists, history, like, etc."
                            )
                    }
                }
            }

            if (isAtLeastAndroid7)
                section(R.string.piped_account) {
                    val menuState = LocalMenuState.current

                    var pipedUsername by remember { mutableStateOf(Preferences.PIPED_USERNAME.value) }
                    var pipedPassword by remember { mutableStateOf(Preferences.PIPED_PASSWORD.value) }
                    var pipedInstanceName by remember { mutableStateOf(Preferences.PIPED_INSTANCE_NAME.value) }
                    var pipedApiBaseUrl by remember { mutableStateOf(Preferences.PIPED_API_BASE_URL.value) }
                    var pipedApiToken by remember { mutableStateOf(Preferences.PIPED_API_TOKEN.value) }

                    var isLoading by remember { mutableStateOf(false) }
                    if (isLoading)
                        DefaultDialog(
                            onDismiss = {
                                isLoading = false
                            }
                        ) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                    var instances by persistList<Instance>(tag = "otherSettings/pipedInstances")
                    var noInstances by remember { mutableStateOf(false) }
                    var loadInstances by remember { mutableStateOf(false) }
                    var showInstances by remember { mutableStateOf(false) }
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
                        Toaster.i("No instances found")

                    var executeLogin by remember { mutableStateOf(false) }
                    var session by remember {
                        mutableStateOf<Result<Session>?>(null)
                    }
                    if (executeLogin) {
                        LaunchedEffect(Unit) {
                            CoroutineScope(Dispatchers.IO).launch {
                                isLoading = true
                                session = Piped.login(
                                    apiBaseUrl = Url(pipedApiBaseUrl),
                                    username = pipedUsername,
                                    password = pipedPassword
                                )?.onFailure {
                                    Timber.e("Failed piped login ${it.stackTraceToString()}")
                                    isLoading = false
                                    Toaster.e("Piped login failed")
                                    loadInstances = false
                                    session = null
                                    executeLogin = false
                                }
                                if (session?.isSuccess == false)
                                    return@launch

                                Toaster.s("Piped login successful")
                                Timber.i("Piped login successful")

                                session.let {
                                    it?.getOrNull()?.token?.let { token ->
                                        pipedApiToken = token
                                        Preferences.PIPED_API_TOKEN.value = token
                                        pipedApiBaseUrl = it.getOrNull()!!.apiBaseUrl.toString()
                                        Preferences.PIPED_API_BASE_URL.value = pipedApiBaseUrl
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
                                            Preferences.PIPED_API_BASE_URL.value = pipedApiBaseUrl
                                            pipedInstanceName = it.name
                                            Preferences.PIPED_INSTANCE_NAME.value = pipedInstanceName
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

                    if (search appearsIn R.string.enable_piped_syncronization)
                        SettingComponents.BooleanEntry(
                            Preferences.ENABLE_PIPED,
                            R.string.enable_piped_syncronization
                        )

                    AnimatedVisibility(Preferences.ENABLE_PIPED.value) {
                        Column(
                            modifier = Modifier.padding(start = 25.dp)
                        ) {
                            if (search appearsIn R.string.piped_custom_instance)
                                SettingComponents.BooleanEntry(
                                    Preferences.IS_CUSTOM_PIPED,
                                    R.string.piped_custom_instance
                                )

                            var isPipedCustomEnabled by remember { mutableStateOf(Preferences.IS_CUSTOM_PIPED.value) }
                            AnimatedVisibility(visible = isPipedCustomEnabled) {
                                Column {
                                    if (search appearsIn R.string.piped_custom_instance)
                                        SettingComponents.InputDialogEntry(
                                            Preferences.PIPED_API_BASE_URL,
                                            R.string.piped_custom_instance,
                                            InputDialogConstraints.URL
                                        )
                                }
                            }
                            AnimatedVisibility(visible = !isPipedCustomEnabled) {
                                Column {
                                    if (search appearsIn R.string.piped_change_instance)
                                        SettingComponents.Text(
                                            title = stringResource(R.string.piped_change_instance),
                                            subtitle = pipedInstanceName,
                                            onClick = { loadInstances = true }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.open),
                                                contentDescription = null,
                                                tint = colorPalette().text
                                            )
                                        }
                                }
                            }

                            if (search appearsIn R.string.piped_username)
                                SettingComponents.InputDialogEntry(
                                    Preferences.PIPED_USERNAME,
                                    R.string.piped_username,
                                    InputDialogConstraints.ALL
                                )
                            if (search appearsIn R.string.piped_password)
                                SettingComponents.InputDialogEntry(
                                    Preferences.PIPED_PASSWORD,
                                    R.string.piped_password,
                                    InputDialogConstraints.ALL,
                                    keyboardOption = KeyboardOptions(keyboardType = KeyboardType.Password)
                                )

                            val (titleId, subtitle) = remember {
                                if (pipedApiToken.isBlank())
                                    R.string.piped_connect to ""
                                else
                                    R.string.piped_disconnect to context.getString(R.string.piped_connected_to_s, pipedInstanceName)
                            }
                            if (search appearsIn titleId)
                                SettingComponents.Text(
                                    title = stringResource(titleId),
                                    subtitle = subtitle,
                                    isEnabled = pipedPassword.isNotEmpty() && pipedUsername.isNotEmpty() && pipedApiBaseUrl.isNotEmpty(),
                                    onClick = {
                                        if (pipedApiToken.isNotEmpty()) {
                                            pipedApiToken = ""
                                            Preferences.PIPED_API_TOKEN.value = ""
                                            executeLogin = false
                                        } else
                                            executeLogin = true
                                    }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.piped_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                        }
                    }
                }

            if (isAtLeastAndroid81)
                section(R.string.social_discord) {
                    if (search appearsIn R.string.discord_enable_rich_presence)
                        SettingComponents.BooleanEntry(
                            Preferences.DISCORD_LOGIN,
                            R.string.discord_enable_rich_presence
                        )

                    AnimatedVisibility(Preferences.DISCORD_LOGIN.value) {
                        var loginDiscord by remember { mutableStateOf(false) }
                        var discordPersonalAccessToken by remember { mutableStateOf(Preferences.DISCORD_ACCESS_TOKEN.value) }

                        val (titleId, subtitle) = remember {
                            if (discordPersonalAccessToken.isBlank())
                                R.string.discord_connect to ""
                            else
                                R.string.discord_disconnect to context.getString(R.string.discord_connected_to_discord_account)
                        }

                        Column {
                            if (search appearsIn titleId)
                                SettingComponents.Text(
                                    title = stringResource(titleId),
                                    subtitle = subtitle,
                                    onClick = {
                                        if (discordPersonalAccessToken.isNotEmpty()) {
                                            discordPersonalAccessToken = ""
                                            Preferences.DISCORD_ACCESS_TOKEN.value = ""
                                        } else
                                            loginDiscord = true
                                    }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.piped_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

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
                                shape = Preferences.THUMBNAIL_BORDER_RADIUS.value.shape
                            ) {
                                DiscordLoginAndGetToken(
                                    rememberNavController(),
                                    onGetToken = { token ->
                                        loginDiscord = false
                                        discordPersonalAccessToken = token
                                        Preferences.DISCORD_ACCESS_TOKEN.value = token
                                        Toaster.i(token)
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}
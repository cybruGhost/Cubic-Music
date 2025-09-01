package it.fast4x.rimusic.ui.screens.settings

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import app.kreate.android.R
import io.ktor.http.Url
import it.fast4x.compose.persist.persistList
import it.fast4x.innertube.utils.parseCookieString
import it.fast4x.piped.Piped
import it.fast4x.piped.models.Instance
import it.fast4x.piped.models.Session
import it.fast4x.rimusic.appContext
import me.knighthat.coil.ImageCacheFactory
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.rimusic.extensions.discord.DiscordPresenceManager
import it.fast4x.rimusic.extensions.youtubelogin.YouTubeLogin
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.DefaultDialog
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.Menu
import it.fast4x.rimusic.ui.components.themed.MenuEntry
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.discordPersonalAccessTokenKey
import it.fast4x.rimusic.utils.enableYouTubeLoginKey
import it.fast4x.rimusic.utils.enableYouTubeSyncKey
import it.fast4x.rimusic.utils.isAtLeastAndroid7
import it.fast4x.rimusic.utils.isAtLeastAndroid81
import it.fast4x.rimusic.utils.isDiscordPresenceEnabledKey
import it.fast4x.rimusic.utils.isPipedCustomEnabledKey
import it.fast4x.rimusic.utils.isPipedEnabledKey
import it.fast4x.rimusic.utils.pipedApiBaseUrlKey
import it.fast4x.rimusic.utils.pipedApiTokenKey
import it.fast4x.rimusic.utils.pipedInstanceNameKey
import it.fast4x.rimusic.utils.pipedPasswordKey
import it.fast4x.rimusic.utils.pipedUsernameKey
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.rememberEncryptedPreference
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.restartActivityKey
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import it.fast4x.rimusic.utils.ytAccountEmailKey
import it.fast4x.rimusic.utils.ytAccountNameKey
import it.fast4x.rimusic.utils.ytAccountThumbnailKey
import it.fast4x.rimusic.utils.ytCookieKey
import it.fast4x.rimusic.utils.ytDataSyncIdKey
import it.fast4x.rimusic.utils.ytVisitorDataKey
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import timber.log.Timber
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import it.fast4x.rimusic.typography
import me.knighthat.component.dialog.RestartAppDialog
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.utils.enablePictureInPictureKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.ytAccountThumbnail

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
                    // rememberEncryptedPreference only works correct with API 24 and up
                    var isYouTubeLoginEnabled by rememberPreference(enableYouTubeLoginKey, false)
                    var isYouTubeSyncEnabled by rememberPreference(enableYouTubeSyncKey, false)
                    var loginYouTube by remember { mutableStateOf(false) }
                    var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = "")
                    var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
                    var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
                    var accountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
                    var accountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
                    var accountChannelHandle by rememberPreference(
                        key = ytAccountChannelHandleKey,
                        defaultValue = ""
                    )
                    var accountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")
                    var isLoggedIn = remember(cookie) {
                        "SAPISID" in parseCookieString(cookie)
                    }

                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_youtube_music_login),
                        text = "",
                        isChecked = isYouTubeLoginEnabled,
                        onCheckedChange = {
                            isYouTubeLoginEnabled = it
                            if (!it) {
                                visitorData = ""
                                dataSyncId = ""
                                cookie = ""
                                accountName = ""
                                accountChannelHandle = ""
                                accountEmail = ""
                            }
                        },
                        icon = R.drawable.ytmusic
                    )

                    AnimatedVisibility(visible = isYouTubeLoginEnabled) {
                        Column {
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
                                            .height(50.dp)
                                            .clip(thumbnailShape())
                                    )

                                Column {
                                    OtherSettingsEntry(
                                        title = if (isLoggedIn) stringResource(R.string.youtube_disconnect) else stringResource(R.string.youtube_connect),
                                        text = if (isLoggedIn) "$accountName ${accountChannelHandle}" else "",
                                        icon = R.drawable.person,
                                        onClick = {
                                            if (isLoggedIn) {
                                                cookie = ""
                                                accountName = ""
                                                accountChannelHandle = ""
                                                accountEmail = ""
                                                accountThumbnail = ""
                                                visitorData = ""
                                                dataSyncId = ""
                                                loginYouTube = false
                                                //Delete cookies after logout
                                                val cookieManager = CookieManager.getInstance()
                                                cookieManager.removeAllCookies(null)
                                                cookieManager.flush()
                                                WebStorage.getInstance().deleteAllData()
                                                restartService = true
                                            } else
                                                loginYouTube = true
                                        }
                                    )

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
                                            onLogin = { cookieRetrieved ->
                                                if (cookieRetrieved.contains("SAPISID")) {
                                                    isLoggedIn = true
                                                    loginYouTube = false
                                                    Toaster.i( context.getString(R.string.youtube_login_successful) )
                                                    restartService = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            OtherSwitchSettingEntry(
                                title = "Sync data with YTM account",
                                text = "Playlists, albums, artists, history, like, etc.",
                                isChecked = isYouTubeSyncEnabled,
                                onCheckedChange = {
                                    isYouTubeSyncEnabled = it
                                },
                                icon = R.drawable.sync
                            )
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
                                            it.fast4x.rimusic.ui.components.themed.InputTextDialog(
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
                                    it.fast4x.rimusic.ui.components.themed.InputTextDialog(
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
                                    it.fast4x.rimusic.ui.components.themed.InputTextDialog(
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
    val isYouTubeSyncEnabled = appContext().preferences.getBoolean(enableYouTubeSyncKey, false)
    return isYouTubeSyncEnabled && isYouTubeLoggedIn() && isYouTubeLoginEnabled()
}

fun isYouTubeLoggedIn(): Boolean {
    val cookie = appContext().preferences.getString(ytCookieKey, "")
    val isLoggedIn = cookie?.let { parseCookieString(it) }?.contains("SAPISID") == true
    return isLoggedIn
}
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.media3.common.util.UnstableApi::class
)

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
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import app.kreate.android.R
import io.ktor.http.Url
import it.fast4x.compose.persist.persistList
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.utils.parseCookieString
import it.fast4x.piped.Piped
import it.fast4x.piped.models.Instance
import it.fast4x.piped.models.Session
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.extensions.discord.DiscordLoginAndGetToken
import it.fast4x.rimusic.extensions.discord.DiscordPresenceManager
import it.fast4x.rimusic.extensions.youtubelogin.YouTubeLogin
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import it.fast4x.rimusic.ui.components.LocalMenuState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.component.dialog.RestartAppDialog
import timber.log.Timber



@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@SuppressLint("BatteryLife")
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
                else 1f
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

        // Settings Description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.accounts_settings_description),
                color = colorPalette().textSecondary,
                style = typography().s,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // YouTube Music Section
        YouTubeMusicAccountSection(
            restartService = restartService,
            onRestartService = { restartService = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Piped Section
        if (isAtLeastAndroid7) {
            PipedAccountSection()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discord Section
        if (isAtLeastAndroid7) {
            DiscordAccountSection()
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}

// Helper composables for settings items
@Composable
private fun SettingItem(
    title: String,
    text: String = "",
    icon: Int? = null,
    onClick: (() -> Unit)? = null,
    isChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = colorPalette()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 16.dp),
                    tint = colors.text
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colors.text,
                    style = typography().m,
                    fontWeight = FontWeight.Medium
                )
                
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        color = colors.textSecondary,
                        style = typography().s,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        
        onClick?.let {
            androidx.compose.material3.IconButton(
                onClick = it,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_forward),
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
        }
        
        onCheckedChange?.let {
            androidx.compose.material3.Switch(
                checked = isChecked,
                onCheckedChange = it
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: Int? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colorPalette().background1, thumbnailShape())
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    tint = colorPalette().text
                )
            }
            
            Text(
                text = title,
                color = colorPalette().text,
                style = typography().m,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Divider
        androidx.compose.material3.Divider(
            color = colorPalette().background2,
            thickness = 1.dp
        )
        
        // Content
        content()
    }
}

@Composable
private fun YouTubeMusicAccountSection(
    restartService: Boolean,
    onRestartService: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    var isYouTubeLoginEnabled by rememberPreference(enableYouTubeLoginKey, false)
    var isYouTubeSyncEnabled by rememberPreference(enableYouTubeSyncKey, false)
    var loginYouTube by remember { mutableStateOf(false) }
    var visitorData by rememberPreference(key = ytVisitorDataKey, defaultValue = Innertube.DEFAULT_VISITOR_DATA)
    var dataSyncId by rememberPreference(key = ytDataSyncIdKey, defaultValue = "")
    var cookie by rememberPreference(key = ytCookieKey, defaultValue = "")
    var accountName by rememberPreference(key = ytAccountNameKey, defaultValue = "")
    var accountEmail by rememberPreference(key = ytAccountEmailKey, defaultValue = "")
    var accountChannelHandle by rememberPreference(
        key = ytAccountChannelHandleKey,
        defaultValue = ""
    )
    var accountThumbnail by rememberPreference(key = ytAccountThumbnailKey, defaultValue = "")
    
    // Force recomposition when cookie changes
    val isLoggedIn = remember(cookie) {
        val hasSAPISID = "SAPISID" in parseCookieString(cookie)
        Timber.d("YouTubeMusicAccountSection: Checking login status - cookie has SAPISID: $hasSAPISID")
        hasSAPISID
    }
    
    // Debug log
    LaunchedEffect(cookie) {
        Timber.d("YouTubeMusicAccountSection: Cookie changed: ${cookie.take(50)}...")
        Timber.d("YouTubeMusicAccountSection: Has SAPISID: ${cookie.contains("SAPISID")}")
    }
    
    // ... rest of the code remains the same

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(600)) + scaleIn(
            animationSpec = tween(600),
            initialScale = 0.9f
        )
    ) {
        SectionCard(
            title = "YOUTUBE MUSIC",
            icon = R.drawable.ytmusic
        ) {
            SettingItem(
                title = stringResource(R.string.enable_youtube_music_login),
                text = "",
                icon = R.drawable.ytmusic,
                isChecked = isYouTubeLoginEnabled,
                onCheckedChange = {
                    isYouTubeLoginEnabled = it
                    if (!it) {
                        // Clear all data when disabled
                        visitorData = Innertube.DEFAULT_VISITOR_DATA
                        dataSyncId = ""
                        cookie = ""
                        accountName = ""
                        accountChannelHandle = ""
                        accountEmail = ""
                        accountThumbnail = ""
                    }
                }
            )

            AnimatedVisibility(visible = isYouTubeLoginEnabled) {
                Column {
                    // Account Info Display
                    if (isLoggedIn) {
                        AccountInfoDisplay(
                            accountThumbnail = accountThumbnail,
                            accountName = accountName,
                            accountEmail = accountEmail,
                            accountChannelHandle = accountChannelHandle
                        )
                    }

                    // Connect/Disconnect Button
                    SettingItem(
                        title = if (isLoggedIn) stringResource(R.string.youtube_disconnect) 
                               else stringResource(R.string.youtube_connect),
                        text = if (isLoggedIn) "Logged in" else "Not logged in",
                        icon = if (isLoggedIn) R.drawable.logout else R.drawable.person,
                        onClick = {
                            if (isLoggedIn) {
                                // Logout
                                cookie = ""
                                accountName = ""
                                accountChannelHandle = ""
                                accountEmail = ""
                                accountThumbnail = ""
                                visitorData = Innertube.DEFAULT_VISITOR_DATA
                                dataSyncId = ""
                                
                                // Delete cookies after logout
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.removeAllCookies(null)
                                cookieManager.flush()
                                WebStorage.getInstance().deleteAllData()
                                
                                android.widget.Toast.makeText(context, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show()
                                onRestartService(true)
                            } else {
                                // Login
                                loginYouTube = true
                            }
                        }
                    )

                    // Login Modal
                    if (loginYouTube) {
                        LoginYouTubeModal(
                            loginYouTube = loginYouTube,
                            onDismiss = { loginYouTube = false },
                            thumbnailRoundness = thumbnailRoundness,
                            onLoginSuccess = { cookieRetrieved ->
                                loginYouTube = false
                                if (cookieRetrieved.contains("SAPISID")) {
                                    android.widget.Toast.makeText(context, "YouTube login successful", android.widget.Toast.LENGTH_SHORT).show()
                                    onRestartService(true)
                                }
                            }
                        )
                    }

                    // Sync Option
                    SettingItem(
                        title = "Sync data with YTM account",
                        text = "Playlists, albums, artists, history, likes, etc.",
                        icon = R.drawable.sync,
                        isChecked = isYouTubeSyncEnabled,
                        onCheckedChange = { isYouTubeSyncEnabled = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInfoDisplay(
    accountThumbnail: String,
    accountName: String,
    accountEmail: String,
    accountChannelHandle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colorPalette().background2, thumbnailShape())
            .padding(16.dp)
    ) {
        Text(
            text = "Account Information",
            color = colorPalette().text,
            fontWeight = FontWeight.Bold,
            style = typography().m,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (accountThumbnail.isNotEmpty()) {
                ImageCacheFactory.AsyncImage(
                    thumbnailUrl = accountThumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(thumbnailShape())
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(thumbnailShape()),
                    tint = colorPalette().textSecondary
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
            ) {
                if (accountName.isNotEmpty()) {
                    Text(
                        text = accountName,
                        color = colorPalette().text,
                        fontWeight = FontWeight.Medium,
                        style = typography().m
                    )
                }
                
                if (accountChannelHandle.isNotEmpty()) {
                    Text(
                        text = accountChannelHandle,
                        color = colorPalette().textSecondary,
                        style = typography().s,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if (accountEmail.isNotEmpty()) {
                    Text(
                        text = accountEmail,
                        color = colorPalette().textSecondary,
                        style = typography().s,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginYouTubeModal(
    loginYouTube: Boolean,
    onDismiss: () -> Unit,
    thumbnailRoundness: ThumbnailRoundness,
    onLoginSuccess: (String) -> Unit
) {
    CustomModalBottomSheet(
        showSheet = loginYouTube,
        onDismissRequest = onDismiss,
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
                onLoginSuccess(cookieRetrieved)
            }
        )
    }
}

@Composable
private fun PipedAccountSection() {
    val context = LocalContext.current
    
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
    var session by remember { mutableStateOf<Result<Session>?>(null) }
    
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    
    // Check if Piped is connected
    val isPipedConnected = remember(pipedApiToken) {
        pipedApiToken.isNotEmpty()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(800)) + scaleIn(
            animationSpec = tween(800),
            initialScale = 0.9f
        )
    ) {
        SectionCard(
            title = stringResource(R.string.piped_account),
            icon = R.drawable.piped_logo
        ) {
            // Loading Dialog
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SettingItem(
                title = stringResource(R.string.enable_piped_syncronization),
                text = "",
                icon = R.drawable.piped_logo,
                isChecked = isPipedEnabled,
                onCheckedChange = { isPipedEnabled = it }
            )

            AnimatedVisibility(visible = isPipedEnabled) {
                Column {
                    // Account Info Display
                    if (isPipedConnected) {
                        PipedAccountInfoDisplay(
                            instanceName = pipedInstanceName,
                            username = pipedUsername
                        )
                    }
                    
                    SettingItem(
                        title = stringResource(R.string.piped_custom_instance),
                        text = "",
                        icon = R.drawable.server,
                        isChecked = isPipedCustomEnabled,
                        onCheckedChange = { isPipedCustomEnabled = it }
                    )
                    
                    AnimatedVisibility(visible = isPipedCustomEnabled) {
                        Column {
                            var showCustomInstanceDialog by remember { mutableStateOf(false) }
                            SettingItem(
                                title = stringResource(R.string.piped_custom_instance),
                                text = pipedApiBaseUrl,
                                icon = R.drawable.server,
                                onClick = { showCustomInstanceDialog = true }
                            )
                            
                            // Note: You need to implement your InputTextDialog here
                        }
                    }
                    
                    AnimatedVisibility(visible = !isPipedCustomEnabled) {
                        SettingItem(
                            title = stringResource(R.string.piped_change_instance),
                            text = pipedInstanceName,
                            icon = R.drawable.open,
                            onClick = {
                                loadInstances = true
                            }
                        )
                    }

                    var showUsernameDialog by remember { mutableStateOf(false) }
                    SettingItem(
                        title = stringResource(R.string.piped_username),
                        text = pipedUsername,
                        icon = R.drawable.person,
                        onClick = { showUsernameDialog = true }
                    )
                    
                    var showPasswordDialog by remember { mutableStateOf(false) }
                    SettingItem(
                        title = stringResource(R.string.piped_password),
                        text = if (pipedPassword.isNotEmpty()) "********" else "",
                        icon = R.drawable.locked,
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.semantics { password() }
                    )

                    // Connect/Disconnect Button
                    SettingItem(
                        title = if (isPipedConnected) stringResource(R.string.piped_disconnect) 
                               else stringResource(R.string.piped_connect),
                        text = if (isPipedConnected) stringResource(R.string.piped_connected_to_s).format(pipedInstanceName) 
                               else "Not connected",
                        icon = R.drawable.piped_logo,
                        onClick = {
                            if (isPipedConnected) {
                                pipedApiToken = ""
                                pipedUsername = ""
                                pipedPassword = ""
                                android.widget.Toast.makeText(context, "Disconnected from Piped", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                if (pipedUsername.isEmpty() || pipedPassword.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Please enter username and password", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    executeLogin = true
                                }
                            }
                        }
                    )
                }
            }

            // Load instances effect
            if (loadInstances) {
                LaunchedEffect(Unit) {
                    isLoading = true
                    Piped.getInstances()?.getOrNull()?.let {
                        instances = it
                    } ?: run { noInstances = true }
                    isLoading = false
                    showInstances = true
                    loadInstances = false
                }
            }

            // Show no instances message
            if (noInstances) {
                LaunchedEffect(Unit) {
                    android.widget.Toast.makeText(context, "No instances found", android.widget.Toast.LENGTH_SHORT).show()
                    noInstances = false
                }
            }

            // Execute login effect
            if (executeLogin) {
                LaunchedEffect(Unit) {
                    isLoading = true
                    val loginResult = Piped.login(
                        apiBaseUrl = Url(pipedApiBaseUrl),
                        username = pipedUsername,
                        password = pipedPassword
                    )
                    
                    loginResult?.onSuccess { sessionData ->
                        android.widget.Toast.makeText(context, "Piped login successful", android.widget.Toast.LENGTH_SHORT).show()
                        Timber.i("Piped login successful")
                        
                        pipedApiToken = sessionData.token
                        pipedApiBaseUrl = sessionData.apiBaseUrl.toString()
                    }?.onFailure {
                        Timber.e("Failed piped login ${it.stackTraceToString()}")
                        android.widget.Toast.makeText(context, "Piped login failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    
                    isLoading = false
                    executeLogin = false
                }
            }

            // Show instances menu
            if (showInstances && instances.isNotEmpty()) {
                menuState.display {
                    Menu {
                        instances.forEach { instance ->
                            MenuEntry(
                                icon = R.drawable.server,
                                text = instance.name,
                                secondaryText = "${instance.locationsFormatted} • Users: ${instance.userCount}",
                                onClick = {
                                    pipedApiBaseUrl = instance.apiBaseUrl.toString()
                                    pipedInstanceName = instance.name
                                    menuState.hide()
                                    showInstances = false
                                }
                            )
                        }
                        MenuEntry(
                            icon = R.drawable.chevron_back,
                            text = stringResource(R.string.cancel),
                            onClick = {
                                menuState.hide()
                                showInstances = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipedAccountInfoDisplay(
    instanceName: String,
    username: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colorPalette().background2, thumbnailShape())
            .padding(16.dp)
    ) {
        Text(
            text = "Piped Account",
            color = colorPalette().text,
            fontWeight = FontWeight.Bold,
            style = typography().m,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.server),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorPalette().textSecondary
            )
            Text(
                text = "Instance: $instanceName",
                color = colorPalette().textSecondary,
                style = typography().s,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.person),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorPalette().textSecondary
            )
            Text(
                text = "Username: $username",
                color = colorPalette().textSecondary,
                style = typography().s,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun DiscordAccountSection() {
    val context = LocalContext.current
    
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
    
    // Check if Discord is connected
    val isDiscordConnected = remember(discordPersonalAccessToken) {
        discordPersonalAccessToken.isNotEmpty() && isTokenValid
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
            animationSpec = tween(1000),
            initialScale = 0.9f
        )
    ) {
        SectionCard(
            title = stringResource(R.string.social_discord) + " " + stringResource(R.string.beta_title),
            icon = R.drawable.logo_discord
        ) {
            SettingItem(
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
                    // Account Info Display
                    if (isDiscordConnected) {
                        DiscordAccountInfoDisplay(
                            discordAvatar = discordAvatar,
                            discordUsername = discordUsername
                        )
                    }
                    
                    // Connect/Disconnect Button
                    SettingItem(
                        title = if (isDiscordConnected) stringResource(R.string.discord_disconnect) 
                               else stringResource(R.string.discord_connect),
                        text = if (isDiscordConnected) stringResource(R.string.discord_connected_to_discord_account) 
                               else "Not connected",
                        icon = if (isDiscordConnected) R.drawable.logout else R.drawable.logo_discord,
                        onClick = {
                            if (isDiscordConnected) {
                                discordPersonalAccessToken = ""
                                discordUsername = ""
                                discordAvatar = ""
                                showTokenError = false
                                android.widget.Toast.makeText(context, "Disconnected from Discord", android.widget.Toast.LENGTH_SHORT).show()
                                RestartAppDialog.showDialog()
                            } else {
                                loginDiscord = true
                            }
                        }
                    )

                    // Login Modal
                    if (loginDiscord) {
                        LoginDiscordModal(
                            loginDiscord = loginDiscord,
                            thumbnailRoundness = ThumbnailRoundness.Heavy,
                            onDismiss = { loginDiscord = false },
                            onLoginSuccess = { token, username, avatar ->
                                discordPersonalAccessToken = token
                                discordUsername = username
                                discordAvatar = avatar
                                android.widget.Toast.makeText(context, "Connected to Discord", android.widget.Toast.LENGTH_SHORT).show()
                                RestartAppDialog.showDialog()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscordAccountInfoDisplay(
    discordAvatar: String,
    discordUsername: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colorPalette().background2, thumbnailShape())
            .padding(16.dp)
    ) {
        Text(
            text = "Discord Account",
            color = colorPalette().text,
            fontWeight = FontWeight.Bold,
            style = typography().m,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (discordAvatar.isNotEmpty()) {
                ImageCacheFactory.AsyncImage(
                    thumbnailUrl = discordAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(thumbnailShape())
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(thumbnailShape()),
                    tint = colorPalette().textSecondary
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = discordUsername,
                    color = colorPalette().text,
                    fontWeight = FontWeight.Medium,
                    style = typography().m
                )
                
                Text(
                    text = "Discord Rich Presence",
                    color = colorPalette().textSecondary,
                    style = typography().s,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LoginDiscordModal(
    loginDiscord: Boolean,
    thumbnailRoundness: ThumbnailRoundness,
    onDismiss: () -> Unit,
    onLoginSuccess: (String, String, String) -> Unit
) {
    CustomModalBottomSheet(
        showSheet = loginDiscord,
        onDismissRequest = onDismiss,
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
            onGetToken = onLoginSuccess
        )
    }
}

// Utility functions at the end of the file
fun isYouTubeLoginEnabled(): Boolean {
    val isYouTubeLoginEnabled = appContext().preferences.getBoolean(enableYouTubeLoginKey, false)
    return isYouTubeLoginEnabled
}

fun isYouTubeSyncEnabled(): Boolean {
    val isYouTubeSyncEnabled = appContext().preferences.getBoolean(enableYouTubeSyncKey, false)
    val isLoggedIn = isYouTubeLoggedIn()
    val isLoginEnabled = isYouTubeLoginEnabled()
    return isYouTubeSyncEnabled && isLoggedIn && isLoginEnabled
}

fun isYouTubeLoggedIn(): Boolean {
    val cookie = appContext().preferences.getString(ytCookieKey, "")
    val isLoggedIn = cookie?.let { parseCookieString(it) }?.contains("SAPISID") == true
    return isLoggedIn
}

// Helper function to get latest cookie from multiple accounts
fun getLatestYouTubeCookie(): String? {
    val cookie = appContext().preferences.getString(ytCookieKey, "")
    return if (cookie?.contains("SAPISID") == true) cookie else null
}
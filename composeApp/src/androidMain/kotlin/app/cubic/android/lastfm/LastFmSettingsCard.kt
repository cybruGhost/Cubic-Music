package app.cubic.android.lastfm

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.screens.settings.OtherSwitchSettingEntry
import app.it.fast4x.rimusic.ui.screens.settings.SettingsSectionCard
import app.it.fast4x.rimusic.utils.encryptedPreferences
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.BuildConfig
import app.kreate.android.R
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import timber.log.Timber

private sealed interface LastFmConnectionStatus {
    data object Disconnected : LastFmConnectionStatus
    data object Checking : LastFmConnectionStatus
    data class Online(val profile: LastFmProfile) : LastFmConnectionStatus
    data class Issue(val detail: String) : LastFmConnectionStatus
}

@Composable
fun LastFmSettingsCard() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val preferences = remember { context.preferences }
    val securePreferences = remember { context.encryptedPreferences }
    val lastFmConfigured = remember {
        CubicLastFmClient.initialize(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_API_SECRET)
        CubicLastFmClient.isConfigured()
    }
    var enabled by rememberPreference(lastFmEnabledKey, true)
    var nowPlayingEnabled by rememberPreference(lastFmNowPlayingEnabledKey, true)
    var scrobblingEnabled by rememberPreference(lastFmScrobblingEnabledKey, true)
    var sessionKey by remember {
        mutableStateOf(securePreferences.getString(lastFmSessionKey, "").orEmpty())
    }
    var username by remember {
        mutableStateOf(securePreferences.getString(lastFmUsernameKey, "").orEmpty())
    }
    var avatarUrl by remember {
        mutableStateOf(securePreferences.getString(lastFmAvatarUrlKey, "").orEmpty())
    }
    var status by remember { mutableStateOf<LastFmConnectionStatus>(LastFmConnectionStatus.Disconnected) }
    var refreshRequest by remember { mutableIntStateOf(0) }
    var showLogin by remember { mutableStateOf(false) }

    DisposableEffect(securePreferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            when (key) {
                lastFmSessionKey -> sessionKey = preferences.getString(key, "").orEmpty()
                lastFmUsernameKey -> username = preferences.getString(key, "").orEmpty()
                lastFmAvatarUrlKey -> avatarUrl = preferences.getString(key, "").orEmpty()
            }
        }
        securePreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { securePreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == lastFmLastScrobbleAtKey) refreshRequest++
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(enabled, nowPlayingEnabled, scrobblingEnabled, sessionKey) {
        binder?.refreshLastFmIntegration()
    }

    LaunchedEffect(sessionKey, username, refreshRequest) {
        CubicLastFmClient.initialize(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_API_SECRET)
        CubicLastFmClient.sessionKey = sessionKey.ifBlank { null }
        if (!CubicLastFmClient.isConfigured()) {
            status = LastFmConnectionStatus.Issue(context.getString(R.string.lastfm_build_not_configured))
            return@LaunchedEffect
        }
        if (sessionKey.isBlank() || username.isBlank()) {
            status = LastFmConnectionStatus.Disconnected
            return@LaunchedEffect
        }

        status = LastFmConnectionStatus.Checking
        CubicLastFmClient.getProfile(username)
            .onSuccess { profile ->
                status = LastFmConnectionStatus.Online(profile)
                val nextAvatar = profile.avatarUrl.orEmpty()
                if (nextAvatar != avatarUrl) {
                    securePreferences.edit { putString(lastFmAvatarUrlKey, nextAvatar) }
                }
            }
            .onFailure { error ->
                Timber.tag("CubicLastFm").w(error, "Last.fm account status check failed")
                status = LastFmConnectionStatus.Issue(
                    error.message?.takeIf(String::isNotBlank)
                        ?: context.getString(R.string.lastfm_status_issue_detail),
                )
            }
    }

    SettingsSectionCard(
        title = stringResource(R.string.lastfm_title),
        icon = R.drawable.logo_lastfm,
        content = {
        LastFmStatusPanel(
            status = status,
            username = username,
            avatarUrl = avatarUrl,
            onRetry = { refreshRequest++ },
        )

        Spacer(Modifier.height(8.dp))

        if (sessionKey.isBlank()) {
            Button(
                onClick = { showLogin = true },
                enabled = lastFmConfigured,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.lastfm_connect))
            }
        } else {
            OtherSwitchSettingEntry(
                title = stringResource(R.string.lastfm_enable),
                text = stringResource(R.string.lastfm_enable_description),
                isChecked = enabled,
                onCheckedChange = { enabled = it },
                icon = R.drawable.logo_lastfm,
            )

            AnimatedVisibility(visible = enabled) {
                Column {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.lastfm_now_playing),
                        text = stringResource(R.string.lastfm_now_playing_description),
                        isChecked = nowPlayingEnabled,
                        onCheckedChange = { nowPlayingEnabled = it },
                        icon = R.drawable.play,
                    )
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.lastfm_scrobble),
                        text = stringResource(R.string.lastfm_scrobble_description),
                        isChecked = scrobblingEnabled,
                        onCheckedChange = { scrobblingEnabled = it },
                        icon = R.drawable.history,
                    )
                }
            }

            TextButton(
                onClick = {
                    enabled = false
                    sessionKey = ""
                    username = ""
                    avatarUrl = ""
                    securePreferences.edit {
                        remove(lastFmSessionKey)
                        remove(lastFmUsernameKey)
                        remove(lastFmAvatarUrlKey)
                    }
                    CubicLastFmClient.sessionKey = null
                    status = LastFmConnectionStatus.Disconnected
                },
            ) {
                Text(stringResource(R.string.lastfm_disconnect), color = colorPalette().red)
            }
        }
        },
    )

    if (showLogin) {
        LastFmLoginDialog(
            onDismiss = { showLogin = false },
            onConnected = { session ->
                preferences.edit(commit = true) { putBoolean(lastFmEnabledKey, true) }
                securePreferences.edit(commit = true) {
                    putString(lastFmSessionKey, session.key)
                    putString(lastFmUsernameKey, session.username)
                    remove(lastFmAvatarUrlKey)
                }
                CubicLastFmClient.sessionKey = session.key
                enabled = true
                sessionKey = session.key
                username = session.username
                avatarUrl = ""
                status = LastFmConnectionStatus.Checking
                refreshRequest++
                binder?.refreshLastFmIntegration()
                showLogin = false
            },
        )
    }
}

@Composable
private fun LastFmStatusPanel(
    status: LastFmConnectionStatus,
    username: String,
    avatarUrl: String,
    onRetry: () -> Unit,
) {
    val online = status is LastFmConnectionStatus.Online
    val issue = status is LastFmConnectionStatus.Issue
    val statusColor = when {
        online -> Color(0xFF2EBD59)
        issue -> colorPalette().red
        status is LastFmConnectionStatus.Checking -> colorPalette().accent
        else -> colorPalette().textSecondary
    }
    val title = when (status) {
        LastFmConnectionStatus.Disconnected -> stringResource(R.string.lastfm_status_disconnected)
        LastFmConnectionStatus.Checking -> stringResource(R.string.lastfm_status_checking)
        is LastFmConnectionStatus.Online -> stringResource(R.string.lastfm_status_online)
        is LastFmConnectionStatus.Issue -> stringResource(R.string.lastfm_status_issue)
    }
    val detail = when (status) {
        LastFmConnectionStatus.Disconnected -> stringResource(R.string.lastfm_status_disconnected_detail)
        LastFmConnectionStatus.Checking -> stringResource(R.string.lastfm_status_checking_detail)
        is LastFmConnectionStatus.Online -> stringResource(
            R.string.lastfm_status_online_detail,
            status.profile.username,
            status.profile.playCount,
        )
        is LastFmConnectionStatus.Issue -> status.detail
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorPalette().background2)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFD51007).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = username,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.logo_lastfm),
                    error = painterResource(R.drawable.logo_lastfm),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.logo_lastfm),
                    contentDescription = null,
                    tint = Color(0xFFD51007),
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status is LastFmConnectionStatus.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = statusColor,
                    )
                } else {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                }
                Text(
                    text = title,
                    color = colorPalette().text,
                    style = typography().s.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Text(
                text = detail,
                color = colorPalette().textSecondary,
                style = typography().xs,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (issue) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry), color = colorPalette().accent)
            }
        }
    }
}

@Composable
private fun LastFmLoginDialog(
    onDismiss: () -> Unit,
    onConnected: (LastFmSession) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val glassShape = RoundedCornerShape(28.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colorPalette().text,
        unfocusedTextColor = colorPalette().text,
        cursorColor = Color(0xFFFF6259),
        focusedBorderColor = Color(0xFFFF6259),
        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
        focusedContainerColor = Color.White.copy(alpha = 0.09f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        focusedLabelColor = Color(0xFFFF8B84),
        unfocusedLabelColor = colorPalette().textSecondary,
    )

    Dialog(
        onDismissRequest = { if (!connecting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(28.dp, glassShape)
                .clip(glassShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorPalette().background2.copy(alpha = 0.98f),
                            Color(0xFF5B1117).copy(alpha = 0.96f),
                            colorPalette().background1.copy(alpha = 0.98f),
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), glassShape)
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFD51007).copy(alpha = 0.18f))
                            .border(
                                1.dp,
                                Color(0xFFFF8B84).copy(alpha = 0.35f),
                                RoundedCornerShape(15.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.logo_lastfm),
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(27.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.lastfm_connect_title),
                            color = colorPalette().text,
                            style = typography().m.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = stringResource(R.string.lastfm_login_subtitle),
                            color = colorPalette().textSecondary,
                            style = typography().xs,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.lastfm_login_privacy),
                    color = colorPalette().textSecondary,
                    style = typography().xs,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.lastfm_username)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.lastfm_password)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordVisible) R.drawable.eye_off else R.drawable.eye,
                                ),
                                contentDescription = stringResource(R.string.lastfm_show_password),
                                tint = colorPalette().textSecondary,
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorText?.let {
                    Text(text = it, color = colorPalette().red, style = typography().xs)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(enabled = !connecting, onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cancel),
                            color = colorPalette().textSecondary,
                        )
                    }
                    Button(
                        enabled = username.isNotBlank() && password.isNotBlank() && !connecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD51007),
                            contentColor = Color.White,
                        ),
                        onClick = {
                            connecting = true
                            errorText = null
                            scope.launch {
                                CubicLastFmClient.initialize(
                                    BuildConfig.LASTFM_API_KEY,
                                    BuildConfig.LASTFM_API_SECRET,
                                )
                                CubicLastFmClient.login(username, password)
                                    .onSuccess(onConnected)
                                    .onFailure { error ->
                                        Timber.tag("CubicLastFm").w(error, "Last.fm sign-in failed")
                                        errorText = error.message
                                            ?.takeIf(String::isNotBlank)
                                            ?: "Last.fm sign-in failed"
                                    }
                                password = ""
                                connecting = false
                            }
                        },
                    ) {
                        if (connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Text(stringResource(R.string.lastfm_connect))
                        }
                    }
                }
            }
        }
    }
}

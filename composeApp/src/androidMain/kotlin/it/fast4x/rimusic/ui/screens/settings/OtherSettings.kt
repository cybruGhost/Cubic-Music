package it.fast4x.rimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.BuildConfig
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.ValidationType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.StringListDialog
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.defaultFolderKey
import it.fast4x.rimusic.utils.extraspaceKey
import it.fast4x.rimusic.utils.isAtLeastAndroid10
import it.fast4x.rimusic.utils.isAtLeastAndroid12
import it.fast4x.rimusic.utils.isAtLeastAndroid6
import it.fast4x.rimusic.utils.isIgnoringBatteryOptimizations
import it.fast4x.rimusic.utils.isKeepScreenOnEnabledKey
import it.fast4x.rimusic.utils.isProxyEnabledKey
import it.fast4x.rimusic.utils.logDebugEnabledKey
import it.fast4x.rimusic.utils.parentalControlEnabledKey
import it.fast4x.rimusic.utils.proxyHostnameKey
import it.fast4x.rimusic.utils.proxyModeKey
import it.fast4x.rimusic.utils.proxyPortKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.showFoldersOnDeviceKey
import it.fast4x.rimusic.utils.textCopyToClipboard
import me.knighthat.utils.Toaster
import java.io.File
import java.net.Proxy

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var isProxyEnabled by rememberPreference(isProxyEnabledKey, false)
    var proxyHost by rememberPreference(proxyHostnameKey, "")
    var proxyPort by rememberPreference(proxyPortKey, 1080)
    var proxyMode by rememberPreference(proxyModeKey, Proxy.Type.HTTP)

    var defaultFolder by rememberPreference(defaultFolderKey, "/")
    var isKeepScreenOnEnabled by rememberPreference(isKeepScreenOnEnabledKey, false)
    var showFolders by rememberPreference(showFoldersOnDeviceKey, true)

    var blackListedPaths by remember {
        val file = File(context.filesDir, "Blacklisted_paths.txt")
        if (file.exists()) {
            mutableStateOf(file.readLines())
        } else {
            mutableStateOf(emptyList())
        }
    }

    var parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    var logDebugEnabled by rememberPreference(logDebugEnabledKey, false)
    var extraspace by rememberPreference(extraspaceKey, false)

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
            title = stringResource(R.string.tab_miscellaneous),
            iconId = R.drawable.equalizer,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.other_settings_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 

        Spacer(modifier = Modifier.height(16.dp))

        // On Device Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                animationSpec = tween(600),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.on_device),
                icon = R.drawable.folder,
                content = {
                    // Modern wrapper with icon
                    var showBlacklistDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.blacklisted_folders),
                        text = stringResource(R.string.edit_blacklist_for_on_device_songs),
                        icon = R.drawable.blacklisted_folder,
                        onClick = { showBlacklistDialog = true }
                    )
                    
                    // Only the dialog, not the old component
                    if (showBlacklistDialog) {
                        StringListDialog(
                            title = stringResource(R.string.blacklisted_folders),
                            addTitle = stringResource(R.string.add_folder),
                            addPlaceholder = if (isAtLeastAndroid10) {
                                "Android/media/com.whatsapp/WhatsApp/Media"
                            } else {
                                "/storage/emulated/0/Android/media/com.whatsapp/"
                            },
                            conflictTitle = stringResource(R.string.this_folder_already_exists),
                            removeTitle = stringResource(R.string.are_you_sure_you_want_to_remove_this_folder_from_the_blacklist),
                            list = blackListedPaths,
                            add = { newPath: String ->
                                blackListedPaths = blackListedPaths + newPath
                                val file = File(context.filesDir, "Blacklisted_paths.txt")
                                file.writeText(blackListedPaths.joinToString("\n"))
                            },
                            remove = { path: String ->
                                blackListedPaths = blackListedPaths.filter { it != path }
                                val file = File(context.filesDir, "Blacklisted_paths.txt")
                                file.writeText(blackListedPaths.joinToString("\n"))
                            },
                            onDismiss = { showBlacklistDialog = false }
                        )
                    }
                    


                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.folders),
                        text = stringResource(R.string.show_folders_in_on_device_page),
                        isChecked = showFolders,
                        onCheckedChange = { showFolders = it },
                        icon = R.drawable.playlist
                    )
                    
                    AnimatedVisibility(visible = showFolders) {
                        // Modern wrapper with icon
                        var showFolderDialog by remember { mutableStateOf(false) }
                        OtherSettingsEntry(
                            title = stringResource(R.string.folder_that_will_show_when_you_open_on_device_page),
                            text = defaultFolder,
                            icon = R.drawable.music_file,
                            onClick = { showFolderDialog = true }
                        )
                        
                        // Only the dialog, not the old component
                        if (showFolderDialog) {
                            InputTextDialog(
                                title = stringResource(R.string.folder_that_will_show_when_you_open_on_device_page),
                                value = defaultFolder,
                                placeholder = stringResource(R.string.folder_that_will_show_when_you_open_on_device_page),
                                onDismiss = { showFolderDialog = false },
                                setValue = { defaultFolder = it }
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Android Head Unit Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                animationSpec = tween(800),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.androidheadunit),
                icon = R.drawable.car,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.extra_space),
                        text = "",
                        isChecked = extraspace,
                        onCheckedChange = { extraspace = it },
                        icon = R.drawable.space
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Service Lifetime Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                animationSpec = tween(1000),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.service_lifetime),
                icon = R.drawable.battery,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.keep_screen_on),
                        text = stringResource(R.string.prevents_screen_timeout),
                        isChecked = isKeepScreenOnEnabled,
                        onCheckedChange = { isKeepScreenOnEnabled = it },
                        icon = R.drawable.devices
                    )

                    ImportantSettingsDescription(text = stringResource(R.string.battery_optimizations_applied))

                    if (isAtLeastAndroid12) {
                        SettingsDescription(text = stringResource(R.string.is_android12))
                    }

                    val msgNoBatteryOptim = stringResource(R.string.not_find_battery_optimization_settings)

                                            OtherSettingsEntry(
                            title = stringResource(R.string.ignore_battery_optimizations),
                            text = if (isIgnoringBatteryOptimizations) {
                                stringResource(R.string.already_unrestricted)
                            } else {
                                stringResource(R.string.disable_background_restrictions)
                            },
                            icon = R.drawable.battery_opti,
                        onClick = {
                            if (!isAtLeastAndroid6) return@OtherSettingsEntry

                            try {
                                activityResultLauncher.launch(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            } catch (e: ActivityNotFoundException) {
                                try {
                                    activityResultLauncher.launch(
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    Toaster.i("$msgNoBatteryOptim ${BuildConfig.APP_NAME}")
                                }
                            }
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Proxy Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1200)) + scaleIn(
                animationSpec = tween(1200),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.proxy),
                icon = R.drawable.network,
                content = {
                    SettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
                    
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_proxy),
                        text = "",
                        isChecked = isProxyEnabled,
                        onCheckedChange = { isProxyEnabled = it },
                        icon = R.drawable.server
                    )

                    AnimatedVisibility(visible = isProxyEnabled) {
                        Column {
                            // Modern wrapper with icon
                            var showProxyModeDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.proxy_mode),
                                text = proxyMode.name,
                                icon = R.drawable.server,
                                onClick = { showProxyModeDialog = true }
                            )
                            
                            // Only the dialog, not the old component
                            if (showProxyModeDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.proxy_mode),
                                    selectedValue = proxyMode,
                                    values = Proxy.Type.values().toList(),
                                    onValueSelected = { proxyMode = it },
                                    valueText = { it.name },
                                    onDismiss = { showProxyModeDialog = false }
                                )
                            }
                            // Modern wrapper with icon
                            var showProxyHostDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.proxy_host),
                                text = proxyHost,
                                icon = R.drawable.server,
                                onClick = { showProxyHostDialog = true }
                            )
                            
                            // Only the dialog, not the old component
                            if (showProxyHostDialog) {
                                InputTextDialog(
                                    title = stringResource(R.string.proxy_host),
                                    value = proxyHost,
                                    placeholder = stringResource(R.string.proxy_host),
                                    onDismiss = { showProxyHostDialog = false },
                                    setValue = { proxyHost = it },
                                    validationType = ValidationType.Ip
                                )
                            }
                            // Modern wrapper with icon
                            var showProxyPortDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.proxy_port),
                                text = proxyPort.toString(),
                                icon = R.drawable.server,
                                onClick = { showProxyPortDialog = true }
                            )
                            
                            // Only the dialog, not the old component
                            if (showProxyPortDialog) {
                                InputTextDialog(
                                    title = stringResource(R.string.proxy_port),
                                    value = proxyPort.toString(),
                                    placeholder = stringResource(R.string.proxy_port),
                                    onDismiss = { showProxyPortDialog = false },
                                    setValue = { proxyPort = it.toIntOrNull() ?: 1080 }
                                )
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Parental Control Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1400)) + scaleIn(
                animationSpec = tween(1400),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.parental_control),
                icon = R.drawable.shield_checkmark,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.parental_control),
                        text = stringResource(R.string.info_prevent_play_songs_with_age_limitation),
                        isChecked = parentalControlEnabled,
                        onCheckedChange = { parentalControlEnabled = it },
                        icon = R.drawable.parental_control
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1600)) + scaleIn(
                animationSpec = tween(1600),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.debug),
                icon = R.drawable.bugs,
                content = {
                    var text by remember { mutableStateOf(null as String?) }
                    val noLogAvailable = stringResource(R.string.no_log_available)

                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_log_debug),
                        text = stringResource(R.string.if_enabled_create_a_log_file_to_highlight_errors),
                        isChecked = logDebugEnabled,
                        onCheckedChange = {
                            logDebugEnabled = it
                            if (!it) {
                                val file = File(context.filesDir.resolve("logs"), "N-Zik_log.txt")
                                if (file.exists())
                                    file.delete()

                                val filec = File(context.filesDir.resolve("logs"), "N-Zik_crash_log.txt")
                                if (filec.exists())
                                    filec.delete()
                            } else
                                Toaster.i(R.string.restarting_rimusic_is_required)
                        },
                        icon = R.drawable.information
                    )
                    
                    ImportantSettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
                    
                    OtherSettingsEntry(
                        title = stringResource(R.string.copy_log_to_clipboard),
                        text = "",
                        icon = R.drawable.copy,
                        onClick = {
                            val file = File(context.filesDir.resolve("logs"), "N-Zik_log.txt")
                            if (file.exists()) {
                                text = file.readText()
                                text?.let {
                                    textCopyToClipboard(it, context)
                                }
                            } else
                                Toaster.w(noLogAvailable)
                        }
                    )
                    
                    OtherSettingsEntry(
                        title = stringResource(R.string.copy_crash_log_to_clipboard),
                        text = "",
                        icon = R.drawable.copy,
                        onClick = {
                            val file = File(context.filesDir.resolve("logs"), "N-Zik_crash_log.txt")
                            if (file.exists()) {
                                text = file.readText()
                                text?.let {
                                    textCopyToClipboard(it, context)
                                }
                            } else
                                Toaster.w(noLogAvailable)
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}
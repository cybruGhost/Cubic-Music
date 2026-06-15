package app.it.fast4x.rimusic.ui.screens.settings

import android.content.Context
import android.os.Build
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.service.PlaybackSourceKind
import app.kreate.android.service.PlaybackSourceMonitor
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.DurationInMilliseconds
import app.it.fast4x.rimusic.enums.DurationInMinutes
import app.it.fast4x.rimusic.enums.ExoPlayerMinTimeForEvent
import app.it.fast4x.rimusic.enums.InnertubePlayerSource
import app.it.fast4x.rimusic.enums.Languages
import app.it.fast4x.rimusic.enums.MaxSongs
import app.it.fast4x.rimusic.enums.MusicAnimationType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.NavigationBarType
import app.it.fast4x.rimusic.enums.NotificationType
import app.it.fast4x.rimusic.enums.PauseBetweenSongs
import app.it.fast4x.rimusic.enums.PipModule
import app.it.fast4x.rimusic.enums.PresetsReverb
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.screens.spotify.LogType
import app.it.fast4x.rimusic.ui.screens.spotify.SpotifyCanvasState
import app.it.fast4x.rimusic.ui.styling.DefaultDarkColorPalette
import app.it.fast4x.rimusic.ui.styling.DefaultLightColorPalette
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.RestartActivity
import app.it.fast4x.rimusic.utils.RestartPlayerService
import app.it.fast4x.rimusic.utils.audioReverbPresetKey
import app.it.fast4x.rimusic.utils.autoLoadSongsInQueueKey
import app.it.fast4x.rimusic.utils.bassboostEnabledKey
import app.it.fast4x.rimusic.utils.bassboostLevelKey
import app.it.fast4x.rimusic.utils.closeWithBackButtonKey
import app.it.fast4x.rimusic.utils.closebackgroundPlayerKey
import app.it.fast4x.rimusic.utils.customThemeDark_Background0Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background1Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background2Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background3Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background4Key
import app.it.fast4x.rimusic.utils.customThemeDark_TextKey
import app.it.fast4x.rimusic.utils.customThemeDark_accentKey
import app.it.fast4x.rimusic.utils.customThemeDark_iconButtonPlayerKey
import app.it.fast4x.rimusic.utils.customThemeDark_textDisabledKey
import app.it.fast4x.rimusic.utils.customThemeDark_textSecondaryKey
import app.it.fast4x.rimusic.utils.customThemeLight_Background0Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background1Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background2Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background3Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background4Key
import app.it.fast4x.rimusic.utils.customThemeLight_TextKey
import app.it.fast4x.rimusic.utils.customThemeLight_accentKey
import app.it.fast4x.rimusic.utils.customThemeLight_iconButtonPlayerKey
import app.it.fast4x.rimusic.utils.customThemeLight_textDisabledKey
import app.it.fast4x.rimusic.utils.customThemeLight_textSecondaryKey
import app.it.fast4x.rimusic.utils.disableClosingPlayerSwipingDownKey
import app.it.fast4x.rimusic.utils.discoverKey
import app.it.fast4x.rimusic.utils.enablePictureInPictureAutoKey
import app.it.fast4x.rimusic.utils.enablePictureInPictureKey
import app.it.fast4x.rimusic.utils.excludeSongsWithDurationLimitKey
import app.it.fast4x.rimusic.utils.exoPlayerMinTimeForEventKey
import app.it.fast4x.rimusic.utils.handleAudioFocusEnabledKey
import app.it.fast4x.rimusic.utils.isAtLeastAndroid12
import app.it.fast4x.rimusic.utils.isAtLeastAndroid6
import app.it.fast4x.rimusic.utils.isPauseOnVolumeZeroEnabledKey
import app.it.fast4x.rimusic.utils.innertubePlayerSourceKey
import app.it.fast4x.rimusic.utils.jumpPreviousKey
import app.it.fast4x.rimusic.utils.keepPlayerMinimizedKey
import app.it.fast4x.rimusic.utils.languageAppKey
import app.it.fast4x.rimusic.utils.logDebugEnabledKey
import app.it.fast4x.rimusic.utils.loudnessBaseGainKey
import app.it.fast4x.rimusic.utils.maxSongsInQueueKey
import app.it.fast4x.rimusic.utils.minimumSilenceDurationKey
import app.it.fast4x.rimusic.utils.navigationBarPositionKey
import app.it.fast4x.rimusic.utils.navigationBarTypeKey
import app.it.fast4x.rimusic.utils.newReleaseNotificationsEnabledKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.showPlayerOutputDeviceKey
import app.it.fast4x.rimusic.utils.showPlayerPlaybackContextKey
import app.it.fast4x.rimusic.utils.notificationTypeKey
import app.it.fast4x.rimusic.utils.nowPlayingIndicatorKey
import app.it.fast4x.rimusic.utils.nowPlayingProgressRingKey
import app.it.fast4x.rimusic.utils.pauseBetweenSongsKey
import app.it.fast4x.rimusic.utils.persistentQueueKey
import app.it.fast4x.rimusic.utils.pipModuleKey
import app.it.fast4x.rimusic.utils.playbackFadeAudioDurationKey
import app.it.fast4x.rimusic.utils.playlistindicatorKey
import app.it.fast4x.rimusic.utils.rememberEqualizerLauncher
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.resumePlaybackOnStartKey
import app.it.fast4x.rimusic.utils.resumePlaybackWhenDeviceConnectedKey
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.settingsAssistantAutoPopupKey
import app.it.fast4x.rimusic.utils.settingsAssistantEnabledKey
import app.it.fast4x.rimusic.utils.shakeEventEnabledKey
import app.it.fast4x.rimusic.utils.showRescueCenterInMenuKey
import app.it.fast4x.rimusic.utils.showLyricsSourceSwitcherKey
import app.it.fast4x.rimusic.utils.skipMediaOnErrorKey
import app.it.fast4x.rimusic.utils.skipSilenceKey
import app.it.fast4x.rimusic.utils.useVolumeKeysToChangeSongKey
import app.it.fast4x.rimusic.utils.volumeNormalizationKey
import app.it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import app.it.fast4x.rimusic.ui.components.themed.InputTextDialog
import app.kreate.android.me.knighthat.component.dialog.RestartAppDialog
import app.kreate.android.me.knighthat.component.tab.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
// Spotify login helpers
import app.it.fast4x.rimusic.ui.screens.spotify.clearSpotifySession
import app.it.fast4x.rimusic.ui.screens.spotify.getMaskedSpDc
import app.it.fast4x.rimusic.ui.screens.spotify.getSpotifySessionInfo
import app.it.fast4x.rimusic.ui.screens.spotify.isSpotifyLoggedIn
import app.it.fast4x.rimusic.ui.screens.spotify.renewSpotifySession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun GeneralSettings(
    navController: NavController
) {
    val binder = LocalPlayerServiceBinder.current

    var languageApp by rememberPreference(languageAppKey, Languages.System)
    val systemLocale = LocaleListCompat.getDefault().get(0).toString()

    var exoPlayerMinTimeForEvent by rememberPreference(
        exoPlayerMinTimeForEventKey, ExoPlayerMinTimeForEvent.`20s`
    )

    var persistentQueue                  by rememberPreference(persistentQueueKey, false)
    var resumePlaybackOnStart            by rememberPreference(resumePlaybackOnStartKey, false)
    var closebackgroundPlayer            by rememberPreference(closebackgroundPlayerKey, false)
    var closeWithBackButton              by rememberPreference(closeWithBackButtonKey, true)
    var resumePlaybackWhenDeviceConnected by rememberPreference(resumePlaybackWhenDeviceConnectedKey, false)
    var skipSilence                      by rememberPreference(skipSilenceKey, false)
    var skipMediaOnError                 by rememberPreference(skipMediaOnErrorKey, true)
    var volumeNormalization              by rememberPreference(volumeNormalizationKey, false)
    var keepPlayerMinimized              by rememberPreference(keepPlayerMinimizedKey, false)
    var disableClosingPlayerSwipingDown  by rememberPreference(disableClosingPlayerSwipingDownKey, false)
    var navigationBarPosition            by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)
    var navigationBarType                by rememberPreference(navigationBarTypeKey, NavigationBarType.IconAndText)
    var pauseBetweenSongs                by rememberPreference(pauseBetweenSongsKey, PauseBetweenSongs.`0`)
    var maxSongsInQueue                  by rememberPreference(maxSongsInQueueKey, MaxSongs.`500`)

    val search = Search()

    var shakeEventEnabled         by rememberPreference(shakeEventEnabledKey, false)
    var useVolumeKeysToChangeSong by rememberPreference(useVolumeKeysToChangeSongKey, false)

    var customThemeLight_Background0     by rememberPreference(customThemeLight_Background0Key, DefaultLightColorPalette.background0.hashCode())
    var customThemeLight_Background1     by rememberPreference(customThemeLight_Background1Key, DefaultLightColorPalette.background1.hashCode())
    var customThemeLight_Background2     by rememberPreference(customThemeLight_Background2Key, DefaultLightColorPalette.background2.hashCode())
    var customThemeLight_Background3     by rememberPreference(customThemeLight_Background3Key, DefaultLightColorPalette.background3.hashCode())
    var customThemeLight_Background4     by rememberPreference(customThemeLight_Background4Key, DefaultLightColorPalette.background4.hashCode())
    var customThemeLight_Text            by rememberPreference(customThemeLight_TextKey, DefaultLightColorPalette.text.hashCode())
    var customThemeLight_TextSecondary   by rememberPreference(customThemeLight_textSecondaryKey, DefaultLightColorPalette.textSecondary.hashCode())
    var customThemeLight_TextDisabled    by rememberPreference(customThemeLight_textDisabledKey, DefaultLightColorPalette.textDisabled.hashCode())
    var customThemeLight_IconButtonPlayer by rememberPreference(customThemeLight_iconButtonPlayerKey, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    var customThemeLight_Accent          by rememberPreference(customThemeLight_accentKey, DefaultLightColorPalette.accent.hashCode())

    var customThemeDark_Background0     by rememberPreference(customThemeDark_Background0Key, DefaultDarkColorPalette.background0.hashCode())
    var customThemeDark_Background1     by rememberPreference(customThemeDark_Background1Key, DefaultDarkColorPalette.background1.hashCode())
    var customThemeDark_Background2     by rememberPreference(customThemeDark_Background2Key, DefaultDarkColorPalette.background2.hashCode())
    var customThemeDark_Background3     by rememberPreference(customThemeDark_Background3Key, DefaultDarkColorPalette.background3.hashCode())
    var customThemeDark_Background4     by rememberPreference(customThemeDark_Background4Key, DefaultDarkColorPalette.background4.hashCode())
    var customThemeDark_Text            by rememberPreference(customThemeDark_TextKey, DefaultDarkColorPalette.text.hashCode())
    var customThemeDark_TextSecondary   by rememberPreference(customThemeDark_textSecondaryKey, DefaultDarkColorPalette.textSecondary.hashCode())
    var customThemeDark_TextDisabled    by rememberPreference(customThemeDark_textDisabledKey, DefaultDarkColorPalette.textDisabled.hashCode())
    var customThemeDark_IconButtonPlayer by rememberPreference(customThemeDark_iconButtonPlayerKey, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    var customThemeDark_Accent          by rememberPreference(customThemeDark_accentKey, DefaultDarkColorPalette.accent.hashCode())

    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog  by rememberSaveable { mutableStateOf(false) }

    var playbackFadeAudioDuration    by rememberPreference(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled)
    var excludeSongWithDurationLimit by rememberPreference(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)
    var playlistindicator            by rememberPreference(playlistindicatorKey, false)
    var nowPlayingIndicator          by rememberPreference(nowPlayingIndicatorKey, MusicAnimationType.Bubbles)
    var nowPlayingProgressRing       by rememberPreference(nowPlayingProgressRingKey, true)
    var discoverIsEnabled            by rememberPreference(discoverKey, false)
    var isPauseOnVolumeZeroEnabled   by rememberPreference(isPauseOnVolumeZeroEnabledKey, false)

    val launchEqualizer by rememberEqualizerLauncher(audioSessionId = { binder?.player?.audioSessionId })

    var minimumSilenceDuration by rememberPreference(minimumSilenceDurationKey, 2_000_000L)
    var restartService         by rememberSaveable { mutableStateOf(false) }
    var restartActivity        by rememberSaveable { mutableStateOf(false) }
    var loudnessBaseGain       by rememberPreference(loudnessBaseGainKey, 5.00f)
    var autoLoadSongsInQueue   by rememberPreference(autoLoadSongsInQueueKey, true)
    var bassboostEnabled       by rememberPreference(bassboostEnabledKey, false)
    var bassboostLevel         by rememberPreference(bassboostLevelKey, 0.5f)
    var audioReverb            by rememberPreference(audioReverbPresetKey, PresetsReverb.NONE)
    var audioFocusEnabled      by rememberPreference(handleAudioFocusEnabledKey, true)
    var enablePictureInPicture by rememberPreference(enablePictureInPictureKey, false)
    var enablePictureInPictureAuto by rememberPreference(enablePictureInPictureAutoKey, false)
    var pipModule              by rememberPreference(pipModuleKey, PipModule.Cover)
    var jumpPrevious           by rememberPreference(jumpPreviousKey, "3")
    var notificationType       by rememberPreference(notificationTypeKey, NotificationType.Default)
    var newReleaseNotificationsEnabled by rememberPreference(newReleaseNotificationsEnabledKey, false)
    var showCommentsButton     by rememberPreference("show_comments_button", true)
    var settingsAssistantEnabled by rememberPreference(settingsAssistantEnabledKey, true)
    var settingsAssistantAutoPopup by rememberPreference(settingsAssistantAutoPopupKey, true)
    var showRescueCenterInMenu by rememberPreference(showRescueCenterInMenuKey, true)
    val logDebugEnabled by rememberPreference(logDebugEnabledKey, false)
    var showSettingsAssistant by rememberSaveable { mutableStateOf(false) }
    val settingsAssistantShown = rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(settingsAssistantEnabled, settingsAssistantAutoPopup) {
        if (settingsAssistantEnabled && settingsAssistantAutoPopup && !settingsAssistantShown.value) {
            settingsAssistantShown.value = true
            showSettingsAssistant = true
        }
    }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
            .verticalScroll(scrollState)
    ) {
        HeaderWithIcon(
            title    = stringResource(R.string.tab_general),
            iconId   = R.drawable.ic_launcher_monochrome,
            enabled  = false,
            showIcon = true,
            modifier = Modifier,
            onClick  = {}
        )

        SettingsDescription(
            text      = stringResource(R.string.general_settings_description),
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        search.ToolBarButton()
        search.SearchBar(this)

        if (showSettingsAssistant) {
            SettingsAssistantDialog(
                onDismiss = { showSettingsAssistant = false },
                onResultSelected = { result ->
                    showSettingsAssistant = false
                    if (result.tabIndex == 0) {
                        search.input = TextFieldValue(result.query)
                        search.isVisible = true
                        coroutineScope.launch {
                            scrollState.animateScrollTo(result.scrollHint)
                        }
                    } else {
                        SettingsAssistantNavigation.request(result.tabIndex)
                    }
                }
            )
        }

        // ── Confirmation dialogs (light / dark theme reset) ───────────────────
        if (resetCustomLightThemeDialog) {
            ConfirmationDialog(
                text      = stringResource(R.string.do_you_really_want_to_reset_the_custom_light_theme_colors),
                onDismiss = { resetCustomLightThemeDialog = false },
                onConfirm = {
                    resetCustomLightThemeDialog        = false
                    customThemeLight_Background0       = DefaultLightColorPalette.background0.hashCode()
                    customThemeLight_Background1       = DefaultLightColorPalette.background1.hashCode()
                    customThemeLight_Background2       = DefaultLightColorPalette.background2.hashCode()
                    customThemeLight_Background3       = DefaultLightColorPalette.background3.hashCode()
                    customThemeLight_Background4       = DefaultLightColorPalette.background4.hashCode()
                    customThemeLight_Text              = DefaultLightColorPalette.text.hashCode()
                    customThemeLight_TextSecondary     = DefaultLightColorPalette.textSecondary.hashCode()
                    customThemeLight_TextDisabled      = DefaultLightColorPalette.textDisabled.hashCode()
                    customThemeLight_IconButtonPlayer  = DefaultLightColorPalette.iconButtonPlayer.hashCode()
                    customThemeLight_Accent            = DefaultLightColorPalette.accent.hashCode()
                }
            )
        }

        if (resetCustomDarkThemeDialog) {
            ConfirmationDialog(
                text      = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
                onDismiss = { resetCustomDarkThemeDialog = false },
                onConfirm = {
                    resetCustomDarkThemeDialog        = false
                    customThemeDark_Background0       = DefaultDarkColorPalette.background0.hashCode()
                    customThemeDark_Background1       = DefaultDarkColorPalette.background1.hashCode()
                    customThemeDark_Background2       = DefaultDarkColorPalette.background2.hashCode()
                    customThemeDark_Background3       = DefaultDarkColorPalette.background3.hashCode()
                    customThemeDark_Background4       = DefaultDarkColorPalette.background4.hashCode()
                    customThemeDark_Text              = DefaultDarkColorPalette.text.hashCode()
                    customThemeDark_TextSecondary     = DefaultDarkColorPalette.textSecondary.hashCode()
                    customThemeDark_TextDisabled      = DefaultDarkColorPalette.textDisabled.hashCode()
                    customThemeDark_IconButtonPlayer  = DefaultDarkColorPalette.iconButtonPlayer.hashCode()
                    customThemeDark_Accent            = DefaultDarkColorPalette.accent.hashCode()
                }
            )
        }

        // ── Language ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(600)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(600), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.languages),
                icon    = R.drawable.discover,
                content = {
                    var showLanguageDialog by remember { mutableStateOf(false) }

                    if (search.inputValue.isBlank() || stringResource(R.string.app_language).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title   = stringResource(R.string.app_language),
                            text    = languageApp.text,
                            onClick = { showLanguageDialog = true },
                            icon    = R.drawable.translate
                        )
                    }

                    if (showLanguageDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.app_language) + ": $systemLocale",
                            selectedValue   = languageApp,
                            onValueSelected = { languageApp = it; RestartAppDialog.showDialog() },
                            valueText       = { it.text },
                            values          = Languages.values().toList(),
                            onDismiss       = { showLanguageDialog = false }
                        )
                    }

                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.settings_assistant_popup),
                        text = stringResource(R.string.settings_assistant_popup_description),
                        isChecked = settingsAssistantEnabled,
                        onCheckedChange = { settingsAssistantEnabled = it },
                        icon = R.drawable.search
                    )

                    AnimatedVisibility(visible = settingsAssistantEnabled) {
                        Column(modifier = Modifier.padding(start = 25.dp)) {
                            OtherSwitchSettingEntry(
                                title = stringResource(R.string.settings_assistant_auto_popup),
                                text = stringResource(R.string.settings_assistant_auto_popup_description),
                                isChecked = settingsAssistantAutoPopup,
                                onCheckedChange = { settingsAssistantAutoPopup = it },
                                icon = R.drawable.search_circle
                            )
                            OtherSettingsEntry(
                                title = stringResource(R.string.settings_advanced_search),
                                text = stringResource(R.string.settings_advanced_search_description),
                                onClick = { showSettingsAssistant = true },
                                icon = R.drawable.search
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DebugRescueCenterLauncher(
                        onOpenDebugSettings = {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("settings_tab_index", 7)
                            navController.navigate(app.it.fast4x.rimusic.enums.NavRoutes.settings.name)
                        }
                    )

                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.show_rescue_center_in_menu),
                        text = if (logDebugEnabled) {
                            stringResource(R.string.show_rescue_center_in_menu_debug_description)
                        } else {
                            stringResource(R.string.show_rescue_center_in_menu_description)
                        },
                        isChecked = showRescueCenterInMenu,
                        onCheckedChange = { showRescueCenterInMenu = it },
                        icon = R.drawable.rescue
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // ── Cubic Canvas ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(650)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(650), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.cubic_canvas),
                icon    = R.drawable.spotifycanvas,
                content = {
                    val context = LocalContext.current

                    var spotifyCanvasEnabled  by rememberPreference("spotifyCanvasEnabled", false)
                    var showSpotifyCanvasLogs by rememberPreference("showSpotifyCanvasLogs", false)

                    var showBetaWarning  by remember { mutableStateOf(false) }
                    var showResetDialog  by remember { mutableStateOf(false) }

                    // ── Spotify login state (read live from prefs) ─────────────
                    var isSpotifyConnected by remember { mutableStateOf(isSpotifyLoggedIn(context)) }
                    var showSpotifySessionDetails by remember { mutableStateOf(false) }
                    var showCanvasLogPanel by remember { mutableStateOf(false) }
                    var canvasLogFilter by remember { mutableStateOf("all") }
                    var showLogoutDialog   by remember { mutableStateOf(false) }
                    val spotifyLoginSuccess by navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.getStateFlow("spotify_login_success", false)
                        ?.collectAsState()
                        ?: remember { mutableStateOf(false) }

                    LaunchedEffect(spotifyLoginSuccess) {
                        if (spotifyLoginSuccess) {
                            isSpotifyConnected = isSpotifyLoggedIn(context)
                            showSpotifySessionDetails = isSpotifyConnected
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("spotify_login_success", false)
                        }
                    }


                    // ── Main toggle ────────────────────────────────────────────
                    if (search.inputValue.isBlank() || stringResource(R.string.cubic_canvas).contains(search.inputValue, true)) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.padding(bottom = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Yellow.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicText(
                                        text  = stringResource(R.string.beta_short),
                                        style = typography().xs.semiBold.copy(color = Color.Yellow)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicText(
                                    text  = stringResource(R.string.cubic_canvas_experimental_feature),
                                    style = typography().xs.copy(color = Color.Gray)
                                )
                            }

                            OtherSwitchSettingEntry(
                                title          = stringResource(R.string.cubic_canvas),
                                text           = stringResource(R.string.cubic_canvas_description),
                                isChecked      = spotifyCanvasEnabled,
                                onCheckedChange = { newValue ->
                                    if (newValue && !spotifyCanvasEnabled) showBetaWarning = true
                                    spotifyCanvasEnabled = newValue
                                },
                                icon = R.drawable.spotifycanvas
                            )
                        }
                    }

                    // ── Expanded settings (canvas enabled) ─────────────────────
                    if (spotifyCanvasEnabled) {
                        Column(modifier = Modifier.padding(start = 25.dp, top = 8.dp)) {

                            // Beta warning box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Yellow.copy(alpha = 0.1f))
                                    .border(1.dp, Color.Yellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.foundation.text.BasicText(
                                    text  = "⚠️ This is a beta feature and may be unstable, removed, or changed at any time. Use at your own risk.",
                                    style = typography().xs.copy(color = Color.Yellow.copy(alpha = 0.9f))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Spotify account login row ──────────────────────
                            if (search.inputValue.isBlank() || stringResource(R.string.cubic_canvas_spotify_account).contains(search.inputValue, true)) {
                                val spotifySessionInfo = getSpotifySessionInfo(context)
                                val dateFormatter = remember {
                                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                }
                                val now = System.currentTimeMillis()
                                val expiresAt = spotifySessionInfo?.expiresAt ?: 0L
                                val sessionStateText = when {
                                    spotifySessionInfo == null -> stringResource(R.string.cubic_canvas_no_session_saved)
                                    expiresAt <= 0L -> stringResource(R.string.spotify_connected)
                                    expiresAt <= now -> stringResource(R.string.cubic_canvas_session_estimate_expired)
                                    else -> {
                                        val hoursLeft = ((expiresAt - now) / (60L * 60L * 1000L)).coerceAtLeast(0L)
                                        stringResource(R.string.cubic_canvas_estimated_session_life, hoursLeft)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSpotifyConnected) Color(0xFF1DB954).copy(alpha = 0.12f)
                                            else colorPalette().background2
                                        )
                                        .clickable {
                                            if (isSpotifyConnected) {
                                                showSpotifySessionDetails = !showSpotifySessionDetails
                                            } else {
                                                navController.navigate(NavRoutes.spotifyLogin.name)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter          = painterResource(R.drawable.spotifycanvas),
                                            contentDescription = null,
                                            tint             = if (isSpotifyConnected) Color(0xFF1DB954)
                                                               else colorPalette().textSecondary,
                                            modifier         = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            androidx.compose.foundation.text.BasicText(
                                                text  = stringResource(R.string.cubic_canvas_spotify_account),
                                                style = typography().s.semiBold.copy(color = colorPalette().text)
                                            )
                                            androidx.compose.foundation.text.BasicText(
                                                text  = if (isSpotifyConnected)
                                                            sessionStateText
                                                        else
                                                            stringResource(R.string.cubic_canvas_tap_to_login),
                                                style = typography().xs.copy(
                                                    color = if (isSpotifyConnected) Color(0xFF1DB954)
                                                            else colorPalette().textSecondary
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSpotifyConnected) Color(0xFF1DB954).copy(alpha = 0.2f)
                                                    else colorPalette().accent.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            androidx.compose.foundation.text.BasicText(
                                                text  = if (isSpotifyConnected) stringResource(R.string.connected_uppercase) else stringResource(R.string.log_in_uppercase),
                                                style = typography().xs.semiBold.copy(
                                                    color = if (isSpotifyConnected) Color(0xFF1DB954)
                                                            else colorPalette().accent
                                                )
                                            )
                                        }
                                    }
                                }

                                if (isSpotifyConnected && showSpotifySessionDetails && spotifySessionInfo != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colorPalette().background2.copy(alpha = 0.85f))
                                            .border(
                                                1.dp,
                                                Color(0xFF1DB954).copy(alpha = 0.25f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_spotify_session_details),
                                                style = typography().s.semiBold.copy(color = colorPalette().text)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(
                                                    R.string.cubic_canvas_account_value,
                                                    spotifySessionInfo.accountName.ifBlank { stringResource(R.string.cubic_canvas_spotify_account) }
                                                ),
                                                style = typography().xs.copy(color = colorPalette().text)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_cookie_preview_value, getMaskedSpDc(context)),
                                                style = typography().xs.copy(color = colorPalette().text)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_sp_dc_value, spotifySessionInfo.spDc),
                                                style = typography().xs.copy(color = colorPalette().textSecondary)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_saved_value, dateFormatter.format(Date(spotifySessionInfo.savedAt))),
                                                style = typography().xs.copy(color = colorPalette().textSecondary)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_estimated_expiry_value, dateFormatter.format(Date(spotifySessionInfo.expiresAt))),
                                                style = typography().xs.copy(
                                                    color = if (spotifySessionInfo.expiresAt > now) Color(0xFF1DB954)
                                                    else Color.Yellow
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_renewal_uses_current_cookies),
                                                style = typography().xs.copy(color = colorPalette().textSecondary)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF1DB954).copy(alpha = 0.15f))
                                                        .clickable {
                                                            val renewed = renewSpotifySession(context)
                                                            isSpotifyConnected = isSpotifyLoggedIn(context)
                                                            if (renewed) {
                                                                showSpotifySessionDetails = true
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.cubic_canvas_session_renewed),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.cubic_canvas_no_fresh_cookie),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    androidx.compose.foundation.text.BasicText(
                                                        text = stringResource(R.string.cubic_canvas_renew_session),
                                                        style = typography().xs.semiBold.copy(color = Color(0xFF1DB954))
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colorPalette().accent.copy(alpha = 0.12f))
                                                        .clickable {
                                                            navController.navigate(NavRoutes.spotifyLogin.name)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    androidx.compose.foundation.text.BasicText(
                                                        text = stringResource(R.string.cubic_canvas_open_login),
                                                        style = typography().xs.semiBold.copy(color = colorPalette().accent)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Disconnect button (only when logged in)
                                if (isSpotifyConnected) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Red.copy(alpha = 0.08f))
                                            .clickable { showLogoutDialog = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter            = painterResource(R.drawable.close),
                                                contentDescription = null,
                                                tint               = Color.Red,
                                                modifier           = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            androidx.compose.foundation.text.BasicText(
                                                text  = stringResource(R.string.cubic_canvas_disconnect_spotify_account),
                                                style = typography().s.copy(color = Color.Red)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Debug logs toggle
                            OtherSwitchSettingEntry(
                                title           = stringResource(R.string.spotify_debug_logs),
                                text            = stringResource(R.string.cubic_canvas_show_fetching_info),
                                isChecked       = showSpotifyCanvasLogs,
                                onCheckedChange = { showSpotifyCanvasLogs = it },
                                icon            = R.drawable.information
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colorPalette().accent.copy(alpha = 0.12f))
                                        .clickable {
                                            canvasLogFilter = "all"
                                            showCanvasLogPanel = !showCanvasLogPanel || canvasLogFilter != "all"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicText(
                                        text = if (showCanvasLogPanel && canvasLogFilter == "all") "Hide logs" else "Show logs",
                                        style = typography().xs.semiBold.copy(color = colorPalette().accent)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Red.copy(alpha = 0.12f))
                                        .clickable {
                                            canvasLogFilter = "errors"
                                            showCanvasLogPanel = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicText(
                                        text = "Errors",
                                        style = typography().xs.semiBold.copy(color = Color.Red)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1DB954).copy(alpha = 0.12f))
                                        .clickable {
                                            canvasLogFilter = "success"
                                            showCanvasLogPanel = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicText(
                                        text = "Success",
                                        style = typography().xs.semiBold.copy(color = Color(0xFF1DB954))
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Yellow.copy(alpha = 0.12f))
                                        .clickable {
                                            canvasLogFilter = "search"
                                            showCanvasLogPanel = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicText(
                                        text = "⌕",
                                        style = typography().xs.semiBold.copy(color = Color.Yellow)
                                    )
                                }
                            }

                            if (showCanvasLogPanel) {
                                val filteredLogs = SpotifyCanvasState.logEntries
                                    .filter { entry ->
                                        when (canvasLogFilter) {
                                            "errors" -> entry.type == LogType.ERROR || entry.type == LogType.WARNING
                                            "success" -> entry.type == LogType.SUCCESS
                                            "search" -> {
                                                entry.message.contains("Search query:", true) ||
                                                    entry.message.contains("Track id found via query:", true) ||
                                                    entry.message.contains("Now playing metadata:", true) ||
                                                    entry.message.contains("Using Spotify track id:", true)
                                            }
                                            else -> true
                                        }
                                    }
                                    .takeLast(12)
                                    .asReversed()

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorPalette().background2.copy(alpha = 0.92f))
                                        .border(
                                            1.dp,
                                            colorPalette().textSecondary.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_logs),
                                                style = typography().s.semiBold.copy(color = colorPalette().text),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Red.copy(alpha = 0.1f))
                                                    .clickable { SpotifyCanvasState.clearLogs() }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicText(
                                                    text = stringResource(R.string.clear),
                                                    style = typography().xs.semiBold.copy(color = Color.Red)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (filteredLogs.isEmpty()) {
                                            androidx.compose.foundation.text.BasicText(
                                                text = stringResource(R.string.cubic_canvas_no_logs_for_filter_yet),
                                                style = typography().xs.copy(color = colorPalette().textSecondary)
                                            )
                                        } else {
                                            filteredLogs.forEach { entry ->
                                                val logColor = when (entry.type) {
                                                    LogType.ERROR -> Color.Red
                                                    LogType.WARNING -> Color.Yellow
                                                    LogType.SUCCESS -> Color(0xFF1DB954)
                                                    LogType.LOADING -> colorPalette().accent
                                                    LogType.INFO -> colorPalette().textSecondary
                                                }

                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    androidx.compose.foundation.text.BasicText(
                                                        text = entry.message,
                                                        style = typography().xs.copy(color = logColor)
                                                    )
                                                    androidx.compose.foundation.text.BasicText(
                                                        text = Date(entry.timestamp).toString(),
                                                        style = typography().xs.copy(color = colorPalette().textSecondary)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Report issues
                            OtherSettingsEntry(
                                title   = stringResource(R.string.cubic_canvas_report_issues),
                                text    = stringResource(R.string.cubic_canvas_open_github_issues),
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://github.com/cybruGhost/Cubic-Music/issues")
                                            }
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.cubic_canvas_cannot_open_browser), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                icon = try { R.drawable.github_icon } catch (_: Exception) { R.drawable.alert }
                            )

                            // Reset button
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Red.copy(alpha = 0.1f))
                                    .clickable { showResetDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter            = painterResource(R.drawable.refresh),
                                        contentDescription = null,
                                        tint               = Color.Red,
                                        modifier           = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.foundation.text.BasicText(
                                        text  = stringResource(R.string.cubic_canvas_reset_settings),
                                        style = typography().s.semiBold.copy(color = Color.Red)
                                    )
                                }
                            }
                        }
                    }

                    // ── Beta warning dialog ────────────────────────────────────
                    if (showBetaWarning) {
                        AlertDialog(
                            onDismissRequest = { showBetaWarning = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter            = painterResource(R.drawable.alert_circle),
                                        contentDescription = null,
                                        tint               = Color.Yellow,
                                        modifier           = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text  = stringResource(R.string.cubic_canvas_beta_warning_title),
                                        style = typography().l.semiBold.copy(color = colorPalette().text),
                                        color = colorPalette().text
                                    )
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        text     = stringResource(R.string.cubic_canvas_beta_testing),
                                        style    = typography().s.copy(color = colorPalette().text),
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        color    = colorPalette().text
                                    )
                                    Text(
                                        text     = stringResource(R.string.cubic_canvas_important_notes),
                                        style    = typography().s.semiBold.copy(color = colorPalette().text),
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        color    = colorPalette().text
                                    )
                                    Text(
                                        text = "• This feature may not work for all tracks\n" +
                                               "• You have to sign to Spotify due to their change of terms\n" +
                                               "• It requires an active internet connection\n" +
                                               "• Performance may vary on older devices\n" +
                                               "• The feature may be removed in future updates\n" +
                                               "• Data usage may be higher when enabled",
                                        style    = typography().xs.copy(color = colorPalette().text),
                                        modifier = Modifier.padding(start = 8.dp),
                                        color    = colorPalette().text
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text  = stringResource(R.string.cubic_canvas_beta_acknowledge),
                                        style = typography().xs.copy(color = colorPalette().textSecondary),
                                        color = colorPalette().textSecondary
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showBetaWarning = false }) {
                                    Text(text = stringResource(R.string.cubic_canvas_i_understand), color = colorPalette().accent)
                                }
                            },
                            containerColor    = colorPalette().background1,
                            titleContentColor = colorPalette().text,
                            textContentColor  = colorPalette().text
                        )
                    }

                    // ── Logout confirmation dialog ──────────────────────────────
                    if (showLogoutDialog) {
                        ConfirmationDialog(
                            text      = stringResource(R.string.cubic_canvas_disconnect_confirm),
                            onDismiss = { showLogoutDialog = false },
                            onConfirm = {
                                clearSpotifySession(context)
                                isSpotifyConnected = false
                                showLogoutDialog   = false
                                Toast.makeText(context, context.getString(R.string.cubic_canvas_spotify_disconnected), Toast.LENGTH_SHORT).show()
                            },
                            confirmText = stringResource(R.string.cubic_canvas_disconnect)
                        )
                    }

                    // ── Reset canvas settings dialog ───────────────────────────
                    if (showResetDialog) {
                        ConfirmationDialog(
                            text      = stringResource(R.string.cubic_canvas_reset_confirm),
                            onDismiss = { showResetDialog = false },
                            onConfirm = {
                                spotifyCanvasEnabled  = false
                                showSpotifyCanvasLogs = false
                                showResetDialog       = false
                            },
                            confirmText = stringResource(R.string.cubic_canvas_reset)
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback source is Innertube-only now; keep this hidden so settings do not expose fake source choices.
        AnimatedVisibility(
            visible = false,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(675)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(675), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = "Playback Source",
                icon    = R.drawable.refresh,
                content = {
                    var innertubePlayerSource by rememberPreference(
                        innertubePlayerSourceKey,
                        InnertubePlayerSource.OldInnertube
                    )
                    var showInnertubeSourceDialog by remember { mutableStateOf(false) }
                    val playbackSourceStatus by PlaybackSourceMonitor.status.collectAsState()
                    if (search.inputValue.isBlank() || "Playback Source".contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title = "Playback source",
                            text = "Locked to ${innertubePlayerSource.text}",
                            onClick = { showInnertubeSourceDialog = true },
                            icon = R.drawable.ytmusic
                        )
                        SettingsDescription(
                            text = "Playback is locked to Innertube only. Lovable, MetroList, Invidious, Piped, Android, and iOS source fallbacks are disabled.",
                            modifier = Modifier.padding(start = 25.dp, top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                        if (showInnertubeSourceDialog) {
                            ValueSelectorDialog(
                                title = "Playback source",
                                selectedValue = innertubePlayerSource,
                                onValueSelected = {
                                    innertubePlayerSource = it
                                    showInnertubeSourceDialog = false
                                },
                                valueText = { it.text },
                                values = listOf(InnertubePlayerSource.OldInnertube),
                                onDismiss = { showInnertubeSourceDialog = false }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 25.dp, top = 10.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .background(colorPalette().background2.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        when (playbackSourceStatus.source) {
                                            PlaybackSourceKind.Unknown -> colorPalette().textDisabled
                                            else -> Color(0xFF34C759)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicText(
                                text = buildString {
                                    append("Selected source: ")
                                    append(innertubePlayerSource.text)
                                    if (playbackSourceStatus.isFallback) append(" fallback")
                                },
                                style = typography().xxs.copy(color = colorPalette().text)
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Comments Button ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(687)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(687), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = "Cool buttons",
                icon    = R.drawable.comments,
                content = {
                    var showCommentsButton by rememberPreference("show_comments_button", true)
                    var showVideoButton by rememberPreference(showButtonPlayerVideoKey, false)
                    var showLyricsSourceSwitcher by rememberPreference(showLyricsSourceSwitcherKey, true)
                    var showPlayerPlaybackContext by rememberPreference(showPlayerPlaybackContextKey, true)
                    var showPlayerOutputDevice by rememberPreference(showPlayerOutputDeviceKey, true)
                    if (search.inputValue.isBlank() || "Cool buttons comments video".contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = "Show comments button",
                            text            = "Display comments button on album art",
                            isChecked       = showCommentsButton,
                            onCheckedChange = { showCommentsButton = it },
                            icon            = R.drawable.comments
                        )
                        SettingsDescription(
                            text      = "Show/hide the comments button in the player screen",
                            modifier  = Modifier.padding(start = 25.dp, top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                        OtherSwitchSettingEntry(
                            title           = "Show video button",
                            text            = "Display instant video switch on album art",
                            isChecked       = showVideoButton,
                            onCheckedChange = { showVideoButton = it },
                            icon            = R.drawable.video
                        )
                        OtherSwitchSettingEntry(
                            title           = "Show lyrics sources",
                            text            = "Display source pills in the lyrics screen",
                            isChecked       = showLyricsSourceSwitcher,
                            onCheckedChange = { showLyricsSourceSwitcher = it },
                            icon            = R.drawable.song_lyrics
                        )
                        OtherSwitchSettingEntry(
                            title           = "Show playing source",
                            text            = "Show where the current queue started from",
                            isChecked       = showPlayerPlaybackContext,
                            onCheckedChange = { showPlayerPlaybackContext = it },
                            icon            = R.drawable.playlist
                        )
                        OtherSwitchSettingEntry(
                            title           = "Show listening device",
                            text            = "Display the active Bluetooth or wired output",
                            isChecked       = showPlayerOutputDevice,
                            onCheckedChange = { showPlayerOutputDevice = it },
                            icon            = R.drawable.music
                        )
                        ImportantSettingsDescription(text = "Changes take effect immediately")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Notifications ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(700)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(700), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.notifications),
                icon    = R.drawable.notification2,
                content = {
                    var showNotificationTypeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.notification_type).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title   = stringResource(R.string.notification_type),
                            text    = notificationType.textName,
                            onClick = { showNotificationTypeDialog = true },
                            icon    = R.drawable.notification1
                        )
                        ImportantSettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
                    }
                    if (showNotificationTypeDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.notification_type_info),
                            selectedValue   = notificationType,
                            onValueSelected = { notificationType = it; RestartAppDialog.showDialog() },
                            valueText       = { it.textName },
                            values          = NotificationType.values().toList(),
                            onDismiss       = { showNotificationTypeDialog = false }
                        )
                    }
                    if (search.inputValue.isBlank() || stringResource(R.string.new_releases_notifications).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.new_releases_notifications),
                            text = stringResource(R.string.new_releases_notifications_description),
                            isChecked = newReleaseNotificationsEnabled,
                            onCheckedChange = { newReleaseNotificationsEnabled = it },
                            icon = R.drawable.album
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Playback ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(800)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(800), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.playback),
                icon    = R.drawable.play_forward,
                content = {
                    var showJumpPreviousDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.jump_previous).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title   = stringResource(R.string.jump_previous),
                            text    = jumpPrevious,
                            onClick = { showJumpPreviousDialog = true },
                            icon    = R.drawable.play_skip_back
                        )
                    }
                    if (showJumpPreviousDialog) {
                        InputTextDialog(
                            title       = stringResource(R.string.jump_previous_blank),
                            value       = jumpPrevious,
                            placeholder = stringResource(R.string.jump_previous_blank),
                            onDismiss   = { showJumpPreviousDialog = false },
                            setValue    = { if (TextUtils.isDigitsOnly(it)) jumpPrevious = it }
                        )
                    }

                    var showMinListeningTimeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.min_listening_time).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.min_listening_time),
                            text  = when (exoPlayerMinTimeForEvent) {
                                ExoPlayerMinTimeForEvent.`10s` -> "10s"
                                ExoPlayerMinTimeForEvent.`15s` -> "15s"
                                ExoPlayerMinTimeForEvent.`20s` -> "20s"
                                ExoPlayerMinTimeForEvent.`30s` -> "30s"
                                ExoPlayerMinTimeForEvent.`40s` -> "40s"
                                ExoPlayerMinTimeForEvent.`60s` -> "60s"
                            },
                            onClick = { showMinListeningTimeDialog = true },
                            icon    = R.drawable.time
                        )
                    }
                    if (showMinListeningTimeDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.is_min_list_time_for_tips_or_quick_pics),
                            selectedValue   = exoPlayerMinTimeForEvent,
                            onValueSelected = { exoPlayerMinTimeForEvent = it },
                            valueText       = {
                                when (it) {
                                    ExoPlayerMinTimeForEvent.`10s` -> "10s"
                                    ExoPlayerMinTimeForEvent.`15s` -> "15s"
                                    ExoPlayerMinTimeForEvent.`20s` -> "20s"
                                    ExoPlayerMinTimeForEvent.`30s` -> "30s"
                                    ExoPlayerMinTimeForEvent.`40s` -> "40s"
                                    ExoPlayerMinTimeForEvent.`60s` -> "60s"
                                }
                            },
                            values    = ExoPlayerMinTimeForEvent.values().toList(),
                            onDismiss = { showMinListeningTimeDialog = false }
                        )
                    }

                    var showExcludeSongsDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.exclude_songs_with_duration_limit).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.exclude_songs_with_duration_limit),
                            text  = when (excludeSongWithDurationLimit) {
                                DurationInMinutes.Disabled -> stringResource(R.string.vt_disabled)
                                DurationInMinutes.`3`  -> "3m"
                                DurationInMinutes.`5`  -> "5m"
                                DurationInMinutes.`10` -> "10m"
                                DurationInMinutes.`15` -> "15m"
                                DurationInMinutes.`20` -> "20m"
                                DurationInMinutes.`25` -> "25m"
                                DurationInMinutes.`30` -> "30m"
                                DurationInMinutes.`60` -> "60m"
                            },
                            onClick = { showExcludeSongsDialog = true },
                            icon    = R.drawable.playbackduration
                        )
                    }
                    if (showExcludeSongsDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.exclude_songs_with_duration_limit_description),
                            selectedValue   = excludeSongWithDurationLimit,
                            onValueSelected = { excludeSongWithDurationLimit = it },
                            valueText       = {
                                when (it) {
                                    DurationInMinutes.Disabled -> stringResource(R.string.vt_disabled)
                                    DurationInMinutes.`3`  -> "3m"
                                    DurationInMinutes.`5`  -> "5m"
                                    DurationInMinutes.`10` -> "10m"
                                    DurationInMinutes.`15` -> "15m"
                                    DurationInMinutes.`20` -> "20m"
                                    DurationInMinutes.`25` -> "25m"
                                    DurationInMinutes.`30` -> "30m"
                                    DurationInMinutes.`60` -> "60m"
                                }
                            },
                            values    = DurationInMinutes.values().toList(),
                            onDismiss = { showExcludeSongsDialog = false }
                        )
                    }

                    var showPauseBetweenSongsDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.pause_between_songs).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.pause_between_songs),
                            text  = when (pauseBetweenSongs) {
                                PauseBetweenSongs.`0`  -> "0s"
                                PauseBetweenSongs.`5`  -> "5s"
                                PauseBetweenSongs.`10` -> "10s"
                                PauseBetweenSongs.`15` -> "15s"
                                PauseBetweenSongs.`20` -> "20s"
                                PauseBetweenSongs.`30` -> "30s"
                                PauseBetweenSongs.`40` -> "40s"
                                PauseBetweenSongs.`50` -> "50s"
                                PauseBetweenSongs.`60` -> "60s"
                            },
                            onClick = { showPauseBetweenSongsDialog = true },
                            icon    = R.drawable.pause
                        )
                    }
                    if (showPauseBetweenSongsDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.pause_between_songs_description),
                            selectedValue   = pauseBetweenSongs,
                            onValueSelected = { pauseBetweenSongs = it },
                            valueText       = {
                                when (it) {
                                    PauseBetweenSongs.`0`  -> "0s"
                                    PauseBetweenSongs.`5`  -> "5s"
                                    PauseBetweenSongs.`10` -> "10s"
                                    PauseBetweenSongs.`15` -> "15s"
                                    PauseBetweenSongs.`20` -> "20s"
                                    PauseBetweenSongs.`30` -> "30s"
                                    PauseBetweenSongs.`40` -> "40s"
                                    PauseBetweenSongs.`50` -> "50s"
                                    PauseBetweenSongs.`60` -> "60s"
                                }
                            },
                            values    = PauseBetweenSongs.values().toList(),
                            onDismiss = { showPauseBetweenSongsDialog = false }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Player Controls ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(900)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(900), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.player_controls),
                icon    = R.drawable.player_control,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.player_pause_on_volume_zero).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.player_pause_on_volume_zero),
                            text            = stringResource(R.string.info_pauses_player_when_volume_zero),
                            isChecked       = isPauseOnVolumeZeroEnabled,
                            onCheckedChange = { isPauseOnVolumeZeroEnabled = it },
                            icon            = R.drawable.volume_up
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.player_keep_minimized).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.player_keep_minimized),
                            text            = stringResource(R.string.when_click_on_a_song_player_start_minimized),
                            isChecked       = keepPlayerMinimized,
                            onCheckedChange = { keepPlayerMinimized = it },
                            icon            = R.drawable.maximize
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.player_collapsed_disable_swiping_down).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.player_collapsed_disable_swiping_down),
                            text            = stringResource(R.string.avoid_closing_the_player_cleaning_queue_by_swiping_down),
                            isChecked       = disableClosingPlayerSwipingDown,
                            onCheckedChange = { disableClosingPlayerSwipingDown = it },
                            icon            = R.drawable.reorder
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.player_auto_load_songs_in_queue).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.player_auto_load_songs_in_queue),
                            text            = stringResource(R.string.player_auto_load_songs_in_queue_description),
                            isChecked       = autoLoadSongsInQueue,
                            onCheckedChange = { autoLoadSongsInQueue = it; restartService = true },
                            icon            = R.drawable.playlist
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Queue Management ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(1000)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(1000), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.queue_management),
                icon    = R.drawable.playlist,
                content = {
                    var showMaxSongsDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.max_songs_in_queue).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.max_songs_in_queue),
                            text  = when (maxSongsInQueue) {
                                MaxSongs.Unlimited -> stringResource(R.string.unlimited)
                                MaxSongs.`50`   -> MaxSongs.`50`.name
                                MaxSongs.`100`  -> MaxSongs.`100`.name
                                MaxSongs.`200`  -> MaxSongs.`200`.name
                                MaxSongs.`300`  -> MaxSongs.`300`.name
                                MaxSongs.`500`  -> MaxSongs.`500`.name
                                MaxSongs.`1000` -> MaxSongs.`1000`.name
                                MaxSongs.`2000` -> MaxSongs.`2000`.name
                                MaxSongs.`3000` -> MaxSongs.`3000`.name
                            },
                            onClick = { showMaxSongsDialog = true },
                            icon    = R.drawable.music_file
                        )
                    }
                    if (showMaxSongsDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.max_songs_in_queue),
                            selectedValue   = maxSongsInQueue,
                            onValueSelected = { maxSongsInQueue = it },
                            valueText       = {
                                when (it) {
                                    MaxSongs.Unlimited -> stringResource(R.string.unlimited)
                                    MaxSongs.`50`   -> MaxSongs.`50`.name
                                    MaxSongs.`100`  -> MaxSongs.`100`.name
                                    MaxSongs.`200`  -> MaxSongs.`200`.name
                                    MaxSongs.`300`  -> MaxSongs.`300`.name
                                    MaxSongs.`500`  -> MaxSongs.`500`.name
                                    MaxSongs.`1000` -> MaxSongs.`1000`.name
                                    MaxSongs.`2000` -> MaxSongs.`2000`.name
                                    MaxSongs.`3000` -> MaxSongs.`3000`.name
                                }
                            },
                            values    = MaxSongs.values().toList(),
                            onDismiss = { showMaxSongsDialog = false }
                        )
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.discover).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.discover),
                            text            = stringResource(R.string.discoverinfo),
                            isChecked       = discoverIsEnabled,
                            onCheckedChange = { discoverIsEnabled = it },
                            icon            = R.drawable.search
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.playlistindicator).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.playlistindicator),
                            text            = stringResource(R.string.playlistindicatorinfo),
                            isChecked       = playlistindicator,
                            onCheckedChange = { playlistindicator = it },
                            icon            = R.drawable.playlist
                        )

                    var showNowPlayingIndicatorDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.now_playing_indicator).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title   = stringResource(R.string.now_playing_indicator),
                            text    = nowPlayingIndicator.text,
                            onClick = { showNowPlayingIndicatorDialog = true },
                            icon    = R.drawable.playing_indicator
                        )
                    }
                    if (showNowPlayingIndicatorDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.now_playing_indicator),
                            selectedValue   = nowPlayingIndicator,
                            onValueSelected = { nowPlayingIndicator = it },
                            valueText       = { it.text },
                            values          = MusicAnimationType.values().toList(),
                            onDismiss       = { showNowPlayingIndicatorDialog = false }
                        )
                    }
                    if (search.inputValue.isBlank() || stringResource(R.string.now_playing_progress_ring).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.now_playing_progress_ring),
                            text = stringResource(R.string.now_playing_progress_ring_info),
                            isChecked = nowPlayingProgressRing,
                            onCheckedChange = { nowPlayingProgressRing = it },
                            icon = R.drawable.playing_indicator
                        )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── App Behavior ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(1100)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(1100), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.app_behavior),
                icon    = R.drawable.settings,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.resume_playback).contains(search.inputValue, true)) {
                        if (isAtLeastAndroid6) {
                            OtherSwitchSettingEntry(
                                title           = stringResource(R.string.resume_playback),
                                text            = stringResource(R.string.when_device_is_connected),
                                isChecked       = resumePlaybackWhenDeviceConnected,
                                onCheckedChange = { resumePlaybackWhenDeviceConnected = it; restartService = true },
                                icon            = R.drawable.play
                            )
                            RestartPlayerService(restartService, onRestart = { restartService = false })
                        }
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.persistent_queue).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.persistent_queue),
                            text            = stringResource(R.string.save_and_restore_playing_songs),
                            isChecked       = persistentQueue,
                            onCheckedChange = { persistentQueue = it; restartService = true },
                            icon            = R.drawable.download
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                        AnimatedVisibility(visible = persistentQueue) {
                            Column(modifier = Modifier.padding(start = 25.dp)) {
                                OtherSwitchSettingEntry(
                                    title           = stringResource(R.string.resume_playback_on_start),
                                    text            = stringResource(R.string.resume_automatically_when_app_opens),
                                    isChecked       = resumePlaybackOnStart,
                                    onCheckedChange = { resumePlaybackOnStart = it; restartService = true },
                                    icon            = R.drawable.play
                                )
                                RestartPlayerService(restartService, onRestart = { restartService = false })
                            }
                        }
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.close_app_with_back_button).contains(search.inputValue, true)) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            OtherSwitchSettingEntry(
                                title           = stringResource(R.string.close_app_with_back_button),
                                text            = stringResource(R.string.when_you_use_the_back_button_from_the_home_page),
                                isChecked       = closeWithBackButton,
                                onCheckedChange = { closeWithBackButton = it; restartActivity = true },
                                icon            = R.drawable.close
                            )
                        }
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.close_background_player).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.close_background_player),
                            text            = stringResource(R.string.when_app_swipe_out_from_task_manager),
                            isChecked       = closebackgroundPlayer,
                            onCheckedChange = { closebackgroundPlayer = it; restartService = true },
                            icon            = R.drawable.close
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.skip_media_on_error).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.skip_media_on_error),
                            text            = stringResource(R.string.skip_media_on_error_description),
                            isChecked       = skipMediaOnError,
                            onCheckedChange = { skipMediaOnError = it; restartService = true },
                            icon            = R.drawable.alert_circle
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Audio Effects ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(1200)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(1200), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.audio_effects),
                icon    = R.drawable.sound_effect,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.skip_silence).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.skip_silence),
                            text            = stringResource(R.string.skip_silent_parts_during_playback),
                            isChecked       = skipSilence,
                            onCheckedChange = { skipSilence = it },
                            icon            = R.drawable.pause
                        )
                        AnimatedVisibility(visible = skipSilence) {
                            val initialValue by remember { derivedStateOf { minimumSilenceDuration.toFloat() / 1000L } }
                            var newValue     by remember(initialValue) { mutableFloatStateOf(initialValue) }
                            Column(modifier = Modifier.padding(start = 25.dp)) {
                                SliderSettingsEntry(
                                    title          = stringResource(R.string.minimum_silence_length),
                                    text           = stringResource(R.string.minimum_silence_length_description),
                                    state          = newValue,
                                    onSlide        = { newValue = it },
                                    onSlideComplete = {
                                        minimumSilenceDuration = newValue.toLong() * 1000L
                                        restartService         = true
                                    },
                                    toDisplay = { stringResource(R.string.format_ms, it.toLong()) },
                                    range     = 1.00f..2000.000f
                                )
                                RestartPlayerService(restartService, onRestart = { restartService = false })
                            }
                        }
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.loudness_normalization).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.loudness_normalization),
                            text            = stringResource(R.string.autoadjust_the_volume),
                            isChecked       = volumeNormalization,
                            onCheckedChange = { volumeNormalization = it },
                            icon            = R.drawable.volume_up
                        )
                        AnimatedVisibility(visible = volumeNormalization) {
                            val initialValue by remember { derivedStateOf { loudnessBaseGain } }
                            var newValue     by remember(initialValue) { mutableFloatStateOf(initialValue) }
                            Column(modifier = Modifier.padding(start = 25.dp)) {
                                SliderSettingsEntry(
                                    title           = stringResource(R.string.settings_loudness_base_gain),
                                    text            = stringResource(R.string.settings_target_gain_loudness_info),
                                    state           = newValue,
                                    onSlide         = { newValue = it },
                                    onSlideComplete = { loudnessBaseGain = newValue },
                                    toDisplay       = { "%.1f dB".format(loudnessBaseGain).replace(",", ".") },
                                    range           = -20f..20f
                                )
                            }
                        }
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.settings_audio_bass_boost).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.settings_audio_bass_boost),
                            text            = "",
                            isChecked       = bassboostEnabled,
                            onCheckedChange = { bassboostEnabled = it },
                            icon            = R.drawable.equalizer
                        )
                        AnimatedVisibility(visible = bassboostEnabled) {
                            val initialValue by remember { derivedStateOf { bassboostLevel } }
                            var newValue     by remember(initialValue) { mutableFloatStateOf(initialValue) }
                            Column(modifier = Modifier.padding(start = 25.dp)) {
                                SliderSettingsEntry(
                                    title           = stringResource(R.string.settings_bass_boost_level),
                                    text            = "",
                                    state           = newValue,
                                    onSlide         = { newValue = it },
                                    onSlideComplete = { bassboostLevel = newValue },
                                    toDisplay       = { "%.1f".format(bassboostLevel).replace(",", ".") },
                                    range           = 0f..1f
                                )
                            }
                        }
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.audio_fade_title).contains(search.inputValue, true)) {
                        var showFadeDurationDialog by remember { mutableStateOf(false) }
                        OtherSettingsEntry(
                            title = stringResource(R.string.audio_fade_title),
                            text  = when (playbackFadeAudioDuration) {
                                DurationInMilliseconds.Disabled -> stringResource(R.string.disabled)
                                else                            -> playbackFadeAudioDuration.text
                            },
                            onClick = { showFadeDurationDialog = true },
                            icon    = R.drawable.volume_up
                        )
                        if (showFadeDurationDialog) {
                            ValueSelectorDialog(
                                title           = stringResource(R.string.fade_duration_title),
                                selectedValue   = playbackFadeAudioDuration,
                                onValueSelected = { playbackFadeAudioDuration = it },
                                valueText       = {
                                    when (it) {
                                        DurationInMilliseconds.Disabled -> stringResource(R.string.disabled)
                                        else                            -> it.text
                                    }
                                },
                                values    = DurationInMilliseconds.values().toList(),
                                onDismiss = { showFadeDurationDialog = false }
                            )
                        }
                        SettingsDescription(
                            text      = stringResource(R.string.audio_fade_description),
                            modifier  = Modifier.padding(start = 25.dp, top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    var showAudioReverbDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.settings_audio_reverb).contains(search.inputValue, true)) {
                        OtherSettingsEntry(
                            title   = stringResource(R.string.settings_audio_reverb),
                            text    = audioReverb.textName,
                            onClick = { showAudioReverbDialog = true },
                            icon    = R.drawable.reverb
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                    if (showAudioReverbDialog) {
                        ValueSelectorDialog(
                            title           = stringResource(R.string.settings_audio_reverb_info_apply_a_depth_effect_to_the_audio),
                            selectedValue   = audioReverb,
                            onValueSelected = { audioReverb = it; restartService = true },
                            valueText       = { it.textName },
                            values          = PresetsReverb.values().toList(),
                            onDismiss       = { showAudioReverbDialog = false }
                        )
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.settings_audio_focus).contains(search.inputValue, true))
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.settings_audio_focus),
                            text            = stringResource(R.string.settings_audio_focus_info),
                            isChecked       = audioFocusEnabled,
                            onCheckedChange = { audioFocusEnabled = it },
                            icon            = R.drawable.focus_audio
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.equalizer).contains(search.inputValue, true))
                        OtherSettingsEntry(
                            title   = stringResource(R.string.equalizer),
                            text    = stringResource(R.string.interact_with_the_system_equalizer),
                            onClick = launchEqualizer,
                            icon    = R.drawable.equalizer
                        )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Gestures & Events ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(1300)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(1300), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.gestures_events),
                icon    = R.drawable.gesture,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.event_volumekeys).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.event_volumekeys),
                            text            = stringResource(R.string.event_volumekeysinfo),
                            isChecked       = useVolumeKeysToChangeSong,
                            onCheckedChange = { useVolumeKeysToChangeSong = it; restartService = true },
                            icon            = R.drawable.volume_control
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.event_shake).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.event_shake),
                            text            = stringResource(R.string.shake_to_change_song),
                            isChecked       = shakeEventEnabled,
                            onCheckedChange = { shakeEventEnabled = it; restartService = true },
                            icon            = R.drawable.shake_gesture
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Picture in Picture ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter   = androidx.compose.animation.fadeIn(animationSpec = tween(1400)) +
                      androidx.compose.animation.scaleIn(animationSpec = tween(1400), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title   = stringResource(R.string.picture_in_picture),
                icon    = R.drawable.video,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.settings_enable_pip).contains(search.inputValue, true)) {
                        OtherSwitchSettingEntry(
                            title           = stringResource(R.string.settings_enable_pip),
                            text            = "",
                            isChecked       = enablePictureInPicture,
                            onCheckedChange = { enablePictureInPicture = it; restartActivity = true },
                            icon            = R.drawable.logo_youtube
                        )
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                        AnimatedVisibility(visible = enablePictureInPicture) {
                            Column(modifier = Modifier.padding(start = 25.dp)) {
                                var showPipModuleDialog by remember { mutableStateOf(false) }
                                OtherSettingsEntry(
                                    title = stringResource(R.string.settings_pip_module),
                                    text  = when (pipModule) { PipModule.Cover -> stringResource(R.string.pipmodule_cover) },
                                    onClick = { showPipModuleDialog = true },
                                    icon    = R.drawable.logo_youtube
                                )
                                if (showPipModuleDialog) {
                                    ValueSelectorDialog(
                                        title           = stringResource(R.string.settings_pip_module),
                                        selectedValue   = pipModule,
                                        onValueSelected = { pipModule = it; restartActivity = true },
                                        valueText       = { when (it) { PipModule.Cover -> stringResource(R.string.pipmodule_cover) } },
                                        values          = PipModule.values().toList(),
                                        onDismiss       = { showPipModuleDialog = false }
                                    )
                                }
                                if (isAtLeastAndroid12) {
                                    OtherSwitchSettingEntry(
                                        title           = stringResource(R.string.settings_enable_pip_auto),
                                        text            = stringResource(R.string.pip_info_from_android_12_pip_can_be_automatically_enabled),
                                        isChecked       = enablePictureInPictureAuto,
                                        onCheckedChange = { enablePictureInPictureAuto = it; restartActivity = true },
                                        icon            = R.drawable.logo_youtube
                                    )
                                }
                                RestartActivity(restartActivity, onRestart = { restartActivity = false })
                            }
                        }
                    }
                }
            )
        }

        SettingsGroupSpacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}

private data class SettingsAssistantResult(
    val title: String,
    val section: String,
    val query: String,
    val scrollHint: Int,
    val tabIndex: Int = 0,
)

@Composable
private fun SettingsAssistantDialog(
    onDismiss: () -> Unit,
    onResultSelected: (SettingsAssistantResult) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) {
        val normalizedQuery = query.trim()
        settingsAssistantResults()
            .map { result -> result to result.matchScore(normalizedQuery) }
            .filter { (_, score) ->
                normalizedQuery.isBlank() || score > 0
            }
            .sortedWith(
                compareByDescending<Pair<SettingsAssistantResult, Int>> { it.second }
                    .thenBy { it.first.tabIndex }
                    .thenBy { it.first.title }
            )
            .map { it.first }
            .take(7)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.92f),
        containerColor = colorPalette().background1.copy(alpha = 0.92f),
        titleContentColor = colorPalette().text,
        textContentColor = colorPalette().textSecondary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(colorPalette().accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("*", color = colorPalette().accent, style = typography().m.semiBold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.settings_assistant_title), color = colorPalette().text, style = typography().s.semiBold)
                    Text(
                        text = "Search every settings screen",
                        color = colorPalette().accent,
                        style = typography().xxs
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorPalette().background0.copy(alpha = 0.34f))
                    .padding(10.dp)
            ) {
                Text(stringResource(R.string.settings_assistant_prompt), style = typography().xs)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorPalette().text,
                        unfocusedTextColor = colorPalette().text,
                        focusedContainerColor = colorPalette().background2,
                        unfocusedContainerColor = colorPalette().background2,
                        focusedBorderColor = colorPalette().accent,
                        unfocusedBorderColor = colorPalette().textSecondary.copy(alpha = 0.45f),
                        cursorColor = colorPalette().accent,
                        focusedLabelColor = colorPalette().accent,
                        unfocusedLabelColor = colorPalette().textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                results.forEach { result ->
                    TextButton(
                        onClick = { onResultSelected(result) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorPalette().background2.copy(alpha = 0.42f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(result.title, color = colorPalette().text, style = typography().xs.semiBold)
                            Text(result.section, color = colorPalette().textSecondary, style = typography().xxs)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (results.isEmpty()) {
                    Text(stringResource(R.string.no_results_found), style = typography().xs)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

private fun settingsAssistantResults() = listOf(
    SettingsAssistantResult("App language", "GeneralSettings.kt > Languages", "App language", 400, 0),
    SettingsAssistantResult("Settings assistant", "GeneralSettings.kt > Languages", "Settings assistant advanced search", 450, 0),
    SettingsAssistantResult("Cubic Canvas", "GeneralSettings.kt > Cubic Canvas", "Cubic Canvas Spotify Canvas", 900, 0),
    SettingsAssistantResult("New release notifications", "GeneralSettings.kt > Notifications", "New release notifications app alerts", 2400, 0),
    SettingsAssistantResult("Auto load songs in queue", "GeneralSettings.kt > Player", "Auto load songs in queue", 3100, 0),
    SettingsAssistantResult("Discover", "GeneralSettings.kt > Player", "Discover queue radio recommendations", 3300, 0),
    SettingsAssistantResult("Now playing indicator", "GeneralSettings.kt > Player", "Now Playing Indicator locator", 3400, 0),
    SettingsAssistantResult("Skip media on error", "GeneralSettings.kt > Playback", "Skip media on error retry playback", 3900, 0),
    SettingsAssistantResult("Audio bass boost", "GeneralSettings.kt > Audio", "Audio bass boost equalizer", 4300, 0),
    SettingsAssistantResult("Audio focus", "GeneralSettings.kt > Audio", "Audio focus calls interruptions", 4600, 0),
    SettingsAssistantResult("Picture-in-picture", "GeneralSettings.kt > Picture-in-picture", "Picture in picture miniplayer", 5200, 0),
    SettingsAssistantResult("Theme mode", "UiSettings.kt > Theme", "theme dark light colors palette", 0, 1),
    SettingsAssistantResult("Home tabs", "UiSettings.kt > Home", "home tabs quick picks songs artists albums", 0, 1),
    SettingsAssistantResult("Show On Device", "UiSettings.kt > Home", "show on device songs tab", 0, 1),
    SettingsAssistantResult("Player appearance", "AppearanceSettings.kt", "player appearance thumbnail lyrics controls", 0, 2),
    SettingsAssistantResult("Lyrics appearance", "AppearanceSettings.kt > Lyrics", "lyrics karaoke size alignment animation", 0, 2),
    SettingsAssistantResult("Mini player", "AppearanceSettings.kt > Player", "mini player appearance controls", 0, 2),
    SettingsAssistantResult("Quick picks", "AIRecommendationSettings.kt > Home recommendations", "quick picks recommendations casual discover", 0, 3),
    SettingsAssistantResult("AI recommendations", "AIRecommendationSettings.kt", "ai recommendations discovery moods", 0, 3),
    SettingsAssistantResult("Cache location", "DataSettings.kt > Cache", "cache location private system storage", 0, 4),
    SettingsAssistantResult("Image cache", "DataSettings.kt > Cache", "image cache thumbnails coil", 0, 4),
    SettingsAssistantResult("Song cache", "DataSettings.kt > Cache", "song cache exoplayer cached songs", 0, 4),
    SettingsAssistantResult("Downloads", "DataSettings.kt > Downloads", "downloads storage downloaded songs", 0, 4),
    SettingsAssistantResult("Backup and restore", "DataSettings.kt > Backup", "backup restore export import database settings migration", 0, 4),
    SettingsAssistantResult("Restart player", "DataSettings.kt > Player", "restart player service", 0, 4),
    SettingsAssistantResult("YouTube Music login", "AccountsSettings.kt > YouTube Music", "youtube music ytm account cookies login", 0, 5),
    SettingsAssistantResult("Discord presence", "AccountsSettings.kt > Discord", "discord presence rich presence login", 0, 5),
    SettingsAssistantResult("Spotify", "AccountsSettings.kt > Spotify", "spotify account canvas login", 0, 5),
    SettingsAssistantResult("Proxy", "NetworkSettings.kt > Proxy", "proxy hostname port mode", 0, 6),
    SettingsAssistantResult("Network quality", "NetworkSettings.kt", "network quality wifi data internet", 0, 6),
    SettingsAssistantResult("On Device songs", "OtherSettings.kt > On Device", "On Device songs local files", 0, 7),
    SettingsAssistantResult("On Device folders", "OtherSettings.kt > On Device", "folders on device blacklist default folder", 0, 7),
    SettingsAssistantResult("Battery optimization", "OtherSettings.kt > Android", "battery optimization background", 0, 7),
    SettingsAssistantResult("Debug logs", "OtherSettings.kt > Debug", "debug logs logcat", 0, 7),
    SettingsAssistantResult("About app", "About.kt", "about changelog version update", 0, 8),
)

private fun SettingsAssistantResult.matchScore(rawQuery: String): Int {
    if (rawQuery.isBlank()) return 1
    val searchable = "$title $section $query".lowercase()
    val terms = rawQuery.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return 1

    return terms.sumOf { term ->
        when {
            title.lowercase() == term -> 80
            title.lowercase().contains(term) -> 50
            section.lowercase().contains(term) -> 35
            query.lowercase().contains(term) -> 25
            searchable.contains(term) -> 10
            else -> 0
        }
    }
}

package it.fast4x.rimusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.autoDownloadSongKey
import it.fast4x.rimusic.utils.autoDownloadSongWhenAlbumBookmarkedKey
import it.fast4x.rimusic.utils.autoDownloadSongWhenLikedKey
import it.fast4x.rimusic.utils.isConnectionMeteredEnabledKey
import it.fast4x.rimusic.utils.navigationBarPositionKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.enums.AudioQualityFormat
import it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog

import it.fast4x.rimusic.utils.audioQualityFormatKey
import it.fast4x.rimusic.utils.RestartPlayerService

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun NetworkSettings(
    navController: NavController
) {
    var isConnectionMeteredEnabled by rememberPreference(isConnectionMeteredEnabledKey, true)
    var autoDownloadSong by rememberPreference(autoDownloadSongKey, false)
    var autoDownloadSongWhenLiked by rememberPreference(autoDownloadSongWhenLikedKey, false)
    var autoDownloadSongWhenAlbumBookmarked by rememberPreference(autoDownloadSongWhenAlbumBookmarkedKey, false)
    var audioQualityFormat by rememberPreference(audioQualityFormatKey, AudioQualityFormat.Auto)
    var restartService by rememberSaveable { mutableStateOf(false) }
    var showAudioQualityDialog by rememberSaveable { mutableStateOf(false) }
    
    var navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)

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
            .verticalScroll(rememberScrollState())
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.tab_network),
            iconId = R.drawable.network,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.network_settings_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Settings Section
        SettingsSectionCard(
            title = stringResource(R.string.connection_settings),
            icon = R.drawable.network,
            content = {
                OtherSwitchSettingEntry(
                    title = stringResource(R.string.enable_connection_metered),
                    text = stringResource(R.string.info_enable_connection_metered),
                    isChecked = isConnectionMeteredEnabled,
                    onCheckedChange = {
                        isConnectionMeteredEnabled = it
                    },
                    icon = R.drawable.wifi
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Audio Quality Settings Section
        SettingsSectionCard(
            title = stringResource(R.string.audio_quality_format),
            icon = R.drawable.speaker,
            content = {
                OtherSettingsEntry(
                    title = stringResource(R.string.audio_quality_format),
                    text = when (audioQualityFormat) {
                        AudioQualityFormat.Auto -> stringResource(R.string.audio_quality_automatic)
                        AudioQualityFormat.High -> stringResource(R.string.audio_quality_format_high)
                        AudioQualityFormat.Medium -> stringResource(R.string.audio_quality_format_medium)
                        AudioQualityFormat.Low -> stringResource(R.string.audio_quality_format_low)
                    },
                    icon = R.drawable.audio_quality,
                    onClick = { showAudioQualityDialog = true }
                )
                RestartPlayerService(restartService, onRestart = { restartService = false })
            }
        )

        if (showAudioQualityDialog) {
            ValueSelectorDialog(
                title = stringResource(R.string.audio_quality_format),
                values = AudioQualityFormat.values().toList(),
                selectedValue = audioQualityFormat,
                onValueSelected = {
                    audioQualityFormat = it
                    restartService = true
                    showAudioQualityDialog = false
                },
                onDismiss = { showAudioQualityDialog = false },
                valueText = {
                    when (it) {
                        AudioQualityFormat.Auto -> stringResource(R.string.audio_quality_automatic)
                        AudioQualityFormat.High -> stringResource(R.string.audio_quality_format_high)
                        AudioQualityFormat.Medium -> stringResource(R.string.audio_quality_format_medium)
                        AudioQualityFormat.Low -> stringResource(R.string.audio_quality_format_low)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto Download Settings Section
        SettingsSectionCard(
            title = stringResource(R.string.download),
            icon = R.drawable.arrow_down,
            content = {
                OtherSwitchSettingEntry(
                    title = stringResource(R.string.settings_enable_autodownload_song),
                    text = stringResource(R.string.auto_download_song_description),
                    isChecked = autoDownloadSong,
                    onCheckedChange = {
                        autoDownloadSong = it
                    },
                    icon = R.drawable.download
                )

                AnimatedVisibility(
                    visible = autoDownloadSong,
                    enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                        animationSpec = tween(400),
                        initialScale = 0.9f
                    ),
                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                        animationSpec = tween(200),
                        targetScale = 0.9f
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.settings_enable_autodownload_song_when_liked),
                            text = stringResource(R.string.auto_download_when_liked_description),
                            isChecked = autoDownloadSongWhenLiked,
                            onCheckedChange = {
                                autoDownloadSongWhenLiked = it
                            },
                            icon = R.drawable.heart
                        )

                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.settings_enable_autodownload_song_when_album_bookmarked),
                            text = stringResource(R.string.auto_download_when_album_bookmarked_description),
                            isChecked = autoDownloadSongWhenAlbumBookmarked,
                            onCheckedChange = {
                                autoDownloadSongWhenAlbumBookmarked = it
                            },
                            icon = R.drawable.bookmark
                        )
                    }
                }
            }
        )

        SettingsGroupSpacer(
            modifier = Modifier.height(Dimensions.bottomSpacer)
        )
    }
}

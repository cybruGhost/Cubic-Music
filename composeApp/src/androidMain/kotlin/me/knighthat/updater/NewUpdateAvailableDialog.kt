package me.knighthat.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.kreate.android.BuildConfig
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.styling.shimmer
import it.fast4x.rimusic.utils.bold
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.lastUpdateCheckKey
import it.fast4x.rimusic.utils.updateCancelledKey
import it.fast4x.rimusic.appContext
import me.knighthat.utils.Repository
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.graphics.Color
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.ui.styling.ModernBlackColorPalette
import it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import it.fast4x.rimusic.utils.colorPaletteModeKey
import it.fast4x.rimusic.utils.rememberPreference

@Composable
fun DialogText(
    text: String,
    style: TextStyle,
    spacerHeight: Dp = 10.dp
) {
    BasicText(
        text = text,
        style = style,
    )
    Spacer(Modifier.height(spacerHeight))
}

object NewUpdateAvailableDialog {

    /**
     * `false` by default.
     *
     * When this field is set to `true`, it'll
     * keep the dialog from showing up even when
     * [isActive] is `true`.
     *
     * This is used to prevent user from getting
     * annoyed by constant pop-up saying that
     * there's a new update available.
     *
     * This value will be reset once the app is
     * restart, either by user or by setting it
     * programmatically.
     */
    var isCancelled: Boolean by mutableStateOf( !BuildConfig.IS_AUTOUPDATE )

    private var showChangelog by mutableStateOf(false)
    private var changelogText by mutableStateOf("")

    var isActive: Boolean by mutableStateOf( false )

    fun onDismiss() {
        isCancelled = true
        isActive = false
        showChangelog = false
        
        // Mark update as cancelled when user cancels (but don't update the check time)
        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        sharedPrefs.edit()
            .putBoolean(updateCancelledKey, true)
            .apply()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun Render() {
        if( isCancelled || !isActive ) return

        val uriHandler = LocalUriHandler.current
        var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.System)

        if (showChangelog) {
            Dialog(onDismissRequest = { onDismiss() }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Header with title and version
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                            animationSpec = tween(300),
                            initialScale = 0.9f
                        )
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                    Color(0xFF1A1A1A) // Gray dark for pitch black themes
                                } else {
                                    colorPalette().background1
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.changelog_list),
                                        contentDescription = null,
                                        tint = colorPalette().accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BasicText(
                                        text = stringResource(
                                            R.string.update_changelogs,
                                            Updater.githubRelease?.tagName ?: BuildConfig.VERSION_NAME
                                        ),
                                        style = typography().l.bold.copy(color = colorPalette().text)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Changelog content
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                            animationSpec = tween(400),
                            initialScale = 0.9f
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp), // Limit height to leave space for the back button
                            colors = CardDefaults.cardColors(
                                containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                    Color(0xFF1A1A1A) // Gray dark for pitch black themes
                                } else {
                                    colorPalette().background1
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownText(
                                    modifier = Modifier.padding(8.dp),
                                    markdown = changelogText.ifEmpty { stringResource(R.string.no_changelog_available) },
                                    maxLines = 100,
                                    style = typography().xs.semiBold.copy(color = colorPalette().text)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Back button
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                            animationSpec = tween(500),
                            initialScale = 0.9f
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangelog = false },
                            colors = CardDefaults.cardColors(
                                containerColor = colorPalette().accent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_left),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicText(
                                        text = stringResource(R.string.back),
                                        style = typography().s.semiBold.copy(color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Dialog(onDismissRequest = { onDismiss() }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Header with title
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        animationSpec = tween(300),
                        initialScale = 0.9f
                    )
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                Color(0xFF1A1A1A) // Gray dark for pitch black themes
                            } else {
                                colorPalette().background1
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                    tint = colorPalette().accent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicText(
                                    text = buildString {
                                        // Determine if this is a beta or stable update
                                        val releaseSuffix = Updater.githubRelease?.tagName?.removePrefix("v")?.split("-")?.getOrNull(1) ?: ""
                                        val currentSuffix = BuildConfig.VERSION_NAME.removePrefix("v").split("-").getOrNull(1) ?: ""
                                        
                                        // Show beta title if either current or new version is beta
                                        if (releaseSuffix == "b" || currentSuffix == "b") {
                                            append(stringResource(R.string.beta_title))
                                            append(" ")
                                        } else {
                                            append(stringResource(R.string.stable_title))
                                            append(" ")
                                        }
                                        append(stringResource(R.string.update_available))
                                    },
                                    style = typography().l.bold.copy(color = colorPalette().text)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BasicText(
                                    text = stringResource(R.string.app_update_dialog_version, Updater.githubRelease?.tagName ?: BuildConfig.VERSION_NAME),
                                    style = typography().xs.copy(color = colorPalette().textSecondary)
                                )
                                BasicText(
                                    text = stringResource(R.string.app_update_dialog_size, Updater.build.readableSize.ifEmpty { "?" }),
                                    style = typography().xs.copy(color = colorPalette().textSecondary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Go to github page to download
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                        animationSpec = tween(400),
                        initialScale = 0.9f
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                val tagUrl = "${Repository.GITHUB}/${Repository.LATEST_TAG_URL}"
                                uriHandler.openUri(tagUrl)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                Color(0xFF1A1A1A) // Gray dark for pitch black themes
                            } else {
                                colorPalette().background1
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            BasicText(
                                text = stringResource(R.string.open_the_github_releases_web_page_and_download_latest_version),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.globe),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Option 2: Go straight to download page
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                        animationSpec = tween(500),
                        initialScale = 0.9f
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                uriHandler.openUri(Updater.build.downloadUrl)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                Color(0xFF1A1A1A) // Gray dark for pitch black themes
                            } else {
                                colorPalette().background1
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            BasicText(
                                text = stringResource(R.string.download_latest_version_from_github_you_will_find_the_file_in_the_notification_area_and_you_can_install_by_clicking_on_it),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Option 3: View Changelog
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                        animationSpec = tween(600),
                        initialScale = 0.9f
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                changelogText = Updater.githubRelease?.body ?: ""
                                showChangelog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                Color(0xFF1A1A1A) // Gray dark for pitch black themes
                            } else {
                                colorPalette().background1
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            BasicText(
                                text = stringResource(R.string.view_changelog, Updater.githubRelease?.tagName ?: BuildConfig.VERSION_NAME),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.changelog_list),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(700)) + scaleIn(
                        animationSpec = tween(700),
                        initialScale = 0.9f
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() },
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette().accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    tint = colorPalette().text,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicText(
                                    text = stringResource(R.string.cancel),
                                    style = typography().s.semiBold.copy(color = Color.White))
                            }
                        }
                    }
                }
            }
        }
    }
}
}
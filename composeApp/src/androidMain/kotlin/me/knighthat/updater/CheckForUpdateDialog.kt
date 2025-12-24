package me.knighthat.updater

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.CheckUpdateState
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.checkUpdateStateKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.ui.styling.ModernBlackColorPalette
import it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import it.fast4x.rimusic.utils.bold
import it.fast4x.rimusic.utils.colorPaletteModeKey
import it.fast4x.rimusic.utils.semiBold

object CheckForUpdateDialog {

    private var isCanceled: Boolean by mutableStateOf(false)
    var isActive: Boolean by mutableStateOf(false)

    fun onDismiss() {
        isCanceled = true
        isActive = false
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun Render() {
        if (isCanceled || !isActive) return

        var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Enabled)
        var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.System)

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
                                    text = stringResource(R.string.check_at_github_for_updates),
                                    style = typography().l.bold.copy(color = colorPalette().text)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BasicText(
                                    text = stringResource(R.string.when_an_update_is_available_you_will_be_asked_if_you_want_to_install_info),
                                    style = typography().xs.copy(color = colorPalette().textSecondary)
                                )
                                BasicText(
                                    text = stringResource(R.string.but_these_updates_would_not_go_through),
                                    style = typography().xs.copy(color = colorPalette().textSecondary)
                                )
                                BasicText(
                                    text = stringResource(R.string.you_can_still_turn_it_on_or_off_from_the_settings),
                                    style = typography().xs.copy(color = colorPalette().textSecondary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Check for updates
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
                                Updater.checkForUpdate(checkBetaUpdates = false)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette().accent
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
                                text = stringResource(R.string.check_update),
                                style = typography().xs.semiBold.copy(color = Color.White),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.update),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Option 2: Cancel (skip this time)
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
                            .clickable { onDismiss() },
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
                                text = stringResource(R.string.cancel),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_left),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Option 3: Turn off auto check
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
                                checkUpdateState = CheckUpdateState.Disabled
                                onDismiss()
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
                                text = stringResource(R.string.turn_off),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
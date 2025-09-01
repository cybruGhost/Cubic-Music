package it.fast4x.rimusic.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import app.kreate.android.BuildConfig
import app.kreate.android.drawable.APP_ICON_IMAGE_BITMAP
import app.kreate.android.R
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.extensions.contributors.ShowDevelopers
import it.fast4x.rimusic.extensions.contributors.ShowTranslators
import it.fast4x.rimusic.extensions.contributors.countDevelopers
import it.fast4x.rimusic.extensions.contributors.countTranslators
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.*
import me.knighthat.utils.Repository
import me.knighthat.updater.ChangelogsDialog
import me.knighthat.updater.Updater
import me.knighthat.updater.NewUpdateAvailableDialog
import it.fast4x.rimusic.enums.CheckUpdateState
import it.fast4x.rimusic.utils.checkUpdateStateKey
import it.fast4x.rimusic.utils.checkBetaUpdatesKey
import it.fast4x.rimusic.utils.lastUpdateCheckKey
import it.fast4x.rimusic.utils.updateCancelledKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import it.fast4x.rimusic.ui.styling.ModernBlackColorPalette
import it.fast4x.rimusic.utils.colorPaletteModeKey
import androidx.compose.ui.graphics.Color
import it.fast4x.rimusic.enums.ColorPaletteMode
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalAnimationApi
@Composable
fun About() {
    // Function to extract the version suffix
    fun extractVersionSuffix(versionStr: String): String {
        val parts = versionStr.removePrefix("v").split("-")
        return if (parts.size > 1) parts[1] else ""
    }
    val uriHandler = LocalUriHandler.current
    val showChangelog = remember { mutableStateOf(false) }
    var lastUpdateCheck by rememberPreference(lastUpdateCheckKey, 0L)
    var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Enabled)
    var checkBetaUpdates by rememberPreference(checkBetaUpdatesKey, extractVersionSuffix(BuildConfig.VERSION_NAME) == "b")
    var updateCancelled by rememberPreference(updateCancelledKey, false)
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    
    // Synchronize updateCancelled with SharedPreferences
    LaunchedEffect(Unit) {
        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        updateCancelled = sharedPrefs.getBoolean(updateCancelledKey, false)
        lastUpdateCheck = sharedPrefs.getLong(lastUpdateCheckKey, 0L)
    }
    
    // Listen for SharedPreferences changes and force save
    LaunchedEffect(updateCancelled) {
        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        sharedPrefs.edit()
            .putBoolean(updateCancelledKey, updateCancelled)
            .apply()
    }
    
    // Listen for lastUpdateCheck changes in SharedPreferences
    LaunchedEffect(Unit) {
        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == lastUpdateCheckKey) {
                lastUpdateCheck = sharedPrefs.getLong(lastUpdateCheckKey, 0L)
            }
        }
    }
    

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

        if (UiType.ViMusic.isCurrent())
            if (NavigationBarPosition.Right.isCurrent() || NavigationBarPosition.Left.isCurrent())
                Spacer(Modifier.height(Dimensions.halfheaderHeight))

         // Header 
         HeaderWithIcon(
             title = stringResource(R.string.about),
             iconId = R.drawable.information,
             enabled = false,
             showIcon = true,
             modifier = Modifier,
             onClick = {}
         )
         SettingsDescription(
            text = stringResource(R.string.about_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 
 
         // Header Cards Row - App Info & Update Check
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                animationSpec = tween(600),
                initialScale = 0.8f
            )
        ) {
                         Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
             ) {
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(IntrinsicSize.Max),
                     horizontalArrangement = Arrangement.spacedBy(12.dp)
                 ) {
                     // App Info Card
                     Card(
                         modifier = Modifier
                             .weight(1f)
                             .fillMaxHeight()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = colorPalette().accent.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                        Color(0xFF1A1A1A) // Gray dark for pitch black themes
                    } else {
                        colorPalette().background1
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                         modifier = Modifier
                             .fillMaxHeight()
                             .padding(20.dp),
                         horizontalAlignment = Alignment.CenterHorizontally,
                         verticalArrangement = Arrangement.Top
                     ) {
                                                 // Top content
                         Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                                         // App Icon
                     Box(
                         modifier = Modifier
                                     .size(60.dp)
                             .background(
                                 brush = Brush.radialGradient(
                                     colors = listOf(
                                         colorPalette().accent.copy(alpha = 0.1f),
                                         colorPalette().accent.copy(alpha = 0.05f)
                                     )
                                 ),
                        shape = CircleShape
                             ),
                         contentAlignment = Alignment.Center
                     ) {
                         Image(
                             bitmap = APP_ICON_IMAGE_BITMAP,
                    contentDescription = null,
                                     modifier = Modifier.size(28.dp)
                )
            }

                             Spacer(modifier = Modifier.height(12.dp))

                                         // App Name
            val pkgManager = appContext().packageManager
                     val appInfo = pkgManager.getApplicationInfo(appContext().packageName, 0)
            BasicText(
                         text = pkgManager.getApplicationLabel(appInfo).toString(),
                style = TextStyle(
                                     fontSize = typography().l.bold.fontSize,
                                     fontWeight = typography().l.bold.fontWeight,
                    color = colorPalette().text,
                             textAlign = TextAlign.Center
                         ),
                         modifier = Modifier.fillMaxWidth()
                     )

                             Spacer(modifier = Modifier.height(6.dp))

                                                                                     // Version
                          BasicText(
                              text = "v${getVersionName()}",
                                   style = typography().xs.secondary.copy(
                                  textAlign = TextAlign.Center
                              ),
                              modifier = Modifier.fillMaxWidth()
                          )
                              
                              Spacer(modifier = Modifier.height(8.dp))
                          }

                                                 // Bottom content - Author & Changelog
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             // Author
                         Row(
                             horizontalArrangement = Arrangement.Center,
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable {
                                     val url = "${Repository.GITHUB}/${Repository.OWNER}"
                                     uriHandler.openUri(url)
                                 }
                         ) {
                             BasicText(
                                 text = stringResource(R.string.by_string),
                                     style = typography().xs.secondary.copy(
                                     textAlign = TextAlign.Center
                                 ),
                             )
                             Spacer(modifier = Modifier.width(5.dp))
                             Icon(
                                 painter = painterResource(R.drawable.github_icon),
                                 tint = colorPalette().accent,
                                 contentDescription = null,
                                     modifier = Modifier.size(14.dp)
                             )
                                 Spacer(modifier = Modifier.width(3.dp))
                             BasicText(
                                 text = Repository.OWNER,
                                     style = typography().xs.secondary.copy(
                                     textDecoration = TextDecoration.Underline,
                                     color = colorPalette().accent,
                                     textAlign = TextAlign.Center
                                 )
                             )
                                                           }
                              
                              Spacer(modifier = Modifier.height(8.dp))
                              
                              // View Changelog Button
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .clip(RoundedCornerShape(8.dp))
                                      .border(
                                          width = 1.dp,
                                          color = colorPalette().textSecondary.copy(alpha = 0.3f),
                                          shape = RoundedCornerShape(8.dp)
                                      )
                                      .clickable { showChangelog.value = true }
                                      .padding(8.dp)
                              ) {
                                 Icon(
                                     painter = painterResource(R.drawable.changelog_list),
                                     tint = colorPalette().textSecondary,
                                     contentDescription = null,
                                     modifier = Modifier.size(16.dp)
                                 )
                                 Spacer(modifier = Modifier.width(6.dp))
            BasicText(
                                     text = stringResource(R.string.view_changelog_settings_title),
                                     style = typography().xs.semiBold.copy(
                                         color = colorPalette().textSecondary
                                     )
                                 )
                             }
                         }
                     }
                }

                                     // Update Check Card
                     Card(
                         modifier = Modifier
                             .weight(1f)
                             .fillMaxHeight()
                             .shadow(
                                 elevation = 8.dp,
                                 shape = RoundedCornerShape(16.dp),
                                 spotColor = colorPalette().accent.copy(alpha = 0.3f)
                             ),
                     shape = RoundedCornerShape(16.dp),
                     colors = CardDefaults.cardColors(
                         containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                             Color(0xFF1A1A1A) // Gray dark for pitch black themes
                         } else {
                             colorPalette().background1
                         }
                     ),
                     elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                 ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Top content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Update Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                colorPalette().accent.copy(alpha = 0.1f),
                                                colorPalette().accent.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
            ) {
                Icon(
                                    painter = painterResource(R.drawable.update),
                                    tint = colorPalette().accent,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Update Title
                            BasicText(
                                text = stringResource(R.string.update),
                                style = TextStyle(
                                    fontSize = typography().l.bold.fontSize,
                                    fontWeight = typography().l.bold.fontWeight,
                                    color = colorPalette().text,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Update Status
                            if (BuildConfig.IS_AUTOUPDATE) {
                                // Keep updateCancelled state synchronized with SharedPreferences
                                LaunchedEffect(NewUpdateAvailableDialog.isActive, lastUpdateCheck) {
                                    val sharedPrefs = appContext().getSharedPreferences("settings", 0)
                                    updateCancelled = sharedPrefs.getBoolean(updateCancelledKey, false)
                                }
                                
                                val updateStatusText = when {
                                    lastUpdateCheck == 0L -> stringResource(R.string.never_checked)
                                    NewUpdateAvailableDialog.isActive -> stringResource(R.string.update_available)
                                    updateCancelled -> stringResource(R.string.update_available)
                                    else -> {
                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        val date = Date(lastUpdateCheck)
                                        "${stringResource(R.string.up_to_date)} (${dateFormat.format(date)})"
                                    }
                                }
                                
                                val statusColor = if (NewUpdateAvailableDialog.isActive || updateCancelled) {
                                    colorPalette().accent
                                } else {
                                    colorPalette().textSecondary
                                }
                                 
                                BasicText(
                                    text = updateStatusText,
                                    style = typography().xs.secondary.copy(
                                        textAlign = TextAlign.Center,
                                        color = statusColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                BasicText(
                                    text = stringResource(R.string.check_update),
                                    style = typography().xs.secondary.copy(
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                                                                                                    // Bottom content - Update Controls
                          if (BuildConfig.IS_AUTOUPDATE) {
                             
                                                           Column(
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  verticalArrangement = Arrangement.spacedBy(4.dp)
                              ) {
                                                                   // Auto Update Toggle
                                  Row(
                                      horizontalArrangement = Arrangement.Center,
                                      verticalAlignment = Alignment.CenterVertically,
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .clip(RoundedCornerShape(8.dp))
                                          .border(
                                              width = 1.dp,
                                              color = colorPalette().textSecondary.copy(alpha = 0.3f),
                                              shape = RoundedCornerShape(8.dp)
                                          )
                                          .clickable {
                                              checkUpdateState = when (checkUpdateState) {
                                                  CheckUpdateState.Disabled -> CheckUpdateState.Enabled
                                                  CheckUpdateState.Enabled -> CheckUpdateState.Ask
                                                  CheckUpdateState.Ask -> CheckUpdateState.Disabled
                                              }
                                          }
                                          .padding(8.dp)
                                  ) {
                                                                           Icon(
                                          painter = painterResource(
                                              when (checkUpdateState) {
                                                  CheckUpdateState.Enabled -> R.drawable.checkmark
                                                  CheckUpdateState.Ask -> R.drawable.information
                                                  CheckUpdateState.Disabled -> R.drawable.close
                                              }
                                          ),
                                         tint = when (checkUpdateState) {
                                             CheckUpdateState.Enabled -> colorPalette().accent
                                             CheckUpdateState.Ask -> colorPalette().textSecondary
                                             CheckUpdateState.Disabled -> colorPalette().red
                                         },
                                         contentDescription = null,
                                         modifier = Modifier.size(16.dp)
                                     )
                                     Spacer(modifier = Modifier.width(6.dp))
                                                                           BasicText(
                                          text = when (checkUpdateState) {
                                              CheckUpdateState.Enabled -> stringResource(R.string.auto_update_enabled)
                                              CheckUpdateState.Ask -> stringResource(R.string.auto_update_ask)
                                              CheckUpdateState.Disabled -> stringResource(R.string.auto_update_disabled)
                                          },
                                         style = typography().xxs.semiBold.copy(
                                             textAlign = TextAlign.Center,
                                             color = when (checkUpdateState) {
                                                 CheckUpdateState.Enabled -> colorPalette().accent
                                                 CheckUpdateState.Ask -> colorPalette().textSecondary
                                                 CheckUpdateState.Disabled -> colorPalette().red
                                             }
                                         ),
                                         modifier = Modifier.fillMaxWidth()
                                     )
                                                                   }
                                  
                                  
                                  // Beta Updates Toggle (only for full builds, not for beta users)
                                  if (BuildConfig.IS_AUTOUPDATE && extractVersionSuffix(BuildConfig.VERSION_NAME) == "f" || extractVersionSuffix(BuildConfig.VERSION_NAME) == "b") {
                                      Spacer(modifier = Modifier.height(2.dp))
                                      
                                      Row(
                                          horizontalArrangement = Arrangement.Center,
                                          verticalAlignment = Alignment.CenterVertically,
                                          modifier = Modifier
                                              .fillMaxWidth()
                                              .clip(RoundedCornerShape(8.dp))
                                              .border(
                                                  width = 1.dp,
                                                  color = colorPalette().textSecondary.copy(alpha = 0.3f),
                                                  shape = RoundedCornerShape(8.dp)
                                              )
                                              .clickable { checkBetaUpdates = !checkBetaUpdates }
                                              .padding(8.dp)
                                      ) {
                                          Icon(
                                              painter = painterResource(
                                                  if (checkBetaUpdates) R.drawable.checkmark else R.drawable.close
                                              ),
                                              tint = if (checkBetaUpdates) colorPalette().accent else colorPalette().red,
                                              contentDescription = null,
                                              modifier = Modifier.size(16.dp)
                                          )
                                          Spacer(modifier = Modifier.width(6.dp))
                                          BasicText(
                                              text = stringResource(
                                                  if (checkBetaUpdates) R.string.check_beta_update_enabled 
                                                  else R.string.check_beta_update_disabled
                                              ),
                                              style = typography().xxs.semiBold.copy(
                                                  textAlign = TextAlign.Center,
                                                  color = if (checkBetaUpdates) colorPalette().accent else colorPalette().red
                                              ),
                                              modifier = Modifier.fillMaxWidth()
                                          )
                                      }
                                  }
                                  
                                  // Check Update Button (moved to bottom)
                                  Spacer(modifier = Modifier.height(4.dp))
                                  
                                  Row(
                                      horizontalArrangement = Arrangement.Center,
                                      verticalAlignment = Alignment.CenterVertically,
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .clip(RoundedCornerShape(8.dp))
                                          .border(
                                              width = 1.dp,
                                              color = colorPalette().textSecondary.copy(alpha = 0.3f),
                                              shape = RoundedCornerShape(8.dp)
                                          )
                                          .clickable {
                                              lastUpdateCheck = System.currentTimeMillis()
                                              NewUpdateAvailableDialog.isCancelled = false
                                              // Reset updateCancelled when manually checking
                                              val sharedPrefs = appContext().getSharedPreferences("settings", 0)
                                              sharedPrefs.edit()
                                                  .putBoolean(updateCancelledKey, false)
                                                  .apply()
                                              // Force a fresh check
                                              Updater.checkForUpdate(true, checkBetaUpdates)
                                          }
                                          .padding(8.dp)
                                  ) {
                                      Icon(
                                          painter = painterResource(R.drawable.update),
                                          tint = colorPalette().textSecondary,
                                          contentDescription = null,
                                          modifier = Modifier.size(16.dp)
                                      )
                                      Spacer(modifier = Modifier.width(6.dp))
                                      BasicText(
                                          text = stringResource(R.string.check_update),
                                          style = typography().xxs.semiBold.copy(
                                              textAlign = TextAlign.Center,
                                              color = colorPalette().textSecondary
                                          ),
                                          modifier = Modifier.fillMaxWidth()
                                      )
                                  }
                             }
                         } else {
                             BasicText(
                                 text = stringResource(R.string.description_app_not_installed_by_apk),
                                 style = typography().xxs.secondary.copy(
                                     textAlign = TextAlign.Center,
                                     color = colorPalette().textSecondary
                                 ),
                                 modifier = Modifier.fillMaxWidth()
                             )
                         }
                    }
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Support & Links Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                animationSpec = tween(800),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.troubleshooting),
                icon = R.drawable.information,
                content = {
                    // Support Items
                    ModernSettingsEntry(
                        title = stringResource(R.string.view_the_source_code),
                        text = stringResource(R.string.you_will_be_redirected_to_github),
                        icon = R.drawable.github_icon,
                        onClick = { uriHandler.openUri(Repository.REPO_URL) }
                    )

                    ModernSettingsEntry(
                        title = stringResource(R.string.report_an_issue),
                        text = stringResource(R.string.you_will_be_redirected_to_github),
                        icon = R.drawable.trending,
                        onClick = {
                            val issuePath = "/issues/new?assignees=&labels=bug&template=bug_report.yaml"
                            uriHandler.openUri(Repository.REPO_URL + issuePath)
                        }
                    )

                    ModernSettingsEntry(
                        title = stringResource(R.string.request_a_feature_or_suggest_an_idea),
                        text = stringResource(R.string.you_will_be_redirected_to_github),
                        icon = R.drawable.star_brilliant,
                        onClick = {
                            val issuePath = "/issues/new?assignees=&labels=feature_request&template=feature_request.yaml"
                            uriHandler.openUri(Repository.REPO_URL + issuePath)
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contributors Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                animationSpec = tween(1000),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.contributors),
                icon = R.drawable.people,
                content = {
                    // Translators Section
                    var translatorsExpanded by remember { mutableStateOf(false) }
                    val translatorsRotation by animateFloatAsState(
                        targetValue = if (translatorsExpanded) 90f else 0f,
                        animationSpec = tween(300),
                        label = "translators_rotation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { translatorsExpanded = !translatorsExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_forward),
                            tint = colorPalette().accent,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer(rotationZ = translatorsRotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicText(
                            text = "${countTranslators()} " + stringResource(R.string.translators),
                            style = typography().xs.semiBold.copy(color = colorPalette().textSecondary)
                        )
                    }

                    AnimatedVisibility(
                        visible = translatorsExpanded,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                            animationSpec = tween(300),
                            initialScale = 0.95f
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                            animationSpec = tween(200),
                            targetScale = 0.95f
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp)
                        ) {
                            SettingsDescription(text = stringResource(R.string.in_alphabetical_order))
                            ShowTranslators()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Developers Section
                    var developersExpanded by remember { mutableStateOf(false) }
                    val developersRotation by animateFloatAsState(
                        targetValue = if (developersExpanded) 90f else 0f,
                        animationSpec = tween(300),
                        label = "developers_rotation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { developersExpanded = !developersExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_forward),
                            tint = colorPalette().accent,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer(rotationZ = developersRotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicText(
                            text = "${countDevelopers()} ${stringResource(R.string.about_developers_designers)}",
                            style = typography().xs.semiBold.copy(color = colorPalette().textSecondary)
                        )
                    }

                    AnimatedVisibility(
                        visible = developersExpanded,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                            animationSpec = tween(300),
                            initialScale = 0.95f
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                            animationSpec = tween(200),
                            targetScale = 0.95f
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp)
                        ) {
                            SettingsDescription(text = stringResource(R.string.in_alphabetical_order))
                            ShowDevelopers()
                        }
                    }
                }
            )
        }


        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }

    // Changelog dialog
    if (showChangelog.value) {
        val seenChangelogs = rememberPreference(seenChangelogsVersionKey, "")
        val changelogs = remember { ChangelogsDialog(seenChangelogs) }

        LaunchedEffect(Unit) { changelogs.isActive = true }
        changelogs.Render()
        
        LaunchedEffect(changelogs.isActive) {
            if (!changelogs.isActive) showChangelog.value = false
        }
    }
}
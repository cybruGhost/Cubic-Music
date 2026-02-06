package it.fast4x.rimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import coil.annotation.ExperimentalCoilApi
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.CacheType
import it.fast4x.rimusic.enums.CoilDiskCacheMaxSize
import it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import it.fast4x.rimusic.enums.ExoPlayerDiskCacheMaxSize
import it.fast4x.rimusic.enums.ExoPlayerDiskDownloadCacheMaxSize
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.ui.components.themed.CacheSpaceIndicator
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.InputNumericDialog
import it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.RestartPlayerService
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.coilCustomDiskCacheKey
import it.fast4x.rimusic.utils.coilDiskCacheMaxSizeKey
import it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import it.fast4x.rimusic.utils.exoPlayerCustomCacheKey
import it.fast4x.rimusic.utils.exoPlayerDiskCacheMaxSizeKey
import it.fast4x.rimusic.utils.exoPlayerDiskDownloadCacheMaxSizeKey
import it.fast4x.rimusic.utils.pauseSearchHistoryKey
import it.fast4x.rimusic.utils.pauseListenHistoryKey
import it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.knighthat.component.export.ExportDatabaseDialog
import me.knighthat.component.export.ExportSettingsDialog
import me.knighthat.component.import.ImportDatabase
import me.knighthat.component.import.ImportMigration
import me.knighthat.component.import.ImportSettings
import me.knighthat.utils.Toaster
import me.knighthat.coil.ImageCacheFactory
// First, make sure you have these imports at the top of your DataSettings.kt file:
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import it.fast4x.rimusic.typography

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.window.Dialog



@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalCoilApi::class)
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun DataSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`2GB`
    )

    var exoPlayerDiskDownloadCacheMaxSize by rememberPreference(
        exoPlayerDiskDownloadCacheMaxSizeKey,
        ExoPlayerDiskDownloadCacheMaxSize.`2GB`
    )

    var exoPlayerCacheLocation by rememberPreference(
        exoPlayerCacheLocationKey, ExoPlayerCacheLocation.Private
    )

    var showExoPlayerCustomCacheDialog by remember { mutableStateOf(false) }
    var exoPlayerCustomCache by rememberPreference(
        exoPlayerCustomCacheKey,32
    )

    var showCoilCustomDiskCacheDialog by remember { mutableStateOf(false) }
    var coilCustomDiskCache by rememberPreference(
        coilCustomDiskCacheKey,32
    )
    var showKreateDisclaimer by remember { mutableStateOf(false) }
    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)
    var pauseListenHistory by rememberPreference(pauseListenHistoryKey, false)

    var cleanCacheOfflineSongs by remember {
        mutableStateOf(false)
    }

    var cleanDownloadCache by remember {
        mutableStateOf(false)
    }
    var cleanCacheImages by remember {
        mutableStateOf(false)
    }
    
    var cacheCleanedCounter by remember {
        mutableIntStateOf(0)
    }

    if (cleanCacheOfflineSongs) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_cache),
            onDismiss = {
                cleanCacheOfflineSongs = false
            },
            onConfirm = {
                binder?.cache?.let { cache ->
                    cache.keys.forEach { song ->
                        cache.removeResource(song)
                    }
                }
                cleanCacheOfflineSongs = false
                cacheCleanedCounter++
            }
        )
    }

    if (cleanDownloadCache) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_cache),
            onDismiss = {
                cleanDownloadCache = false
            },
            onConfirm = {
                binder?.downloadCache?.let { downloadCache ->
                    downloadCache.keys.forEach { songId ->
                        downloadCache.removeResource(songId)

                        CoroutineScope(Dispatchers.IO).launch {
                            Database.songTable
                                .findById(songId)
                                .first()
                                ?.asMediaItem
                                ?.let { MyDownloadHelper.removeDownload(context, it) }
                        }
                    }
                }
                cleanDownloadCache = false
                cacheCleanedCounter++
            }
        )
    }

    if (cleanCacheImages) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_cache),
            onDismiss = {
                cleanCacheImages = false
            },
            onConfirm = {
                // Utiliser la nouvelle méthode sécurisée pour nettoyer le cache
                me.knighthat.coil.ImageCacheFactory.clearImageCache()
                cleanCacheImages = false
                cacheCleanedCounter++
            }
        )
    }

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
            title = stringResource(R.string.tab_data),
                       iconId = R.drawable.server,
                       enabled = false,
                       showIcon = true,
                       modifier = Modifier,
                       onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.data_settings_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 
        Spacer(modifier = Modifier.height(16.dp))


        // Cache Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                animationSpec = tween(600),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.cache),
                icon = R.drawable.server,
                description = stringResource(R.string.cache_cleared),
                content = {
                    ImageCacheFactory.getDiskCache()?.let { diskCache ->
                        val diskCacheSize = remember(diskCache.size, cleanCacheImages) {
                            diskCache.size
                        }

                        var showImageCacheDialog by remember { mutableStateOf(false) }
                        
                        key(cacheCleanedCounter) {
                        CacheSettingsEntry(
                            title = stringResource(R.string.image_cache_max_size),
                            text = when (coilDiskCacheMaxSize) {
                                CoilDiskCacheMaxSize.Custom -> "${stringResource(R.string.custom)}: ${coilCustomDiskCache}MB"
                                else -> coilDiskCacheMaxSize.text
                            },
                            icon = R.drawable.image,
                            onClick = { showImageCacheDialog = true },
                            onTrashClick = { cleanCacheImages = true }
                        )



                        if (showImageCacheDialog) {
                            ValueSelectorDialog(
                                title = stringResource(R.string.image_cache_max_size),
                                selectedValue = coilDiskCacheMaxSize,
                                values = CoilDiskCacheMaxSize.values().toList(),
                                onValueSelected = {
                                    coilDiskCacheMaxSize = it
                                    if (coilDiskCacheMaxSize == CoilDiskCacheMaxSize.Custom)
                                        showCoilCustomDiskCacheDialog = true
                                    restartService = true
                                },
                                valueText = { it.text },
                                onDismiss = { showImageCacheDialog = false }
                            )
                        }

                        RestartPlayerService(restartService, onRestart = { restartService = false })

                        if (showCoilCustomDiskCacheDialog) {
                            InputNumericDialog(
                                title = stringResource(R.string.set_custom_cache),
                                placeholder = stringResource(R.string.enter_value_in_mb),
                                value = coilCustomDiskCache.toString(),
                                valueMin = "32",
                                valueMax = "10000",
                                onDismiss = { showCoilCustomDiskCacheDialog = false },
                                setValue = {
                                    coilCustomDiskCache = it.toInt()
                                    showCoilCustomDiskCacheDialog = false
                                    restartService = true
                                }
                            )
                        }

                        CacheSpaceIndicator(cacheType = CacheType.Images, horizontalPadding = 20.dp)
                        
                        SettingsDescription(text = "${Formatter.formatShortFileSize(context, diskCacheSize)} ${stringResource(R.string.used)} (${diskCacheSize * 100 / coilDiskCacheMaxSize.bytes}%)")
                         
                        // Add diagnostic cache
                         val cacheSize = me.knighthat.coil.ImageCacheFactory.getCacheSize()
                         
                         SettingsDescription(
                             text = "${stringResource(R.string.cache_status)}: ${
                                 if (cacheSize > 0) stringResource(R.string.cache_working) 
                                 else stringResource(R.string.cache_empty)
                            }"
                        )

                        }
                    }

                    binder?.cache?.let { cache ->
                        val diskCacheSize = remember(cache.cacheSpace, cleanCacheOfflineSongs) {
                            cache.cacheSpace
                        }

                        var showSongCacheDialog by remember { mutableStateOf(false) }
                        
                        key(cacheCleanedCounter) {
                        CacheSettingsEntry(
                            title = stringResource(R.string.song_cache_max_size),
                            text = when (exoPlayerDiskCacheMaxSize) {
                                ExoPlayerDiskCacheMaxSize.Custom -> "${stringResource(R.string.custom)}: ${exoPlayerCustomCache}MB"
                                ExoPlayerDiskCacheMaxSize.Disabled -> "Disabled"
                                else -> exoPlayerDiskCacheMaxSize.text
                            },
                            icon = R.drawable.music_file,
                            onClick = { showSongCacheDialog = true },
                            onTrashClick = { cleanCacheOfflineSongs = true }
                        )

                        if (showSongCacheDialog) {
                            ValueSelectorDialog(
                                title = stringResource(R.string.song_cache_max_size),
                                selectedValue = exoPlayerDiskCacheMaxSize,
                                values = ExoPlayerDiskCacheMaxSize.values().toList(),
                                onValueSelected = {
                                    exoPlayerDiskCacheMaxSize = it
                                    if (exoPlayerDiskCacheMaxSize == ExoPlayerDiskCacheMaxSize.Custom)
                                        showExoPlayerCustomCacheDialog = true
                                    restartService = true
                                },
                                valueText = { it.text },
                                onDismiss = { showSongCacheDialog = false }
                            )
                        }

                        RestartPlayerService(restartService, onRestart = { restartService = false })

                        if (showExoPlayerCustomCacheDialog) {
                            InputNumericDialog(
                                title = stringResource(R.string.set_custom_cache),
                                placeholder = stringResource(R.string.enter_value_in_mb),
                                value = exoPlayerCustomCache.toString(),
                                valueMin = "32",
                                valueMax = "10000",
                                onDismiss = { showExoPlayerCustomCacheDialog = false },
                                setValue = {
                                    exoPlayerCustomCache = it.toInt()
                                    showExoPlayerCustomCacheDialog = false
                                    restartService = true
                                }
                            )
                        }

                        CacheSpaceIndicator(cacheType = CacheType.CachedSongs, horizontalPadding = 20.dp)
                        
                        SettingsDescription(text = "${Formatter.formatShortFileSize(context, diskCacheSize)} ${stringResource(R.string.used)} (${diskCacheSize * 100 / exoPlayerDiskCacheMaxSize.bytes}%)")
                        }
                    }

                    binder?.downloadCache?.let { downloadCache ->
                        val diskDownloadCacheSize = remember(downloadCache.cacheSpace, cleanDownloadCache) {
                            downloadCache.cacheSpace
                        }

                        var showDownloadCacheDialog by remember { mutableStateOf(false) }
                        
                        key(cacheCleanedCounter) {
                        CacheSettingsEntry(
                            title = stringResource(R.string.song_download_max_size),
                            text = exoPlayerDiskDownloadCacheMaxSize.text,
                            icon = R.drawable.download,
                            onClick = { showDownloadCacheDialog = true },
                            onTrashClick = { cleanDownloadCache = true }
                        )

                        RestartPlayerService(restartService, onRestart = { restartService = false })

                        if (showDownloadCacheDialog) {
                            ValueSelectorDialog(
                                title = stringResource(R.string.song_download_max_size),
                                selectedValue = exoPlayerDiskDownloadCacheMaxSize,
                                values = ExoPlayerDiskDownloadCacheMaxSize.values().toList(),
                                onValueSelected = {
                                    exoPlayerDiskDownloadCacheMaxSize = it
                                    restartService = true
                                },
                                valueText = { it.text },
                                onDismiss = { showDownloadCacheDialog = false }
                            )
                        }

                        CacheSpaceIndicator(cacheType = CacheType.DownloadedSongs, horizontalPadding = 20.dp)
                        
                        SettingsDescription(text = "${Formatter.formatShortFileSize(context, diskDownloadCacheSize)} ${stringResource(R.string.used)} (${if (exoPlayerDiskDownloadCacheMaxSize.bytes > 0) "${diskDownloadCacheSize * 100 / exoPlayerDiskDownloadCacheMaxSize.bytes}%" else stringResource(R.string.unlimited)})")
                        }
                    }

                    var showCacheLocationDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.set_cache_location),
                        text = exoPlayerCacheLocation.text,
                        icon = R.drawable.folder,
                        onClick = { showCacheLocationDialog = true }
                    )
                    
                    SettingsDescription(stringResource(R.string.info_private_cache_location_can_t_cleaned))

                    if (showCacheLocationDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.set_cache_location),
                            selectedValue = exoPlayerCacheLocation,
                            values = ExoPlayerCacheLocation.values().toList(),
                            onValueSelected = {
                                exoPlayerCacheLocation = it
                                restartService = true
                            },
                            valueText = { it.text },
                            onDismiss = { showCacheLocationDialog = false }
                        )
                    }

                    RestartPlayerService(restartService, onRestart = { restartService = false })


                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
// Kreate Backup Warning - Simple & Clear Version
AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = tween(750)) + scaleIn(
        animationSpec = tween(750),
        initialScale = 0.9f
    )
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable { showKreateDisclaimer = true },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.alert),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                BasicText(
                    text = "⚠️ IMPORTANT NOTICE",
                    style = typography().m.copy(color = Color.White)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Click indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Tap",
                        style = typography().xs.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.information),
                        contentDescription = "Show details",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Warning message
            BasicText(
                text = "🚨Attention Kreate Users Experiencing Import Issues",
                style = typography().s.copy(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Click instruction
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.moon),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                BasicText(
                    text = "Tap this card for detailed information and workarounds",
                    style = typography().xs.copy(color = Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}

// Kreate Disclaimer Dialog (Popup)
if (showKreateDisclaimer) {
    Dialog(
        onDismissRequest = { showKreateDisclaimer = false }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colorPalette().background1
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.alert),
                        contentDescription = null,
                        tint = colorPalette().accent,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    BasicText(
                        text = "🚨 Kreate Backup Import Issue",
                        style = typography().m.copy(color = colorPalette().accent)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Important Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x1AFF9800) // 10% opacity orange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "⚠️ DISCLAIMER: This is a Kreate app issue (fix not soon)",
                            style = typography().s.copy(color = colorPalette().text)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicText(
                        text = "The inability to import Kreate backups is NOT exclusive to Cubic Music. Even RiPlay & RiMusic (the app Kreate was forked from) cannot import Kreate's database format successfully.",
                        style = typography().xs.copy(color = colorPalette().text)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    BasicText(
                        text = "Why This Happens:",
                        style = typography().xs.copy(color = colorPalette().accent)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    BasicText(
                        text = "🔴 Kreate's export system has inherent bugs in its CSV generation\n" +
                              "🔴 The backup files contain malformed data that breaks standard CSV parsing\n" +
                              "🔴 Even the original app (RiPlay) fails when trying to read Kreate exports",
                        style = typography().xxs.copy(color = colorPalette().text)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    BasicText(
                        text = "What This Means:",
                        style = typography().xs.copy(color = colorPalette().accent)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    BasicText(
                        text = "✅ Cubic Music is working correctly with standard CSV imports\n" +
                              "✅ The problem originates in Kreate's export logic\n" +
                              "✅ No music app can reliably import Kreate backups until Kreate fixes its export system",
                        style = typography().xxs.copy(color = colorPalette().text)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    BasicText(
                        text = "Current Status: Work in Progress 🔧",
                        style = typography().xs.copy(color = colorPalette().accent)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    BasicText(
                        text = "We're investigating Kreate's export format and building a compatibility layer that will:\n\n" +
                              "🧹 Detect and fix Kreate's malformed CSV entries\n" +
                              "🛠️ Clean corrupted data before import\n" +
                              "📊 Provide error reports showing what couldn't be imported",
                        style = typography().xxs.copy(color = colorPalette().text)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottom line in emphasized card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette().background4
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "Bottom Line: This isn't about Cubic Music vs. Kreate—it's about Kreate generating broken backup files that no app can read properly.",
                                style = typography().xxs.copy(color = colorPalette().text),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Close button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showKreateDisclaimer = false },
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette().accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "Close",
                            style = typography().s.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

Spacer(modifier = Modifier.height(16.dp))

        // Backup and Restore Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                animationSpec = tween(800),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.title_backup_and_restore),
                icon = R.drawable.server,
                content = {
                    val exportDbDialog = ExportDatabaseDialog(context)

                    OtherSettingsEntry(
                        title = stringResource(R.string.save_to_backup),
                        text = stringResource(R.string.export_the_database),
                        icon = R.drawable.export_outline,
                        onClick = exportDbDialog::export
                    )

                    ImportantSettingsDescription(text = stringResource(
                        R.string.personal_preference
                    ))

                    val importDatabase = ImportDatabase(context)

                    OtherSettingsEntry(
                        title = stringResource(R.string.restore_from_backup),
                        text = stringResource(R.string.import_the_database),
                        icon = R.drawable.import_outline,
                        onClick = importDatabase::onShortClick
                    )
                    ImportantSettingsDescription(text = stringResource(
                        R.string.existing_data_will_be_overwritten,
                        context.applicationInfo.nonLocalizedLabel
                    ))

                    val exportSettingsDialog = ExportSettingsDialog(context)

                    OtherSettingsEntry(
                        title = stringResource(R.string.title_export_settings),
                        text = stringResource(R.string.store_settings_in_a_file),
                        icon = R.drawable.export_outline,
                        onClick = exportSettingsDialog::export
                    )
                    ImportantSettingsDescription(
                        stringResource(R.string.description_exclude_credentials)
                    )

                    val importSettings = ImportSettings(context)

                    OtherSettingsEntry(
                        title = stringResource(R.string.title_import_settings),
                        text = stringResource(R.string.restore_settings_from_file, stringResource(R.string.title_export_settings)),
                        icon = R.drawable.import_outline,
                        onClick = importSettings::onShortClick
                    )
                    ImportantSettingsDescription(text = stringResource(
                        R.string.existing_data_will_be_overwritten,
                        context.applicationInfo.nonLocalizedLabel
                    ))

                    val importMigration = ImportMigration(context, binder)

                    OtherSettingsEntry(
                        title = stringResource(R.string.title_import_settings_migration),
                        text = stringResource(R.string.description_import_settings_migration),
                        icon = R.drawable.data_migration,
                        onClick = importMigration::onShortClick
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search History Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                animationSpec = tween(1000),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.search_history),
                icon = R.drawable.search,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.pause_search_history),
                        text = stringResource(R.string.neither_save_new_searched_query),
                        isChecked = pauseSearchHistory,
                        onCheckedChange = {
                            pauseSearchHistory = it
                            restartService = true
                        },
                        icon = R.drawable.pause
                    )

                    RestartPlayerService(restartService, onRestart = { restartService = false })

                    val queriesCount by remember {
                        Database.searchTable
                            .findAllContain("")
                            .map { it.size }
                    }.collectAsState(0, Dispatchers.IO)

                    OtherSettingsEntry(
                        title = stringResource(R.string.clear_search_history),
                        text = if (queriesCount > 0) {
                            "${stringResource(R.string.delete)} " + queriesCount + stringResource(R.string.search_queries)
                        } else {
                            stringResource(R.string.history_is_empty)
                        },
                        icon = R.drawable.trash,
                        onClick = {
                            Database.asyncTransaction {
                                searchTable.deleteAll()
                            }
                            Toaster.done()
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

                // Search History Section
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                        animationSpec = tween(1000),
                        initialScale = 0.9f
                    )
                ) {
                    SettingsSectionCard(
                        title = stringResource(R.string.player_pause_listen_history),
                        icon = R.drawable.musical_notes,
                        content = {
                            OtherSwitchSettingEntry(
                                title = stringResource(R.string.player_pause_listen_history),
                                text = stringResource(R.string.player_pause_listen_history_info),
                                isChecked = pauseListenHistory,
                                onCheckedChange = {
                                    pauseListenHistory = it
                                    restartService = true
                                },
                                icon = R.drawable.pause
                            )
        
                            RestartPlayerService(restartService, onRestart = { restartService = false })
                        }
                    )
                }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}
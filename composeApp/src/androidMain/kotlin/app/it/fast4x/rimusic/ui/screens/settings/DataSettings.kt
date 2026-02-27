package app.it.fast4x.rimusic.ui.screens.settings

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
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.CacheType
import app.it.fast4x.rimusic.enums.CoilDiskCacheMaxSize
import app.it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import app.it.fast4x.rimusic.enums.ExoPlayerDiskCacheMaxSize
import app.it.fast4x.rimusic.enums.ExoPlayerDiskDownloadCacheMaxSize
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.ui.components.themed.CacheSpaceIndicator
import app.it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.InputNumericDialog
import app.it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.RestartPlayerService
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.coilCustomDiskCacheKey
import app.it.fast4x.rimusic.utils.coilDiskCacheMaxSizeKey
import app.it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import app.it.fast4x.rimusic.utils.exoPlayerCustomCacheKey
import app.it.fast4x.rimusic.utils.exoPlayerDiskCacheMaxSizeKey
import app.it.fast4x.rimusic.utils.exoPlayerDiskDownloadCacheMaxSizeKey
import app.it.fast4x.rimusic.utils.pauseSearchHistoryKey
import app.it.fast4x.rimusic.utils.pauseListenHistoryKey
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.component.export.ExportDatabaseDialog
import app.kreate.android.me.knighthat.component.export.ExportSettingsDialog
import app.kreate.android.me.knighthat.component.import.ImportDatabase
import app.kreate.android.me.knighthat.component.import.ImportMigration
import app.kreate.android.me.knighthat.component.import.ImportSettings
import app.kreate.android.me.knighthat.utils.Toaster
import app.kreate.android.me.knighthat.coil.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import app.it.fast4x.rimusic.typography

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
                app.kreate.android.me.knighthat.coil.ImageCacheFactory.clearImageCache()
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
                        
                        SettingsDescription(text = "${Formatter.formatShortFileSize(context, diskCacheSize)} ${stringResource(R.string.used)} (${if (coilDiskCacheMaxSize.bytes > 0) "${diskCacheSize * 100 / coilDiskCacheMaxSize.bytes}%" else stringResource(R.string.unlimited)})")

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
                        
                         SettingsDescription(text = "${Formatter.formatShortFileSize(context, diskCacheSize)} ${stringResource(R.string.used)} (${if (exoPlayerDiskCacheMaxSize.bytes > 0) "${diskCacheSize * 100 / exoPlayerDiskCacheMaxSize.bytes}%" else stringResource(R.string.unlimited)})")
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
// Kreate Backup Fix - Direct Solution (Compact Version)
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
            containerColor = Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(8.dp), // Reduced from 12dp
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Reduced from 4dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp), // Reduced padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.alert),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp) // Reduced from 28dp
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = "✅ KREATE FIX AVAILABLE",
                    style = typography().s.copy(color = Color.White) // Reduced from .m to .s
                )
                
                BasicText(
                    text = "Tap for import steps",
                    style = typography().xs.copy(color = Color.White.copy(alpha = 0.9f)), // Added
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Tap indicator
            Icon(
                painter = painterResource(R.drawable.chevron_forward),
                contentDescription = "Tap",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Kreate Fix Dialog
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
                    .padding(20.dp) // Reduced from 24dp
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.checked_filled),
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp) // Reduced from 28dp
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    BasicText(
                        text = "✅ KREATE DATABASE FIX",
                        style = typography().m.copy(color = Color(0xFF4CAF50))
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp)) // Reduced from 20dp
                
                // Why this exists
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette().background4
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp) // Reduced from 16dp
                    ) {
                        BasicText(
                            text = "⚠️ Why this exists:",
                            style = typography().s.copy(color = colorPalette().accent)
                        )
                        Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
                        BasicText(
                            text = "Kreate exports backups with user_version 28 that crashes Cubic Music. This tool downgrades the version and fixes malformed data.",
                            style = typography().xs.copy(color = colorPalette().text)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Steps
                BasicText(
                    text = "📋 FOLLOW THESE STEPS:",
                    style = typography().s.copy(color = colorPalette().accent)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step 1 with BIG OBVIOUS BUTTON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "1",
                                style = typography().xxs.copy(color = Color.White)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            BasicText(
                                text = "Convert your Kreate backup:",
                                style = typography().xs.copy(color = colorPalette().text)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // 👇 OBVIOUS BUTTON for the link
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://kreatebackfix.vercel.app/")
                                        )
                                        context.startActivity(intent)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.btn_radio_on_mtrl),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        BasicText(
                                            text = "OPEN FIX TOOL",
                                            style = typography().s.copy(color = Color.White)
                                        )
                                    }
                                    
                                    Icon(
                                        painter = painterResource(R.drawable.devices),
                                        contentDescription = "Open link",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Steps 2-5 (simplified)
                    StepItem(number = "2", text = "Upload your .db backup file")
                    StepItem(number = "3", text = "Download the converted database")
                    StepItem(number = "4", text = "Settings → Data → Restore from backup")
                    StepItem(number = "5", text = "Import the converted file")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // What the tool does (compact)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette().background4
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        BasicText(
                            text = "✅ What this tool does:",
                            style = typography().s.copy(color = colorPalette().accent)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicText(
                            text = "• Fixes malformed Kreate exports\n" +
                                  "• Downgrades from version 28\n" +
                                  "• Makes it importable to Cubic Music",
                            style = typography().xs.copy(color = colorPalette().text)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Close button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showKreateDisclaimer = false },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "Got it",
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
    @Composable
fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = number,
                style = typography().xxs.copy(color = Color.White)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        BasicText(
            text = text,
            style = typography().xs.copy(color = colorPalette().text),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
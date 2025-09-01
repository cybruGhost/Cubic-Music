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
        exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System
    )

    var showExoPlayerCustomCacheDialog by remember { mutableStateOf(false) }
    var exoPlayerCustomCache by rememberPreference(
        exoPlayerCustomCacheKey,32
    )

    var showCoilCustomDiskCacheDialog by remember { mutableStateOf(false) }
    var coilCustomDiskCache by rememberPreference(
        coilCustomDiskCacheKey,32
    )
    
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
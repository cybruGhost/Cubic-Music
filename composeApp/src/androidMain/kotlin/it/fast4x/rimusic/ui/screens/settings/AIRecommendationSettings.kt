package it.fast4x.rimusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.PlayEventsType
import it.fast4x.rimusic.enums.LocalRecommandationsNumber
import it.fast4x.rimusic.enums.RecommendationsNumber
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.enableQuickPicksPageKey
import it.fast4x.rimusic.utils.playEventsTypeKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.showChartsKey
import it.fast4x.rimusic.utils.showMonthlyPlaylistInQuickPicksKey
import it.fast4x.rimusic.utils.showMoodsAndGenresKey
import it.fast4x.rimusic.utils.showNewAlbumsArtistsKey
import it.fast4x.rimusic.utils.showNewAlbumsKey
import it.fast4x.rimusic.utils.showPlaylistMightLikeKey
import it.fast4x.rimusic.utils.showRelatedAlbumsKey
import it.fast4x.rimusic.utils.showSimilarArtistsKey
import it.fast4x.rimusic.utils.showTipsKey
import it.fast4x.rimusic.utils.recommendationsNumberKey
import it.fast4x.rimusic.utils.enableCreateMonthlyPlaylistsKey
import it.fast4x.rimusic.utils.showMonthlyPlaylistsKey
import it.fast4x.rimusic.utils.showMyTopPlaylistKey
import it.fast4x.rimusic.utils.showStatsListeningTimeKey
import it.fast4x.rimusic.utils.maxStatisticsItemsKey
import it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import it.fast4x.rimusic.enums.MaxStatisticsItems
import it.fast4x.rimusic.enums.MaxTopPlaylistItems
import kotlinx.coroutines.Dispatchers
import me.knighthat.utils.Toaster
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun DefaultAIRecommendationSettings() {
    var playEventType by rememberPreference(
        playEventsTypeKey,
        PlayEventsType.MostPlayed
    )
    playEventType = PlayEventsType.MostPlayed
    var showTips by rememberPreference(showTipsKey, true)
    showTips = true
    var showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    showRelatedAlbums = true
    var showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    showSimilarArtists = true
    var showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    showNewAlbumsArtists = true
    var showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    showNewAlbums = true
    var showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    showPlaylistMightLike = true
    var showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    showMoodsAndGenres = true
    var showMonthlyPlaylistInQuickPicks by rememberPreference(showMonthlyPlaylistInQuickPicksKey, true)
    showMonthlyPlaylistInQuickPicks = true
    var showCharts by rememberPreference(showChartsKey, true)
    showCharts = true
    var enableQuickPicksPage by rememberPreference(enableQuickPicksPageKey, true)
    enableQuickPicksPage = true
    var localRecommandationsNumber by rememberPreference(
        key = "LocalRecommandationsNumber",
        defaultValue = LocalRecommandationsNumber.SixQ
    )
    localRecommandationsNumber = LocalRecommandationsNumber.SixQ
    var recommendationsNumber by rememberPreference(recommendationsNumberKey, RecommendationsNumber.Adaptive)
    recommendationsNumber = RecommendationsNumber.Adaptive
    
    // Monthly Playlists Settings
    var enableCreateMonthlyPlaylists by rememberPreference(enableCreateMonthlyPlaylistsKey, true)
    enableCreateMonthlyPlaylists = true
    var showMonthlyPlaylists by rememberPreference(showMonthlyPlaylistsKey, true)
    showMonthlyPlaylists = true
    
    // Statistics Settings
    var showMyTopPlaylist by rememberPreference(showMyTopPlaylistKey, true)
    showMyTopPlaylist = true
    var showStatsListeningTime by rememberPreference(showStatsListeningTimeKey, true)
    showStatsListeningTime = true
    var maxStatisticsItems by rememberPreference(maxStatisticsItemsKey, MaxStatisticsItems.`10`)
    maxStatisticsItems = MaxStatisticsItems.`10`
    
    // Top Playlists Settings
    var maxTopPlaylistItems by rememberPreference(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`)
    maxTopPlaylistItems = MaxTopPlaylistItems.`10`
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun AIRecommendationSettings(
    navController: NavController
) {
    var playEventType by rememberPreference(
        playEventsTypeKey,
        PlayEventsType.MostPlayed
    )
    var showTips by rememberPreference(showTipsKey, true)
    var showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    var showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    var showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    var showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    var showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    var showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    var showMonthlyPlaylistInQuickPicks by rememberPreference(showMonthlyPlaylistInQuickPicksKey, true)
    var showCharts by rememberPreference(showChartsKey, true)
    var enableQuickPicksPage by rememberPreference(enableQuickPicksPageKey, true)
    var clearEvents by remember { mutableStateOf(false) }
    var localRecommandationsNumber by rememberPreference(
        key = "LocalRecommandationsNumber",
        defaultValue = LocalRecommandationsNumber.SixQ
    )
    var recommendationsNumber by rememberPreference(recommendationsNumberKey, RecommendationsNumber.Adaptive)
    
    // Monthly Playlists Settings
    var enableCreateMonthlyPlaylists by rememberPreference(enableCreateMonthlyPlaylistsKey, true)
    var showMonthlyPlaylists by rememberPreference(showMonthlyPlaylistsKey, true)
    
    // Statistics Settings
    var showMyTopPlaylist by rememberPreference(showMyTopPlaylistKey, true)
    var showStatsListeningTime by rememberPreference(showStatsListeningTimeKey, true)
    var maxStatisticsItems by rememberPreference(maxStatisticsItemsKey, MaxStatisticsItems.`10`)
    
    // Top Playlists Settings
    var maxTopPlaylistItems by rememberPreference(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`)
    
    if (clearEvents) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
            onDismiss = { clearEvents = false },
            onConfirm = {
                Database.asyncTransaction {
                    eventTable.deleteAll()
                    Toaster.done()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
            .verticalScroll(rememberScrollState())
    ) {
        HeaderWithIcon(
            title = if (!isYouTubeLoggedIn()) stringResource(R.string.ai_recommendations) else stringResource(R.string.home),
            iconId = if (!isYouTubeLoggedIn()) R.drawable.sparkles else R.drawable.ytmusic,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.quick_picks_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        ) 

        Spacer(modifier = Modifier.height(16.dp))

        // General Settings Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                animationSpec = tween(600),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.tab_general),
                icon = R.drawable.settings,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_quick_picks_page),
                        text = "",
                        isChecked = enableQuickPicksPage,
                        onCheckedChange = {
                            enableQuickPicksPage = it
                        },
                        icon = R.drawable.sparkles
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Sections
        AnimatedVisibility(
            visible = enableQuickPicksPage,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                animationSpec = tween(800),
                initialScale = 0.9f
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                animationSpec = tween(200),
                targetScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.quick_picks),
                icon = R.drawable.star_brilliant,
                content = {
                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.tips)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.tips),
                        isChecked = showTips,
                        onCheckedChange = {
                            showTips = it
                        },
                        icon = R.drawable.person
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.charts)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.charts),
                        isChecked = showCharts,
                        onCheckedChange = {
                            showCharts = it
                        },
                        icon = R.drawable.trending
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.related_albums)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.related_albums),
                        isChecked = showRelatedAlbums,
                        onCheckedChange = {
                            showRelatedAlbums = it
                        },
                        icon = R.drawable.album
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.similar_artists)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.similar_artists),
                        isChecked = showSimilarArtists,
                        onCheckedChange = {
                            showSimilarArtists = it
                        },
                        icon = R.drawable.people
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums_of_your_artists)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.new_albums_of_your_artists),
                        isChecked = showNewAlbumsArtists,
                        onCheckedChange = {
                            showNewAlbumsArtists = it
                        },
                        icon = R.drawable.alternative_version
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.new_albums),
                        isChecked = showNewAlbums,
                        onCheckedChange = {
                            showNewAlbums = it
                        },
                        icon = R.drawable.album
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.playlists_you_might_like)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.playlists_you_might_like),
                        isChecked = showPlaylistMightLike,
                        onCheckedChange = {
                            showPlaylistMightLike = it
                        },
                        icon = R.drawable.playlist
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.moods_and_genres)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.moods_and_genres),
                        isChecked = showMoodsAndGenres,
                        onCheckedChange = {
                            showMoodsAndGenres = it
                        },
                        icon = R.drawable.moods
                    )

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                        text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(R.string.monthly_playlists),
                        isChecked = showMonthlyPlaylistInQuickPicks,
                        onCheckedChange = {
                            showMonthlyPlaylistInQuickPicks = it
                        },
                        icon = R.drawable.calendar
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips Configuration Section
        AnimatedVisibility(
            visible = enableQuickPicksPage && showTips,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                animationSpec = tween(1000),
                initialScale = 0.9f
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                animationSpec = tween(200),
                targetScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.tips),
                icon = R.drawable.person,
                content = {
                    var showTipsDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.tips),
                        text = playEventType.text,
                        icon = R.drawable.sort_vertical,
                        onClick = { showTipsDialog = true }
                    )

                    if (showTipsDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.tips),
                            selectedValue = playEventType,
                            values = PlayEventsType.values().toList(),
                            onValueSelected = { playEventType = it },
                            valueText = { it.text },
                            onDismiss = { showTipsDialog = false }
                        )
                    }

                    var showQuickSelectionDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.quick_selection_type),
                        text = stringResource(R.string.quick_selection, localRecommandationsNumber.value),
                        icon = R.drawable.sparkles,
                        onClick = { showQuickSelectionDialog = true }
                    )

                    if (showQuickSelectionDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.quick_selection_type),
                            selectedValue = localRecommandationsNumber,
                            values = LocalRecommandationsNumber.values().toList(),
                            onValueSelected = { localRecommandationsNumber = it },
                            valueText = { option ->
                                stringResource(R.string.quick_selection, option.value)
                            },
                            onDismiss = { showQuickSelectionDialog = false }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Playlists Section
        AnimatedVisibility(
            visible = enableQuickPicksPage && showMonthlyPlaylistInQuickPicks,
            enter = fadeIn(animationSpec = tween(1400)) + scaleIn(
                animationSpec = tween(1400),
                initialScale = 0.9f
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                animationSpec = tween(200),
                targetScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.monthly_playlists),
                icon = R.drawable.calendar,
                content = {
                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.enable_monthly_playlists_creation),
                        text = "",
                        isChecked = enableCreateMonthlyPlaylists,
                        onCheckedChange = {
                            enableCreateMonthlyPlaylists = it
                        },
                        icon = R.drawable.calendar_clear
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Smart Recommendations Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1400)) + scaleIn(
                animationSpec = tween(1400),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.smart_recommendations),
                icon = R.drawable.smart_shuffle,
                content = {
                    var showRecommendationsDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.smart_recommendations_number),
                        text = if (recommendationsNumber == RecommendationsNumber.Adaptive) 
                            stringResource(R.string.smart_recommendations_adaptive_description) else recommendationsNumber.text,
                        icon = R.drawable.shuffle,
                        onClick = { showRecommendationsDialog = true }
                    )

                    if (showRecommendationsDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.smart_recommendations_number),
                            selectedValue = recommendationsNumber,
                            values = RecommendationsNumber.values().toList(),
                            onValueSelected = { recommendationsNumber = it },
                            valueText = { 
                                when (it) {
                                    RecommendationsNumber.Adaptive -> stringResource(R.string.smart_recommendations_adaptive)
                                    else -> it.text
                                }
                            },
                            onDismiss = { showRecommendationsDialog = false }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1600)) + scaleIn(
                animationSpec = tween(1600),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.statistics),
                icon = R.drawable.trending,
                content = {
                    var showStatisticsDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        text = maxStatisticsItems.name,
                        icon = R.drawable.musical_notes,
                        onClick = { showStatisticsDialog = true }
                    )

                    if (showStatisticsDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.statistics_max_number_of_items),
                            selectedValue = maxStatisticsItems,
                            values = MaxStatisticsItems.values().toList(),
                            onValueSelected = { maxStatisticsItems = it },
                            valueText = { it.name },
                            onDismiss = { showStatisticsDialog = false }
                        )
                    }

                    OtherSwitchSettingEntry(
                        title = stringResource(R.string.listening_time),
                        text = stringResource(R.string.shows_the_number_of_songs_heard_and_their_listening_time),
                        isChecked = showStatsListeningTime,
                        onCheckedChange = {
                            showStatsListeningTime = it
                        },
                        icon = R.drawable.time
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top Playlists Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1800)) + scaleIn(
                animationSpec = tween(1800),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.playlist_top),
                icon = R.drawable.playlist,
                content = {
                    var showTopPlaylistsDialog by remember { mutableStateOf(false) }
                    OtherSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        text = maxTopPlaylistItems.name,
                        icon = R.drawable.musical_notes,
                        onClick = { showTopPlaylistsDialog = true }
                    )

                    if (showTopPlaylistsDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.statistics_max_number_of_items),
                            selectedValue = maxTopPlaylistItems,
                            values = MaxTopPlaylistItems.values().toList(),
                            onValueSelected = { maxTopPlaylistItems = it },
                            valueText = { it.name },
                            onDismiss = { showTopPlaylistsDialog = false }
                        )
                    }

                    OtherSwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.my_playlist_top1)}",
                        text = "",
                        isChecked = showMyTopPlaylist,
                        onCheckedChange = {
                            showMyTopPlaylist = it
                        },
                        icon = R.drawable.trending
                    )
                }

            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Management Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(2000)) + scaleIn(
                animationSpec = tween(2000),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.tab_data),
                icon = R.drawable.server,
                content = {
                    val eventsCount by remember {
                        Database.eventTable
                                .countAll()
                    }.collectAsState( 0L, Dispatchers.IO )

                    OtherSettingsEntry(
                        title = stringResource(R.string.reset_quick_picks),
                        text = if (eventsCount > 0) {
                            stringResource(R.string.delete_playback_events, eventsCount)
                        } else {
                            stringResource(R.string.quick_picks_are_cleared)
                        },
                        icon = R.drawable.trash,
                        onClick = { clearEvents = true }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset to Default Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(2200)) + scaleIn(
                animationSpec = tween(2200),
                initialScale = 0.9f
            )
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.settings_reset),
                icon = R.drawable.refresh,
                content = {
                    var resetToDefault by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    OtherSettingsEntry(
                        title = stringResource(R.string.settings_reset),
                        text = stringResource(R.string.settings_restore_default_settings),
                        icon = R.drawable.refresh,
                        onClick = { resetToDefault = true }
                    )
                    if (resetToDefault) {
                        DefaultAIRecommendationSettings()
                        resetToDefault = false
                        navController.popBackStack()
                        Toaster.done()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}

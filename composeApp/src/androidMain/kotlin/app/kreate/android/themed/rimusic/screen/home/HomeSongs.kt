package app.kreate.android.themed.rimusic.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.relatedSongs
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.EXPLICIT_PREFIX
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.BuiltInPlaylist
import it.fast4x.rimusic.enums.DurationInMinutes
import it.fast4x.rimusic.enums.MaxTopPlaylistItems
import it.fast4x.rimusic.enums.RecommendationsNumber
import it.fast4x.rimusic.enums.SongSortBy
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import it.fast4x.rimusic.service.modern.isLocal
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.items.SongItemPlaceholder
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.overlay
import it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import it.fast4x.rimusic.utils.Preference
import it.fast4x.rimusic.utils.Preference.HOME_SONGS_SORT_BY
import it.fast4x.rimusic.utils.Preference.HOME_SONGS_SORT_ORDER
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.durationTextToMillis
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.excludeSongsWithDurationLimitKey
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.includeLocalSongsKey
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.parentalControlEnabledKey
import it.fast4x.rimusic.utils.recommendationsNumberKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.knighthat.component.SongItem
import me.knighthat.component.Sort
import me.knighthat.component.song.PeriodSelector
import me.knighthat.component.tab.DeleteAllDownloadedSongsDialog
import me.knighthat.component.tab.DownloadAllSongsDialog
import me.knighthat.component.tab.ExportSongsToCSVDialog
import me.knighthat.component.tab.HiddenSongs
import me.knighthat.component.tab.ItemSelector
import me.knighthat.component.tab.Search
import me.knighthat.database.ext.FormatWithSong

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun HomeSongs(
    navController: NavController,
    builtInPlaylist: BuiltInPlaylist,
    lazyListState: LazyListState,
    itemSelector: ItemSelector<Song>,
    search: Search,
    buttons: MutableList<Button>,
    itemsOnDisplay: MutableList<Song>,
    getSongs: () -> List<Song>,
    onRecommendationCountChange: (Int) -> Unit = {},
    onRecommendationsLoadingChange: (Boolean) -> Unit = {},
    isRecommendationEnabled: Boolean = false,
) {
    // Essentials
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current

    //<editor-fold defaultstate="collapsed" desc="Settings">
    val parentalControlEnabled by rememberPreference( parentalControlEnabledKey, false )
    val maxTopPlaylistItems by rememberPreference( MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10` )
    val includeLocalSongs by rememberPreference( includeLocalSongsKey, true )
    val excludeSongWithDurationLimit by rememberPreference( excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled )
    //</editor-fold>

    var items by remember { mutableStateOf(emptyList<Song>()) }

    val songSort = Sort ( HOME_SONGS_SORT_BY, HOME_SONGS_SORT_ORDER )
    val topPlaylists = PeriodSelector( Preference.HOME_SONGS_TOP_PLAYLIST_PERIOD )
    val hiddenSongs = HiddenSongs()
    val exportDialog = ExportSongsToCSVDialog(
        playlistName = builtInPlaylist.text,
        songs = getSongs
    )
    val downloadAllDialog = DownloadAllSongsDialog( getSongs )
    val deleteDownloadsDialog = DeleteAllDownloadedSongsDialog( getSongs )

    /**
     * This variable tells [LazyColumn] to render [SongItemPlaceholder]
     * instead of [SongItem] queried from the database.
     *
     * This indication also tells user that songs are being loaded
     * and not it's definitely not freezing up.
     *
     * > This variable should **_NOT_** be set to `false` while inside **first** phrase,
     * and should **_NOT_** be set to `true` while in **second** phrase.
     */
    var isLoading by remember { mutableStateOf(false) }

    //<editor-fold defaultstate="collapsed" desc="Smart recommendation state">
    val recommendationsNumber by rememberPreference( recommendationsNumberKey, RecommendationsNumber.Adaptive )
    var relatedSongs by remember { mutableStateOf(emptyList<Song>()) }
    var relatedSongsPositions by remember { mutableStateOf(emptyMap<Song, Int>()) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }
    //</editor-fold>

    // This phrase loads all songs across types into [items]
    // No filtration applied to this stage, only sort
    LaunchedEffect( builtInPlaylist, topPlaylists.period, songSort.sortBy, songSort.sortOrder, hiddenSongs.isFirstIcon ) {
        isLoading = true

        val retrievedSongs = when( builtInPlaylist ) {
            BuiltInPlaylist.All -> Database.songTable
                                           .sortAll( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() )
                                           .map { list ->
                                               // Include local songs if enabled
                                               list.fastFilter {
                                                   !includeLocalSongs || !it.id.startsWith( LOCAL_KEY_PREFIX, true )
                                               }
                                           }

            BuiltInPlaylist.Downloaded -> {
                // [MyDownloadHelper] provide a list of downloaded songs, which is faster to retrieve
                // than using `Cache.isCached()` call
                val downloaded: List<String> = MyDownloadHelper.downloads
                                                               .value
                                                               .values
                                                               .filter { it.state == Download.STATE_COMPLETED }
                                                               .fastMap { it.request.id }
                Database.songTable
                        .sortAll( songSort.sortBy, songSort.sortOrder )
                        .map { list ->
                            list.fastFilter { it.id in downloaded }
                        }
            }

            BuiltInPlaylist.Offline -> Database.formatTable
                                               .sortAllWithSongs( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() )
                                               .map { list ->
                                                   list.fastFilter {
                                                        val contentLength = it.format.contentLength ?: return@fastFilter false
                                                        binder?.cache?.isCached( it.song.id, 0, contentLength ) == true
                                                    }.map( FormatWithSong::song )
                                               }

            BuiltInPlaylist.Favorites -> Database.songTable.sortFavorites( songSort.sortBy, songSort.sortOrder )

            BuiltInPlaylist.Top -> Database.eventTable
                                           .findSongsMostPlayedBetween(
                                               from = topPlaylists.period.timeStampInMillis(),
                                               limit = maxTopPlaylistItems.toInt()
                                           )
                                           .map { list ->
                                               // Exclude songs with duration higher than what [excludeSongWithDurationLimit] is
                                               list.fastFilter { song ->
                                                   excludeSongWithDurationLimit == DurationInMinutes.Disabled
                                                           || song.durationText
                                                                  ?.let { durationTextToMillis(it) < excludeSongWithDurationLimit.asMillis } == true
                                               }
                                           }

            BuiltInPlaylist.OnDevice -> flowOf( emptyList() )
        }

        retrievedSongs.flowOn( Dispatchers.IO )
                      .distinctUntilChanged()
                      .collect { items = it }
    }

    LaunchedEffect(isRecommendationEnabled, items) {
        if (!isRecommendationEnabled || items.isEmpty()) {
            relatedSongs = emptyList()
            isRecommendationsLoading = false
            onRecommendationsLoadingChange(false)
            return@LaunchedEffect
        }

        // If we already have recommendations and the list size hasn't changed significantly,
        // we don't recalculate to avoid unnecessary recalculations during playback
        if (relatedSongs.isNotEmpty() && 
            relatedSongs.size >= recommendationsNumber.calculateAdaptiveRecommendations(items.size) * 0.8) {
            return@LaunchedEffect
        }

        isRecommendationsLoading = true
        onRecommendationsLoadingChange(true)

        val targetRecommendations = recommendationsNumber.calculateAdaptiveRecommendations(items.size)
        val allRelatedSongs = mutableListOf<Song>()
        val existingSongIds = items.map { it.id }.toSet()

        val numberOfRequests = when {
            items.size <= 100 -> 1
            items.size <= 500 -> 3
            items.size <= 1000 -> 5
            items.size <= 2000 -> 8
            else -> 10
        }

        val seedSongs = items.shuffled().take(numberOfRequests)

        for (seedSong in seedSongs) {
            try {
                val requestBody = NextBody(videoId = seedSong.id)
                val relatedSongsResult = Innertube.relatedSongs(requestBody)?.getOrNull()

                relatedSongsResult?.songs?.forEach { songItem ->
                    songItem.info?.let { info ->
                        info.endpoint?.videoId?.let { videoId ->
                            if (!existingSongIds.contains(videoId)) {
                                val prefix = if (songItem.explicit) EXPLICIT_PREFIX else ""
                                val song = Song(
                                    id = "$prefix$videoId",
                                    title = info.name!!,
                                    artistsText = songItem.authors?.joinToString { author -> author.name ?: "" },
                                    durationText = songItem.durationText,
                                    thumbnailUrl = songItem.thumbnail?.url
                                )

                                if (!allRelatedSongs.any { it.id == song.id }) {
                                    allRelatedSongs.add(song)
                                }
                            }
                        }
                    }
                }

                if (numberOfRequests > 1) delay(200L)

            } catch (e: Exception) {
                continue
            }
        }

        relatedSongs = allRelatedSongs.take(targetRecommendations)
        
        // Assign stable positions to recommendations
        val newPositions = relatedSongs.associate { song ->
            song to (0..items.size).random()
        }
        relatedSongsPositions = newPositions
        
        isRecommendationsLoading = false
        onRecommendationsLoadingChange(false)
    }

    LaunchedEffect( items, search.inputValue, isRecommendationEnabled, relatedSongsPositions ) {
        items.toMutableList()
             .apply {
                 if (isRecommendationEnabled) {
                     relatedSongsPositions.forEach { (song, position) ->
                         // Use the memorized position, but ensure it's within bounds
                         val safePosition = position.coerceIn(0, size)
                         add( safePosition, song )
                     }
                 }
             }
             .distinctBy( Song::id )
             .filter { !parentalControlEnabled || !it.title.startsWith( EXPLICIT_PREFIX, true ) }
             .filter { song ->
                 val containsTitle = song.cleanTitle().contains( search.inputValue, true )
                 val containsArtist = song.cleanArtistsText().contains( search.inputValue, true )
                 containsTitle || containsArtist
             }
             .let { 
                 itemsOnDisplay.clear()
                 itemsOnDisplay.addAll(it)
             }

        isLoading = false
    }

    LaunchedEffect( relatedSongs.size, isRecommendationEnabled ) {
        if (isRecommendationEnabled) {
            onRecommendationCountChange(relatedSongs.size)
        } else {
            onRecommendationCountChange(0)
        }
    }

    LaunchedEffect( builtInPlaylist ) {
        val firstButton = if( builtInPlaylist == BuiltInPlaylist.Top ) topPlaylists else songSort
        buttons.add( 0, firstButton )
        buttons.add( 3, downloadAllDialog )
        buttons.add( 4, deleteDownloadsDialog )
        buttons.add( exportDialog )
    }

    //<editor-fold defaultstate="collapsed" desc="Dialog Renders">
    exportDialog.Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()
    //</editor-fold>

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = !isLoading,
        contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer ),
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxSize()
    ) {
        if( isLoading )
            items(
                count = 20,
                key = { it }
            ) { SongItemPlaceholder() }

        itemsIndexed(
            items = itemsOnDisplay,
            key = { _, song -> song.id }
        ) { index, song ->
            val mediaItem = song.asMediaItem

            val isLocal by remember { derivedStateOf { mediaItem.isLocal } }
            val isDownloaded = isLocal || isDownloadedSong( mediaItem.mediaId )

            SwipeablePlaylistItem(
                mediaItem = mediaItem,
                onPlayNext = { binder?.player?.addNext( mediaItem ) },
                onDownload = {
                    if( builtInPlaylist != BuiltInPlaylist.OnDevice ) {
                        binder?.cache?.removeResource(mediaItem.mediaId)
                        Database.asyncTransaction {
                            formatTable.updateContentLengthOf( mediaItem.mediaId )
                        }
                        if ( !isLocal )
                            manageDownload(
                                context = context,
                                mediaItem = mediaItem,
                                downloadState = isDownloaded
                            )
                    }
                },
                onEnqueue = {
                    binder?.player?.enqueue(mediaItem)
                }
            ) {
                val isRecommended = song in relatedSongs

                SongItem(
                    song = song,
                    itemSelector = itemSelector,
                    navController = navController,
                    isRecommended = isRecommended,
                    modifier = Modifier.animateItem(),
                    thumbnailOverlay = {
                        if ( songSort.sortBy == SongSortBy.PlayTime || builtInPlaylist == BuiltInPlaylist.Top ) {
                            var text = song.formattedTotalPlayTime
                            var typography = typography().xxs
                            var alignment = Alignment.BottomCenter

                            if( builtInPlaylist == BuiltInPlaylist.Top ) {
                                text = (index + 1).toString()
                                typography = typography().m
                                alignment = Alignment.Center
                            }

                            BasicText(
                                text = text,
                                style = typography.semiBold.center.color(colorPalette().onOverlay),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(alignment)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                colorPalette().overlay
                                            )
                                        ),
                                        shape = thumbnailShape()
                                    )
                            )
                        }
                    },
                    onClick = {
                        search.hideIfEmpty()

                        binder?.stopRadio()

                        val mediaItems = getSongs().fastMap( Song::asMediaItem )
                        binder?.player?.forcePlayAtIndex( mediaItems, index )
                    }
                )
            }

        }
    }
}
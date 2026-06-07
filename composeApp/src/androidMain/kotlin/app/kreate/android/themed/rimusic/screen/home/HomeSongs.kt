package app.kreate.android.themed.rimusic.screen.home

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.relatedSongs
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.BuiltInPlaylist
import app.it.fast4x.rimusic.enums.DurationInMinutes
import app.it.fast4x.rimusic.enums.MaxTopPlaylistItems
import app.it.fast4x.rimusic.enums.RecommendationsNumber
import app.it.fast4x.rimusic.enums.SongSortBy
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.items.SongItemPlaceholder
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.onOverlay
import app.it.fast4x.rimusic.ui.styling.overlay
import app.it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.Preference.HOME_SONGS_SORT_BY
import app.it.fast4x.rimusic.utils.Preference.HOME_SONGS_SORT_ORDER
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.center
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.durationTextToMillis
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.excludeSongsWithDurationLimitKey
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.it.fast4x.rimusic.utils.includeLocalSongsKey
import app.it.fast4x.rimusic.utils.importYTMLikedSongs
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.parentalControlEnabledKey
import app.it.fast4x.rimusic.utils.PlaybackContextStore
import app.it.fast4x.rimusic.utils.recommendationsNumberKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import app.kreate.android.me.knighthat.component.SongItem
import app.kreate.android.me.knighthat.component.Sort
import app.kreate.android.me.knighthat.component.song.PeriodSelector
import app.kreate.android.me.knighthat.component.tab.DeleteAllDownloadedSongsDialog
import app.kreate.android.me.knighthat.component.tab.DownloadAllSongsDialog
import app.kreate.android.me.knighthat.component.tab.ExportSongsToCSVDialog
import app.kreate.android.me.knighthat.component.tab.HiddenSongs
import app.kreate.android.me.knighthat.component.tab.ItemSelector
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.database.ext.FormatWithSong
import app.kreate.android.themed.rimusic.component.AlphabetIndexBar
import app.kreate.android.themed.rimusic.component.buildSongAlphabetIndex
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    //<editor-fold defaultstate="collapsed" desc="Settings">
    val parentalControlEnabled by rememberPreference( parentalControlEnabledKey, false )
    val maxTopPlaylistItems by rememberPreference( MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10` )
    val includeLocalSongs by rememberPreference( includeLocalSongsKey, true )
    val excludeSongWithDurationLimit by rememberPreference( excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled )
    val customDownloadUri by rememberPreference(MyDownloadHelper.CUSTOM_DOWNLOAD_URI_KEY, "")
    //</editor-fold>

    var items by remember { mutableStateOf(emptyList<Song>()) }
    var customFolderSongs by remember { mutableStateOf(emptyList<Song>()) }
    var ytmFavoritesSynced by remember { mutableStateOf(false) }

    val songSort = Sort ( HOME_SONGS_SORT_BY, HOME_SONGS_SORT_ORDER )
    val topPlaylists = PeriodSelector( Preference.HOME_SONGS_TOP_PLAYLIST_PERIOD )
    val hiddenSongs = HiddenSongs()
    val exportDialog = ExportSongsToCSVDialog(
        playlistName = builtInPlaylist.text,
        songs = getSongs
    )
    val downloadAllDialog = DownloadAllSongsDialog(
        getSongs = getSongs,
        redownloadExisting = builtInPlaylist == BuiltInPlaylist.CorruptDownloads,
        titleId = if (builtInPlaylist == BuiltInPlaylist.CorruptDownloads) {
            R.string.do_you_really_want_to_redownload_corrupt
        } else {
            R.string.do_you_really_want_to_download_all
        },
        menuTitleId = if (builtInPlaylist == BuiltInPlaylist.CorruptDownloads) {
            R.string.redownload_all_corrupt_songs
        } else {
            R.string.download
        },
        messageTitleId = if (builtInPlaylist == BuiltInPlaylist.CorruptDownloads) {
            R.string.redownload_all_corrupt_songs
        } else {
            R.string.info_download_all_songs
        }
    )
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

    LaunchedEffect(customDownloadUri) {
        customFolderSongs = emptyList()
        if (customDownloadUri.isBlank()) return@LaunchedEffect

        customFolderSongs = loadCustomDownloadFolderSongs(context, customDownloadUri)
    }

    //<editor-fold defaultstate="collapsed" desc="Smart recommendation state">
    val recommendationsNumber by rememberPreference( recommendationsNumberKey, RecommendationsNumber.Adaptive )
    var relatedSongs by remember { mutableStateOf(emptyList<Song>()) }
    var relatedSongsPositions by remember { mutableStateOf(emptyMap<Song, Int>()) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }
    //</editor-fold>

    LaunchedEffect(builtInPlaylist) {
        if (builtInPlaylist == BuiltInPlaylist.Favorites && isYouTubeSyncEnabled() && !ytmFavoritesSynced) {
            importYTMLikedSongs()
            ytmFavoritesSynced = true
        }
    }

    // This phrase loads all songs across types into [items]
    // No filtration applied to this stage, only sort
    LaunchedEffect( builtInPlaylist, topPlaylists.period, songSort.sortBy, songSort.sortOrder, hiddenSongs.isFirstIcon, customFolderSongs ) {
        isLoading = true

        val retrievedSongs = when( builtInPlaylist ) {
            BuiltInPlaylist.All -> Database.songTable
                                           .sortAll( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() )
                                           .map { list ->
                                               // Include local songs if enabled
                                               list.fastFilter {
                                                   includeLocalSongs || !it.id.startsWith( LOCAL_KEY_PREFIX, true )
                                               }
                                           }

            BuiltInPlaylist.Downloaded -> {
                // [MyDownloadHelper] provide a list of downloaded songs, which is faster to retrieve
                // than using `Cache.isCached()` call
                val downloaded: Set<String> = MyDownloadHelper.downloads
                                                               .value
                                                               .values
                                                               .filter {
                                                                   it.state == Download.STATE_COMPLETED &&
                                                                       MyDownloadHelper.isDownloadCached(it.request.id)
                                                               }
                                                               .fastMap { it.request.id }
                                                               .toSet()
                val customFolderSongsSnapshot = customFolderSongs
                Database.songTable
                        .sortAll( songSort.sortBy, songSort.sortOrder )
                        .map { list ->
                            (list.fastFilter { it.id in downloaded } + customFolderSongsSnapshot)
                                .distinctBy(Song::id)
                                .sortedForHome(songSort.sortBy, songSort.sortOrder)
                        }
            }

            BuiltInPlaylist.CorruptDownloads -> {
                val corruptDownloaded: Set<String> = MyDownloadHelper.downloads
                    .value
                    .values
                    .filter {
                        it.state == Download.STATE_FAILED ||
                            (
                                it.state == Download.STATE_COMPLETED &&
                                    !MyDownloadHelper.isDownloadCached(it.request.id)
                                )
                    }
                    .fastMap { it.request.id }
                    .toSet()
                Database.songTable
                    .sortAll(songSort.sortBy, songSort.sortOrder)
                    .map { list ->
                        list.fastFilter { it.id in corruptDownloaded }
                            .sortedForHome(songSort.sortBy, songSort.sortOrder)
                    }
            }

            BuiltInPlaylist.Offline -> Database.formatTable
                                               .sortAllWithSongs( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() )
                                               .map { list ->
                                                   list.fastFilter {
                                                        val hasAnyCachedSpan = runCatching {
                                                            binder?.cache?.getCachedSpans(it.song.id)?.isNotEmpty() == true
                                                        }.getOrDefault(false)
                                                        val contentLength = it.format.contentLength
                                                        hasAnyCachedSpan || (
                                                            contentLength != null &&
                                                                binder?.cache?.isCached( it.song.id, 0, contentLength ) == true
                                                            )
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
                       .collect { 
                          items = it
                          isLoading = false
                      }
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
        val filteredItems = items
             .toMutableList()
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

        itemsOnDisplay.clear()
        itemsOnDisplay.addAll(filteredItems)
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
        buttons.removeAll { it === songSort || it === topPlaylists || it === downloadAllDialog || it === deleteDownloadsDialog || it === exportDialog }
        buttons.add( 0, firstButton )
        buttons.add( minOf(3, buttons.size), downloadAllDialog )
        if (builtInPlaylist != BuiltInPlaylist.CorruptDownloads) {
            buttons.add( minOf(4, buttons.size), deleteDownloadsDialog )
        }
        if( exportDialog !in buttons ) buttons.add( exportDialog )
    }

    //<editor-fold defaultstate="collapsed" desc="Dialog Renders">
    exportDialog.Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()
    //</editor-fold>

    val bulkDownloadIds by MyDownloadHelper.bulkDownloadIds.collectAsState()
    val downloadProgresses by MyDownloadHelper.progresses.collectAsState()
    val downloadStates by MyDownloadHelper.downloads.collectAsState()

    val visibleBulkDownloadSongs = remember(items, bulkDownloadIds, downloadStates) {
        val songsById = items.associateBy { it.id }
        bulkDownloadIds.mapNotNull { id ->
            songsById[id]?.takeIf { song ->
                downloadStates[song.id]?.state in setOf(
                    Download.STATE_DOWNLOADING,
                    Download.STATE_QUEUED,
                    Download.STATE_RESTARTING,
                    Download.STATE_COMPLETED
                )
            }
        }
    }

    val alphabetIndex = remember(itemsOnDisplay.toList()) {
        buildSongAlphabetIndex(itemsOnDisplay)
    }
    val listHeaderOffset = if (visibleBulkDownloadSongs.isNotEmpty()) 1 else 0

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = !isLoading,
            contentPadding = PaddingValues(
                end = 34.dp,
                bottom = Dimensions.bottomSpacer
            ),
            modifier = Modifier.fillMaxSize()
        ) {
        if (visibleBulkDownloadSongs.isNotEmpty()) {
            val total = visibleBulkDownloadSongs.size
            val completed = visibleBulkDownloadSongs.count { song ->
                downloadStates[song.id]?.state == Download.STATE_COMPLETED
            }
            val active = visibleBulkDownloadSongs.count { song ->
                downloadStates[song.id]?.state in setOf(
                    Download.STATE_DOWNLOADING,
                    Download.STATE_QUEUED,
                    Download.STATE_RESTARTING
                )
            }
            val progress = (
                visibleBulkDownloadSongs.sumOf { song ->
                    when (downloadStates[song.id]?.state) {
                        Download.STATE_COMPLETED -> 1.0
                        Download.STATE_DOWNLOADING -> downloadProgresses[song.id]?.toDouble() ?: 0.0
                        else -> 0.0
                    }
                }.toFloat() / total.toFloat()
                ).coerceIn(0f, 1f)

            item(key = "bulk_download_progress") {
                Surface(
                    color = colorPalette().background1,
                    shape = thumbnailShape(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            text = context.getString(R.string.download_all_progress_title, completed, total),
                            style = typography().xs.semiBold,
                            maxLines = 1
                        )
                        BasicText(
                            text = context.getString(R.string.download_all_progress_subtitle, active),
                            style = typography().xxs.color(colorPalette().textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            color = colorPalette().accent,
                            trackColor = colorPalette().background3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        visibleBulkDownloadSongs
                            .firstOrNull { song ->
                                downloadStates[song.id]?.state in setOf(
                                    Download.STATE_DOWNLOADING,
                                    Download.STATE_QUEUED,
                                    Download.STATE_RESTARTING
                                )
                            }
                            ?.let { activeSong ->
                                BasicText(
                                    text = context.getString(
                                        R.string.download_all_progress_current_song,
                                        activeSong.title
                                    ),
                                    style = typography().xxs.color(colorPalette().text),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                    }
                }
            }
        }

        if( isLoading )
            items(
                count = 20,
                key = { it }
            ) { SongItemPlaceholder() }

        itemsIndexed(
            items = itemsOnDisplay,
            key = { index, song -> song.id.ifBlank { "home_song_$index" } }
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

                        val mediaItems = itemsOnDisplay.fastMap( Song::asMediaItem )
                        PlaybackContextStore.set("Playing from ${context.resources.getString(builtInPlaylist.textId)}")
                        binder?.player?.forcePlayAtIndex( mediaItems, index )
                    }
                )
            }

        }
        }

        if (!isLoading && search.inputValue.isBlank() && alphabetIndex.size > 1 && itemsOnDisplay.size >= 20) {
            AlphabetIndexBar(
                alphabetIndex = alphabetIndex,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) { letter ->
                alphabetIndex[letter]?.let { songIndex ->
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(songIndex + listHeaderOffset)
                    }
                }
            }
        }
    }
}

private suspend fun loadCustomDownloadFolderSongs(
    context: android.content.Context,
    treeUriString: String,
): List<Song> = kotlinx.coroutines.withContext(Dispatchers.IO) {
    if (treeUriString.isBlank()) return@withContext emptyList()

    val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return@withContext emptyList()
    root.walkAudioFiles().mapNotNull { file ->
        val fileUri = file.uri
        val fileName = file.name?.substringBeforeLast(".").orEmpty()
        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, fileUri)
            val metadataTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
            val metadataArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?.takeUnless { it.equals("unknown", ignoreCase = true) || it.equals("<unknown>", ignoreCase = true) }
            val filenameParts = splitArtistTitle(fileName)
            val title = metadataTitle ?: filenameParts?.second ?: fileName
            val artist = metadataArtist ?: filenameParts?.first
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            Song(
                id = fileUri.toString(),
                title = title.ifBlank { file.name ?: fileUri.lastPathSegment.orEmpty() },
                artistsText = artist,
                durationText = durationMs?.toDurationText(),
                thumbnailUrl = null
            )
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }.distinctBy(Song::id)
}

private fun DocumentFile.walkAudioFiles(): List<DocumentFile> {
    if (!exists()) return emptyList()
    if (isFile) return listOfNotNull(takeIf { it.isPlayableAudioFile() })
    return listFiles().flatMap { child ->
        when {
            child.isDirectory -> child.walkAudioFiles()
            child.isPlayableAudioFile() -> listOf(child)
            else -> emptyList()
        }
    }
}

private fun List<Song>.sortedForHome(sortBy: SongSortBy, sortOrder: app.it.fast4x.rimusic.enums.SortOrder): List<Song> {
    if (sortBy in setOf(SongSortBy.DateAdded, SongSortBy.DatePlayed, SongSortBy.AlbumName)) {
        return toList()
    }

    val sorted = when (sortBy) {
        SongSortBy.PlayTime -> sortedBy(Song::totalPlayTimeMs)
        SongSortBy.RelativePlayTime -> sortedBy(Song::relativePlayTime)
        SongSortBy.Title -> sortedBy { it.cleanTitle().lowercase() }
        SongSortBy.Artist -> sortedBy { it.cleanArtistsText().lowercase() }
        SongSortBy.Duration -> sortedBy { durationTextToMillis(it.durationText.orEmpty()) }
        SongSortBy.DateLiked -> sortedBy { it.likedAt ?: Long.MIN_VALUE }
        SongSortBy.DateAdded,
        SongSortBy.DatePlayed,
        SongSortBy.AlbumName -> toList()
    }
    return sortOrder.applyTo(sorted)
}

private fun splitArtistTitle(name: String): Pair<String, String>? {
    val parts = name.split(Regex("\\s+-\\s+"), limit = 2)
    if (parts.size != 2) return null
    val artist = parts[0].trim()
    val title = parts[1].trim()
    return if (artist.isNotBlank() && title.isNotBlank()) artist to title else null
}

private fun DocumentFile.isPlayableAudioFile(): Boolean {
    val mime = type.orEmpty()
    if (mime.startsWith("audio/", ignoreCase = true)) return true
    val extension = name?.substringAfterLast('.', "").orEmpty().lowercase()
    return extension in setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "mp4")
}

private fun Long.toDurationText(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

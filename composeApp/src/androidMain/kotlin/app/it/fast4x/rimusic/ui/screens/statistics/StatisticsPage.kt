package app.it.fast4x.rimusic.ui.screens.statistics

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.MaxStatisticsItems
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.StatisticsCategory
import app.it.fast4x.rimusic.enums.StatisticsType
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import app.it.fast4x.rimusic.ui.items.AlbumItem
import app.it.fast4x.rimusic.ui.items.ArtistItem
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.ui.items.SongItem
import app.it.fast4x.rimusic.ui.screens.settings.SettingsEntry
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.ui.styling.shimmer
import app.it.fast4x.rimusic.utils.UpdateYoutubeAlbum
import app.it.fast4x.rimusic.utils.UpdateYoutubeArtist
import app.it.fast4x.rimusic.utils.PlaybackContextStore
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.center
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.it.fast4x.rimusic.utils.formatAsTime
import app.it.fast4x.rimusic.utils.getDownloadState
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.isNowPlaying
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.maxStatisticsItemsKey
import app.it.fast4x.rimusic.utils.navigationBarPositionKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showStatsListeningTimeKey
import app.it.fast4x.rimusic.utils.statisticsCategoryKey
import app.kreate.android.me.knighthat.coil.thumbnail
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.enqueue
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.kreate.android.me.knighthat.coil.resolveArtworkUrl
import app.kreate.android.me.knighthat.database.ext.AlbumListeningStat
import app.kreate.android.me.knighthat.database.ext.ArtistListeningStat
import app.kreate.android.me.knighthat.database.ext.SongListeningStat
import kotlin.math.min
import kotlin.math.roundToInt


@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun StatisticsPage(
    navController: NavController,
    statisticsType: StatisticsType
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val showStatsListeningTime by rememberPreference(showStatsListeningTimeKey, true)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val context = LocalContext.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSize = thumbnailSizeDp.px

    val maxStatisticsItems by rememberPreference( maxStatisticsItemsKey, MaxStatisticsItems.`10` )
    val from = remember( statisticsType ) { statisticsType.timeStampInMillis() }

    val artistStats by remember {
        Database.eventTable
                .findArtistListeningStatsBetween(
                    from = from,
                    limit = maxStatisticsItems.toInt()
                )
                .distinctUntilChanged()
    }.collectAsState( emptyList(), Dispatchers.IO )
    val artists = artistStats.map(ArtistListeningStat::artist)
    val albumStats by remember {
        Database.eventTable
                .findAlbumListeningStatsBetween(
                    from = from,
                    limit = maxStatisticsItems.toInt()
                )
                .distinctUntilChanged()
    }.collectAsState( emptyList(), Dispatchers.IO )
    val albums = albumStats.map(AlbumListeningStat::album)
    val playlists by remember {
        Database.eventTable
                .findPlaylistMostPlayedBetweenAsPreview(
                    from = from,
                    limit = maxStatisticsItems.toInt()
                )
                .distinctUntilChanged()
    }.collectAsState( emptyList(), Dispatchers.IO )
    var totalPlayTimes by remember { mutableLongStateOf(0L) }
    val totalPlayTimesFlow = remember(from) {
        Database.eventTable
            .findSongsMostPlayedBetween(
                from = from,
                limit = Int.MAX_VALUE
            )
            .distinctUntilChanged()
            .map { it.sumOf(Song::totalPlayTimeMs) }
    }
    val totalPlayTimesState = totalPlayTimesFlow.collectAsState(0L, Dispatchers.IO)
    totalPlayTimes = totalPlayTimesState.value

    val songStats by remember {
        Database.eventTable
            .findSongListeningStatsBetween(
                from = from,
                limit = maxStatisticsItems.toInt()
            )
            .distinctUntilChanged()
    }.collectAsState(emptyList(), Dispatchers.IO)
    val songs = songStats.map(SongListeningStat::song)

    val navigationBarPosition by rememberPreference(

        navigationBarPositionKey,
        NavigationBarPosition.Bottom
    )

    var statisticsCategory by rememberPreference(
        statisticsCategoryKey,
        StatisticsCategory.Songs
    )
    val buttonsList = listOf(
        StatisticsCategory.Songs to StatisticsCategory.Songs.text,
        StatisticsCategory.Artists to StatisticsCategory.Artists.text,
        StatisticsCategory.Albums to StatisticsCategory.Albums.text,
        StatisticsCategory.Playlists to StatisticsCategory.Playlists.text
    )
    val statisticsPeriodText = statisticsType.text

    // Calcul of real listening time for the selected period (Songs category)
    var totalPlayTimesSongs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(songs, from) {
        var total = 0L
        songs.forEach { song ->
            // Get the sum of playtime for the period
            val playTime = Database.eventTable.getSongPlayTimeBetween(song.id, from).first()
            total += playTime
        }
        totalPlayTimesSongs = total
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
    ) {
            val lazyGridState = rememberLazyGridState()
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(
                    if(statisticsCategory == StatisticsCategory.Songs) 200.dp else playlistThumbnailSizeDp
                ),
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {

                item(
                    key = "header",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    HeaderWithIcon(
                        title = statisticsPeriodText,
                        iconId = statisticsType.iconId,
                        enabled = true,
                        showIcon = true,
                        modifier = Modifier,
                        onClick = {}
                    )
                }

                item(
                    key = "total_time_listened",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    ListeningTimeHero(
                        entries = when (statisticsCategory) {
                            StatisticsCategory.Songs -> songStats.map {
                                ListeningWheelEntry(
                                    it.playTimeMs,
                                    resolveArtworkUrl(it.song.id, it.song.thumbnailUrl)
                                )
                            }
                            StatisticsCategory.Artists -> artistStats.map {
                                ListeningWheelEntry(it.playTimeMs, it.artist.thumbnailUrl)
                            }
                            StatisticsCategory.Albums -> albumStats.map {
                                ListeningWheelEntry(it.playTimeMs, it.album.thumbnailUrl)
                            }
                            StatisticsCategory.Playlists -> emptyList()
                        },
                        totalTimeText = formatAsTime(totalPlayTimes),
                        periodText = statisticsPeriodText
                    )
                }

                item(
                    key = "header_tabs",
                    span = { GridItemSpan(maxLineSpan) }
                ) {

                    ButtonsRow(
                        chips = buttonsList,
                        currentValue = statisticsCategory,
                        onValueUpdate = { statisticsCategory = it },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                }

                if (statisticsCategory == StatisticsCategory.Songs) {

                        if (showStatsListeningTime)
                            item(
                                key = "headerListeningTime",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                                    SettingsEntry(
                                        title = "${songs.size} ${stringResource(R.string.statistics_songs_heard)}",
                                        text = "${formatAsTime(totalPlayTimesSongs)} ${stringResource(R.string.statistics_of_time_taken)}",
                                        onClick = {},
                                        trailingContent = {
                                            Image(
                                                painter = painterResource(R.drawable.musical_notes),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette().shimmer),
                                                modifier = Modifier
                                                    .size(34.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .background(
                                                color = colorPalette().background4,
                                                shape = thumbnailRoundness.shape
                                            )

                                    )
                                }
                            }


                    items(
                        count = songs.count(),
                    ) {
                        val currentDownloadState = getDownloadState(songs.get(it).asMediaItem.mediaId)
                        val isDownloaded = isDownloadedSong(songs.get(it).asMediaItem.mediaId)
                        var forceRecompose by remember { mutableStateOf(false) }
                        SwipeablePlaylistItem(
                            mediaItem = songs.get(it).asMediaItem,
                            onPlayNext = {
                                binder?.player?.addNext(songs.get(it).asMediaItem)
                            },
                            onEnqueue = {
                                binder?.player?.enqueue(songs.get(it).asMediaItem)
                            }
                        ) {
                            SongItem(
                                song = songs.get(it).asMediaItem,
                                onDownloadClick = {
                                    binder?.cache?.removeResource(songs.get(it).asMediaItem.mediaId)
                                    Database.asyncTransaction {
                                        formatTable.deleteBySongId( songs[it].id )
                                    }
                                    manageDownload(
                                        context = context,
                                        mediaItem = songs.get(it).asMediaItem,
                                        downloadState = isDownloaded
                                    )
                                },
                                downloadState = currentDownloadState,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailSizePx = thumbnailSize,
                                onThumbnailContent = {
                                    BasicText(
                                        text = "${it + 1}",
                                        style = typography().s.semiBold.center.color(colorPalette().text),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(thumbnailSizeDp)
                                            .align(Alignment.Center)
                                    )
                                },
                                modifier = Modifier
                                    .background(colorPalette().background0)
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    navController = navController,
                                                    mediaItem = songs.get(it).asMediaItem,
                                                    onDismiss = {
                                                        menuState.hide()
                                                        forceRecompose = true
                                                    },
                                                    disableScrollingText = disableScrollingText
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.stopRadio()
                                            PlaybackContextStore.set(
                                                context.getString(R.string.playing_from_statistics),
                                                statisticsPeriodText
                                            )
                                            binder?.player?.forcePlayAtIndex(
                                                songs.map(Song::asMediaItem),
                                                it
                                            )
                                        }
                                    )
                                    .fillMaxWidth(),
                                disableScrollingText = disableScrollingText,
                                isNowPlaying = binder?.player?.isNowPlaying(songs.get(it).id) ?: false,
                                forceRecompose = forceRecompose
                            )
                        }
                    }
                }

                if (statisticsCategory == StatisticsCategory.Artists)
                    items(
                        count = artists.count()
                    ) {

                        if (artists[it].thumbnailUrl.toString() == "null")
                            UpdateYoutubeArtist(artists[it].id)

                        ArtistItem(
                            thumbnailUrl = artists[it].thumbnailUrl,
                            name = "${it+1}. ${artists[it].name}",
                            showName = true,
                            subscribersCount = null,
                            thumbnailSizePx = artistThumbnailSizePx,
                            thumbnailSizeDp = artistThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .clickable(onClick = {
                                    if (artists[it].id != "") {
                                        navController.navigate("${NavRoutes.artist.name}/${artists[it].id}")
                                    }
                                }),
                            disableScrollingText = disableScrollingText
                        )
                    }

                if (statisticsCategory == StatisticsCategory.Albums)
                    items(
                        count = albums.count()
                    ) {

                        if (albums[it].thumbnailUrl.toString() == "null")
                            UpdateYoutubeAlbum(albums[it].id)

                        AlbumItem(
                            thumbnailUrl = albums[it].thumbnailUrl,
                            title = "${it+1}. ${albums[it].title}",
                            authors = albums[it].authorsText,
                            year = albums[it].year,
                            thumbnailSizePx = albumThumbnailSizePx,
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .clickable(onClick = {
                                    if (albums[it].id != "")
                                        navController.navigate("${NavRoutes.album.name}/${albums[it].id}")
                                }),
                            disableScrollingText = disableScrollingText
                        )
                    }

                if (statisticsCategory == StatisticsCategory.Playlists) {
                    items(
                        count = playlists.count()
                    ) {
                        val thumbnails by remember {
                            Database.songPlaylistMapTable
                                    .sortSongsByPlayTime( playlists[it].playlist.id )
                                    .distinctUntilChanged()
                                    .map { list ->
                                        list.takeLast( 4 ).map { song ->
                                            song.thumbnailUrl.thumbnail( playlistThumbnailSizePx / 2 )
                                        }
                                    }
                        }.collectAsState( emptyList(), Dispatchers.IO )

                        PlaylistItem(
                            thumbnailContent = {
                                if (thumbnails.toSet().size == 1) {
                                    ImageCacheFactory.AsyncImage(
                                        thumbnailUrl = thumbnails.first(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        onError = {error ->
                                            Timber.e("Failed AsyncImage in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                                        }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        listOf(
                                            Alignment.TopStart,
                                            Alignment.TopEnd,
                                            Alignment.BottomStart,
                                            Alignment.BottomEnd
                                        ).forEachIndexed { index, alignment ->
                                            val thumbnail = thumbnails.getOrNull(index)
                                            if (thumbnail != null)
                                                ImageCacheFactory.AsyncImage(
                                                    thumbnailUrl = thumbnail,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .align(alignment)
                                                        .size(playlistThumbnailSizeDp /2),
                                                    onError = {error ->
                                                        Timber.e("Failed AsyncImage 1 in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                                                    }
                                                )
                                        }
                                    }
                                }
                            },
                            songCount = playlists[it].songCount,
                            name = "${it+1}. ${playlists[it].playlist.name}",
                            channelName = null,
                            thumbnailSizeDp = playlistThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .clickable(onClick = {
                                    val playlistId: String = playlists[it].playlist.id.toString()
                                    if ( playlistId.isEmpty() ) return@clickable    // Fail-safe??

                                    val pBrowseId: String = cleanPrefix(playlists[it].playlist.browseId ?: "")
                                    val route: String =
                                        if ( pBrowseId.isNotEmpty() )
                                            "${NavRoutes.playlist.name}/$pBrowseId"
                                        else
                                            "${NavRoutes.localPlaylist.name}/$playlistId"

                                    navController.navigate(route = route)
                                }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }


            }

            Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))

        }
}

private data class ListeningWheelEntry(
    val playTimeMs: Long,
    val artworkUrl: String?
)

private data class ListeningWheelSlice(
    val playTimeMs: Long,
    val image: ImageBitmap?
)

@Composable
private fun ListeningTimeHero(
    entries: List<ListeningWheelEntry>,
    totalTimeText: String,
    periodText: String
) {
    val topEntries = remember(entries) { entries.take(6) }
    val slices by produceState(
        initialValue = topEntries.map { ListeningWheelSlice(it.playTimeMs, null) },
        key1 = topEntries
    ) {
        value = withContext(Dispatchers.IO) {
            topEntries.map { entry ->
                ListeningWheelSlice(
                    playTimeMs = entry.playTimeMs.coerceAtLeast(1L),
                    image = ImageCacheFactory.loadBitmap(
                        entry.artworkUrl,
                        allowHardware = false
                    )?.asImageBitmap()
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        val wheelSize = if (maxWidth < 360.dp) 132.dp else 164.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListeningArtworkWheel(
                slices = slices,
                modifier = Modifier.size(wheelSize)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 22.dp)
            ) {
                BasicText(
                    text = "Total time listened",
                    style = typography().s.copy(color = colorPalette().textSecondary)
                )
                BasicText(
                    text = totalTimeText,
                    style = typography().xxl.semiBold.copy(color = colorPalette().accent),
                    maxLines = 1
                )
                BasicText(
                    text = periodText,
                    modifier = Modifier.padding(top = 5.dp),
                    style = typography().xxs.copy(color = colorPalette().textSecondary)
                )
            }
        }
    }
}

@Composable
private fun ListeningArtworkWheel(
    slices: List<ListeningWheelSlice>,
    modifier: Modifier = Modifier
) {
    val palette = colorPalette()
    val fallbackColors = listOf(
        palette.accent,
        palette.text.copy(alpha = 0.72f),
        palette.background4,
        palette.accent.copy(alpha = 0.58f),
        palette.textSecondary,
        palette.background2
    )

    Canvas(modifier = modifier.clip(androidx.compose.foundation.shape.CircleShape)) {
        val diameter = min(size.width, size.height)
        val destinationSize = IntSize(diameter.roundToInt(), diameter.roundToInt())
        val total = slices.sumOf { it.playTimeMs }.coerceAtLeast(1L).toFloat()

        fun drawSliceContent(slice: ListeningWheelSlice, index: Int) {
            val image = slice.image
            if (image == null) {
                drawRect(fallbackColors[index % fallbackColors.size])
                return
            }

            val sourceEdge = min(image.width, image.height)
            drawImage(
                image = image,
                srcOffset = IntOffset(
                    x = (image.width - sourceEdge) / 2,
                    y = (image.height - sourceEdge) / 2
                ),
                srcSize = IntSize(sourceEdge, sourceEdge),
                dstOffset = IntOffset.Zero,
                dstSize = destinationSize,
                filterQuality = FilterQuality.High
            )
        }

        if (slices.isEmpty()) {
            drawCircle(palette.accent.copy(alpha = 0.18f))
        } else if (slices.size == 1) {
            drawSliceContent(slices.first(), 0)
        } else {
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = if (index == slices.lastIndex) {
                    270f - startAngle
                } else {
                    360f * (slice.playTimeMs / total)
                }
                val path = Path().apply {
                    moveTo(diameter / 2f, diameter / 2f)
                    arcTo(
                        rect = Rect(0f, 0f, diameter, diameter),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweep,
                        forceMoveTo = false
                    )
                    close()
                }
                clipPath(path) {
                    drawSliceContent(slice, index)
                }
                drawPath(
                    path = path,
                    color = palette.background0.copy(alpha = 0.62f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                startAngle += sweep
            }
        }

        drawCircle(
            color = palette.text.copy(alpha = 0.14f),
            radius = diameter / 2f - 1.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

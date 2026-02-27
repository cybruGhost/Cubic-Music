package app.kreate.android.themed.rimusic.screen.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.ColorPaletteName
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.enums.QueueLoopType
import app.it.fast4x.rimusic.enums.SongsNumber
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.AddToPlaylistPlayerMenu
import app.it.fast4x.rimusic.ui.components.themed.DownloadStateIconButton
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.PlayerMenu
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.actionspacedevenlyKey
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.blackgradientKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.colorPaletteNameKey
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.expandedplayertoggleKey
import app.it.fast4x.rimusic.utils.getDownloadState
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.mediaItems
import app.it.fast4x.rimusic.utils.miniQueueExpandedKey
import app.it.fast4x.rimusic.utils.playAtIndex
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerTypeKey
import app.it.fast4x.rimusic.utils.playlistindicatorKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showButtonPlayerAddToPlaylistKey
import app.it.fast4x.rimusic.utils.showButtonPlayerArrowKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDiscoverKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDownloadKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLoopKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLyricsKey
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import app.it.fast4x.rimusic.utils.showButtonPlayerShuffleKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSleepTimerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerStartRadioKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSystemEqualizerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.showNextSongsInPlayerKey
import app.it.fast4x.rimusic.utils.showPlaybackSpeedButtonKey
import app.it.fast4x.rimusic.utils.showalbumcoverKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.showsongsKey
import app.it.fast4x.rimusic.utils.showthumbnailKey
import app.it.fast4x.rimusic.utils.shuffleQueue
import app.it.fast4x.rimusic.utils.swipeUpQueueKey
import app.it.fast4x.rimusic.utils.tapqueueKey
import app.it.fast4x.rimusic.utils.textoutlineKey
import app.it.fast4x.rimusic.utils.transparentBackgroundPlayerActionBarKey
import app.it.fast4x.rimusic.utils.visualizerEnabledKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.kreate.android.me.knighthat.component.player.PlaybackSpeed
import app.kreate.android.me.knighthat.utils.Toaster

private class PagerViewPort(
    private val showSongsState: MutableState<SongsNumber>,
    private val pagerState: PagerState,
): PageSize {

    override fun Density.calculateMainAxisPageSize( availableSpace: Int, pageSpacing: Int ): Int {
        val canShow = minOf( showSongsState.value.toInt() , pagerState.pageCount )
        return if( canShow > 1 )
            (availableSpace - 2 * pageSpacing) / canShow
        else
            availableSpace
    }
}

@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun BoxScope.ActionBar(
    navController: NavController,
    showQueueState: MutableState<Boolean>,
    showSearchEntityState: MutableState<Boolean>,
    rotateState: MutableState<Boolean>,
    showVisualizerState: MutableState<Boolean>,
    showSleepTimerState: MutableState<Boolean>,
    showLyricsState: MutableState<Boolean>,
    discoverState: MutableState<Boolean>,
    queueLoopState: MutableState<QueueLoopType>,
    expandPlayerState: MutableState<Boolean>,
    onDismiss: () -> Unit
) {
    // Essentials
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return
    val menuState = LocalMenuState.current

    val mediaItem = binder.player.currentMediaItem ?: return

    val playerBackgroundColors by rememberPreference( playerBackgroundColorsKey, PlayerBackgroundColors.BlurredCoverColor )
    val blackGradient by rememberPreference( blackgradientKey, false )
    val showLyricsThumbnail by rememberPreference(showlyricsthumbnailKey, false)
    val showNextSongsInPlayer by rememberPreference( showNextSongsInPlayerKey, false )
    val miniQueueExpanded by rememberPreference( miniQueueExpandedKey, true )
    val tapQueue by rememberPreference( tapqueueKey, true )
    val transparentBackgroundActionBarPlayer by rememberPreference( transparentBackgroundPlayerActionBarKey, false )
    val swipeUpQueue by rememberPreference( swipeUpQueueKey, true )
    val disableScrollingText by rememberPreference( disableScrollingTextKey, false )

    var showQueue by showQueueState
    var isShowingVisualizer by showVisualizerState
    var isShowingLyrics by showLyricsState

    Row(
        modifier = Modifier.padding( if( isLandscape ) WindowInsets.navigationBars.asPaddingValues() else PaddingValues() )
                           .align(if (isLandscape) Alignment.BottomEnd else Alignment.BottomCenter)
                           .requiredHeight(if (showNextSongsInPlayer && (showLyricsThumbnail || (!isShowingLyrics || miniQueueExpanded))) 90.dp else 50.dp)
                           .fillMaxWidth(if (isLandscape) 0.8f else 1f)
                           .clickable( enabled = tapQueue ) {
                               showQueue = true
                           }
                           .background(
                               color = colorPalette().background2.copy(
                                   alpha =
                                       if (transparentBackgroundActionBarPlayer
                                           || (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient
                                                   || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient)
                                               )
                                           && blackGradient)
                                           0.0f
                                       else
                                           0.7f // 0.0 > 0.1
                               ),
                               shape = if (isLandscape) CircleShape else RoundedCornerShape(0.dp)
                           )
                           .clip(if (isLandscape) CircleShape else RoundedCornerShape(0.dp))
                           .pointerInput(Unit) {
                               if (swipeUpQueue)
                                   detectVerticalDragGestures(
                                       onVerticalDrag = { _, dragAmount ->
                                           if (dragAmount < 0) showQueue = true
                                       }
                                   )
                           },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            if ( showNextSongsInPlayer && (showLyricsThumbnail || !isShowingLyrics || miniQueueExpanded) ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .background(
                            colorPalette().background2.copy(
                                alpha = if (transparentBackgroundActionBarPlayer) 0.0f else 0.3f
                            )
                        )
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                ) {
                    var currentIndex by remember { mutableIntStateOf( binder.player.currentMediaItemIndex ) }
                    var nextIndex by remember { mutableIntStateOf( binder.player.nextMediaItemIndex ) }
                    val mediaItems = remember { mutableStateListOf<MediaItem>() }

                    val pagerStateQueue = rememberPagerState( pageCount = { mediaItems.size } )

                    binder.player.DisposableListener {
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                currentIndex = binder.player.currentMediaItemIndex
                                nextIndex = binder.player.nextMediaItemIndex
                            }

                            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                                mediaItems.clear()
                                for (i in 0 until timeline.windowCount) {
                                    mediaItems.add(timeline.getWindow(i, Timeline.Window()).mediaItem)
                                }
                                currentIndex = binder.player.currentMediaItemIndex
                                nextIndex = binder.player.nextMediaItemIndex
                            }
                        }
                    }

                   // Instant update and snap when the queue itself changes
                    LaunchedEffect( binder.player.mediaItems ) {
                        mediaItems.clear()
                        mediaItems.addAll( binder.player.mediaItems )

                       val targetPage = nextIndex.coerceIn( 0, pagerStateQueue.pageCount.coerceAtLeast(1) - 1 )
                        pagerStateQueue.requestScrollToPage(targetPage)
                    }

                    // Smooth slide when only the track skips
                    LaunchedEffect( nextIndex ) {
                        if (pagerStateQueue.pageCount > 0) {
                            val targetPage = nextIndex.coerceIn(0, pagerStateQueue.pageCount - 1)
                            pagerStateQueue.animateScrollToPage(targetPage)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(vertical = 7.5.dp)
                            .weight(0.07f)
                            .conditional( pagerStateQueue.currentPage == currentIndex ) {
                                padding(
                                    horizontal = 3.dp
                                )
                            }
                    ) {
                        val coroutine = rememberCoroutineScope()

                        Icon(
                            painter = painterResource(
                                id = if ( pagerStateQueue.currentPage > currentIndex ) R.drawable.chevron_forward
                                else if ( pagerStateQueue.currentPage == currentIndex ) R.drawable.play
                                else R.drawable.chevron_back
                            ),
                            contentDescription = null,
                            modifier = Modifier.size( 25.dp )
                                               .clickable(
                                                   interactionSource = remember { MutableInteractionSource() },
                                                   indication = null,
                                               ) {
                                                   coroutine.launch {
                                                       pagerStateQueue.animateScrollToPage( currentIndex )
                                                   }
                                               },
                            tint = colorPalette().accent
                        )
                    }

                    val showSongsState = rememberPreference( showsongsKey, SongsNumber.`2` )
                    val viewPort = remember {
                        PagerViewPort( showSongsState, pagerStateQueue )
                    }

                    HorizontalPager(
                        state = pagerStateQueue,
                        pageSize = viewPort,
                        pageSpacing = 10.dp,
                        modifier = Modifier.weight(1f)
                    ) { index ->
                        val mediaItemAtIndex by remember { derivedStateOf { mediaItems[index] } }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        binder.player.playAtIndex(index)
                                    },
                                    onLongClick = {
                                        if ( index < mediaItems.size ) {
                                            binder.player.addNext( mediaItemAtIndex )
                                            Toaster.s( R.string.addednext )
                                        }
                                    }
                                )
                        ) {
                            val showAlbumCover by rememberPreference( showalbumcoverKey, true )
                            if ( showAlbumCover )
                                Box( Modifier.align(Alignment.CenterVertically) ) {
                                    ImageCacheFactory.Thumbnail(
                                        thumbnailUrl = mediaItemAtIndex.mediaMetadata
                                                                       .artworkUri
                                                                       .toString(),
                                        contentDescription = "song_pos_$index",
                                        modifier = Modifier
                                            .padding(end = 5.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .size(30.dp)
                                    )
                                }

                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth()
                            ) {
                                val colorPaletteMode by rememberPreference( colorPaletteModeKey, ColorPaletteMode.Dark )
                                val textOutline by rememberPreference( textoutlineKey, false )

                                //<editor-fold defaultstate="collapsed" desc="Title">
                                Box {
                                    val titleText by remember {
                                        derivedStateOf {
                                            cleanPrefix( mediaItemAtIndex.mediaMetadata.title.toString() )
                                        }
                                    }

                                    BasicText(
                                        text = titleText,
                                        style = TextStyle(
                                            color = colorPalette().text,
                                            fontSize = typography().xxxs.semiBold.fontSize,
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                    )
                                    BasicText(
                                        text = titleText,
                                        style = TextStyle(
                                            drawStyle = Stroke(
                                                width = 0.25f,
                                                join = StrokeJoin.Round
                                            ),
                                            color = if (!textOutline) Color.Transparent
                                            else if (colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))) Color.White.copy(
                                                0.65f
                                            )
                                            else Color.Black,
                                            fontSize = typography().xxxs.semiBold.fontSize,
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                    )
                                }
                                //</editor-fold>
                                //<editor-fold defaultstate="collapsed" desc="Artists">
                                Box {
                                    val artistsText by remember {
                                        derivedStateOf {
                                            cleanPrefix( mediaItemAtIndex.mediaMetadata.artist.toString() )
                                        }
                                    }

                                    BasicText(
                                        text = artistsText,
                                        style = TextStyle(
                                            color = colorPalette().text,
                                            fontSize = typography().xxxs.semiBold.fontSize,
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.conditional(!disableScrollingText) { basicMarquee() }
                                    )
                                    BasicText(
                                        text = artistsText,
                                        style = TextStyle(
                                            drawStyle = Stroke(
                                                width = 0.25f,
                                                join = StrokeJoin.Round
                                            ),
                                            color =
                                                if ( !textOutline )
                                                    Color.Transparent
                                                else if (
                                                    colorPaletteMode == ColorPaletteMode.Light
                                                    || (colorPaletteMode == ColorPaletteMode.System && !isSystemInDarkTheme())
                                                )
                                                    Color.White.copy( 0.65f )
                                                else
                                                    Color.Black,
                                            fontSize = typography().xxxs.semiBold.fontSize,
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.conditional( !disableScrollingText ) { basicMarquee() }
                                    )
                                }
                                //</editor-fold>
                            }
                        }
                    }

                    if ( showSongsState.value == SongsNumber.`1` )
                        IconButton(
                            icon = R.drawable.trash,
                            color = Color.White,
                            enabled = true,
                            onClick = {
                                binder.player.removeMediaItem( nextIndex )
                            },
                            modifier = Modifier
                                .weight(.07f)
                                .size(40.dp)
                                .padding(vertical = 7.5.dp)
                        )
                }
            }

            val actionsSpaceEvenly by rememberPreference( actionspacedevenlyKey, false )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (actionsSpaceEvenly) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
            ) {
                val showButtonPlayerVideo by rememberPreference( showButtonPlayerVideoKey, false )
                if (showButtonPlayerVideo)
                    IconButton(
                        icon = R.drawable.video,
                        color = colorPalette().accent,
                        onClick = {
                            binder.gracefulPause()
                            showSearchEntityState.value = true
                        },
                        modifier = Modifier.size( 24.dp )
                    )

                val showButtonPlayerDiscover by rememberPreference( showButtonPlayerDiscoverKey, false )
                if (showButtonPlayerDiscover) {
                    var discoverIsEnabled by discoverState

                    IconButton(
                        icon = R.drawable.star_brilliant,
                        color = if (discoverIsEnabled) colorPalette().text else colorPalette().textDisabled,
                        onClick = {},
                        modifier = Modifier
                            .size(24.dp)
                            .combinedClickable(
                                onClick = { discoverIsEnabled = !discoverIsEnabled },
                                onLongClick = {
                                    Toaster.i(R.string.discoverinfo)
                                }
                            )
                    )
                }

                val showButtonPlayerDownload by rememberPreference( showButtonPlayerDownloadKey, true )
                if (showButtonPlayerDownload) {
                    val isDownloaded = isDownloadedSong( mediaItem.mediaId )

                    DownloadStateIconButton(
                        icon = if (isDownloaded) R.drawable.downloaded else R.drawable.download,
                        color = if (isDownloaded) colorPalette().accent else Color.Gray,
                        downloadState = getDownloadState(mediaItem.mediaId),
                        mediaId = mediaItem.mediaId,
                        onClick = {
                            manageDownload(
                                context = context,
                                mediaItem = mediaItem,
                                downloadState = isDownloaded
                            )
                        },
                        onCancelButtonClicked = {
                            manageDownload(
                                context = context,
                                mediaItem = mediaItem,
                                downloadState = true
                            )
                        },
                        modifier = Modifier.size( 24.dp )
                    )
                }

                val showButtonPlayerAddToPlaylist by rememberPreference( showButtonPlayerAddToPlaylistKey, true )
                if (showButtonPlayerAddToPlaylist) {
                    val showPlaylistIndicator by rememberPreference( playlistindicatorKey, false )
                    val colorPaletteName by rememberPreference( colorPaletteNameKey, ColorPaletteName.Dynamic )
                    val color = colorPalette()
                    val isSongMappedToPlaylist by remember( mediaItem.mediaId ) {
                        Database.songPlaylistMapTable.isMapped( mediaItem.mediaId )
                    }.collectAsState( false, Dispatchers.IO )

                    IconButton(
                        icon = R.drawable.add_in_playlist,
                        color = if (isSongMappedToPlaylist && showPlaylistIndicator) Color.White else color.accent,
                        onClick = {
                            menuState.display {
                                AddToPlaylistPlayerMenu(
                                    navController = navController,
                                    onDismiss = menuState::hide,
                                    mediaItem = mediaItem,
                                    binder = binder,
                                    onClosePlayer = onDismiss,
                                )
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .conditional(isSongMappedToPlaylist && showPlaylistIndicator) {
                                background(color.accent, CircleShape).padding(all = 5.dp)
                            }
                    )
                }

                val showButtonPlayerLoop by rememberPreference( showButtonPlayerLoopKey, true )
                if (showButtonPlayerLoop) {
                    var queueLoopType by queueLoopState
                    val effectRotationEnabled by rememberPreference( effectRotationKey, true )

                    IconButton(
                        icon = queueLoopType.iconId,
                        color = colorPalette().accent,
                        onClick = {
                            queueLoopType = queueLoopType.next()
                            if (effectRotationEnabled)
                                rotateState.value = !rotateState.value
                        },
                        modifier = Modifier.size( 24.dp )
                    )
                }

                val showButtonPlayerShuffle by rememberPreference( showButtonPlayerShuffleKey, true )
                if (showButtonPlayerShuffle)
                    IconButton(
                        icon = R.drawable.shuffle,
                        color = colorPalette().accent,
                        onClick = binder.player::shuffleQueue,
                        modifier = Modifier.size( 24.dp )
                    )

                val showButtonPlayerLyrics by rememberPreference( showButtonPlayerLyricsKey, true )
                if (showButtonPlayerLyrics)
                    IconButton(
                        icon = R.drawable.song_lyrics,
                        color = if ( isShowingLyrics ) colorPalette().accent else Color.Gray,
                        enabled = true,
                        onClick = {
                            if( isShowingVisualizer )
                                isShowingVisualizer = !isShowingVisualizer
                            isShowingLyrics = !isShowingLyrics
                        },
                        modifier = Modifier.size( 24.dp )
                    )

                val playerType by rememberPreference( playerTypeKey, PlayerType.Essential )
                val showThumbnail by rememberPreference( showthumbnailKey, true )
                if (!isLandscape || ((playerType == PlayerType.Essential) && !showThumbnail)) {
                    val expandedPlayerToggle by rememberPreference( expandedplayertoggleKey, true )
                    var expandedPlayer by expandPlayerState

                    if (expandedPlayerToggle && !showLyricsThumbnail)
                        IconButton(
                            icon = R.drawable.maximize,
                            color = if ( expandedPlayer ) colorPalette().accent else Color.Gray,
                            onClick = {
                                expandedPlayer = !expandedPlayer
                            },
                            modifier = Modifier.size( 20.dp )
                        )
                }

                val visualizerEnabled by rememberPreference( visualizerEnabledKey, false )
                if (visualizerEnabled)
                    IconButton(
                        icon = R.drawable.sound_effect,
                        color = if ( isShowingVisualizer ) colorPalette().accent else Color.Gray,
                        onClick = {
                            if (isShowingLyrics)
                                isShowingLyrics = !isShowingLyrics
                            isShowingVisualizer = !isShowingVisualizer
                        },
                        modifier = Modifier.size( 24.dp )
                    )


                val showButtonPlayerSleepTimer by rememberPreference( showButtonPlayerSleepTimerKey, false )
                if (showButtonPlayerSleepTimer) {
                    val sleepTimerMillisLeft: Long? by
                        (binder.sleepTimerMillisLeft ?: flowOf(null)).collectAsState( null )

                    IconButton(
                        icon = R.drawable.sleep,
                        color = if (sleepTimerMillisLeft != null) colorPalette().accent else Color.Gray,
                        onClick = {
                            showSleepTimerState.value = true
                        },
                        modifier = Modifier.size( 24.dp )
                    )
                }

                val showButtonPlayerSystemEqualizer by rememberPreference( showButtonPlayerSystemEqualizerKey, false )
                if (showButtonPlayerSystemEqualizer) {
                    val activityResultLauncher =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

                    IconButton(
                        icon = R.drawable.equalizer,
                        color = colorPalette().accent,
                        onClick = {
                            try {
                                activityResultLauncher.launch(
                                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                        putExtra(
                                            AudioEffect.EXTRA_AUDIO_SESSION,
                                            binder.player.audioSessionId
                                        )
                                        putExtra(
                                            AudioEffect.EXTRA_PACKAGE_NAME,
                                            context.packageName
                                        )
                                        putExtra(
                                            AudioEffect.EXTRA_CONTENT_TYPE,
                                            AudioEffect.CONTENT_TYPE_MUSIC
                                        )
                                    }
                                )
                            } catch (e: ActivityNotFoundException) {
                                Toaster.e( R.string.info_not_find_application_audio )
                            }
                        },
                        modifier = Modifier.size( 20.dp )
                    )
                }

                val showButtonPlayerStartRadio by rememberPreference( showButtonPlayerStartRadioKey, false )
                if (showButtonPlayerStartRadio)
                    IconButton(
                        icon = R.drawable.radio,
                        color = colorPalette().accent,
                        onClick = {
                            binder.startRadio( mediaItem )
                        },
                        modifier = Modifier.size( 24.dp )
                    )

                val showPlaybackSpeedButton by rememberPreference( showPlaybackSpeedButtonKey, false )
                if( showPlaybackSpeedButton ) {
                    val playbackSpeed = remember { PlaybackSpeed() }

                    playbackSpeed.Render()
                    playbackSpeed.ToolBarButton()
                }

                val showButtonPlayerArrow by rememberPreference( showButtonPlayerArrowKey, true )
                if (showButtonPlayerArrow)
                    IconButton(
                        icon = R.drawable.chevron_up,
                        color = colorPalette().accent,
                        enabled = true,
                        onClick = {
                            showQueue = true
                        },
                        modifier = Modifier
                            //.padding(end = 12.dp)
                            .size(24.dp),
                    )

                val showButtonPlayerMenu by rememberPreference( showButtonPlayerMenuKey, false )
                if( showButtonPlayerMenu || isLandscape ) {
                    val isInLandscape = isLandscape

                    IconButton(
                        icon = R.drawable.ellipsis_vertical,
                        color = colorPalette().accent,
                        onClick = {
                            val currentMediaItem = binder.player.currentMediaItem
                            if (currentMediaItem != null) {
                                menuState.display {
                                    PlayerMenu(
                                        navController = navController,
                                        onDismiss = menuState::hide,
                                        mediaItem = currentMediaItem,
                                        binder = binder,
                                        onClosePlayer = onDismiss,
                                        onShowSleepTimer = {
                                            showSleepTimerState.value = true
                                            menuState.hide()
                                        },
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = if (isInLandscape) 90f else 0f
                            }
                    )
                }
            }
        }
    }
}
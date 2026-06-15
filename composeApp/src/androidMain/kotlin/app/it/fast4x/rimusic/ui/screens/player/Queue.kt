package app.it.fast4x.rimusic.ui.screens.player


import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.navigation.NavController
import app.kreate.android.R
import com.valentinilk.shimmer.shimmer
import app.it.fast4x.compose.persist.persist
import app.it.fast4x.compose.persist.persistList
import app.it.fast4x.compose.reordering.draggedItem
import app.it.fast4x.compose.reordering.rememberReorderingState
import app.it.fast4x.compose.reordering.reorder
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.QueueLoopType
import app.it.fast4x.rimusic.enums.QueueType
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.SwipeableQueueItem
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Dialog
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.items.SongItemPlaceholder
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.DiscoverQueueTrigger
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.discoverKey
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.findMediaItemIndexById
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.isNowPlaying
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.mediaItems
import app.it.fast4x.rimusic.utils.offlineQueueNetworkRefillKey
import app.it.fast4x.rimusic.utils.queueTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.shouldBePlaying
import app.kreate.android.me.knighthat.component.SongItem
import app.kreate.android.me.knighthat.component.tab.ExportSongsToCSVDialog
import app.kreate.android.me.knighthat.component.tab.ItemSelector
import app.kreate.android.me.knighthat.component.tab.Locator
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.component.ui.screens.player.DeleteFromQueue
import app.kreate.android.me.knighthat.component.ui.screens.player.OfflineQueueNetworkRefill
import app.kreate.android.me.knighthat.component.ui.screens.player.QueueArrow
import app.kreate.android.me.knighthat.component.ui.screens.player.Repeat
import app.kreate.android.me.knighthat.component.ui.screens.player.ShuffleQueue
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import app.kreate.android.themed.rimusic.component.playlist.PositionLock
import kotlin.math.roundToInt


@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@androidx.media3.common.util.UnstableApi
@Composable
fun Queue(
    navController: NavController,
    onDismiss: (QueueLoopType) -> Unit,
    onDiscoverClick: (Boolean) -> Unit,
) {
    // Essentials
    val context = LocalContext.current
    val windowInsets = WindowInsets.systemBars
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val rippleIndication = ripple(bounded = false)

    Box( Modifier.fillMaxSize() ) {
       var items by remember(player.currentTimeline) {
            mutableStateOf(player.currentTimeline.mediaItems.map( MediaItem::asSong ))
        }
        var currentMediaId by remember { mutableStateOf(player.currentMediaItem?.mediaId.orEmpty()) }
        player.DisposableListener {
            object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    items = player.currentTimeline.mediaItems.map( MediaItem::asSong )
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaId = mediaItem?.mediaId.orEmpty()
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (
                        events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                        events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                        events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                    ) {
                        currentMediaId = player.currentMediaItem?.mediaId.orEmpty()
                        items = player.currentTimeline.mediaItems.map(MediaItem::asSong)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    currentMediaId = player.currentMediaItem?.mediaId.orEmpty()
                }
            }
        }
        var itemsOnDisplay by persistList<Song>( "queue/on_display" )
        val nowPlayingSong by remember(items, currentMediaId) {
            derivedStateOf {
                val normalizedCurrent = currentMediaId.normalizedQueueSongId()
                items.firstOrNull { it.id.normalizedQueueSongId() == normalizedCurrent }
            }
        }

        val lazyListState = rememberLazyListState()
        val reorderingState = rememberReorderingState(
            lazyListState = lazyListState,
            key = items,
            onDragEnd = player::moveMediaItem,
            extraItemCount = 0
        )

        val positionLock = remember { PositionLock() }

        val itemSelector = ItemSelector<Song>()
        LaunchedEffect( itemSelector.isActive ) {
            // Setting this field to true means disable it
            if( itemSelector.isActive )
                positionLock.isFirstIcon = true
        }

        fun getSongs() = itemSelector.ifEmpty { items }

        val search = Search(lazyListState)
        LaunchedEffect( items, currentMediaId, search.inputValue ) {
            items.filter {
                    // Without cleaning, user can search explicit songs with "e:"
                    // I kinda want this to be a feature, but it seems unnecessary
                    val containsTitle = it.cleanTitle().contains( search.inputValue, true )
                    val containsArtist = it.cleanArtistsText().contains( search.inputValue, true )

                    containsTitle || containsArtist
                }
                .filterNot { song ->
                    nowPlayingSong?.id == song.id
                }
                .let { itemsOnDisplay = it }
        }

        val plistName = remember { mutableStateOf("") }
        val exportDialog = ExportSongsToCSVDialog(
            playlistName = plistName.value,
            songs = ::getSongs
        )
        val isDownloadedQueue = items.isNotEmpty() && items.all { it.isLocal || isDownloadedSong(it.id) }
        val currentSongIsOffline = nowPlayingSong?.let { it.isLocal || isDownloadedSong(it.id) } == true ||
            currentMediaId.isNotBlank() && isDownloadedSong(currentMediaId)
        var offlineQueueNetworkRefillEnabled by rememberPreference( offlineQueueNetworkRefillKey, false )
        val showNetworkRefill = isDownloadedQueue || currentSongIsOffline
        var showNetworkRefillInfo by rememberSaveable { mutableStateOf(false) }
        var refillOffsetX by rememberSaveable { mutableStateOf(0f) }
        var refillOffsetY by rememberSaveable { mutableStateOf(0f) }
        val shuffle = ShuffleQueue( player, reorderingState )
        var discoverIsEnabled by rememberPreference(discoverKey, false)
        var discoverOffsetX by rememberSaveable { mutableStateOf(0f) }
        var discoverOffsetY by rememberSaveable { mutableStateOf(0f) }
        val repeat = Repeat.init()
        val deleteDialog = DeleteFromQueue {
            if( itemSelector.isEmpty() ) {
                player.stop()
                player.clearMediaItems()
            } else
                itemSelector.map( items::indexOf )
                            .filter { it >= 0 && it < player.mediaItemCount }
                            .sorted()
                            // Goes backward to prevent item from being skipped
                            // due to the previous element is removed and the indices
                            // are updated.
                            .reversed()
                            .forEach( player::removeMediaItem )

            itemSelector.isActive = false

            onDismiss()
        }
        val addToPlaylist = PlaylistsMenu.init(
            navController = navController,
            mediaItems = { getSongs().map( Song::asMediaItem ) },
            onFailure = { throwable, preview ->
                Timber.e( "Failed to add songs to playlist ${preview.playlist.name} on HomeSongs" )
                throwable.printStackTrace()
            },
            finalAction = {
                // Turn of selector clears the selected list
                itemSelector.isActive = false
            }
        )
        val queueArrow = QueueArrow { onDismiss( repeat.type ) }
        val locator = Locator( lazyListState, ::getSongs )

        fun queueIndexOf(songId: String): Int {
            val directIndex = player.findMediaItemIndexById(songId)
            if (directIndex >= 0) return directIndex
            val normalized = songId.normalizedQueueSongId()
            return (0 until player.mediaItemCount).firstOrNull { index ->
                player.getMediaItemAt(index).mediaId.normalizedQueueSongId() == normalized
            } ?: -1
        }

        fun playQueueSong(song: Song) {
            if (player.isNowPlaying(song.id)) {
                if (player.shouldBePlaying) player.pause() else player.play()
                return
            }
            val actualIndex = queueIndexOf(song.id)
            if (actualIndex >= 0) {
                player.seekToDefaultPosition(actualIndex)
                player.prepare()
                player.playWhenReady = true
            } else {
                Toaster.w(R.string.playing_song_not_found_on_current_list)
            }
        }

        // Dialog renders
        exportDialog.Render()
        (deleteDialog as Dialog).Render()
        if (showNetworkRefillInfo) {
            AlertDialog(
                onDismissRequest = { showNetworkRefillInfo = false },
                containerColor = colorPalette().background1,
                titleContentColor = colorPalette().text,
                textContentColor = colorPalette().textSecondary,
                title = { Text(stringResource(R.string.queue_network_refill_dialog_title)) },
                text = { Text(stringResource(R.string.queue_network_refill_dialog_text)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            offlineQueueNetworkRefillEnabled = true
                            showNetworkRefillInfo = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colorPalette().accent)
                    ) {
                        Text(stringResource(R.string.turn_on))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            offlineQueueNetworkRefillEnabled = false
                            showNetworkRefillInfo = false
                        }
                    ) {
                        Text(stringResource(R.string.turn_off))
                    }
                }
            )
        }

        Column {
            val queueType by rememberPreference( queueTypeKey, QueueType.Essential )
            val backgroundAlpha = if( queueType == QueueType.Modern ) .5f else 1f

            LazyColumn(
                state = reorderingState.lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = windowInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add( WindowInsets(bottom = Dimensions.bottomSpacer) )
                    .asPaddingValues(),
                modifier = Modifier.weight( 1f )
                                   .background(
                                       colorPalette().background0.copy( alpha = backgroundAlpha )
                                   )

            ) {
                nowPlayingSong?.let { song ->
                    item(key = "now_playing_pinned_${song.id}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                text = "Now playing",
                                style = TextStyle(
                                    color = colorPalette().textSecondary,
                                    fontStyle = typography().s.fontStyle
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            SongItem(
                                song = song,
                                itemSelector = itemSelector,
                                navController = navController,
                                trailingContent = { Box(Modifier.width(24.dp)) },
                                onClick = {
                                    playQueueSong(song)
                                    search.hideIfEmpty()
                                }
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = itemsOnDisplay,
                  key = { index, song -> "${song.id}-$index" }
                ) { index, song ->

                    val isLocal by remember { derivedStateOf { song.isLocal } }
                    val isDownloaded = isLocal || isDownloadedSong(song.id)

                    Box(
                        modifier = Modifier.fillMaxWidth()
                                           .draggedItem(
                                               reorderingState = reorderingState,
                                               index = index
                                           )
                    ) {
                        // Drag anchor
                        if ( !positionLock.isLocked() ) {
                            Box(
                                modifier = Modifier.padding( end = 16.dp ) // Accommodate horizontal padding of SongItem
                                    .size( 24.dp )
                                    .zIndex( 2f )
                                    .align( Alignment.CenterEnd ),
                                contentAlignment = Alignment.Center
                            ) {

                                IconButton(
                                    icon = R.drawable.reorder,
                                    color = colorPalette().textDisabled,
                                    indication = rippleIndication,
                                    onClick = {},
                                    modifier = Modifier.reorder(
                                        reorderingState = reorderingState,
                                        index = index
                                    )
                                )
                            }
                        }

                        val mediaItem = song.asMediaItem
                        SwipeableQueueItem(
                            mediaItem = mediaItem,
                            onPlayNext = {
                                val currentIndex = binder.player.currentMediaItemIndex
                                val actualIndex = queueIndexOf(song.id)
                                val targetIndex = (currentIndex + 1).coerceAtMost(binder.player.mediaItemCount - 1)
                                if (actualIndex != targetIndex && actualIndex in 0 until binder.player.mediaItemCount) {
                                    binder.player.moveMediaItem(actualIndex, targetIndex)
                                }
                            },
                            onDownload = {
                                binder.cache.removeResource(song.id)
                                if (!isLocal)
                                    manageDownload(
                                        context = context,
                                        mediaItem = mediaItem,
                                        downloadState = isDownloaded
                                    )
                            },
                            onRemoveFromQueue = {
                                /*
                                     Compose gotcha here, variables passed into this
                                     block will be held through recomposition.

                                     Meaning, if index at initialization is 0
                                     then 0 will stay here through recomposition.

                                     To bypass it, pass another function that requires
                                     computation to extract data.
                                */
                                val actualIndex = queueIndexOf( song.id )
                                if (actualIndex in 0 until player.mediaItemCount) {
                                    player.removeMediaItem( actualIndex )
                                    Toaster.s(
                                        "${context.resources.getString(R.string.deleted)} ${song.cleanTitle()}"
                                    )
                                }
                            },
                            onEnqueue = {
                                binder.player.enqueue(
                                    mediaItem,
                                    context
                                )
                            }
                        ) {
                            SongItem(
                                song = song,
                                itemSelector = itemSelector,
                                navController = navController,
                                trailingContent = {
                                    if( !positionLock.isLocked() )
                                    // Create a fake box to store drag anchor and checkbox
                                        Box( Modifier.width( 24.dp ) )
                                },
                                onClick = {
                                    playQueueSong(song)

                                    /*
                                        Due to the small size of checkboxes,
                                        we shouldn't disable [itemSelector]
                                     */

                                    search.hideIfEmpty()
                                }
                            )
                        }
                    }
                }

                if( binder.isLoadingRadio )
                    item {
                        Column( Modifier.shimmer() ) {
                            repeat(3) { index ->
                                SongItemPlaceholder( Modifier.alpha( 1f - index * 0.125f ) )
                            }
                        }
                    }
            }

            // Search box
            Box(
                modifier = Modifier.fillMaxWidth()
                                   .background( colorPalette().background1 ),
            ) { search.SearchBar( this@Column ) }

            Box(
                modifier = Modifier.fillMaxWidth()
                                   .clickable { onDismiss( repeat.type ) }
                                   .background (colorPalette().background1 )
                                   .height( 60.dp ) //bottom bar queue
            ) {
                if( !isLandscape ) {
                    // Move mini player up as search bar appears
                    val yOffset = if( search.isVisible ) -125 else -65

                    Box(
                        Modifier.absoluteOffset( 0.dp, yOffset.dp )
                                .align( Alignment.TopCenter )
                    ) { MiniPlayer( {}, {} ) }
                }

                if ( !queueArrow.isEnabled )
                    Image(
                        painter = painterResource( R.drawable.horizontal_bold_line_rounded ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier.absoluteOffset( 0.dp, (-10).dp )
                                           .align( Alignment.TopCenter )
                                           .size( 30.dp )
                    )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding( horizontal = 8.dp )
                                       .fillMaxWidth()
                ) {
                    /* Number of songs
                     *
                     * Opted out of using [IconInfo] because it has [Modifier#fillMaxWidth]
                     * which makes it harder to adopt flexible width.
                     */
                    Row(
                        modifier = Modifier.height( TabToolBar.TOOLBAR_ICON_SIZE )
                                           .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource( R.drawable.musical_notes ),
                            contentDescription = "Number of songs in queue",
                            tint = colorPalette().text,
                            modifier = Modifier.padding( end = 2.dp )
                        )
                        BasicText(
                            text = player.mediaItemCount.toString(),
                            style = TextStyle(
                                color = colorPalette().text,
                                fontStyle = typography().l.fontStyle
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TabToolBar.Buttons(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight( 1f ),
                        buttons = mutableListOf<Button>().apply {
                            add( locator )
                            add( search )
                            add( positionLock )
                            add( repeat )
                            add( shuffle )
                            add( itemSelector )
                            add( deleteDialog )
                            add( addToPlaylist )
                            add( exportDialog )
                        }
                    )

                    if( queueArrow.isEnabled )
                        queueArrow.ToolBarButton()
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = reorderingState.lazyListState,
            modifier = Modifier.padding(bottom = Dimensions.miniPlayerHeight)
        )

        if (showNetworkRefill) {
            QueueNetworkRefillChip(
                enabled = offlineQueueNetworkRefillEnabled,
                onToggle = {
                    offlineQueueNetworkRefillEnabled = !offlineQueueNetworkRefillEnabled
                },
                onInfo = { showNetworkRefillInfo = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.End).asPaddingValues())
                    .padding(top = 12.dp, end = 12.dp)
                    .offset { IntOffset(refillOffsetX.roundToInt(), refillOffsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            refillOffsetX = (refillOffsetX + dragAmount.x).coerceIn(-900f, 0f)
                            refillOffsetY = (refillOffsetY + dragAmount.y).coerceIn(0f, 600f)
                        }
                    }
                    .zIndex(4f)
            )
        }

        if (discoverIsEnabled) {
            QueueDiscoverChip(
                onApply = {
                    onDiscoverClick(true)
                    DiscoverQueueTrigger.request()
                },
                onTurnOff = {
                    discoverIsEnabled = false
                    onDiscoverClick(false)
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Start).asPaddingValues())
                    .padding(top = 72.dp, start = 12.dp)
                    .offset { IntOffset(discoverOffsetX.roundToInt(), discoverOffsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            discoverOffsetX = (discoverOffsetX + dragAmount.x).coerceIn(0f, 1000f)
                            discoverOffsetY = (discoverOffsetY + dragAmount.y).coerceIn(-80f, 1600f)
                        }
                    }
                    .zIndex(5f)
            )
        }
    }
}

@Composable
private fun QueueDiscoverChip(
    onApply: () -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onApply),
        color = colorPalette().background1.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB76CFF))
            )
            Column(modifier = Modifier.width(126.dp)) {
                BasicText(
                    text = stringResource(R.string.discover_queue_banner_title),
                    style = typography().xxs.semiBold.copy(color = colorPalette().text),
                    maxLines = 1
                )
                BasicText(
                    text = stringResource(R.string.discover_queue_banner_text),
                    style = typography().xxs.copy(color = colorPalette().textSecondary),
                    maxLines = 1
                )
            }
            BasicText(
                text = stringResource(R.string.turn_off),
                style = typography().xxs.semiBold.copy(color = Color.White),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onTurnOff)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun QueueNetworkRefillChip(
    enabled: Boolean,
    onToggle: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) Color(0xFF167D4A) else colorPalette().background1
    val contentColor = if (enabled) Color.White else colorPalette().text
    val dotColor = if (enabled) Color(0xFF70F2A4) else Color(0xFFFF5A5F)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onInfo),
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(modifier = Modifier.width(112.dp)) {
                BasicText(
                    text = stringResource(R.string.queue_network_refill_button),
                    style = typography().xxs.semiBold.copy(color = contentColor),
                    maxLines = 1
                )
                BasicText(
                    text = if (enabled) {
                        stringResource(R.string.queue_network_refill_on)
                    } else {
                        stringResource(R.string.queue_network_refill_off)
                    },
                    style = typography().xxs.copy(
                        color = if (enabled) Color.White.copy(alpha = 0.78f) else colorPalette().textSecondary
                    ),
                    maxLines = 1
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() }
            )
            BasicText(
                text = "?",
                style = typography().xxs.semiBold.copy(color = contentColor),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.08f))
                    .clickable(onClick = onInfo)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun String.normalizedQueueSongId(): String =
    removePrefix(EXPLICIT_PREFIX)
        .substringAfterLast("/")
        .substringBefore("?")

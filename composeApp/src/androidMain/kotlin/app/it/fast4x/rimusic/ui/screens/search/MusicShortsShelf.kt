package app.it.fast4x.rimusic.ui.screens.search

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.screens.player.Comment
import app.it.fast4x.rimusic.ui.screens.player.CommentResponse
import app.it.fast4x.rimusic.ui.screens.player.fetchCommentsPage
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.isYouTubeVideoId
import app.it.fast4x.rimusic.utils.audioQualityFormatKey
import app.it.fast4x.rimusic.utils.okHttpDataSourceFactory
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.kreate.android.me.knighthat.coil.resolveArtworkUrl
import app.kreate.android.me.knighthat.coil.thumbnail
import app.kreate.android.me.knighthat.utils.PropUtils
import app.kreate.android.service.getInnertubePlayerFormatUrl
import app.kreate.android.service.getInnertubeVideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private object ReelCommentsCache {
    private const val MAX_ENTRIES = 24
    private const val TTL_MS = 10L * 60L * 1_000L
    private const val FETCH_TIMEOUT_MS = 8_000L

    private data class Entry(
        val response: CommentResponse,
        val storedAtMs: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(MAX_ENTRIES, 0.75f, true)
    private val fetchLocks = ConcurrentHashMap<String, Mutex>()

    @Synchronized
    fun get(videoId: String): CommentResponse? {
        val entry = entries[videoId] ?: return null
        if (System.currentTimeMillis() - entry.storedAtMs > TTL_MS) {
            entries.remove(videoId)
            return null
        }
        return entry.response
    }

    @Synchronized
    fun put(videoId: String, response: CommentResponse) {
        entries[videoId] = Entry(response, System.currentTimeMillis())
        while (entries.size > MAX_ENTRIES) {
            entries.entries.iterator().run {
                if (hasNext()) {
                    next()
                    remove()
                }
            }
        }
    }

    suspend fun firstPage(videoId: String): CommentResponse? {
        get(videoId)?.let { return it }
        val mutex = fetchLocks.getOrPut(videoId) { Mutex() }
        return mutex.withLock {
            get(videoId)?.let { return@withLock it }
            val response = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                runCatching { fetchCommentsPage(videoId) }.getOrNull()
            }
            response?.let { put(videoId, it) }
            fetchLocks.remove(videoId, mutex)
            response
        }
    }
}

private const val SHORT_STREAM_TIMEOUT_MS = 20_000L
private const val MAX_SHORT_STREAM_RETRIES = 1

private fun YtmHomeSectionItem.musicShortArtworkUrl(size: Int = 1280): String? {
    val mediaId = videoId.trim().ifBlank { id.trim() }
    return resolveArtworkUrl(
        mediaId = mediaId,
        thumbnailUrl = thumbnailUrl.trim().takeIf(String::isNotBlank)
            ?: thumbnail.trim().takeIf(String::isNotBlank),
    ).thumbnail(size)
}

internal fun YtmHomeSectionItem.asMusicShortMediaItem(): MediaItem? {
    val mediaId = videoId.trim().ifBlank { id.trim() }
    if (!mediaId.isYouTubeVideoId() || title.isBlank()) return null
    return Song(
        id = mediaId,
        title = title.trim(),
        artistsText = artistsText.trim().ifBlank { subtitle.trim().ifBlank { null } },
        durationText = null,
        thumbnailUrl = thumbnailUrl.trim().takeIf(String::isNotBlank)
            ?: thumbnail.trim().takeIf(String::isNotBlank),
    ).asMediaItem
}

@Composable
internal fun MusicShortsShelf(
    items: List<YtmHomeSectionItem>,
    isLoading: Boolean,
    onOpenFeed: (YtmHomeSectionItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.music_shorts_title),
            color = colorPalette().text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.music_shorts_subtitle),
            color = colorPalette().textSecondary,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.8f),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = colorPalette().accent, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(R.string.music_shorts_unavailable),
                        color = colorPalette().textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            return@Column
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items, key = { it.videoId.ifBlank { it.id } }) { item ->
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clickable { onOpenFeed(item) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette().background1),
                    ) {
                        ImageCacheFactory.AsyncImage(
                            thumbnailUrl = item.musicShortArtworkUrl(size = 720),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                        )
                    }
                    Text(
                        text = item.artistsText.ifBlank { item.subtitle },
                        color = colorPalette().textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MusicShortsFeed(
    items: List<YtmHomeSectionItem>,
    initialIndex: Int,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onClose: () -> Unit,
    onPlayFullSong: (YtmHomeSectionItem) -> Unit,
    onAddToPlaylist: (YtmHomeSectionItem) -> Unit,
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val audioQualityFormat by rememberPreference(audioQualityFormatKey, AudioQualityFormat.Auto)
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )
    val scope = rememberCoroutineScope()
    val latestLoadMore by rememberUpdatedState(onLoadMore)
    val latestClose by rememberUpdatedState(onClose)
    val latestPlayFullSong by rememberUpdatedState(onPlayFullSong)
    val latestAddToPlaylist by rememberUpdatedState(onAddToPlaylist)
    val progressiveMediaSourceFactory = remember(context) {
        ProgressiveMediaSource.Factory(context.okHttpDataSourceFactory)
    }
    val videoPlayer = remember(context) {
        ExoPlayer.Builder(context.applicationContext)
            .setRenderersFactory(
                DefaultRenderersFactory(context.applicationContext).setEnableDecoderFallback(true)
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(context.okHttpDataSourceFactory))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 1f
            }
    }
    var loadingVideo by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf(false) }
    var pausedByUser by remember { mutableStateOf(false) }
    var videoGeneration by remember { mutableLongStateOf(0L) }
    var commentsVideoId by remember { mutableStateOf<String?>(null) }
    var commentCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val activeIndex = pagerState.currentPage.coerceIn(0, items.lastIndex)
    val activeItem = items[activeIndex]
    val activeMediaId = activeItem.videoId.trim().ifBlank { activeItem.id.trim() }
    var videoLoadAttempt by remember(activeMediaId) { mutableStateOf(0) }
    var previewElapsedMs by remember(activeMediaId) { mutableLongStateOf(0L) }
    var advanceInFlight by remember(activeMediaId) { mutableStateOf(false) }
    var advanceWhenLoaded by remember(activeMediaId) { mutableStateOf(false) }
    var advanceAfterInteraction by remember(activeMediaId) { mutableStateOf(false) }
    var pausedForInteraction by remember(activeMediaId) { mutableStateOf(false) }
    val activeStoredSong by remember(activeMediaId) {
        Database.songTable.findById(activeMediaId)
    }.collectAsState(initial = null, context = Dispatchers.IO)
    val activeArtworkUrl = remember(
        activeMediaId,
        activeStoredSong?.thumbnailUrl,
        activeItem.thumbnailUrl,
        activeItem.thumbnail,
    ) {
        val fetchedArtwork = activeItem.thumbnailUrl
            .trim()
            .takeIf(String::isNotBlank)
            ?: activeItem.thumbnail.trim().takeIf(String::isNotBlank)
        val retainedArtwork = PropUtils.retainIfModified(
            activeStoredSong?.thumbnailUrl,
            fetchedArtwork,
        )
        resolveArtworkUrl(activeMediaId, retainedArtwork).thumbnail(1920)
    }
    val commentsOpen = commentsVideoId != null
    val menuOpen = menuState.isDisplayed
    val interactionOpen = commentsOpen || menuOpen

    fun advancePreview(page: Int) {
        if (advanceInFlight || page != pagerState.currentPage) return
        advanceInFlight = true
        scope.launch {
            if (page < items.lastIndex) {
                pagerState.animateScrollToPage(page + 1)
            } else {
                advanceWhenLoaded = true
                latestLoadMore()
                advanceInFlight = false
            }
        }
    }

    DisposableEffect(videoPlayer) {
        onDispose { videoPlayer.release() }
    }

    LaunchedEffect(pagerState, items.size) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (page >= items.size - 3) latestLoadMore()
            }
    }

    LaunchedEffect(activeMediaId, videoLoadAttempt) {
        val generation = videoGeneration + 1L
        videoGeneration = generation
        loadingVideo = true
        videoError = false
        pausedByUser = false
        commentsVideoId = null
        runCatching {
            videoPlayer.stop()
            videoPlayer.clearMediaItems()
        }
        val resolvedStreams = withContext(Dispatchers.IO) {
            withTimeoutOrNull(SHORT_STREAM_TIMEOUT_MS) {
                coroutineScope {
                    val videoDeferred = async {
                        runCatching { getInnertubeVideoStream(activeMediaId) }.getOrNull()
                    }
                    val audioDeferred = async {
                        runCatching {
                            getInnertubePlayerFormatUrl(
                                videoId = activeMediaId,
                                audioQualityFormat = audioQualityFormat,
                                connectionMetered = false,
                            )
                        }.getOrNull()
                    }
                    videoDeferred.await() to audioDeferred.await()
                }
            }
        }
        if (generation != videoGeneration) return@LaunchedEffect
        val stream = resolvedStreams?.first
        val audioUrl = resolvedStreams?.second
        if (stream == null || audioUrl == null) {
            if (videoLoadAttempt < MAX_SHORT_STREAM_RETRIES) {
                delay(750L * (videoLoadAttempt + 1L))
                videoLoadAttempt += 1
            } else {
                loadingVideo = false
                videoError = true
            }
            return@LaunchedEffect
        }

        val videoMediaItem = MediaItem.Builder()
            .setMediaId(activeMediaId)
            .setUri(stream.url)
            .setMimeType(stream.mimeType)
            .build()
        val videoSource = progressiveMediaSourceFactory.createMediaSource(videoMediaItem)
        val audioMediaItem = MediaItem.Builder()
            .setMediaId("$activeMediaId:audio")
            .setUri(audioUrl)
            .build()
        val audioSource = progressiveMediaSourceFactory.createMediaSource(audioMediaItem)
        videoPlayer.setMediaSource(
            MergingMediaSource(true, true, videoSource, audioSource)
        )
        videoPlayer.volume = 1f
        videoPlayer.prepare()
        videoPlayer.playWhenReady = true
        loadingVideo = false
    }

    // Start comments alongside stream resolution so opening the sheet never waits on playback.
    LaunchedEffect(activeMediaId) {
        ReelCommentsCache.firstPage(activeMediaId)?.let { response ->
            commentCounts = commentCounts + (activeMediaId to response.comments.size)
        }
    }

    LaunchedEffect(
        commentsOpen,
        menuOpen,
        activeMediaId,
        loadingVideo,
        videoError,
        pausedByUser,
    ) {
        videoPlayer.repeatMode =
            if (commentsOpen) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        when {
            menuOpen -> {
            if (videoPlayer.isPlaying && !pausedByUser) {
                pausedForInteraction = true
                videoPlayer.pause()
            }
            }

            commentsOpen -> {
                pausedForInteraction = false
                previewElapsedMs = 0L
                if (!pausedByUser && !loadingVideo && !videoError &&
                    videoPlayer.playbackState != Player.STATE_IDLE
                ) {
                    videoPlayer.play()
                }
            }

            pausedForInteraction && !pausedByUser && !videoError -> {
                pausedForInteraction = false
                videoPlayer.play()
            }
        }
    }

    LaunchedEffect(
        activeMediaId,
        activeIndex,
        interactionOpen,
        loadingVideo,
        videoError,
    ) {
        while (!commentsOpen && !menuOpen && !loadingVideo && !videoError) {
            delay(250L)
            if (videoPlayer.isPlaying) {
                previewElapsedMs += 250L
                if (previewElapsedMs >= 45_000L) {
                    advancePreview(activeIndex)
                    break
                }
            }
        }
    }

    // Comments are a reading surface: keep the same 45-second sample looping behind them.
    LaunchedEffect(commentsOpen, activeMediaId, loadingVideo, videoError, pausedByUser) {
        while (commentsOpen && !loadingVideo && !videoError && !pausedByUser) {
            delay(250L)
            if (videoPlayer.currentPosition >= 45_000L) {
                videoPlayer.seekTo(0L)
                videoPlayer.play()
            } else if (
                videoPlayer.playbackState == Player.STATE_READY &&
                !videoPlayer.isPlaying
            ) {
                videoPlayer.play()
            }
        }
    }

    LaunchedEffect(interactionOpen, advanceAfterInteraction, activeMediaId) {
        if (!interactionOpen && advanceAfterInteraction) {
            advanceAfterInteraction = false
            advancePreview(activeIndex)
        }
    }

    LaunchedEffect(items.size, advanceWhenLoaded, activeMediaId) {
        if (advanceWhenLoaded && activeIndex < items.lastIndex) {
            advanceWhenLoaded = false
            advancePreview(activeIndex)
        }
    }

    LaunchedEffect(videoError, interactionOpen, activeMediaId) {
        if (videoError && !interactionOpen) {
            delay(1_500L)
            if (videoError) advancePreview(activeIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).zIndex(20f)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            key = { index -> items[index].videoId.ifBlank { items[index].id } },
        ) { page ->
            val item = items[page]
            val itemId = item.videoId.ifBlank { item.id }
            MusicShortVideoPage(
                item = item,
                artworkUrl = if (page == activeIndex) {
                    activeArtworkUrl
                } else {
                    item.musicShortArtworkUrl(size = 1920)
                },
                isActive = page == activeIndex,
                player = videoPlayer,
                isLoading = page == activeIndex && loadingVideo,
                hasError = page == activeIndex && videoError,
                isPaused = page == activeIndex && pausedByUser,
                isLiked = page == activeIndex && activeStoredSong?.likedAt != null,
                commentCount = commentCounts[itemId],
                onTogglePlayback = {
                    if (videoPlayer.isPlaying) {
                        videoPlayer.pause()
                        pausedByUser = true
                    } else if (videoPlayer.playbackState != Player.STATE_IDLE) {
                        videoPlayer.play()
                        pausedByUser = false
                    }
                },
                onToggleLike = {
                    item.asMusicShortMediaItem()?.let { mediaItem ->
                        Database.asyncTransaction {
                            songTable.insertIgnore(mediaItem.asSong)
                            songTable.toggleLike(mediaItem.mediaId)
                        }
                    }
                },
                onOpenComments = { commentsVideoId = itemId },
                onPlaybackError = {
                    if (page == pagerState.currentPage) {
                        if (videoLoadAttempt < MAX_SHORT_STREAM_RETRIES) {
                            videoLoadAttempt += 1
                        } else {
                            loadingVideo = false
                            videoError = true
                        }
                    }
                },
                onPlayFullSong = { latestPlayFullSong(item) },
                onAddToPlaylist = { latestAddToPlaylist(item) },
                onShare = { shareMusicShort(context, item) },
                onVideoEnded = {
                    if (page == pagerState.currentPage) {
                        if (commentsOpen) {
                            previewElapsedMs = 0L
                            videoPlayer.seekTo(0L)
                            if (!pausedByUser) videoPlayer.play()
                        } else if (menuOpen) {
                            advanceAfterInteraction = true
                        } else {
                            advancePreview(page)
                        }
                    }
                },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { latestClose() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            if (isLoadingMore) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(start = 10.dp).size(18.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CubicBitsWordmark(
                modifier = Modifier
                    .width(132.dp)
                    .height(54.dp),
            )
        }
    }

    commentsVideoId?.let { videoId ->
        ReelCommentsSheet(
            videoId = videoId,
            onDismiss = { commentsVideoId = null },
            onLoadedCount = { count ->
                commentCounts = commentCounts + (videoId to count)
            },
        )
    }
}

private fun shareMusicShort(context: Context, item: YtmHomeSectionItem) {
    val mediaId = item.videoId.trim().ifBlank { item.id.trim() }
    if (!mediaId.isYouTubeVideoId()) return
    val artist = item.artistsText.ifBlank { item.subtitle }.trim()
    val text = buildString {
        append(item.title.trim())
        if (artist.isNotBlank()) append(" - ").append(artist)
        append('\n').append("https://music.youtube.com/watch?v=").append(mediaId)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, context.getString(R.string.thumbnail_share_chooser)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

@Composable
private fun CubicBitsWordmark(modifier: Modifier = Modifier) {
    val brand = ImageBitmap.imageResource(R.drawable.cubic_bits_brand)
    Canvas(modifier = modifier) {
        drawImage(
            image = brand,
            srcOffset = IntOffset(150, 165),
            srcSize = IntSize(730, 365),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}

@Composable
private fun MusicShortVideoPage(
    item: YtmHomeSectionItem,
    artworkUrl: String?,
    isActive: Boolean,
    player: ExoPlayer,
    isLoading: Boolean,
    hasError: Boolean,
    isPaused: Boolean,
    isLiked: Boolean,
    commentCount: Int?,
    onTogglePlayback: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onPlaybackError: () -> Unit,
    onPlayFullSong: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onVideoEnded: () -> Unit,
) {
    val context = LocalContext.current
    val latestEnded by rememberUpdatedState(onVideoEnded)
    val latestPlaybackError by rememberUpdatedState(onPlaybackError)
    var positionMs by remember(item.videoId, item.id) { mutableLongStateOf(0L) }
    var isActuallyPlaying by remember(item.videoId, item.id) { mutableStateOf(false) }
    var controlsVisible by remember(item.videoId, item.id) { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    DisposableEffect(player, item.videoId, isActive) {
        if (!isActive) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) latestEnded()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isActuallyPlaying = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) = latestPlaybackError()
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, isActive, item.videoId, item.id) {
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            isActuallyPlaying = player.isPlaying
            delay(200L)
        }
    }

    LaunchedEffect(isActive, isActuallyPlaying, isPaused, isLoading, hasError, item.videoId, item.id) {
        if (isActive && isActuallyPlaying && !isPaused && !isLoading && !hasError) {
            controlsVisible = true
            delay(3_000L)
            if (player.isPlaying) controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    fun togglePlaybackWithControls() {
        controlsVisible = true
        onTogglePlayback()
    }

    val progress = (positionMs.toFloat() / 45_000f).coerceIn(0f, 1f)
    val mediaShape = RoundedCornerShape(24.dp)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ImageCacheFactory.AsyncImage(
            thumbnailUrl = artworkUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(34.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.64f),
                            Color.Black.copy(alpha = 0.76f),
                            Color.Black.copy(alpha = 0.94f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 78.dp)
                .fillMaxWidth(0.82f)
                .aspectRatio(0.84f)
                .shadow(
                    elevation = 22.dp,
                    shape = mediaShape,
                    clip = false,
                    ambientColor = Color(0xFFB96CFF).copy(alpha = 0.30f),
                    spotColor = Color(0xFF6EDBFF).copy(alpha = 0.26f),
                )
                .clip(mediaShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.30f), mediaShape),
        ) {
            ImageCacheFactory.AsyncImage(
                thumbnailUrl = artworkUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isActive) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setKeepContentOnPlayerReset(true)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { it.player = player },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = ::togglePlaybackWithControls,
                            onDoubleClick = {
                                controlsVisible = true
                                if (!isLiked) onToggleLike()
                            },
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.38f),
                            )
                        )
                    )
            )

            if (isActive && !hasError && (controlsVisible || isPaused || isLoading)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(90.dp)
                        .clickable(onClick = ::togglePlaybackWithControls),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 4.dp.toPx()
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.72f),
                            radius = size.minDimension / 2f,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = size.minDimension / 2f - stroke,
                            style = Stroke(width = stroke),
                        )
                        drawArc(
                            color = Color(0xFFB96CFF),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (isActuallyPlaying && !isPaused) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }

            if (isActive && hasError) {
                Text(
                    text = stringResource(R.string.music_shorts_unavailable),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }

        if (isActive && !hasError && (controlsVisible || isPaused || isLoading)) {
            Text(
                text = stringResource(R.string.music_shorts_tap_playback),
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 78.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 88.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.music_shorts_sample),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artistsText.ifBlank { item.subtitle },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp, top = 154.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReelActionButton(
                icon = if (isLiked) R.drawable.heart else R.drawable.heart_outline,
                label = stringResource(R.string.music_shorts_like),
                onClick = onToggleLike,
                tint = if (isLiked) Color(0xFFB96CFF) else Color.White,
            )
            ReelActionButton(
                icon = R.drawable.comments,
                label = commentCount?.toString() ?: stringResource(R.string.music_shorts_comments),
                onClick = onOpenComments,
            )
            ReelActionButton(
                icon = R.drawable.play,
                label = stringResource(R.string.music_shorts_play_full),
                onClick = onPlayFullSong,
            )
            ReelActionButton(
                icon = R.drawable.add_in_playlist,
                label = stringResource(R.string.music_shorts_add),
                onClick = onAddToPlaylist,
            )
            ReelActionButton(
                icon = R.drawable.share_social,
                label = stringResource(R.string.music_shorts_share),
                onClick = onShare,
            )
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2B2931).copy(alpha = 0.78f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(27.dp),
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelCommentsSheet(
    videoId: String,
    onDismiss: () -> Unit,
    onLoadedCount: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cachedPage = remember(videoId) { ReelCommentsCache.get(videoId) }
    var comments by remember(videoId) {
        mutableStateOf(cachedPage?.comments.orEmpty().distinctBy(::reelCommentStableId))
    }
    var continuation by remember(videoId) { mutableStateOf(cachedPage?.continuation) }
    var loading by remember(videoId) { mutableStateOf(cachedPage == null) }
    var loadingMore by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        if (cachedPage != null) {
            onLoadedCount(comments.size)
            return@LaunchedEffect
        }
        loading = true
        val response = ReelCommentsCache.firstPage(videoId)
        comments = response?.comments.orEmpty().distinctBy(::reelCommentStableId)
        continuation = response?.continuation
        loading = false
        onLoadedCount(comments.size)
    }

    CustomModalBottomSheet(
        showSheet = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF151515),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.56f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 620.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.music_shorts_comments),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }

                comments.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.music_shorts_no_comments),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 14.sp,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(comments, key = ::reelCommentStableId) { comment ->
                        ReelCommentRow(comment)
                    }
                    continuation?.let { token ->
                        item(key = "load-more-comments") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (loadingMore) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp),
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            if (loadingMore) return@TextButton
                                            loadingMore = true
                                            scope.launch {
                                                val response = withTimeoutOrNull(12_000L) {
                                                    runCatching {
                                                        fetchCommentsPage(videoId, token)
                                                    }.getOrNull()
                                                }
                                                comments = (comments + response?.comments.orEmpty())
                                                    .distinctBy(::reelCommentStableId)
                                                continuation = response?.continuation
                                                    ?.takeIf { it != token }
                                                ReelCommentsCache.put(
                                                    videoId,
                                                    CommentResponse(comments, continuation),
                                                )
                                                loadingMore = false
                                                onLoadedCount(comments.size)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.music_shorts_load_more),
                                            color = Color(0xFFB96CFF),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun reelCommentStableId(comment: Comment): String =
    comment.id.ifBlank {
        "${comment.author}|${comment.timestamp}|${comment.content.hashCode()}"
    }

@Composable
private fun ReelCommentRow(comment: Comment) {
    val avatarUrl = comment.authorThumbnails
        .maxByOrNull { thumbnail -> thumbnail.width * thumbnail.height }
        ?.url
        .orEmpty()
    val avatarInitials = remember(comment.author) {
        comment.author
            .trim()
            .split(Regex("\\s+"))
            .asSequence()
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull(String::firstOrNull)
            .joinToString("")
            .uppercase()
            .ifBlank { "CM" }
    }
    val avatarColor = remember(comment.author) {
        val colors = listOf(
            Color(0xFF3D6FB4),
            Color(0xFF8A4F9E),
            Color(0xFF2F7D6D),
            Color(0xFF9A5B45),
            Color(0xFF6E5BA7),
            Color(0xFFAD3E62),
        )
        colors[(comment.author.hashCode() and Int.MAX_VALUE) % colors.size]
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (avatarUrl.isNotBlank()) {
                ImageCacheFactory.AsyncImage(
                    thumbnailUrl = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.timestamp.isNotBlank()) {
                    Text(
                        text = "  ${comment.timestamp}",
                        color = Color.White.copy(alpha = 0.48f),
                        fontSize = 11.sp,
                    )
                }
            }
            Text(
                text = comment.content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            if (comment.likes > 0) {
                Text(
                    text = stringResource(R.string.music_shorts_likes, comment.likes),
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

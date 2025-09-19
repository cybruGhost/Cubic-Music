package it.fast4x.rimusic.ui.screens.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.enums.ThumbnailCoverType
import it.fast4x.rimusic.enums.ThumbnailType
import it.fast4x.rimusic.service.LoginRequiredException
import it.fast4x.rimusic.service.NoInternetException
import it.fast4x.rimusic.service.PlayableFormatNonSupported
import it.fast4x.rimusic.service.PlayableFormatNotFoundException
import it.fast4x.rimusic.service.TimeoutException
import it.fast4x.rimusic.service.UnknownException
import it.fast4x.rimusic.service.UnplayableException
import it.fast4x.rimusic.service.VideoIdMismatchException
import it.fast4x.rimusic.service.modern.isLocal
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.components.themed.RotateThumbnailCoverAnimation
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.DisposableListener
import it.fast4x.rimusic.utils.clickOnLyricsTextKey
import it.fast4x.rimusic.utils.coverThumbnailAnimationKey
import it.fast4x.rimusic.utils.currentWindow
import it.fast4x.rimusic.utils.doubleShadowDrop
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalConfiguration
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.showCoverThumbnailAnimationKey
import it.fast4x.rimusic.utils.showlyricsthumbnailKey
import it.fast4x.rimusic.utils.showvisthumbnailKey
import it.fast4x.rimusic.utils.thumbnailTypeKey
import it.fast4x.rimusic.utils.thumbnailpauseKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.utils.Toaster
import timber.log.Timber
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// Data class for comments
data class Comment(
    val id: String,
    val author: String,
    val content: String,
    val timestamp: String,
    val likes: Int = 0
)

// Function to fetch comments from API with pagination
suspend fun fetchComments(videoId: String, page: Int = 1): List<Comment> = withContext(Dispatchers.IO) {
    val comments = mutableListOf<Comment>()
    
    try {
        val url = URL("https://yt.omada.cafe/api/v1/comments/$videoId?page=$page")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            
            val jsonResponse = JSONObject(response.toString())
            val commentsArray = jsonResponse.optJSONArray("comments")
            
            if (commentsArray != null) {
                for (i in 0 until commentsArray.length()) {
                    val commentObj = commentsArray.getJSONObject(i)
                    comments.add(
                        Comment(
                            id = commentObj.optString("id", ""),
                            author = commentObj.optString("author", "Unknown"),
                            content = commentObj.optString("content", ""),
                            timestamp = commentObj.optString("timestamp", ""),
                            likes = commentObj.optInt("likes", 0)
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching comments")
    }
    
    return@withContext comments
}

// Function to shuffle comments without repetition
fun shuffleComments(comments: List<Comment>): List<Comment> {
    val shuffled = comments.toMutableList()
    var currentIndex = shuffled.size
    
    // Fisher-Yates shuffle algorithm
    while (currentIndex != 0) {
        val randomIndex = Random.nextInt(currentIndex)
        currentIndex--
        val temp = shuffled[currentIndex]
        shuffled[currentIndex] = shuffled[randomIndex]
        shuffled[randomIndex] = temp
    }
    
    return shuffled
}

@Composable
fun CommentsOverlay(
    videoId: String,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var selectedComment by remember { mutableStateOf<String?>(null) }
    var hasMoreComments by remember { mutableStateOf(true) }
    val scrollState = rememberLazyListState()
    
    // Dark overlay for better readability - only when comments are visible
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .clickable(onClick = onDismiss)
        )
    }
    
    LaunchedEffect(videoId, isVisible) {
        if (isVisible && comments.isEmpty()) {
            isLoading = true
            val fetchedComments = fetchComments(videoId, 1)
            comments = shuffleComments(fetchedComments)
            isLoading = false
            hasMoreComments = fetchedComments.isNotEmpty()
        }
    }
    
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            selectedComment = null
            currentPage = 1
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(700)),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8A2BE2) // Purple color
                )
            } else if (comments.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No comments yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "@cyberghost",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.95f)
                        .height(LocalConfiguration.current.screenHeightDp.dp * 0.8f) // 80% of screen height
                ) {
                    // Comments title
                    Text(
                        text = "Community Reactions",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Comments list
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        itemsIndexed(
                            comments,
                            key = { index, comment ->
                                if (comment.id.isNotBlank()) "${comment.id}_$index"
                                else "${comment.author}_${comment.timestamp}_$index"
                            }
                        ) { index, comment ->

                            // Use the same key for both selection + LazyColumn uniqueness
                            val commentKey = if (comment.id.isNotBlank()) "${comment.id}_$index"
                                            else "${comment.author}_${comment.timestamp}_$index"

                            val isSelected = selectedComment == commentKey

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF8A2BE2).copy(alpha = 0.3f) // Purple highlight
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedComment = if (isSelected) null else commentKey
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = comment.author,
                                            color = if (isSelected) Color(0xFF8A2BE2) else Color.White, // Purple for selected
                                            fontSize = 14.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = comment.timestamp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = comment.content,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (comment.likes > 0) {
                                            Image(
                                                painter = painterResource(R.drawable.heart_shape),
                                                contentDescription = "Likes",
                                                colorFilter = ColorFilter.tint(Color(0xFF8A2BE2)), // Purple heart
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = formatLikes(comment.likes),
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(16.dp),
                                    color = Color(0xFF8A2BE2), // Purple color
                                    strokeWidth = 2.dp
                                )
                            } else if (hasMoreComments) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clickable {
                                            isLoadingMore = true
                                            currentPage++
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val moreComments = fetchComments(videoId, currentPage)
                                                if (moreComments.isEmpty()) {
                                                    hasMoreComments = false
                                                } else {
                                                    // Filter out any comments that might already be in the list
                                                    val newComments = moreComments.filter { newComment -> 
                                                        !comments.any { it.id == newComment.id }
                                                    }
                                                    comments = comments + shuffleComments(newComments)
                                                }
                                                isLoadingMore = false
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "Load more comments...",
                                        color = Color(0xFF8A2BE2), // Purple color
                                        fontSize = 14.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else if (comments.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "No more comments",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        modifier = Modifier.align(Alignment.Center)
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

// Helper function to format likes count
private fun formatLikes(likes: Int): String {
    return when {
        likes >= 1000000 -> "${likes / 1000000}M"
        likes >= 1000 -> "${likes / 1000}K"
        else -> likes.toString()
    }
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun Thumbnail(
    thumbnailTapEnabledKey: Boolean,
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    isShowingStatsForNerds: Boolean,
    onShowStatsForNerds: (Boolean) -> Unit,
    isShowingVisualizer: Boolean,
    onShowEqualizer: (Boolean) -> Unit,
    onMaximize: () -> Unit,
    onDoubleTap: () -> Unit,
    showthumbnail: Boolean,
    modifier: Modifier = Modifier
) {
    println("Thumbnail call")
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    println("Thumbnail call after return")

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }

    var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
    var nullableWindow by remember {
        mutableStateOf(player.currentWindow)
    }

    var error by remember {
        mutableStateOf<PlaybackException?>(player.playerError)
    }

    val localMusicFileNotFoundError = stringResource(R.string.error_local_music_not_found)
    val networkerror = stringResource(R.string.error_a_network_error_has_occurred)
    val notfindplayableaudioformaterror =
        stringResource(R.string.error_couldn_t_find_a_playable_audio_format)
    val originalvideodeletederror =
        stringResource(R.string.error_the_original_video_source_of_this_song_has_been_deleted)
    val songnotplayabledueserverrestrictionerror =
        stringResource(R.string.error_this_song_cannot_be_played_due_to_server_restrictions)
    val videoidmismatcherror =
        stringResource(R.string.error_the_returned_video_id_doesn_t_match_the_requested_one)
    val unknownplaybackerror =
        stringResource(R.string.error_an_unknown_playback_error_has_occurred)

    val unknownerror = stringResource(R.string.error_unknown)
    val nointerneterror = stringResource(R.string.error_no_internet)
    val timeouterror = stringResource(R.string.error_timeout)

    val formatUnsupported = stringResource(R.string.error_file_unsupported_format)

    var artImageAvailable by remember {
        mutableStateOf(true)
    }

    val clickLyricsText by rememberPreference(clickOnLyricsTextKey, true)
    var showvisthumbnail by rememberPreference(showvisthumbnailKey, false)
    
    // State for comments visibility
    var showComments by remember { mutableStateOf(false) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableWindow = player.currentWindow
                // Hide comments when song changes
                showComments = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                error = player.playerError
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                error = playbackException
                binder.stopRadio()
            }
        }
    }

    val window = nullableWindow ?: return

    val coverPainter = ImageCacheFactory.Painter(
        thumbnailUrl = window.mediaItem.mediaMetadata.artworkUri.toString(),
        onError = { 
            artImageAvailable = false 
            // Retry loading after a short delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000) // Wait 1 second
                if (!artImageAvailable) {
                    // Try to preload the image
                    ImageCacheFactory.preloadImage(window.mediaItem.mediaMetadata.artworkUri.toString())
                }
            }
        },
        onSuccess = { 
            artImageAvailable = true 
        }
    )

    val showCoverThumbnailAnimation by rememberPreference(showCoverThumbnailAnimationKey, false)
    var coverThumbnailAnimation by rememberPreference(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl)

    // Dim the thumbnail when comments are visible
    val thumbnailAlpha by animateFloatAsState(
        targetValue = if (showComments) 0.3f else 1f,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    AnimatedContent(
        targetState = window,
        transitionSpec = {
            val duration = 500
            val slideDirection = if (targetState.firstPeriodIndex > initialState.firstPeriodIndex)
                AnimatedContentTransitionScope.SlideDirection.Left
            else AnimatedContentTransitionScope.SlideDirection.Right

            ContentTransform(
                targetContentEnter = slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeIn(
                    animationSpec = tween(duration)
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(duration)
                ),
                initialContentExit = slideOutOfContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeOut(
                    animationSpec = tween(duration)
                ) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(duration)
                ),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        contentAlignment = Alignment.Center, label = ""
    ) { currentWindow ->

        val thumbnailType by rememberPreference(thumbnailTypeKey, ThumbnailType.Modern)

        var modifierUiType by remember { mutableStateOf(modifier) }

        if (showthumbnail)
            if ((!isShowingLyrics && !isShowingVisualizer) || (isShowingVisualizer && showvisthumbnail) || (isShowingLyrics && showlyricsthumbnail))
                if (thumbnailType == ThumbnailType.Modern)
                    modifierUiType = modifier
                        .padding(vertical = 8.dp)
                        .aspectRatio(1f)
                        .fillMaxSize()
                        .doubleShadowDrop(if (showCoverThumbnailAnimation) CircleShape else thumbnailShape(), 4.dp, 8.dp)
                        .clip(if (showCoverThumbnailAnimation) CircleShape else thumbnailShape())
                else modifierUiType = modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clip(if (showCoverThumbnailAnimation) CircleShape else thumbnailShape())

        Box(
            modifier = modifierUiType
        ) {
            if (showthumbnail) {
                if ((!isShowingLyrics && !isShowingVisualizer) || (isShowingVisualizer && showvisthumbnail) || (isShowingLyrics && showlyricsthumbnail))
                    if (artImageAvailable) {
                        if (showCoverThumbnailAnimation)
                            RotateThumbnailCoverAnimation(
                                painter = coverPainter,
                                isSongPlaying = player.isPlaying,
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { onShowStatsForNerds(true) },
                                            onTap = if (thumbnailTapEnabledKey && !showComments) {
                                                {
                                                    onShowLyrics(true)
                                                    onShowEqualizer(false)
                                                }
                                            } else null,
                                            onDoubleTap = { onDoubleTap() }
                                        )
                                    }
                                    .graphicsLayer { alpha = thumbnailAlpha },
                                type = coverThumbnailAnimation
                            )
                        else
                            Image (
                                painter = coverPainter,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { onShowStatsForNerds(true) },
                                            onTap = if (thumbnailTapEnabledKey && !showComments) {
                                                {
                                                    onShowLyrics(true)
                                                    onShowEqualizer(false)
                                                }
                                            } else null,
                                            onDoubleTap = { onDoubleTap() }
                                        )
                                    }
                                    .fillMaxSize()
                                    .clip(thumbnailShape())
                                    .graphicsLayer { alpha = thumbnailAlpha }
                            )

                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_box),
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { onShowStatsForNerds(true) },
                                        onTap = if (thumbnailTapEnabledKey && !showComments) {
                                            {
                                                onShowLyrics(true)
                                                onShowEqualizer(false)
                                            }
                                        } else null,
                                        onDoubleTap = { onDoubleTap() }
                                    )
                                }
                                .fillMaxSize()
                                .clip(thumbnailShape())
                                .graphicsLayer { alpha = thumbnailAlpha },
                            contentDescription = "Background Image",
                            contentScale = ContentScale.Fit
                        )
                    }

                // Comments toggle button (pencil icon) - only show when comments are not visible
                if (!showComments) {
                    Image(
                        painter = painterResource(R.drawable.pencil),
                        contentDescription = "Toggle comments",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clickable {
                                showComments = true
                            }
                    )
                }

                // Comments overlay
                CommentsOverlay(
                    videoId = currentWindow.mediaItem.mediaId,
                    isVisible = showComments,
                    onDismiss = { showComments = false }
                )

                if (showlyricsthumbnail)
                    Lyrics(
                        mediaId = currentWindow.mediaItem.mediaId,
                        isDisplayed = isShowingLyrics && error == null && !showComments,
                        onDismiss = {
                            onShowLyrics(false)
                        },
                        ensureSongInserted = { Database.insertIgnore( currentWindow.mediaItem ) },
                        size = thumbnailSizeDp,
                        mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                        durationProvider = player::getDuration,
                        isLandscape = isLandscape,
                        clickLyricsText = clickLyricsText,
                    )

                StatsForNerds(
                    mediaId = currentWindow.mediaItem.mediaId,
                    isDisplayed = isShowingStatsForNerds && error == null && !showComments,
                    onDismiss = { onShowStatsForNerds(false) }
                )
                if (showvisthumbnail) {
                    NextVisualizer(
                        isDisplayed = isShowingVisualizer && !showComments
                    )
                }

                var errorCounter by remember { mutableIntStateOf(0) }

                if (error != null) {
                    errorCounter = errorCounter.plus(1)
                    if (errorCounter < 3) {
                        Timber.e("Playback error: ${error?.cause?.cause}")
                        Toaster.e(
                            if (currentWindow.mediaItem.isLocal)
                                localMusicFileNotFoundError
                            else when (error?.cause?.cause) {
                                is UnresolvedAddressException, is UnknownHostException -> networkerror
                                is PlayableFormatNotFoundException -> notfindplayableaudioformaterror
                                is UnplayableException -> originalvideodeletederror
                                is LoginRequiredException -> songnotplayabledueserverrestrictionerror
                                is VideoIdMismatchException -> videoidmismatcherror
                                is PlayableFormatNonSupported -> formatUnsupported
                                is NoInternetException -> nointerneterror
                                is TimeoutException -> timeouterror
                                is UnknownException -> unknownerror
                                else -> unknownplaybackerror
                            }
                        )
                    } else errorCounter = 0
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
fun Modifier.thumbnailpause(
    shouldBePlaying: Boolean
) = composed {
    var thumbnailpause by rememberPreference(thumbnailpauseKey, false)
    val scale by animateFloatAsState(if ((thumbnailpause) && (!shouldBePlaying)) 0.9f else 1f)

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
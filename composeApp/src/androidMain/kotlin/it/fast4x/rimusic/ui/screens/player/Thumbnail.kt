package it.fast4x.rimusic.ui.screens.player

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
import androidx.compose.foundation.layout.Arrangement
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.Job

// Data class for comments with author thumbnails
data class Comment(
    val id: String,
    val author: String,
    val content: String,
    val timestamp: String,
    val likes: Int = 0,
    val authorThumbnails: List<AuthorThumbnail> = emptyList()
)

// Data class for author thumbnails
data class AuthorThumbnail(
    val url: String,
    val width: Int,
    val height: Int
)

// Data class for comment response with continuation
data class CommentResponse(
    val comments: List<Comment>,
    val continuation: String?
)

// Function to fetch comments with pagination support
suspend fun fetchCommentsPage(videoId: String, continuation: String? = null): CommentResponse = withContext(Dispatchers.IO) {
    val comments = mutableListOf<Comment>()
    var nextContinuation: String? = null
    
    try {
        // Build URL for this page
        val urlStr = if (continuation == null) {
            "https://yt.omada.cafe/api/v1/comments/$videoId"
        } else {
            "https://yt.omada.cafe/api/v1/comments/$videoId?continuation=${URLEncoder.encode(continuation, "UTF-8")}"
        }

        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            val jsonResponse = JSONObject(response)

            val commentsArray = jsonResponse.optJSONArray("comments")
            if (commentsArray != null && commentsArray.length() > 0) {
                for (i in 0 until commentsArray.length()) {
                    val commentObj = commentsArray.getJSONObject(i)

                    // Parse author thumbnails
                    val thumbnailsArray = commentObj.optJSONArray("authorThumbnails")
                    val authorThumbnails = mutableListOf<AuthorThumbnail>()
                    if (thumbnailsArray != null) {
                        for (j in 0 until thumbnailsArray.length()) {
                            val thumbnailObj = thumbnailsArray.getJSONObject(j)
                            authorThumbnails.add(
                                AuthorThumbnail(
                                    url = thumbnailObj.optString("url", ""),
                                    width = thumbnailObj.optInt("width", 0),
                                    height = thumbnailObj.optInt("height", 0)
                                )
                            )
                        }
                    }

                    comments.add(
                        Comment(
                            id = commentObj.optString("commentId", ""),
                            author = commentObj.optString("author", "Unknown"),
                            content = commentObj.optString("content", ""),
                            timestamp = commentObj.optString("publishedText", ""),
                            likes = commentObj.optInt("likeCount", 0),
                            authorThumbnails = authorThumbnails
                        )
                    )
                }
            }

            // Grab continuation token for the next page
            nextContinuation = if (jsonResponse.has("continuation")) {
                jsonResponse.optString("continuation", null)
            } else null

        } else {
            Timber.e("Failed to fetch comments: HTTP ${connection.responseCode}")
        }

    } catch (e: Exception) {
        Timber.e(e, "Error fetching comments")
    }

    return@withContext CommentResponse(comments, nextContinuation)
}

@Composable
fun CommentsOverlay(
    videoId: String,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var rotationJob by remember { mutableStateOf<Job?>(null) }
    var isRotationRunning by remember { mutableStateOf(true) }
    var selectedCommentId by remember { mutableStateOf<String?>(null) }
    var expandedComments by remember { mutableStateOf<Set<String>>(setOf()) }
    var currentContinuation by remember { mutableStateOf<String?>(null) }
    var hasMoreComments by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    
    // Pause rotation when user interacts with comments
    val isUserInteracting = selectedCommentId != null || expandedComments.isNotEmpty()
    
    // Load comments when overlay becomes visible
    LaunchedEffect(videoId, isVisible) {
        if (isVisible && comments.isEmpty()) {
            isLoading = true
            errorMessage = null
            val response = fetchCommentsPage(videoId)
            if (response.comments.isNotEmpty()) {
                comments = response.comments
                currentContinuation = response.continuation
                hasMoreComments = response.continuation != null
                startRotation(comments, currentIndex, isRotationRunning, isUserInteracting) { newIndex ->
                    currentIndex = newIndex
                }
            } else {
                errorMessage = "No comments available"
            }
            isLoading = false
        }
    }
    
    // Handle rotation when visibility or interaction changes
    LaunchedEffect(isVisible, comments, isUserInteracting) {
        if (isVisible && comments.isNotEmpty()) {
            if (isRotationRunning && !isUserInteracting) {
                rotationJob?.cancel()
                startRotation(comments, currentIndex, isRotationRunning, isUserInteracting) { newIndex ->
                    currentIndex = newIndex
                }
            } else if (isUserInteracting) {
                rotationJob?.cancel()
            }
        } else {
            rotationJob?.cancel()
            isRotationRunning = true
            selectedCommentId = null
            expandedComments = setOf()
        }
    }
    
    // Function to load more comments
    val loadMoreComments = {
        if (hasMoreComments && !isLoadingMore && currentContinuation != null) {
            isLoadingMore = true
            CoroutineScope(Dispatchers.IO).launch {
                val response = fetchCommentsPage(videoId, currentContinuation)
                if (response.comments.isNotEmpty()) {
                    comments = comments + response.comments
                    currentContinuation = response.continuation
                    hasMoreComments = response.continuation != null
                }
                isLoadingMore = false
            }
        }
    }
    
    // Dark overlay for better readability
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
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(700)),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8A2BE2)
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage!!,
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
                        text = "aghh song doesnt have comments lmo!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Display current comment with rotation
                val currentComment = comments.getOrNull(currentIndex)
                if (currentComment != null) {
                    val isSelected = selectedCommentId == currentComment.id
                    val isExpanded = expandedComments.contains(currentComment.id)
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.95f) // Increased width for better visibility
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF8A2BE2).copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedCommentId = if (isSelected) null else currentComment.id
                                // Pause rotation when user selects a comment
                                if (!isSelected) {
                                    rotationJob?.cancel()
                                } else {
                                    isRotationRunning = true
                                    startRotation(comments, currentIndex, isRotationRunning, isUserInteracting) { newIndex ->
                                        currentIndex = newIndex
                                    }
                                }
                            }
                    ) {
                        // Author info with profile picture
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Author thumbnail (profile picture)
                            val profilePicUrl = currentComment.authorThumbnails
                                .maxByOrNull { it.width * it.height }?.url
                            
                            if (!profilePicUrl.isNullOrEmpty()) {
                                Image(
                                    painter = ImageCacheFactory.Painter(profilePicUrl),
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback if no thumbnail available
                                Image(
                                    painter = painterResource(R.drawable.ic_launcher_box),
                                    contentDescription = "Default profile",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Author name with limited width to prevent pushing likes away
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentComment.author,
                                    color = if (isSelected) Color(0xFF8A2BE2) else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentComment.timestamp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Like count with heart icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.heart_shape),
                                    contentDescription = "Likes",
                                    colorFilter = ColorFilter.tint(
                                        if (currentComment.likes > 0) Color(0xFF8A2BE2) 
                                        else Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formatLikes(currentComment.likes),
                                    color = if (currentComment.likes > 0) Color.White.copy(alpha = 0.8f) 
                                           else Color.White.copy(alpha = 0.3f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        
                        // Comment content with scroll for long comments
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(if (isExpanded) 200.dp else 100.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentComment.content,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Read more/less button for long comments
                        if (currentComment.content.length > 150) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable {
                                        expandedComments = if (isExpanded) {
                                            expandedComments - currentComment.id
                                        } else {
                                            expandedComments + currentComment.id
                                        }
                                        // Pause rotation when user interacts with comments
                                        rotationJob?.cancel()
                                    }
                            ) {
                                Text(
                                    text = if (isExpanded) "Read less" else "Read more",
                                    color = Color(0xFF8A2BE2),
                                    fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                        
                        // Navigation buttons and comment counter - FIXED VERSION
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Navigation row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous button
                                IconButton(
                                    onClick = {
                                        rotationJob?.cancel()
                                        isRotationRunning = false
                                        if (currentIndex > 0) {
                                            currentIndex--
                                        }
                                    },
                                    enabled = currentIndex > 0
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.backward),
                                        contentDescription = "Previous comment",
                                        colorFilter = ColorFilter.tint(
                                            if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Progress dots - Only show 4 dots maximum - FIXED VERSION
                                val visibleDots = 4
                                val startIndex = kotlin.math.max(0, kotlin.math.min(currentIndex - 1, comments.size - visibleDots))
                                val endIndex = kotlin.math.min(startIndex + visibleDots, comments.size)

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    for (i in startIndex until endIndex) {
                                        // Animated progress dot
                                        AnimatedProgressDot(
                                            isActive = i == currentIndex,
                                            modifier = Modifier.size(8.dp),
                                            activeColor = Color(0xFF8A2BE2),
                                            inactiveColor = Color.White.copy(alpha = 0.3f)
                                        )
                                        if (i < endIndex - 1) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Next button
                                IconButton(
                                    onClick = {
                                        rotationJob?.cancel()
                                        isRotationRunning = false
                                        if (currentIndex < comments.size - 1) {
                                            currentIndex++
                                        } else if (hasMoreComments && !isLoadingMore) {
                                            // Load more comments when reaching the end
                                            loadMoreComments()
                                        }
                                    },
                                    enabled = currentIndex < comments.size - 1 || (hasMoreComments && !isLoadingMore)
                                ) {
                                    if (isLoadingMore && currentIndex == comments.size - 1) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color(0xFF8A2BE2),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.forward),
                                            contentDescription = "Next comment",
                                            colorFilter = ColorFilter.tint(
                                                if (currentIndex < comments.size - 1 || (hasMoreComments && !isLoadingMore)) 
                                                    Color.White 
                                                else Color.White.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // Comment counter - NOW VISIBLE with proper spacing
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${currentIndex + 1}/${comments.size}" + if (hasMoreComments) "+" else "",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Animated progress dot component with purple color
@Composable
fun AnimatedProgressDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF8A2BE2),
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    var animationState by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                // Pulse animation for active dot
                delay(500)
                animationState = (animationState + 1) % 2
            }
        }
    }
    
    val dotSize by animateFloatAsState(
        targetValue = if (isActive && animationState == 1) 1.2f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "dotSize"
    )
    
    val dotColor by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(durationMillis = 300),
        label = "dotColor"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = dotSize
                scaleY = dotSize
            }
            .clip(CircleShape)
            .background(if (isActive) activeColor else inactiveColor)
    )
}

// Helper function to start comment rotation
private fun startRotation(
    comments: List<Comment>,
    currentIndex: Int,
    isRotationRunning: Boolean,
    isUserInteracting: Boolean,
    onIndexChange: (Int) -> Unit
): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        var index = currentIndex
        while (isRotationRunning && !isUserInteracting) {
            delay(calculateDelay(comments.getOrNull(index)?.content))
            
            if (isRotationRunning && !isUserInteracting) {
                // Move to next comment
                index = (index + 1) % comments.size
                onIndexChange(index)
            }
        }
    }
}

// Helper function to calculate delay based on comment length
private fun calculateDelay(content: String?): Long {
    if (content == null) return 9000L // 9 seconds base
    
    val baseDelay = 9000L // 9 seconds base
    val wordCount = content.split("\\s+".toRegex()).size
    
    return if (wordCount > 30) {
        baseDelay + ((wordCount - 30) / 10) * 1000
    } else {
        baseDelay
    }
}

// Helper function to format likes count
private fun formatLikes(likes: Int): String {
    return when {
        likes >= 1000000 -> String.format("%.1fM", likes / 1000000.0)
        likes >= 1000 -> String.format("%.1fK", likes / 1000.0)
        else -> likes.toString()
    }
}

// The rest of the Thumbnail composable remains exactly the same as in your original code
// Only the CommentsOverlay function has been modified

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
                                    .clickable {
                                        if (thumbnailTapEnabledKey && !showComments) {
                                            onShowLyrics(true)
                                            onShowEqualizer(false)
                                        }
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
                                    .clickable {
                                        if (thumbnailTapEnabledKey && !showComments) {
                                            onShowLyrics(true)
                                            onShowEqualizer(false)
                                        }
                                    }
                                    .fillMaxSize()
                                    .clip(thumbnailShape())
                                    .graphicsLayer { alpha = thumbnailAlpha }
                            )

                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_box),
                            modifier = Modifier
                                .clickable {
                                    if (thumbnailTapEnabledKey && !showComments) {
                                        onShowLyrics(true)
                                        onShowEqualizer(false)
                                    }
                                }
                                .fillMaxSize()
                                .clip(thumbnailShape())
                                .graphicsLayer { alpha = thumbnailAlpha },
                            contentDescription = "Background Image",
                            contentScale = ContentScale.Fit
                        )
                    }

                // Comments toggle button (comments icon) - only show when comments are not visible
                if (!showComments) {
                    Image(
                        painter = painterResource(R.drawable.comments),
                        contentDescription = "Toggle comments",
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

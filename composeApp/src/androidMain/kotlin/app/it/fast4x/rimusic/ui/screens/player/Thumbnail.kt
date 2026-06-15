package app.it.fast4x.rimusic.ui.screens.player

import android.content.Intent
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.kreate.android.R
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ThumbnailCoverType
import app.it.fast4x.rimusic.enums.ThumbnailType
import app.it.fast4x.rimusic.service.LoginRequiredException
import app.it.fast4x.rimusic.service.NoInternetException
import app.it.fast4x.rimusic.service.PlayableFormatNonSupported
import app.it.fast4x.rimusic.service.PlayableFormatNotFoundException
import app.it.fast4x.rimusic.service.TimeoutException
import app.it.fast4x.rimusic.service.UnknownException
import app.it.fast4x.rimusic.service.UnplayableException
import app.it.fast4x.rimusic.service.VideoIdMismatchException
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.ui.components.themed.RotateThumbnailCoverAnimation
import app.it.fast4x.rimusic.ui.screens.player.components.YoutubePlayer
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.ExternalUris
import app.it.fast4x.rimusic.utils.OmadaSearchClient
import app.it.fast4x.rimusic.utils.OmadaSearchResult
import app.it.fast4x.rimusic.utils.buildThumbnailShareLink
import app.it.fast4x.rimusic.utils.clickOnLyricsTextKey
import app.it.fast4x.rimusic.utils.coverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.doubleShadowDrop
import app.it.fast4x.rimusic.utils.shareThumbnailCard
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.playerVideoModeActiveKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.showCoverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.showvisthumbnailKey
import app.it.fast4x.rimusic.utils.thumbnailTypeKey
import app.it.fast4x.rimusic.utils.thumbnailpauseKey
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.Innertube.SearchFilter
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.coil.*
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

private const val YOUTUBEI_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3"

// Function to fetch comments with pagination support. Innertube is tried first, Omada stays as fallback.
suspend fun fetchCommentsPage(videoId: String, continuation: String? = null): CommentResponse = withContext(Dispatchers.IO) {
    runCatching { fetchInnertubeCommentsPage(videoId, continuation) }
        .onFailure { Timber.w(it, "Innertube comments failed, falling back to Omada for %s", videoId) }
        .getOrNull()
        ?.takeIf { it.comments.isNotEmpty() }
        ?.let { return@withContext it }

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
                jsonResponse.optString("continuation").takeIf { it.isNotBlank() }
            } else null

        } else {
            Timber.e("Failed to fetch comments: HTTP ${connection.responseCode}")
        }

    } catch (e: Exception) {
        Timber.e(e, "Error fetching comments")
    }

    return@withContext CommentResponse(comments, nextContinuation)
}

private fun innertubeContextJson(): JSONObject = JSONObject()
    .put("client", JSONObject()
        .put("clientName", "WEB")
        .put("clientVersion", "2.20240605.01.00")
        .put("hl", java.util.Locale.getDefault().language.ifBlank { "en" })
        .put("gl", java.util.Locale.getDefault().country.ifBlank { "US" })
    )

private fun innertubeText(obj: JSONObject?): String =
    obj?.optString("simpleText")?.takeIf { it.isNotBlank() }
        ?: obj?.optJSONArray("runs")?.let { runs ->
            buildString {
                for (i in 0 until runs.length()) {
                    append(runs.optJSONObject(i)?.optString("text").orEmpty())
                }
            }.trim()
        }.orEmpty()

private fun parseCompactCount(text: String): Int {
    val cleaned = text.trim().replace(",", "")
    if (cleaned.isBlank()) return 0
    val multiplier = when {
        cleaned.endsWith("K", ignoreCase = true) -> 1_000
        cleaned.endsWith("M", ignoreCase = true) -> 1_000_000
        else -> 1
    }
    return (cleaned.dropLast(if (multiplier == 1) 0 else 1).toDoubleOrNull() ?: 0.0).times(multiplier).toInt()
}

private fun parseInnertubeComment(renderer: JSONObject): Comment? {
    val author = innertubeText(renderer.optJSONObject("authorText")).ifBlank { "Unknown" }
    val content = innertubeText(renderer.optJSONObject("contentText"))
    if (content.isBlank()) return null

    val thumbnails = mutableListOf<AuthorThumbnail>()
    renderer.optJSONObject("authorThumbnail")?.optJSONArray("thumbnails")?.let { array ->
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            thumbnails.add(
                AuthorThumbnail(
                    url = item.optString("url", ""),
                    width = item.optInt("width", 0),
                    height = item.optInt("height", 0)
                )
            )
        }
    }

    return Comment(
        id = renderer.optString("commentId").ifBlank { renderer.optString("trackingParams", content.hashCode().toString()) },
        author = author,
        content = content,
        timestamp = innertubeText(renderer.optJSONObject("publishedTimeText")),
        likes = parseCompactCount(innertubeText(renderer.optJSONObject("voteCount"))),
        authorThumbnails = thumbnails
    )
}

private fun collectInnertubeComments(node: Any?, comments: MutableList<Comment>, continuations: MutableList<String>) {
    when (node) {
        is JSONObject -> {
            node.optJSONObject("commentThreadRenderer")
                ?.optJSONObject("comment")
                ?.optJSONObject("commentRenderer")
                ?.let { parseInnertubeComment(it) }
                ?.let { comments.add(it) }

            node.optJSONObject("commentRenderer")
                ?.let { parseInnertubeComment(it) }
                ?.let { comments.add(it) }

            node.optJSONObject("continuationCommand")?.optString("token")?.takeIf { it.isNotBlank() }?.let(continuations::add)
            node.optJSONObject("nextContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }?.let(continuations::add)

            val keys = node.keys()
            while (keys.hasNext()) collectInnertubeComments(node.opt(keys.next()), comments, continuations)
        }
        is JSONArray -> {
            for (i in 0 until node.length()) collectInnertubeComments(node.opt(i), comments, continuations)
        }
    }
}

private fun fetchInnertubeCommentsPage(videoId: String, continuation: String?): CommentResponse {
    val endpoint = if (continuation == null) "next" else "browse"
    val body = JSONObject()
        .put("context", innertubeContextJson())
        .apply {
            if (continuation == null) put("videoId", videoId) else put("continuation", continuation)
        }
        .toString()

    val connection = (URL("https://www.youtube.com/youtubei/v1/$endpoint?key=$YOUTUBEI_KEY").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 8000
        readTimeout = 8000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    }

    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
    if (connection.responseCode !in 200..299) error("Innertube comments HTTP ${connection.responseCode}")

    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
    val comments = mutableListOf<Comment>()
    val continuations = mutableListOf<String>()
    collectInnertubeComments(JSONObject(response), comments, continuations)
    val uniqueComments = comments.distinctBy { it.id }
    val nextContinuation = continuations.firstOrNull { it != continuation }
    if (continuation == null && uniqueComments.isEmpty() && nextContinuation != null) {
        return fetchInnertubeCommentsPage(videoId, nextContinuation)
    }
    return CommentResponse(uniqueComments, nextContinuation)
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
    val scope = rememberCoroutineScope()

    // Pause rotation when user interacts with comments
    val isUserInteracting = selectedCommentId != null || expandedComments.isNotEmpty()

    fun launchRotation() {
        rotationJob?.cancel()
        if (isVisible && comments.isNotEmpty() && isRotationRunning && !isUserInteracting) {
            rotationJob = startRotation(
                comments = comments,
                currentIndex = currentIndex,
                isRotationRunning = isRotationRunning,
                isUserInteracting = isUserInteracting
            ) { newIndex ->
                currentIndex = newIndex.coerceIn(0, (comments.lastIndex).coerceAtLeast(0))
            }
        }
    }
    
    // Load comments when overlay becomes visible
    LaunchedEffect(videoId, isVisible) {
        if (!isVisible) {
            return@LaunchedEffect
        }

        comments = emptyList()
        currentIndex = 0
        selectedCommentId = null
        expandedComments = emptySet()
        currentContinuation = null
        hasMoreComments = true
        isLoadingMore = false

        if (comments.isEmpty()) {
            isLoading = true
            errorMessage = null
            val response = fetchCommentsPage(videoId)
            if (response.comments.isNotEmpty()) {
                comments = response.comments
                currentContinuation = response.continuation
                hasMoreComments = response.continuation != null
                currentIndex = 0
                launchRotation()
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
                launchRotation()
            } else if (isUserInteracting) {
                rotationJob?.cancel()
            }
        } else {
            rotationJob?.cancel()
            isRotationRunning = true
            currentIndex = 0
            selectedCommentId = null
            expandedComments = emptySet()
        }
    }
    
    // Function to load more comments
    val loadMoreComments: (Boolean) -> Unit = { advanceToNewPage ->
        if (hasMoreComments && !isLoadingMore && currentContinuation != null) {
            isLoadingMore = true
            scope.launch {
                val oldCount = comments.size
                val response = fetchCommentsPage(videoId, currentContinuation)
                if (response.comments.isNotEmpty()) {
                    comments = comments + response.comments
                    currentContinuation = response.continuation
                    hasMoreComments = response.continuation != null
                    if (advanceToNewPage) {
                        currentIndex = oldCount.coerceAtMost(comments.lastIndex)
                    }
                } else {
                    hasMoreComments = false
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
                                selectedCommentId = currentComment.id
                                rotationJob?.cancel()
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
                                        selectedCommentId = comments.getOrNull((currentIndex - 1).coerceAtLeast(0))?.id
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
                                            selectedCommentId = comments.getOrNull(currentIndex)?.id
                                        } else if (hasMoreComments && !isLoadingMore) {
                                            loadMoreComments(true)
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
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val displayedPlayerState = rememberDisplayedPlayerState(binder)

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }

    var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
 
    val showCommentsButton by rememberPreference("show_comments_button", true)
    val showVideoButton by rememberPreference(showButtonPlayerVideoKey, false)
    var showVideo by rememberPreference(playerVideoModeActiveKey, false)

    LaunchedEffect(showVideoButton) {
        if (!showVideoButton) showVideo = false
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
    var showThumbnailShareDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Hide comments when song changes
                showComments = false
                showVideo = false
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

    val displayedMediaItem = displayedPlayerState.mediaItem ?: return
    val baseVideoId = remember(displayedMediaItem.mediaId) {
        displayedMediaItem.mediaId.toYoutubeVideoId().takeIf { it.isYoutubeVideoId() }
    }
    val resolvedVideoId = baseVideoId

    LaunchedEffect(displayedMediaItem.mediaId, displayedMediaItem.mediaMetadata.artworkUri) {
        artImageAvailable = true
        if (displayedMediaItem.mediaMetadata.artworkUri != null) {
            ImageCacheFactory.preloadImage(displayedMediaItem.mediaMetadata.artworkUri.toString())
        }
    }

    val coverPainter = ImageCacheFactory.Painter(
        thumbnailUrl = displayedMediaItem.mediaMetadata.artworkUri?.toString().orEmpty(),
        onError = { 
            artImageAvailable = false 
            // Retry loading after a short delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000) // Wait 1 second
                if (!artImageAvailable) {
                    // Try to preload the image
                    displayedMediaItem.mediaMetadata.artworkUri?.toString()?.let(ImageCacheFactory::preloadImage)
                }
            }
        },
        onSuccess = { 
            artImageAvailable = true 
        }
    )
    if (showThumbnailShareDialog) {
        ThumbnailShareDialog(
            mediaItem = displayedMediaItem,
            currentThumbnailUrl = displayedMediaItem.mediaMetadata.artworkUri?.toString(),
            onDismiss = { showThumbnailShareDialog = false }
        )
    }

    val showCoverThumbnailAnimation by rememberPreference(showCoverThumbnailAnimationKey, false)
    var coverThumbnailAnimation by rememberPreference(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl)

    // Dim the thumbnail when comments are visible
    val thumbnailAlpha by animateFloatAsState(
        targetValue = if (showComments) 0.3f else 1f,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    AnimatedContent(
        targetState = displayedMediaItem,
        transitionSpec = {
            val duration = 500
            val slideDirection = if (targetState.mediaId > initialState.mediaId)
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
    ) { currentDisplayedMediaItem: MediaItem ->

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
                if (showVideoButton && showVideo && resolvedVideoId != null) {
                    YoutubePlayer(
                        ytVideoId = resolvedVideoId,
                        lifecycleOwner = lifecycleOwner,
                        showPlayer = true,
                        syncPlayer = player,
                        onCurrentSecond = {},
                        onSwitchToAudioPlayer = { showVideo = false }
                    )
                } else if ((!isShowingLyrics && !isShowingVisualizer) || (isShowingVisualizer && showvisthumbnail) || (isShowingLyrics && showlyricsthumbnail))
                    if (artImageAvailable) {
                        if (showCoverThumbnailAnimation)
                            RotateThumbnailCoverAnimation(
                                painter = coverPainter,
                                isSongPlaying = displayedPlayerState.shouldBePlaying,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                        if (thumbnailTapEnabledKey && !showComments) {
                                            onShowLyrics(true)
                                            onShowEqualizer(false)
                                        }
                                    },
                                        onLongClick = { showThumbnailShareDialog = true }
                                    )
                                    .graphicsLayer { alpha = thumbnailAlpha },
                                type = coverThumbnailAnimation
                            )
                        else
                            Image (
                                painter = coverPainter,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                        if (thumbnailTapEnabledKey && !showComments) {
                                            onShowLyrics(true)
                                            onShowEqualizer(false)
                                        }
                                    },
                                        onLongClick = { showThumbnailShareDialog = true }
                                    )
                                    .fillMaxSize()
                                    .clip(thumbnailShape())
                                    .graphicsLayer { alpha = thumbnailAlpha }
                            )

                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_box),
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                    if (thumbnailTapEnabledKey && !showComments) {
                                        onShowLyrics(true)
                                        onShowEqualizer(false)
                                    }
                                },
                                    onLongClick = { showThumbnailShareDialog = true }
                                )
                                .fillMaxSize()
                                .clip(thumbnailShape())
                                .graphicsLayer { alpha = thumbnailAlpha },
                            contentDescription = "Background Image",
                            contentScale = ContentScale.Fit
                        )
                    }

                // Comments toggle button (comments icon) - only show when comments are not visible
                if (!showVideo && !showComments && showCommentsButton) {
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

                if (!showComments && showVideoButton && resolvedVideoId != null) {
                    Image(
                        painter = painterResource(if (showVideo) R.drawable.musical_notes else R.drawable.video),
                        contentDescription = if (showVideo) "Return to audio" else "Show video",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = if (showCommentsButton && !showVideo) 58.dp else 8.dp,
                                end = 8.dp
                            )
                            .shadow(8.dp, RoundedCornerShape(12.dp), clip = false)
                            .size(width = 50.dp, height = 36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .clickable {
                                showVideo = !showVideo && resolvedVideoId != null
                                if (showVideo) {
                                    showComments = false
                                    onShowLyrics(false)
                                    onShowEqualizer(false)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                // Comments overlay
                CommentsOverlay(
                    videoId = resolvedVideoId ?: currentDisplayedMediaItem.mediaId.toYoutubeVideoId(),
                    isVisible = showComments,
                    onDismiss = { showComments = false }
                )

                if (showlyricsthumbnail)
                    Lyrics(
                        mediaId = currentDisplayedMediaItem.mediaId,
                        isDisplayed = isShowingLyrics && error == null && !showComments,
                        onDismiss = {
                            onShowLyrics(false)
                        },
                        ensureSongInserted = { insertIgnore(currentDisplayedMediaItem) },
                        size = thumbnailSizeDp,
                        mediaMetadataProvider = currentDisplayedMediaItem::mediaMetadata,
                        durationProvider = player::getDuration,
                        isLandscape = isLandscape,
                        clickLyricsText = clickLyricsText,
                    )

                StatsForNerds(
                    mediaId = currentDisplayedMediaItem.mediaId,
                    isDisplayed = isShowingStatsForNerds && error == null && !showComments,
                    onDismiss = { onShowStatsForNerds(false) }
                )
                if (showvisthumbnail) {
                    NextVisualizer(
                        isDisplayed = isShowingVisualizer && !showComments
                    )
                }

                var lastPlaybackError by remember { mutableStateOf<PlaybackException?>(null) }

                LaunchedEffect(error) {
                    val currentError = error
                    if (currentError != null && currentError !== lastPlaybackError) {
                        lastPlaybackError = currentError
                        Toaster.e(
                            if (currentDisplayedMediaItem.isLocal)
                                localMusicFileNotFoundError
                            else when (currentError.cause?.cause) {
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
                    }
                }
            }
        }
    }
}

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

@Composable
private fun ThumbnailShareDialog(
    mediaItem: MediaItem,
    currentThumbnailUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailUrl by remember(mediaItem.mediaId) { mutableStateOf(currentThumbnailUrl.orEmpty()) }
    var loadingAlternative by remember { mutableStateOf(false) }
    var creatingShare by remember { mutableStateOf(false) }
    var alternatives by remember(mediaItem.mediaId) { mutableStateOf<List<OmadaSearchResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
    val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
    val videoId = mediaItem.mediaId.toYoutubeVideoId()
    val shareLink = buildThumbnailShareLink(videoId)
    val cubicShareLink = ExternalUris.cubicMusicShare(videoId)
    val shareFailed = stringResource(R.string.thumbnail_share_failed)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette().background1,
        titleContentColor = colorPalette().text,
        textContentColor = colorPalette().textSecondary,
        shape = RoundedCornerShape(22.dp),
        title = { Text(stringResource(R.string.thumbnail_share_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colorPalette().background2.copy(alpha = 0.96f),
                                    colorPalette().background1.copy(alpha = 0.86f)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ImageCacheFactory.Thumbnail(
                            thumbnailUrl = thumbnailUrl.ifBlank { currentThumbnailUrl },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title.ifBlank { stringResource(R.string.thumbnail_share_app_name) }, color = colorPalette().text, maxLines = 2)
                                Text(artist, color = colorPalette().textSecondary, maxLines = 1)
                            }
                        }
                    }
                }
                if (alternatives.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.thumbnail_share_pick_art),
                        color = colorPalette().text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(alternatives) { result ->
                            val candidate = result.thumbnailUrl.orEmpty()
                            if (candidate.isNotBlank()) {
                                ImageCacheFactory.Thumbnail(
                                    thumbnailUrl = candidate,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colorPalette().background2)
                                        .clickable { thumbnailUrl = candidate }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                ThumbnailShareAction(
                    text = if (creatingShare) stringResource(R.string.thumbnail_share_creating) else stringResource(R.string.thumbnail_share_card_with_link_action),
                    enabled = !creatingShare,
                    accent = true,
                    onClick = {
                        scope.launch {
                            creatingShare = true
                            shareThumbnailCard(
                                context = context,
                                mediaId = videoId,
                                title = title,
                                artist = artist,
                                artworkUrl = thumbnailUrl.ifBlank { currentThumbnailUrl },
                                includeSongLink = true
                            ).onFailure { Toaster.e(it.message ?: shareFailed) }
                            creatingShare = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                ThumbnailShareAction(
                    text = stringResource(R.string.thumbnail_share_card_action),
                    enabled = !creatingShare,
                    onClick = {
                        scope.launch {
                            creatingShare = true
                            shareThumbnailCard(
                                context = context,
                                mediaId = videoId,
                                title = title,
                                artist = artist,
                                artworkUrl = thumbnailUrl.ifBlank { currentThumbnailUrl },
                                includeSongLink = false
                            ).onFailure { Toaster.e(it.message ?: shareFailed) }
                            creatingShare = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                ThumbnailShareAction(
                    text = stringResource(R.string.thumbnail_share_text_action),
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "$title\n$artist\n$shareLink")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.thumbnail_share_chooser)))
                    }
                )
                Spacer(Modifier.height(8.dp))
                ThumbnailShareAction(
                    text = stringResource(R.string.thumbnail_share_cubic_link_action),
                    enabled = !creatingShare,
                    onClick = {
                        scope.launch {
                            creatingShare = true
                            shareThumbnailCard(
                                context = context,
                                mediaId = videoId,
                                title = title,
                                artist = artist,
                                artworkUrl = thumbnailUrl.ifBlank { currentThumbnailUrl },
                                includeSongLink = true,
                                songLinkOverride = cubicShareLink
                            ).onFailure { Toaster.e(it.message ?: shareFailed) }
                            creatingShare = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                ThumbnailShareAction(
                    text = if (loadingAlternative) stringResource(R.string.thumbnail_share_finding) else stringResource(R.string.thumbnail_share_try_omada),
                    enabled = !loadingAlternative,
                    onClick = {
                        scope.launch {
                            loadingAlternative = true
                            val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
                            val found = OmadaSearchClient.search(query, type = "video")
                                .getOrNull()
                                .orEmpty()
                                .filter { it.thumbnailUrl?.isNotBlank() == true }
                                .distinctBy { it.thumbnailUrl }
                                .take(8)
                            alternatives = found
                            thumbnailUrl = found.firstOrNull()?.thumbnailUrl.orEmpty().ifBlank { thumbnailUrl }
                            loadingAlternative = false
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        dismissButton = {}
    )
}

@Composable
private fun ThumbnailShareAction(
    text: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (accent) colorPalette().accent else colorPalette().background2
    val fg = if (accent) colorPalette().onAccent else colorPalette().text
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun String.toYoutubeVideoId(): String =
    removePrefix("e:")
        .substringAfterLast("watch?v=")
        .substringAfterLast("/")
        .substringBefore("&")
        .substringBefore("?")

private fun String.isYoutubeVideoId(): Boolean =
    matches(Regex("^[A-Za-z0-9_-]{11}$"))

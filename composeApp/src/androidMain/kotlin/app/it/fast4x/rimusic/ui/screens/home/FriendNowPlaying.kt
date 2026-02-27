package app.it.fast4x.rimusic.ui.screens.home

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import app.kreate.android.R
import androidx.navigation.NavController
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.models.Song
import kotlinx.serialization.json.Json

// ===== IMPORT THE CUBIC JAM HELPER FUNCTIONS =====
import app.it.fast4x.rimusic.ui.screens.cubicjam.refreshFriendsActivity

// ===== DATA CLASSES =====
data class FriendNowPlaying(
    val friendName: String,
    val trackTitle: String,
    val artistName: String,
    val albumArtUrl: String?,
    val isPlaying: Boolean = true,
    val id: String = "",
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val isOnline: Boolean = true
)

data class FriendStatus(
    val friendName: String,
    val isOnline: Boolean,
    val isPlaying: Boolean,
    val lastActivity: FriendNowPlaying? = null
)

data class NowPlayingState(
    val friendNowPlaying: FriendNowPlaying? = null,
    val friendStatus: FriendStatus? = null,
    val isDismissed: Boolean = false,
    val swipeOffset: Float = 0f,
    val isLoading: Boolean = false,
    val lastUpdateTime: Long = 0
)

// ===== HELPER FUNCTIONS =====
@Composable
fun rememberNowPlayingState(): NowPlayingStateHolder {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }
    
    val state = remember { 
        mutableStateOf(NowPlayingState()) 
    }
    
    val isCubicJamLoggedIn = remember {
        val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("bearer_token", null)
        val isLoggedIn = token != null
        
        Timber.d("CubicJam - Is logged in: $isLoggedIn, Token exists: ${token != null}")
        isLoggedIn
    }
    
    // Function to fetch friend activity
    fun fetchFriendNowPlaying() {
        if (!isCubicJamLoggedIn || state.value.isLoading) return
        
        scope.launch {
            try {
                state.value = state.value.copy(isLoading = true)
                
                val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
                val result = refreshFriendsActivity(prefs, json)
                
                Timber.d("CubicJam - Fetch result: ${result != null}")
                
                if (result != null && result.activity.isNotEmpty()) {
                    val currentUsername = prefs.getString("username", null)
                    val currentTime = System.currentTimeMillis()
                    
                    // Find first online friend (playing or not)
                    var activeFriend: app.it.fast4x.rimusic.ui.screens.cubicjam.FriendActivityResponseItem? = null
                    var onlineFriend: app.it.fast4x.rimusic.ui.screens.cubicjam.FriendActivityResponseItem? = null
                    
                    for (activityItem in result.activity) {
                        val username = activityItem.profile.username
                        val isOnline = activityItem.is_online
                        
                        Timber.d("CubicJam - Checking friend: $username, online: $isOnline")
                        
                        // Skip current user
                        if (username == currentUsername) continue
                        
                        // First check for online and playing friend
                        if (isOnline && activityItem.is_playing && activityItem.track_id.isNotEmpty()) {
                            activeFriend = activityItem
                            Timber.d("CubicJam - Found active playing friend: $username")
                            break
                        }
                        
                        // If no playing friend found yet, track any online friend
                        if (isOnline && onlineFriend == null) {
                            onlineFriend = activityItem
                            Timber.d("CubicJam - Found online friend: $username")
                        }
                    }
                    
                    // Priority: 1. Playing friend, 2. Online friend, 3. Nothing
                    val targetFriend = activeFriend ?: onlineFriend
                    
                    if (targetFriend != null) {
                        val displayName = targetFriend.profile.display_name ?: targetFriend.profile.username
                        
                        if (targetFriend.is_playing && targetFriend.track_id.isNotEmpty()) {
                            // Friend is playing
                            val friendNowPlaying = FriendNowPlaying(
                                friendName = displayName,
                                trackTitle = targetFriend.title,
                                artistName = targetFriend.artist,
                                albumArtUrl = targetFriend.artwork_url,
                                isPlaying = targetFriend.is_playing,
                                id = targetFriend.track_id,
                                durationMs = targetFriend.duration_ms,
                                positionMs = targetFriend.position_ms,
                                isOnline = targetFriend.is_online
                            )
                            
                            Timber.d("CubicJam - Created playing friend: ${friendNowPlaying.friendName}")
                            
                            // Smooth update - only update if different
                            val currentFriend = state.value.friendNowPlaying
                            if (currentFriend?.id != friendNowPlaying.id || 
                                currentFriend?.isPlaying != friendNowPlaying.isPlaying ||
                                currentFriend?.isOnline != friendNowPlaying.isOnline) {
                                
                                state.value = NowPlayingState(
                                    friendNowPlaying = friendNowPlaying,
                                    friendStatus = FriendStatus(
                                        friendName = displayName,
                                        isOnline = true,
                                        isPlaying = true,
                                        lastActivity = friendNowPlaying
                                    ),
                                    isDismissed = state.value.isDismissed,
                                    swipeOffset = 0f,
                                    isLoading = false,
                                    lastUpdateTime = currentTime
                                )
                            } else {
                                state.value = state.value.copy(isLoading = false)
                            }
                        } else {
                            // Friend is online but not playing
                            val friendStatus = FriendStatus(
                                friendName = displayName,
                                isOnline = true,
                                isPlaying = false,
                                lastActivity = state.value.friendNowPlaying
                            )
                            
                            Timber.d("CubicJam - Friend online but not playing: $displayName")
                            
                            // Smooth update
                            if (state.value.friendStatus?.friendName != friendStatus.friendName ||
                                state.value.friendStatus?.isPlaying != friendStatus.isPlaying ||
                                state.value.friendStatus?.isOnline != friendStatus.isOnline) {
                                
                                state.value = NowPlayingState(
                                    friendNowPlaying = state.value.friendNowPlaying,
                                    friendStatus = friendStatus,
                                    isDismissed = state.value.isDismissed,
                                    swipeOffset = 0f,
                                    isLoading = false,
                                    lastUpdateTime = currentTime
                                )
                            } else {
                                state.value = state.value.copy(isLoading = false)
                            }
                        }
                    } else {
                        Timber.d("CubicJam - No online friends found")
                        // Keep last state but mark as no online friends
                        state.value = state.value.copy(
                            isLoading = false,
                            lastUpdateTime = currentTime
                        )
                    }
                } else {
                    Timber.d("CubicJam - No activity data in response")
                    state.value = state.value.copy(
                        isLoading = false,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Timber.e("CubicJam - Error fetching friend now playing: ${e.message}")
                state.value = state.value.copy(isLoading = false)
            }
        }
    }
    
    // Start periodic updates every 20 seconds
    LaunchedEffect(isCubicJamLoggedIn) {
        if (isCubicJamLoggedIn) {
            Timber.d("CubicJam - Starting 20-second updates")
            while (true) {
                fetchFriendNowPlaying()
                delay(20000) // Update every 20 seconds
            }
        } else {
            Timber.d("CubicJam - Not logged in, skipping periodic updates")
        }
    }
    
    // Initial fetch
    LaunchedEffect(isCubicJamLoggedIn) {
        if (isCubicJamLoggedIn) {
            Timber.d("CubicJam - Initial fetch")
            fetchFriendNowPlaying()
        }
    }
    
    return NowPlayingStateHolder(
        state = state.value,
        fetchFriendNowPlaying = { fetchFriendNowPlaying() },
        updateState = { newState ->
            state.value = newState
        }
    )
}

data class NowPlayingStateHolder(
    val state: NowPlayingState,
    val fetchFriendNowPlaying: () -> Unit,
    val updateState: (NowPlayingState) -> Unit
)

// ===== ANIMATED COMPONENTS =====

@Composable
private fun GlowingDot(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = modifier
            .size(18.dp)
            .drawWithContent {
                // Outer glow ring
                drawCircle(
                    color = if (isPlaying) Color(0xFF00FF88) else Color(0xFF8888FF),
                    radius = size.minDimension / 2 * pulseScale,
                    alpha = glowAlpha * 0.4f,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Inner circle with gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isPlaying) Color(0xFF00FF88) else Color(0xFF8888FF),
                            if (isPlaying) Color(0xFF008844) else Color(0xFF4444AA)
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.minDimension / 3
                    ),
                    radius = size.minDimension / 3,
                    alpha = if (isPlaying) glowAlpha else 0.8f
                )
            }
    )
}

@Composable
private fun AnimatedVibingProgressBar(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vibing")
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        if (isPlaying) {
            // Animated gradient bar for playing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .drawWithContent {
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00FF88),
                                Color(0xFF0088FF),
                                Color(0xFF8800FF),
                                Color(0xFF00FF88)
                            ),
                            start = Offset(gradientOffset * size.width, 0f),
                            end = Offset(size.width + gradientOffset * size.width, 0f)
                        )
                        drawRect(brush = brush)
                    }
            )
        } else {
            // Static progress bar for paused
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00FF88), Color(0xFF0088FF))
                        )
                    )
            )
        }
    }
}

// ===== MAIN COMPONENT =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendNowPlayingSection(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val nowPlayingStateHolder = rememberNowPlayingState()
    val state = nowPlayingStateHolder.state
    
    // Use derivedStateOf to prevent unnecessary recompositions
    val shouldShow by remember(state) {
        derivedStateOf {
            !state.isLoading && state.friendStatus != null && !state.isDismissed
        }
    }
    
    if (shouldShow) {
        val friendStatus = state.friendStatus!!
        val friendNowPlaying = state.friendNowPlaying
        
        Timber.d("CubicJam - Showing: ${friendStatus.friendName}")
        
        SmoothFriendNowPlaying(
            friendStatus = friendStatus,
            friendNowPlaying = friendNowPlaying,
            onDismiss = {
                nowPlayingStateHolder.updateState(state.copy(isDismissed = true))
            },
            onPlayClick = { friend ->
                binder?.let {
                    val song = Song(
                        id = friend.id,
                        title = friend.trackTitle,
                        artistsText = friend.artistName,
                        durationText = "",
                        thumbnailUrl = friend.albumArtUrl,
                        likedAt = null
                    )
                    it.stopRadio()
                    it.player?.forcePlay(song.asMediaItem)
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun SmoothFriendNowPlaying(
    friendStatus: FriendStatus,
    friendNowPlaying: FriendNowPlaying?,
    onDismiss: () -> Unit,
    onPlayClick: (FriendNowPlaying) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var swipeOffset by remember { mutableStateOf(0f) }
    
    val maxSwipeDistance = 300f
    val dismissThreshold = maxSwipeDistance * 0.6f
    
    // Use BlackCherryCosmos shader for the background
    val selectedShader = remember { BlackCherryCosmos }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset { IntOffset(swipeOffset.roundToInt(), 0) }
            .alpha(1f - (abs(swipeOffset) / dismissThreshold).coerceIn(0f, 1f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(swipeOffset) > dismissThreshold) {
                            onDismiss()
                        } else {
                            swipeOffset = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val newOffset = swipeOffset + dragAmount
                        swipeOffset = newOffset.coerceIn(-maxSwipeDistance, maxSwipeDistance)
                        change.consume()
                    }
                )
            }
    ) {
        // Main container with BlackCherryCosmos shader
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .shaderBackground(selectedShader)
        ) {
            // Dark overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
            
            // Content with proper spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Album Art - Larger thumbnail
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (friendNowPlaying?.albumArtUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(friendNowPlaying.albumArtUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${friendNowPlaying.trackTitle} album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music),
                            contentDescription = "No album art",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Glowing dot in top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        GlowingDot(isPlaying = friendStatus.isPlaying && friendStatus.isOnline)
                    }
                }
                
                // Text Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Friend name and status in one line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Friend name - larger and bolder
                        Text(
                            text = friendStatus.friendName,
                           style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Status badge
                        Text(
                            text = when {
                                friendStatus.isPlaying && friendStatus.isOnline -> "🫠 vibing"
                                friendStatus.isOnline -> "😎 online"
                                else -> "🥺 offline"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    friendStatus.isPlaying && friendStatus.isOnline -> Color(0xFF00FF88)
                                    friendStatus.isOnline -> Color(0xFF8888FF)
                                    else -> Color.White.copy(alpha = 0.6f)
                                }
                            )
                        )
                    }
                    
                    // Song title with marquee effect for long text
                    if (friendStatus.isOnline && friendNowPlaying?.trackTitle?.isNotEmpty() == true) {
                        Text(
                            text = friendNowPlaying.trackTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Artist name
                        Text(
                            text = friendNowPlaying.artistName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (friendNowPlaying != null) {
                        // Friend was recently playing but is now paused/offline
                        Text(
                            text = "Last played: ${friendNowPlaying.trackTitle}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Friend is online but not playing anything recently
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Progress bar - only show if friend is online
                    if (friendStatus.isOnline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (friendNowPlaying?.durationMs ?: 0 > 0) {
                            (friendNowPlaying?.positionMs?.toFloat() ?: 0f) / (friendNowPlaying?.durationMs?.toFloat() ?: 1f)
                        } else 0f
                        
                        AnimatedVibingProgressBar(
                            isPlaying = friendStatus.isPlaying,
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (friendNowPlaying != null && friendStatus.isPlaying) {
                        // Play Button
                        IconButton(
                            onClick = { onPlayClick(friendNowPlaying) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = "Play this song",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // Dismiss Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            // Subtle swipe hint
            if (abs(swipeOffset) < 10f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .alpha(0.4f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_right),
                        contentDescription = "Swipe hint",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
package app.it.fast4x.rimusic.ui.screens.cubicjam

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle
import app.kreate.android.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.shadow

// ===== GLOWING DOT =====
@Composable
fun GlowingDot(isPlaying: Boolean, isOnline: Boolean, modifier: Modifier = Modifier) {
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
            .size(14.dp)
            .drawWithContent {
                val dotColor = when {
                    isPlaying && isOnline -> Color(0xFF00FF88)
                    isOnline -> Color(0xFF8888FF)
                    else -> Color(0xFF757575) // Gray for offline
                }
                
                // Outer glow ring
                drawCircle(
                    color = dotColor,
                    radius = size.minDimension / 2 * pulseScale,
                    alpha = if (isOnline) glowAlpha * 0.4f else 0.3f,
                    style = StrokeStyle(width = 2.dp.toPx())
                )
                
                // Inner circle with gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            dotColor,
                            dotColor.copy(alpha = 0.7f)
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.minDimension / 3
                    ),
                    radius = size.minDimension / 3,
                    alpha = if (isPlaying && isOnline) glowAlpha else 0.8f
                )
            }
    )
}

// ===== ANIMATED PROGRESS BAR (SAME AS HOME) =====
@Composable
fun AnimatedVibingProgressBar(
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

@Composable
fun UserHeaderSection(
    displayName: String?,
    username: String?,
    friendCode: String?,
    avatarUrl: String?,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCopyCode: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D1B69), // Deep purple
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with purple gradient border
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A103F))
                        .border(
                            2.dp,
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9D4EDD), // Bright purple
                                    Color(0xFF5A189A)  // Deep purple
                                )
                            ),
                            CircleShape
                        )
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF9D4EDD),
                                            Color(0xFF5A189A)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            displayName?.let { name ->
                                Text(
                                    text = name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            } ?: run {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    username?.let { name ->
                        Text(
                            text = "@$name",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFD0BCFF) // Light lavender
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Logout button with purple accent
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF5A189A), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.logout),
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Friend code section with purple gradient
            friendCode?.let { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A103F))
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF9D4EDD),
                                    Color(0xFF5A189A)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onCopyCode)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.qrcode),
                                contentDescription = "MY Code",
                                tint = Color(0xFFD0BCFF), // Light lavender
                                modifier = Modifier.size(20.dp)
                            )

                            Column {
                                Text(
                                    text = "MY CODE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFD0BCFF).copy(alpha = 0.8f)
                                    )
                                )
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onCopyCode,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF5A189A), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.copy),
                                contentDescription = "Copy",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View profile button with purple gradient
            Button(
                onClick = onProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5A189A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.globe),
                    contentDescription = "View Profile",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Profile",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// ===== STYLISH STATS CARDS WITH PURPLE THEME =====
@Composable
fun StatsSection(
    onlineFriends: Int,
    totalFriends: Int,
    listeningFriends: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            value = onlineFriends.toString(),
            label = "Online",
            icon = R.drawable.wifi,
            color = Color(0xFF00FF88), // Green
            backgroundColor = Color(0xFF1A103F),
            borderColor = Color(0xFF00FF88).copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )

        StatsCard(
            value = listeningFriends.toString(),
            label = "Listening",
            icon = R.drawable.music,
            color = Color(0xFF9D4EDD), // Purple
            backgroundColor = Color(0xFF1A103F),
            borderColor = Color(0xFF9D4EDD).copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )

        StatsCard(
            value = totalFriends.toString(),
            label = "Total",
            icon = R.drawable.people,
            color = Color(0xFF5A189A), // Deep purple
            backgroundColor = Color(0xFF1A103F),
            borderColor = Color(0xFF5A189A).copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatsCard(
    value: String,
    label: String,
    icon: Int,
    color: Color,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon and value row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = color
                    )
                )
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFFD0BCFF) // Light lavender
                )
            )
        }
    }
}

@Composable
fun SimpleStatsCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(14.dp)) // subtle shadow
            .border(
                BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cubicJamSurfaceVariant.copy(alpha = 0.8f), // slightly transparent
            contentColor = cubicJamOnSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.2f), cubicJamSurfaceVariant.copy(alpha = 0.6f))
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = color
                )
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = cubicJamOnSurfaceVariant.copy(alpha = 0.9f)
                )
            )
        }
    }
}

// ===== FRIEND CARD - BLACK CHERRY COSMOS ONLY FOR ONLINE =====
@Composable
fun FriendCard(
    friend: FriendActivity,
    onClick: () -> Unit,
    isOnline: Boolean
) {
    if (isOnline) {
        // ONLINE FRIEND: With BlackCherryCosmos shader
        OnlineFriendCard(friend = friend, onClick = onClick)
    } else {
        // OFFLINE FRIEND: Normal card
        OfflineFriendCard(friend = friend, onClick = onClick)
    }
}

// ===== ONLINE FRIEND CARD WITH BLACK CHERRY COSMOS =====
@Composable
private fun OnlineFriendCard(
    friend: FriendActivity,
    onClick: () -> Unit
) {
    val selectedShader = remember { BlackCherryCosmos }
    var swipeOffset by remember { mutableStateOf(0f) }
    val maxSwipeDistance = 300f
    val dismissThreshold = maxSwipeDistance * 0.6f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .offset { IntOffset(swipeOffset.roundToInt(), 0) }
            .alpha(1f - (abs(swipeOffset) / dismissThreshold).coerceIn(0f, 1f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(swipeOffset) > dismissThreshold) {
                            swipeOffset = 0f
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .shaderBackground(selectedShader)
                .clickable(onClick = onClick)
        ) {
            // Dark overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Album art - Same as home screen
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (friend.activity?.artwork_url != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(friend.activity.artwork_url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Album Art",
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
                    
                    // Avatar overlay
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (friend.profile.avatar_url != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(friend.profile.avatar_url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = friend.profile.username.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    // Glowing dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    ) {
                        GlowingDot(
                            isPlaying = friend.activity?.is_playing == true,
                            isOnline = true
                        )
                    }
                }
                
                // Friend info - EXACTLY LIKE HOME SCREEN
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Friend name and status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = friend.profile.display_name ?: friend.profile.username,
                             style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = when {
                                friend.activity?.is_playing == true -> "🫠 vibing"
                                else -> "😎 online"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    friend.activity?.is_playing == true -> Color(0xFF00FF88)
                                    else -> Color(0xFF8888FF)
                                }
                            )
                        )
                    }
                    
                    // NOW PLAYING - CORE CONTENT
                    friend.activity?.let { activity ->
                        if (activity.title.isNotEmpty()) {
                            // Song title with marquee
                            Text(
                                text = activity.title,
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
                                text = activity.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Progress bar for playing tracks
                            if (activity.is_playing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val progress = if (activity.duration_ms != null && activity.duration_ms > 0) {
                                    (activity.position_ms?.toFloat() ?: 0f) / activity.duration_ms.toFloat()
                                } else 0f
                                
                                AnimatedVibingProgressBar(
                                    isPlaying = activity.is_playing,
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = "Not playing",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    } ?: run {
                        Text(
                            text = "No recent activity",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // View button
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_right),
                        contentDescription = "View Profile",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ===== OFFLINE FRIEND CARD - NORMAL CARD =====
@Composable
private fun OfflineFriendCard(
    friend: FriendActivity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cubicJamSurface,
            contentColor = cubicJamOnSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cubicJamSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (friend.activity?.artwork_url != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(friend.activity.artwork_url)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.music),
                        contentDescription = "No Album Art",
                        tint = cubicJamPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Avatar overlay
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(cubicJamSurface)
                        .border(1.dp, cubicJamSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (friend.profile.avatar_url != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(friend.profile.avatar_url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = friend.profile.username.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = cubicJamPrimary
                            )
                        )
                    }
                }
                
                // Glowing dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    GlowingDot(
                        isPlaying = friend.activity?.is_playing == true,
                        isOnline = false
                    )
                }
            }
            
            // Friend info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Friend name and status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = friend.profile.display_name ?: friend.profile.username,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = cubicJamOnSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = cubicJamOnSurfaceVariant
                        )
                    )
                }
                
                // Last activity or message
                friend.activity?.let { activity ->
                    if (activity.title.isNotEmpty()) {
                        Text(
                            text = "Last played: ${activity.title}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = cubicJamOnSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        activity.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = cubicJamOnSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "No recent activity",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = cubicJamOnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                } ?: run {
                    Text(
                        text = "No recent activity",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = cubicJamOnSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
                
                // Last seen time
                friend.activity?.updated_at?.let { updatedAt ->
                    val formattedTime = remember(updatedAt) {
                        try {
                            val instant = Instant.parse(updatedAt)
                            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                            val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                            dateTime.format(formatter)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    formattedTime?.let {
                        Text(
                            text = "Last seen: $it",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = cubicJamOnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            // Simple view button
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = "View Profile",
                    tint = cubicJamOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ===== SECTION HEADER - NORMAL (NO SHADER) =====
@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = cubicJamPrimary,
                modifier = Modifier.size(20.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = cubicJamOnSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = cubicJamOnSurfaceVariant
                    )
                )
            }
        }
    }
}

// ===== LOADING SECTION =====
@Composable
fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = cubicJamPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = "Loading friends...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = cubicJamOnSurfaceVariant
                )
            )
        }
    }
}

// ===== EMPTY FRIENDS SECTION - NORMAL (NO SHADER) =====
@Composable
fun EmptyFriendsSection(
    onAddFriendClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.people),
            contentDescription = "No Friends",
            tint = cubicJamPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No Friends Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = cubicJamOnSurface
                )
            )
            
            Text(
                text = "Add friends to see what they're listening to",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = cubicJamOnSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Button(
            onClick = onAddFriendClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cubicJamPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = "Add Friend",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Friend")
        }
    }
}

// ===== ERROR CARD =====
@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cubicJamError.copy(alpha = 0.1f),
            contentColor = cubicJamError
        ),
        border = BorderStroke(1.dp, cubicJamError.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.alert_circle),
                contentDescription = "Error",
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
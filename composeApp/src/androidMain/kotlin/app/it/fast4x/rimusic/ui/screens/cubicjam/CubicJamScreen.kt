package app.it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import app.kreate.android.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos

// ─── Palette ──────────────────────────────────────────────────────────────────
private val CJDeepVoid      = Color(0xFF08080F)
private val CJInk           = Color(0xFF0E0E1A)
private val CJPanel         = Color(0xFF141424)
private val CJPanelBright   = Color(0xFF1C1C30)
private val CJLavender      = Color(0xFF9D6FFF)
private val CJElectric      = Color(0xFF6E3FFF)
private val CJMint          = Color(0xFF00E5A0)
private val CJCoral         = Color(0xFFFF5F6D)
private val CJGold          = Color(0xFFFFD166)
private val CJWhite         = Color(0xFFF0EEF8)
private val CJMuted         = Color(0xFF7C7A9A)
private val CJDivider       = Color(0xFF252540)

// ─── Animated aurora background ───────────────────────────────────────────────
@Composable
private fun AuroraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "shift"
    )
    Box(
        modifier = modifier.drawBehind {
            val w = size.width; val h = size.height
            // Base
            drawRect(CJDeepVoid)
            // Blob 1 – lavender top-left
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CJElectric.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w * (0.15f + shift * 0.12f), h * 0.18f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * (0.15f + shift * 0.12f), h * 0.18f)
            )
            // Blob 2 – mint bottom-right
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CJMint.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * (0.85f - shift * 0.1f), h * 0.72f),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(w * (0.85f - shift * 0.1f), h * 0.72f)
            )
            // Blob 3 – coral tiny accent top-right
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CJCoral.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.08f),
                    radius = w * 0.3f
                ),
                radius = w * 0.3f,
                center = Offset(w * 0.88f, h * 0.08f)
            )
        }
    )
}

// ─── Noise overlay ────────────────────────────────────────────────────────────
private fun DrawScope.drawNoise(alpha: Float = 0.03f) {
    val step = 3f
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            val v = ((x * 1234 + y * 5678).toLong() and 0xFF).toFloat() / 255f
            drawRect(
                color = Color.White.copy(alpha = alpha * v),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(step, step)
            )
            x += step
        }
        y += step
    }
}

// ─── Pulsing ring component ───────────────────────────────────────────────────
@Composable
private fun PulsingRing(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringAlpha"
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .drawBehind {
                drawCircle(color = color, radius = size.minDimension / 2 * scale, alpha = ringAlpha)
                drawCircle(color = color, radius = size.minDimension / 2)
            }
    )
}

// ─── Waveform bars ────────────────────────────────────────────────────────────
@Composable
private fun WaveformBars(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val heights = (0 until 5).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(400 + i * 120, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

// ─── Stat pill ────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(
    value: String,
    label: String,
    color: Color,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CJPanel)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = color
            )
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = CJMuted,
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )
        )
    }
}

// ─── User header ──────────────────────────────────────────────────────────────
@Composable
private fun CJUserHeader(
    displayName: String?,
    username: String?,
    friendCode: String?,
    avatarUrl: String?,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Row: avatar + name + logout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .drawBehind {
                        // Gradient ring
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(CJLavender, CJMint, CJElectric, CJLavender)),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    .padding(4.dp)
                    .clip(CircleShape)
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(CJElectric, CJLavender))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (displayName ?: username ?: "?").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = CJWhite
                            )
                        )
                    }
                }
            }

            // Name block
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName ?: username ?: "Listener",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CJWhite,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${username ?: "unknown"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = CJMuted)
                )
            }

            // Logout
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CJPanel)
                    .border(1.dp, CJDivider, CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.logout),
                    contentDescription = "Logout",
                    tint = CJMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Friend code pill
        friendCode?.let { code ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CJPanel)
                    .border(1.dp, CJDivider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.qrcode),
                    contentDescription = null,
                    tint = CJLavender,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FRIEND CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CJMuted,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CJWhite
                        )
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.copy),
                    contentDescription = "Copy",
                    tint = CJMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Profile button
        Button(
            onClick = onProfileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = CJWhite
            ),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(CJLavender, CJMint))
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(CJLavender.copy(alpha = 0.12f), CJMint.copy(alpha = 0.08f))
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.globe),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "View Profile",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

// ─── Section label ────────────────────────────────────────────────────────────
@Composable
private fun CJSectionLabel(title: String, count: Int, color: Color, icon: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulsingRing(color = color, modifier = Modifier.size(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            )
        )
        Text(
            text = "· $count",
            style = MaterialTheme.typography.labelSmall.copy(color = CJMuted)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// ─── Online friend card ───────────────────────────────────────────────────────
@Composable
private fun CJOnlineFriendCard(friend: FriendActivity, onClick: () -> Unit) {
    val isPlaying = friend.activity?.is_playing == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Shader background for online+playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shaderBackground(BlackCherryCosmos)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(CJPanel, CJPanelBright)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(CJLavender.copy(alpha = 0.5f), CJMint.copy(alpha = 0.3f))),
                        RoundedCornerShape(20.dp)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Artwork + avatar stack
            Box(modifier = Modifier.size(64.dp)) {
                // Album art
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CJInk),
                    contentAlignment = Alignment.Center
                ) {
                    friend.activity?.artwork_url?.let { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Icon(
                        painter = painterResource(R.drawable.music),
                        contentDescription = null,
                        tint = CJLavender.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Mini avatar
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(CJDeepVoid)
                        .border(1.5.dp, CJLavender, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    friend.profile.avatar_url?.let { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Text(
                        text = friend.profile.username.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CJWhite,
                            fontSize = 8.sp
                        )
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Name + status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = friend.profile.display_name ?: friend.profile.username,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CJWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isPlaying) CJMint.copy(alpha = 0.18f)
                                else CJLavender.copy(alpha = 0.18f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "vibing" else "online",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isPlaying) CJMint else CJLavender,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                friend.activity?.let { act ->
                    if (act.title.isNotEmpty()) {
                        Text(
                            text = act.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CJWhite.copy(alpha = 0.9f)
                            ),
                            maxLines = 1,
                            modifier = if (isPlaying) Modifier.basicMarquee() else Modifier,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = act.artist,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CJMuted,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isPlaying) {
                            Spacer(Modifier.height(6.dp))
                            // Waveform + progress
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WaveformBars(color = CJMint)
                                // Progress bar
                                val progress = if ((act.duration_ms ?: 0) > 0)
                                    (act.position_ms?.toFloat() ?: 0f) / act.duration_ms!!.toFloat()
                                else 0f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(CJWhite.copy(alpha = 0.12f))
                                ) {
                                    AnimatedProgressFill(progress = progress)
                                }
                            }
                        }
                    }
                }
            }

            // Arrow
            Icon(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
                tint = CJMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AnimatedProgressFill(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "shimmerPos"
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(animatedProgress)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(CJMint, CJLavender, CJMint),
                        start = Offset(size.width * shimmer, 0f),
                        end = Offset(size.width * (shimmer + 1f), 0f)
                    )
                )
            }
    )
}

// ─── Offline friend card ──────────────────────────────────────────────────────
@Composable
private fun CJOfflineFriendCard(friend: FriendActivity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CJPanel)
            .border(1.dp, CJDivider, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CJPanelBright)
                .border(1.dp, CJDivider, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            friend.profile.avatar_url?.let { url ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Text(
                text = friend.profile.username.take(1).uppercase(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CJMuted
                )
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = friend.profile.display_name ?: friend.profile.username,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = CJWhite.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val lastSong = friend.activity?.title?.takeIf { it.isNotEmpty() }
            if (lastSong != null) {
                Text(
                    text = "Last: $lastSong",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CJMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CJMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Offline dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(CJMuted.copy(alpha = 0.4f))
        )
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun CJEmptyState(onAddFriendClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Decorative ring
        Box(
            modifier = Modifier
                .size(96.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(CJLavender.copy(alpha = 0.6f), CJMint.copy(alpha = 0.4f), CJLavender.copy(alpha = 0.6f))
                        ),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(brush = Brush.radialGradient(
                        listOf(CJLavender.copy(alpha = 0.08f), Color.Transparent)
                    ))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.people),
                contentDescription = null,
                tint = CJLavender.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "No Friends Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CJWhite
                )
            )
            Text(
                text = "Add friends to see what they're listening to in real time",
                style = MaterialTheme.typography.bodySmall.copy(color = CJMuted),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Button(
            onClick = onAddFriendClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(CJLavender, CJMint))),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 0.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        tint = CJLavender,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Add Friend",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CJLavender
                        )
                    )
                }
            }
        }
    }
}

// ─── Error banner ─────────────────────────────────────────────────────────────
@Composable
private fun CJErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CJCoral.copy(alpha = 0.12f))
            .border(1.dp, CJCoral.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.alert_circle),
            contentDescription = null,
            tint = CJCoral,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = CJCoral),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                tint = CJCoral,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─── Logout dialog ────────────────────────────────────────────────────────────
@Composable
private fun CJLogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Sign Out",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CJWhite
                )
            )
        },
        text = {
            Text(
                "Are you sure you want to sign out of CubicJam?",
                style = MaterialTheme.typography.bodyMedium.copy(color = CJMuted)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = CJCoral)
            ) { Text("Sign Out", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = CJMuted)
            ) { Text("Cancel") }
        },
        containerColor = CJPanel,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─── MAIN SCREEN ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubicJamScreen(
    navController: NavController,
    cubicJamManager: CubicJamManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }

    val preferences = remember {
        context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
    }

    var isLoggedIn by remember { mutableStateOf(preferences.getString("bearer_token", null) != null) }
    var username by remember { mutableStateOf(preferences.getString("username", null)) }
    var displayName by remember { mutableStateOf(preferences.getString("display_name", null)) }
    var friendCode by remember { mutableStateOf(preferences.getString("friend_code", null)) }
    var avatarUrl by remember { mutableStateOf(preferences.getString("avatar_url", null)) }

    var friendsActivity by remember { mutableStateOf<List<FriendActivity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var onlineFriendsCount by remember { mutableStateOf(0) }
    var totalFriendsCount by remember { mutableStateOf(0) }
    var listeningFriendsCount by remember { mutableStateOf(0) }

    suspend fun fetchFriends() {
        if (!isLoggedIn) { errorMessage = "Not logged in"; return }
        try {
            isLoading = true
            errorMessage = null
            val result = refreshFriendsActivity(preferences, json)
            if (result != null && result.success) {
                val activities = result.activity
                onlineFriendsCount = activities.count { it.is_online }
                totalFriendsCount = result.total_friends
                listeningFriendsCount = result.friends_listening

                val friendActivities = activities.map { a ->
                    FriendActivity(
                        profile = a.profile,
                        activity = Activity(
                            track_id = a.track_id, title = a.title, artist = a.artist,
                            album = a.album, artwork_url = a.artwork_url,
                            is_playing = a.is_playing, position_ms = a.position_ms,
                            duration_ms = a.duration_ms, updated_at = a.updated_at
                        ),
                        is_online = a.is_online
                    )
                }

                val filtered = username?.let { u -> friendActivities.filter { it.profile.username != u } } ?: friendActivities
                friendsActivity = filtered.sortedByDescending { f ->
                    (if (f.is_online) 1_000_000L else 0L) +
                        (f.activity?.updated_at?.let {
                            try { Instant.parse(it).toEpochMilli() } catch (e: Exception) { 0L }
                        } ?: 0L)
                }
                errorMessage = if (filtered.isEmpty()) "No friends found. Add some!" else null
            } else {
                errorMessage = "Couldn't load friends. Check your connection."
            }
        } catch (e: Exception) {
            Timber.tag("CubicJam").e(e, "fetchFriends failed")
            errorMessage = "Error: ${e.message}"
            friendsActivity = emptyList()
        } finally {
            isLoading = false
        }
    }

    fun logout() {
        preferences.edit().clear().apply()
        refreshJob?.cancel()
        isLoggedIn = false; username = null; displayName = null; friendCode = null
        friendsActivity = emptyList()
        onlineFriendsCount = 0; totalFriendsCount = 0; listeningFriendsCount = 0
        showLogoutDialog = false
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            fetchFriends()
            refreshJob = scope.launch {
                while (true) {
                    delay(30_000)
                    if (isLoggedIn) fetchFriends() else break
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { refreshJob?.cancel(); refreshJob = null } }

    if (!isLoggedIn) { CubicJamAuth(navController = navController); return }

    Box(modifier = Modifier.fillMaxSize()) {
        // Aurora animated background
        AuroraBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Header ──
            item {
                CJUserHeader(
                    displayName = displayName,
                    username = username,
                    friendCode = friendCode,
                    avatarUrl = avatarUrl,
                    onLogoutClick = { showLogoutDialog = true },
                    onProfileClick = {
                        username?.let { navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/$it") }
                    },
                    context = context
                )
            }

            // ── Stats pills ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(
                        value = onlineFriendsCount.toString(),
                        label = "Online",
                        color = CJMint,
                        icon = R.drawable.wifi,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = listeningFriendsCount.toString(),
                        label = "Vibing",
                        color = CJLavender,
                        icon = R.drawable.music,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = totalFriendsCount.toString(),
                        label = "Friends",
                        color = CJGold,
                        icon = R.drawable.people,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Error ──
            if (errorMessage != null) {
                item {
                    CJErrorBanner(
                        message = errorMessage ?: "",
                        onDismiss = { errorMessage = null }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Loading skeleton ──
            if (isLoading && friendsActivity.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CJLavender,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "Finding your crew…",
                                style = MaterialTheme.typography.bodySmall.copy(color = CJMuted)
                            )
                        }
                    }
                }
            } else if (friendsActivity.isEmpty() && !isLoading) {
                item { CJEmptyState(onAddFriendClick = {
                    navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/feed/")
                }) }
            } else {
                // ── Online friends ──
                val online = friendsActivity.filter { it.is_online }
                if (online.isNotEmpty()) {
                    item {
                        CJSectionLabel(
                            title = "Live Now",
                            count = online.size,
                            color = CJMint,
                            icon = R.drawable.wifi
                        )
                    }
                    items(online) { friend ->
                        CJOnlineFriendCard(
                            friend = friend,
                            onClick = {
                                navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/${friend.profile.username}")
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Offline friends ──
                val offline = friendsActivity.filter { !it.is_online }
                if (offline.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        // Hairline divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(1.dp)
                                .background(CJDivider)
                        )
                        Spacer(Modifier.height(12.dp))
                        CJSectionLabel(
                            title = "Offline",
                            count = offline.size,
                            color = CJMuted,
                            icon = R.drawable.alert_circle
                        )
                    }
                    items(offline) { friend ->
                        CJOfflineFriendCard(
                            friend = friend,
                            onClick = {
                                navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/${friend.profile.username}")
                            }
                        )
                    }
                }
            }
        }

        // ── FAB row ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Refresh
            FloatingActionButton(
                onClick = { scope.launch { fetchFriends() } },
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, CJDivider, CircleShape),
                containerColor = CJPanel,
                contentColor = CJMuted,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CJLavender
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Add friend
            FloatingActionButton(
                onClick = { navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/feed/") },
                modifier = Modifier
                    .height(48.dp)
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(CJElectric, CJLavender)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                        )
                    },
                containerColor = Color.Transparent,
                contentColor = CJWhite,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Add Friend",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Add Friend",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        // ── Logout dialog ──
        if (showLogoutDialog) {
            CJLogoutDialog(
                onConfirm = ::logout,
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}

@Composable
fun rememberFormattedTime(updatedAt: String?): String? {
    return remember(updatedAt) {
        updatedAt?.let {
            try {
                val instant = Instant.parse(it)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                dateTime.format(formatter)
            } catch (e: Exception) { null }
        }
    }
}
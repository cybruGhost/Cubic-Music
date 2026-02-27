package app.it.fast4x.rimusic.ui.screens.rewind.slides

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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.*
import java.time.LocalDate
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.utils.DataStoreUtils
import app.kreate.android.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestOfAllSlide(data: RewindData, onNext: () -> Unit) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val year = LocalDate.now().year
    val scope = rememberCoroutineScope()
    
    var username by remember { mutableStateOf("Music Fan") }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Fluid animation for background
    val infiniteTransition = rememberInfiniteTransition()
    val fluidX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        )
    )
    val fluidY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing)
        )
    )
    
    LaunchedEffect(Unit) {
        scope.launch {
            username = runBlocking {
                DataStoreUtils.getStringBlocking(context, DataStoreUtils.KEY_USERNAME, "Music Fan")
            }
        }
    }
    
    val topSong = data.topSongs.firstOrNull()
    val topArtist = data.topArtists.firstOrNull()
    val topAlbum = data.topAlbums.firstOrNull()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        // Fluid background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f)
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Gradient blobs
            val colors = listOf(
                colorPalette.accent.copy(alpha = 0.15f),
                colorPalette.accent.copy(alpha = 0.1f),
                Color.Transparent
            )
            
            // Large fluid shape 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = colors,
                    center = Offset(
                        x = centerX + width * 0.2f * sin(fluidX),
                        y = centerY + height * 0.1f * cos(fluidY)
                    ),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = Offset(
                    x = centerX + width * 0.2f * sin(fluidX),
                    y = centerY + height * 0.1f * cos(fluidY)
                )
            )
            
            // Large fluid shape 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = colors,
                    center = Offset(
                        x = centerX - width * 0.2f * cos(fluidX * 1.3f),
                        y = centerY - height * 0.1f * sin(fluidY * 1.3f)
                    ),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(
                    x = centerX - width * 0.2f * cos(fluidX * 1.3f),
                    y = centerY - height * 0.1f * sin(fluidY * 1.3f)
                )
            )
        }
        
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Year pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colorPalette.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = year.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = colorPalette.accent,
                            letterSpacing = 4.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Welcome text
                    Text(
                        text = "Hey $username,",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = colorPalette.textSecondary,
                        letterSpacing = 1.sp
                    )
                    
                    Text(
                        text = "Your Rewind is Ready",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorPalette.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Stats cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = "${data.stats.totalPlays}",
                        label = "Total Plays",
                        color = colorPalette.accent,
                        modifier = Modifier.weight(1f),
                        colorPalette = colorPalette
                    )
                    
                    StatCard(
                        value = "${data.totalUniqueSongs}",
                        label = "Unique Songs",
                        color = colorPalette.accent.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        colorPalette = colorPalette
                    )
                }
            }
            
            // Tab selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TabButton(
                        text = "Highlights",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        color = colorPalette.accent,
                        colorPalette = colorPalette
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    TabButton(
                        text = "Share",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        color = colorPalette.accent,
                        colorPalette = colorPalette
                    )
                }
            }
            
            // Content based on selected tab
            if (selectedTab == 0) {
                // Top items
                item {
                    TopItemCard(
                        rank = 1,
                        title = "TOP SONG",
                        mainText = topSong?.song?.title ?: "No data",
                        subText = topSong?.song?.artistsText ?: "",
                        stats = "${topSong?.playCount ?: 0} plays",
                        minutes = (topSong?.minutes ?: 0).toLong(),  // Fixed: Convert Int to Long
                        color = colorPalette.accent,
                        icon = "🎵",
                        colorPalette = colorPalette
                    )
                }
                
                item {
                    TopItemCard(
                        rank = 1,
                        title = "TOP ARTIST",
                        mainText = topArtist?.artist?.name ?: "No data",
                        subText = "${topArtist?.songCount ?: 0} songs",
                        stats = "${topArtist?.minutes ?: 0} minutes",
                        minutes = (topArtist?.minutes ?: 0).toLong(),  // Fixed: Convert Int to Long
                        color = colorPalette.accent,
                        icon = "⭐",
                        colorPalette = colorPalette
                    )
                }
                
                item {
                    TopItemCard(
                        rank = 1,
                        title = "TOP ALBUM",
                        mainText = topAlbum?.album?.title ?: "No data",
                        subText = topAlbum?.album?.authorsText ?: "",
                        stats = "${topAlbum?.songCount ?: 0} songs",
                        minutes = (topAlbum?.minutes ?: 0).toLong(),  // Fixed: Convert Int to Long
                        color = colorPalette.accent,
                        icon = "💿",
                        colorPalette = colorPalette
                    )
                }
                
                // Monthly breakdown
                if (data.monthlyStats.isNotEmpty()) {
                    item {
                        MonthlyMiniChart(
                            monthlyStats = data.monthlyStats.take(6),
                            accentColor = colorPalette.accent,
                            colorPalette = colorPalette
                        )
                    }
                }
            } else {
                // Share tab
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette.background2.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(  // Fixed: BorderStroke is now properly referenced
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    colorPalette.accent.copy(alpha = 0.5f),
                                    colorPalette.accent.copy(alpha = 0.2f),
                                    colorPalette.accent.copy(alpha = 0.5f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Share icon with animation
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                colorPalette.accent,
                                                colorPalette.accent.copy(alpha = 0.5f),
                                                Color.Transparent
                                            ),
                                            radius = 150f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 48.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "Share Your Story",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorPalette.text
                            )
                            
                            Text(
                                text = "Show the world your music journey",
                                fontSize = 14.sp,
                                color = colorPalette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                            )
                            
                            // Share buttons
                            ShareButton(
                                text = "Web Visualization",
                                description = "Interactive analytics",
                                icon = "🌐",
                                color = colorPalette.accent,
                                onClick = {
                                    val dataString = encodeRewindDataForUrl(data, username, year)
                                    val url = "https://cubicrewind.lovable.app?data=$dataString"
                                    uriHandler.openUri(url)
                                },
                                colorPalette = colorPalette
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ShareButton(
                                text = "Share Summary",
                                description = "Text format",
                                icon = "📱",
                                color = colorPalette.accent.copy(alpha = 0.8f),
                                onClick = {
                                    shareRewindData(
                                        context = context,
                                        data = data,
                                        username = username,
                                        ranking = "Top Listener",
                                        year = year
                                    )
                                },
                                colorPalette = colorPalette
                            )
                        }
                    }
                }
                
                // Stats preview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette.background2.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Quick Stats",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorPalette.textSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            StatRow(
                                label = "Days with music",
                                value = "${data.daysWithMusic}",
                                color = colorPalette.accent,
                                colorPalette = colorPalette
                            )
                            
                            StatRow(
                                label = "Average daily",
                                value = "${data.stats.averageDailyMinutes.toInt()} min",
                                color = colorPalette.accent.copy(alpha = 0.8f),
                                colorPalette = colorPalette
                            )
                            
                            StatRow(
                                label = "Unique artists",
                                value = "${data.totalUniqueArtists}",
                                color = colorPalette.accent.copy(alpha = 0.6f),
                                colorPalette = colorPalette
                            )
                        }
                    }
                }
            }
            
            // Continue button
            item {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorPalette.accent,
                        contentColor = colorPalette.onAccent
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Text(
                        text = "Continue →",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Footer
            item {
                Text(
                    text = "Your data stays on your device",
                    fontSize = 11.sp,
                    color = colorPalette.textDisabled,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))  // Fixed: BorderStroke is now properly referenced
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (selected) colorPalette.onAccent else color,  // Fixed: colorPalette is now accessible
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TopItemCard(
    rank: Int,
    title: String,
    mainText: String,
    subText: String,
    stats: String,
    minutes: Long,  // Fixed: Changed from Int to Long
    color: Color,
    icon: String,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    val hours = (minutes / 60).toInt()  // Fixed: Convert Long to Int for hours
    val remainingMinutes = (minutes % 60).toInt()  // Fixed: Convert Long to Int for minutes
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))  // Fixed: BorderStroke is now properly referenced
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorPalette.onAccent  // Fixed: colorPalette is now accessible
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = mainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorPalette.text,  // Fixed: colorPalette is now accessible
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (subText.isNotEmpty() && subText != "null") {
                    Text(
                        text = subText,
                        fontSize = 14.sp,
                        color = colorPalette.textSecondary,  // Fixed: colorPalette is now accessible
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stats,
                        fontSize = 13.sp,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (minutes > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp)
                        )
                        
                        Text(
                            text = if (hours > 0) "${hours}h ${remainingMinutes}m" else "${remainingMinutes}m",
                            fontSize = 13.sp,
                            color = color.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Icon
            Text(
                text = icon,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun MonthlyMiniChart(
    monthlyStats: List<MonthlyStat>,
    accentColor: Color,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    val maxMinutes = monthlyStats.maxOfOrNull { it.minutes } ?: 1
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))  // Fixed: BorderStroke is now properly referenced
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Monthly Activity",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                monthlyStats.forEach { stat ->
                   val height = if (maxMinutes > 0) {
                        ((stat.minutes.toFloat() / maxMinutes.toFloat()) * 80f).dp
                    } else {
                        0.dp
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(height.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stat.month.take(3),
                            fontSize = 11.sp,
                            color = accentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareButton(
    text: String,
    description: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))  // Fixed: BorderStroke is now properly referenced
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorPalette.text  // Fixed: colorPalette is now accessible
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colorPalette.textSecondary  // Fixed: colorPalette is now accessible
                )
            }
            
            // Arrow
            Text(
                text = "→",
                fontSize = 20.sp,
                color = color
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: Color,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette  // Fixed: Added colorPalette parameter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = colorPalette.textSecondary  // Fixed: colorPalette is now accessible
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Add clickable modifier extension
private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(onClick) {
        detectTapGestures(
            onTap = { onClick() }
        )
    }
)

// Keep existing helper functions
private fun encodeRewindDataForUrl(data: RewindData, username: String, year: Int): String {
    return try {
        val jsonString = buildString {
            append("{")
            append("\"username\":\"${escapeJsonString(username)}\",")
            append("\"year\":\"${year}\",")
            append("\"totalPlays\":${data.stats.totalPlays},")
            append("\"totalMinutes\":${data.stats.totalMinutes},")
            append("\"uniqueSongs\":${data.totalUniqueSongs},")
            append("\"uniqueArtists\":${data.totalUniqueArtists},")
            append("\"uniqueAlbums\":${data.totalUniqueAlbums},")
            append("\"uniquePlaylists\":${data.totalUniquePlaylists},")
            append("\"daysWithMusic\":${data.daysWithMusic},")
            append("\"averageDailyMinutes\":${data.stats.averageDailyMinutes},")
            
            append("\"topSongs\":[")
            data.topSongs.take(10).forEachIndexed { index, song ->
                if (index > 0) append(",")
                append("{")
                append("\"title\":\"${escapeJsonString(song.song.title)}\",")
                append("\"artist\":\"${escapeJsonString(song.song.artistsText ?: "")}\",")
                append("\"plays\":${song.playCount},")
                append("\"minutes\":${song.minutes}")
                append("}")
            }
            append("],")
            
            append("\"topArtists\":[")
            data.topArtists.take(10).forEachIndexed { index, artist ->
                if (index > 0) append(",")
                append("{")
                append("\"name\":\"${escapeJsonString(artist.artist.name ?: "")}\",")
                append("\"minutes\":${artist.minutes},")
                append("\"songCount\":${artist.songCount}")
                append("}")
            }
            append("],")
            
            append("\"topAlbums\":[")
            data.topAlbums.take(5).forEachIndexed { index, album ->
                if (index > 0) append(",")
                append("{")
                append("\"title\":\"${escapeJsonString(album.album.title ?: "")}\",")
                append("\"artist\":\"${escapeJsonString(album.album.authorsText ?: "")}\",")
                append("\"minutes\":${album.minutes},")
                append("\"songCount\":${album.songCount}")
                append("}")
            }
            append("],")
            
            append("\"topPlaylists\":[")
            data.topPlaylists.take(5).forEachIndexed { index, playlist ->
                if (index > 0) append(",")
                append("{")
                append("\"name\":\"${escapeJsonString(playlist.playlist.playlist.name)}\",")
                append("\"minutes\":${playlist.minutes},")
                append("\"songCount\":${playlist.songCount}")
                append("}")
            }
            append("],")
            
            data.stats.firstPlayDate?.let {
                append("\"firstPlay\":\"${escapeJsonString(it)}\",")
            }
            data.stats.lastPlayDate?.let {
                append("\"lastPlay\":\"${escapeJsonString(it)}\",")
            }
            if (endsWith(",")) {
                deleteCharAt(length - 1)
            }
            append("}")
        }
        java.net.URLEncoder.encode(jsonString, "UTF-8")
    } catch (e: Exception) {
        ""
    }
}

private fun escapeJsonString(str: String): String {
    return str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

private fun shareRewindData(
    context: android.content.Context,
    data: RewindData,
    username: String,
    ranking: String,
    year: Int
) {
    val shareText = buildString {
        append("🎵 My $year Music Rewind 🎵\n\n")
        append("👤 $username\n")
        append("🏆 $ranking\n\n")
        append("📊 Stats:\n")
        append("• Total Plays: ${data.stats.totalPlays}\n")
        append("• Total Listening: ${data.stats.totalMinutes} minutes\n")
        append("• Unique Songs: ${data.totalUniqueSongs}\n")
        append("• Active Days: ${data.daysWithMusic}\n")
        
        if (data.stats.lastPlayDate != null) {
            append("• Last Played: ${data.stats.lastPlayDate}\n")
        }
        
        append("\n")
        
        if (data.topSongs.isNotEmpty()) {
            append("🔥 Top Songs:\n")
            data.topSongs.take(5).forEachIndexed { index, song ->
                append("${index + 1}. ${song.song.title} - ${song.song.artistsText ?: "Unknown Artist"}\n")
            }
            append("\n")
        }
        
        if (data.topArtists.isNotEmpty()) {
            append("⭐ Top Artists:\n")
            data.topArtists.take(5).forEachIndexed { index, artist ->
                append("${index + 1}. ${artist.artist.name ?: "Unknown Artist"}\n")
            }
        }
        
        append("\n🎶 Generated with Cubic Rewind")
    }
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "My $year Music Rewind")
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share Your Rewind"))
}
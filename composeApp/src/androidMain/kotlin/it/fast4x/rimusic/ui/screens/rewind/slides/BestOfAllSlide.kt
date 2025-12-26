package it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.rimusic.ui.screens.rewind.*
import java.time.LocalDate
import android.content.Intent
import androidx.compose.ui.platform.LocalUriHandler
import java.net.URLEncoder
import it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun BestOfAllSlide(data: RewindData, onNext: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val year = LocalDate.now().year
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()  // Add this line
    
    val topSong = data.topSongs.firstOrNull()
    val topArtist = data.topArtists.firstOrNull()
    val topAlbum = data.topAlbums.firstOrNull()
    var username by remember { mutableStateOf("Music Fan") }
   
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                username = runBlocking {
                    DataStoreUtils.getStringBlocking(context, DataStoreUtils.KEY_USERNAME, "Music Fan")
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // BlackCherryCosmos inspired background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        // Deep purple cosmic background
                        canvas.nativeCanvas.drawColor(Color(0xFF0A0514).toArgb())
                        
                        // Cherry blossom effect (purple/pink gradient)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0).copy(alpha = 0.3f),
                                    Color(0xFF673AB7).copy(alpha = 0.2f),
                                    Color(0xFF311B92).copy(alpha = 0.1f),
                                    Color(0xFF0A0514)
                                ),
                                center = center,
                                radius = this.size.minDimension * 0.9f
                            )
                        )
                        
                        // Cherry blossom "petals" effect
                        repeat(80) {
                            val petalSize = (1..4).random().toFloat()
                            val alpha = (5..25).random() / 100f
                            val color = when ((0..2).random()) {
                                0 -> Color(0xFFE040FB) // Pink
                                1 -> Color(0xFF7C4DFF) // Purple
                                else -> Color(0xFF536DFE) // Blue
                            }
                            
                            drawCircle(
                                color = color.copy(alpha = alpha),
                                radius = petalSize,
                                center = androidx.compose.ui.geometry.Offset(
                                    (0..this.size.width.toInt()).random().toFloat(),
                                    (0..this.size.height.toInt()).random().toFloat()
                                )
                            )
                        }
                    }
                }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "REWIND $year",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "Hi $username",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE040FB),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Your Year in Music",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
                        // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x152A1F3A)
                ),
                border = BorderStroke(1.5.dp, Color(0x30E040FB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE040FB),
                                        Color(0xFF7C4DFF),
                                        Color(0xFF311B92)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 28.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "View Your Complete Rewind",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "See your music journey in a whole new way with interactive visualizations, " +
                               "detailed analytics, and beautiful charts that bring your listening habits to life.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Share Button
            Card(
                onClick = {
                    val dataString = encodeRewindDataForUrl(data, username, year)
                    val url = "https://cubicrewind.lovable.app?data=$dataString"
                    uriHandler.openUri(url)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE040FB),
                            Color(0xFF7C4DFF),
                            Color(0xFF536DFE)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0x20E040FB),
                                    Color(0x107C4DFF),
                                    Color(0x20E040FB)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE040FB))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌌",
                                fontSize = 20.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Share on Web",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Interactive visualizations & analytics",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.6f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↗",
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Top Song
            TopItemCard(
                icon = "🎵",
                title = "TOP SONG",
                mainText = topSong?.song?.title ?: "N/A",
                subText = topSong?.song?.artistsText ?: "",
                stats = "${topSong?.playCount ?: 0} plays • ${topSong?.minutes ?: 0} min",
                color = Color(0xFFE040FB)
            )
            
            // Top Artist
            TopItemCard(
                icon = "⭐",
                title = "TOP ARTIST",
                mainText = topArtist?.artist?.name ?: "N/A",
                subText = "${topArtist?.songCount ?: 0} songs",
                stats = "${topArtist?.minutes ?: 0} minutes",
                color = Color(0xFF7C4DFF)
            )
            
            // Top Album
            TopItemCard(
                icon = "💿",
                title = "TOP ALBUM",
                mainText = topAlbum?.album?.title ?: "N/A",
                subText = topAlbum?.album?.authorsText ?: "",
                stats = "${topAlbum?.songCount ?: 0} songs • ${topAlbum?.minutes ?: 0} min",
                color = Color(0xFF536DFE)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Alternative share button
            Card(
                onClick = {
                    shareRewindData(
                        context = context,
                        data = data,
                        username = username,
                        ranking = "Top Listener",
                        year = year
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x157C4DFF)
                ),
                border = BorderStroke(1.dp, Color(0x30E040FB))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📤",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Share Summary",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Data processed locally • Securely shared",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopItemCard(
    icon: String,
    title: String,
    mainText: String,
    subText: String,
    stats: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x152A1F3A)
        ),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = 0.4f),
                                color.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 26.sp
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Text(
                    text = mainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stats,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

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
            if (toString().endsWith(",")) {
                deleteCharAt(length - 1)
            }
            append("}")
        }
        URLEncoder.encode(jsonString, "UTF-8")
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
        append("🎤 Username: $username\n")
        append("🏆 Ranking: $ranking\n\n")
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
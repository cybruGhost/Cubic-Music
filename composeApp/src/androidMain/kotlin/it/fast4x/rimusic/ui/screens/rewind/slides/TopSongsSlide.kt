package it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mikepenz.hypnoticcanvas.shaderBackground
import it.fast4x.rimusic.ui.screens.rewind.TopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import java.net.URLEncoder


@Composable
fun TopSongsSlide(
    songs: List<TopSong>,
    onNext: () -> Unit
) {
    // Using InkFlow shader background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(com.mikepenz.hypnoticcanvas.shaders.InkFlow)
    ) {
        // Dark overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with better styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main title with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0).copy(alpha = 0.4f),
                                    Color(0xFF673AB7).copy(alpha = 0.4f),
                                    Color(0xFF3F51B5).copy(alpha = 0.4f)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎵 TOP 10 SONGS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                        
                        Text(
                            text = "Your most played tracks of the year",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Show message if no songs
            if (songs.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.25f),
                                CircleShape
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎵",
                        fontSize = 40.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No songs data available",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Start listening to see your stats!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            } else {
                // Top 10 Songs List
                val displayedSongs = if (songs.size > 10) songs.take(10) else songs
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top 3 with golden medals
                    displayedSongs.take(3).forEachIndexed { index, song ->
                        TopSongItem(
                            rank = index + 1,
                            song = song,
                            modifier = Modifier.fillMaxWidth(),
                            isTop3 = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Songs 4-10
                    displayedSongs.drop(3).forEachIndexed { index, song ->
                        TopSongItem(
                            rank = index + 4,
                            song = song,
                            modifier = Modifier.fillMaxWidth(),
                            isTop3 = false
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Subtle navigation hint
            Column(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "➤",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "swipe for artists",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun TopSongItem(
    rank: Int,
    song: TopSong,
    modifier: Modifier = Modifier,
    isTop3: Boolean = false
) {
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    
    // Get colors based on rank
    val rankColors = when (rank) {
        1 -> Pair(Color(0xFFFFD700), Color(0xFFFFC107)) // Gold
        2 -> Pair(Color(0xFFC0C0C0), Color(0xFF9E9E9E)) // Silver
        3 -> Pair(Color(0xFFCD7F32), Color(0xFF8D6E63)) // Bronze
        in 4..6 -> Pair(Color(0xFF9C27B0), Color(0xFF7B1FA2)) // Purple
        else -> Pair(Color(0xFF2196F3), Color(0xFF1976D2)) // Blue
    }
    
    // Fetch thumbnail
    LaunchedEffect(song.song.title, song.song.artistsText) {
        if (song.song.title.isNotEmpty() && song.song.artistsText != null) {
            val fetchedUrl = fetchSongThumbnail(song.song.title, song.song.artistsText!!)
            thumbnailUrl = fetchedUrl
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54) // Solid dark purple, not transparent
        ),
        border = BorderStroke(
            1.5.dp,
            rankColors.first.copy(alpha = if (isTop3) 0.8f else 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTop3) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rank badge with gradient
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rankColors.first,
                                rankColors.second
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Color.White.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (rank <= 3) when (rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "$rank"
                    } else "$rank",
                    fontSize = if (rank <= 3) 20.sp else 18.sp,
                    color = if (rank <= 3) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Song thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A1B3D),
                                Color(0xFF1A1030)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(
                    url = thumbnailUrl,
                    contentDescription = "${song.song.title} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.song.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = song.song.artistsText ?: "Unknown Artist",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rankColors.first.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${song.playCount}",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (song.minutes > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.minutes} min",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "plays",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3D2E54),
                        Color(0xFF2A1F3A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Fallback emoji
            Text(
                text = "🎵",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// Function to fetch song thumbnail from API
suspend fun fetchSongThumbnail(songTitle: String, artist: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$songTitle $artist", "UTF-8")
            val url = "https://yt.omada.cafe/api/v1/search?q=$query&type=video"
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000 // Reduced timeout for better performance
            connection.readTimeout = 3000
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            
            // Parse JSON response to find video thumbnail
            val thumbnailMatch = Regex("\"videoThumbnails\":\\s*\\[.*?\"url\":\"(.*?)\"").find(response)
            thumbnailMatch?.groups?.get(1)?.value?.let { thumbnailUrl ->
                val cleanedUrl = thumbnailUrl.replace("\\/", "/")
                return@withContext if (cleanedUrl.startsWith("//")) {
                    "https:$cleanedUrl"
                } else if (!cleanedUrl.startsWith("http")) {
                    "https://$cleanedUrl"
                } else {
                    cleanedUrl
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
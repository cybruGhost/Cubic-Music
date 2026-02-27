package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import app.it.fast4x.rimusic.ui.screens.rewind.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import com.mikepenz.hypnoticcanvas.shaderBackground
import androidx.compose.foundation.border
import java.time.LocalDate

@Composable
fun TopArtistsSlide(artists: List<TopArtist>, onNext: () -> Unit) {
    // 只取前10个艺术家
    val topTenArtists = artists.take(10)
    val currentYear = LocalDate.now().year
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(OilFlow)
    ) {
        // 深色覆盖层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 专业标题区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 装饰性徽章
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0),
                                    Color(0xFF7B1FA2),
                                    Color(0xFF4A148C)
                                )
                            )
                        )
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFFE040FB)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♫",
                        fontSize = 32.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 专业标题
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "YOUR TOP ARTISTS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                    
                    Text(
                        text = "$currentYear • Music Rewind",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 副标题
                Text(
                    text = "Based on your listening history",
                    fontSize = 12.sp,
                    color = Color(0xFFB39DDB),
                    fontWeight = FontWeight.Normal,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            if (topTenArtists.isNotEmpty()) {
                // 艺术家列表
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(topTenArtists) { index, artist ->
                        ArtistListItem(
                            rank = index + 1,
                            artist = artist,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0x403D5AFE))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 36.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No Artist Data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Your listening history will appear here",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            
            // 导航提示 - 专业设计
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Swipe for Top Albums",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun ArtistListItem(rank: Int, artist: TopArtist, modifier: Modifier = Modifier) {
    var highQualityThumbnailUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(artist) {
        highQualityThumbnailUrl = fetchHighQualityArtistThumbnail(
            artist.artist.name ?: "",
            rank <= 3 // 为前三名获取更高质量的图片
        )
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x809C27B0).copy(alpha = 0.85f) // 更深的紫色
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 排名徽章
            RankBadge(rank = rank)
            
            // 高质量艺术家头像
            ArtistAvatar(
                thumbnailUrl = highQualityThumbnailUrl,
                artistName = artist.artist.name ?: "Unknown Artist"
            )
            
            // 艺术家信息
            ArtistInfo(
                artistName = artist.artist.name ?: "Unknown Artist",
                songCount = artist.songCount.toInt(),
                minutes = artist.minutes.toInt()
            )
            
            // 播放时间
            ListeningTimeBadge(minutes = artist.minutes.toInt())
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val (backgroundColor, textColor) = when (rank) {
        1 -> Pair(Color(0xFFFFD700), Color.Black) // 金色
        2 -> Pair(Color(0xFFC0C0C0), Color.Black) // 银色
        3 -> Pair(Color(0xFFCD7F32), Color.Black) // 铜色
        else -> Pair(Color(0xFF6A1B9A).copy(alpha = 0.9f), Color.White) // 紫色
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .shadow(
                elevation = if (rank <= 3) 6.dp else 3.dp,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$rank"
            },
            fontSize = when (rank) {
                in 1..3 -> 18.sp
                else -> 16.sp
            },
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun ArtistAvatar(thumbnailUrl: String?, artistName: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color(0xFF7C4DFF)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null && thumbnailUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$artistName profile image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF512DA8),
                                        Color(0xFF311B92)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                error = {
                    FallbackAvatar(artistName = artistName)
                }
            )
        } else {
            FallbackAvatar(artistName = artistName)
        }
        
        // 圆形边框
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun FallbackAvatar(artistName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6200EA),
                        Color(0xFF311B92)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = artistName.firstOrNull()?.toString() ?: "🎤",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ArtistInfo(artistName: String, songCount: Int, minutes: Int) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ){
        Text(
            text = artistName,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$songCount ${if (songCount == 1) "song" else "songs"}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            )
            
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
                    .align(Alignment.CenterVertically)
            )
            
            Text(
                text = "${minutes} min",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ListeningTimeBadge(minutes: Int) {
    val hours = minutes / 60
    val displayText = if (hours > 0) "${hours}h" else "${minutes}m"
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x40FFFFFF))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⏱️",
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = displayText,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 获取高质量艺术家缩略图
private suspend fun fetchHighQualityArtistThumbnail(artistName: String, highPriority: Boolean = false): String? {
    if (artistName.isBlank()) return null
    
    return withContext(Dispatchers.IO) {
        try {
            // 优化查询参数
            val query = URLEncoder.encode("$artistName artist music official", "UTF-8")
            val limit = if (highPriority) 5 else 3 // 为前三名获取更多结果
            val url = "https://yt.omada.cafe/api/v1/search?q=$query&type=video&limit=$limit"
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            // 优先查找高质量图片（maxres > sddefault > high > medium）
            val qualityOrder = listOf("maxres", "sddefault", "high", "medium", "default")
            
            for (quality in qualityOrder) {
                val pattern = Regex("\"quality\"\\s*:\\s*\"$quality\"[^}]*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
                val match = pattern.find(response)
                
                if (match != null) {
                    return@withContext match.groups[1]?.value?.replace("\\/", "/")
                }
            }
            
            // 如果没找到特定质量的，找任何缩略图
            val fallbackPattern = Regex("\"videoThumbnails\"\\s*:\\s*\\[[^\\]]*\\{\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            val fallbackMatch = fallbackPattern.find(response)
            
            return@withContext fallbackMatch?.groups?.get(1)?.value?.replace("\\/", "/")
            
        } catch (e: Exception) {
            // 静默失败
            return@withContext null
        }
    }
}
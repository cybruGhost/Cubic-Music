package app.it.fast4x.rimusic.ui.screens.rewind.slides

import android.media.MediaPlayer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.net.URL
import java.net.URLEncoder
import com.mikepenz.hypnoticcanvas.shaderBackground
import java.time.LocalDate
import androidx.compose.foundation.border

@Composable
fun AfterTopArtistsSlide(
    topArtist: TopArtist? = null,  // 只接收 #1 艺术家
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 自动获取艺术家缩略图
    LaunchedEffect(topArtist) {
        if (topArtist != null) {
            isLoading = true
            try {
                thumbnailUrl = fetchHighQualityArtistThumbnail(topArtist.artist.name ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(OilFlow)  // 使用OilFlow背景
    ) {
        // 深色覆盖层增强可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                // 华丽的标题设计
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xA09C27B0),  // 紫色半透明
                                    Color(0xA0673AB7),  // 深紫色半透明
                                    Color(0xA09C27B0)
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "#",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64B5F6),
                                letterSpacing = 0.sp
                            )
                            Text(
                                text = "1",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                letterSpacing = 0.sp
                            )
                        }
                        
                        Text(
                            text = "ARTIST OF THE YEAR",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
            
            if (topArtist != null) {
                // 大型艺术家头像区域
                Box(
                    modifier = Modifier
                        .size(240.dp)  // 更大的头像
                        .shadow(
                            elevation = 32.dp,
                            shape = CircleShape,
                            clip = true
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 外层光环效果
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.3f),
                                        Color(0xFFFFD700).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // 头像容器
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(
                                width = 4.dp,
                                color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                    ) {
                        if (thumbnailUrl != null) {
                            // 使用高质量缩略图
                            SubcomposeAsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "${topArtist.artist.name} thumbnail",
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
                                            modifier = Modifier.size(50.dp),
                                            strokeWidth = 4.dp,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                },
                                error = {
                                    // 错误回退
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF512DA8),
                                                        Color(0xFF311B92)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "👑",
                                            fontSize = 80.sp,
                                            color = Color(0xFFFFD700).copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            )
                        } else if (isLoading) {
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
                                    modifier = Modifier.size(50.dp),
                                    strokeWidth = 4.dp,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        } else {
                            // 默认状态
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF512DA8),
                                                Color(0xFF311B92)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👑",
                                    fontSize = 100.sp,
                                    color = Color(0xFFFFD700).copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 艺术家信息
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = topArtist.artist.name ?: "Unknown Artist",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "YOUR MOST LISTENED ARTIST",
                        fontSize = 14.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 统计卡片 - 紫色半透明
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xA0673AB7)  // 深紫色半透明
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎵 LISTENING STATS",
                            fontSize = 16.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        
                        // 主要统计数据 - 使用正确的属性名
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(
                                emoji = "🔥",
                                value = "${topArtist.songCount}",  // 使用 songCount 而不是 playCount
                                label = "SONGS",
                                color = Color(0xFFFF6B6B)
                            )
                            
                            StatCard(
                                emoji = "🎵",
                                value = "${topArtist.songCount}",
                                label = "SONGS",
                                color = Color(0xFF4FC3F7)
                            )
                            
                            StatCard(
                                emoji = "⏱️",
                                value = "${topArtist.minutes}",
                                label = "MINUTES",
                                color = Color(0xFF81C784)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        // 附加信息
                        val avgPlaysPerDay = if (topArtist.songCount > 0) 
                            String.format("%.2f", topArtist.songCount / 365.0) 
                        else "0.0"
                        
                        val totalHours = (topArtist.minutes / 60f).toInt()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x803D2E54))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📊 INTERESTING FACTS",
                                    fontSize = 13.sp,
                                    color = Color(0xFFE91E63),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                Text(
                                    text = if (totalHours > 24) {
                                        "You listened to ${topArtist.artist.name ?: "this artist"} for over ${totalHours / 24} days!"
                                    } else if (totalHours > 0) {
                                        "That's ${totalHours} hours of pure musical enjoyment!"
                                    } else {
                                        "Your most consistent artist of the year!"
                                    },
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 年度总结
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x809C27B0)  // 紫色半透明
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏆 ${LocalDate.now().year} ACHIEVEMENT",
                            fontSize = 14.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text(
                            text = "Your #1 artist dominated your listening this year. Every ${String.format("%.1f", topArtist.songCount / 365.0)} songs per day were by ${topArtist.artist.name ?: "this artist"}!",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(50.dp))
                
                // 导航提示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.3f),
                                        Color(0xFFFFD700).copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            fontSize = 22.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "swipe for top albums",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }
            } else {
                // 无数据时的回退界面
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "👑",
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        text = "No artist data available",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = value,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// 获取高质量艺术家缩略图
private suspend fun fetchHighQualityArtistThumbnail(artistName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            // 搜索艺术家相关视频，提高匹配准确度
            val query = URLEncoder.encode("$artistName official music video", "UTF-8")
            val url = "https://yt.omada.cafe/api/v1/search?q=$query&type=video"
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            // 首先尝试获取最高质量的图片 (maxres)
            val maxresPattern = Regex("\"quality\"\\s*:\\s*\"maxres\"\\s*,\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            val maxresMatch = maxresPattern.find(response)
            
            if (maxresMatch != null) {
                val url = maxresMatch.groups[1]?.value?.replace("\\/", "/")
                if (url != null) return@withContext url
            }
            
            // 尝试 sddefault (中等质量)
            val sdPattern = Regex("\"quality\"\\s*:\\s*\"sddefault\"\\s*,\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            val sdMatch = sdPattern.find(response)
            
            if (sdMatch != null) {
                val url = sdMatch.groups[1]?.value?.replace("\\/", "/")
                if (url != null) return@withContext url
            }
            
            // 尝试 hqdefault
            val hqPattern = Regex("\"quality\"\\s*:\\s*\"high\"\\s*,\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            val hqMatch = hqPattern.find(response)
            
            if (hqMatch != null) {
                val url = hqMatch.groups[1]?.value?.replace("\\/", "/")
                if (url != null) return@withContext url
            }
            
            // 最后，尝试任何jpg/png图片
            val anyImagePattern = Regex("\"url\"\\s*:\\s*\"(https?://[^\"]+\\.(jpg|png|webp|jpeg))\"", RegexOption.IGNORE_CASE)
            val anyMatch = anyImagePattern.find(response)
            
            anyMatch?.groups?.get(1)?.value?.let { thumbnailUrl ->
                return@withContext thumbnailUrl.replace("\\/", "/")
            }
            
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
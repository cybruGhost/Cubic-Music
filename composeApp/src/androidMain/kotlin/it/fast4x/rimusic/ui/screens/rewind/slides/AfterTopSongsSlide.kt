package it.fast4x.rimusic.ui.screens.rewind.slides

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import it.fast4x.rimusic.ui.screens.rewind.TopSong
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.json.JSONObject
import coil.compose.AsyncImage
import java.net.URL
import java.net.URLEncoder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt

@Composable
fun AfterTopSongsSlide(
    topSong: TopSong? = null,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    
    // 状态管理
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(20.dp)

    
    // 创建并管理 MediaPlayer
    val mediaPlayer = remember { MediaPlayer() }
    
    // 自动获取歌曲信息并播放
    LaunchedEffect(topSong) {
        if (topSong != null) {
            isLoading = true
            try {
                // 先获取缩略图（主要功能）
                val thumbnail = fetchSongThumbnail(topSong.song.title, topSong.song.artistsText ?: "")
                thumbnailUrl = thumbnail
                
                // 然后尝试获取预览（辅助功能，不阻塞UI）
                CoroutineScope(IO).launch {
                    try {
                        val preview = fetchSongPreview(topSong.song.title, topSong.song.artistsText ?: "")
                        previewUrl = preview
                        
                        // 自动播放预览（如果有）
                        preview?.let { url ->
                            withContext(Dispatchers.Main) {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(url)
                                    mediaPlayer.prepareAsync()
                                    mediaPlayer.setOnPreparedListener { player ->
                                        player.start()
                                        // 30秒后自动停止
                                        launch {
                                            delay(30000)
                                            if (player.isPlaying) {
                                                player.pause()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 静默失败，预览不是必需的
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 静默失败，预览不是必需的
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    // 清理 MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }
    
    // 使用简单的黑色背景替代shader以提高性能
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 添加一些渐变效果
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0A0A1A),
                            Color.Black
                        ),
                        radius = 800f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题 - 简化
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "#",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4FC3F7),
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = "1",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 0.sp
                    )
                }
                
                Text(
                    text = "YOUR TOP SONG",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
            
            if (topSong != null) {
                // 大型缩略图 - 使用更简单的加载方式
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .shadow(
                                elevation = 16.dp,
                                shape = thumbShape,
                                clip = false
                            )
                            .clip(thumbShape),
                        contentAlignment = Alignment.Center
                    ){
                    if (thumbnailUrl != null) {
                       // 使用SubcomposeAsyncImage，更好地处理加载状态
                        SubcomposeAsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "${topSong.song.title} thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF1E3A5F),
                                                    Color(0xFF0F1B2D)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎵",
                                        fontSize = 60.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        )
                    } else {
                        // 没有缩略图URL时的回退
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF2A1B3D),
                                            Color(0xFF1A1030)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            } else {
                                Text(
                                    text = "🎵",
                                    fontSize = 80.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                    
                    // 播放状态指示器（简化）
                    if (mediaPlayer.isPlaying) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomEnd)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF64B5F6).copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♪",
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // 歌曲信息 - 更简洁
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = topSong.song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = topSong.song.artistsText ?: "Unknown Artist",
                        fontSize = 16.sp,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 关键统计数据 - 更简单，只有播放次数和分钟数
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 播放次数
                    StatColumn(
                        emoji = "🔥",
                        value = "${topSong.playCount}",
                        label = "PLAYS",
                        color = Color(0xFFFF6B6B)
                    )
                    
                    // 分钟数
                    StatColumn(
                        emoji = "⏱️",
                        value = "${topSong.minutes}",
                        label = "MINUTES",
                        color = Color(0xFF4FC3F7)
                    )
                }
                
                Spacer(modifier = Modifier.height(36.dp))
                
                // 有趣的统计事实 - 简化
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "📊 LISTENING STAT",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    val totalHours = (topSong.minutes / 60f).roundToInt()
                    val statText = if (totalHours > 0) {
                        "That's ${totalHours} hours of listening!"
                    } else {
                        "Your most played track this year!"
                    }
                    
                    Text(
                        text = statText,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 导航提示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "swipe to continue",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
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
                        text = "🎵",
                        fontSize = 56.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Text(
                        text = "No song data available",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 统计列组件
@Composable
private fun StatColumn(
    emoji: String,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

// 获取歌曲缩略图的函数（使用你提供的代码）
private suspend fun fetchTopSongThumbnail(songTitle: String, artist: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$songTitle $artist", "UTF-8")
            val url = "https://yt.omada.cafe/api/v1/search?q=$query&type=video"

            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val response = connection.getInputStream().bufferedReader().use { it.readText() }

            val maxresPattern = Regex("\"quality\"\\s*:\\s*\"maxres\".*?\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            maxresPattern.find(response)?.groups?.get(1)?.value?.let { return@withContext it.replace("\\/", "/") }

            val hqPattern = Regex("\"videoThumbnails\"\\s*:\\s*\\[.*?\\{\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"")
            hqPattern.find(response)?.groups?.get(1)?.value?.let { return@withContext it.replace("\\/", "/") }

            val simplePattern = Regex("\"url\"\\s*:\\s*\"(https?://[^\"]+\\.(jpg|png|webp))\"")
            simplePattern.findAll(response)
                .mapNotNull { it.groups[1]?.value?.replace("\\/", "/") }
                .firstOrNull()?.let { return@withContext it }

            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

// 获取歌曲预览音频的函数（单独处理）
private suspend fun fetchSongPreview(title: String, artist: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = "https://api.deezer.com/search?q=$query&limit=1"
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            if (json.has("data")) {
                val dataArray = json.getJSONArray("data")
                if (dataArray.length() > 0) {
                    val songData = dataArray.getJSONObject(0)
                    return@withContext songData.optString("preview", null)
                }
            }
            return@withContext null
        } catch (e: Exception) {
            // 静默失败，预览不是必需的
            return@withContext null
        }
    }
}
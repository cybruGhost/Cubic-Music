package it.fast4x.rimusic.ui.screens.rewind

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.BorderStroke

// Import the data classes and fetcher from your original code
import it.fast4x.rimusic.ui.screens.rewind.RewindDataFetcher
import it.fast4x.rimusic.ui.screens.rewind.RewindData
import it.fast4x.rimusic.ui.screens.rewind.ListeningStats
import it.fast4x.rimusic.ui.screens.rewind.TopSong
import it.fast4x.rimusic.ui.screens.rewind.TopArtist
import it.fast4x.rimusic.ui.screens.rewind.TopAlbum
import it.fast4x.rimusic.ui.screens.rewind.TopPlaylist
import it.fast4x.rimusic.ui.screens.rewind.MonthlyStat
import it.fast4x.rimusic.ui.screens.rewind.DailyStat
import it.fast4x.rimusic.ui.screens.rewind.HourlyStat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    var rewindData by remember { mutableStateOf<RewindData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("Music Fan") }
    var userRanking by remember { mutableStateOf("Explorer") }
    var userPercentile by remember { mutableStateOf("Top 50%") }
    var showMinutesInHours by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val currentYear = LocalDate.now().year
    
    // Loading optimization - load data in chunks
    var loadedTopSongs by remember { mutableStateOf<List<TopSong>?>(null) }
    var loadedTopArtists by remember { mutableStateOf<List<TopArtist>?>(null) }
    var loadedTopAlbums by remember { mutableStateOf<List<TopAlbum>?>(null) }
    var loadedTopPlaylists by remember { mutableStateOf<List<TopPlaylist>?>(null) }
    var loadedStats by remember { mutableStateOf<ListeningStats?>(null) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                username = runBlocking {
                    DataStoreUtils.getStringBlocking(context, DataStoreUtils.KEY_USERNAME, "Music Fan")
                }
                
                // Load basic stats first for quick display
                rewindData = RewindDataFetcher.getRewindData(currentYear)
                
                if (rewindData != null) {
                    loadedStats = rewindData!!.stats
                    
                    // Calculate ranking quickly
                    val joinDate = runBlocking {
                        DataStoreUtils.getStringBlocking(context, "join_date", "")
                    }
                    val rankingResult = calculateUserRanking(rewindData!!, joinDate)
                    userRanking = rankingResult.first
                    userPercentile = rankingResult.second
                    
                    // Load other data in background
                    scope.launch {
                        loadedTopSongs = rewindData!!.topSongs
                        loadedTopArtists = rewindData!!.topArtists
                        loadedTopAlbums = rewindData!!.topAlbums
                        loadedTopPlaylists = rewindData!!.topPlaylists
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0B1A),
                            Color(0xFF1A1529),
                            Color(0xFF2A1F3A)
                        )
                    )
                )
        )
        
        if (isLoading) {
            LoadingScreen()
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> WelcomeScreen(
                        username = username,
                        onGetStarted = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    1 -> RewindContentScreen(
                        username = username,
                        rewindData = rewindData,
                        loadedTopSongs = loadedTopSongs,
                        loadedTopArtists = loadedTopArtists,
                        loadedTopAlbums = loadedTopAlbums,
                        loadedTopPlaylists = loadedTopPlaylists,
                        loadedStats = loadedStats,
                        userRanking = userRanking,
                        userPercentile = userPercentile,
                        showMinutesInHours = showMinutesInHours,
                        onToggleMinutesHours = { showMinutesInHours = !showMinutesInHours },
                        onOpenDetailedRewind = {
                            val dataString = encodeRewindDataForUrl(rewindData, username, currentYear)
                            val url = "https://cubicrewind.lovable.app?data=$dataString"
                            uriHandler.openUri(url)
                        },
                        onShareRewind = {
                            shareRewindData(context, rewindData, username, userRanking, currentYear)
                        }
                    )
                }
            }
            
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PageIndicator(
                        pageCount = 2,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
    
    miniPlayer()
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9C27B0),
                                Color(0xFF673AB7)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎵",
                    fontSize = 36.sp
                )
            }
            
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "GENERATING YOUR REWIND...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
@Composable
fun WelcomeScreen(
    username: String,
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon with subtle gradient
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF),
                            Color(0xFF512DA8),
                            Color(0xFF311B92)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Using Text as a fallback since painterResource might not be available
            // Replace with actual icon when you have the resources
            Text(
                text = "♪", // Simple music note character
                fontSize = 48.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Welcome Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "REWIND ${LocalDate.now().year}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 20.sp
                        )
                    ) {
                        append("Welcome back, ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF7C4DFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    ) {
                        append(username)
                    }
                },
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Description
        Text(
            text = "Your personal music journey for ${LocalDate.now().year}. " +
                   "Discover insights about your listening habits and favorite artists.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Privacy Note Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1B26)
            ),
            border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Privacy icon using text
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒", // Simple lock emoji as fallback
                        fontSize = 16.sp,
                        color = Color(0xFF7C4DFF)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Privacy First",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your data never leaves your device. " +
                           "All analytics are processed locally with complete privacy.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Get Started Button
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C4DFF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun FeatureCard(icon: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE040FB),
                                Color(0xFF7E57C2)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindContentScreen(
    username: String,
    rewindData: RewindData?,
    loadedTopSongs: List<TopSong>?,
    loadedTopArtists: List<TopArtist>?,
    loadedTopAlbums: List<TopAlbum>?,
    loadedTopPlaylists: List<TopPlaylist>?,
    loadedStats: ListeningStats?,
    userRanking: String,
    userPercentile: String,
    showMinutesInHours: Boolean,
    onToggleMinutesHours: () -> Unit,
    onOpenDetailedRewind: () -> Unit,
    onShareRewind: () -> Unit
) {
    val data = rewindData ?: return
    val stats = loadedStats ?: data.stats
    val scope = rememberCoroutineScope()
    
    // Separate pager for top songs/artists
    val topItemsPagerState = rememberPagerState(pageCount = { 2 })
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Title with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "♫",
                        fontSize = 22.sp,
                        color = Color(0xFFE040FB),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "REWIND",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "♫",
                        fontSize = 22.sp,
                        color = Color(0xFFE040FB),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // Year Display
                Text(
                    text = data.year.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE040FB),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Personalized Greeting
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )
                        ) {
                            append("For ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFE040FB),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        ) {
                            append(username)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )
                        ) {
                            append("'s musical journey")
                        }
                    },
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Enhanced User Ranking Card
        item {
            EnhancedUserRankingCard(userRanking, userPercentile)
        }
        
        // Action Buttons - FIXED: Added missing ActionButtonsCard
        item {
            ActionButtonsCard(onOpenDetailedRewind, onShareRewind)
        }
        
        // Main Stats Card with toggle
        item {
            MainStatsCard(
                data = data,
                showMinutesInHours = showMinutesInHours,
                onToggleMinutesHours = onToggleMinutesHours
            )
        }
        
        // Top Songs & Artists Pager
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A1F3A)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 Top Content",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        // Pager tabs
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3D2E54))
                                .padding(2.dp)
                        ) {
                            TabButton(
                                text = "Songs",
                                isSelected = topItemsPagerState.currentPage == 0,
                                onClick = {
                                    scope.launch {
                                        topItemsPagerState.animateScrollToPage(0)
                                    }
                                }
                            )
                            TabButton(
                                text = "Artists",
                                isSelected = topItemsPagerState.currentPage == 1,
                                onClick = {
                                    scope.launch {
                                        topItemsPagerState.animateScrollToPage(1)
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalPager(state = topItemsPagerState) { page ->
                        when (page) {
                            0 -> {
                                val songs = loadedTopSongs ?: data.topSongs
                                if (songs.isNotEmpty()) {
                                    Column {
                                        songs.take(10).forEachIndexed { index, song ->
                                            TopSongItem(
                                                rank = index + 1,
                                                song = song,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No songs data available",
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            1 -> {
                                val artists = loadedTopArtists ?: data.topArtists
                                if (artists.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(artists.take(10).chunked(2)) { row ->
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.forEach { artist ->
                                                    ArtistCard(
                                                        artist = artist,
                                                        modifier = Modifier
                                                            .width(150.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No artists data available",
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Top Albums Section
        item {
            val albums = loadedTopAlbums ?: data.topAlbums
            if (albums.isNotEmpty()) {
                TopAlbumsCard(albums)
            }
        }
        
        // Top Playlists Section
        item {
            val playlists = loadedTopPlaylists ?: data.topPlaylists
            if (playlists.isNotEmpty()) {
                TopPlaylistsCard(playlists)
            }
        }
        
        // Enhanced Listening Patterns
        item {
            EnhancedListeningPatternsCard(stats)
        }
        
        // More Insights
        item {
            MoreInsightsCard(data, showMinutesInHours)
        }
        
        // Footer note
        item {
            Text(
                text = "Your data is stored locally on your device only",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun ActionButtonsCard(
    onOpenDetailedRewind: () -> Unit,
    onShareRewind: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Detailed Rewind Button
        Card(
            onClick = onOpenDetailedRewind,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF7C4DFF).copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon using text character
                Text(
                    text = "📊",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Detailed Analysis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7C4DFF)
                )
            }
        }

        // Share Button
        Card(
            onClick = onShareRewind,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF7C4DFF)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon using text character
                Text(
                    text = "↗",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Share",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF7C4DFF) else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        ),
        border = if (!isSelected) BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        ) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                // Small indicator dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}

// Optional: Tab Bar Container for better organization
@Composable
fun TabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B26)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                TabButton(
                    text = tab,
                    isSelected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}
@Composable
fun EnhancedUserRankingCard(userRanking: String, userPercentile: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🏆 Your Music Ranking",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = userRanking,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = userPercentile,
                    fontSize = 14.sp,
                    color = Color(0xFFE040FB),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFC107),
                                Color(0xFFFF9800)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (userRanking) {
                    "Legendary Listener" -> Text("👑", fontSize = 32.sp)
                    "Music Enthusiast" -> Text("🔥", fontSize = 32.sp)
                    "Regular Listener" -> Text("⭐", fontSize = 32.sp)
                    "Casual Listener" -> Text("🎵", fontSize = 32.sp)
                    else -> Text("🏆", fontSize = 32.sp)
                }
            }
        }
    }
}

@Composable
fun MainStatsCard(
    data: RewindData,
    showMinutesInHours: Boolean,
    onToggleMinutesHours: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Your Year in Numbers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Toggle button for minutes/hours
                Text(
                    text = if (showMinutesInHours) "Switch to Min" else "Switch to Hrs",
                    fontSize = 12.sp,
                    color = Color(0xFFE040FB),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3D2E54))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onToggleMinutesHours()
                            }
                        }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedStatBox(
                    value = data.stats.totalPlays.toString(),
                    label = "Total Plays",
                    icon = "▶️",
                    color = Color(0xFFE040FB),
                    modifier = Modifier.weight(1f)
                )
                
                EnhancedStatBox(
                    value = if (showMinutesInHours) {
                        val hours = data.stats.totalMinutes / 60
                        val remainingMinutes = data.stats.totalMinutes % 60
                        if (hours > 0) "$hours h" else "$remainingMinutes m"
                    } else {
                        "${data.stats.totalMinutes} m"
                    },
                    label = if (showMinutesInHours) "Hours Listened" else "Minutes Listened",
                    icon = "⏱️",
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedStatBox(
                    value = data.totalUniqueSongs.toString(),
                    label = "Unique Songs",
                    icon = "🎶",
                    color = Color(0xFFAB47BC),
                    modifier = Modifier.weight(1f)
                )
                
                EnhancedStatBox(
                    value = data.daysWithMusic.toString(),
                    label = "Active Days",
                    icon = "📅",
                    color = Color(0xFFCE93D8),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EnhancedStatBox(
    value: String,
    label: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopSongsCard(songs: List<TopSong>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🔥 Top 10 Songs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            songs.take(10).forEachIndexed { index, song ->
                TopSongItem(
                    rank = index + 1,
                    song = song,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun TopSongItem(
    rank: Int,
    song: TopSong,
    modifier: Modifier = Modifier
) {
    var thumbnailUrl by remember { mutableStateOf<String?>(song.song.thumbnailUrl) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(song.song.title, song.song.artistsText) {
        if (thumbnailUrl.isNullOrEmpty() && song.song.title.isNotEmpty() && song.song.artistsText != null) {
            scope.launch {
                val fetchedUrl = fetchSongThumbnail(song.song.title, song.song.artistsText!!)
                if (fetchedUrl != null) {
                    thumbnailUrl = fetchedUrl
                }
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enhanced rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFC107))
                            )
                            2 -> Brush.radialGradient(
                                colors = listOf(Color(0xFFC0C0C0), Color(0xFF9E9E9E))
                            )
                            3 -> Brush.radialGradient(
                                colors = listOf(Color(0xFFCD7F32), Color(0xFF8D6E63))
                            )
                            else -> Brush.radialGradient(
                                colors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = if (rank <= 3) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            // Song thumbnail with fallback
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(
                    url = thumbnailUrl,
                    contentDescription = "Song thumbnail",
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Text(
                    text = song.song.artistsText ?: "Unknown Artist",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Enhanced play count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${song.playCount}",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${song.minutes} min",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TopArtistsCard(artists: List<TopArtist>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "⭐ Top 10 Artists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(artists.take(10)) { artist ->
                    ArtistCard(artist = artist, modifier = Modifier.width(150.dp))
                }
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: TopArtist,
    modifier: Modifier = Modifier
) {
    var thumbnailUrl by remember { mutableStateOf<String?>(artist.artist.thumbnailUrl) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(artist.artist.name) {
        if (thumbnailUrl.isNullOrEmpty() && artist.artist.name != null) {
            scope.launch {
                val fetchedUrl = fetchArtistThumbnail(artist.artist.name!!)
                if (fetchedUrl != null) {
                    thumbnailUrl = fetchedUrl
                }
            }
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Enhanced artist thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(
                    url = thumbnailUrl,
                    contentDescription = "Artist thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = artist.artist.name ?: "Unknown Artist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${artist.minutes} min",
                fontSize = 12.sp,
                color = Color(0xFFE040FB),
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${artist.songCount} songs",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopAlbumsCard(albums: List<TopAlbum>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "💿 Top Albums",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            albums.take(5).forEachIndexed { index, album ->
                AlbumItem(
                    rank = index + 1,
                    album = album,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

// FIXED: Added missing AlbumItem function
@Composable
fun AlbumItem(
    rank: Int,
    album: TopAlbum,
    modifier: Modifier = Modifier
) {
    var thumbnailUrl by remember { mutableStateOf<String?>(album.album.thumbnailUrl) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Color(0xFF9C27B0).copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = if (rank <= 3) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            // Album cover with fallback
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(
                    url = thumbnailUrl,
                    contentDescription = "Album cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Album info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.album.title ?: "Unknown Album",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Text(
                    text = album.album.authorsText ?: "Unknown Artist",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${album.songCount}",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${album.minutes} min",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TopPlaylistsCard(playlists: List<TopPlaylist>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📋 Top Playlists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            playlists.take(5).forEachIndexed { index, playlist ->
                PlaylistItem(
                    rank = index + 1,
                    playlist = playlist,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(
    rank: Int,
    playlist: TopPlaylist,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.playlist.playlist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Text(
                    text = "${playlist.songCount} songs",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = "${playlist.minutes} min",
                fontSize = 12.sp,
                color = Color(0xFFE040FB),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EnhancedListeningPatternsCard(stats: ListeningStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "⏰ Listening Patterns",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // First row: Most Active Day and Hour
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stats.mostActiveDay != null) {
                    EnhancedPatternItem(
                        icon = "📅",
                        title = "Most Active Day",
                        value = stats.mostActiveDay.dayOfWeek,
                        description = "Day you listened the most",
                        color = Color(0xFF7E57C2),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (stats.mostActiveHour != null) {
                    EnhancedPatternItem(
                        icon = "🕒",
                        title = "Most Active Hour",
                        value = stats.mostActiveHour.hour,
                        description = "Peak listening time",
                        color = Color(0xFFAB47BC),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Second row: Most Active Month and Average daily minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedPatternItem(
                    icon = "📊",
                    title = "Daily Average",
                    value = String.format("%.1f min", stats.averageDailyMinutes),
                    description = "Average per day",
                    color = Color(0xFFE040FB),
                    modifier = Modifier.weight(1f)
                )
                
                // Average session length
                val avgSessionMinutes = if (stats.totalPlays > 0) {
                    String.format("%.1f", stats.totalMinutes.toDouble() / stats.totalPlays)
                } else "0"
                
                EnhancedPatternItem(
                    icon = "🎵",
                    title = "Avg. Session",
                    value = "${avgSessionMinutes} min",
                    description = "Average per play",
                    color = Color(0xFFCE93D8),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Third row: First and Last Play
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stats.firstPlayDate != null) {
                    EnhancedPatternItem(
                        icon = "🎬",
                        title = "First Play",
                        value = stats.firstPlayDate,
                        description = "When you started",
                        color = Color(0xFF673AB7),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (stats.lastPlayDate != null) {
                    EnhancedPatternItem(
                        icon = "⏹️",
                        title = "Last Played",
                        value = stats.lastPlayDate,
                        description = "Most recent listen",
                        color = Color(0xFF9575CD),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPatternItem(
    icon: String,
    title: String,
    value: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun EnhancedTimeStatItem(
    label: String,
    minutes: Long,
    plays: Int,
    showMinutesInHours: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2E54)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (showMinutesInHours) {
                            val hours = minutes / 60
                            val remainingMinutes = minutes % 60
                            if (hours > 0) "$hours h $remainingMinutes m" else "$remainingMinutes m"
                        } else {
                            "$minutes m"
                        },
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$plays plays",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun MoreInsightsCard(data: RewindData, showMinutesInHours: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1F3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📈 More Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Average plays per day
            val avgPlaysPerDay = if (data.stats.totalPlays > 0) {
                String.format("%.1f", data.stats.totalPlays / 365.0)
            } else "0"
            
            EnhancedInsightRow(
                icon = "📊",
                title = "Daily Average Plays",
                value = "$avgPlaysPerDay plays/day"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Average minutes per day
            val avgMinutesPerDay = if (data.stats.totalMinutes > 0) {
                String.format("%.1f", data.stats.totalMinutes / 365.0)
            } else "0"
            
            EnhancedInsightRow(
                icon = "⏱️",
                title = "Daily Listening",
                value = if (showMinutesInHours) {
                    val hours = avgMinutesPerDay.toDouble() / 60
                    String.format("%.1f hours/day", hours)
                } else {
                    "$avgMinutesPerDay min/day"
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add average listening session time
            val avgSessionMinutes = if (data.stats.totalMinutes > 0 && data.stats.totalPlays > 0) {
                val avg = data.stats.totalMinutes.toDouble() / data.stats.totalPlays.toDouble()
                String.format("%.1f", avg)
            } else "0"
            
            EnhancedInsightRow(
                icon = "🎵",
                title = "Avg. Session Length",
                value = "${avgSessionMinutes} min"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add unique artists count
            EnhancedInsightRow(
                icon = "👥",
                title = "Unique Artists",
                value = data.totalUniqueArtists.toString()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add unique albums count
            EnhancedInsightRow(
                icon = "💿",
                title = "Unique Albums",
                value = data.totalUniqueAlbums.toString()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add unique playlists count
            EnhancedInsightRow(
                icon = "📋",
                title = "Unique Playlists",
                value = data.totalUniquePlaylists.toString()
            )
        }
    }
}

@Composable
fun EnhancedInsightRow(
    icon: String,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE040FB),
                            Color(0xFF7E57C2)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (page == currentPage) 
                            Color(0xFFE040FB)
                        else 
                            Color.White.copy(alpha = 0.3f)
                    )
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
            // Silently fail - we'll use fallback
        }
        return@withContext null
    }
}

// Function to fetch artist thumbnail from API
suspend fun fetchArtistThumbnail(artistName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(artistName, "UTF-8")
            val url = "https://yt.omada.cafe/api/v1/search?q=$query&type=channel"
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            
            // Parse JSON response to find author thumbnail
            val thumbnailMatch = Regex("\"authorThumbnails\":\\s*\\[.*?\"url\":\"(.*?)\"").find(response)
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
            // Silently fail - we'll use fallback
        }
        return@withContext null
    }
}

// Simple image loader composable without Coil
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(url) {
        if (url != null) {
            isLoading = true
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 2000 // Fast timeout
                    connection.readTimeout = 2000
                    val inputStream = connection.getInputStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    bitmap?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }
    
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
        if (isLoading) {
            placeholder()
        } else if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Fallback emoji if image fails to load
            Text(
                text = if (contentDescription?.contains("artist") == true) "🎤" else "🎵",
                fontSize = 20.sp
            )
        }
    }
}

// Enhanced ranking calculation
private fun calculateUserRanking(data: RewindData?, joinDate: String): Pair<String, String> {
    if (data == null) return Pair("New Listener", "Just Starting")
    
    val totalMinutes = data.stats.totalMinutes
    val totalPlays = data.stats.totalPlays
    val uniqueSongs = data.totalUniqueSongs
    val daysWithMusic = data.daysWithMusic
    
    // Calculate comprehensive score
    var score = 0
    
    // Hours listened scoring
    val hoursListened = totalMinutes / 60.0
    score += when {
        hoursListened >= 1000 -> 40
        hoursListened >= 500 -> 35
        hoursListened >= 200 -> 30
        hoursListened >= 100 -> 25
        hoursListened >= 50 -> 20
        hoursListened >= 20 -> 15
        hoursListened >= 10 -> 10
        hoursListened >= 5 -> 5
        else -> 1
    }
    
    // Total plays scoring
    score += when {
        totalPlays >= 10000 -> 40
        totalPlays >= 5000 -> 35
        totalPlays >= 2000 -> 30
        totalPlays >= 1000 -> 25
        totalPlays >= 500 -> 20
        totalPlays >= 200 -> 15
        totalPlays >= 100 -> 10
        totalPlays >= 50 -> 5
        else -> 1
    }
    
    // Unique songs scoring
    score += when {
        uniqueSongs >= 1000 -> 30
        uniqueSongs >= 500 -> 25
        uniqueSongs >= 200 -> 20
        uniqueSongs >= 100 -> 15
        uniqueSongs >= 50 -> 10
        uniqueSongs >= 20 -> 5
        else -> 1
    }
    
    // Active days scoring
    val activeDaysPercent = (daysWithMusic / 365.0) * 100
    score += when {
        activeDaysPercent >= 90 -> 30
        activeDaysPercent >= 75 -> 25
        activeDaysPercent >= 50 -> 20
        activeDaysPercent >= 30 -> 15
        activeDaysPercent >= 20 -> 10
        activeDaysPercent >= 10 -> 5
        else -> 1
    }
    
    // Join date bonus (simplified)
    if (joinDate.isNotEmpty()) {
        try {
            val joinYear = joinDate.substring(0, 4).toInt()
            val currentYear = LocalDate.now().year
            val yearsActive = currentYear - joinYear
            
            score += when {
                yearsActive >= 10 -> 20
                yearsActive >= 5 -> 15
                yearsActive >= 3 -> 10
                yearsActive >= 2 -> 5
                yearsActive >= 1 -> 2
                else -> 1
            }
        } catch (e: Exception) {
            score += 1
        }
    }
    
    // Determine ranking and percentile with better names
    return when {
        score >= 140 -> Pair("Legendary Listener", "Top 1%")
        score >= 120 -> Pair("Music Maestro", "Top 3%")
        score >= 100 -> Pair("Audio Aficionado", "Top 5%")
        score >= 80 -> Pair("Melody Master", "Top 10%")
        score >= 60 -> Pair("Music Enthusiast", "Top 20%")
        score >= 40 -> Pair("Regular Listener", "Top 35%")
        score >= 20 -> Pair("Casual Listener", "Top 50%")
        score >= 10 -> Pair("New Explorer", "Top 75%")
        else -> Pair("Just Starting", "Top 90%")
    }
}

private fun encodeRewindDataForUrl(data: RewindData?, username: String, year: Int): String {
    if (data == null) return ""
    
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
            
            append("\"listeningPatterns\":{")
            data.stats.mostActiveDay?.let {
                append("\"mostActiveDay\":\"${escapeJsonString(it.dayOfWeek)}\",")
            }
            data.stats.mostActiveHour?.let {
                append("\"mostActiveHour\":\"${escapeJsonString(it.hour)}\",")
            }
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

private fun shareRewindData(context: Context, data: RewindData?, username: String, ranking: String, year: Int) {
    if (data == null) return
    
    val shareText = buildString {
        append("🎵 My $year Music Rewind 🎵\n\n")
        append("🎤 Username: $username\n")
        append("🏆 Ranking: $ranking\n\n")
        append("📊 Stats:\n")
        append("• Total Plays: ${data.stats.totalPlays}\n")
        append("• Total Listening: ${data.stats.totalMinutes} minutes\n")
        append("• Unique Songs: ${data.totalUniqueSongs}\n")
        append("• Active Days: ${data.daysWithMusic}\n")
        
        if (data.stats.mostActiveDay != null) {
            append("• Most Active Day: ${data.stats.mostActiveDay.dayOfWeek}\n")
        }
        if (data.stats.mostActiveHour != null) {
            append("• Most Active Hour: ${data.stats.mostActiveHour.hour}\n")
        }
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
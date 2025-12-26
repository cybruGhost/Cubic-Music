package it.fast4x.rimusic.ui.screens.rewind

import app.kreate.android.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import com.mikepenz.hypnoticcanvas.shaders.Shader
import it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.random.Random
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale



// Import your slide components
import it.fast4x.rimusic.ui.screens.rewind.slides.*
import it.fast4x.rimusic.ui.screens.rewind.slides.AfterTopArtistsSlide


// Professional shader backgrounds (only for loading screen)
val shaderOptions: List<Shader> = listOf(BlackCherryCosmos, GoldenMagma)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    var rewindData by remember { mutableStateOf<RewindData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("Music Fan") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentYear = LocalDate.now().year
    
    // Main pager state for slides - REDUCED TO 8 SLIDES (removed daily/hourly)
   // Change from 8 to 9 slides
    val mainPagerState = rememberPagerState(pageCount = { 10 })
    
    LaunchedEffect(Unit) {
        try {
            // Get username
            username = runBlocking {
                DataStoreUtils.getStringBlocking(context, DataStoreUtils.KEY_USERNAME, "Music Fan")
            }
            
            // Load rewind data WITHOUT daily/hourly stats
            val fetchedData = RewindDataFetcher.getRewindData(currentYear)
            
            // Create clean data WITHOUT daily/hourly stats
            rewindData = fetchedData.copy(
                dailyStats = emptyList(),
                hourlyStats = emptyList()
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Create empty data if fetch fails
            rewindData = createEmptyRewindData(currentYear)
        } finally {
            delay(600) // Smooth loading transition
            isLoading = false
        }
    }
    
   Box(
    modifier = Modifier.fillMaxSize()  // NO PADDING!
) {
        if (isLoading) {
            // Random shader for loading screen only (50/50 chance)
            val currentShader = remember { shaderOptions.random() }
            OptimizedLoadingScreen(currentShader)
        } else {
            // FULL SCREEN - NO HEADERS, NO BACKGROUNDS, JUST BLACK
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val data = rewindData
                
                HorizontalPager(
                    state = mainPagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> "page_$page" }
                ) { page ->
                    // EACH SLIDE IS FULL SCREEN - NO CONTAINERS
                    when (page) {
                        0 -> WelcomeSlide(
                            username = username,
                            year = currentYear,
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(1) }
                            }
                        )
                        1 -> StatsSlide(
                            username = username,
                            data = data ?: createEmptyRewindData(currentYear),
                            userRanking = "Music Explorer",
                            userPercentile = "Top 50%",
                            showMinutesInHours = false,
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(2) }
                            }
                        )
                        2 -> TopSongsSlide(
                            songs = data?.topSongs ?: emptyList(),
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(3) }
                            }
                        )
                        3 -> AfterTopSongsSlide(
                            topSong = data?.topSongs?.firstOrNull(), // Pass the #1 song
                            onNext = { scope.launch { mainPagerState.animateScrollToPage(4) } }
                        )
                        4 -> TopArtistsSlide(
                            artists = data?.topArtists ?: emptyList(),
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(5) }
                            }
                        )
                        5 -> AfterTopArtistsSlide( // NEW SLIDE ADDED HERE
                            topArtist = data?.topArtists?.firstOrNull(), // Get #1 artist
                            onNext = { 
                                scope.launch { mainPagerState.animateScrollToPage(6) } // Next is now 6
                            }
                        )
                        6 -> TopAlbumsSlide( // Changed from 5 to 6
                            albums = data?.topAlbums ?: emptyList(),
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(7) } // Changed from 6 to 7
                            }
                        )
                        7 -> MonthlyStatsSlide( // Changed from 6 to 7
                            monthlyStats = data?.monthlyStats ?: emptyList(),
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(8) } // Changed from 7 to 8
                            }
                        )
                        8 -> BestOfAllSlide( // Changed from 7 to 8
                            data = data ?: createEmptyRewindData(currentYear),
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(9) } // Changed from 8 to 9
                            }
                        )
                        9 -> DonateSlide( // Changed from 8 to 9
                            onNext = {
                                scope.launch { mainPagerState.animateScrollToPage(0) }
                            }

                        )
                    }
                }
                
                // Simple page indicator at the bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(10) { index ->  // Changed from 10 to 8
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (index == mainPagerState.currentPage) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == mainPagerState.currentPage) 
                                            Color.White 
                                        else 
                                            Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
    
    miniPlayer()
}

@Composable
fun OptimizedLoadingScreen(shader: Shader) {
    var currentProgress by remember { mutableStateOf(0) }
    var loadingText by remember { mutableStateOf("Loading your music journey...") }
    
    LaunchedEffect(Unit) {
        // Simple progress that completes
        repeat(100) {
            delay(30)
            currentProgress += 1
            
            // Update text based on progress
            loadingText = when {
                currentProgress < 25 -> "Fetching your listening data..."
                currentProgress < 50 -> "Analyzing top songs..."
                currentProgress < 75 -> "Compiling artist stats..."
                currentProgress < 90 -> "Preparing your rewind..."
                else -> "Almost ready!"
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(shader),  // Shader background ONLY here
        contentAlignment = Alignment.Center
    ) {
        // Premium gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        radius = 800f
                    )
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // Logo area
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.rewindlogo),
                    contentDescription = "Rewind Logo",
                    modifier = Modifier
                        .size(83.dp)
                        .clip(RoundedCornerShape(15.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Loading content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title with 3 lines format
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LocalDate.now().year.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MUSIC",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "REWIND",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                }
                
                // Progress indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = loadingText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        LinearProgressIndicator(
                            progress = currentProgress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
                
                // Simple hint
                Text(
                    text = "Swipe to navigate",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// Helper function to create empty RewindData
private fun createEmptyRewindData(year: Int): RewindData {
    return RewindData(
        topSongs = emptyList(),
        topArtists = emptyList(),
        topAlbums = emptyList(),
        topPlaylists = emptyList(),
        stats = ListeningStats(
            totalPlays = 0,
            totalMinutes = 0,
            mostActiveDay = null,
            mostActiveHour = null,
            mostActiveMonth = null,
            averageDailyMinutes = 0.0,
            firstPlayDate = null,
            lastPlayDate = null
        ),
        monthlyStats = emptyList(),
        dailyStats = emptyList(),  // Empty
        hourlyStats = emptyList(), // Empty
        totalUniqueSongs = 0,
        totalUniqueArtists = 0,
        totalUniqueAlbums = 0,
        totalUniquePlaylists = 0,
        year = year,
        daysWithMusic = 0
    )
}
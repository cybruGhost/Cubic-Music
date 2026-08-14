package app.it.fast4x.rimusic.ui.screens.rewind

import app.kreate.android.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

// Import your slide components
import app.it.fast4x.rimusic.ui.screens.rewind.slides.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlin.math.cos
import kotlin.math.sin

private const val RewindDeckPageCount = 12

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    var rewindData by remember { mutableStateOf<RewindData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("Music Fan") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentYear = LocalDate.now().year
    
    // Main pager state for slides
    val mainPagerState = rememberPagerState(pageCount = { RewindDeckPageCount })
    
    LaunchedEffect(Unit) {
        try {
            username = runBlocking {
                DataStoreUtils.getStringBlocking(context, DataStoreUtils.KEY_USERNAME, "Music Fan")
            }
            
            rewindData = RewindDataFetcher.getRewindData(currentYear)
            
        } catch (e: Exception) {
            e.printStackTrace()
            rewindData = createEmptyRewindData(currentYear)
        } finally {
            delay(600)
            isLoading = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // FLUID LOADING SCREEN using color palette
            FluidLoadingScreen(colorPalette)
        } else {
            // FULL SCREEN SLIDES
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorPalette.background0)
            ) {
                val data = rewindData
                
                HorizontalPager(
                    state = mainPagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> "page_$page" }
                ) { page ->
                    val safeData = data ?: createEmptyRewindData(currentYear)
                    val nextPage: () -> Unit = {
                        scope.launch {
                            mainPagerState.animateScrollToPage((page + 1).coerceAtMost(RewindDeckPageCount - 1))
                        }
                    }
                    when (page) {
                        0 -> RewindIntroCard(
                            username = username,
                            year = currentYear,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        1 -> RewindTotalTimeCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        2 -> RewindTopSongCard(
                            topSong = safeData.topSongs.firstOrNull(),
                            year = safeData.year,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        3 -> RewindTopArtistsCard(
                            artists = safeData.topArtists,
                            year = safeData.year,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        4 -> RewindTopSongsCard(
                            songs = safeData.topSongs,
                            year = safeData.year,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        5 -> RewindPeakTimeCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        6 -> RewindListeningDaysCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        7 -> RewindDiscoveryCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        8 -> RewindTopAlbumCard(
                            topAlbum = safeData.topAlbums.firstOrNull(),
                            year = safeData.year,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        9 -> RewindAlbumsCard(
                            albums = safeData.topAlbums,
                            year = safeData.year,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        10 -> RewindMonthlyCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onNext = nextPage
                        )
                        11 -> RewindFinaleCard(
                            data = safeData,
                            page = page,
                            pageCount = RewindDeckPageCount,
                            onShare = {
                                scope.launch { mainPagerState.animateScrollToPage(0) }
                            }
                        )
                    }
                }
            }
        }
    }
    
    miniPlayer()
}

@Composable
fun FluidLoadingScreen(colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette) {
    val configuration = LocalConfiguration.current
    
    var currentProgress by remember { mutableStateOf(0) }
    var loadingText by remember { mutableStateOf("Loading your music journey...") }
    
    // FLUID ANIMATION - these are Floats for position calculations, NOT colors
    val infiniteTransition = rememberInfiniteTransition()
    val fluidOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        )
    )
    val fluidOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        )
    )
    val fluidOffset3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        )
    )
    
    LaunchedEffect(Unit) {
        repeat(100) {
            delay(30)
            currentProgress += 1
            
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
            .background(colorPalette.background0),
        contentAlignment = Alignment.Center
    ) {
        // FLUID BACKGROUND using accent colors - these are fixed Colors, not animated
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val width = size.width
            val height = size.height
            
            // Create fluid wave effect using accent colors - THESE ARE COLORS, NOT ANIMATED FLOATS
            val accentColor1 = colorPalette.accent.copy(alpha = 0.15f)
            val accentColor2 = colorPalette.accent.copy(alpha = 0.1f)
            val accentColor3 = colorPalette.accent.copy(alpha = 0.05f)
            
            // First wave - using fluidOffset1 for POSITION only
            drawCircle(
                color = accentColor1, // This is a Color
                radius = width * 0.8f,
                center = Offset(
                    x = width * (0.5f + 0.2f * sin(fluidOffset1)), // fluidOffset1 used for position
                    y = height * (0.5f + 0.2f * cos(fluidOffset1 * 1.3f))
                )
            )
            
            // Second wave - using fluidOffset2 for POSITION only
            drawCircle(
                color = accentColor2, // This is a Color
                radius = width * 0.9f,
                center = Offset(
                    x = width * (0.5f + 0.25f * cos(fluidOffset2)), // fluidOffset2 used for position
                    y = height * (0.5f + 0.25f * sin(fluidOffset2 * 1.7f))
                )
            )
            
            // Third wave - using fluidOffset3 for POSITION only
            drawCircle(
                color = accentColor3, // This is a Color
                radius = width * 1.0f,
                center = Offset(
                    x = width * (0.5f + 0.15f * sin(fluidOffset3 * 1.2f)), // fluidOffset3 used for position
                    y = height * (0.5f + 0.15f * cos(fluidOffset3 * 0.8f))
                )
            )
        }
        
        // Gradient overlay - using proper Color list
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf<Color>( // Explicitly typed as List<Color>
                            Color.Transparent,
                            colorPalette.background0.copy(alpha = 0.7f),
                            colorPalette.background0
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
                    .background(colorPalette.background2)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.rewind_playback_cover),
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
                // Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LocalDate.now().year.toString(),
                        color = colorPalette.accent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MUSIC",
                        color = colorPalette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "REWIND",
                        color = colorPalette.accent,
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
                        .background(colorPalette.background2)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = loadingText,
                            color = colorPalette.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        LinearProgressIndicator(
                            progress = { currentProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colorPalette.accent,
                            trackColor = colorPalette.background3
                        )
                    }
                }
                
                // Hint
                Text(
                    text = "Swipe to navigate",
                    color = colorPalette.textDisabled,
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
        dailyStats = emptyList(),
        hourlyStats = emptyList(),
        totalUniqueSongs = 0,
        totalUniqueArtists = 0,
        totalUniqueAlbums = 0,
        totalUniquePlaylists = 0,
        year = year,
        daysWithMusic = 0
    )
}

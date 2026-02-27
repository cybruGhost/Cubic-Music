package app.it.fast4x.rimusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import database.MusicDatabaseDesktop
import database.entities.Album
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.PlayerBody
import it.fast4x.innertube.requests.player
import app.it.fast4x.rimusic.enums.PageType
import app.it.fast4x.rimusic.extensions.webpotoken.PoTokenGenerator
import app.it.fast4x.rimusic.styling.Dimensions.layoutColumnBottomPadding
import app.it.fast4x.rimusic.styling.Dimensions.layoutColumnBottomSpacer
import app.it.fast4x.rimusic.styling.Dimensions.layoutColumnTopPadding
import app.it.fast4x.rimusic.styling.Dimensions.layoutColumnsHorizontalPadding
import app.it.fast4x.rimusic.ui.components.MiniPlayer
import app.it.fast4x.rimusic.ui.pages.AlbumsPage
import app.it.fast4x.rimusic.ui.pages.SongsPage
import app.it.fast4x.rimusic.ui.screens.AlbumScreen
import app.it.fast4x.rimusic.ui.screens.ArtistScreen
import app.it.fast4x.rimusic.ui.screens.MoodScreen
import app.it.fast4x.rimusic.ui.screens.PlaylistScreen
import app.it.fast4x.rimusic.ui.screens.QuickPicsScreen
import app.it.fast4x.rimusic.utils.getPipedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import player.frame.FrameContainer
import rimusic.composeapp.generated.resources.Res
import rimusic.composeapp.generated.resources.album
import rimusic.composeapp.generated.resources.app_icon
import rimusic.composeapp.generated.resources.app_logo_text
import rimusic.composeapp.generated.resources.artists
import rimusic.composeapp.generated.resources.library
import rimusic.composeapp.generated.resources.musical_notes
import vlcj.VlcjFrameController


@Composable
fun ThreeColumnsApp() {

    val db = remember { MusicDatabaseDesktop }

    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }

    var videoId by remember { mutableStateOf("") }
    var nowPlayingSong by remember { mutableStateOf<Song?>(null) }
    var artistId by remember { mutableStateOf("") }
    var albumId by remember { mutableStateOf("") }
    var playlistId by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<Innertube.Mood.Item?>(null) }

    val formatAudio =
        remember { mutableStateOf<PlayerResponse.StreamingData.AdaptiveFormat?>(null) }
    
    var playerError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        if (videoId.isEmpty()) return@LaunchedEffect
        
        playerError = null
        formatAudio.value = null

        println("=== ATTEMPTING TO PLAY VIDEO: $videoId ===")
        
        try {
            // 1. Try to generate PoToken first (REQUIRED to bypass server restrictions)
            println("Generating PoToken...")
            val poTokenGenerator = PoTokenGenerator()
            val poTokenResult = poTokenGenerator.getWebClientPoToken(videoId)
            val poToken = poTokenResult?.playerRequestPoToken
            
            println("PoToken generated: ${if (poToken != null) "SUCCESS" else "FAILED"}")
            if (poToken != null) {
                println("PoToken (first 30 chars): ${poToken.take(30)}...")
            }
            
            // 2. Call player WITH PoToken (this fixes "server restrictions" error)
            println("Calling Innertube.player with PoToken...")
            val response = Innertube.player(
                videoId = videoId,
                poToken = poToken  // CRITICAL: This bypasses YouTube's restrictions
            )
            
            response.onSuccess { playerResponse ->
                println("✅ PLAYER REQUEST SUCCESSFUL WITH POTOKEN!")
                println("Streaming data available: ${playerResponse.streamingData != null}")
                println("Available formats: ${playerResponse.streamingData?.adaptiveFormats?.size ?: 0}")
                
                // Get the best audio format
                formatAudio.value = playerResponse.streamingData?.autoMaxQualityFormat?.let { format ->
                    // Add range parameter to avoid YouTube throttling
                    val urlWithRange = if (format.contentLength != null) {
                        "${format.url}&range=0-${format.contentLength}"
                    } else {
                        "${format.url}&range=0-10000000" // Default 10MB range
                    }
                    format.copy(url = urlWithRange)
                }
                
                if (formatAudio.value != null) {
                    println("✅ Audio URL obtained successfully!")
                    println("Audio URL (first 100 chars): ${formatAudio.value?.url?.take(100)}...")
                    println("Audio format: ${formatAudio.value?.mimeType}")
                    println("Audio bitrate: ${formatAudio.value?.bitrate}")
                } else {
                    println("⚠️ No audio format found in response")
                    playerError = "No audio format available"
                }
                
            }.onFailure { error ->
                println("❌ PLAYER REQUEST FAILED: ${error.message}")
                playerError = error.message ?: "Unknown error"
                
                // Try fallback without PoToken (might still fail due to restrictions)
                println("Trying fallback without PoToken...")
                Innertube.player(videoId = videoId, poToken = null).onSuccess { fallbackResponse ->
                    println("Fallback succeeded (limited quality)")
                    formatAudio.value = fallbackResponse.streamingData?.autoMaxQualityFormat?.let { format ->
                        val urlWithRange = if (format.contentLength != null) {
                            "${format.url}&range=0-${format.contentLength}"
                        } else {
                            "${format.url}&range=0-10000000"
                        }
                        format.copy(url = urlWithRange)
                    }
                }.onFailure { fallbackError ->
                    println("❌ Fallback also failed: ${fallbackError.message}")
                    playerError = "Both attempts failed: ${error.message}, ${fallbackError.message}"
                }
            }
            
        } catch (e: Exception) {
            println("❌ ERROR in player request: ${e.message}")
            e.printStackTrace()
            playerError = e.message ?: "Unknown exception"
        }

        nowPlayingSong = db.getSong(videoId)
        println("Now playing song from DB: ${nowPlayingSong?.title}")
        
        if (playerError != null) {
            println("=== PLAYER ERROR ===")
            println("Video ID: $videoId")
            println("Error: $playerError")
            println("=== END ERROR ===")
        }
    }

    coroutineScope.launch {
        db.getAllSongs().collect {
            println("Songs in database: ${it.size}")
        }
    }

    val url = formatAudio.value?.url
    
    val frameController = remember(url) { VlcjFrameController() }

    var showPageSheet by remember { mutableStateOf(false) }
    var showPageType by remember { mutableStateOf(PageType.QUICKPICS) }

    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.Gray,
        topBar = {
            // Optional: Show error banner if player failed
            if (playerError != null && videoId.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, Color.Red)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Playback error: $playerError",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        bottomBar = {
            if (url != null) {
                MiniPlayer(
                    frameController = frameController,
                    url = url,
                    song = nowPlayingSong,
                    onExpandAction = { showPageSheet = true }
                )
            } else if (playerError != null && videoId.isNotEmpty()) {
                // Show error in mini player area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, Color.Red.copy(alpha = 0.5f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❌ Cannot play: ${playerError?.take(50)}...",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    ) { innerPadding ->

        ThreeColumnsLayout(
            onHomeClick = { showPageType = PageType.QUICKPICS },
            onSongClick = {
                // Reset error when new song is selected
                playerError = null
                formatAudio.value = null
                videoId = it.id
                println("ThreeColumnsApp onSongClick videoId $videoId")
            },
            onAlbumClick = {
                albumId = it.id
                showPageType = PageType.ALBUM
                showPageSheet = true
            },
            frameController = frameController,
            centerPanelContent = {
                when (showPageType) {
                    PageType.ALBUM -> {
                        AlbumScreen(
                            browseId = albumId,
                            onSongClick = {
                                playerError = null
                                formatAudio.value = null
                                videoId = it.id
                                coroutineScope.launch {
                                    db.upsert(it)
                                }
                            },
                            onAlbumClick = {
                                albumId = it
                                showPageType = PageType.ALBUM
                                showPageSheet = true
                            }
                        )
                    }

                    PageType.ARTIST -> {
                        ArtistScreen(
                            browseId = artistId,
                            onSongClick = {
                                playerError = null
                                formatAudio.value = null
                                videoId = it.id
                                coroutineScope.launch {
                                    db.upsert(it)
                                }
                            },
                            onPlaylistClick = {
                                playlistId = it
                                showPageType = PageType.PLAYLIST
                                showPageSheet = true
                            },
                            onViewAllAlbumsClick = {},
                            onViewAllSinglesClick = {},
                            onAlbumClick = {
                                albumId = it
                                showPageType = PageType.ALBUM
                                showPageSheet = true
                            },
                            onClosePage = { showPageSheet = false }
                        )
                    }

                    PageType.PLAYLIST -> {
                        PlaylistScreen(
                            browseId = playlistId,
                            onSongClick = {
                                playerError = null
                                formatAudio.value = null
                                videoId = it.id
                                coroutineScope.launch {
                                    db.upsert(it)
                                }
                            },
                            onAlbumClick = {
                                albumId = it
                                showPageType = PageType.ALBUM
                                showPageSheet = true
                            },
                            onClosePage = { showPageSheet = false }
                        )
                    }

                    PageType.MOOD -> {
                        mood?.let {
                            MoodScreen(
                                mood = it,
                                onAlbumClick = { id ->
                                    albumId = id
                                    showPageType = PageType.ALBUM
                                    showPageSheet = true
                                },
                                onArtistClick = { id ->
                                    artistId = id
                                    showPageType = PageType.ARTIST
                                    showPageSheet = true
                                },
                                onPlaylistClick = { id ->
                                    playlistId = id
                                    showPageType = PageType.PLAYLIST
                                    showPageSheet = true
                                }
                            )
                        }
                    }

                    PageType.QUICKPICS -> {
                        QuickPicsScreen(
                            onSongClick = {
                                playerError = null
                                formatAudio.value = null
                                videoId = it.id
                                coroutineScope.launch {
                                    db.upsert(it)
                                }
                                println("ThreeColumnsApp onSongClick videoId $videoId")
                            },
                            onAlbumClick = {
                                albumId = it
                                showPageType = PageType.ALBUM
                                showPageSheet = true
                            },
                            onArtistClick = {
                                artistId = it
                                showPageType = PageType.ARTIST
                                showPageSheet = true
                            },
                            onPlaylistClick = {
                                playlistId = it
                                showPageType = PageType.PLAYLIST
                                showPageSheet = true
                            },
                            onMoodClick = {
                                mood = it
                                showPageType = PageType.MOOD
                                showPageSheet = true
                            }
                        )
                    }

                    else -> {}
                }
            }
        )
    }
}

@Composable
fun ThreeColumnsLayout(
    onHomeClick: () -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    frameController: VlcjFrameController = remember { VlcjFrameController() },
    centerPanelContent: @Composable () -> Unit = {}
) {
    Row(Modifier.fillMaxSize()) {
        RightPanelContent(
            onShowPlayer = {}
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                FrameContainer(
                    Modifier.requiredHeight(200.dp),
                    frameController.size.collectAsState(null).value?.run {
                        IntSize(first, second)
                    } ?: IntSize.Zero,
                    frameController.bytes.collectAsState(null).value
                )
            }
        }

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = Color.Gray.copy(alpha = 0.6f)
        )
        CenterPanelContent(
            onHomeClick = onHomeClick,
            content = {
                centerPanelContent()
            }
        )
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = Color.Gray.copy(alpha = 0.6f)
        )
        LeftPanelContent(
            onSongClick = onSongClick,
            onAlbumClick = onAlbumClick
        )
    }
}

@Composable
fun LeftPanelContent(
    onSongClick: (Song) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding)
    ) {
        val (currentTabIndex, setCurrentTabIndex) = remember { mutableStateOf(0) }
        TabRow(
            currentTabIndex,
            modifier = Modifier
                .height(36.dp)
                .fillMaxWidth(),
            backgroundColor = Color.Gray.copy(alpha = 0.2f),
            contentColor = Color.White.copy(alpha = 0.6f)
        ) {
            Tab(
                currentTabIndex == 0,
                onClick = { setCurrentTabIndex(0) },
                text = { /* Text("") */ },
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.musical_notes),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Tab(
                currentTabIndex == 1,
                onClick = { setCurrentTabIndex(1) },
                text = { /* Text("") */ },
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.artists),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Tab(
                currentTabIndex == 2,
                onClick = { setCurrentTabIndex(2) },
                text = { /* Text("") */ },
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.album),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Tab(
                currentTabIndex == 3,
                onClick = { setCurrentTabIndex(3) },
                text = { /* Text("") */ },
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.library),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }

        when (currentTabIndex) {
            0 -> SongsPage(onSongClick = onSongClick)
            1 -> {}
            2 -> AlbumsPage(onAlbumClick = onAlbumClick)
            3 -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenterPanelContent(
    onHomeClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.7f)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding)
            .padding(bottom = layoutColumnBottomPadding)
            .verticalScroll(scrollState)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                colorFilter = ColorFilter.tint(Color.Green.copy(alpha = 0.6f)),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {}
                    )
            )
            Text(
                text = "Cubic-Music",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier
                    .clickable { onHomeClick() }
                    .padding(horizontal = 8.dp)
            )
        }

        Column(Modifier.fillMaxSize().border(1.dp, color = Color.Black)) {
            content()
            Spacer(Modifier.height(layoutColumnBottomSpacer))
        }
    }
}

@Composable
fun RightPanelContent(
    onShowPlayer: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    var showPlayer by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.23f)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding)
    ) {
        Spacer(Modifier.size(layoutColumnTopPadding))
        Row {
            content()
        }
    }
}
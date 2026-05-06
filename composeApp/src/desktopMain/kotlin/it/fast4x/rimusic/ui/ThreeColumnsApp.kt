package app.it.fast4x.rimusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.MusicDatabaseDesktop
import database.entities.Album
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.PlayerBody
import it.fast4x.innertube.requests.player
import app.it.fast4x.rimusic.enums.PageType
import app.it.fast4x.rimusic.extensions.webpotoken.PoTokenGenerator
import app.it.fast4x.rimusic.styling.Dimensions.*
import app.it.fast4x.rimusic.ui.components.MiniPlayer
import app.it.fast4x.rimusic.ui.pages.AlbumsPage
import app.it.fast4x.rimusic.ui.pages.SongsPage
import app.it.fast4x.rimusic.ui.screens.*
import app.it.fast4x.rimusic.utils.getPipedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import player.frame.FrameContainer
import rimusic.composeapp.generated.resources.*
import androidx.compose.runtime.collectAsState
import vlcj.VlcjFrameController

// ── Palette ───────────────────────────────────────────────────────────────────
private object TColor {
    val bg          = Color(0xFF0D1117)
    val panelBg     = Color(0xFF111820)
    val glass       = Color(0x991A2332)
    val glassDim    = Color(0x661E2D3D)
    val border      = Color(0xFF21303F)
    val accent      = Color(0xFF1DB954)
    val accentDim   = Color(0xFF14532D)
    val textPrimary = Color(0xFFEEF2FF)
    val textSecond  = Color(0xFF8899AA)
    val textDim     = Color(0xFF445566)
    val red         = Color(0xFFFF6B6B)
    val divider     = Color(0xFF1A2530)
}

// ── ThreeColumnsApp ───────────────────────────────────────────────────────────
@Composable
fun ThreeColumnsApp() {
    val db = remember { MusicDatabaseDesktop }
    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }

    var videoId         by remember { mutableStateOf("") }
    var nowPlayingSong  by remember { mutableStateOf<Song?>(null) }
    var artistId        by remember { mutableStateOf("") }
    var albumId         by remember { mutableStateOf("") }
    var playlistId      by remember { mutableStateOf("") }
    var mood            by remember { mutableStateOf<Innertube.Mood.Item?>(null) }
    val formatAudio     = remember { mutableStateOf<PlayerResponse.StreamingData.AdaptiveFormat?>(null) }
    var playerError     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        if (videoId.isEmpty()) return@LaunchedEffect
        playerError = null
        formatAudio.value = null

        try {
            val poToken = runCatching {
                PoTokenGenerator().getWebClientPoToken(videoId)?.playerRequestPoToken
            }.getOrNull()

            Innertube.player(videoId = videoId, poToken = poToken)
                .onSuccess { res ->
                    formatAudio.value = res.streamingData?.autoMaxQualityFormat?.let { f ->
                        val range = if (f.contentLength != null) "0-${f.contentLength}" else "0-10000000"
                        f.copy(url = "${f.url}&range=$range")
                    }
                    if (formatAudio.value == null) playerError = "No audio format available"
                }
                .onFailure { err ->
                    playerError = err.message
                    Innertube.player(videoId = videoId, poToken = null)
                        .onSuccess { res ->
                            formatAudio.value = res.streamingData?.autoMaxQualityFormat?.let { f ->
                                val range = if (f.contentLength != null) "0-${f.contentLength}" else "0-10000000"
                                f.copy(url = "${f.url}&range=$range")
                            }
                            if (formatAudio.value != null) playerError = null
                        }
                        .onFailure { playerError = "Playback failed: ${err.message}" }
                }
        } catch (e: Exception) {
            playerError = e.message ?: "Unknown error"
        }

        nowPlayingSong = db.getSong(videoId)
    }

    val url             = formatAudio.value?.url
    val frameController = remember(url) { VlcjFrameController() }

    var showPageType    by remember { mutableStateOf(PageType.QUICKPICS) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TColor.bg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Error banner
            AnimatedVisibility(visible = playerError != null && videoId.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TColor.red.copy(alpha = 0.12f))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text     = "⚠ ${playerError}",
                        color    = TColor.red,
                        fontSize = 12.sp
                    )
                }
            }

            // Main 3-column row
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ThreeColumnsLayout(
                    onHomeClick    = { showPageType = PageType.QUICKPICS },
                    onSongClick    = {
                        playerError   = null
                        formatAudio.value = null
                        videoId       = it.id
                    },
                    onAlbumClick   = {
                        albumId       = it.id
                        showPageType  = PageType.ALBUM
                    },
                    frameController = frameController,
                    centerPanelContent = {
                        when (showPageType) {
                            PageType.ALBUM -> AlbumScreen(
                                browseId     = albumId,
                                onSongClick  = { s ->
                                    playerError = null; formatAudio.value = null; videoId = s.id
                                    coroutineScope.launch { db.upsert(s) }
                                },
                                onAlbumClick = { id -> albumId = id; showPageType = PageType.ALBUM }
                            )
                            PageType.ARTIST -> ArtistScreen(
                                browseId            = artistId,
                                onSongClick         = { s ->
                                    playerError = null; formatAudio.value = null; videoId = s.id
                                    coroutineScope.launch { db.upsert(s) }
                                },
                                onPlaylistClick     = { id -> playlistId = id; showPageType = PageType.PLAYLIST },
                                onViewAllAlbumsClick = {},
                                onViewAllSinglesClick = {},
                                onAlbumClick        = { id -> albumId = id; showPageType = PageType.ALBUM },
                                onClosePage         = { showPageType = PageType.QUICKPICS }
                            )
                            PageType.PLAYLIST -> PlaylistScreen(
                                browseId    = playlistId,
                                onSongClick = { s ->
                                    playerError = null; formatAudio.value = null; videoId = s.id
                                    coroutineScope.launch { db.upsert(s) }
                                },
                                onAlbumClick = { id -> albumId = id; showPageType = PageType.ALBUM },
                                onClosePage = { showPageType = PageType.QUICKPICS }
                            )
                            PageType.MOOD -> mood?.let { m ->
                                MoodScreen(
                                    mood           = m,
                                    onAlbumClick   = { id -> albumId = id; showPageType = PageType.ALBUM },
                                    onArtistClick  = { id -> artistId = id; showPageType = PageType.ARTIST },
                                    onPlaylistClick = { id -> playlistId = id; showPageType = PageType.PLAYLIST }
                                )
                            }
                            PageType.QUICKPICS -> QuickPicsScreen(
                                onSongClick = { s ->
                                    playerError = null; formatAudio.value = null; videoId = s.id
                                    coroutineScope.launch { db.upsert(s) }
                                },
                                onAlbumClick    = { id -> albumId = id; showPageType = PageType.ALBUM },
                                onArtistClick   = { id -> artistId = id; showPageType = PageType.ARTIST },
                                onPlaylistClick = { id -> playlistId = id; showPageType = PageType.PLAYLIST },
                                onMoodClick     = { m -> mood = m; showPageType = PageType.MOOD }
                            )
                            else -> {}
                        }
                    }
                )
            }

            // Bottom mini-player
            AnimatedVisibility(
                visible = url != null || (playerError != null && videoId.isNotEmpty()),
                enter   = slideInVertically(initialOffsetY = { it }) + fadeIn()
            ) {
                if (url != null) {
                    GlassyMiniPlayerBar(
                        frameController = frameController,
                        url             = url,
                        song            = nowPlayingSong,
                        error           = null
                    )
                } else if (playerError != null) {
                    ErrorPlayerBar(error = playerError!!)
                }
            }

            // Empty state bottom bar
            if (url == null && (playerError == null || videoId.isEmpty())) {
                EmptyPlayerBar()
            }
        }
    }
}

// ── Three columns layout ──────────────────────────────────────────────────────
@Composable
fun ThreeColumnsLayout(
    onHomeClick        : () -> Unit           = {},
    onSongClick        : (Song) -> Unit       = {},
    onAlbumClick       : (Album) -> Unit      = {},
    frameController    : VlcjFrameController  = remember { VlcjFrameController() },
    centerPanelContent : @Composable () -> Unit = {}
) {
    Row(Modifier.fillMaxSize()) {
        // Right panel — video/art preview
        RightPanel(frameController = frameController)

        GlassyDivider()

        // Center panel — main content
        CenterPanel(onHomeClick = onHomeClick) { centerPanelContent() }

        GlassyDivider()

        // Left panel — library
        LeftPanel(onSongClick = onSongClick, onAlbumClick = onAlbumClick)
    }
}

// ── Right panel ───────────────────────────────────────────────────────────────
@Composable
fun RightPanel(frameController: VlcjFrameController) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.23f)
            .background(TColor.panelBg)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding),
        verticalArrangement     = Arrangement.Top,
        horizontalAlignment     = Alignment.Start
    ) {
        Spacer(Modifier.size(layoutColumnTopPadding))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TColor.glass)
                .aspectRatio(1f)
                .drawBehind {
                    drawRoundRect(
                        color        = TColor.border,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            FrameContainer(
                modifier      = Modifier.requiredHeight(200.dp),
                size          = frameController.size.collectAsState(null).value?.run {
                    IntSize(first, second)
                } ?: IntSize.Zero,
                bytes         = frameController.bytes.collectAsState(null).value
            )
        }
    }
}

// ── Center panel ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenterPanel(onHomeClick: () -> Unit = {}, content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.7f)
            .background(TColor.bg)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding, bottom = layoutColumnBottomPadding)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Brand header
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(listOf(TColor.accent, Color(0xFF0A7A3A))),
                        CircleShape
                    )
                    .combinedClickable(onClick = {}, onLongClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text       = "RiMusic",
                color      = TColor.textPrimary,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.clickable { onHomeClick() }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TColor.glass)
                .drawBehind {
                    drawRoundRect(
                        color        = TColor.border,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                    )
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                content()
                Spacer(Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}

// ── Left panel (library tabs) ─────────────────────────────────────────────────
@Composable
fun LeftPanel(onSongClick: (Song) -> Unit = {}, onAlbumClick: (Album) -> Unit = {}) {
    val (currentTab, setTab) = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TColor.panelBg)
            .padding(horizontal = layoutColumnsHorizontalPadding)
            .padding(top = layoutColumnTopPadding),
        verticalArrangement     = Arrangement.Top,
        horizontalAlignment     = Alignment.Start
    ) {
        // Glassy tab row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TColor.glass)
        ) {
            TabRow(
                selectedTabIndex = currentTab,
                modifier         = Modifier.fillMaxSize(),
                backgroundColor  = Color.Transparent,
                contentColor     = TColor.accent,
                indicator        = { /* custom indicator */ },
                divider          = {}
            ) {
                listOf("♫" to "Songs", "🎤" to "Artists", "💿" to "Albums", "📚" to "Library").forEachIndexed { idx, (icon, label) ->
                    LibraryTab(
                        icon     = icon,
                        label    = label,
                        selected = currentTab == idx,
                        onClick  = { setTab(idx) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (currentTab) {
            0 -> SongsPage(onSongClick = onSongClick)
            2 -> AlbumsPage(onAlbumClick = onAlbumClick)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Coming soon", color = TColor.textDim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LibraryTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Tab(
        selected          = selected,
        onClick           = onClick,
        modifier          = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected  -> TColor.accentDim
                    isHovered -> TColor.glassDim
                    else      -> Color.Transparent
                }
            )
            .hoverable(interactionSource),
        selectedContentColor   = TColor.accent,
        unselectedContentColor = TColor.textDim,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 16.sp)
                Text(label, fontSize = 9.sp)
            }
        }
    )
}

// ── Player bars ───────────────────────────────────────────────────────────────

@Composable
private fun GlassyMiniPlayerBar(
    frameController : VlcjFrameController,
    url             : String,
    song            : Song?,
    error           : String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Color(0xEE0D1117), Color(0xEE111C28), Color(0xEE0D1117)))
            )
            .drawBehind {
                drawLine(
                    color       = TColor.border,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // Green progress glow at top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, TColor.accent.copy(0.7f), Color.Transparent))
                )
        )
        MiniPlayer(
            frameController = frameController,
            frameRenderer   = frameController,
            url             = url,
            song            = song,
            onExpandAction  = {}
        )
    }
}

@Composable
private fun ErrorPlayerBar(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x22FF4444))
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .drawBehind {
                drawLine(TColor.red.copy(0.4f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("⚠", fontSize = 18.sp)
            Text(
                text     = "Cannot play: ${error.take(80)}",
                color    = TColor.red,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyPlayerBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xDD0D1117))
            .padding(vertical = 14.dp)
            .drawBehind {
                drawLine(TColor.border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔒", fontSize = 14.sp)
            Text("Select a track to start playing", color = TColor.textDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GlassyDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(TColor.divider)
    )
}

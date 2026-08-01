package app.it.fast4x.rimusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.it.fast4x.rimusic.ui.desktop.CubicAlbumsPage
import app.it.fast4x.rimusic.ui.desktop.CubicArtistsPage
import app.it.fast4x.rimusic.ui.desktop.CubicBrowsePage
import app.it.fast4x.rimusic.ui.desktop.CubicColors
import app.it.fast4x.rimusic.ui.desktop.CubicDesktopTheme
import app.it.fast4x.rimusic.ui.desktop.CubicDiscoveryData
import app.it.fast4x.rimusic.ui.desktop.CubicDiscoveryRepository
import app.it.fast4x.rimusic.ui.desktop.CubicEmptyState
import app.it.fast4x.rimusic.ui.desktop.CubicExpandedPlayerDialog
import app.it.fast4x.rimusic.ui.desktop.CubicNowPlayingPanel
import app.it.fast4x.rimusic.ui.desktop.CubicPlayerRail
import app.it.fast4x.rimusic.ui.desktop.CubicPlaylistsPage
import app.it.fast4x.rimusic.ui.desktop.CubicRoutes
import app.it.fast4x.rimusic.ui.desktop.CubicSidebar
import app.it.fast4x.rimusic.ui.desktop.CubicSongCollection
import app.it.fast4x.rimusic.ui.desktop.CubicSongsPage
import app.it.fast4x.rimusic.ui.desktop.CubicTopBar
import app.it.fast4x.rimusic.ui.screens.AlbumScreen
import app.it.fast4x.rimusic.ui.screens.MoodScreen
import app.it.fast4x.rimusic.ui.screens.PlaylistScreen
import app.it.fast4x.rimusic.ui.screens.SearchScreen
import database.DB
import database.entities.Song
import database.entities.SongEntity
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.utils.NewPipeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import vlcj.VlcjController

@Composable
fun CubicDesktopApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CubicRoutes.Browse
    val controller = remember { VlcjController() }
    val desktopHttpClient = remember { OkHttpClient() }
    val database = remember { DB }
    val scope = rememberCoroutineScope()

    val librarySongs by remember(database) { database.songsByTitleAsc() }.collectAsState(initial = emptyList())
    val libraryAlbums by remember(database) { database.getAllAlbums() }.collectAsState(initial = emptyList())
    val sessionHistory = remember { mutableStateListOf<Song>() }

    var currentSong by remember { mutableStateOf<Song?>(null) }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var showExpandedPlayer by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<Innertube.Mood.Item?>(null) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }

    var discovery by remember { mutableStateOf<CubicDiscoveryData?>(null) }
    var discoveryLoading by remember { mutableStateOf(true) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    var discoveryRefresh by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        NewPipeUtils.init { desktopHttpClient }
        onDispose { controller.dispose() }
    }

    LaunchedEffect(activeStreamUrl) {
        val url = activeStreamUrl ?: return@LaunchedEffect
        controller.load(url)
        controller.play()
    }

    LaunchedEffect(discoveryRefresh) {
        discoveryLoading = true
        discoveryError = null
        CubicDiscoveryRepository
            .load(force = discoveryRefresh > 0, seedVideoId = currentSong?.id ?: "HZnNt9nnEhw")
            .onSuccess { discovery = it }
            .onFailure { discoveryError = it.message ?: "The live catalog is unavailable." }
        discoveryLoading = false
    }

    fun navigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openSearch() {
        val normalized = searchQuery.trim()
        if (normalized.isNotEmpty()) {
            activeSearchQuery = normalized
            navigate(CubicRoutes.Search)
        }
    }

    fun playSong(song: Song) {
        val selectedSong = librarySongs.firstOrNull { it.song.id == song.id }?.song ?: song
        currentSong = selectedSong
        sessionHistory.removeAll { it.id == selectedSong.id }
        sessionHistory.add(0, selectedSong)

        scope.launch {
            playbackMessage = null
            isResolving = true
            activeStreamUrl = null
            val url = resolveCubicDesktopPlaybackUrl(selectedSong.id)
            if (url != null) {
                activeStreamUrl = url
                playbackMessage = "Playing"
            } else {
                playbackMessage = "Couldn't resolve stream for ${selectedSong.title}"
            }
            isResolving = false
        }
    }

    fun toggleFavorite() {
        val song = currentSong ?: return
        val updated = song.toggleLike()
        currentSong = updated
        val historyIndex = sessionHistory.indexOfFirst { it.id == updated.id }
        if (historyIndex >= 0) sessionHistory[historyIndex] = updated
        scope.launch { database.upsert(updated) }
    }

    CubicDesktopTheme {
        Box(Modifier.fillMaxSize().background(CubicColors.Background)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(CubicColors.Window)
                ) {
                    val showNowPlaying = maxWidth >= 1220.dp
                    Row(Modifier.fillMaxSize()) {
                        CubicSidebar(currentRoute = currentRoute, onNavigate = ::navigate)

                        Column(Modifier.weight(1f).fillMaxSize()) {
                            CubicTopBar(
                                currentRoute = currentRoute,
                                canGoBack = currentRoute !in CubicRoutes.primary,
                                searchQuery = searchQuery,
                                hasLibrarySongs = librarySongs.isNotEmpty(),
                                onBack = { navController.popBackStack() },
                                onSearchQueryChange = { searchQuery = it },
                                onSearch = ::openSearch,
                                onNavigate = ::navigate,
                                onShuffle = { librarySongs.randomOrNull()?.song?.let(::playSong) }
                            )

                            Box(Modifier.weight(1f).fillMaxWidth().background(CubicColors.Window)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = CubicRoutes.Browse,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(CubicRoutes.Browse) {
                                        CubicBrowsePage(
                                            data = discovery,
                                            isLoading = discoveryLoading,
                                            error = discoveryError,
                                            onRetry = { discoveryRefresh++ },
                                            onSongClick = ::playSong,
                                            onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) },
                                            onArtistClick = { selectedArtistId = it; navigate(CubicRoutes.Artist) },
                                            onPlaylistClick = { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) },
                                            onMoodClick = { selectedMood = it; navigate(CubicRoutes.Mood) }
                                        )
                                    }
                                    composable(CubicRoutes.Songs) {
                                        CubicSongsPage(librarySongs, CubicSongCollection.All, currentSong?.id, ::playSong)
                                    }
                                    composable(CubicRoutes.Recent) {
                                        CubicSongsPage(sessionHistory.map { SongEntity(it) }, CubicSongCollection.Recent, currentSong?.id, ::playSong)
                                    }
                                    composable(CubicRoutes.Favorites) {
                                        CubicSongsPage(librarySongs, CubicSongCollection.Favorites, currentSong?.id, ::playSong)
                                    }
                                    composable(CubicRoutes.Albums) {
                                        CubicAlbumsPage(libraryAlbums) { selectedAlbumId = it; navigate(CubicRoutes.Album) }
                                    }
                                    composable(CubicRoutes.Artists) {
                                        CubicArtistsPage(discovery, discoveryLoading, discoveryError, { discoveryRefresh++ }) {
                                            selectedArtistId = it
                                            navigate(CubicRoutes.Artist)
                                        }
                                    }
                                    composable(CubicRoutes.Playlists) {
                                        CubicPlaylistsPage(discovery, discoveryLoading, discoveryError, { discoveryRefresh++ }) {
                                            selectedPlaylistId = it
                                            navigate(CubicRoutes.Playlist)
                                        }
                                    }
                                    composable(CubicRoutes.Search) {
                                        SearchScreen(
                                            query = activeSearchQuery,
                                            onSongClick = ::playSong,
                                            onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) },
                                            onArtistClick = { selectedArtistId = it; navigate(CubicRoutes.Artist) },
                                            onPlaylistClick = { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }
                                        )
                                    }
                                    composable(CubicRoutes.Album) {
                                        AlbumScreen(
                                            browseId = selectedAlbumId.orEmpty(),
                                            onSongClick = ::playSong,
                                            onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) }
                                        )
                                    }
                                    composable(CubicRoutes.Artist) {
                                        DesktopArtistRoute(
                                            browseId = selectedArtistId.orEmpty(),
                                            onSongClick = ::playSong,
                                            onPlaylistClick = { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) },
                                            onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) }
                                        )
                                    }
                                    composable(CubicRoutes.Playlist) {
                                        PlaylistScreen(
                                            browseId = selectedPlaylistId.orEmpty(),
                                            onSongClick = ::playSong,
                                            onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) },
                                            onClosePage = { navController.popBackStack() }
                                        )
                                    }
                                    composable(CubicRoutes.Mood) {
                                        selectedMood?.let { mood ->
                                            MoodScreen(
                                                mood = mood,
                                                onAlbumClick = { selectedAlbumId = it; navigate(CubicRoutes.Album) },
                                                onArtistClick = { selectedArtistId = it; navigate(CubicRoutes.Artist) },
                                                onPlaylistClick = { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }
                                            )
                                        } ?: CubicEmptyState("No mood selected", "Return to Browse and choose a mood.")
                                    }
                                }
                            }
                        }

                        if (showNowPlaying) {
                            CubicNowPlayingPanel(
                                currentSong = currentSong,
                                isResolving = isResolving,
                                playbackMessage = playbackMessage,
                                recentSongs = sessionHistory.map { SongEntity(it) },
                                onToggleFavorite = ::toggleFavorite,
                                onSongClick = ::playSong,
                                onOpenPlayer = { currentSong?.let { showExpandedPlayer = true } }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CubicPlayerRail(
                        controller = controller,
                        currentSong = currentSong,
                        isResolving = isResolving,
                        playbackMessage = playbackMessage,
                        onOpenPlayer = { currentSong?.let { showExpandedPlayer = true } },
                        modifier = Modifier.fillMaxWidth(0.78f)
                    )
                }
            }

            val expandedSong = currentSong
            if (showExpandedPlayer && expandedSong != null) {
                CubicExpandedPlayerDialog(
                    controller = controller,
                    currentSong = expandedSong,
                    isResolving = isResolving,
                    playbackMessage = playbackMessage,
                    onToggleFavorite = ::toggleFavorite,
                    onDismiss = { showExpandedPlayer = false }
                )
            }
        }
    }
}

private suspend fun resolveCubicDesktopPlaybackUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    resolveCubicInnertubePlaybackUrl(videoId)
}

private suspend fun resolveCubicInnertubePlaybackUrl(videoId: String): String? {
    val responses = mutableListOf<PlayerResponse?>()
    responses += runCatching {
        Innertube.player(videoId = videoId)?.getOrNull()
    }.getOrNull()

    return responses
        .asSequence()
        .filterNotNull()
        .mapNotNull { response ->
            orderedCubicDesktopInnertubeFormats(response.streamingData)
                .firstNotNullOfOrNull { format ->
                    NewPipeUtils.getStreamUrl(format, videoId)
                        .getOrNull()
                        ?.withCubicDesktopPlaybackRange(format.contentLengthValue)
                }
        }
        .firstOrNull()
}

private fun orderedCubicDesktopInnertubeFormats(
    streamingData: PlayerResponse.StreamingData?
): List<PlayerResponse.StreamingData.Format> {
    val formats = streamingData?.adaptiveFormats
        ?.filter { format ->
            format.isAudio &&
                format.hasCubicPlayableSource &&
                format.mimeType.contains("audio", ignoreCase = true)
        }
        .orEmpty()
    if (formats.isEmpty()) return emptyList()

    val preferred = sequenceOf(
        streamingData?.highestQualityFormat,
        streamingData?.mediumQualityFormat,
        streamingData?.lowestQualityFormat,
        streamingData?.autoMaxQualityFormat
    )
        .filterNotNull()
        .filter { candidate ->
            candidate.isAudio &&
                candidate.hasCubicPlayableSource &&
                candidate.mimeType.contains("audio", ignoreCase = true)
        }
        .toList()

    return (preferred + formats.sortedByDescending { it.bitrateValue ?: 0 }).distinctBy { it.itagValue }
}

private fun String.withCubicDesktopPlaybackRange(contentLength: Long?): String {
    val separator = if ('?' in this) "&" else "?"
    val safeRangeEnd = contentLength?.takeIf { it > 0 } ?: 10_000_000L
    return "$this${separator}range=0-$safeRangeEnd"
}

private val PlayerResponse.StreamingData.Format.hasCubicPlayableSource: Boolean
    get() = !url.isNullOrBlank() || !signatureCipher.isNullOrBlank()

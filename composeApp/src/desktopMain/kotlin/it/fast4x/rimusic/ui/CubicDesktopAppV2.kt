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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import app.it.fast4x.rimusic.ui.desktop.CubicAlbumDetailPage
import app.it.fast4x.rimusic.net.CubicRangeTransfer
import app.it.fast4x.rimusic.net.attachCubicPlaybackIdentity
import app.it.fast4x.rimusic.net.cubicDesktopPlaybackClients
import app.it.fast4x.rimusic.net.resolveCubicWebPoTokenStream
import app.it.fast4x.rimusic.ui.desktop.CubicAlbumsPage
import app.it.fast4x.rimusic.ui.desktop.CubicArtistDetailPage
import app.it.fast4x.rimusic.ui.desktop.CubicArtistsPage
import app.it.fast4x.rimusic.ui.desktop.CubicLiveSearchPage
import app.it.fast4x.rimusic.ui.desktop.CubicRichBrowsePage
import app.it.fast4x.rimusic.ui.desktop.CubicColors
import app.it.fast4x.rimusic.ui.desktop.CubicDesktopTheme
import app.it.fast4x.rimusic.ui.desktop.CubicDiscoveryData
import app.it.fast4x.rimusic.ui.desktop.CubicDiscoveryRepository
import app.it.fast4x.rimusic.ui.desktop.CubicDownloadStore
import app.it.fast4x.rimusic.ui.desktop.CubicNewReleasesPage
import app.it.fast4x.rimusic.ui.desktop.CubicEmptyState
import app.it.fast4x.rimusic.ui.desktop.CubicExpandedPlayerDialogV2
import app.it.fast4x.rimusic.ui.desktop.CubicNowPlayingPanelV2
import app.it.fast4x.rimusic.ui.desktop.CubicMiniPlayer
import app.it.fast4x.rimusic.ui.desktop.CubicPlayerRailV2
import app.it.fast4x.rimusic.ui.desktop.CubicPlaylistsPage
import app.it.fast4x.rimusic.ui.desktop.CubicProfilePage
import app.it.fast4x.rimusic.ui.desktop.CubicRoutes
import app.it.fast4x.rimusic.ui.desktop.CubicSearchPage
import app.it.fast4x.rimusic.ui.desktop.CubicSettingsPage
import app.it.fast4x.rimusic.ui.desktop.CubicSidebar
import app.it.fast4x.rimusic.ui.desktop.CubicSongCollection
import app.it.fast4x.rimusic.ui.desktop.CubicSongsPage
import app.it.fast4x.rimusic.ui.desktop.CubicSongsDiscoveryPage
import app.it.fast4x.rimusic.ui.desktop.CubicUserPlaylistsPage
import app.it.fast4x.rimusic.ui.desktop.CubicPlaylistStore
import app.it.fast4x.rimusic.ui.desktop.CubicTasteStore
import app.it.fast4x.rimusic.ui.desktop.CubicTopBar
import app.it.fast4x.rimusic.ui.screens.MoodScreen
import app.it.fast4x.rimusic.ui.screens.PlaylistScreen
import app.it.fast4x.rimusic.utils.asSong
import database.DB
import database.entities.Song
import database.entities.SongEntity
import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.requests.relatedPage
import it.fast4x.innertube.utils.NewPipeUtils
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import windows.FfmpegAudioController

@Composable
fun CubicDesktopAppV2(windowState: WindowState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CubicRoutes.Browse
    val playbackHttpClient = remember { OkHttpClient() }
    val downloadHttpClient = remember { OkHttpClient() }
    val controller = remember { FfmpegAudioController() }
    val playerState by controller.state.collectAsState()
    val database = remember { DB }
    val scope = rememberCoroutineScope()

    val librarySongs by remember(database) { database.songsByTitleAsc() }.collectAsState(initial = emptyList())
    val libraryAlbums by remember(database) { database.getAllAlbums() }.collectAsState(initial = emptyList())
    val sessionHistory = remember { mutableStateListOf<Song>() }
    val playbackQueue = remember { mutableStateListOf<Song>() }
    val downloadProgress = remember { mutableStateMapOf<String, Float>() }

    var currentSong by remember { mutableStateOf<Song?>(null) }
    var currentQueueIndex by remember { mutableIntStateOf(-1) }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var showExpandedPlayer by remember { mutableStateOf(false) }
    var localPlaylists by remember { mutableStateOf(CubicPlaylistStore.names()) }
    var downloadedIds by remember { mutableStateOf(CubicDownloadStore.downloadedSongIds()) }
    var syncedLyrics by remember { mutableStateOf<String?>(null) }
    var plainLyrics by remember { mutableStateOf<String?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var completionHandledFor by remember { mutableStateOf<String?>(null) }
    var playbackRetryCount by remember { mutableIntStateOf(0) }
    var pendingResumePosition by remember { mutableStateOf(0L) }

    var selectedMood by remember { mutableStateOf<Innertube.Mood.Item?>(null) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var isRadioExpanding by remember { mutableStateOf(false) }
    var miniMode by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }

    var discovery by remember { mutableStateOf<CubicDiscoveryData?>(null) }
    var discoveryLoading by remember { mutableStateOf(true) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    var discoveryRefresh by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        NewPipeUtils.init { playbackHttpClient }
        controller.onStreamFailure = { failedUrl, positionMs ->
            scope.launch {
                val song = currentSong
                if (song == null || activeStreamUrl != failedUrl || isResolving || playbackRetryCount >= 2 || !failedUrl.startsWith("http")) return@launch
                playbackRetryCount++
                markCubicDesktopStreamClientFailed(song.id, failedUrl)
                isResolving = true
                playbackMessage = "Refreshing stream..."
                activeStreamUrl = null
                val refreshed = resolveCubicDesktopPlaybackUrlV2(song.id, playbackHttpClient)
                if (refreshed != null) {
                    pendingResumePosition = positionMs
                    activeStreamUrl = refreshed
                    playbackMessage = "Playing"
                } else {
                    playbackMessage = "Playback failed - retry the track"
                }
                isResolving = false
            }
        }
        onDispose {
            controller.onStreamFailure = null
            controller.dispose()
        }
    }

    LaunchedEffect(librarySongs) {
        if (librarySongs.isNotEmpty() && sessionHistory.isEmpty()) {
            val byId = librarySongs.associateBy { it.song.id }
            sessionHistory.addAll(CubicTasteStore.ids().mapNotNull { byId[it]?.song })
        }
    }

    LaunchedEffect(windowState.isMinimized) {
        if (windowState.isMinimized) {
            miniMode = true
            // Windows ignores floating size changes while a maximized window is still minimized.
            // Restore first, then leave maximized placement before applying the compact bounds.
            windowState.isMinimized = false
            delay(90)
            windowState.placement = WindowPlacement.Floating
            delay(50)
            windowState.size = DpSize(460.dp, 118.dp)
            windowState.position = WindowPosition(Alignment.BottomEnd)
        }
    }

    LaunchedEffect(activeStreamUrl) {
        activeStreamUrl?.let { url ->
            val resumeAt = pendingResumePosition
            pendingResumePosition = 0L
            controller.loadAt(url, resumeAt)
            controller.play()
        }
    }

    LaunchedEffect(discoveryRefresh, currentSong?.id) {
        discoveryLoading = true
        discoveryError = null
        val personalSeeds = buildList {
            currentSong?.id?.let(::add)
            addAll(sessionHistory.map { it.id })
            addAll(CubicTasteStore.ids())
            addAll(librarySongs.filter { it.song.isLiked }.map { it.song.id })
        }.filter(String::isNotBlank).distinct()
        val seed = personalSeeds.getOrNull(discoveryRefresh.mod(personalSeeds.size.coerceAtLeast(1))) ?: "HZnNt9nnEhw"
        CubicDiscoveryRepository.load(force = discoveryRefresh > 0 || personalSeeds.isNotEmpty(), seedVideoId = seed)
            .onSuccess { discovery = it }
            .onFailure { discoveryError = it.message ?: "The live catalog is unavailable." }
        discoveryLoading = false
    }

    LaunchedEffect(currentSong?.id) {
        val song = currentSong
        syncedLyrics = null
        plainLyrics = null
        lyricsLoading = song != null
        if (song != null) {
            val artist = song.artistsText.orEmpty().substringBefore(',').trim()
            if (artist.isNotBlank()) {
                val matches = LrcLib.lyrics(artist, song.title)?.getOrNull().orEmpty()
                syncedLyrics = matches.firstNotNullOfOrNull { it.syncedLyrics?.takeIf(String::isNotBlank) }
                plainLyrics = matches.firstNotNullOfOrNull { it.plainLyrics?.takeIf(String::isNotBlank) }
            }
        }
        lyricsLoading = false
    }

    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true; restoreState = true }
    }

    fun openSearch() {
        searchQuery.trim().takeIf(String::isNotEmpty)?.let {
            activeSearchQuery = it
        }
    }

    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 2) {
            delay(250)
            activeSearchQuery = query
        }
    }

    fun startSong(song: Song, queueIndex: Int) {
        currentSong = librarySongs.firstOrNull { it.song.id == song.id }?.song ?: song
        activeStreamUrl = null
        isResolving = true
        controller.stop()
        controller.setExpectedDuration(currentSong?.durationText.cubicDurationMillis())
        currentQueueIndex = queueIndex
        completionHandledFor = null
        playbackRetryCount = 0
        pendingResumePosition = 0L
        sessionHistory.removeAll { it.id == song.id }
        sessionHistory.add(0, currentSong!!)
        scope.launch { database.upsert(currentSong!!) }
        scope.launch {
        CubicTasteStore.record(song.id)
            playbackMessage = null
            val cached = CubicDownloadStore.localFile(song.id)
            val resolved = cached?.takeIf { it.exists() }?.toURI()?.toString()
                ?: resolveCubicDesktopPlaybackUrlV2(song.id, playbackHttpClient)
            if (resolved != null) {
                activeStreamUrl = resolved
                playbackMessage = if (cached != null) "Playing offline" else "Playing"
            } else {
                playbackMessage = "Stream unavailable Ã¢â‚¬â€ try another track"
            }
            isResolving = false
        }
    }

    fun playSong(song: Song) {
        playbackQueue.clear()
        playbackQueue.add(song)
        startSong(song, 0)
    }


    fun playSongsAsQueue(songs: List<Song>, startIndex: Int = 0) {
        val unique = songs.distinctBy { it.id }
        if (unique.isEmpty()) return
        playbackQueue.clear()
        playbackQueue.addAll(unique)
        val safeIndex = startIndex.coerceIn(playbackQueue.indices)
        startSong(playbackQueue[safeIndex], safeIndex)
    }
    fun playQueueIndex(index: Int) {
        playbackQueue.getOrNull(index)?.let { startSong(it, index) }
    }

    fun playPrevious() = playQueueIndex(currentQueueIndex - 1)
    fun playNext() = playQueueIndex(currentQueueIndex + 1)

    fun toggleFavorite() {
        val updated = currentSong?.toggleLike() ?: return
        currentSong = updated
        playbackQueue.indexOfFirst { it.id == updated.id }.takeIf { it >= 0 }?.let { playbackQueue[it] = updated }
        sessionHistory.indexOfFirst { it.id == updated.id }.takeIf { it >= 0 }?.let { sessionHistory[it] = updated }
        scope.launch { database.upsert(updated) }
    }

    fun addSongToPlaylist(song: Song, playlist: String) {
        CubicPlaylistStore.addSong(playlist, song.id)
        scope.launch { database.upsert(song) }
        playbackMessage = "Added to $playlist"
        localPlaylists = CubicPlaylistStore.names()
    }

    fun clearDesktopData() {
        controller.stop()
        currentSong = null
        activeStreamUrl = null
        isResolving = false
        playbackQueue.clear()
        sessionHistory.clear()
        currentQueueIndex = -1
        showExpandedPlayer = false
        syncedLyrics = null
        plainLyrics = null
        scope.launch {
            librarySongs.forEach { database.delete(it.song) }
            withContext(Dispatchers.IO) {
                CubicDownloadStore.clear()
                CubicTasteStore.clear()
                CubicPlaylistStore.clear()
            }
            downloadedIds = emptySet()
            localPlaylists = emptyList()
            playbackMessage = "Desktop data cleared"
        }
    }

    fun downloadSong(song: Song) {
        if (song.id in downloadedIds || downloadProgress.containsKey(song.id)) return
        downloadProgress[song.id] = 0f
        scope.launch {
            val streamUrl = resolveCubicDesktopDownloadUrlV2(song.id, downloadHttpClient)
            val result = if (streamUrl == null) Result.failure(IllegalStateException("No downloadable stream was returned."))
            else CubicDownloadStore.download(downloadHttpClient, song, streamUrl) { progress ->
                scope.launch { downloadProgress[song.id] = progress }
            }
            result.onSuccess {
                database.upsert(song)
                downloadedIds = downloadedIds + song.id
                playbackMessage = if (currentSong?.id == song.id) "Downloaded for offline playback" else playbackMessage
            }.onFailure {
                playbackMessage = "Download failed: ${it.message ?: "unknown error"}"
            }
            downloadProgress.remove(song.id)
        }
    }

    LaunchedEffect(currentSong?.id, currentQueueIndex, playbackQueue.size) {
        val seed = currentSong?.id
        val shouldExpand = seed != null && currentQueueIndex >= playbackQueue.lastIndex - 3 && !isRadioExpanding
        if (shouldExpand) {
            isRadioExpanding = true
            try {
                val additions = Innertube.relatedPage(NextBody(videoId = seed))?.getOrNull()?.songs.orEmpty()
                    .map { it.asSong }
                    .filter { candidate -> playbackQueue.none { it.id == candidate.id } }
                    .distinctBy { it.id }
                playbackQueue.addAll(additions)
            } finally {
                isRadioExpanding = false
            }
        }
    }

    LaunchedEffect(playerState.isPlaying, playerState.timestamp, playerState.duration, currentSong?.id, isResolving, activeStreamUrl) {
        val songId = currentSong?.id
        if (songId != null && !isResolving && activeStreamUrl != null && !playerState.isPlaying && playerState.duration > 0 &&
            playerState.timestamp >= playerState.duration - 900 && completionHandledFor != songId && currentQueueIndex < playbackQueue.lastIndex
        ) {
            completionHandledFor = songId
            playNext()
        }

    }
    val cachedSongs = librarySongs.filter { it.song.id in downloadedIds }

    CubicDesktopTheme {
        Box(Modifier.fillMaxSize().background(CubicColors.Background)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        if (miniMode) {
            CubicMiniPlayer(controller, currentSong, isResolving, currentQueueIndex in 0 until playbackQueue.lastIndex, ::playNext) { miniMode = false; windowState.placement = WindowPlacement.Maximized }
        } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CubicColors.Window)) {
                    val showNowPlaying = maxWidth >= 1220.dp
                    Row(Modifier.fillMaxSize()) {
                        CubicSidebar(currentRoute, ::navigate)
                        Column(Modifier.weight(1f).fillMaxSize()) {
                            CubicTopBar(
                                currentRoute, currentRoute !in CubicRoutes.primary, searchQuery, librarySongs.isNotEmpty(),
                                                                onBack = { navController.popBackStack() }, onSearchQueryChange = { searchQuery = it }, onSearch = ::openSearch,
                                onNavigate = ::navigate, onShuffle = { librarySongs.randomOrNull()?.song?.let(::playSong) }
                            )
                            Box(Modifier.weight(1f).fillMaxWidth().background(CubicColors.Window)) {
                                val liveQuery = searchQuery.trim()
                                if (liveQuery.length >= 2) {
                                    CubicLiveSearchPage(
                                        query = liveQuery,
                                        onSongClick = ::playSong,
                                        onAlbumClick = { selectedAlbumId = it; searchQuery = ""; navigate(CubicRoutes.Album) },
                                        onArtistClick = { selectedArtistId = it; searchQuery = ""; navigate(CubicRoutes.Artist) },
                                        onPlaylistClick = { selectedPlaylistId = it; searchQuery = ""; navigate(CubicRoutes.Playlist) }
                                    )
                                } else NavHost(navController, CubicRoutes.Browse, Modifier.fillMaxSize()) {
                                    composable(CubicRoutes.Browse) {
                                        CubicRichBrowsePage(discovery, discoveryLoading, discoveryError, { discoveryRefresh++ }, ::playSong, { songs -> playSongsAsQueue(songs) },
                                            { selectedAlbumId = it; navigate(CubicRoutes.Album) },
                                            { selectedArtistId = it; navigate(CubicRoutes.Artist) },
                                            { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) },
                                            { selectedMood = it; navigate(CubicRoutes.Mood) })
                                    }
                                    composable(CubicRoutes.Songs) { CubicSongsDiscoveryPage(discovery, discoveryLoading, discoveryError, librarySongs, { discoveryRefresh++ }, ::playSong, { selectedArtistId = it; navigate(CubicRoutes.Artist) }) { songs -> playSongsAsQueue(songs) } }
                                    composable(CubicRoutes.NewReleases) { CubicNewReleasesPage(discovery, discoveryLoading, discoveryError, { discoveryRefresh++ }) { selectedAlbumId = it; navigate(CubicRoutes.Album) } }
                                    composable(CubicRoutes.Recent) { CubicSongsPage(sessionHistory.map(::SongEntity), CubicSongCollection.Recent, currentSong?.id, ::playSong) }
                                    composable(CubicRoutes.Favorites) { CubicSongsPage(librarySongs, CubicSongCollection.Favorites, currentSong?.id, ::playSong) }
                                    composable(CubicRoutes.Cached) { CubicSongsPage(cachedSongs, CubicSongCollection.Cached, currentSong?.id, ::playSong) }
                                    composable(CubicRoutes.Albums) { CubicAlbumsPage(libraryAlbums) { selectedAlbumId = it; navigate(CubicRoutes.Album) } }
                                    composable(CubicRoutes.Artists) { CubicArtistsPage(discovery, discoveryLoading, discoveryError, { discoveryRefresh++ }) { selectedArtistId = it; navigate(CubicRoutes.Artist) } }
                                    composable(CubicRoutes.Playlists) {
                                        CubicUserPlaylistsPage(
                                            discovery, localPlaylists, librarySongs,
                                            onCreate = { localPlaylists = CubicPlaylistStore.create(it) },
                                            onSongClick = ::playSong
                                        ) { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }
                                    }
                                    composable(CubicRoutes.Search) { CubicLiveSearchPage(activeSearchQuery, ::playSong, { selectedAlbumId = it; navigate(CubicRoutes.Album) }, { selectedArtistId = it; navigate(CubicRoutes.Artist) }, { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }) }
                                    composable(CubicRoutes.Album) { CubicAlbumDetailPage(selectedAlbumId.orEmpty(), { songs, index -> playSongsAsQueue(songs, index) }) { selectedAlbumId = it; navigate(CubicRoutes.Album) } }
                                    composable(CubicRoutes.Artist) { CubicArtistDetailPage(selectedArtistId.orEmpty(), ::playSong, { selectedAlbumId = it; navigate(CubicRoutes.Album) }, { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }) }
                                    composable(CubicRoutes.Playlist) { PlaylistScreen(selectedPlaylistId.orEmpty(), ::playSong, { selectedAlbumId = it; navigate(CubicRoutes.Album) }, { navController.popBackStack() }) }
                                    composable(CubicRoutes.Mood) {
                                        selectedMood?.let { mood -> MoodScreen(mood, { selectedAlbumId = it; navigate(CubicRoutes.Album) }, { selectedArtistId = it; navigate(CubicRoutes.Artist) }, { selectedPlaylistId = it; navigate(CubicRoutes.Playlist) }) }
                                            ?: CubicEmptyState("No mood selected", "Return to Browse and choose a mood.")
                                    }
                                    composable(CubicRoutes.Profile) { CubicProfilePage(librarySongs.size, downloadedIds.size) { navigate(CubicRoutes.Settings) } }
                                    composable(CubicRoutes.Settings) { CubicSettingsPage(downloadedIds.size, ::clearDesktopData) }
                                }
                            }
                        }
                        if (showNowPlaying) {
                            CubicNowPlayingPanelV2(currentSong, isResolving, playbackMessage, playbackQueue, currentQueueIndex, syncedLyrics, plainLyrics, playerState.timestamp, lyricsLoading,
                                downloadedIds, downloadProgress, ::toggleFavorite, localPlaylists, ::playQueueIndex, ::addSongToPlaylist, ::downloadSong) { currentSong?.let { showExpandedPlayer = true } }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CubicPlayerRailV2(controller, currentSong, isResolving, playbackMessage, currentQueueIndex > 0,
                        currentQueueIndex in 0 until playbackQueue.lastIndex, ::playPrevious, ::playNext,
                        { currentSong?.let { showExpandedPlayer = true } }, Modifier.fillMaxWidth(0.82f))
                }
            }
            val expandedSong = currentSong
            if (showExpandedPlayer && expandedSong != null) {
                CubicExpandedPlayerDialogV2(controller, expandedSong, isResolving, playbackMessage, playbackQueue, currentQueueIndex,
                    syncedLyrics, plainLyrics, playerState.timestamp, lyricsLoading, downloadedIds, downloadProgress, ::toggleFavorite, localPlaylists, ::playQueueIndex, ::addSongToPlaylist, ::downloadSong,
                    ::playPrevious, ::playNext) { showExpandedPlayer = false }
            }
        }
    }
        }
}

private const val CUBIC_STREAM_CLIENT_BACKOFF_MS = 10 * 60 * 1000L
private val cubicFailedStreamClientsUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()
private const val CUBIC_CPN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private fun cubicStreamClientKey(videoId: String, clientName: String) = "$videoId:$clientName"

private fun markCubicDesktopStreamClientFailed(videoId: String, url: String) {
    val clientName = url.toHttpUrlOrNull()?.queryParameter("c").orEmpty()
    if (clientName.isNotBlank()) {
        cubicFailedStreamClientsUntil[cubicStreamClientKey(videoId, clientName)] =
            System.currentTimeMillis() + CUBIC_STREAM_CLIENT_BACKOFF_MS
    }
}

private fun cubicPlaybackCpn(): String = buildString(16) {
    repeat(16) { append(CUBIC_CPN_ALPHABET.random()) }
}

private suspend fun resolveCubicDesktopPlaybackUrlV2(videoId: String, httpClient: OkHttpClient): String? =
    resolveCubicDesktopMediaUrlV2(videoId, httpClient)

private suspend fun resolveCubicDesktopDownloadUrlV2(videoId: String, httpClient: OkHttpClient): String? =
    resolveCubicDesktopMediaUrlV2(videoId, httpClient)

private suspend fun resolveCubicDesktopMediaUrlV2(videoId: String, httpClient: OkHttpClient): String? = withContext(Dispatchers.IO) {
    resolveCubicWebPoTokenStream(videoId, httpClient)?.let { return@withContext it }

    var signatureTimestamp: Int? = null
    var signatureTimestampLoaded = false
    val now = System.currentTimeMillis()
    val clients = cubicDesktopPlaybackClients.filter { client ->
        (cubicFailedStreamClientsUntil[cubicStreamClientKey(videoId, client.clientName)] ?: 0L) <= now
    }.ifEmpty {
        cubicFailedStreamClientsUntil.keys.removeIf { it.startsWith("$videoId:") }
        cubicDesktopPlaybackClients
    }

    for (client in clients) {
        if (client.loginRequired && Innertube.cookie.isNullOrBlank()) continue
        if (client.useSignatureTimestamp && !signatureTimestampLoaded) {
            signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
            signatureTimestampLoaded = true
        }
        val context = client.toContext(
            Innertube.locale,
            Innertube.visitorData.ifBlank { Innertube.DEFAULT_VISITOR_DATA },
            Innertube.dataSyncId.takeIf { !Innertube.cookie.isNullOrBlank() }
        )
        val response = runCatching {
            withTimeout(10_000L) {
                Innertube.player(videoId = videoId, context = context, signatureTimestamp = signatureTimestamp)?.getOrNull()
            }
        }.getOrNull() ?: continue
        if (response.playabilityStatus?.status != "OK") {
            System.err.println("Cubic player rejected ${client.clientName}: ${response.playabilityStatus?.status} ${response.playabilityStatus?.reason.orEmpty()}")
            continue
        }

        System.err.println("Cubic player ${client.clientName}: ${orderedCubicDesktopFormatsV2(response.streamingData).size} audio formats")

        for (format in orderedCubicDesktopFormatsV2(response.streamingData)) {
            val rawUrl = strictCubicDesktopStreamUrl(format, videoId) ?: continue
            val identifiedUrl = attachCubicPlaybackIdentity(rawUrl, client, cubicPlaybackCpn()) ?: continue
            val validatedUrl = CubicRangeTransfer.validate(httpClient, identifiedUrl) ?: continue
            return@withContext validatedUrl
        }
    }
    null
}
private fun strictCubicDesktopStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): String? =
    NewPipeUtils.getStreamUrl(format, videoId).getOrNull()?.takeIf(String::isNotBlank)

private fun orderedCubicDesktopFormatsV2(streamingData: PlayerResponse.StreamingData?): List<PlayerResponse.StreamingData.Format> {
    val knownAudioItags = setOf(139, 140, 141, 171, 249, 250, 251, 774)
    val preferredAudioItags = listOf(251, 774, 141, 140, 250, 249, 139, 171)
    val formats = (streamingData?.adaptiveFormats.orEmpty() + streamingData?.formats.orEmpty())
        .filter { format ->
            format.isAudio && (!format.url.isNullOrBlank() || !format.signatureCipher.isNullOrBlank()) &&
                (format.itagValue == null || format.itagValue in knownAudioItags)
        }
        .distinctBy { it.itagValue ?: it.mimeType + it.url.orEmpty() + it.signatureCipher.orEmpty() }
    return formats.sortedWith(
        compareBy<PlayerResponse.StreamingData.Format> {
            preferredAudioItags.indexOf(it.itagValue).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
        }.thenByDescending { it.bitrateValue ?: 0 }
    )
}

private fun String?.cubicDurationMillis(): Long {
    val parts = this?.split(':')?.mapNotNull(String::toLongOrNull) ?: return 0L
    if (parts.isEmpty()) return 0L
    return parts.fold(0L) { total, part ->
        if (part < 0L || total > Long.MAX_VALUE / 60L) return 0L
        total * 60L + part
    } * 1000L
}


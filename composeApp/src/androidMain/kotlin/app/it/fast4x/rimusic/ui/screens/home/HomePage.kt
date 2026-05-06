package app.it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import androidx.core.content.edit
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.compose.persist.persist
import app.it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.Countries
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmPlaylist
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSection
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.Artist
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.ShimmerHost
import app.it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.components.themed.TitleMiniSection
import app.it.fast4x.rimusic.ui.items.AlbumItem
import app.it.fast4x.rimusic.ui.items.AlbumItemPlaceholder
import app.it.fast4x.rimusic.ui.items.ArtistItem
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.ui.items.PlaylistItemPlaceholder
import app.it.fast4x.rimusic.ui.items.SongItem
import app.it.fast4x.rimusic.ui.items.SongItemPlaceholder
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.it.fast4x.rimusic.utils.loadedDataKey
import app.it.fast4x.rimusic.utils.parentalControlEnabledKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.secondary
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.selectedCountryCodeKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showSearchTabKey
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.homeScreenTabIndexKey
import app.it.fast4x.rimusic.utils.preferences
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private enum class YtmHomeSectionStyle { SongGrid, SongRail, VideoRail, ArtistRail, AlbumRail, PlaylistRail }

private val gridSectionKeywords = listOf(
    "quick picks",
    "trending songs",
    "long listens",
    "heard in shorts",
    "forgotten favourites",
    "forgotten favorites",
    "trending songs for you",
)

private fun homeSectionStyle(title: String, items: List<YtmHomeSectionItem>): YtmHomeSectionStyle {
    val lower = title.trim().lowercase()
    val firstType = items.firstOrNull()?.type?.trim()?.lowercase().orEmpty()
    return when {
        lower.contains("video") -> YtmHomeSectionStyle.VideoRail
        lower.contains("channel") -> YtmHomeSectionStyle.ArtistRail
        gridSectionKeywords.any { lower.contains(it) } -> YtmHomeSectionStyle.SongGrid
        firstType == "video" -> YtmHomeSectionStyle.VideoRail
        firstType == "artist" -> YtmHomeSectionStyle.ArtistRail
        firstType == "album" -> YtmHomeSectionStyle.AlbumRail
        firstType == "playlist" -> YtmHomeSectionStyle.PlaylistRail
        else -> YtmHomeSectionStyle.SongRail
    }
}

private fun isAcceptedSection(title: String): Boolean {
    return title.isNotBlank()
}

private fun YtmHomeSectionItem.asPlayableSong(): Song? {
    val mediaId = videoId.trim().ifBlank { id.trim() }
    val cleanedTitle = title.trim()
    if (mediaId.isBlank() || cleanedTitle.isBlank()) return null
    return Song(
        id = mediaId,
        title = cleanedTitle,
        artistsText = artistsText.trim().ifBlank { subtitle.trim().ifBlank { null } },
        durationText = null,
        thumbnailUrl = thumbnailUrl.trim().ifBlank { thumbnail.trim().ifBlank { null } },
        totalPlayTimeMs = 1L
    )
}

private fun YtmHomeSectionItem.asAlbum(): Album = Album(
    id = browseId.ifBlank { albumId.ifBlank { playlistId.ifBlank { id.ifBlank { title.trim() } } } },
    title = title.trim().ifBlank { null },
    thumbnailUrl = thumbnailUrl.trim().ifBlank { thumbnail.trim().ifBlank { null } },
    authorsText = artistsText.trim().ifBlank { subtitle.trim().ifBlank { null } },
    isYoutubeAlbum = true
)

private fun YtmHomeSectionItem.asArtist(): Artist = Artist(
    id = browseId.ifBlank { artistId.ifBlank { artistIds.firstOrNull().orEmpty().ifBlank { id.ifBlank { title.trim() } } } },
    name = title.trim().ifBlank { null },
    thumbnailUrl = thumbnailUrl.trim().ifBlank { thumbnail.trim().ifBlank { null } },
    isYoutubeArtist = true
)

@Composable
private fun AccentChip(label: String, modifier: Modifier = Modifier) {
    val palette = colorPalette()
    BasicText(
        text = label,
        style = typography().xxs.semiBold.copy(color = palette.accent),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.accent.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun YtmPlaylistsFooter(
    playlists: List<YtmPlaylist>,
    onPlaylistClick: (String) -> Unit,
    onOpenLibraryClick: () -> Unit
) {
    if (playlists.isEmpty()) return
    SectionCard(
        title = "From your YT Music playlists",
        subtitle = "Saved mixes and collections from the active account",
        hasMore = true,
        moreLabel = "Playlists",
        onMoreClick = onOpenLibraryClick
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 4.dp)
        ) {
            items(playlists, key = { it.playlistId.ifBlank { it.title } }) { playlist ->
                PlaylistItem(
                    browseId = playlist.playlistId,
                    thumbnailContent = {
                        AsyncImage(
                            model = playlist.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    songCount = playlist.songCount.toIntOrNull(),
                    name = playlist.title,
                    channelName = playlist.subtitle,
                    thumbnailSizeDp = 108.dp,
                    modifier = Modifier.clickable {
                        if (playlist.playlistId.isNotBlank()) onPlaylistClick(playlist.playlistId)
                    },
                    alternative = true,
                    showSongsCount = false,
                    disableScrollingText = false,
                    isYoutubePlaylist = true
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorPalette().accent.copy(alpha = 0.14f))
                        .clickable(onClick = onOpenLibraryClick)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "Move to playlists",
                        style = typography().xs.semiBold.copy(color = colorPalette().accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    hasMore: Boolean = false,
    moreLabel: String = "More",
    onMoreClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = colorPalette()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(top = 14.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.background1.copy(alpha = 0.85f), palette.background2.copy(alpha = 0.62f))
                ),
                shape = shape
            )
            .background(
                brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)),
                shape = shape
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.04f))),
                shape = shape
            )
            .clip(shape)
            .padding(vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = title, style = typography().m.semiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    BasicText(
                        text = subtitle,
                        style = typography().xxs.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (hasMore) {
                Spacer(Modifier.width(8.dp))
                AccentChip(
                    label = moreLabel,
                    modifier = if (onMoreClick != null) Modifier.clickable(onClick = onMoreClick) else Modifier
                )
            }
        }
        content()
    }
}

@Composable
private fun YtmBanner() {
    val palette = colorPalette()
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = 10.dp, bottom = 4.dp)
            .fillMaxWidth()
            .height(148.dp)
            .clip(shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data("https://www.gstatic.com/youtube/media/ytm/images/sbg/wsbg@4000x2250.png").crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(palette.background0.copy(alpha = 0.28f), palette.background0.copy(alpha = 0.74f)))
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, palette.background1.copy(alpha = 0.68f))))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painter = painterResource(R.drawable.ytmusic), contentDescription = null, modifier = Modifier.size(22.dp))
                BasicText(text = "YouTube Music", style = typography().s.semiBold)
            }
            BasicText(
                text = "Personalized picks, mixes and new finds for the active account",
                style = typography().xxs.secondary,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun HomeShimmer(albumThumbnailSizeDp: androidx.compose.ui.unit.Dp, playlistThumbnailSizeDp: androidx.compose.ui.unit.Dp) {
    ShimmerHost(modifier = Modifier.padding(top = 4.dp)) {
        TitleMiniSection(title = "Loading your picks...", modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 6.dp))
        repeat(3) { SongItemPlaceholder() }
        TitleMiniSection(title = "Fresh from YT Music", modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp))
        LazyRow { items(3) { AlbumItemPlaceholder(thumbnailSizeDp = albumThumbnailSizeDp, alternative = true) } }
        TitleMiniSection(title = "More for you", modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp))
        LazyRow { items(3) { PlaylistItemPlaceholder(thumbnailSizeDp = playlistThumbnailSizeDp, alternative = true) } }
    }
}

@Composable
private fun NotLoggedInCard() {
    val palette = colorPalette()
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = 20.dp, bottom = 8.dp)
            .fillMaxWidth()
            .background(palette.background1, shape)
            .border(0.7.dp, palette.background2, shape)
            .padding(vertical = 28.dp, horizontal = 20.dp)
    ) {
        Image(painter = painterResource(R.drawable.ytmusic), contentDescription = null, modifier = Modifier.size(40.dp).alpha(0.52f))
        BasicText(text = stringResource(R.string.log_in_to_ytm), style = typography().s.secondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomePage(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val context = LocalContext.current
    val refreshScope = rememberCoroutineScope()

    var homePageResult by persist<Result<List<YtmHomeSection>?>>("home/home/sessionFeedResult")
    var ytmPlaylistsResult by persist<Result<List<YtmPlaylist>?>>("home/home/ytmPlaylistsResult")
    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    var loadedData by rememberPreference(loadedDataKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val showSearchTab by rememberPreference(showSearchTabKey, false)
    val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
    val activeYouTubeCookie by rememberPreference(ytCookieKey, "")
    val activeYouTubeAccountHandle by rememberPreference(ytAccountChannelHandleKey, "")
    val activeYouTubeSessionId = YouTubeSessionStore.getCurrentSession(context)?.sessionId.orEmpty()

    @Suppress("UNUSED_VARIABLE")
    val ignoredSettings = Pair(selectedCountryCode, parentalControlEnabled)

    suspend fun loadData() {
        if (appRunningInBackground) return
        if (!isYouTubeLoggedIn()) {
            homePageResult = Result.success(null)
            ytmPlaylistsResult = Result.success(emptyList())
            loadedData = true
            return
        }
        val currentSession = YouTubeSessionStore.applyCurrentSession(context)
            ?.let { YtmSessionApi.ensureScopedSession(it) }
        val cookie = currentSession?.cookie?.takeIf { it.isNotBlank() }
        if (cookie.isNullOrBlank()) {
            homePageResult = Result.success(null)
            ytmPlaylistsResult = Result.success(emptyList())
            loadedData = true
            return
        }
        coroutineScope {
            val homeDeferred = async(Dispatchers.IO) {
                YouTubeRequestThrottler.run {
                    YtmSessionApi.fetchAllHomeFeed(
                        cookies = cookie,
                        authUser = currentSession.authUser.ifBlank { null },
                        pageId = currentSession.pageId.ifBlank { null },
                        maxPages = 50
                    ).map { it.sections }
                }
            }
            val playlistsDeferred = async(Dispatchers.IO) {
                YouTubeRequestThrottler.run {
                    YtmSessionApi.fetchPlaylists(
                        cookies = cookie,
                        authUser = currentSession.authUser.ifBlank { null },
                        pageId = currentSession.pageId.ifBlank { null }
                    )
                }
            }
            homePageResult = homeDeferred.await()
            ytmPlaylistsResult = playlistsDeferred.await()
        }
        if (loadedData) return
        runCatching { refreshScope.launch(Dispatchers.IO) {} }
            .onFailure {
                Timber.e("Failed loadData in HomePage ${it.stackTraceToString()}")
                loadedData = false
            }
            .onSuccess { loadedData = true }
    }

    LaunchedEffect(Unit) { loadData() }

    LaunchedEffect(activeYouTubeCookie, activeYouTubeSessionId, activeYouTubeAccountHandle) {
        loadedData = false
        loadData()
    }

    var refreshing by remember { mutableStateOf(false) }
    fun refresh() {
        if (refreshing || appRunningInBackground) return
        loadedData = false
        refreshScope.launch(Dispatchers.IO) {
            if (appRunningInBackground) return@launch
            refreshing = true
            loadData()
            delay(500)
            refreshing = false
        }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val scrollState = rememberScrollState()
    val endInsetDp = windowInsets.only(WindowInsetsSides.End).asPaddingValues().calculateRightPadding(LayoutDirection.Ltr)
    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    val playlistRail = ytmPlaylistsResult?.getOrNull().orEmpty()

    fun openPlaylistLibraryTab() {
        context.preferences.edit { putInt(homeScreenTabIndexKey, 4) }
        navController.navigate(NavRoutes.home.name)
    }

    var showLoader by remember { mutableStateOf(!loadedData) }
    LaunchedEffect(loadedData) {
        if (loadedData) {
            delay(600)
            showLoader = false
        } else {
            showLoader = true
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showLoader) 0f else 1f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    suspend fun playSection(items: List<YtmHomeSectionItem>, clickedItem: YtmHomeSectionItem) {
        val queuedSongs = items.mapNotNull { item ->
            item.asPlayableSong()?.let { song -> item to song }
        }
        val queue = queuedSongs.map { (_, song) -> song.asMediaItem }
        if (queue.isEmpty()) return
        val clickedMediaId = clickedItem.videoId.trim()
        val clickedTitle = clickedItem.title.trim()
        val clickedArtist = clickedItem.subtitle.trim()
        val exactIndex: Int = queuedSongs.indexOfFirst { (sourceItem, song) ->
            sourceItem.videoId.trim() == clickedMediaId &&
                song.title.trim() == clickedTitle &&
                song.artistsText.orEmpty().trim() == clickedArtist
        }
        val titleIndex: Int = queuedSongs.indexOfFirst { (sourceItem, song) ->
            sourceItem.videoId.trim() == clickedMediaId &&
                song.title.trim() == clickedTitle
        }
        val metadataIndex: Int = queue.indexOfFirst { mediaItem ->
            mediaItem.mediaMetadata.title?.toString()?.trim() == clickedTitle &&
                mediaItem.mediaMetadata.artist?.toString().orEmpty().trim() == clickedArtist
        }
        val startIndex = listOf(exactIndex, titleIndex, metadataIndex).firstOrNull { it >= 0 } ?: 0
        binder?.stopRadio()
        binder?.player?.forcePlayAtIndex(queue, startIndex)
    }
    
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::refresh) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(
                    if (NavigationBarPosition.Right.isCurrent()) Dimensions.contentWidthRightBar else 1f
                )
        ) {
            Column(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                AppHeader(navController).Draw()

                if (UiType.ViMusic.isCurrent() && showSearchTab) {
                    HeaderWithIcon(
                        title = stringResource(R.string.home),
                        iconId = R.drawable.search,
                        enabled = true,
                        showIcon = true,
                        modifier = Modifier,
                        onClick = onSearchClick
                    )
                }

                YtmBanner()
                if (showLoader) {
                    HomeShimmer(albumThumbnailSizeDp, playlistThumbnailSizeDp)
                } else {
                    val sections = homePageResult?.getOrNull()
                    if (sections == null) {
                        NotLoggedInCard()
                    } else {
                        val displaySections = sections
                            .filter { section -> isAcceptedSection(section.title) && section.items.isNotEmpty() }
                            .ifEmpty { sections.filter { it.items.isNotEmpty() } }

                        Column(modifier = Modifier.alpha(contentAlpha)) {
                            displaySections.forEach { section ->
                                val style = homeSectionStyle(section.title, section.items)
                                val sectionKey = buildString {
                                    append(section.title.trim())
                                    append("|")
                                    append(section.browseId.trim())
                                    append("|")
                                    append(section.params.trim())
                                }
                                val isExpanded = sectionKey in expandedSections
                                val hasMore = section.browseId.isNotBlank() || section.params.isNotBlank() || section.items.size > 6

                                SectionCard(
                                    title = section.title,
                                    hasMore = hasMore,
                                    moreLabel = if (isExpanded) "Less" else "More",
                                    onMoreClick = if (hasMore) {
                                        {
                                            expandedSections = if (isExpanded) {
                                                expandedSections - sectionKey
                                            } else {
                                                expandedSections + sectionKey
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                ) {
                                    when (style) {
                                        YtmHomeSectionStyle.SongGrid -> {
                                            val gridItems = section.items
                                                .filter { it.type == "song" || it.type == "video" }
                                            if (gridItems.isNotEmpty()) {
                                                val rowCount = (gridItems.size + 1) / 2
                                                val rowHeight = songThumbnailSizeDp + 24.dp
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(rowHeight * rowCount)
                                                        .padding(horizontal = 4.dp)
                                                ) {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        userScrollEnabled = false,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        items(gridItems) { item ->
                                                            val song = item.asPlayableSong()
                                                            if (song != null) {
                                                                SongItem(
                                                                    song = song,
                                                                    thumbnailSizePx = songThumbnailSizePx,
                                                                    thumbnailSizeDp = songThumbnailSizeDp,
                                                                    onDownloadClick = {},
                                                                    downloadState = Download.STATE_STOPPED,
                                                                    disableScrollingText = disableScrollingText,
                                                                    isNowPlaying = false,
                                                                    modifier = Modifier.clickable {
                                                                        refreshScope.launch { playSection(section.items, item) }
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        YtmHomeSectionStyle.SongRail,
                                        YtmHomeSectionStyle.VideoRail -> {
                                            val railItems = section.items
                                                .filter { it.type == "song" || it.type == "video" }
                                            LazyRow(
                                                contentPadding = PaddingValues(start = 8.dp, top = 0.dp, end = 8.dp + endInsetDp, bottom = 0.dp)
                                            ) {
                                                items(railItems) { item ->
                                                    val song = item.asPlayableSong()
                                                    if (song != null) {
                                                        SongItem(
                                                            song = song,
                                                            thumbnailSizePx = songThumbnailSizePx,
                                                            thumbnailSizeDp = songThumbnailSizeDp,
                                                            onDownloadClick = {},
                                                            downloadState = Download.STATE_STOPPED,
                                                            disableScrollingText = disableScrollingText,
                                                            isNowPlaying = false,
                                                            modifier = Modifier
                                                                .then(if (style == YtmHomeSectionStyle.VideoRail) Modifier.width(220.dp) else Modifier)
                                                                .clickable {
                                                                    refreshScope.launch { playSection(section.items, item) }
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        YtmHomeSectionStyle.AlbumRail -> {
                                            val albumItems = section.items
                                                .filter { it.type == "album" }
                                            LazyRow(
                                                contentPadding = PaddingValues(start = 8.dp, top = 0.dp, end = 8.dp + endInsetDp, bottom = 0.dp)
                                            ) {
                                                items(albumItems) { item ->
                                                    AlbumItem(
                                                        album = item.asAlbum(),
                                                        alternative = true,
                                                        thumbnailSizePx = albumThumbnailSizePx,
                                                        thumbnailSizeDp = albumThumbnailSizeDp,
                                                        disableScrollingText = disableScrollingText,
                                                        modifier = Modifier.clickable {
                                                            val albumId = item.browseId.ifBlank { item.playlistId }
                                                            if (albumId.isNotBlank()) onAlbumClick(albumId)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        YtmHomeSectionStyle.ArtistRail -> {
                                            val artistItems = section.items
                                                .filter { it.type == "artist" }
                                            LazyRow(
                                                contentPadding = PaddingValues(start = 8.dp, top = 0.dp, end = 8.dp + endInsetDp, bottom = 0.dp)
                                            ) {
                                                items(artistItems) { item ->
                                                    ArtistItem(
                                                        artist = item.asArtist(),
                                                        thumbnailSizePx = artistThumbnailSizePx,
                                                        thumbnailSizeDp = artistThumbnailSizeDp,
                                                        disableScrollingText = disableScrollingText,
                                                        modifier = Modifier.clickable {
                                                            if (item.browseId.isNotBlank()) onArtistClick(item.browseId)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        YtmHomeSectionStyle.PlaylistRail -> {
                                            val playlistItems = section.items
                                                .filter { it.type == "playlist" }
                                            LazyRow(
                                                contentPadding = PaddingValues(start = 8.dp, top = 0.dp, end = 8.dp + endInsetDp, bottom = 0.dp)
                                            ) {
                                                items(playlistItems) { item ->
                                                    PlaylistItem(
                                                        browseId = item.playlistId.ifBlank { item.browseId },
                                                        thumbnailContent = {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(context).data(item.thumbnail).crossfade(true).build(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        },
                                                        songCount = null,
                                                        name = item.title,
                                                        channelName = item.subtitle,
                                                        thumbnailSizeDp = playlistThumbnailSizeDp,
                                                        modifier = Modifier.clickable {
                                                            val playlistId = item.playlistId.ifBlank { item.browseId }
                                                            if (playlistId.isNotBlank()) onPlaylistClick(playlistId)
                                                        },
                                                        alternative = true,
                                                        showSongsCount = false,
                                                        disableScrollingText = disableScrollingText,
                                                        isYoutubePlaylist = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            YtmPlaylistsFooter(
                                playlists = playlistRail,
                                onPlaylistClick = onPlaylistClick,
                                onOpenLibraryClick = ::openPlaylistLibraryTab
                            )
                            Spacer(modifier = Modifier.height(Dimensions.bottomSpacer + Dimensions.miniPlayerHeight + 20.dp))
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                miniPlayer()
            }

            if (UiType.ViMusic.isCurrent() && showFloatingIcon) {
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )
            }
        }
        }
    }
}

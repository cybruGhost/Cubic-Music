package app.it.fast4x.rimusic.ui.screens.home.apple

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSection
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.repository.QuickPicksRepository
import app.it.fast4x.rimusic.ui.screens.home.HomeFeedSessionCache
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.it.fast4x.rimusic.utils.isYouTubeVideoId
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.requests.HomePage
import it.fast4x.innertube.requests.discoverPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppleHomeItemKind {
    Song,
    Album,
    Artist,
    Playlist,
}

private data class AppleHomeItem(
    val stableId: String,
    val kind: AppleHomeItemKind,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val destinationId: String = "",
    val mediaItem: MediaItem? = null,
)

private data class AppleHomeSection(
    val title: String,
    val items: List<AppleHomeItem>,
)

private fun mergeAppleHomeSections(
    vararg sources: List<AppleHomeSection>,
): List<AppleHomeSection> {
    val merged = linkedMapOf<String, AppleHomeSection>()
    sources.forEach { sections ->
        sections.forEach { section ->
            val key = section.title.trim().lowercase()
            if (key.isBlank()) return@forEach
            val existing = merged[key]
            merged[key] = if (existing == null) {
                section
            } else {
                existing.copy(
                    items = (existing.items + section.items)
                        .distinctBy(AppleHomeItem::stableId)
                )
            }
        }
    }
    return merged.values.toList()
}

private fun Innertube.Item?.toAppleHomeItem(): AppleHomeItem? = when (this) {
    is Innertube.SongItem -> asSong
        .takeIf { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }
        ?.let { song ->
            AppleHomeItem(
                stableId = "song:${song.id}",
                kind = AppleHomeItemKind.Song,
                title = song.title,
                subtitle = song.artistsText.orEmpty(),
                thumbnailUrl = song.thumbnailUrl.orEmpty(),
                mediaItem = song.asMediaItem,
            )
        }

    is Innertube.VideoItem -> asSong
        .takeIf { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }
        ?.let { song ->
            AppleHomeItem(
                stableId = "video:${song.id}",
                kind = AppleHomeItemKind.Song,
                title = song.title,
                subtitle = song.artistsText.orEmpty(),
                thumbnailUrl = song.thumbnailUrl.orEmpty(),
                mediaItem = song.asMediaItem,
            )
        }

    is Innertube.AlbumItem -> key.takeIf(String::isNotBlank)?.let { id ->
        AppleHomeItem(
            stableId = "album:$id",
            kind = AppleHomeItemKind.Album,
            title = info?.name.orEmpty(),
            subtitle = authors?.joinToString(", ") { author -> author.name.orEmpty() }.orEmpty(),
            thumbnailUrl = thumbnail?.url.orEmpty(),
            destinationId = id,
        )
    }

    is Innertube.ArtistItem -> key.takeIf(String::isNotBlank)?.let { id ->
        AppleHomeItem(
            stableId = "artist:$id",
            kind = AppleHomeItemKind.Artist,
            title = info?.name.orEmpty(),
            subtitle = subscribersCountText.orEmpty(),
            thumbnailUrl = thumbnail?.url.orEmpty(),
            destinationId = id,
        )
    }

    is Innertube.PlaylistItem -> key.takeIf(String::isNotBlank)?.let { id ->
        AppleHomeItem(
            stableId = "playlist:$id",
            kind = AppleHomeItemKind.Playlist,
            title = info?.name.orEmpty(),
            subtitle = channel?.name.orEmpty(),
            thumbnailUrl = thumbnail?.url.orEmpty(),
            destinationId = id,
        )
    }

    else -> null
}

private fun isAppleMadeForYouSection(title: String): Boolean {
    val normalized = title.trim().lowercase()
    return normalized == "for you" ||
        "made for you" in normalized ||
        "mixed for you" in normalized ||
        "quick pick" in normalized ||
        "daily discover" in normalized ||
        "trending songs for you" in normalized ||
        "fresh find" in normalized
}

private fun Song.toAppleMadeForYouItem(): AppleHomeItem? =
    takeIf { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }
        ?.let { song ->
            AppleHomeItem(
                stableId = "song:${song.id}",
                kind = AppleHomeItemKind.Song,
                title = song.title,
                subtitle = song.artistsText.orEmpty(),
                thumbnailUrl = song.thumbnailUrl.orEmpty(),
                mediaItem = song.asMediaItem,
            )
        }

private fun YtmHomeSectionItem.toAppleHomeItem(): AppleHomeItem? {
    val normalizedType = type.lowercase()
    val image = thumbnailUrl.ifBlank { thumbnail }
    return when (normalizedType) {
        "song", "video" -> {
            val sourceId = videoId.trim().ifBlank { id.trim() }
            if (!sourceId.isYouTubeVideoId() || title.isBlank()) return null
            val song = Song(
                id = sourceId,
                title = title,
                artistsText = artistsText.ifBlank { subtitle }.ifBlank { null },
                durationText = null,
                thumbnailUrl = image.ifBlank { null },
            )
            AppleHomeItem(
                stableId = "song:$sourceId",
                kind = AppleHomeItemKind.Song,
                title = title,
                subtitle = artistsText.ifBlank { subtitle },
                thumbnailUrl = image,
                mediaItem = song.asMediaItem,
            )
        }

        "album" -> browseId.ifBlank { albumId }.ifBlank { playlistId }
            .takeIf(String::isNotBlank)
            ?.let { destination ->
                AppleHomeItem(
                    stableId = "album:$destination",
                    kind = AppleHomeItemKind.Album,
                    title = title,
                    subtitle = artistsText.ifBlank { subtitle },
                    thumbnailUrl = image,
                    destinationId = destination,
                )
            }

        "artist" -> browseId.ifBlank { artistId }
            .takeIf(String::isNotBlank)
            ?.let { destination ->
                AppleHomeItem(
                    stableId = "artist:$destination",
                    kind = AppleHomeItemKind.Artist,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = image,
                    destinationId = destination,
                )
            }

        "playlist" -> playlistId.ifBlank { browseId }
            .takeIf(String::isNotBlank)
            ?.let { destination ->
                AppleHomeItem(
                    stableId = "playlist:$destination",
                    kind = AppleHomeItemKind.Playlist,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = image,
                    destinationId = destination,
                )
            }

        else -> null
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleHomeScreen(
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val scope = rememberCoroutineScope()
    val isSignedIn = isYouTubeLoggedIn()
    val activeCookie by rememberPreference(ytCookieKey, "")
    val activeHandle by rememberPreference(ytAccountChannelHandleKey, "")

    var sections by remember { mutableStateOf<List<AppleHomeSection>>(emptyList()) }
    var madeForYouItems by remember { mutableStateOf<List<AppleHomeItem>>(emptyList()) }
    var newReleases by remember { mutableStateOf<List<AppleHomeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshGeneration by remember { mutableIntStateOf(0) }

    suspend fun load(forceReload: Boolean) = withContext(Dispatchers.IO) {
        val accountKey = activeHandle.ifBlank { activeCookie.hashCode().toString() }
        coroutineScope {
            val casualMadeForYou = async {
                QuickPicksRepository.loadCasualPlayedRecommendations(
                    limit = 12,
                    forceRefresh = forceReload,
                )
            }
            val guestHome = async {
                (if (!forceReload) HomeFeedSessionCache.directHomePage else null)
                    ?: YtMusic.getHomePage(setLogin = false).getOrNull()
                        .also(HomeFeedSessionCache::storeDirectHome)
            }
            val discover = async {
                (if (!forceReload) HomeFeedSessionCache.discoverPage else null)
                    ?: Innertube.discoverPage().getOrNull()
                        .also(HomeFeedSessionCache::storeDiscover)
            }
            val publicGuestHome = async {
                (if (!forceReload) HomeFeedSessionCache.publicGuestSections else null)
                    ?: YtmSessionApi.fetchAllHomeFeed(
                        cookies = "",
                        guest = true,
                        maxPages = 50,
                        safetyCap = 5,
                    ).getOrNull()?.sections.orEmpty()
                        .also(HomeFeedSessionCache::storePublicGuest)
            }
            val signedHome = async {
                if (!isSignedIn) {
                    HomeFeedSessionCache.clearSigned()
                    return@async emptyList<YtmHomeSection>()
                }
                if (!forceReload) {
                    HomeFeedSessionCache.signedSections(accountKey)?.let { return@async it }
                }
                val session = YouTubeSessionStore.applyCurrentSession(context)
                    ?.let { YtmSessionApi.ensureScopedSession(it) }
                    ?: return@async emptyList()
                if (!YouTubeSessionStore.hasAuthCookies(session.cookie)) return@async emptyList()
                YouTubeRequestThrottler.run {
                    YtmSessionApi.fetchAllHomeFeed(
                        cookies = session.cookie,
                        authUser = session.authUser.ifBlank { null },
                        pageId = session.pageId.ifBlank { null },
                        maxPages = 3,
                        safetyCap = 2,
                    ).getOrNull()?.sections.orEmpty()
                        .also { sections -> HomeFeedSessionCache.storeSigned(accountKey, sections) }
                }
            }

            val guestSections = guestHome.await()
                ?.sections
                .orEmpty()
                .mapNotNull { section ->
                    section.items.mapNotNull { item -> item.toAppleHomeItem() }
                        .distinctBy(AppleHomeItem::stableId)
                        .takeIf(List<AppleHomeItem>::isNotEmpty)
                        ?.let { items -> AppleHomeSection(section.title, items) }
                }
            val authenticatedSections = signedHome.await()
                .mapNotNull { section ->
                    section.items.mapNotNull(YtmHomeSectionItem::toAppleHomeItem)
                        .distinctBy(AppleHomeItem::stableId)
                        .takeIf(List<AppleHomeItem>::isNotEmpty)
                        ?.let { items -> AppleHomeSection(section.title, items) }
                }
            val publicSections = publicGuestHome.await()
                .mapNotNull { section ->
                    section.items.mapNotNull(YtmHomeSectionItem::toAppleHomeItem)
                        .distinctBy(AppleHomeItem::stableId)
                        .takeIf(List<AppleHomeItem>::isNotEmpty)
                        ?.let { items -> AppleHomeSection(section.title, items) }
                }

            sections = mergeAppleHomeSections(
                authenticatedSections,
                publicSections,
                guestSections,
            )
            madeForYouItems = casualMadeForYou.await()
                .mapNotNull(Song::toAppleMadeForYouItem)
                .distinctBy(AppleHomeItem::stableId)
                .take(12)
            newReleases = discover.await()
                ?.newReleaseAlbums
                .orEmpty()
                .mapNotNull { album -> album.toAppleHomeItem() }
                .distinctBy(AppleHomeItem::stableId)
        }
    }

    LaunchedEffect(isSignedIn, activeCookie, activeHandle, refreshGeneration) {
        if (refreshGeneration == 0) isLoading = true
        runCatching { load(forceReload = isRefreshing) }
        isLoading = false
        isRefreshing = false
    }

    fun playSection(section: AppleHomeSection, selected: AppleHomeItem) {
        val queue = section.items.mapNotNull(AppleHomeItem::mediaItem)
        if (queue.isEmpty()) return
        val startIndex = queue.indexOfFirst { item -> item.mediaId == selected.mediaItem?.mediaId }
            .takeIf { index -> index >= 0 }
            ?: 0
        binder?.stopRadio()
        binder?.player?.forcePlayAtIndex(queue, startIndex)
    }

    val madeForYouTitle = stringResource(R.string.apple_made_for_you)
    val madeForYouSection = remember(madeForYouItems, madeForYouTitle) {
        AppleHomeSection(
            title = madeForYouTitle,
            items = madeForYouItems,
        )
    }
    val playlistSections = sections
        .filterNot { section -> isAppleMadeForYouSection(section.title) }
        .mapNotNull { section ->
            section.items.filter { item -> item.kind == AppleHomeItemKind.Playlist }
                .takeIf(List<AppleHomeItem>::isNotEmpty)
                ?.let { items -> AppleHomeSection(section.title, items) }
        }
    val remainingSections = sections
        .filterNot { section -> isAppleMadeForYouSection(section.title) }
        .mapNotNull { section ->
            section.items.filterNot { item -> item.kind == AppleHomeItemKind.Playlist }
                .takeIf(List<AppleHomeItem>::isNotEmpty)
                ?.let { items -> AppleHomeSection(section.title, items) }
        }

    val screenBackground = Brush.verticalGradient(
        colors = listOf(
            colorPalette().accent.copy(alpha = 0.10f),
            colorPalette().background0,
            colorPalette().background0,
        ),
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                refreshGeneration++
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (isLoading && sections.isEmpty() && newReleases.isEmpty()) {
            CircularProgressIndicator(
                color = colorPalette().accent,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBackground),
                contentPadding = PaddingValues(top = 8.dp, bottom = Dimensions.bottomSpacer + 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (madeForYouSection.items.isNotEmpty()) {
                    item(key = "apple-made-for-you") {
                        AppleHomeSectionRow(
                            section = madeForYouSection,
                            featured = true,
                            onItemClick = { item ->
                                scope.launch { playSection(madeForYouSection, item) }
                            },
                        )
                    }
                }

                if (newReleases.isNotEmpty()) {
                    item(key = "apple-new-releases") {
                        AppleHomeSectionRow(
                            section = AppleHomeSection(
                                title = stringResource(R.string.new_albums),
                                items = newReleases,
                            ),
                            featured = true,
                            onItemClick = { item -> onAlbumClick(item.destinationId) },
                        )
                    }
                }

                items(
                    items = playlistSections,
                    key = { section -> "apple-playlists:${section.title}" },
                ) { section ->
                    AppleHomeSectionRow(
                        section = section,
                        onItemClick = { item -> onPlaylistClick(item.destinationId) },
                    )
                }

                items(
                    items = remainingSections,
                    key = { section -> "apple-section:${section.title}" },
                ) { section ->
                    AppleHomeSectionRow(
                        section = section,
                        onItemClick = { item ->
                            when (item.kind) {
                                AppleHomeItemKind.Song -> scope.launch { playSection(section, item) }
                                AppleHomeItemKind.Album -> onAlbumClick(item.destinationId)
                                AppleHomeItemKind.Artist -> onArtistClick(item.destinationId)
                                AppleHomeItemKind.Playlist -> onPlaylistClick(item.destinationId)
                            }
                        },
                    )
                }

                if (sections.isEmpty() && newReleases.isEmpty()) {
                    item(key = "apple-empty") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.page_not_been_loaded),
                                color = colorPalette().textSecondary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 40.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A frosted, translucent surface: soft gradient fill + a hairline highlight border
 * that catches light along the top edge, plus a diffuse shadow. This is what stands
 * in for real backdrop-blur "glass" without needing a blur-behind renderer.
 */
@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorPalette().background1.copy(alpha = 0.55f),
                        colorPalette().background2.copy(alpha = 0.35f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.02f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 220f),
                ),
                shape = shape,
            ),
    ) {
        content()
    }
}

@Composable
private fun AppleHomeSectionRow(
    section: AppleHomeSection,
    onItemClick: (AppleHomeItem) -> Unit,
    featured: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorPalette().accent,
                                colorPalette().accent.copy(alpha = 0.3f),
                            ),
                        ),
                    ),
            )
            Text(
                text = section.title,
                color = colorPalette().text,
                fontSize = if (featured) 24.sp else 21.sp,
                lineHeight = if (featured) 28.sp else 25.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = section.items.take(18),
                key = AppleHomeItem::stableId,
            ) { item ->
                AppleHomeTile(item = item, featured = featured, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun AppleHomeTile(
    item: AppleHomeItem,
    onClick: () -> Unit,
    featured: Boolean = false,
) {
    val tileSize = if (featured) 176.dp else 156.dp
    val imageShape = if (item.kind == AppleHomeItemKind.Artist) CircleShape else RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .width(tileSize)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .shadow(
                    elevation = 10.dp,
                    shape = imageShape,
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.40f),
                )
                .clip(imageShape)
                .background(colorPalette().background2)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    ),
                    shape = imageShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.thumbnailUrl.isNotBlank()) {
                Image(
                    painter = ImageCacheFactory.Painter(item.thumbnailUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // subtle bottom scrim so a play affordance / future badges stay legible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f).compositeOver(Color.Transparent),
                                ),
                                startY = 0f,
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colorPalette().accent.copy(alpha = 0.35f),
                                    colorPalette().background2,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.title.firstOrNull()?.uppercase().orEmpty(),
                        color = colorPalette().text,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                color = colorPalette().text,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    color = colorPalette().textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
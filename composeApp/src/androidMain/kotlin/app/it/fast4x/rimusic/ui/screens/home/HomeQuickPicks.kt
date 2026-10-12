package app.it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Context
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.compose.persist.persist
import app.it.fast4x.compose.persist.persistList
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.HomePage
import it.fast4x.innertube.requests.chartsPageComplete
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.relatedPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.Countries
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.PlayEventsType
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.isVideoEnabled
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSection
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.ShimmerHost
import app.it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.components.themed.Loader
import app.it.fast4x.rimusic.ui.components.themed.Menu
import app.it.fast4x.rimusic.ui.components.themed.MenuEntry
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemGridMenu
import app.it.fast4x.rimusic.ui.components.themed.TextPlaceholder
import app.it.fast4x.rimusic.ui.components.themed.Title
import app.it.fast4x.rimusic.ui.components.themed.Title2Actions

import app.it.fast4x.rimusic.ui.items.AlbumItem
import app.it.fast4x.rimusic.ui.items.AlbumItemPlaceholder
import app.it.fast4x.rimusic.ui.items.ArtistItem
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.ui.items.PlaylistItemPlaceholder
import app.it.fast4x.rimusic.ui.items.SongItem
import app.it.fast4x.rimusic.ui.items.SongItemPlaceholder
import app.it.fast4x.rimusic.ui.items.VideoItem
import app.it.fast4x.rimusic.ui.screens.find.FindScreen
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.px
import app.it.fast4x.rimusic.utils.WelcomeMessage
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.bold
import app.it.fast4x.rimusic.utils.center
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.isNowPlaying
import app.it.fast4x.rimusic.utils.isYouTubeVideoId
import app.it.fast4x.rimusic.utils.loadedDataKey
import app.it.fast4x.rimusic.utils.parentalControlEnabledKey
import app.it.fast4x.rimusic.utils.PlaybackContextStore
import app.it.fast4x.rimusic.utils.playEventsTypeKey
import app.it.fast4x.rimusic.utils.playVideo
import app.it.fast4x.rimusic.utils.quickPicsDiscoverPageKey
import app.it.fast4x.rimusic.utils.quickPicsHomePageKey
import app.it.fast4x.rimusic.utils.quickPicsRelatedPageKey
import app.it.fast4x.rimusic.utils.quickPicsTrendingSongKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.secondary
import app.it.fast4x.rimusic.utils.selectedCountryCodeKey
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showChartsKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showMonthlyPlaylistInQuickPicksKey
import app.it.fast4x.rimusic.utils.showMoodsAndGenresKey
import app.it.fast4x.rimusic.utils.showNewAlbumsArtistsKey
import app.it.fast4x.rimusic.utils.showNewAlbumsKey
import app.it.fast4x.rimusic.utils.showPlaylistMightLikeKey
import app.it.fast4x.rimusic.utils.showRelatedAlbumsKey
import app.it.fast4x.rimusic.utils.showSearchTabKey
import app.it.fast4x.rimusic.utils.showSimilarArtistsKey
import app.it.fast4x.rimusic.utils.showTipsKey
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import app.kreate.android.service.findReplacementVideoId
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import app.it.fast4x.rimusic.ui.components.themed.LazyMenu
import coil.request.ImageRequest
// checkupdate
import app.kreate.android.BuildConfig
import app.kreate.android.me.knighthat.coil.ImageCacheFactory

//rewind
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import kotlin.random.Random
import java.time.LocalDate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton


// ===== NOTIFICATION DATA CLASS =====
data class NotificationData(
    val version: String,
    val url: String,
    val title: String,
    val contents: String,
    val show: Boolean,
    val is_force: Boolean,
    val force_update: Boolean,
    val isUpdate: Boolean,
    val image_url: String? = null,
    val showImage: Boolean = true,
    val showText: Boolean = true
)

private data class SimilarArtistShelf(
    val seedName: String,
    val artists: List<Innertube.ArtistItem>,
)

private fun notificationDataFromJson(notificationJson: org.json.JSONObject): NotificationData {
    val isForce = notificationJson.optBoolean(
        "is_force",
        notificationJson.optBoolean("force_update", false)
    )
    val forceUpdate = notificationJson.optBoolean("force_update", isForce)

    return NotificationData(
        version = notificationJson.optString("version"),
        url = notificationJson.optString("url"),
        title = notificationJson.optString("title"),
        contents = notificationJson.optString("contents"),
        show = notificationJson.optBoolean("show", false),
        is_force = isForce,
        force_update = forceUpdate,
        isUpdate = notificationJson.optBoolean("isUpdate", false),
        image_url = notificationJson.optString("image_url").takeUnless {
            it.isBlank() || it.equals("null", ignoreCase = true)
        },
        showImage = notificationJson.optBoolean("show_image", true),
        showText = notificationJson.optBoolean("show_text", true)
    )
}

private fun notificationDataFromRawJson(rawJson: String): NotificationData? =
    runCatching {
        if (rawJson.isBlank()) return@runCatching null
        val responseRoot = org.json.JSONObject(rawJson)
        val wrappedContent = responseRoot.optString("content")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val payloadRoot = wrappedContent
            ?.let { org.json.JSONObject(it) }
            ?: responseRoot
        val notificationJson = payloadRoot.optJSONObject("notification") ?: payloadRoot
        notificationDataFromJson(notificationJson)
    }.getOrNull()

// ===== END NOTIFICATION DATA CLASS =====

// ===== VERSION COMPARISON =====
fun isNewerVersion(jsonVersion: String, appVersion: String): Boolean {
    try {
        // Remove 'v' prefix and any suffixes (like -beta, -release)
        val cleanJson = jsonVersion.removePrefix("v").split("-").first()
        val cleanApp = appVersion.removePrefix("v").split("-").first()

        // Split by dots
        val jsonParts = cleanJson.split(".").map { it.toIntOrNull() ?: 0 }
        val appParts = cleanApp.split(".").map { it.toIntOrNull() ?: 0 }

        // Compare each part
        for (i in 0 until maxOf(jsonParts.size, appParts.size)) {
            val jsonPart = jsonParts.getOrElse(i) { 0 }
            val appPart = appParts.getOrElse(i) { 0 }

            if (jsonPart > appPart) return true
            if (jsonPart < appPart) return false
        }

        return false  // Versions are equal
    } catch (e: Exception) {
        Timber.e("Error comparing versions: $e")
        return false
    }
}
// ===== END VERSION COMPARISON =====

@Composable
private fun YtmHomeCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    imageWidth: androidx.compose.ui.unit.Dp = 116.dp,
    imageHeight: androidx.compose.ui.unit.Dp = imageWidth,
    rounded: Boolean = true
) {
    Column(
        modifier = modifier
            .width(imageWidth)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ImageCacheFactory.AsyncImage(
            thumbnailUrl = thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(if (rounded) RoundedCornerShape(8.dp) else CircleShape),
        )
        BasicText(
            text = cleanPrefix(title),
            style = typography().xs.semiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            BasicText(
                text = cleanPrefix(subtitle),
                style = typography().xxs.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ytmHomeItemKey(item: Any?): String =
    when (item) {
        is Innertube.SongItem -> listOf(item.key, item.info?.name, item.authors?.joinToString(",") { it.name ?: "" })
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { "song_$it" }
            ?: "song_fallback_${item.hashCode()}"
        is Innertube.AlbumItem -> listOf(item.key, item.info?.name, item.authors?.joinToString(",") { it.name ?: "" })
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { "album_$it" }
            ?: "album_fallback_${item.hashCode()}"
        is Innertube.ArtistItem -> listOf(item.key, item.info?.name, item.subscribersCountText)
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { "artist_$it" }
            ?: "artist_fallback_${item.hashCode()}"
        is Innertube.PlaylistItem -> listOf(item.key, item.info?.name, item.channel?.name)
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { "playlist_$it" }
            ?: "playlist_fallback_${item.hashCode()}"
        is Innertube.VideoItem -> listOf(item.key, item.info?.name, item.authors?.joinToString(",") { it.name ?: "" })
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { "video_$it" }
            ?: "video_fallback_${item.hashCode()}"
        null -> "null_item"
        else -> "item_${item.hashCode()}"
    }

private fun homeQuickSongKey(song: Innertube.SongItem?, index: Int): String =
    song?.key?.takeIf { it.isNotBlank() }
        ?: "quick_song_${song?.info?.name.orEmpty()}_${index}"

private fun homeQuickArtistKey(artist: Innertube.ArtistItem?, index: Int): String =
    artist?.key?.takeIf { it.isNotBlank() }
        ?: "quick_artist_${artist?.info?.name.orEmpty()}_${index}"

private fun homeQuickVideoKey(video: Innertube.VideoItem?, index: Int): String =
    video?.key?.takeIf { it.isNotBlank() }
        ?: "quick_video_${video?.info?.name.orEmpty()}_${index}"

private fun homeQuickPlaylistKey(playlist: Innertube.PlaylistItem?): String =
    playlist?.key?.takeIf { it.isNotBlank() }
        ?: "quick_playlist_${playlist?.info?.name.orEmpty()}_${playlist?.channel?.name.orEmpty()}"

private fun homeQuickAlbumKey(album: Innertube.AlbumItem?): String =
    album?.key?.takeIf { it.isNotBlank() }
        ?: "quick_album_${album?.info?.name.orEmpty()}_${album?.authors?.joinToString(",") { it.name.orEmpty() }.orEmpty()}"

private fun YtmHomeSectionItem.asQuickPickSong(): Song? {
    val mediaId = videoId.trim().ifBlank { id.trim() }
    val cleanedTitle = cleanPrefix(title).trim()
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

private fun YtmHomeSectionItem.artistDestinationId(): String =
    browseId.ifBlank { artistId }.ifBlank { id }.trim()

private fun isVisibleQuickPicksSection(title: String): Boolean {
    return title.trim().isNotBlank()
}


private enum class HomeQuickSectionGroup {
    FOR_YOU,
    NEW_RELEASES,
    NEW_ALBUMS,
    RELATED_ALBUMS,
    SIMILAR_ARTISTS,
    PLAYLISTS,
    QUICK_PICKS,
    MOODS,
    CHARTS,
    TOP_ARTISTS,
    OTHER,
}

private fun homeQuickSectionGroup(title: String): HomeQuickSectionGroup {
    val normalized = title.trim().lowercase()
    return when {
        normalized == "for you" || "mixed for you" in normalized -> HomeQuickSectionGroup.FOR_YOU
        "new release" in normalized -> HomeQuickSectionGroup.NEW_RELEASES
        "new album" in normalized -> HomeQuickSectionGroup.NEW_ALBUMS
        "related album" in normalized || "albums for you" in normalized -> HomeQuickSectionGroup.RELATED_ALBUMS
        "similar artist" in normalized ||
            "music channel" in normalized ||
            "artists you may like" in normalized ||
            "recommended artist" in normalized -> HomeQuickSectionGroup.SIMILAR_ARTISTS
        "playlist" in normalized -> HomeQuickSectionGroup.PLAYLISTS
        "quick pick" in normalized -> HomeQuickSectionGroup.QUICK_PICKS
        "mood" in normalized || "genre" in normalized -> HomeQuickSectionGroup.MOODS
        "top artist" in normalized -> HomeQuickSectionGroup.TOP_ARTISTS
        "chart" in normalized -> HomeQuickSectionGroup.CHARTS
        else -> HomeQuickSectionGroup.OTHER
    }
}

private fun isTastePlaylistSection(title: String): Boolean {
    val normalized = title.trim().lowercase()
    return "playlist you might like" in normalized ||
        "playlists you might like" in normalized ||
        "playlists for you" in normalized ||
        "recommended playlist" in normalized ||
        "mixed for you" in normalized ||
        "create a mix" in normalized
}

private fun normalizedPlaylistId(value: String): String =
    value.trim().removePrefix("VL")

private fun ytmHomeItemStableKey(item: YtmHomeSectionItem): String = listOf(
    item.type,
    item.videoId,
    item.playlistId,
    item.browseId,
    item.id,
    item.title,
).joinToString("|")

private fun mergeYtmHomeSections(
    vararg sources: List<YtmHomeSection>,
): List<YtmHomeSection> {
    val merged = linkedMapOf<String, YtmHomeSection>()
    sources.forEach { sections ->
        sections.forEach { section ->
            val key = section.title.trim().lowercase()
            if (key.isBlank()) return@forEach
            val existing = merged[key]
            merged[key] = if (existing == null) {
                section.copy(items = section.items.distinctBy(::ytmHomeItemStableKey))
            } else {
                val items = (existing.items + section.items)
                    .distinctBy(::ytmHomeItemStableKey)
                existing.copy(
                    subtitle = existing.subtitle.ifBlank { section.subtitle },
                    itemCount = items.size,
                    hasMore = existing.hasMore || section.hasMore,
                    items = items,
                )
            }
        }
    }
    return merged.values.toList()
}

@Composable
private fun YtmHomeFeedSections(
    sections: List<YtmHomeSection>,
    endPaddingValues: PaddingValues,
    onPlayableClick: (List<YtmHomeSectionItem>, YtmHomeSectionItem) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
) {
    sections
        .filter { section -> section.title.isNotBlank() && section.items.isNotEmpty() }
        .forEach { section ->
            BasicText(
                text = section.title,
                style = typography().l.semiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 6.dp),
            )
            if (section.subtitle.isNotBlank()) {
                BasicText(
                    text = section.subtitle,
                    style = typography().xs.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    items = section.items.distinctBy(::ytmHomeItemStableKey),
                    key = ::ytmHomeItemStableKey,
                ) { item ->
                    when (item.type.lowercase()) {
                        "song", "video" -> YtmHomeCard(
                            title = item.title,
                            subtitle = item.artistsText.ifBlank { item.subtitle },
                            thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                            modifier = Modifier.clickable {
                                onPlayableClick(section.items, item)
                            },
                        )

                        "album" -> YtmHomeCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                            modifier = Modifier.clickable {
                                item.browseId.ifBlank { item.albumId.ifBlank { item.playlistId } }
                                    .takeIf(String::isNotBlank)
                                    ?.let(onAlbumClick)
                            },
                        )

                        "artist" -> YtmHomeCard(
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { item.artistsText },
                            thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                            imageWidth = 104.dp,
                            imageHeight = 104.dp,
                            rounded = false,
                            modifier = Modifier.clickable {
                                item.browseId.ifBlank { item.artistId }
                                    .takeIf(String::isNotBlank)
                                    ?.let(onArtistClick)
                            },
                        )

                        "playlist" -> YtmHomeCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                            modifier = Modifier.clickable {
                                item.playlistId.ifBlank { item.browseId }
                                    .takeIf(String::isNotBlank)
                                    ?.let(onPlaylistClick)
                            },
                        )
                    }
                }
            }
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
fun HomeQuickPicks(
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodClick: (mood: Innertube.Mood.Item) -> Unit,
    onSettingsClick: () -> Unit
) {
    var showFindSheet by rememberSaveable { mutableStateOf(false) }
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    var playEventType by rememberPreference(playEventsTypeKey, PlayEventsType.CasualPlayed)

    // ===== REWIND SECTION =====
    // Check if current date is between Dec 6th and Dec 31st
    val calendar = java.util.Calendar.getInstance()
    val currentMonth = calendar.get(java.util.Calendar.MONTH) // 0-11, where 11=December
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    var trendingList by persistList<Song>("home/quickpicks/trending_list")
    var trending by persist<Song?>("home/quickpicks/trending")
    val trendingInit by persist<Song?>(tag = "home/quickpicks/trending_init")
    var trendingPreference by rememberPreference(quickPicsTrendingSongKey, trendingInit)
    var showNewUserMessage by remember { mutableStateOf(false) }

    // Variable to store the real most popular song (before shuffle)
    var mostPopularSong by remember { mutableStateOf<Song?>(null) }
    var recentCasualIdsCsv by rememberPreference("homeQuickPicksRecentCasualIds", "")

    var relatedPageResult by persist<Result<Innertube.RelatedPage?>?>(tag = "home/quickpicks/relatedPageResult")
    var relatedInit by persist<Innertube.RelatedPage?>(tag = "home/relatedPage")
    var relatedPreference by rememberPreference(quickPicsRelatedPageKey, relatedInit)
    var similarArtistShelves by remember { mutableStateOf<List<SimilarArtistShelf>>(emptyList()) }

    var discoverPageResult by persist<Result<Innertube.DiscoverPage?>>("home/quickpicks/discoveryAlbumsResult")
    var discoverPageInit by persist<Innertube.DiscoverPage>("home/quickpicks/discoveryAlbumsInit")
    var discoverPagePreference by rememberPreference(quickPicsDiscoverPageKey, discoverPageInit)

    var homePageResult by persist<Result<HomePage?>>("home/quickpicks/homePageResult")
    var homePageInit by persist<HomePage?>("home/quickpicks/homePageInit")
    var homePagePreference by rememberPreference(quickPicsHomePageKey, homePageInit)
    var homePageSessionId by rememberPreference("quickPicsHomePageSessionId", "")
    var homePageAccountHandle by rememberPreference("quickPicsHomePageAccountHandle", "")
    var selectedHomeChipTitle by rememberPreference("quickPicsHomePageChipTitle", "")
    var selectedHomeChipParams by rememberPreference("quickPicsHomePageChipParams", "")
    var sessionHomeFeedResult by remember { mutableStateOf<Result<List<YtmHomeSection>?>?>(null) }
    var sessionHomeFeedInit by remember { mutableStateOf<List<YtmHomeSection>?>(null) }
    var publicGuestHomeFeedResult by remember {
        mutableStateOf(
            HomeFeedSessionCache.publicGuestSections
                ?.let { sections -> Result.success(sections) }
        )
    }
    var sessionHomeFeedSessionId by rememberPreference("quickPicsSessionHomeFeedSessionId", "")
    var sessionHomeFeedAccountHandle by rememberPreference("quickPicsSessionHomeFeedAccountHandle", "")

    var chartsPageResult by persist<Result<Innertube.ChartsPage?>>("home/quickpicks/chartsPageResult")
    var chartsPageInit by persist<Innertube.ChartsPage>("home/quickpicks/chartsPageInit")
    var chartsPageCountryCode by rememberPreference("quickPicsChartsPageCountryCode", "")
    //    var chartsPagePreference by rememberPreference(quickPicsChartsPageKey, chartsPageInit)

    var localRecommandationsNumber by rememberPreference(
        key = "LocalRecommandationsNumber",
        defaultValue = app.it.fast4x.rimusic.enums.LocalRecommandationsNumber.TwelveQ
    )
    var recommendations12Migrated by rememberPreference(
        key = "homeQuickPicksRecommendations12Migrated",
        defaultValue = false
    )
    LaunchedEffect(recommendations12Migrated) {
        if (!recommendations12Migrated) {
            if (localRecommandationsNumber == app.it.fast4x.rimusic.enums.LocalRecommandationsNumber.SixQ) {
                localRecommandationsNumber = app.it.fast4x.rimusic.enums.LocalRecommandationsNumber.TwelveQ
            }
            recommendations12Migrated = true
        }
    }
    val localCount = localRecommandationsNumber.value

    fun legacyHomePersonalizedSongs(): List<Song> =
        (homePageResult?.getOrNull() ?: homePageInit)
            ?.sections
            ?.flatMap { section ->
                section.items.mapNotNull { item ->
                    when (item) {
                        is Innertube.SongItem -> item.asSong
                        is Innertube.VideoItem -> item.asSong
                        else -> null
                    }
                }
            }
            ?.map { song -> song.copy(title = cleanPrefix(song.title)) }
            ?.filter { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }
            ?.distinctBy { song -> song.id }
            ?: emptyList()

    fun homePersonalizedSongs(): List<Song> =
        mergeYtmHomeSections(
            sessionHomeFeedResult?.getOrNull() ?: sessionHomeFeedInit.orEmpty(),
            publicGuestHomeFeedResult?.getOrNull().orEmpty(),
        )
            .flatMap { section ->
                section.items.mapNotNull { item ->
                    when (item.type.lowercase()) {
                        "song", "video" -> {
                            val mediaId = item.videoId.trim()
                            val title = cleanPrefix(item.title).trim()
                            if (mediaId.isBlank() || title.isBlank()) {
                                null
                            } else {
                                Song(
                                    id = mediaId,
                                    title = title,
                                    artistsText = item.subtitle.trim().ifBlank { null },
                                    durationText = null,
                                    thumbnailUrl = item.thumbnail.trim().ifBlank { null },
                                    totalPlayTimeMs = 1L
                                )
                            }
                        }
                        else -> null
                    }
                }
            }
            .filter { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }
            .distinctBy { song -> song.id }
            .ifEmpty { legacyHomePersonalizedSongs() }

    suspend fun localLikedSongs(limit: Int = 25): List<Song> =
        Database.songTable
            .allFavorites()
            .first()
            .asReversed()
            .filter { song -> song.id.isNotBlank() && song.title.isNotBlank() }
            .take(limit)

    suspend fun localDiscoverySongs(limit: Int = localCount * 4): List<Song> =
        Database.songTable
            .all(excludeHidden = true)
            .first()
            .filter { song -> song.id.isNotBlank() && song.title.isNotBlank() }
            .shuffled()
            .take(limit)

    fun casualFallbackSongs(personalizedHomeSongs: List<Song>): List<Song> =
        personalizedHomeSongs.ifEmpty {
            chartsPageResult?.getOrNull()?.songs
                ?.map { it.asSong }
                .orEmpty()
        }
            .filter { song -> song.id.isNotBlank() && song.title.isNotBlank() }
            .distinctBy { it.id }

    fun Song.primaryArtistKey(): String =
        artistsText
            ?.split(",", "&", "feat.", "ft.", ignoreCase = true)
            ?.firstOrNull()
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: title.substringBefore("-").trim().lowercase()

    fun Song.primaryArtistName(): String =
        artistsText
            ?.split(",", "&", "feat.", "ft.", ignoreCase = true)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: title.substringBefore("-").trim()

    fun List<Song>.artistVariety(maxPerArtist: Int = 2): List<Song> {
        val counts = mutableMapOf<String, Int>()
        return filter { song ->
            val artist = song.primaryArtistKey()
            val count = counts[artist] ?: 0
            if (count >= maxPerArtist) {
                false
            } else {
                counts[artist] = count + 1
                true
            }
        }
    }

    fun interleaveSongPools(pools: List<List<Song>>): List<Song> {
        val shuffledPools = pools.map { pool ->
            pool.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .shuffled()
        }
        val largestPool = shuffledPools.maxOfOrNull { it.size } ?: 0
        return (0 until largestPool)
            .flatMap { index -> shuffledPools.mapNotNull { pool -> pool.getOrNull(index) } }
            .distinctBy { it.id }
    }

    fun rememberCasualMix(songs: List<Song>) {
        val previousIds = recentCasualIdsCsv
            .split(',')
            .filter { it.isNotBlank() }
        recentCasualIdsCsv = (songs.map { it.id } + previousIds)
            .distinct()
            .take(localCount * 5)
            .joinToString(",")
    }

    fun mergeRelatedPages(
        current: Innertube.RelatedPage?,
        incoming: Innertube.RelatedPage?
    ): Innertube.RelatedPage? {
        if (current == null) return incoming
        if (incoming == null) return current

        return Innertube.RelatedPage(
            songs = (current.songs.orEmpty() + incoming.songs.orEmpty())
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            playlists = (current.playlists.orEmpty() + incoming.playlists.orEmpty())
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            albums = (current.albums.orEmpty() + incoming.albums.orEmpty())
                .filter { item -> item.key.startsWith("MPR") }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            artists = (current.artists.orEmpty() + incoming.artists.orEmpty())
                .filter { item -> item.key.startsWith("UC") }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
        )
    }

    suspend fun loadRelatedForSeeds(seedSongs: List<Song>): Result<Innertube.RelatedPage?>? {
        val pages = mutableListOf<Innertube.RelatedPage>()
        val distinctSeeds = seedSongs
            .filter { song -> song.id.isYouTubeVideoId() }
            .distinctBy { song -> song.id }
            .take(6)
        for (seed in distinctSeeds) {
            var page: Innertube.RelatedPage? = null
            repeat(2) { attempt ->
                if (page != null) return@repeat
                page = runCatching { Innertube.relatedPage(NextBody(videoId = seed.id)) }
                    .getOrNull()
                    ?.getOrNull()
                if (page == null && attempt == 0) delay(180L)
            }
            page?.let(pages::add)
        }

        if (pages.isEmpty()) return null

        val merged = Innertube.RelatedPage(
            songs = pages
                .flatMap { page -> page.songs.orEmpty() }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            playlists = pages
                .flatMap { page -> page.playlists.orEmpty() }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            albums = pages
                .flatMap { page -> page.albums.orEmpty() }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
            artists = pages
                .flatMap { page -> page.artists.orEmpty() }
                .distinctBy { item -> item.key }
                .takeIf { items -> items.isNotEmpty() },
        )
        val hasContent = !merged.songs.isNullOrEmpty() ||
            !merged.playlists.isNullOrEmpty() ||
            !merged.albums.isNullOrEmpty() ||
            !merged.artists.isNullOrEmpty()

        return merged
            .takeIf { hasContent }
            ?.let { Result.success<Innertube.RelatedPage?>(it) }
    }

    suspend fun loadSimilarArtistShelves(seedSongs: List<Song>): List<SimilarArtistShelf> {
        val seeds = seedSongs
            .filter { song -> song.id.isYouTubeVideoId() }
            .distinctBy(Song::primaryArtistKey)
            .take(4)

        return seeds.mapNotNull { seed ->
            var page: Innertube.RelatedPage? = null
            repeat(2) { attempt ->
                if (page != null) return@repeat
                page = runCatching { Innertube.relatedPage(NextBody(videoId = seed.id)) }
                    .getOrNull()
                    ?.getOrNull()
                if (page == null && attempt == 0) delay(180L)
            }

            val seedName = seed.primaryArtistName()
            val artists = page
                ?.artists
                .orEmpty()
                .filter { artist ->
                    artist.key.startsWith("UC") &&
                        !artist.info?.name.isNullOrBlank() &&
                        !artist.info?.name.equals(seedName, ignoreCase = true)
                }
                .distinctBy(Innertube.ArtistItem::key)
                .take(14)

            artists.takeIf(List<Innertube.ArtistItem>::isNotEmpty)
                ?.let { SimilarArtistShelf(seedName = seedName, artists = it) }
        }
    }

    suspend fun buildCasualMix(
        personalizedHomeSongs: List<Song>,
        favoriteSongs: List<Song>,
        discoverySongs: List<Song>
    ): Pair<List<Song>, Result<Innertube.RelatedPage?>?> {
        val recentIds = recentCasualIdsCsv
            .split(',')
            .filter { it.isNotBlank() }
            .toSet()
        val idsToAvoid = recentIds + trendingList.map { it.id }
        val playedIds = favoriteSongs.map { it.id }.toSet()
        val freshDiscoverySongs = discoverySongs
            .filter { song -> song.id !in playedIds }
            .distinctBy { it.id }
        val ytmPool = (casualFallbackSongs(personalizedHomeSongs) + freshDiscoverySongs)
            .distinctBy { it.id }
        val allSeedCandidates = (personalizedHomeSongs + freshDiscoverySongs + favoriteSongs + ytmPool)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
        val freshSeedCandidates = allSeedCandidates.filterNot { it.id in idsToAvoid }
        val seedCandidates = freshSeedCandidates.ifEmpty { allSeedCandidates }
            .shuffled()
            .artistVariety(maxPerArtist = 1)
        val preferredSeed = seedCandidates.firstOrNull()
        val seedSongs = seedCandidates.take(3).ifEmpty { listOfNotNull(preferredSeed) }

        mostPopularSong = preferredSeed ?: favoriteSongs.firstOrNull()
        if (preferredSeed == null) {
            val freshFallback = ytmPool.filterNot { it.id in idsToAvoid }
            return freshFallback.ifEmpty { ytmPool }.shuffled().take(localCount) to null
        }

        val relatedResult = loadRelatedForSeeds(seedSongs)
        val relatedSongs = relatedResult
            ?.getOrNull()
            ?.songs
            .orEmpty()
            .map { it.asSong }
            .filter { song -> song.id.isYouTubeVideoId() && song.title.isNotBlank() }

        val ytmQuota = if (personalizedHomeSongs.isNotEmpty()) {
            (localCount * 0.65f).toInt().coerceAtLeast(1)
        } else {
            (localCount * 0.35f).toInt().coerceAtLeast(1)
        }

        val favoriteQuota = if (favoriteSongs.isNotEmpty() && personalizedHomeSongs.isEmpty()) {
            (localCount * 0.3f).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val balancedCandidates = interleaveSongPools(
            listOf(
                relatedSongs,
                personalizedHomeSongs.take(ytmQuota * 2),
                freshDiscoverySongs,
                favoriteSongs.take(maxOf(favoriteQuota * 2, 2)),
                ytmPool,
            )
        )
        val unseenCandidates = balancedCandidates.filterNot { it.id in idsToAvoid }
        val firstPass = unseenCandidates
            .artistVariety(maxPerArtist = 1)
            .take(localCount)
        val fill = balancedCandidates
            .filterNot { candidate -> firstPass.any { it.id == candidate.id } }
            .artistVariety(maxPerArtist = 2)
        val mixedSongs = (firstPass + fill)
            .distinctBy { it.id }
            .take(localCount)

        return mixedSongs to relatedResult
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current
    val activeYouTubeCookie by rememberPreference(ytCookieKey, "")
    val activeYouTubeAccountHandle by rememberPreference(ytAccountChannelHandleKey, "")
    val activeYouTubeSession = YouTubeSessionStore.getCurrentSession(context)
    val activeYouTubeSessionId = activeYouTubeSession?.sessionId.orEmpty()
    val activeYouTubeAccountIdentity = listOf(
        activeYouTubeSessionId,
        activeYouTubeSession?.authUser.orEmpty(),
        activeYouTubeSession?.pageId.orEmpty(),
        activeYouTubeAccountHandle
    ).joinToString("|")

    val showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    val showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    val showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    val showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    val showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    val showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    val showMonthlyPlaylistInQuickPicks by rememberPreference(
        showMonthlyPlaylistInQuickPicksKey,
        true
    )
    val showTips by rememberPreference(showTipsKey, true)
    val showCharts by rememberPreference(showChartsKey, true)
    // ===== NOTIFICATION MESSAGE =====
    var cachedNotificationJson by rememberPreference("homeQuickPicksNotificationJson", "")
    val cachedNotificationData = remember(cachedNotificationJson) {
        notificationDataFromRawJson(cachedNotificationJson)
    }
    var notificationResult by remember { mutableStateOf<Result<NotificationData?>?>(null) }
    var notificationInit by remember { mutableStateOf(cachedNotificationData) }
    // ===== END NOTIFICATION MESSAGE =====

    LaunchedEffect(cachedNotificationData) {
        cachedNotificationData?.let { notificationInit = it }
    }

    val refreshScope = rememberCoroutineScope()
    val last50Year: Duration = 18250.days
    val from = last50Year.inWholeMilliseconds

    var selectedCountryCode by rememberPreference(selectedCountryCodeKey, Countries.ZZ)

    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    var loadedData by rememberPreference(loadedDataKey, false)

    val syncedLikedSongs by remember {
        Database.songTable
            .allFavorites(limit = 30)
            .distinctUntilChanged()
            .map { songs -> songs.asReversed().filter { it.id.isNotBlank() && it.title.isNotBlank() } }
    }.collectAsState(emptyList(), Dispatchers.IO)

    val syncedYtmArtists by remember {
        Database.artistTable
            .allFollowing()
            .distinctUntilChanged()
            .map { artists ->
                artists.filter { artist ->
                    artist.isYoutubeArtist &&
                        artist.id.isNotBlank() &&
                        !artist.name.isNullOrBlank()
                }
            }
    }.collectAsState(emptyList(), Dispatchers.IO)

    val syncedYtmAlbums by remember {
        Database.albumTable
            .all()
            .distinctUntilChanged()
            .map { albums ->
                albums.filter { album ->
                    album.isYoutubeAlbum &&
                        album.id.isNotBlank() &&
                        !album.title.isNullOrBlank()
                }
            }
    }.collectAsState(emptyList(), Dispatchers.IO)

    var sessionLikedSongsPreview by persistList<Song>("home/quickpicks/sessionLikedSongsPreview")

    suspend fun currentAccountLikedSongs(limit: Int = 120): List<Song> {
        val session = YouTubeSessionStore.applyCurrentSession(context) ?: return localLikedSongs(limit)
        if (!YouTubeSessionStore.hasAuthCookies(session.cookie)) return localLikedSongs(limit)

        val sessionSongs = YtmSessionApi.fetchLikedSongs(
            cookies = session.cookie,
            authUser = session.authUser.ifBlank { null },
            pageId = session.pageId.ifBlank { null }
        ).getOrNull().orEmpty()

        return sessionSongs
            .map { remoteSong ->
                Song(
                    id = remoteSong.id.ifBlank { remoteSong.videoId },
                    title = cleanPrefix(remoteSong.title),
                    artistsText = remoteSong.artistsText.ifBlank { remoteSong.artist.ifBlank { null } },
                    durationText = remoteSong.durationText.ifBlank { remoteSong.duration.ifBlank { null } },
                    thumbnailUrl = remoteSong.thumbnailUrl.ifBlank { remoteSong.thumbnail.ifBlank { null } }
                )
            }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .take(limit)
            .ifEmpty { localLikedSongs(limit) }
    }

    suspend fun preferredCachedSong(fallbackSong: Song): Song = withContext(Dispatchers.IO) {
        val storedSong = Database.songTable.findById(fallbackSong.id).first()
        if (storedSong == null) {
            fallbackSong
        } else {
            val mergedSong = storedSong.copy(
                title = storedSong.title.ifBlank { fallbackSong.title },
                artistsText = storedSong.artistsText ?: fallbackSong.artistsText,
                durationText = storedSong.durationText ?: fallbackSong.durationText,
                thumbnailUrl = storedSong.thumbnailUrl ?: fallbackSong.thumbnailUrl
            )

            if (mergedSong != storedSong) {
                Database.upsert(mergedSong)
            }

            mergedSong
        }
    }

    suspend fun preferredCachedMediaItem(songItem: Innertube.SongItem) =
        preferredCachedSong(songItem.asSong).asMediaItem

    suspend fun preferredCachedMediaItem(videoItem: Innertube.VideoItem) =
        preferredCachedSong(videoItem.asSong).asMediaItem

    suspend fun playHomeSectionItems(
        items: List<Any?>,
        clickedItem: Any
    ) {
        val playableItems = items.mapNotNull { sectionItem ->
            when (sectionItem) {
                is Innertube.SongItem -> preferredCachedMediaItem(sectionItem)
                is Innertube.VideoItem -> preferredCachedMediaItem(sectionItem)
                else -> null
            }
        }
        if (playableItems.isEmpty()) return

        val targetMediaId = when (clickedItem) {
            is Innertube.SongItem -> clickedItem.key
            is Innertube.VideoItem -> clickedItem.key
            else -> ""
        }
        val targetTitle = when (clickedItem) {
            is Innertube.SongItem -> clickedItem.info?.name.orEmpty()
            is Innertube.VideoItem -> clickedItem.info?.name.orEmpty()
            else -> ""
        }
        val targetArtist = when (clickedItem) {
            is Innertube.SongItem -> clickedItem.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
            is Innertube.VideoItem -> clickedItem.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
            else -> ""
        }

        val startIndex = playableItems.indexOfFirst { it.mediaId == targetMediaId }
            .takeIf { it >= 0 }
            ?: playableItems.indexOfFirst { mediaItem ->
                mediaItem.mediaMetadata.title?.toString() == targetTitle &&
                    mediaItem.mediaMetadata.artist?.toString().orEmpty() == targetArtist
            }.takeIf { it >= 0 }
            ?: 0
        binder?.stopRadio()
        PlaybackContextStore.set(context.getString(R.string.playing_from_quick_picks), targetTitle.ifBlank { context.getString(R.string.home) })
        binder?.player?.forcePlayAtIndex(playableItems, startIndex)
    }

    suspend fun playSessionHomeSectionItems(
        items: List<YtmHomeSectionItem>,
        clickedItem: YtmHomeSectionItem
    ) {
        val clickedSourceId = clickedItem.videoId.trim().ifBlank { clickedItem.id.trim() }
        val clickedTitle = cleanPrefix(clickedItem.title).trim()
        val clickedArtist = clickedItem.artistsText.trim()
            .ifBlank { clickedItem.subtitle.trim() }
        val clickedMediaId = if (clickedSourceId.isYouTubeVideoId()) {
            clickedSourceId
        } else {
            withTimeoutOrNull(12_000L) {
                findReplacementVideoId(
                    videoId = clickedSourceId,
                    titleHint = clickedTitle,
                    artistHint = clickedArtist
                )
            }?.takeIf { it.isYouTubeVideoId() }
        }

        if (clickedMediaId == null) {
            Timber.w(
                "Home recommendation has no playable source id=%s title=%s",
                clickedSourceId,
                clickedTitle
            )
            app.kreate.android.me.knighthat.utils.Toaster.w(
                "Searching could not find a playable version of this song"
            )
            return
        }

        val queuedSongs = items.mapNotNull { sectionItem ->
            val rawSong = sectionItem.asQuickPickSong() ?: return@mapNotNull null
            val sourceId = rawSong.id
            val song = when {
                sourceId.isYouTubeVideoId() -> rawSong
                sectionItem == clickedItem -> rawSong.copy(id = clickedMediaId)
                else -> null
            } ?: return@mapNotNull null

            val resolvedSong = preferredCachedSong(song)
            sectionItem to resolvedSong.asMediaItem
        }
        if (queuedSongs.isEmpty()) return

        val exactIndex = queuedSongs.indexOfFirst { (sourceItem, mediaItem) ->
            sourceItem == clickedItem &&
                mediaItem.mediaId == clickedMediaId
        }
        val idIndex = queuedSongs.indexOfFirst { (_, mediaItem) ->
            mediaItem.mediaId == clickedMediaId
        }
        val metadataIndex = queuedSongs.indexOfFirst { (_, mediaItem) ->
            mediaItem.mediaMetadata.title?.toString()?.trim() == clickedTitle &&
                mediaItem.mediaMetadata.artist?.toString().orEmpty().trim() == clickedArtist
        }
        val startIndex = listOf(exactIndex, idIndex, metadataIndex).firstOrNull { it >= 0 } ?: 0

        binder?.stopRadio()
        PlaybackContextStore.set(
            context.getString(R.string.playing_from_quick_picks),
            clickedTitle.ifBlank { context.getString(R.string.home) }
        )
        binder?.player?.forcePlayAtIndex(queuedSongs.map { it.second }, startIndex)
    }
    suspend fun loadData(forceReload: Boolean = false) {
        if (appRunningInBackground) return
        coroutineScope {
            val requestedChartsCountry = selectedCountryCode.name
            val shouldFetchCharts = showCharts && (
                chartsPageResult == null ||
                    forceReload ||
                    chartsPageCountryCode != requestedChartsCountry
                )
            val chartsDeferred = async(Dispatchers.IO) {
                if (shouldFetchCharts) {
                    Innertube.chartsPageComplete(countryCode = requestedChartsCountry)
                } else {
                    chartsPageResult
                }
            }

            val notificationDeferred = async(Dispatchers.IO) {
                if (notificationResult == null || forceReload) {
                    runCatching<Pair<NotificationData?, String?>> {
                        val sources = listOf(
                            SecureApiConfig.cubicNotificationConfigUrl,
                            SecureApiConfig.cubicNotificationConfigFallbackUrl
                        )
                        sources.firstNotNullOfOrNull { url ->
                            runCatching {
                                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                try {
                                    connection.requestMethod = "GET"
                                    connection.connectTimeout = 5_000
                                    connection.readTimeout = 5_000
                                    connection.setRequestProperty("Accept", "application/json")

                                    if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                                        val parsedNotification = notificationDataFromRawJson(response)
                                        parsedNotification?.let { it to response }
                                    } else {
                                        null
                                    }
                                } finally {
                                    connection.disconnect()
                                }
                            }.getOrNull()
                        } ?: (null to null)
                    }
                } else {
                    notificationResult?.map { notification -> Pair(notification, null as String?) }
                }
            }

            val discoverDeferred = async(Dispatchers.IO) {
                if ((showNewAlbums || showNewAlbumsArtists || showMoodsAndGenres) && (discoverPageResult == null || forceReload)) {
                    if (!forceReload && HomeFeedSessionCache.discoverPage != null) {
                        Result.success(HomeFeedSessionCache.discoverPage)
                    } else {
                        Innertube.discoverPage()
                    }
                } else {
                    discoverPageResult
                }
            }

            val guestHomeDeferred = async(Dispatchers.IO) {
                val needsGuestHome =
                    forceReload ||
                        (selectedHomeChipParams.isBlank() && HomeFeedSessionCache.directHomePage == null) ||
                        homePageResult == null ||
                        homePageResult?.isFailure == true ||
                        homePageResult?.getOrNull()?.sections.isNullOrEmpty()
                if (needsGuestHome) {
                    val cached = if (!forceReload && selectedHomeChipParams.isBlank()) {
                        HomeFeedSessionCache.directHomePage
                    } else {
                        null
                    }
                    cached?.let { Result.success(it as HomePage?) }
                        ?: YtMusic.getHomePage(
                            setLogin = false,
                            params = selectedHomeChipParams.takeIf(String::isNotBlank)
                        ).map { it as HomePage? }
                } else {
                    homePageResult
                }
            }

            val publicGuestHomeDeferred = async(Dispatchers.IO) {
                val cached = if (!forceReload) HomeFeedSessionCache.publicGuestSections else null
                cached?.let { Result.success(it) }
                    ?: YtmSessionApi.fetchAllHomeFeed(
                        cookies = "",
                        guest = true,
                        maxPages = 50,
                        safetyCap = 5,
                    ).map { feed -> feed.sections }
            }

            val homeDeferred = async(Dispatchers.IO) {
                val accountKey = activeYouTubeAccountIdentity.ifBlank {
                    activeYouTubeCookie.hashCode().toString()
                }
                val shouldFetchSessionHomeFeed =
                    isYouTubeLoggedIn() && (
                        forceReload ||
                            sessionHomeFeedResult == null ||
                            sessionHomeFeedResult?.isFailure == true ||
                            sessionHomeFeedResult?.getOrNull().isNullOrEmpty()
                        )
                if (shouldFetchSessionHomeFeed) {
                    val cachedSignedSections = HomeFeedSessionCache.signedSections(accountKey)
                    val session = YouTubeSessionStore.applyCurrentSession(context)
                        ?.let { YtmSessionApi.ensureScopedSession(it) }
                    val cookie = session?.cookie?.takeIf { it.isNotBlank() }
                    if (cookie.isNullOrBlank()) {
                        Result.success(cachedSignedSections)
                    } else {
                        val fetched = YouTubeRequestThrottler.run {
                            YtmSessionApi.fetchAllHomeFeed(
                                cookies = cookie,
                                authUser = session.authUser.ifBlank { null },
                                pageId = session.pageId.ifBlank { null },
                                maxPages = 50,
                                safetyCap = 5
                            ).map { it.sections }
                        }
                        if (fetched.isSuccess) fetched
                        else cachedSignedSections?.let { Result.success(it) } ?: fetched
                    }
                } else {
                    sessionHomeFeedResult
                }
            }

            val likedSongsDeferred = async(Dispatchers.IO) {
                if (isYouTubeLoggedIn() && (forceReload || sessionLikedSongsPreview.isEmpty())) {
                    currentAccountLikedSongs()
                } else if (isYouTubeLoggedIn()) {
                    sessionLikedSongsPreview
                } else {
                    emptyList()
                }
            }

            chartsPageResult = chartsDeferred.await()
            if (shouldFetchCharts) {
                chartsPageResult?.getOrNull()?.let { chartsPage ->
                    chartsPageInit = chartsPage
                    chartsPageCountryCode = requestedChartsCountry
                }
            }
            val notificationFetch = notificationDeferred.await()
            notificationResult = notificationFetch?.map { it.first }
            notificationFetch?.getOrNull()?.second
                ?.takeIf { it.isNotBlank() }
                ?.let { cachedNotificationJson = it }
            discoverPageResult = discoverDeferred.await()
            discoverPageResult?.getOrNull()?.let(HomeFeedSessionCache::storeDiscover)
            homePageResult = guestHomeDeferred.await()
            if (selectedHomeChipParams.isBlank()) {
                homePageResult?.getOrNull()?.let(HomeFeedSessionCache::storeDirectHome)
            }
            publicGuestHomeFeedResult = publicGuestHomeDeferred.await()
            publicGuestHomeFeedResult?.getOrNull()
                ?.let(HomeFeedSessionCache::storePublicGuest)
            sessionHomeFeedResult = homeDeferred.await()
            sessionHomeFeedResult?.getOrNull()?.let { sections ->
                val accountKey = activeYouTubeAccountIdentity.ifBlank {
                    activeYouTubeCookie.hashCode().toString()
                }
                HomeFeedSessionCache.storeSigned(accountKey, sections)
            }
            sessionLikedSongsPreview = likedSongsDeferred.await()
        }

        if (!forceReload) {
            if (relatedInit == null) {
                relatedInit = relatedPreference ?: relatedPageResult?.getOrNull()
            }
            if (discoverPageInit == null) {
                discoverPageInit = discoverPagePreference ?: discoverPageResult?.getOrNull() ?: discoverPageInit
            }
            if (chartsPageInit == null) {
                chartsPageInit = chartsPageResult?.getOrNull() ?: chartsPageInit
            }
            if (
                isYouTubeLoggedIn() &&
                sessionHomeFeedInit == null &&
                activeYouTubeSessionId == sessionHomeFeedSessionId &&
                activeYouTubeAccountIdentity == sessionHomeFeedAccountHandle
            ) {
                sessionHomeFeedInit = sessionHomeFeedResult?.getOrNull()
            }
        }

        val isUiHydrated =
            relatedInit != null &&
                discoverPageInit != null &&
                homePageInit != null &&
                (!showCharts || chartsPageInit != null) &&
                publicGuestHomeFeedResult != null &&
                (!isYouTubeLoggedIn() || sessionHomeFeedInit != null || sessionHomeFeedResult != null)

        if (loadedData && !forceReload && isUiHydrated) return

        refreshScope.launch(Dispatchers.IO) {
            runCatching {
                when (playEventType) {
                    PlayEventsType.MostPlayed -> {
                        val songs = Database.eventTable
                            .findSongsMostPlayedBetween(from = from, limit = localCount)
                            .distinctUntilChanged()
                            .first()

                        trendingList = songs.distinctBy { it.id }.take(localCount)
                        trending = trendingList.firstOrNull()
                        mostPopularSong = trending

                        relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                    }

                    PlayEventsType.LastPlayed -> {
                        val songs = Database.eventTable
                            .findSongsLastPlayed(limit = localCount)
                            .distinctUntilChanged()
                            .first()

                        trendingList = songs.distinctBy { it.id }.take(localCount)
                        trending = trendingList.firstOrNull()
                        mostPopularSong = trending

                        relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                    }

                    PlayEventsType.CasualPlayed -> {
                        val favoriteSongs = Database.eventTable
                            .findSongsMostPlayedBetween(from = 0, limit = 10)
                            .distinctUntilChanged()
                            .first()
                        val accountLikedSongs = if (isYouTubeLoggedIn()) currentAccountLikedSongs() else emptyList()
                        val discoverySongs = localDiscoverySongs()
                        val personalizedHomeSongs = if (isYouTubeLoggedIn()) {
                            (homePersonalizedSongs() + accountLikedSongs).distinctBy { it.id }
                        } else {
                            legacyHomePersonalizedSongs()
                        }
                        val casualSongs = casualFallbackSongs(personalizedHomeSongs)
                        showNewUserMessage = personalizedHomeSongs.isEmpty() && favoriteSongs.isEmpty() && casualSongs.isEmpty()

                        val (mixedSongs, relatedResult) = buildCasualMix(
                            personalizedHomeSongs = personalizedHomeSongs,
                            favoriteSongs = favoriteSongs,
                            discoverySongs = discoverySongs
                        )

                        trendingList = mixedSongs.ifEmpty { casualSongs.shuffled().take(localCount) }
                        rememberCasualMix(trendingList)
                        trending = trendingList.firstOrNull()
                        relatedPageResult = relatedResult

                        if (relatedPageResult?.getOrNull() == null && trendingList.isNotEmpty()) {
                            relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                        }
                    }
                }
            }.onFailure {
                Timber.e("Failed loadData in QuickPicsModern ${it.stackTraceToString()}")
                loadedData = false
            }.onSuccess {
                Timber.d("Success loadData in QuickPicsModern")
                trendingPreference = trending
                relatedInit = relatedPageResult?.getOrNull()
                relatedPreference = relatedInit
                discoverPageInit = discoverPageResult?.getOrNull() ?: discoverPageInit
                discoverPagePreference = discoverPageInit
                chartsPageInit = chartsPageResult?.getOrNull() ?: chartsPageInit
                notificationInit = notificationResult?.getOrNull() ?: notificationInit

                homePageResult?.getOrNull()?.let { fetchedHomePage ->
                    homePageInit = fetchedHomePage
                    homePagePreference = fetchedHomePage
                    if (activeYouTubeSessionId.isNotBlank()) {
                        homePageSessionId = activeYouTubeSessionId
                    }
                    homePageAccountHandle = activeYouTubeAccountIdentity
                }
                sessionHomeFeedResult?.getOrNull()?.let { fetchedHomeFeed ->
                    sessionHomeFeedInit = fetchedHomeFeed
                    if (activeYouTubeSessionId.isNotBlank()) {
                        sessionHomeFeedSessionId = activeYouTubeSessionId
                    }
                    sessionHomeFeedAccountHandle = activeYouTubeAccountIdentity
                }

                loadedData = true
            }
        }
    }

    LaunchedEffect(playEventType) {
        val cachedRelatedPage = relatedPageResult?.getOrNull() ?: relatedInit
        val hasRelatedContent = cachedRelatedPage?.let { page ->
            !page.songs.isNullOrEmpty() ||
                !page.playlists.isNullOrEmpty() ||
                !page.albums.isNullOrEmpty() ||
                !page.artists.isNullOrEmpty()
        } == true
        if (loadedData && trendingList.isNotEmpty() && hasRelatedContent) return@LaunchedEffect

        // Only reset trending-related data
        trendingList = emptyList()
        trending = null
        relatedPageResult = null
        relatedInit = null
        mostPopularSong = null
        showNewUserMessage = false

        refreshScope.launch(Dispatchers.IO) {
            runCatching {
                when (playEventType) {
                    PlayEventsType.MostPlayed -> {
                        Database.eventTable
                            .findSongsMostPlayedBetween(from = from, limit = localCount)
                            .distinctUntilChanged()
                            .collect { songs ->
                                trendingList = songs.distinctBy { it.id }.take(localCount)
                                trending = trendingList.firstOrNull()
                                mostPopularSong = trending

                                relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                                return@collect
                            }
                    }

                    PlayEventsType.LastPlayed -> {
                        Database.eventTable
                            .findSongsLastPlayed(limit = localCount)
                            .distinctUntilChanged()
                            .collect { songs ->
                                trendingList = songs.distinctBy { it.id }.take(localCount)
                                trending = trendingList.firstOrNull()
                                mostPopularSong = trending

                                relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                                return@collect
                            }
                    }

                    PlayEventsType.CasualPlayed -> {
                        Database.eventTable
                            .findSongsMostPlayedBetween(from = 0, limit = 10)
                            .distinctUntilChanged()
                            .collect { favoriteSongs ->
                                val accountLikedSongs = if (isYouTubeLoggedIn()) currentAccountLikedSongs() else emptyList()
                                val discoverySongs = localDiscoverySongs()
                                val personalizedHomeSongs = if (isYouTubeLoggedIn()) {
                                    (homePersonalizedSongs() + accountLikedSongs).distinctBy { it.id }
                                } else {
                                    legacyHomePersonalizedSongs()
                                }
                                val casualSongs = casualFallbackSongs(personalizedHomeSongs)
                                showNewUserMessage = personalizedHomeSongs.isEmpty() && favoriteSongs.isEmpty() && casualSongs.isEmpty()

                                val (mixedSongs, relatedResult) = buildCasualMix(
                                    personalizedHomeSongs = personalizedHomeSongs,
                                    favoriteSongs = favoriteSongs,
                                    discoverySongs = discoverySongs
                                )

                                trendingList = mixedSongs.ifEmpty { casualSongs.shuffled().take(localCount) }
                                rememberCasualMix(trendingList)
                                trending = trendingList.firstOrNull()
                                relatedPageResult = relatedResult

                                if (relatedPageResult?.getOrNull() == null && trendingList.isNotEmpty()) {
                                    relatedPageResult = loadRelatedForSeeds(trendingList.take(3))
                                }
                                relatedInit = relatedPageResult?.getOrNull()
                                relatedPreference = relatedInit
                                return@collect
                            }
                    }
                }
            }
        }
    }

    LaunchedEffect(
        playEventType,
        showRelatedAlbums,
        showSimilarArtists,
        trendingList.joinToString(separator = ",") { song -> song.id }
    ) {
        if (trendingList.isEmpty()) return@LaunchedEffect

        val current = relatedPageResult?.getOrNull() ?: relatedInit
        val albumsMissing = showRelatedAlbums && current?.albums.isNullOrEmpty()
        val artistsMissing = showSimilarArtists && current?.artists.isNullOrEmpty()
        if (!albumsMissing && !artistsMissing) return@LaunchedEffect

        val refreshed = withContext(Dispatchers.IO) {
            loadRelatedForSeeds(trendingList)
        }?.getOrNull()
        val merged = mergeRelatedPages(current, refreshed)
        if (merged != null) {
            relatedPageResult = Result.success(merged)
            relatedInit = merged
            relatedPreference = merged
            Timber.d(
                "Related home shelves refreshed albums=%s artists=%s seeds=%s",
                merged.albums.orEmpty().size,
                merged.artists.orEmpty().size,
                trendingList.count { song -> song.id.isYouTubeVideoId() }
            )
        }
    }

    LaunchedEffect(
        showSimilarArtists,
        trendingList.joinToString(separator = ",") { song -> song.id }
    ) {
        if (!showSimilarArtists || trendingList.isEmpty()) {
            similarArtistShelves = emptyList()
            return@LaunchedEffect
        }

        similarArtistShelves = withContext(Dispatchers.IO) {
            loadSimilarArtistShelves(trendingList)
        }
    }

    LaunchedEffect(activeYouTubeCookie, activeYouTubeSessionId, activeYouTubeAccountIdentity) {
        if (!YouTubeSessionStore.hasAuthCookies(activeYouTubeCookie)) {
            sessionHomeFeedResult = null
            sessionHomeFeedInit = null
            sessionHomeFeedSessionId = ""
            sessionHomeFeedAccountHandle = ""
            sessionLikedSongsPreview = emptyList()
            return@LaunchedEffect
        }

        if (
            activeYouTubeSessionId.isNotBlank() &&
            (homePageSessionId != activeYouTubeSessionId || homePageAccountHandle != activeYouTubeAccountIdentity)
        ) {
            sessionHomeFeedResult = null
            sessionHomeFeedInit = null
            sessionHomeFeedSessionId = activeYouTubeSessionId
            sessionHomeFeedAccountHandle = activeYouTubeAccountIdentity
            sessionLikedSongsPreview = emptyList()
            loadData(forceReload = false)
        }
    }

    LaunchedEffect(selectedCountryCode, showCharts) {
        val requestedCountry = selectedCountryCode.name
        if (!showCharts || chartsPageCountryCode == requestedCountry) return@LaunchedEffect

        val result = withContext(Dispatchers.IO) {
            Innertube.chartsPageComplete(countryCode = requestedCountry)
        }
        if (selectedCountryCode.name != requestedCountry) return@LaunchedEffect

        chartsPageResult = result
        result?.getOrNull()?.let { chartsPage ->
            chartsPageInit = chartsPage
            chartsPageCountryCode = requestedCountry
        }
    }

    LaunchedEffect(Unit) {
        if (selectedHomeChipTitle.isNotBlank() || selectedHomeChipParams.isNotBlank()) {
            selectedHomeChipTitle = ""
            selectedHomeChipParams = ""
            homePageResult = null
            homePageInit = null
            homePagePreference = null
        }
        val needsGuestHomeFeed = HomeFeedSessionCache.directHomePage == null
        val needsPublicGuestHomeFeed = publicGuestHomeFeedResult == null
        val needsSignedInHomeFeed =
            isYouTubeLoggedIn() && sessionHomeFeedInit == null && sessionHomeFeedResult == null
        val shouldLoad =
            !loadedData ||
                relatedInit == null ||
                discoverPageInit == null ||
                needsGuestHomeFeed ||
                needsPublicGuestHomeFeed ||
                (showCharts && chartsPageInit == null) ||
                needsSignedInHomeFeed

        if (shouldLoad) loadData(forceReload = false)
    }

    var refreshing by remember { mutableStateOf(false) }
    var lastManualRefreshMs by remember { mutableStateOf(0L) }

    fun refresh() {
        if (refreshing || appRunningInBackground) return
        val now = System.currentTimeMillis()
        if (now - lastManualRefreshMs < 30_000L) return
        lastManualRefreshMs = now

        refreshScope.launch(Dispatchers.IO) {
            if (appRunningInBackground) return@launch
            refreshing = true

            // Clear trending data
            trendingList = emptyList()
            trending = null
            relatedPageResult = null
            relatedInit = null
            mostPopularSong = null

            // DON'T reset loaded flag

            // Reload with force=true (only refreshes what's needed)
            loadData(forceReload = true)

            delay(500)
            refreshing = false
        }
    }

    fun selectPlayEventType(target: PlayEventsType) {
        if (playEventType == target) {
            refresh()
        } else {
            loadedData = false
            playEventType = target
        }
    }

    val songThumbnailSizeDp = 92.dp
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 92.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 80.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 92.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()
    val ytmLikesLazyGridState = rememberLazyGridState()
    val moodAngGenresLazyGridState = rememberLazyGridState()
    val chartsPageSongLazyGridState = rememberLazyGridState()
    val chartsPageArtistLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    val downloadedSongs = remember {
        MyDownloadHelper.downloads.value.filter {
            it.value.state == Download.STATE_COMPLETED
        }.keys.toList()
    }
    val cachedSongs = remember {
        binder?.cache?.keys?.toMutableList()
    }
    cachedSongs?.addAll(downloadedSongs)

    val hapticFeedback = LocalHapticFeedback.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val signedApiHomeSections = remember(sessionHomeFeedResult, sessionHomeFeedInit) {
        sessionHomeFeedResult?.getOrNull() ?: sessionHomeFeedInit.orEmpty()
    }
    val guestApiHomeSections = remember(publicGuestHomeFeedResult) {
        publicGuestHomeFeedResult?.getOrNull().orEmpty()
    }
    val liveApiHomeSections = remember(signedApiHomeSections, guestApiHomeSections) {
        mergeYtmHomeSections(signedApiHomeSections, guestApiHomeSections)
    }
    val activeRelatedPage = relatedPageResult?.getOrNull() ?: relatedInit
    val newReleaseApiSections = remember(liveApiHomeSections) {
        liveApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.NEW_RELEASES }
    }
    val newAlbumApiSections = remember(liveApiHomeSections) {
        liveApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.NEW_ALBUMS }
    }
    val relatedAlbumApiSections = remember(liveApiHomeSections) {
        liveApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.RELATED_ALBUMS }
    }
    val similarArtistApiSections = remember(liveApiHomeSections) {
        liveApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.SIMILAR_ARTISTS }
    }
    val tasteInnertubeArtists = remember(activeRelatedPage) {
        activeRelatedPage
            ?.artists
            .orEmpty()
            .filter { artist -> artist.key.isNotBlank() && !artist.info?.name.isNullOrBlank() }
            .distinctBy(Innertube.ArtistItem::key)
    }
    val tasteInnertubeArtistIds = remember(tasteInnertubeArtists) {
        tasteInnertubeArtists.mapTo(mutableSetOf(), Innertube.ArtistItem::key)
    }
    val tasteSessionArtistItems = remember(similarArtistApiSections, tasteInnertubeArtistIds) {
        similarArtistApiSections
            .flatMap(YtmHomeSection::items)
            .filter { item -> item.type.equals("artist", ignoreCase = true) }
            .filter { item -> item.title.isNotBlank() && item.artistDestinationId().isNotBlank() }
            .distinctBy(YtmHomeSectionItem::artistDestinationId)
            .filterNot { item -> item.artistDestinationId() in tasteInnertubeArtistIds }
    }
    val hasTasteArtistRecommendations =
        tasteInnertubeArtists.isNotEmpty() || tasteSessionArtistItems.isNotEmpty()
    val guestQuickPickSections = remember(guestApiHomeSections) {
        guestApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.QUICK_PICKS }
    }
    val topArtistApiSections = remember(liveApiHomeSections) {
        liveApiHomeSections.filter { homeQuickSectionGroup(it.title) == HomeQuickSectionGroup.TOP_ARTISTS }
    }
    val remainingApiHomeSections = remember(liveApiHomeSections) {
        liveApiHomeSections
            .filter { section ->
                homeQuickSectionGroup(section.title) == HomeQuickSectionGroup.OTHER &&
                    section.items.isNotEmpty()
            }
            .sortedBy { section -> section.title.lowercase() }
    }
    val playApiHomeItem: (List<YtmHomeSectionItem>, YtmHomeSectionItem) -> Unit =
        { sectionItems, item ->
            refreshScope.launch {
                playSessionHomeSectionItems(sectionItems, item)
            }
        }
    val tasteSessionPlaylistItems = remember(liveApiHomeSections) {
        liveApiHomeSections
            .filter { section -> isTastePlaylistSection(section.title) }
            .flatMap(YtmHomeSection::items)
            .filter { item -> item.type.equals("playlist", ignoreCase = true) }
            .distinctBy { item -> item.playlistId.ifBlank { item.browseId } }
            .filter { item -> item.playlistId.isNotBlank() || item.browseId.isNotBlank() }
    }
    val tasteGuestPlaylistItems = remember(homePageResult, homePageInit) {
        (homePageResult?.getOrNull() ?: homePageInit)
            ?.sections
            .orEmpty()
            .filter { section -> isTastePlaylistSection(section.title) }
            .flatMap(HomePage.Section::items)
            .filterIsInstance<Innertube.PlaylistItem>()
            .distinctBy(Innertube.PlaylistItem::key)
            .filter { item -> item.key.isNotBlank() }
    }
    val allHitsPlaylistIds = remember(liveApiHomeSections, homePageResult, homePageInit) {
        buildSet {
            liveApiHomeSections
                .filter { section -> section.title.contains("all hits", ignoreCase = true) }
                .flatMap(YtmHomeSection::items)
                .forEach { item ->
                    normalizedPlaylistId(item.playlistId.ifBlank { item.browseId })
                        .takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            (homePageResult?.getOrNull() ?: homePageInit)
                ?.sections
                .orEmpty()
                .filter { section -> section.title.contains("all hits", ignoreCase = true) }
                .flatMap(HomePage.Section::items)
                .filterIsInstance<Innertube.PlaylistItem>()
                .forEach { item ->
                    normalizedPlaylistId(item.key)
                        .takeIf(String::isNotBlank)
                        ?.let(::add)
                }
        }
    }
    val radioPlaylistItems = remember(activeRelatedPage) {
        activeRelatedPage
            ?.playlists
            .orEmpty()
            .distinctBy(Innertube.PlaylistItem::key)
            .filter { item -> item.key.isNotBlank() }
    }
    val uniqueRadioPlaylistItems = remember(radioPlaylistItems, allHitsPlaylistIds) {
        radioPlaylistItems.filterNot { item ->
            normalizedPlaylistId(item.key) in allHitsPlaylistIds
        }
    }
    val uniqueSessionPlaylistItems = remember(tasteSessionPlaylistItems, allHitsPlaylistIds) {
        tasteSessionPlaylistItems.filterNot { item ->
            normalizedPlaylistId(item.playlistId.ifBlank { item.browseId }) in allHitsPlaylistIds
        }
    }
    val uniqueGuestPlaylistItems = remember(tasteGuestPlaylistItems, allHitsPlaylistIds) {
        tasteGuestPlaylistItems.filterNot { item ->
            normalizedPlaylistId(item.key) in allHitsPlaylistIds
        }
    }
    val hasTastePlaylistRecommendations =
        uniqueRadioPlaylistItems.isNotEmpty() ||
            uniqueSessionPlaylistItems.isNotEmpty() ||
            uniqueGuestPlaylistItems.isNotEmpty()
    val hasRenderableHomeContent =
        trendingList.isNotEmpty() ||
            activeRelatedPage != null ||
            discoverPageInit != null ||
            homePageInit?.sections.orEmpty().isNotEmpty() ||
            liveApiHomeSections.isNotEmpty() ||
            chartsPageInit != null

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = ::refresh
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(
                    if (NavigationBarPosition.Right.isCurrent())
                        Dimensions.contentWidthRightBar
                    else
                        1f
                )
        ) {
            val quickPicksLazyGridItemWidthFactor =
                if (isLandscape && maxWidth * 0.475f >= 320.dp) {
                    0.34f
                } else {
                    0.58f
                }
            val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

            val moodItemWidthFactor =
                if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.42f else 0.78f
            val itemWidth = maxWidth * moodItemWidthFactor

            Column(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {

                if (UiType.ViMusic.isCurrent())
                    HeaderWithIcon(
                        title = if (!isYouTubeLoggedIn()) stringResource(R.string.quick_picks)
                        else stringResource(R.string.home),
                        iconId = R.drawable.search,
                        enabled = true,
                        showIcon = !showSearchTab,
                        modifier = Modifier,
                        onClick = onSearchClick
                    )

                WelcomeMessage(
                    onOpenAccountsSettings = {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("settings_tab_index", 5)
                        navController.navigate(NavRoutes.settings.name)
                    }
                )

                if (showTips) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tips title + dropdown button (grouped together on left)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                menuState.display {
                                    Menu {
                                        MenuEntry(
                                            icon = R.drawable.chevron_up,
                                            text = stringResource(R.string.by_most_played_song),
                                            onClick = {
                                                selectPlayEventType(PlayEventsType.MostPlayed)
                                                menuState.hide()
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.chevron_down,
                                            text = stringResource(R.string.by_last_played_song),
                                            onClick = {
                                                selectPlayEventType(PlayEventsType.LastPlayed)
                                                menuState.hide()
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.random,
                                            text = stringResource(R.string.by_casual_played_song),
                                            onClick = {
                                                selectPlayEventType(PlayEventsType.CasualPlayed)
                                                menuState.hide()
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            BasicText(
                                text = stringResource(R.string.tips),
                                style = typography().l.semiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(R.drawable.chevron_down),
                                contentDescription = "Play events type",
                                tint = colorPalette().text,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Spacer to push icons to the right
                        Spacer(modifier = Modifier.weight(1f))

                        // Home + Play icons (grouped together on right)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isYouTubeLoggedIn()) {
                                IconButton(
                                    onClick = {
                                        navController.navigate(NavRoutes.chipsPage.name)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.multipage),
                                        contentDescription = "More YT Music home",
                                        tint = colorPalette().accent
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    showFindSheet = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.equalizer),
                                    contentDescription = "Find song",
                                    tint = colorPalette().accent
                                )
                            }

                            // Play icon
                            IconButton(
                                onClick = {
                                    refreshScope.launch {
                                        val queue = buildList {
                                            trending?.let { add(preferredCachedSong(it).asMediaItem) }
                                            addAll(
                                                relatedInit?.songs
                                                    ?.map { preferredCachedSong(it.asSong).asMediaItem }
                                                    .orEmpty()
                                            )
                                        }.distinctBy { it.mediaId }
                                        binder?.stopRadio()
                                        PlaybackContextStore.set(context.getString(R.string.playing_from_quick_picks), context.resources.getString(playEventType.textId))
                                        binder?.player?.forcePlayAtIndex(queue, 0)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = stringResource(R.string.play),
                                    tint = colorPalette().text
                                )
                            }
                        }
                    }

                    BasicText(
                        text = playEventType.text,
                        style = typography().xxs.secondary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )

                    if (relatedPageResult != null) {
                        // Keep a compact 3 x 5 discovery set: three vertical rows and
                        // five horizontal columns of real recommendations.

                        // KEEP the persistList for persistence
                        var recommendations by persistList<Song>("home/quickpicks/recommendations_list")

                        // Use LaunchedEffect to update recommendations when source data changes
                        LaunchedEffect(trendingList, relatedInit, localCount, playEventType) {
                            val mainIds = trendingList.map { it.id }.toSet()

                            // Keep recommendations stable during the day while rotating discovery daily.
                            val discoveryDay = System.currentTimeMillis() / 86_400_000L
                            val seed = (trendingList.joinToString { it.id } +
                                (relatedInit?.songs?.joinToString { it.key } ?: "") +
                                discoveryDay).hashCode()
                            val random = kotlin.random.Random(seed)

                            val newRecommendations = if (playEventType == PlayEventsType.MostPlayed ||
                                playEventType == PlayEventsType.LastPlayed) {
                                val first = trendingList.firstOrNull()
                                val others = trendingList.drop(1)
                                val relatedSongs = relatedInit?.songs
                                    ?.map { it.asSong }
                                    ?.filter { it.id !in mainIds }
                                    ?.distinctBy { it.id }
                                    ?.take((15 - (1 + others.size)).coerceAtLeast(0))
                                    .orEmpty()
                                val total = (others + relatedSongs)
                                val extra = if (total.size < 15) {
                                    relatedInit?.songs
                                        ?.map { it.asSong }
                                        ?.filter { it.id !in (others.map { s -> s.id } + (first?.id ?: "")) }
                                        ?.distinctBy { it.id }
                                        ?.take((15 - total.size).coerceAtLeast(0))
                                        .orEmpty()
                                } else emptyList()

                                // Use seeded random for consistent shuffling
                                (listOfNotNull(first) + (total + extra)).shuffled(random).distinctBy { it.id }.take(15)
                            } else {
                                // Random Mode will randomize the list: all mixed
                                val locals = trendingList.take(localCount)
                                val relatedSongs = relatedInit?.songs
                                    ?.map { it.asSong }
                                    ?.filter { it.id !in locals.map { it.id } }
                                    ?.distinctBy { it.id }
                                    ?.take((15 - locals.size).coerceAtLeast(0))
                                    .orEmpty()
                                val total = (locals + relatedSongs)
                                val extra = if (total.size < 15) {
                                    relatedInit?.songs
                                        ?.map { it.asSong }
                                        ?.filter { it.id !in total.map { s -> s.id } }
                                        ?.distinctBy { it.id }
                                        ?.take((15 - total.size).coerceAtLeast(0))
                                        .orEmpty()
                                } else emptyList()

                                // Use seeded random for consistent shuffling
                                (total + extra).shuffled(random).distinctBy { it.id }.take(15)
                            }

                            // Update the persisted recommendations
                            recommendations = newRecommendations
                        }

                        LazyHorizontalGrid(
                            state = quickPicksLazyGridState,
                            rows = GridCells.Fixed(3),
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            contentPadding = endPaddingValues,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimensions.itemsVerticalPadding * 3 * 9)
                        ) {
                            items(recommendations, key = { it.id }) { song ->
                                app.kreate.android.me.knighthat.component.SongItem(
                                    song = song,
                                    navController = navController,
                                    onClick = {
                                        PlaybackContextStore.set(context.getString(R.string.playing_from_quick_picks), context.resources.getString(playEventType.textId))
                                        binder?.startRadio(song, true)
                                    },
                                    modifier = Modifier.width(itemInHorizontalGridWidth),
                                    thumbnailOverlay = {
                                        if (playEventType != PlayEventsType.CasualPlayed &&
                                            mostPopularSong != null &&
                                            song.id == mostPopularSong!!.id) {
                                            Image(
                                                painter = painterResource(R.drawable.star_brilliant),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette().accent),
                                                modifier = Modifier
                                                    .size(23.dp)
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (relatedPageResult == null) Loader()
                }

                // Add this message for new users
                if (showNewUserMessage && playEventType == PlayEventsType.CasualPlayed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 8.dp)
                            .background(
                                color = colorPalette().background1.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            text = "First listen to some songs to get personalized recommendations",
                            style = typography().s.center.color(colorPalette().textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                notificationInit?.let { notification ->
                    val currentVersion = BuildConfig.VERSION_NAME
                    val hasNewUpdate = isNewerVersion(notification.version, currentVersion)
                    val showEmergency = notification.force_update && hasNewUpdate

                    fun openExternalUrl(targetUrl: String) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse(targetUrl)
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        }.onFailure { error ->
                            Timber.w(error, "HomeQuickPicks failed to open external url")
                        }
                    }

                    if (notification.show || hasNewUpdate) {
                        Spacer(modifier = Modifier.height(16.dp))
                        RemoteConfigQuickPicksCard(
                            notification = notification,
                            hasNewUpdate = hasNewUpdate,
                            showEmergency = showEmergency,
                            onOpenUrl = ::openExternalUrl,
                            onOpenAboutUpdate = {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("settings_tab_index", 8)
                                navController.navigate(NavRoutes.settings.name)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }                // ===== END NOTIFICATION MESSAGE SECTION =====

                YtmHomeFeedSections(
                    sections = newReleaseApiSections,
                    endPaddingValues = endPaddingValues,
                    onPlayableClick = playApiHomeItem,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                )


                discoverPageInit?.let { page ->
                    val artists by remember {
                        Database.artistTable
                            .sortFollowingByName()
                            .distinctUntilChanged()
                    }.collectAsState(emptyList(), Dispatchers.IO)

                    var newReleaseAlbumsFiltered by persistList<Innertube.AlbumItem>("home/shared/newalbumsartist")
                    page.newReleaseAlbums.forEach { album ->
                        artists.forEach { artist ->
                            if (artist.name == album.authors?.first()?.name) {
                                newReleaseAlbumsFiltered += album
                            }
                        }
                    }

                    if (showNewAlbumsArtists)
                        if (newReleaseAlbumsFiltered.isNotEmpty() && artists.isNotEmpty()) {

                            BasicText(
                                text = stringResource(R.string.new_albums_of_your_artists),
                                style = typography().l.semiBold,
                                modifier = sectionTextModifier
                            )

                            LazyRow(contentPadding = endPaddingValues) {
                                items(
                                    items = newReleaseAlbumsFiltered.distinctBy { it.key },
                                    key = { homeQuickAlbumKey(it) }) {
                                    AlbumItem(
                                        album = it,
                                        thumbnailSizePx = albumThumbnailSizePx,
                                        thumbnailSizeDp = albumThumbnailSizeDp,
                                        alternative = true,
                                        modifier = Modifier.clickable(onClick = {
                                            onAlbumClick(it.key)
                                        }),
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        }

                    if (showNewAlbums) {
                        Title(
                            title = stringResource(R.string.new_albums),
                            onClick = { navController.navigate(NavRoutes.newAlbums.name) },
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = page.newReleaseAlbums.distinctBy { it.key },
                                key = { homeQuickAlbumKey(it) }) {
                                AlbumItem(
                                    album = it,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier.clickable(onClick = {
                                        onAlbumClick(it.key)
                                    }),
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    }


                }

                if (discoverPageInit?.newReleaseAlbums.isNullOrEmpty()) {
                    YtmHomeFeedSections(
                        sections = newAlbumApiSections,
                        endPaddingValues = endPaddingValues,
                        onPlayableClick = playApiHomeItem,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                // Show only from December 6th (month=11, day>=6) to December 31st (month=11, day<=31)
                if (currentMonth == 11 && currentDay in 6..31) {
                    val currentYear = calendar.get(java.util.Calendar.YEAR)

                    // Randomly select between BlackCherryCosmos and GoldenMagma
                    val selectedShader = remember {
                        if (Random.nextBoolean()) BlackCherryCosmos else GoldenMagma
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shaderBackground(selectedShader)
                            .clickable {
                                navController.navigate("rewind")
                            }
                    ) {
                        // Subtle overlay for better text readability on both shaders
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (selectedShader == BlackCherryCosmos)
                                        Color.Black.copy(alpha = 0.15f)
                                    else
                                        Color.Black.copy(alpha = 0.1f)
                                )
                        )

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            BasicText(
                                text = "REWIND",
                                style = typography().m.bold.color(Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            BasicText(
                                text = "$currentYear",
                                style = typography().s.semiBold.color(
                                    if (selectedShader == BlackCherryCosmos)
                                        Color.LightGray
                                    else
                                        Color.White.copy(alpha = 0.9f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
                // ===== END REWIND SECTION =====

                if (showRelatedAlbums)
                    activeRelatedPage?.albums?.takeIf { it.isNotEmpty() }?.let { albums ->
                        BasicText(
                            text = stringResource(R.string.related_albums),
                            style = typography().l.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = albums.distinctBy { it.key },
                                key = { homeQuickAlbumKey(it) }
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) }),
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    }


                if (showRelatedAlbums && activeRelatedPage?.albums.isNullOrEmpty()) {
                    YtmHomeFeedSections(
                        sections = relatedAlbumApiSections,
                        endPaddingValues = endPaddingValues,
                        onPlayableClick = playApiHomeItem,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                if (showSimilarArtists && hasTasteArtistRecommendations) {
                    BasicText(
                        text = stringResource(R.string.artists_for_your_taste),
                        style = typography().l.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = tasteInnertubeArtists.take(18),
                            key = { artist -> "taste_innertube_${artist.key}" },
                        ) { artist ->
                            ArtistItem(
                                artist = artist,
                                thumbnailSizePx = artistThumbnailSizePx,
                                thumbnailSizeDp = artistThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier.clickable { onArtistClick(artist.key) },
                                disableScrollingText = disableScrollingText
                            )
                        }

                        items(
                            items = tasteSessionArtistItems.take(
                                (18 - tasteInnertubeArtists.size).coerceAtLeast(0)
                            ),
                            key = { item -> "taste_session_${item.artistDestinationId()}" },
                        ) { item ->
                            YtmHomeCard(
                                title = item.title,
                                subtitle = item.subtitle.ifBlank { item.artistsText },
                                thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                                imageWidth = 104.dp,
                                imageHeight = 104.dp,
                                rounded = false,
                                modifier = Modifier.clickable {
                                    onArtistClick(item.artistDestinationId())
                                }
                            )
                        }
                    }
                }

                if (showSimilarArtists) {
                    similarArtistShelves.forEach { shelf ->
                        BasicText(
                            text = stringResource(R.string.similar_to_artist, shelf.seedName),
                            style = typography().l.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = shelf.artists,
                                key = { artist -> "similar_${shelf.seedName}_${artist.key}" },
                            ) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizePx = artistThumbnailSizePx,
                                    thumbnailSizeDp = artistThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier.clickable { onArtistClick(artist.key) },
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    }
                }

                if (showPlaylistMightLike && hasTastePlaylistRecommendations) {
                    BasicText(
                        text = stringResource(R.string.playlists_you_might_like),
                        style = typography().l.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        when {
                            uniqueRadioPlaylistItems.isNotEmpty() -> items(
                                items = uniqueRadioPlaylistItems.take(18),
                                key = { item -> homeQuickPlaylistKey(item) },
                            ) { item ->
                                PlaylistItem(
                                    playlist = item,
                                    thumbnailSizePx = playlistThumbnailSizePx,
                                    thumbnailSizeDp = playlistThumbnailSizeDp,
                                    alternative = true,
                                    showSongsCount = false,
                                    isYoutubePlaylist = true,
                                    modifier = Modifier.clickable { onPlaylistClick(item.key) },
                                    disableScrollingText = disableScrollingText
                                )
                            }

                            uniqueSessionPlaylistItems.isNotEmpty() -> items(
                                items = uniqueSessionPlaylistItems.take(18),
                                key = { item -> item.playlistId.ifBlank { item.browseId } }
                            ) { item ->
                                YtmHomeCard(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl.ifBlank { item.thumbnail },
                                    modifier = Modifier.clickable {
                                        item.playlistId.ifBlank { item.browseId }
                                            .takeIf(String::isNotBlank)
                                            ?.let(onPlaylistClick)
                                    }
                                )
                            }

                            else -> items(
                                items = uniqueGuestPlaylistItems.take(18),
                                key = { item -> item.key }
                            ) { item ->
                                PlaylistItem(
                                    playlist = item,
                                    thumbnailSizePx = playlistThumbnailSizePx,
                                    thumbnailSizeDp = playlistThumbnailSizeDp,
                                    alternative = true,
                                    showSongsCount = false,
                                    isYoutubePlaylist = true,
                                    modifier = Modifier.clickable { onPlaylistClick(item.key) },
                                    disableScrollingText = disableScrollingText
                                )
                            }
                        }
                    }
                }
                YtmHomeFeedSections(
                    sections = guestQuickPickSections,
                    endPaddingValues = endPaddingValues,
                    onPlayableClick = playApiHomeItem,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                )



                if (showMoodsAndGenres)
                    discoverPageInit?.let { page ->

                        if (page.moods.isNotEmpty()) {

                            Title(
                                title = stringResource(R.string.moods_and_genres),
                                onClick = { navController.navigate(NavRoutes.moodsPage.name) },
                            )

                            LazyHorizontalGrid(
                                state = moodAngGenresLazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = ScrollableDefaults.flingBehavior(),
                                contentPadding = endPaddingValues,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimensions.itemsVerticalPadding * 4 * 8)
                            ) {
                                items(
                                    items = page.moods.sortedBy { it.title },
                                    key = { it.endpoint.params ?: it.title }
                                ) {
                                    MoodItemColored(
                                        mood = it,
                                        onClick = { it.endpoint.browseId?.let { _ -> onMoodClick(it) } },
                                        modifier = Modifier
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                val monthlyPlaylists by remember {
                    Database.playlistTable
                        .allAsPreview()
                        .distinctUntilChanged()
                        .map { list ->
                            list.filter {
                                it.playlist.name.startsWith(MONTHLY_PREFIX, true)
                            }
                        }
                }.collectAsState(emptyList(), Dispatchers.IO)

                if (showMonthlyPlaylistInQuickPicks)
                    monthlyPlaylists.let { playlists ->
                        if (playlists.isNotEmpty()) {
                            BasicText(
                                text = stringResource(R.string.monthly_playlists),
                                style = typography().l.semiBold,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 24.dp, bottom = 8.dp)
                            )

                            LazyRow(contentPadding = endPaddingValues) {
                                items(
                                    items = playlists.distinctBy { it.playlist.id },
                                    key = { it.playlist.id }
                                ) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizeDp = playlistThumbnailSizeDp,
                                        thumbnailSizePx = playlistThumbnailSizePx,
                                        alternative = true,
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = null
                                            )
                                            .fillMaxSize()
                                            .clickable(onClick = { navController.navigate(route = "${NavRoutes.localPlaylist.name}/${playlist.playlist.id}") }),
                                        disableScrollingText = disableScrollingText,
                                        isYoutubePlaylist = playlist.playlist.isYoutubePlaylist,
                                        isEditable = playlist.playlist.isEditable
                                    )
                                }
                            }
                        }
                    }

                if (showCharts) {

                    chartsPageInit?.let { page ->

                        Title(
                            title = "${stringResource(R.string.charts)} (${selectedCountryCode.countryName})",
                            onClick = {
                                menuState.display {
                                    LazyMenu(items = Countries.entries) { country ->
                                        MenuEntry(
                                            icon = R.drawable.arrow_right,
                                            text = country.countryName,
                                            onClick = {
                                                selectedCountryCode = country
                                                menuState.hide()
                                            }
                                        )
                                    }
                                }
                            },
                        )

                        page.playlists?.let { playlists ->
                            LazyRow(contentPadding = endPaddingValues) {
                                items(
                                    items = playlists.distinctBy { it.key },
                                    key = { homeQuickPlaylistKey(it) },
                                ) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = playlistThumbnailSizePx,
                                        thumbnailSizeDp = playlistThumbnailSizeDp,
                                        alternative = true,
                                        showSongsCount = false,
                                        modifier = Modifier
                                            .clickable(onClick = { onPlaylistClick(playlist.key) }),
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        }

                        page.songs?.let { songs ->
                            if (songs.isNotEmpty()) {
                                BasicText(
                                    text = stringResource(R.string.chart_top_songs),
                                    style = typography().l.semiBold,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 24.dp, bottom = 8.dp)
                                )

                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .height(130.dp)
                                        .fillMaxWidth(),
                                    state = chartsPageSongLazyGridState,
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                ) {
                                    itemsIndexed(
                                        items = if (parentalControlEnabled)
                                            songs.filter {
                                                !it.asSong.title.startsWith(EXPLICIT_PREFIX)
                                            }.distinctBy { it.key }
                                        else songs.distinctBy { it.key },
                                        key = { index, song -> homeQuickSongKey(song, index) }
                                    ) { index, song ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 16.dp)
                                        ) {
                                            BasicText(
                                                text = "${index + 1}",
                                                style = typography().l.bold.center.color(
                                                    colorPalette().text
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            SongItem(
                                                song = song,
                                                onDownloadClick = {},
                                                downloadState = Download.STATE_STOPPED,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                modifier = Modifier
                                                    .clickable(onClick = {
                                                        refreshScope.launch {
                                                            val mediaItems = songs.map { preferredCachedMediaItem(it) }
                                                            val mediaItemIndex = mediaItems.indexOfFirst { it.mediaId == song.key }
                                                            binder?.stopRadio()
                                                            PlaybackContextStore.set(context.getString(R.string.playing_from_quick_picks), context.getString(R.string.top_songs))
                                                            binder?.player?.forcePlayAtIndex(
                                                                mediaItems,
                                                                mediaItemIndex.takeIf { it >= 0 } ?: 0
                                                            )
                                                        }
                                                    })
                                                    .width(itemWidth),
                                                disableScrollingText = disableScrollingText,
                                                isNowPlaying = binder?.player?.isNowPlaying(song.key) ?: false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        page.videos?.let { videos ->
                            if (videos.isNotEmpty()) {
                                BasicText(
                                    text = "Top music videos",
                                    style = typography().l.semiBold,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 24.dp, bottom = 8.dp)
                                )

                                LazyRow(contentPadding = endPaddingValues) {
                                    itemsIndexed(
                                        items = videos.distinctBy { it.key },
                                        key = { index, video -> homeQuickVideoKey(video, index) }
                                    ) { _, video ->
                                        VideoItem(
                                            video = video,
                                            thumbnailHeightDp = playlistThumbnailSizeDp,
                                            thumbnailWidthDp = playlistThumbnailSizeDp * 1.45f,
                                            disableScrollingText = disableScrollingText,
                                            modifier = Modifier.clickable(onClick = {
                                                refreshScope.launch {
                                                    playHomeSectionItems(videos, video)
                                                }
                                            })
                                        )
                                    }
                                }
                            }
                        }

                        page.artists?.let { artists ->
                            if (artists.isNotEmpty()) {
                                BasicText(
                                    text = stringResource(R.string.chart_top_artists),
                                    style = typography().l.semiBold,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 24.dp, bottom = 8.dp)
                                )

                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .height(130.dp)
                                        .fillMaxWidth(),
                                    state = chartsPageArtistLazyGridState,
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                ) {
                                    itemsIndexed(
                                        items = artists.distinctBy { it.key },
                                        key = { index, artist -> homeQuickArtistKey(artist, index) }
                                    ) { index, artist ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 16.dp)
                                        ) {
                                            BasicText(
                                                text = "${index + 1}",
                                                style = typography().l.bold.center.color(
                                                    colorPalette().text
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            ArtistItem(
                                                artist = artist,
                                                thumbnailSizePx = songThumbnailSizePx,
                                                thumbnailSizeDp = songThumbnailSizeDp,
                                                alternative = false,
                                                modifier = Modifier
                                                    .width(200.dp)
                                                    .clickable(onClick = { onArtistClick(artist.key) }),
                                                disableScrollingText = disableScrollingText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (chartsPageInit?.artists.isNullOrEmpty()) {
                    YtmHomeFeedSections(
                        sections = topArtistApiSections,
                        endPaddingValues = endPaddingValues,
                        onPlayableClick = playApiHomeItem,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                if (isYouTubeLoggedIn()) {
                    if (sessionLikedSongsPreview.isNotEmpty()) {
                        val likedSongsPreview = sessionLikedSongsPreview
                            .distinctBy { it.id }
                            .take(14)
                        BasicText(
                            text = stringResource(R.string.ytm_likes_title),
                            style = typography().l.semiBold,
                            modifier = sectionTextModifier,
                        )

                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(2),
                            modifier = Modifier
                                .height(126.dp)
                                .fillMaxWidth(),
                            state = ytmLikesLazyGridState,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            contentPadding = endPaddingValues,
                        ) {
                            itemsIndexed(
                                items = likedSongsPreview,
                                key = { index, song -> "${song.id.ifBlank { "liked_song" }}_$index" }
                            ) { index, song ->
                                app.kreate.android.me.knighthat.component.SongItem(
                                    song = song,
                                    modifier = Modifier
                                        .width(itemInHorizontalGridWidth),
                                    navController = navController,
                                    onClick = {
                                            val queue = likedSongsPreview
                                                .map(Song::asMediaItem)
                                                .distinctBy { it.mediaId }
                                            val startIndex = queue.indexOfFirst { it.mediaId == song.id }
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(
                                                queue,
                                                startIndex.takeIf { it >= 0 } ?: index
                                            )
                                    },
                                    trailingContent = {
                                        HeaderIconButton(
                                            icon = R.drawable.ellipsis_horizontal,
                                            color = colorPalette().textSecondary,
                                            onClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemGridMenu(
                                                        navController = navController,
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        disableScrollingText = disableScrollingText
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (syncedYtmArtists.isNotEmpty()) {
                        BasicText(
                            text = "Artists from your YT Music account",
                            style = typography().l.semiBold,
                            modifier = sectionTextModifier,
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = syncedYtmArtists.take(16),
                                key = { artist -> artist.id }
                            ) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizePx = artistThumbnailSizePx,
                                    thumbnailSizeDp = artistThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier.clickable {
                                        onArtistClick(artist.id)
                                    },
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeArtist = artist.isYoutubeArtist
                                )
                            }
                        }
                    }

                    if (syncedYtmAlbums.isNotEmpty()) {
                        BasicText(
                            text = "Albums from your YT Music account",
                            style = typography().l.semiBold,
                            modifier = sectionTextModifier,
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = syncedYtmAlbums.take(16),
                                key = { album -> album.id }
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier.clickable {
                                        onAlbumClick(album.id)
                                    },
                                    disableScrollingText = disableScrollingText,
                                    isYoutubeAlbum = album.isYoutubeAlbum
                                )
                            }
                        }
                    }
                }

                val sessionSections = remainingApiHomeSections
                YtmHomeFeedSections(
                    sections = sessionSections,
                    endPaddingValues = endPaddingValues,
                    onPlayableClick = playApiHomeItem,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                )
                val apiSectionTitles = liveApiHomeSections
                    .map { section -> section.title.trim().lowercase() }
                    .toSet()
                (homePageResult?.getOrNull() ?: homePageInit)?.let { page ->
                    page.sections
                        .filterNot { section -> section.items.any { item -> item is Innertube.PlaylistItem } }
                        .filterNot { section ->
                            val title = section.title.trim().lowercase()
                            title.isNotBlank() && title in apiSectionTitles
                        }
                        .forEach {
                            if (it.items.isEmpty() || it.items.firstOrNull()?.key == null) return@forEach

                            Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 24.dp, bottom = 8.dp)
                        ) {
                            val sectionLabel = it.label
                            it.thumbnail?.takeIf { thumbnail -> thumbnail.isNotBlank() }?.let { thumbnail ->
                                ImageCacheFactory.AsyncImage(
                                    thumbnailUrl = thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    text = it.title,
                                    style = typography().l.semiBold.color(colorPalette().text),
                                )
                                if (!sectionLabel.isNullOrBlank()) {
                                    BasicText(
                                        text = sectionLabel,
                                        style = typography().xs.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        LazyRow(contentPadding = endPaddingValues) {
                            itemsIndexed(
                                items = it.items,
                                key = { _, item -> ytmHomeItemKey(item) }
                            ) { _, item ->
                                when (item) {
                                    is Innertube.SongItem -> {
                                        YtmHomeCard(
                                            title = item.info?.name.orEmpty(),
                                            subtitle = item.authors?.joinToString(", ") { author -> author.name ?: "" },
                                            thumbnailUrl = item.thumbnail?.url,
                                            modifier = Modifier.clickable(onClick = {
                                                refreshScope.launch {
                                                    playHomeSectionItems(it.items, item)
                                                }
                                            })
                                        )
                                    }

                                    is Innertube.AlbumItem -> {
                                        YtmHomeCard(
                                            title = item.info?.name.orEmpty(),
                                            subtitle = item.authors?.joinToString(", ") { author -> author.name ?: "" },
                                            thumbnailUrl = item.thumbnail?.url,
                                            modifier = Modifier.clickable(onClick = {
                                                navController.navigate("${NavRoutes.album.name}/${item.key}")
                                            })
                                        )
                                    }

                                    is Innertube.ArtistItem -> {
                                        YtmHomeCard(
                                            title = item.info?.name.orEmpty(),
                                            subtitle = item.subscribersCountText,
                                            thumbnailUrl = item.thumbnail?.url,
                                            imageWidth = 104.dp,
                                            imageHeight = 104.dp,
                                            rounded = false,
                                            modifier = Modifier.clickable(onClick = {
                                                navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                            })
                                        )
                                    }

                                    is Innertube.PlaylistItem -> {
                                        YtmHomeCard(
                                            title = item.info?.name.orEmpty(),
                                            subtitle = item.channel?.name ?: item.songCount?.toString(),
                                            thumbnailUrl = item.thumbnail?.url,
                                            modifier = Modifier.clickable(onClick = {
                                                navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                            })
                                        )
                                    }

                                    is Innertube.VideoItem -> {
                                        YtmHomeCard(
                                            title = item.info?.name.orEmpty(),
                                            subtitle = item.authors?.joinToString(", ") { author -> author.name ?: "" },
                                            thumbnailUrl = item.thumbnail?.url,
                                            imageWidth = 136.dp,
                                            imageHeight = 94.dp,
                                            modifier = Modifier.clickable(onClick = {
                                                refreshScope.launch {
                                                    playHomeSectionItems(it.items, item)
                                                }
                                            })
                                        )
                                    }

                                    null -> {}
                                }
                            }
                        }
                    }
                } ?: run {
                    if (!hasRenderableHomeContent && !loadedData) {
                        ShimmerHost {
                        repeat(3) {
                            SongItemPlaceholder()
                        }

                        TextPlaceholder(modifier = sectionTextModifier)

                        Row {
                            repeat(2) {
                                AlbumItemPlaceholder(
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }

                        TextPlaceholder(modifier = sectionTextModifier)

                        Row {
                            repeat(2) {
                                PlaylistItemPlaceholder(
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }
                        }
                    }
                }

                BasicText(
                    text = stringResource(R.string.home_quick_picks_footer),
                    style = typography().xs.secondary.center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )

                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))

                if (!hasRenderableHomeContent && !refreshing) relatedPageResult?.exceptionOrNull()?.let {
                    BasicText(
                        text = stringResource(R.string.page_not_been_loaded),
                        style = typography().s.secondary.center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(all = 16.dp)
                    )
                }

            }

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if (UiType.ViMusic.isCurrent() && showFloatingIcon)
                MultiFloatingActionsContainer(
                    iconId = R.drawable.search,
                    onClick = onSearchClick,
                    onClickSettings = onSettingsClick,
                    onClickSearch = onSearchClick
                )

            if (showFindSheet) {
                CustomModalBottomSheet(
                    showSheet = true,
                    onDismissRequest = { showFindSheet = false },
                    containerColor = Color.Transparent,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    FindScreen(
                        onDismiss = { showFindSheet = false },
                        onOpenSearch = { query ->
                            showFindSheet = false
                            navController.navigate("${NavRoutes.searchResults.name}/${android.net.Uri.encode(query)}")
                        },
                        miniPlayer = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteConfigQuickPicksCard(
    notification: NotificationData,
    hasNewUpdate: Boolean,
    showEmergency: Boolean,
    onOpenUrl: (String) -> Unit,
    onOpenAboutUpdate: () -> Unit,
) {
    var contentExpanded by remember(notification.contents) { mutableStateOf(false) }
    val showArtwork = hasNewUpdate || notification.showImage
    val collapsedContentLines = if (showArtwork) 4 else 6
    val canExpandContent =
        notification.contents.length > 140 ||
            notification.contents.lineSequence().count() > collapsedContentLines

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = notification.url.isNotBlank()) { onOpenUrl(notification.url) }
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        if (showEmergency) Color(0x33FF6B6B) else colorPalette().accent.copy(alpha = 0.16f),
                        colorPalette().background1.copy(alpha = 0.96f),
                        colorPalette().background0.copy(alpha = 0.98f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (hasNewUpdate) {
                Image(
                    painter = painterResource(R.drawable.critical_update_cubic),
                    contentDescription = stringResource(R.string.critical_update_artwork),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (notification.showText) 164.dp else 204.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else if (notification.showImage) {
                notification.image_url?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    ImageCacheFactory.AsyncImage(
                        thumbnailUrl = imageUrl,
                        contentDescription = notification.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (notification.showText) 156.dp else 196.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (notification.showText) {
                BasicText(
                    text = notification.title,
                    style = typography().m.bold.color(
                        if (showEmergency) Color(0xFFFFD5D5) else colorPalette().text
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicText(
                    text = notification.contents,
                    style = typography().s.color(colorPalette().textSecondary),
                    maxLines = if (contentExpanded) Int.MAX_VALUE else collapsedContentLines,
                    overflow = TextOverflow.Ellipsis
                )

                if (canExpandContent) {
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicText(
                        text = stringResource(if (contentExpanded) R.string.read_less else R.string.read_more),
                        style = typography().xs.semiBold.color(colorPalette().accent),
                        modifier = Modifier.clickable { contentExpanded = !contentExpanded }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BasicText(
                text = if (hasNewUpdate) {
                    stringResource(R.string.update_versions, notification.version, BuildConfig.VERSION_NAME)
                } else {
                    stringResource(R.string.installed_version, BuildConfig.VERSION_NAME)
                },
                style = typography().xs.secondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = if (showEmergency) {
                    stringResource(R.string.critical_fixes_ready)
                } else {
                    stringResource(R.string.open_linked_announcement)
                },
                style = typography().xs.semiBold.color(
                    if (showEmergency) Color(0xFFFF8C8C) else colorPalette().accent
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (hasNewUpdate) colorPalette().accent
                            else colorPalette().background2.copy(alpha = 0.7f)
                        )
                        .clickable {
                            if (hasNewUpdate) onOpenAboutUpdate()
                            else if (notification.url.isNotBlank()) onOpenUrl(notification.url)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = stringResource(if (hasNewUpdate) R.string.update_now else R.string.open_notice),
                        style = typography().s.semiBold.color(
                            if (hasNewUpdate) colorPalette().background0 else colorPalette().text
                        ),
                        maxLines = 1
                    )
                }

                if (hasNewUpdate && notification.url.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette().background2.copy(alpha = 0.72f))
                            .clickable { onOpenUrl(notification.url) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = stringResource(R.string.open_link),
                            style = typography().s.semiBold.color(colorPalette().text)
                        )
                    }
                }
            }
        }
    }

}

package app.it.fast4x.rimusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.entities.Song
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.relatedPage
import app.it.fast4x.rimusic.items.*
import app.it.fast4x.rimusic.styling.Dimensions
import app.it.fast4x.rimusic.ui.components.Loader
import app.it.fast4x.rimusic.utils.asSong
import org.jetbrains.compose.resources.stringResource
import rimusic.composeapp.generated.resources.*

// ── Palette (mirrors DesktopApp) ──────────────────────────────────────────────
private object QColor {
    val bg          = Color(0xFF0D1117)
    val surface     = Color(0xFF161B22)
    val glass       = Color(0x661E2D3D)
    val border      = Color(0xFF21303F)
    val accent      = Color(0xFF1DB954)
    val accentSoft  = Color(0xFF14532D)
    val teal        = Color(0xFF0ABFBC)
    val textPrimary = Color(0xFFEEF2FF)
    val textSecond  = Color(0xFF8899AA)
    val textDim     = Color(0xFF445566)
    val chipActive  = Color(0xFF1DB954)
    val chipInact   = Color(0x661E2D3D)
}

private object DesktopQuickPicksCache {
    var relatedPage  : Innertube.RelatedPage?  = null
    var discoverPage : Innertube.DiscoverPage? = null
    fun clear() { relatedPage = null; discoverPage = null }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicsScreen(
    onSongClick     : (Song) -> Unit,
    onAlbumClick    : (String) -> Unit,
    onArtistClick   : (String) -> Unit,
    onPlaylistClick : (String) -> Unit,
    onMoodClick     : (Innertube.Mood.Item) -> Unit
) {
    val quickGridState      = rememberLazyGridState()
    val moodGridState       = rememberLazyGridState()
    val scrollState         = rememberScrollState()

    val related             = remember { mutableStateOf(DesktopQuickPicksCache.relatedPage) }
    var relatedResult       by remember { mutableStateOf<Result<Innertube.RelatedPage?>?>(null) }
    var discoverResult      by remember { mutableStateOf<Result<Innertube.DiscoverPage?>?>(null) }
    val discover            = remember { mutableStateOf(DesktopQuickPicksCache.discoverPage) }
    var refreshNonce        by remember { mutableStateOf(0) }

    var activeMoodChip      by remember { mutableStateOf<String?>(null) }

    val songGridWidth       = Dimensions.itemInHorizontalGridWidth - 20.dp
    val compactAlbum        = Dimensions.albumThumbnailSize - 12.dp
    val compactArtist       = Dimensions.artistThumbnailSize - 10.dp
    val compactPlaylist     = Dimensions.playlistThumbnailSize - 12.dp

    LaunchedEffect(refreshNonce) {
        if (DesktopQuickPicksCache.relatedPage == null) {
            relatedResult = Innertube.relatedPage(NextBody(videoId = "HZnNt9nnEhw"))
            DesktopQuickPicksCache.relatedPage = relatedResult?.getOrNull()
        }
        if (DesktopQuickPicksCache.discoverPage == null) {
            discoverResult = Innertube.discoverPage()
            DesktopQuickPicksCache.discoverPage = discoverResult?.getOrNull()
        }
        related.value  = DesktopQuickPicksCache.relatedPage
        discover.value = DesktopQuickPicksCache.discoverPage
    }
    relatedResult?.getOrNull()?.also  { related.value  = it }
    discoverResult?.getOrNull()?.also { discover.value = it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "Quick picks",
                color      = QColor.textPrimary,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Refresh icon
                IconPill(icon = "↻", label = "Refresh") {
                    DesktopQuickPicksCache.clear()
                    related.value  = null
                    discover.value = null
                    relatedResult  = null
                    discoverResult = null
                    refreshNonce++
                }
                PlayAllPill()
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Song grid (3-col like YT Music) ──────────────────────────────────
        LazyHorizontalGrid(
            state           = quickGridState,
            rows            = GridCells.Fixed(if (related.value != null) 3 else 1),
            flingBehavior   = ScrollableDefaults.flingBehavior(),
            modifier        = Modifier
                .fillMaxWidth()
                .height(
                    if (related.value != null) Dimensions.itemsVerticalPadding * 3 * 9
                    else Dimensions.itemsVerticalPadding * 9
                )
        ) {
            related.value?.let { page ->
                items(
                    items = page.songs?.distinctBy { it.key } ?: emptyList(),
                    key   = Innertube.SongItem::key
                ) { song ->
                    SongRow(
                        song    = song,
                        onClick = { onSongClick(song.asSong) }
                    )
                }
            } ?: item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Loader() }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Mood chips ────────────────────────────────────────────────────────
        discover.value?.moods?.let { moods ->
            val chipLabels = moods.map { it.title }.distinct().take(8)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chipLabels) { label ->
                    MoodChip(
                        label    = label,
                        isActive = activeMoodChip == label,
                        onClick  = {
                            activeMoodChip = if (activeMoodChip == label) null else label
                            moods.firstOrNull { it.title == label }?.let(onMoodClick)
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── New albums ────────────────────────────────────────────────────────
        discover.value?.newReleaseAlbums?.let { albums ->
            SectionHeader("New albums")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(albums.distinctBy { it.key }, key = { it.key }) { album ->
                    AlbumItem(
                        album           = album,
                        thumbnailSizeDp = compactAlbum,
                        alternative     = true,
                        showAuthors     = true,
                        modifier        = Modifier.clickable { onAlbumClick(album.key) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Related albums ────────────────────────────────────────────────────
        related.value?.albums?.let { albums ->
            SectionHeader(stringResource(Res.string.related_albums))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(albums.distinctBy { it.key }, key = { it.key }) {
                    AlbumItem(
                        album           = it,
                        thumbnailSizeDp = compactAlbum,
                        alternative     = true,
                        showAuthors     = true,
                        modifier        = Modifier.clickable { onAlbumClick(it.key) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Similar artists ───────────────────────────────────────────────────
        related.value?.artists?.let { artists ->
            SectionHeader(stringResource(Res.string.similar_artists))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(artists.distinctBy { it.key }, key = { it.key }) {
                    ArtistItem(
                        artist          = it,
                        thumbnailSizeDp = compactArtist,
                        alternative     = true,
                        modifier        = Modifier.clickable { onArtistClick(it.key) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Playlists you might like ──────────────────────────────────────────
        related.value?.playlists?.let { playlists ->
            SectionHeader(stringResource(Res.string.playlists_you_might_like))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(playlists.distinctBy { it.key }, key = { it.key }) {
                    PlaylistItem(
                        playlist        = it,
                        thumbnailSizeDp = compactPlaylist,
                        alternative     = true,
                        showSongsCount  = false,
                        modifier        = Modifier.clickable { onPlaylistClick(it.key) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Moods & Genres grid ───────────────────────────────────────────────
        discover.value?.moods?.let { moods ->
            SectionHeader(stringResource(Res.string.moods_and_genres))
            Spacer(Modifier.height(12.dp))
            LazyHorizontalGrid(
                state         = moodGridState,
                rows          = GridCells.Fixed(4),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                modifier      = Modifier.height(Dimensions.itemsVerticalPadding * 4 * 8)
            ) {
                items(
                    items = moods.sortedBy { it.title },
                    key   = { it.endpoint.params ?: it.title }
                ) {
                    MoodItemColored(
                        mood     = it,
                        onClick  = { onMoodClick(it) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        color      = QColor.textPrimary,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(song: Innertube.SongItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgAlpha by animateFloatAsState(if (isHovered) 1f else 0f, tween(120))

    Row(
        modifier = Modifier
            .width(320.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(QColor.glass.copy(alpha = bgAlpha))
            .hoverable(interactionSource)
            .combinedClickable(onLongClick = {}, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SongItem(
            song            = song,
            isDownloaded    = false,
            onDownloadClick = {},
            modifier        = Modifier.width(320.dp - 32.dp)
        )
    }
}

@Composable
private fun MoodChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isActive) 1.03f else 1f, tween(120))
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(if (isActive) QColor.chipActive else QColor.chipInact)
            .drawBehind {
                if (!isActive) drawCircle(
                    color  = QColor.border,
                    radius = size.minDimension / 2,
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = if (isActive) Color.Black else QColor.textSecond,
            fontSize   = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun IconPill(icon: String, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isHovered) QColor.glass else Color.Transparent)
            .drawBehind {
                drawCircle(
                    color  = QColor.border,
                    radius = size.minDimension / 2,
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                )
            }
            .hoverable(interactionSource)
            .clickable(interactionSource, indication = null, onClick = onClick)
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 16.sp, color = QColor.textSecond)
    }
}

@Composable
private fun PlayAllPill() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(QColor.accent)
            .clickable {}
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("▶", color = Color.Black, fontSize = 13.sp)
            Text(
                text       = "Play all",
                color      = Color.Black,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

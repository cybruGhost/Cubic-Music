package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.PlayerTimelineType
import app.it.fast4x.rimusic.models.Info
import app.it.fast4x.rimusic.ui.components.SeekBarCustom
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.downloadSyncedLyrics
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.utils.PlaybackContextStore
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun FuckSpotifyPlayerSurface(
    mediaItem: MediaItem,
    isCanvasVisible: Boolean = false,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatIconRes: Int,
    isLiked: Boolean,
    isDownloaded: Boolean,
    downloadState: Int,
    downloadProgress: Float,
    crossfadeEnabled: Boolean,
    artistInfos: List<Info>,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
    onLyrics: () -> Unit,
    onLike: () -> Unit,
    onRepeat: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onSleepTimer: () -> Unit,
    onArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = colorPalette()
    val accent = palette.accent
    val backgroundTop = palette.background2
    val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
    val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
    val album = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
    val artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
    val playbackContext by PlaybackContextStore.info.collectAsState()

    LaunchedEffect(mediaItem.mediaId) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching { downloadSyncedLyrics(mediaItem.asSong) }
        }
    }

    val storedLyrics by remember(mediaItem.mediaId) {
        Database.lyricsTable.findBySongId(mediaItem.mediaId).distinctUntilChanged()
    }.collectAsState(initial = null, context = Dispatchers.IO)
    val artists by remember(mediaItem.mediaId) {
        Database.artistTable.findBySongId(mediaItem.mediaId).distinctUntilChanged()
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)
    val previewLines = remember(storedLyrics, positionMs) {
        buildLyricsPreview(
            synced = storedLyrics?.synced,
            fixed = storedLyrics?.fixed,
            positionMs = positionMs,
        )
    }
    val currentLyricLine = previewLines.firstOrNull { line -> line.isCurrent }?.text.orEmpty()
    val displayArtists = remember(artists, artistInfos, artist) {
        val databaseArtists = artists.associateBy { item -> item.id }
        val metadataArtists = artistInfos
            .filter { info -> info.id.isNotBlank() && !info.name.isNullOrBlank() }
            .map { info ->
                SpotifyArtistSummary(
                    id = info.id,
                    name = info.name.orEmpty(),
                    artworkUrl = databaseArtists[info.id]?.thumbnailUrl.orEmpty(),
                )
            }
        metadataArtists.ifEmpty {
            artists
                .filter { item -> item.id.isNotBlank() && !item.name.isNullOrBlank() }
                .map { item ->
                    SpotifyArtistSummary(
                        id = item.id,
                        name = item.name.orEmpty(),
                        artworkUrl = item.thumbnailUrl.orEmpty(),
                    )
                }
        }.ifEmpty {
            artist.takeIf(String::isNotBlank)?.let { name ->
                listOf(SpotifyArtistSummary(id = "", name = name, artworkUrl = ""))
            }.orEmpty()
        }.distinctBy { item -> item.id.ifBlank { item.name } }
    }

    // Flat, muted card tone derived from accent — matches Spotify's solid
    // (not gradient, not glassy) tinted-panel look. Reference: the "Lyrics
    // preview" card in Spotify sits noticeably MORE saturated than the page
    // background behind it, not just a hair off — bumped the blend factor
    // from 0.30 -> 0.42 to match.
    val cardColor = lerp(Color(0xFF15191B), accent, 0.42f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isCanvasVisible) {
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                            Color(0xFF090909),
                        )
                    } else {
                        listOf(
                            backgroundTop,
                            backgroundTop.copy(alpha = 0.88f),
                            Color(0xFF121212),
                            Color(0xFF080808),
                        )
                    }
                )
            )
    ) {
        val contentWidth = maxWidth.coerceAtMost(460.dp)
        val artworkSize = (contentWidth - 40.dp).coerceAtMost(390.dp)
        val canvasArtworkHeight = (maxHeight * 0.40f).coerceIn(220.dp, 350.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpotifyPlayerHeader(
                album = album,
                source = playbackContext.source,
                detail = playbackContext.detail,
                isCanvasVisible = isCanvasVisible,
                onDismiss = onDismiss,
                onMore = onMore,
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            if (isCanvasVisible) {
                Spacer(Modifier.height(canvasArtworkHeight))
            } else AnimatedContent(
                targetState = artworkUrl,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "FuckSpotifyArtwork",
            ) { displayedArtworkUrl ->
                Image(
                    painter = ImageCacheFactory.Painter(displayedArtworkUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(artworkSize)
                        .aspectRatio(1f)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color.Black.copy(alpha = 0.5f),
                            spotColor = Color.Black.copy(alpha = 0.5f),
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth()
                    .height(46.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                AnimatedContent(
                    targetState = currentLyricLine,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "FuckSpotifyCurrentLyric",
                ) { line ->
                    Text(
                        text = line,
                        color = Color.White.copy(alpha = 0.94f),
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = Triple(mediaItem.mediaId, title, artist),
                    label = "FuckSpotifyMetadata",
                    modifier = Modifier.weight(1f),
                ) { (_, displayedTitle, displayedArtist) ->
                    Column {
                        // Long titles used to get chopped with an ugly ellipsis.
                        // Now they scroll on a single line, same as Spotify.
                        Text(
                            text = displayedTitle,
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayedArtist,
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.sp,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (crossfadeEnabled) {
                                Text(
                                    text = stringResource(R.string.crossfade_active_badge),
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    letterSpacing = 0.2.sp,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FuckSpotifyIconButton(
                    icon = if (isLiked) R.drawable.heart else R.drawable.heart_outline,
                    tint = if (isLiked) accent else Color.White,
                    onClick = onLike,
                )
            }

            Spacer(Modifier.height(14.dp))

            FuckSpotifySeekBar(
                mediaId = mediaItem.mediaId,
                positionMs = positionMs,
                durationMs = durationMs,
                activeColor = Color.White,
                onSeek = onSeek,
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatAsDuration(positionMs.coerceAtLeast(0L)),
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = formatAsDuration(durationMs.coerceAtLeast(0L)),
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Transport row — checked directly against a real Spotify screenshot.
            // Spotify's current build ends this row with the SLEEP TIMER icon,
            // not repeat. Repeat has been moved down into the icon row below
            // (replacing the slot Spotify uses for "connect to device", which
            // this app doesn't have) so the feature isn't lost.
            Row(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FuckSpotifyIconButton(
                    icon = R.drawable.shuffle,
                    tint = if (shuffleEnabled) accent else Color.White,
                    showDot = shuffleEnabled,
                    dotColor = accent,
                    onClick = onShuffle,
                )
                FuckSpotifyIconButton(
                    icon = R.drawable.play_skip_back,
                    tint = Color.White,
                    enabled = canSkipPrevious,
                    iconSize = 31,
                    onClick = onPrevious,
                )
                Surface(
                    color = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(72.dp),
                ) {
                    IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
                FuckSpotifyIconButton(
                    icon = R.drawable.play_skip_forward,
                    tint = Color.White,
                    enabled = canSkipNext,
                    iconSize = 31,
                    onClick = onNext,
                )
                FuckSpotifyIconButton(
                    icon = R.drawable.time,
                    tint = Color.White,
                    onClick = onSleepTimer,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FuckSpotifyIconButton(
                    icon = R.drawable.share_social,
                    tint = Color.White,
                    onClick = onShare,
                )
                FuckSpotifyDownloadButton(
                    downloadState = downloadState,
                    progress = downloadProgress,
                    isDownloaded = isDownloaded,
                    accent = accent,
                    onClick = onDownload,
                )
                Image(
                    painter = painterResource(R.drawable.fuck_spotify_logo),
                    contentDescription = stringResource(R.string.player_surface_fuck_spotify),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                )
                FuckSpotifyIconButton(
                    icon = repeatIconRes,
                    tint = Color.White,
                    onClick = onRepeat,
                )
                FuckSpotifyIconButton(
                    icon = R.drawable.playlist,
                    tint = Color.White,
                    onClick = onQueue,
                )
            }

            Spacer(Modifier.height(28.dp))

            SpotifyLyricsPreviewCard(
                lines = previewLines,
                cardColor = cardColor,
                onLyrics = onLyrics,
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxWidth(),
            )

            if (displayArtists.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .widthIn(max = contentWidth)
                        .fillMaxWidth(),
                ) {
                    items(
                        items = displayArtists,
                        key = { item -> item.id.ifBlank { item.name } },
                    ) { item ->
                        SpotifyArtistCard(
                            artistId = item.id,
                            artistName = item.name,
                            artworkUrl = item.artworkUrl,
                            onArtist = onArtist,
                            modifier = Modifier.width((contentWidth - 28.dp).coerceAtLeast(260.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlayerHeader(
    album: String,
    source: String,
    detail: String,
    isCanvasVisible: Boolean,
    onDismiss: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceLabel = source.ifBlank { stringResource(R.string.now_playing) }
    val detailLabel = detail.ifBlank { album }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FuckSpotifyIconButton(
            icon = R.drawable.chevron_down,
            tint = Color.White,
            onClick = onDismiss,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = sourceLabel.uppercase(),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                letterSpacing = 1.2.sp,
            )
            if (detailLabel.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detailLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                )
            }
        }
        if (isCanvasVisible) {
            Text(
                text = "CANVAS",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        } else {
            FuckSpotifyIconButton(
                icon = R.drawable.ellipsis_vertical,
                tint = Color.White,
                onClick = onMore,
            )
        }
    }
}

@Composable
private fun SpotifyLyricsPreviewCard(
    lines: List<SpotifyPreviewLine>,
    cardColor: Color,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(cardColor)
            .clickable(onClick = onLyrics)
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.fuck_spotify_lyrics_preview),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Icon(
                painter = painterResource(R.drawable.chevron_down),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = lines,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FuckSpotifyLyricsPreview",
        ) { displayedLines ->
            Column(
                modifier = Modifier.heightIn(min = 112.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (displayedLines.isEmpty()) {
                    Text(
                        text = stringResource(R.string.fuck_spotify_no_lyrics),
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                    )
                } else {
                    displayedLines.forEach { line ->
                        Text(
                            text = line.text,
                            color = Color.White.copy(alpha = if (line.isCurrent) 1f else 0.58f),
                            fontSize = if (line.isCurrent) 23.sp else 20.sp,
                            lineHeight = if (line.isCurrent) 29.sp else 26.sp,
                            fontWeight = if (line.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(
            color = Color.White,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier.clickable(onClick = onLyrics),
        ) {
            Text(
                text = stringResource(R.string.fuck_spotify_show_lyrics),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun FuckSpotifyDownloadButton(
    downloadState: Int,
    progress: Float,
    isDownloaded: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val isActive = downloadState == Download.STATE_DOWNLOADING ||
        downloadState == Download.STATE_QUEUED ||
        downloadState == Download.STATE_RESTARTING
    val normalizedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(46.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive && normalizedProgress > 0.01f) {
            Canvas(modifier = Modifier.size(34.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * normalizedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        } else if (isActive) {
            CircularProgressIndicator(
                color = accent,
                trackColor = Color.White.copy(alpha = 0.22f),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(34.dp),
            )
        }
        IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
            Icon(
                painter = painterResource(
                    when {
                        isDownloaded -> R.drawable.downloaded
                        isActive -> R.drawable.download_progress
                        else -> R.drawable.download
                    }
                ),
                contentDescription = stringResource(
                    if (isDownloaded) R.string.downloaded else R.string.download
                ),
                tint = if (isDownloaded || isActive) accent else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SpotifyArtistCard(
    artistId: String,
    artistName: String,
    artworkUrl: String,
    onArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val bodyColor = Color(0xFF1C1C1C)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bodyColor)
            .clickable(enabled = artistId.isNotBlank()) { onArtist(artistId) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(Color(0xFF2A2A2A)),
        ) {
            if (artworkUrl.isNotBlank()) {
                Image(
                    painter = ImageCacheFactory.Painter(artworkUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = artistName.firstOrNull()?.uppercase().orEmpty(),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            // Light scrim only at the very top so the label stays legible
            // regardless of what the artwork looks like — not a full vignette.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                            ),
                        )
                    )
            )
            Text(
                text = stringResource(R.string.fuck_spotify_about_artist),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = artistName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
            )
            if (artistId.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = Color.Transparent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier.clickable { onArtist(artistId) },
                ) {
                    Text(
                        text = stringResource(R.string.fuck_spotify_view_artist),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    )
                }
            }
        }
    }
}

private data class SpotifyArtistSummary(
    val id: String,
    val name: String,
    val artworkUrl: String,
)

@Composable
private fun FuckSpotifySeekBar(
    mediaId: String,
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDuration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val liveValue = positionMs.coerceIn(0L, validDuration.coerceAtLeast(0L))
    var isDragging by remember(mediaId) { mutableStateOf(false) }
    var dragValue by remember(mediaId) { mutableStateOf(liveValue) }
    val displayedValue = if (isDragging) dragValue else liveValue

    LaunchedEffect(liveValue, isDragging) {
        if (!isDragging) dragValue = liveValue
    }

    SeekBarCustom(
        type = PlayerTimelineType.ThinBar,
        value = displayedValue,
        minimumValue = 0L,
        maximumValue = validDuration,
        onDragStart = { target ->
            isDragging = true
            dragValue = target.coerceIn(0L, validDuration)
        },
        onDrag = { delta ->
            dragValue = (dragValue + delta).coerceIn(0L, validDuration)
        },
        onDragEnd = {
            if (isDragging && validDuration > 0L) onSeek(dragValue)
            isDragging = false
        },
        color = activeColor,
            // Reference Spotify screenshot shows a noticeably lighter, more
            // visible inactive track than a faint 24% wash — bumped to 34%.
        backgroundColor = Color.White.copy(alpha = 0.34f),
        scrubberColor = activeColor,
        scrubberRadius = 5.dp,
        modifier = modifier.height(28.dp),
    )
}

@Composable
private fun FuckSpotifyIconButton(
    icon: Int,
    tint: Color,
    enabled: Boolean = true,
    iconSize: Int = 24,
    showDot: Boolean = false,
    dotColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(46.dp)
                .alpha(if (enabled) 1f else 0.32f),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize.dp),
            )
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

private data class SpotifyPreviewLine(
    val text: String,
    val isCurrent: Boolean,
)

private fun buildLyricsPreview(
    synced: String?,
    fixed: String?,
    positionMs: Long,
): List<SpotifyPreviewLine> {
    val timedLines = synced
        ?.takeIf(String::isNotBlank)
        ?.let { value -> runCatching { LrcLib.Lyrics(value).sentences }.getOrNull() }
        .orEmpty()
        .mapNotNull { (timestamp, value) ->
            cleanSpotifyLyricLine(value).takeIf(String::isNotBlank)?.let { timestamp to it }
        }

    if (timedLines.isNotEmpty()) {
        val currentIndex = timedLines
            .indexOfLast { (timestamp, _) -> timestamp <= positionMs.coerceAtLeast(0L) }
            .coerceAtLeast(0)
        val firstIndex = (currentIndex - 1).coerceAtLeast(0)
        val lastIndex = (currentIndex + 1).coerceAtMost(timedLines.lastIndex)
        return (firstIndex..lastIndex).map { index ->
            SpotifyPreviewLine(
                text = timedLines[index].second,
                isCurrent = index == currentIndex,
            )
        }
    }

    return fixed
        .orEmpty()
        .lineSequence()
        .map(::cleanSpotifyLyricLine)
        .filter(String::isNotBlank)
        .take(3)
        .mapIndexed { index, line -> SpotifyPreviewLine(line, index == 0) }
        .toList()
}

private fun cleanSpotifyLyricLine(value: String): String {
    val cleaned = buildString(value.length) {
        var insideTag = false
        value.forEach { character ->
            when (character) {
                '{' -> insideTag = true
                '}' -> insideTag = false
                else -> if (!insideTag) append(character)
            }
        }
    }.trim()
    return if (cleaned.startsWith('<') && cleaned.endsWith('>')) "" else cleaned
}
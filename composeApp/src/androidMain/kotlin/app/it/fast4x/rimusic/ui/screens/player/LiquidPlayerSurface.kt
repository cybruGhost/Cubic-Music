package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.downloadSyncedLyrics
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.utils.liquidPlayerLayoutStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Purely a presentation choice — swapped by the small circular toggle in
 * the top-right corner. No playback / callback behaviour differs between
 * the two; both styles drive the exact same parameters.
 *
 *  Arc  -> soft rounded-square cover with an open arc seek control beneath it.
 *  Ring -> perfectly circular cover wrapped in a near-full radial seek ring.
 */
internal enum class PlayerLayoutStyle {
    Arc,
    Ring,
}

@Composable
internal fun LiquidPlayerSurface(
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
    repeatEnabled: Boolean,
    isLiked: Boolean,
    crossfadeEnabled: Boolean,
    layoutStyle: PlayerLayoutStyle = PlayerLayoutStyle.Arc,
    playbackErrorMessage: String? = null,
    errorActionLabel: String? = null,
    onErrorAction: (() -> Unit)? = null,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
    onAppearance: () -> Unit,
    onSleepTimer: () -> Unit,
    onLyrics: () -> Unit,
    onLike: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // This surface owns its single header; the host suppresses its generic header.
    val palette = colorPalette()
    val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
    val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
    val album = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
    val artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()

    val foreground = if (isCanvasVisible) Color.White else palette.text
    val muted = foreground.copy(alpha = 0.6f)
    val controlSurface = if (isCanvasVisible) Color.Black.copy(alpha = 0.58f) else palette.background2.copy(alpha = 0.86f)

    // Same lyrics pipeline as the rest of the app (FuckSpotifyPlayerSurface):
    // trigger a synced-lyrics fetch/cache on song change, then read whatever
    // is in the local DB reactively. Nothing here is guessed or mocked.
    LaunchedEffect(mediaItem.mediaId) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching { downloadSyncedLyrics(mediaItem.asSong) }
        }
    }

    val storedLyrics by remember(mediaItem.mediaId) {
        Database.lyricsTable.findBySongId(mediaItem.mediaId).distinctUntilChanged()
    }.collectAsState(initial = null, context = Dispatchers.IO)

    val lyricsPreview = remember(storedLyrics, positionMs) {
        buildLiquidLyricsPreview(
            synced = storedLyrics?.synced,
            fixed = storedLyrics?.fixed,
            positionMs = positionMs,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isCanvasVisible) {
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
    ) {
        val horizontalPadding = (maxWidth * 0.055f).coerceIn(16.dp, 28.dp)

        // Single fixed page — NEVER scrolls. The artwork owns the flexible
        // vertical slot, while the controls + compact lyrics dock consume
        // only what they actually need. The host player already owns the
        // fullscreen/system chrome, so we avoid a second navigation inset
        // here (that was the visible dead band at the bottom).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RingPlayerHeader(
                nowPlayingFrom = album,
                foreground = foreground,
                onDismiss = onDismiss,
                onAppearance = onAppearance,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val artWidth = artworkSizeFor(
                        style = layoutStyle,
                        availableWidth = maxWidth,
                        availableHeight = maxHeight,
                    )
                    val artHeight = (artWidth * 1.28f)
                    val artworkAreaHeight = artHeight + 30.dp

                    if (playbackErrorMessage != null && layoutStyle == PlayerLayoutStyle.Ring) {
                        PlayerSurfacePlaybackError(
                            message = playbackErrorMessage,
                            actionLabel = errorActionLabel,
                            onAction = onErrorAction,
                            foreground = foreground,
                            accent = palette.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding),
                        )
                    } else {
                        AnimatedContent(
                            targetState = layoutStyle to artworkUrl,
                            transitionSpec = {
                                (fadeIn(tween(240)) togetherWith fadeOut(tween(170)))
                            },
                            label = "liquid_artwork",
                        ) { (style, displayedArtworkUrl) ->
                            val displayedPainter = ImageCacheFactory.Painter(displayedArtworkUrl)
                            when (style) {
                                PlayerLayoutStyle.Arc -> ArcStyleArtwork(
                                    mediaId = mediaItem.mediaId,
                                    artworkPainter = displayedPainter,
                                    artWidth = artWidth,
                                    artHeight = artHeight,
                                    artworkAreaHeight = artworkAreaHeight,
                                    positionMs = positionMs,
                                    durationMs = durationMs,
                                    onSeek = onSeek,
                                )
                                PlayerLayoutStyle.Ring -> RingStyleArtwork(
                                    mediaId = mediaItem.mediaId,
                                    artworkPainter = displayedPainter,
                                    artWidth = artWidth,
                                    accent = palette.accent,
                                    trackColor = foreground.copy(alpha = 0.16f),
                                    positionMs = positionMs,
                                    durationMs = durationMs,
                                    onSeek = onSeek,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            TrackInfoRow(
                mediaId = mediaItem.mediaId,
                title = title,
                artist = artist,
                isLiked = isLiked,
                foreground = foreground,
                muted = muted,
                accent = palette.accent,
                onQueue = onQueue,
                onLike = onLike,
            )

            RingLinearSeekBar(
                mediaId = mediaItem.mediaId,
                positionMs = positionMs,
                durationMs = durationMs,
                activeColor = palette.accent,
                trackColor = foreground.copy(alpha = 0.18f),
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth(0.86f),
            )
            PlaybackTimeRow(
                positionMs = positionMs,
                durationMs = durationMs,
                timeColor = foreground,
            )

            Spacer(Modifier.height(6.dp))

            PrimaryControlsRow(
                foreground = foreground,
                accent = palette.accent,
                controlSurface = controlSurface,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                shuffleEnabled = shuffleEnabled,
                repeatIconRes = repeatIconRes,
                repeatEnabled = repeatEnabled,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
            )

            Spacer(Modifier.height(4.dp))

            RingBottomActions(
                foreground = foreground,
                accent = palette.accent,
                crossfadeEnabled = crossfadeEnabled,
                onSleepTimer = onSleepTimer,
                onMore = onMore,
            )

            // Compact live lyrics dock. No fake expand state: tapping it opens
            // the real lyrics action supplied by the host, just like the
            // FuckSpotify player.
            LyricsSheet(
                lines = lyricsPreview,
                accent = palette.accent,
                onLyrics = onLyrics,
            )
        }
    }
}

/**
 * Computes an artwork side length that fits BOTH the available width and
 * the available height for the given style's bounding box, so the artwork
 * (plus its seek control) never overflows and the page never needs to
 * scroll. The old 260dp cap made the cover feel timid; this version allows
 * it to breathe up to 292dp while still respecting the available height.
 */
private fun artworkSizeFor(
    style: PlayerLayoutStyle,
    availableWidth: androidx.compose.ui.unit.Dp,
    availableHeight: androidx.compose.ui.unit.Dp,
): androidx.compose.ui.unit.Dp {
    val widthLimit = when (style) {
        PlayerLayoutStyle.Arc -> availableWidth - 44.dp
        PlayerLayoutStyle.Ring -> availableWidth - 40.dp
    }
    val heightLimit = when (style) {
        PlayerLayoutStyle.Arc -> (availableHeight - 30.dp) / 1.28f
        PlayerLayoutStyle.Ring -> availableHeight - 40.dp
    }
    val proportionalLimit = min(availableWidth.value, availableHeight.value).dp * 0.94f
    return minOf(widthLimit, heightLimit, proportionalLimit, 360.dp)
        .coerceAtLeast(1.dp)
}

/* ---------------------------------------------------------------------- */
/*  Artwork headers — visual only, both drive onSeek/position/duration     */
/* ---------------------------------------------------------------------- */

@Composable
private fun ArcStyleArtwork(
    mediaId: String,
    artworkPainter: androidx.compose.ui.graphics.painter.Painter,
    artWidth: androidx.compose.ui.unit.Dp,
    artHeight: androidx.compose.ui.unit.Dp,
    artworkAreaHeight: androidx.compose.ui.unit.Dp,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val palette = colorPalette()
    Box(
        modifier = Modifier
            .width(artWidth + 44.dp)
            .height(artworkAreaHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        LiquidArcSeekBar(
            mediaId = mediaId,
            positionMs = positionMs,
            durationMs = durationMs,
            activeColor = palette.accent,
            trackColor = palette.textDisabled.copy(alpha = 0.4f),
            scrubberColor = palette.accent,
            onSeek = onSeek,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .width(artWidth)
                .height(artHeight)
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = artWidth / 2,
                        bottomEnd = artWidth / 2,
                    ),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = artWidth / 2,
                        bottomEnd = artWidth / 2,
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = artWidth / 2,
                        bottomEnd = artWidth / 2,
                    )
                )
                .background(palette.background2)
        ) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RingStyleArtwork(
    mediaId: String,
    artworkPainter: androidx.compose.ui.graphics.painter.Painter,
    artWidth: androidx.compose.ui.unit.Dp,
    accent: Color,
    trackColor: Color,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val palette = colorPalette()
    // Ring rides almost flush against the artwork edge — matches the
    // reference screenshot, no loose floating gap.
    val ringBoxSize = artWidth + 10.dp

    Box(
        modifier = Modifier.size(ringBoxSize),
        contentAlignment = Alignment.Center
    ) {
        ClassicRingSeekBar(
            mediaId = mediaId,
            positionMs = positionMs,
            durationMs = durationMs,
            activeColor = accent,
            trackColor = trackColor,
            scrubberColor = accent,
            onSeek = onSeek,
            modifier = Modifier.size(ringBoxSize)
        )

        Box(
            modifier = Modifier
                .size(artWidth)
                .clip(CircleShape)
                .background(palette.background2)
        ) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  Track info row (crossfade pill lives in the top bar now, not here)    */
/* ---------------------------------------------------------------------- */

@Composable
private fun TrackInfoRow(
    mediaId: String,
    title: String,
    artist: String,
    isLiked: Boolean,
    foreground: Color,
    muted: Color,
    accent: Color,
    onQueue: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidControlButton(
            icon = R.drawable.playlist,
            tint = foreground,
            onClick = onQueue,
        )

        AnimatedContent(
            targetState = Triple(mediaId, title, artist),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "liquid_metadata",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) { (_, displayedTitle, displayedArtist) ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = displayedTitle,
                    color = foreground,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 3f,
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                )
                Text(
                    text = displayedArtist,
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 3f,
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                )
            }
        }

        LiquidControlButton(
            icon = if (isLiked) R.drawable.heart else R.drawable.heart_outline,
            tint = if (isLiked) accent else foreground,
            onClick = onLike,
        )
    }
}

/**
 * Small, legible pill instead of relying on a tiny 9sp inline string —
 * that was rendering as an illegible cluster of glyphs at that scale. A
 * self-drawn crossfade glyph (two overlapping arcs, no extra drawable
 * resource needed) plus the real string resource, both large enough to
 * actually read. Now lives in the top bar instead of under the artist
 * name, so it never pushes the title/artist column off-center.
 */
@Composable
private fun CrossfadeBadge(accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.16f),
        contentColor = accent,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                val stroke = 1.4.dp.toPx()
                drawArc(
                    color = accent,
                    startAngle = 200f,
                    sweepAngle = 220f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = 20f,
                    sweepAngle = 220f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.crossfade_active_badge),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  Playback time — radial seeker is the single source of timeline input   */
/* ---------------------------------------------------------------------- */

@Composable
private fun PlaybackTimeRow(
    positionMs: Long,
    durationMs: Long,
    timeColor: Color,
) {
    val validDuration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    Row(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .height(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatAsDuration(positionMs.coerceIn(0L, validDuration.coerceAtLeast(0L))),
            color = timeColor,
            fontSize = 11.sp,
        )
        Text(
            text = formatAsDuration(validDuration),
            color = timeColor,
            fontSize = 11.sp,
        )
    }
}

/* ---------------------------------------------------------------------- */
/*  Primary controls                                                      */
/* ---------------------------------------------------------------------- */

@Composable
private fun PrimaryControlsRow(
    foreground: Color,
    accent: Color,
    controlSurface: Color,
    isPlaying: Boolean,
    isBuffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatIconRes: Int,
    repeatEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    val palette = colorPalette()
    Row(
        modifier = Modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidControlButton(
            icon = repeatIconRes,
            tint = if (repeatEnabled) accent else foreground,
            onClick = onRepeat,
        )
        LiquidControlButton(
            icon = R.drawable.play_skip_back,
            tint = foreground,
            enabled = canSkipPrevious,
            onClick = onPrevious,
        )
        Surface(
            color = accent,
            contentColor = palette.background0,
            shape = CircleShape,
            modifier = Modifier
                .size(68.dp)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .border(1.dp, controlSurface.copy(alpha = 0.42f), CircleShape),
        ) {
            IconButton(onClick = onPlayPause) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = palette.background0,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = palette.background0,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
        LiquidControlButton(
            icon = R.drawable.play_skip_forward,
            tint = foreground,
            enabled = canSkipNext,
            onClick = onNext,
        )
        LiquidControlButton(
            icon = R.drawable.shuffle,
            tint = if (shuffleEnabled) accent else foreground,
            onClick = onShuffle,
        )
    }
}

/* ---------------------------------------------------------------------- */
/*  Compact live lyrics dock — animated current-line preview               */
/* ---------------------------------------------------------------------- */

@Composable
private fun LyricsSheet(
    lines: List<LiquidLyricsLine>,
    accent: Color,
    onLyrics: () -> Unit,
) {
    val compactLines = remember(lines) {
        lines.take(3)
    }
    val activeKey = compactLines.firstOrNull { it.isCurrent }?.text.orEmpty()

    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(onLyrics) { detectTapGestures { onLyrics() } },
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.fuck_spotify_lyrics_preview),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.fuck_spotify_show_lyrics),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(6.dp))

            if (compactLines.isEmpty()) {
                Text(
                    text = stringResource(R.string.fuck_spotify_no_lyrics),
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                AnimatedContent(
                    targetState = activeKey to compactLines,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 }) togetherWith
                            (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 5 })
                    },
                    label = "liquid_lyrics_preview",
                ) { (_, displayedLines) ->
                    Column {
                        displayedLines.forEach { line ->
                            LyricsLineText(line = line, accent = accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsLineText(line: LiquidLyricsLine, accent: Color) {
    val color by animateColorAsState(
        targetValue = if (line.isCurrent) accent else Color.White.copy(alpha = 0.5f),
        label = "lyrics_line_color",
    )
    val fontSize by animateFloatAsState(
        targetValue = if (line.isCurrent) 16f else 13f,
        label = "lyrics_line_size",
    )
    Text(
        text = line.text,
        color = color,
        fontSize = fontSize.sp,
        lineHeight = if (line.isCurrent) 20.sp else 17.sp,
        fontWeight = if (line.isCurrent) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Start,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    )
}

@Composable
private fun LiquidControlButton(
    icon: Int,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.34f)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/* ---------------------------------------------------------------------- */
/*  Layout style toggle — plain circle, per the reference.                */
/* ---------------------------------------------------------------------- */

@Composable
private fun LayoutStyleToggle(
    tint: Color,
    background: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = background,
        contentColor = tint,
        shape = CircleShape,
        modifier = modifier
            .size(34.dp)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .border(1.dp, tint.copy(alpha = 0.18f), CircleShape)
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val strokeWidth = 1.6.dp.toPx()
                drawCircle(
                    color = tint,
                    radius = size.minDimension / 2.2f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = tint,
                    radius = size.minDimension / 5.5f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  Radial seek bars                                                      */
/* ---------------------------------------------------------------------- */

@Composable
private fun LiquidArcSeekBar(
    mediaId: String,
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    trackColor: Color,
    scrubberColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDuration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val playbackProgress = if (validDuration > 0L) {
        (positionMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var isDragging by remember(mediaId) { mutableStateOf(false) }
    var draggingProgress by remember(mediaId) { mutableFloatStateOf(playbackProgress) }
    var dragAccepted by remember(mediaId) { mutableStateOf(false) }
    val visibleProgress = if (isDragging && dragAccepted) draggingProgress else playbackProgress

    fun seekFraction(
        offset: Offset,
        size: androidx.compose.ui.unit.IntSize,
        hitSlopPx: Float,
    ): Float? {
        if (validDuration <= 0L) return null
        val center = Offset(size.width / 2f, size.height * 0.58f)
        val radius = size.width * 0.48f
        val distance = (offset - center).getDistance()
        if (abs(distance - radius) > hitSlopPx) return null
        return liquidArcFraction(offset, center)
    }

    Canvas(
        modifier = modifier
            .pointerInput(validDuration) {
                detectTapGestures { offset ->
                    seekFraction(offset, size, 72.dp.toPx())
                        ?.let { onSeek((it * validDuration).toLong()) }
                }
            }
            .pointerInput(validDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = seekFraction(offset, size, 72.dp.toPx())
                        dragAccepted = fraction != null
                        if (fraction != null) draggingProgress = fraction
                    },
                    onDrag = { change, _ ->
                        if (dragAccepted) {
                            val center = Offset(size.width / 2f, size.height * 0.58f)
                            draggingProgress = liquidArcFraction(change.position, center)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (dragAccepted) onSeek((draggingProgress * validDuration).toLong())
                        isDragging = false
                        dragAccepted = false
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccepted = false
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height * 0.58f)
        val radius = size.width * 0.48f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val startAngle = 160f
        val sweepAngle = -140f
        val activeSweep = sweepAngle * visibleProgress

        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        if (visibleProgress > 0.001f) {
            drawArc(
                color = activeColor,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val angle = Math.toRadians((startAngle + activeSweep).toDouble())
        val scrubber = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        )
        val thumbRadius = if (isDragging && dragAccepted) 15.dp.toPx() else 13.dp.toPx()
        drawCircle(Color.White, radius = thumbRadius, center = scrubber)
        drawCircle(
            color = scrubberColor,
            radius = thumbRadius,
            center = scrubber,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Near-full ring seek bar wrapping the circular cover: the track wraps
 * almost the whole way around, leaving a small gap at the top. Same
 * drag/tap seek behaviour as [LiquidArcSeekBar]. Track and progress
 * strokes share the same thickness so they read as one continuous ring.
 */
@Composable
private fun ClassicRingSeekBar(
    mediaId: String,
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    trackColor: Color,
    scrubberColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDuration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val playbackProgress = if (validDuration > 0L) {
        (positionMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var isDragging by remember(mediaId) { mutableStateOf(false) }
    var draggingProgress by remember(mediaId) { mutableFloatStateOf(playbackProgress) }
    var dragAccepted by remember(mediaId) { mutableStateOf(false) }
    val visibleProgress = if (isDragging && dragAccepted) draggingProgress else playbackProgress

    val startAngle = -90f
    val sweepAngle = 360f

    fun seekFraction(
        offset: Offset,
        size: androidx.compose.ui.unit.IntSize,
        hitSlopPx: Float,
    ): Float? {
        if (validDuration <= 0L) return null
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width * 0.46f
        val distance = (offset - center).getDistance()
        if (abs(distance - radius) > hitSlopPx) return null
        return ringFraction(offset, center, startAngle, sweepAngle)
    }

    Canvas(
        modifier = modifier
            .pointerInput(validDuration) {
                detectTapGestures { offset ->
                    seekFraction(offset, size, 72.dp.toPx())
                        ?.let { onSeek((it * validDuration).toLong()) }
                }
            }
            .pointerInput(validDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = seekFraction(offset, size, 72.dp.toPx())
                        dragAccepted = fraction != null
                        if (fraction != null) draggingProgress = fraction
                    },
                    onDrag = { change, _ ->
                        if (dragAccepted) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            draggingProgress = ringFraction(change.position, center, startAngle, sweepAngle)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (dragAccepted) onSeek((draggingProgress * validDuration).toLong())
                        isDragging = false
                        dragAccepted = false
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccepted = false
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringStrokeWidth = 7.dp.toPx()
        val radius = (size.minDimension - ringStrokeWidth) / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val activeSweep = sweepAngle * visibleProgress

        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
        )
        if (visibleProgress > 0.001f) {
            drawArc(
                color = activeColor,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
            )
        }

        val angle = Math.toRadians((startAngle + activeSweep).toDouble())
        val scrubber = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        )
        drawCircle(Color.White, radius = ringStrokeWidth * 0.9f, center = scrubber)
        drawCircle(scrubberColor, radius = ringStrokeWidth * 0.6f, center = scrubber)
    }
}

private fun liquidArcFraction(offset: Offset, center: Offset): Float {
    var degrees = Math.toDegrees(
        atan2(
            (offset.y - center.y).toDouble(),
            (offset.x - center.x).toDouble()
        )
    ).toFloat()
    if (degrees < 0f) degrees += 360f
    val relative = (160f - degrees + 360f) % 360f
    return when {
        relative <= 140f -> (relative / 140f).coerceIn(0f, 1f)
        relative < 250f -> 1f
        else -> 0f
    }
}

private fun ringFraction(offset: Offset, center: Offset, startAngle: Float, sweepAngle: Float): Float {
    var degrees = Math.toDegrees(
        atan2(
            (offset.y - center.y).toDouble(),
            (offset.x - center.x).toDouble()
        )
    ).toFloat()
    if (degrees < 0f) degrees += 360f
    var normalizedStart = startAngle
    if (normalizedStart < 0f) normalizedStart += 360f
    val relative = (degrees - normalizedStart + 360f) % 360f
    return (relative / sweepAngle).coerceIn(0f, 1f)
}

/* ---------------------------------------------------------------------- */
/*  Lyrics data — identical pipeline to FuckSpotifyPlayerSurface: pull the */
/*  cached synced/fixed lyrics record for this song out of the local DB   */
/*  and derive a short "currently near this timestamp" preview from it.   */
/*  Nothing here is invented; it reads the same LrcLib-backed record every */
/*  other player surface in the app uses.                                 */
/* ---------------------------------------------------------------------- */

private data class LiquidLyricsLine(
    val text: String,
    val isCurrent: Boolean,
)

private fun buildLiquidLyricsPreview(
    synced: String?,
    fixed: String?,
    positionMs: Long,
): List<LiquidLyricsLine> {
    val timedLines = synced
        ?.takeIf(String::isNotBlank)
        ?.let { value -> runCatching { LrcLib.Lyrics(value).sentences }.getOrNull() }
        .orEmpty()
        .mapNotNull { (timestamp, value) ->
            cleanLiquidLyricLine(value).takeIf(String::isNotBlank)?.let { timestamp to it }
        }

    if (timedLines.isNotEmpty()) {
        val currentIndex = timedLines
            .indexOfLast { (timestamp, _) -> timestamp <= positionMs.coerceAtLeast(0L) }
            .coerceAtLeast(0)
        val firstIndex = (currentIndex - 1).coerceAtLeast(0)
        val lastIndex = (currentIndex + 1).coerceAtMost(timedLines.lastIndex)
        return (firstIndex..lastIndex).map { index ->
            LiquidLyricsLine(
                text = timedLines[index].second,
                isCurrent = index == currentIndex,
            )
        }
    }

    return fixed
        .orEmpty()
        .lineSequence()
        .map(::cleanLiquidLyricLine)
        .filter(String::isNotBlank)
        .take(3)
        .mapIndexed { index, line -> LiquidLyricsLine(line, index == 0) }
        .toList()
}

private fun cleanLiquidLyricLine(value: String): String {
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
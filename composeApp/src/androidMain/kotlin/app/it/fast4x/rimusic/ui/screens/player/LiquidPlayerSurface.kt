package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.utils.liquidPlayerLayoutStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Purely a presentation choice — swapped by the user via the small toggle
 * in the corner of the surface. No playback / callback behaviour differs
 * between the two; both styles drive the exact same parameters.
 */
private enum class PlayerLayoutStyle {
    Arc,   // open arc around a soft, rounded-square cover
    Ring,  // near-full ring around a circular cover (2nd reference)
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
    isLiked: Boolean,
    crossfadeEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onLike: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = colorPalette()
    val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
    val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
    val artworkPainter = ImageCacheFactory.Painter(
        thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
    )

    val foreground = if (isCanvasVisible) Color.White else palette.text
    val controlSurface = if (isCanvasVisible) Color.Black.copy(alpha = 0.58f) else palette.background2.copy(alpha = 0.86f)

    var layoutStyle by rememberPreference(
        key = liquidPlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.Arc,
    )

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
        val artWidth = (maxWidth * 0.76f).coerceAtMost(310.dp)
        val artHeight = (artWidth * 1.30f).coerceAtMost(maxHeight * 0.67f)
        val artworkAreaHeight = artHeight + 34.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LayoutStyleToggle(
                    style = layoutStyle,
                    tint = foreground,
                    background = controlSurface,
                    onToggle = {
                        layoutStyle = if (layoutStyle == PlayerLayoutStyle.Arc) {
                            PlayerLayoutStyle.Ring
                        } else {
                            PlayerLayoutStyle.Arc
                        }
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = layoutStyle,
                    transitionSpec = {
                        (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                    },
                    label = "player_layout_style",
                ) { style ->
                    when (style) {
                        PlayerLayoutStyle.Arc -> ArcStyleArtwork(
                            title = title,
                            artist = artist,
                            crossfadeEnabled = crossfadeEnabled,
                            artworkPainter = artworkPainter,
                            artWidth = artWidth,
                            artHeight = artHeight,
                            artworkAreaHeight = artworkAreaHeight,
                            isCanvasVisible = isCanvasVisible,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onSeek = onSeek,
                        )
                        PlayerLayoutStyle.Ring -> RingStyleArtwork(
                            title = title,
                            artist = artist,
                            crossfadeEnabled = crossfadeEnabled,
                            artworkPainter = artworkPainter,
                            artWidth = artWidth,
                            artworkAreaHeight = artworkAreaHeight,
                            foreground = foreground,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onSeek = onSeek,
                        )
                    }
                }

                // Layout switcher — purely visual, does not touch playback state.
            }

            if (isCanvasVisible || layoutStyle == PlayerLayoutStyle.Ring) {
                // In canvas mode (or the ring style, which keeps the cover clean)
                // title/artist live below the artwork instead of overlaid on it.
                if (isCanvasVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 22.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.sp,
                        )
                        Text(
                            text = artist,
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.sp,
                        )
                        if (crossfadeEnabled) {
                            Text(
                                text = stringResource(R.string.crossfade_active_badge),
                                color = palette.accent.copy(alpha = 0.72f),
                                fontSize = 9.sp,
                                maxLines = 1,
                                letterSpacing = 0.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = formatAsDuration(positionMs.coerceAtLeast(0L)),
                color = foreground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
            )
            Spacer(Modifier.height(16.dp))

            PrimaryControlsRow(
                foreground = foreground,
                controlSurface = controlSurface,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                isLiked = isLiked,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onLike = onLike,
            )

            Spacer(Modifier.height(12.dp))

            SecondaryControlsRow(
                foreground = foreground,
                shuffleEnabled = shuffleEnabled,
                repeatIconRes = repeatIconRes,
                onShuffle = onShuffle,
                onLyrics = onLyrics,
                onQueue = onQueue,
                onRepeat = onRepeat,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  Artwork headers — visual only, both drive onSeek/positionMs/durationMs */
/* ---------------------------------------------------------------------- */

@Composable
private fun ArcStyleArtwork(
    title: String,
    artist: String,
    crossfadeEnabled: Boolean,
    artworkPainter: androidx.compose.ui.graphics.painter.Painter,
    artWidth: androidx.compose.ui.unit.Dp,
    artHeight: androidx.compose.ui.unit.Dp,
    artworkAreaHeight: androidx.compose.ui.unit.Dp,
    isCanvasVisible: Boolean,
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
        if (!isCanvasVisible) LiquidArcSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            activeColor = palette.text,
            trackColor = palette.textDisabled.copy(alpha = 0.55f),
            scrubberColor = palette.text,
            onSeek = onSeek,
            modifier = Modifier.fillMaxSize()
        )

        if (!isCanvasVisible) Box(
            modifier = Modifier
                .width(artWidth)
                .height(artHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = artWidth / 2,
                        bottomEnd = artWidth / 2,
                    ),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f),
                )
                .clip(
                    RoundedCornerShape(
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(artHeight * 0.48f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                )
                if (crossfadeEnabled) {
                    Text(
                        text = stringResource(R.string.crossfade_active_badge),
                        color = palette.accent.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RingStyleArtwork(
    title: String,
    artist: String,
    crossfadeEnabled: Boolean,
    artworkPainter: androidx.compose.ui.graphics.painter.Painter,
    artWidth: androidx.compose.ui.unit.Dp,
    artworkAreaHeight: androidx.compose.ui.unit.Dp,
    foreground: Color,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val palette = colorPalette()
    val ringDiameter = artWidth * 0.82f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = foreground,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
            modifier = Modifier.fillMaxWidth(0.88f),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = artist,
            color = foreground.copy(alpha = 0.66f),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
        )
        if (crossfadeEnabled) {
            Text(
                text = stringResource(R.string.crossfade_active_badge),
                color = palette.accent.copy(alpha = 0.72f),
                fontSize = 9.sp,
                maxLines = 1,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .width(artWidth + 44.dp)
                .height(artWidth + 44.dp),
            contentAlignment = Alignment.Center
        ) {
            ClassicRingSeekBar(
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
                    .size(ringDiameter)
                    .shadow(elevation = 16.dp, shape = CircleShape)
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
}

/* ---------------------------------------------------------------------- */
/*  Shared control rows — identical bindings for both styles              */
/* ---------------------------------------------------------------------- */

@Composable
private fun PrimaryControlsRow(
    foreground: Color,
    controlSurface: Color,
    isPlaying: Boolean,
    isBuffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isLiked: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
) {
    val palette = colorPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidControlButton(
            icon = R.drawable.play_skip_back,
            tint = foreground,
            enabled = canSkipPrevious,
            onClick = onPrevious,
        )
        Surface(
            color = palette.text,
            contentColor = palette.background0,
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .border(1.dp, controlSurface.copy(alpha = 0.42f), CircleShape),
        ) {
            IconButton(onClick = onPlayPause) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = palette.background0,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = palette.background0,
                        modifier = Modifier.size(28.dp),
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
            icon = if (isLiked) R.drawable.heart else R.drawable.heart_outline,
            tint = if (isLiked) palette.accent else foreground,
            onClick = onLike,
        )
    }
}

@Composable
private fun SecondaryControlsRow(
    foreground: Color,
    shuffleEnabled: Boolean,
    repeatIconRes: Int,
    onShuffle: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onRepeat: () -> Unit,
) {
    val palette = colorPalette()
    Row(
        modifier = Modifier.fillMaxWidth(0.86f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidControlButton(
            icon = R.drawable.shuffle,
            tint = if (shuffleEnabled) palette.accent else foreground,
            onClick = onShuffle,
        )
        LiquidControlButton(
            icon = R.drawable.song_lyrics,
            tint = foreground,
            onClick = onLyrics,
        )
        LiquidControlButton(
            icon = R.drawable.playlist,
            tint = foreground,
            onClick = onQueue,
        )
        LiquidControlButton(
            icon = repeatIconRes,
            tint = foreground,
            onClick = onRepeat,
        )
    }
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
/*  Layout style toggle — small, unobtrusive, in the corner               */
/* ---------------------------------------------------------------------- */

@Composable
private fun LayoutStyleToggle(
    style: PlayerLayoutStyle,
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
                // A tiny glyph that hints "switch layout": a circle + a
                // rounded square, whichever matches the *other* style is
                // drawn solid so it reads as "tap to switch to this one".
                val strokeWidth = 1.6.dp.toPx()
                when (style) {
                    PlayerLayoutStyle.Arc -> {
                        // Currently Arc → hint at Ring (draw a circle, solid)
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
                    PlayerLayoutStyle.Ring -> {
                        // Currently Ring → hint at Arc (draw a rounded square)
                        val inset = strokeWidth
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.32f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  Seek bars                                                             */
/* ---------------------------------------------------------------------- */

@Composable
private fun LiquidArcSeekBar(
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
    var isDragging by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(playbackProgress) }
    var dragAccepted by remember { mutableStateOf(false) }
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
                    seekFraction(offset, size, 48.dp.toPx())
                        ?.let { onSeek((it * validDuration).toLong()) }
                }
            }
            .pointerInput(validDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = seekFraction(offset, size, 48.dp.toPx())
                        dragAccepted = fraction != null
                        if (fraction != null) draggingProgress = fraction
                    },
                    onDrag = { change, _ ->
                        if (dragAccepted) {
                            seekFraction(change.position, size, 64.dp.toPx())
                                ?.let { draggingProgress = it }
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
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val angle = Math.toRadians((startAngle + activeSweep).toDouble())
        val scrubber = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        )
        drawCircle(Color.White, radius = 10.dp.toPx(), center = scrubber)
        drawCircle(
            color = scrubberColor,
            radius = 10.dp.toPx(),
            center = scrubber,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Near-full ring seek bar (modelled on the second reference image): the
 * track wraps almost the whole way around the circular cover, leaving a
 * small gap at the top. Same drag/tap seek behaviour as [LiquidArcSeekBar].
 */
@Composable
private fun ClassicRingSeekBar(
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
    var isDragging by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(playbackProgress) }
    var dragAccepted by remember { mutableStateOf(false) }
    val visibleProgress = if (isDragging && dragAccepted) draggingProgress else playbackProgress

    // Ring geometry: starts at the top with a small gap, sweeps clockwise
    // almost 360 degrees.
    val startAngle = -90f + 6f
    val sweepAngle = 348f

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
                    seekFraction(offset, size, 48.dp.toPx())
                        ?.let { onSeek((it * validDuration).toLong()) }
                }
            }
            .pointerInput(validDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = seekFraction(offset, size, 48.dp.toPx())
                        dragAccepted = fraction != null
                        if (fraction != null) draggingProgress = fraction
                    },
                    onDrag = { change, _ ->
                        if (dragAccepted) {
                            seekFraction(change.position, size, 64.dp.toPx())
                                ?.let { draggingProgress = it }
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
        val radius = size.width * 0.46f
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
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val angle = Math.toRadians((startAngle + activeSweep).toDouble())
        val scrubber = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        )
        drawCircle(Color.White, radius = 9.dp.toPx(), center = scrubber)
        drawCircle(
            color = scrubberColor,
            radius = 9.dp.toPx(),
            center = scrubber,
            style = Stroke(width = 2.5.dp.toPx())
        )
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

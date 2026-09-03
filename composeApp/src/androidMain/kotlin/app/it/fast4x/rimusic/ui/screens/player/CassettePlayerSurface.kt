/*
 * Cassette artwork  (GPL-3.0), with Cubic retaining
 * playback, queue, library, navigation, and settings ownership.
 */
package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val CassetteShellTop = Color(0xFF5A4B3D)
private val CassetteShellMid = Color(0xFF3B3128)
private val CassetteShellBottom = Color(0xFF262019)
private val CassetteCream = Color(0xFFF2E7D0)
private val CassetteCreamDark = Color(0xFFE4D6BC)
private val CassetteLabelBorder = Color(0xFFC9B999)
private val CassetteWindow = Color(0xFF241D15)
private val CassetteSpool = Color(0xFF14100B)
private val CassetteHub = Color(0xFFEFE6D2)
private val CassetteInk = Color(0xFF3A2F24)
private val CassetteDarkKey = Color(0xFF2A241E)

@Composable
internal fun CassettePlayerSurface(
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
    playbackErrorMessage: String? = null,
    errorActionLabel: String? = null,
    onErrorAction: (() -> Unit)? = null,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onLike: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSleepTimer: () -> Unit,
    onAppearance: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = colorPalette()
    val foreground = if (isCanvasVisible) Color.White else palette.text
    val accent = palette.accent
    val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
    val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
    val album = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
    val artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
    val artworkPainter = if (artworkUrl.isNotBlank()) ImageCacheFactory.Painter(artworkUrl) else null
    val duration = durationMs.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val progress = if (duration > 0L) {
        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isCanvasVisible) {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
    ) {
        val horizontalPadding = (maxWidth * 0.055f).coerceIn(16.dp, 28.dp)
        val contentWidth = maxWidth.coerceAtMost(520.dp)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .width(contentWidth)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                )
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        CassetteHeader(
            nowPlayingFrom = album,
            foreground = foreground,
            onDismiss = onDismiss,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            val tapeWidth = minOf(maxWidth, maxHeight * 1.55f, 500.dp)
            if (playbackErrorMessage != null) {
                PlayerSurfacePlaybackError(
                    message = playbackErrorMessage,
                    actionLabel = errorActionLabel,
                    onAction = onErrorAction,
                    foreground = foreground,
                    accent = accent,
                    modifier = Modifier.width(tapeWidth),
                )
            } else {
                CassetteTape(
                    isPlaying = isPlaying,
                    progress = progress,
                    accent = accent,
                    artworkPainter = artworkPainter,
                    crossfadeEnabled = crossfadeEnabled,
                    modifier = Modifier.width(tapeWidth),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = foreground,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
            )
            Text(
                text = artist,
                color = foreground.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
            )
        }

        Spacer(Modifier.height(10.dp))

        CassetteWaveformCard(
            positionMs = positionMs,
            durationMs = duration,
            accent = accent,
            isLiked = isLiked,
            onLike = onLike,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )

        Spacer(Modifier.height(12.dp))

        CassetteTransportRow(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            shuffleEnabled = shuffleEnabled,
            repeatIconRes = repeatIconRes,
            repeatEnabled = repeatEnabled,
            accent = accent,
            foreground = foreground,
            onShuffle = onShuffle,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onRepeat = onRepeat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )

        Spacer(Modifier.height(12.dp))

        CassetteBottomRow(
            accent = accent,
            onLyrics = onLyrics,
            onQueue = onQueue,
            onSleepTimer = onSleepTimer,
            onAppearance = onAppearance,
            onMore = onMore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )
        }
    }
}

@Composable
private fun CassetteHeader(
    nowPlayingFrom: String,
    foreground: Color,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 50.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.chevron_down),
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.now_playing),
                color = foreground,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                letterSpacing = 0.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.9f),
                        offset = Offset(0f, 1f),
                        blurRadius = 4f,
                    )
                ),
            )
            if (nowPlayingFrom.isNotBlank()) {
                Text(
                    text = nowPlayingFrom,
                    color = foreground.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                )
            }

        }
    }
}

@Composable
private fun CassetteWaveformCard(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CassetteCream)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatAsDuration(positionMs.coerceAtLeast(0L)),
                    color = CassetteInk,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = formatAsDuration(durationMs),
                    color = CassetteInk,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            val fraction = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs > 0L) {
                                onSeek(
                                    (offset.x / size.width * durationMs)
                                        .toLong()
                                        .coerceIn(0L, durationMs)
                                )
                            }
                        }
                    }
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures { change, _ ->
                            if (durationMs > 0L) {
                                onSeek(
                                    (change.position.x / size.width * durationMs)
                                        .toLong()
                                        .coerceIn(0L, durationMs)
                                )
                                change.consume()
                            }
                        }
                    },
            ) {
                val barCount = 36
                val gap = size.width / barCount
                val barWidth = gap * 0.55f
                for (index in 0 until barCount) {
                    val wave = abs(
                        sin(index * 1.7) * 0.5 +
                            sin(index * 0.53 + 1.3) * 0.5
                    )
                    val barHeight = size.height * (0.30f + 0.65f * wave.toFloat()).coerceIn(0.15f, 1f)
                    val x = gap * index + (gap - barWidth) / 2f
                    drawRoundRect(
                        color = if ((index + 0.5f) / barCount <= fraction) {
                            accent
                        } else {
                            CassetteInk.copy(alpha = 0.25f)
                        },
                        topLeft = Offset(x, (size.height - barHeight) / 2f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            painter = painterResource(if (isLiked) R.drawable.heart else R.drawable.heart_outline),
            contentDescription = null,
            tint = if (isLiked) accent else CassetteInk,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onLike)
                .padding(2.dp),
        )
    }
}

@Composable
private fun CassetteTransportRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatIconRes: Int,
    repeatEnabled: Boolean,
    accent: Color,
    foreground: Color,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CassettePlainButton(
            icon = R.drawable.shuffle,
            tint = if (shuffleEnabled) accent else foreground,
            onClick = onShuffle,
        )
        Spacer(Modifier.width(6.dp))
        CassetteKey(
            width = 60.dp,
            height = 50.dp,
            background = CassetteCream,
            enabled = canSkipPrevious,
            onClick = onPrevious,
        ) {
            Icon(
                painter = painterResource(R.drawable.play_skip_back),
                contentDescription = null,
                tint = CassetteInk,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        CassetteKey(
            width = 74.dp,
            height = 56.dp,
            background = accent,
            onClick = onPlayPause,
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        CassetteKey(
            width = 60.dp,
            height = 50.dp,
            background = CassetteCream,
            enabled = canSkipNext,
            onClick = onNext,
        ) {
            Icon(
                painter = painterResource(R.drawable.play_skip_forward),
                contentDescription = null,
                tint = CassetteInk,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        CassettePlainButton(
            icon = repeatIconRes,
            tint = if (repeatEnabled) accent else foreground,
            onClick = onRepeat,
        )
    }
}

@Composable
private fun CassetteKey(
    width: Dp,
    height: Dp,
    background: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = width, height = height)
            .alpha(if (enabled) 1f else 0.34f)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

@Composable
private fun CassettePlainButton(
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CassetteBottomRow(
    accent: Color,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSleepTimer: () -> Unit,
    onAppearance: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp)),
    ) {
        CassetteSegment(accent, R.drawable.song_lyrics, Color.White, onLyrics)
        CassetteSegment(CassetteDarkKey, R.drawable.playlist, CassetteCream, onQueue)
        CassetteSegment(CassetteDarkKey, R.drawable.sleep, CassetteCream, onSleepTimer)
        CassetteSegment(CassetteDarkKey, R.drawable.color_palette, CassetteCream, onAppearance)
        CassetteSegment(CassetteDarkKey, R.drawable.ellipsis_horizontal, CassetteCream, onMore)
    }
}

@Composable
private fun RowScope.CassetteSegment(
    background: Color,
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CassetteTape(
    isPlaying: Boolean,
    progress: Float,
    accent: Color,
    artworkPainter: Painter?,
    crossfadeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
            )
        }
    }
    val tape by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "cassette_tape_transfer",
    )

    BoxWithConstraints(modifier = modifier.aspectRatio(1.55f)) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight

        Canvas(Modifier.fillMaxSize()) {
            drawCassetteBase()
        }

        artworkPainter?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = boxWidth * 0.075f, y = boxHeight * 0.075f)
                    .size(width = boxWidth * 0.83f, height = boxHeight * 0.545f)
                    .clip(RoundedCornerShape(boxWidth * 0.02f))
                    .alpha(0.22f),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCassetteDetails(tape, rotation.value, accent)
        }

        artworkPainter?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = boxWidth * 0.10f, y = boxHeight * 0.10f)
                    .size(width = boxWidth * 0.055f, height = boxHeight * 0.085f)
                    .clip(RoundedCornerShape(boxWidth * 0.008f))
                    .border(
                        1.dp,
                        CassetteInk.copy(alpha = 0.6f),
                        RoundedCornerShape(boxWidth * 0.008f),
                    ),
            )
        }

        Text(
            text = "A",
            color = CassetteInk,
            fontWeight = FontWeight.Black,
            fontSize = (boxWidth.value * 0.045f).sp,
            modifier = Modifier.offset(x = boxWidth * 0.182f, y = boxHeight * 0.095f),
        )
        Text(
            text = "60",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (boxWidth.value * 0.052f).sp,
            modifier = Modifier.offset(x = boxWidth * 0.82f, y = boxHeight * 0.465f),
        )

        if (crossfadeEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.crossfade_active_badge),
                    color = accent,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

private fun DrawScope.drawCassetteBase() {
    val width = size.width
    val height = size.height

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(width * 0.05f, height * 0.10f),
        size = Size(width * 0.97f, height * 0.93f),
        cornerRadius = CornerRadius(width * 0.06f, width * 0.06f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(width * 0.035f, height * 0.075f),
        size = Size(width * 0.965f, height * 0.925f),
        cornerRadius = CornerRadius(width * 0.055f, width * 0.055f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = Offset(width * 0.022f, height * 0.048f),
        size = Size(width * 0.96f, height * 0.93f),
        cornerRadius = CornerRadius(width * 0.05f, width * 0.05f),
    )

    val shellTopLeft = Offset(width * 0.005f, 0f)
    val shellSize = Size(width * 0.97f, height * 0.955f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(CassetteShellTop, CassetteShellMid, CassetteShellBottom)
        ),
        topLeft = shellTopLeft,
        size = shellSize,
        cornerRadius = CornerRadius(width * 0.045f, width * 0.045f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(shellTopLeft.x + width * 0.012f, shellTopLeft.y + height * 0.02f),
        size = Size(shellSize.width - width * 0.024f, shellSize.height - height * 0.04f),
        cornerRadius = CornerRadius(width * 0.035f, width * 0.035f),
        style = Stroke(width = width * 0.003f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = shellTopLeft,
        size = shellSize,
        cornerRadius = CornerRadius(width * 0.045f, width * 0.045f),
        style = Stroke(width = width * 0.0025f),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.18f),
        start = Offset(width * 0.06f, height * 0.018f),
        end = Offset(width * 0.92f, height * 0.018f),
        strokeWidth = height * 0.012f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.30f),
        start = Offset(width * 0.06f, height * 0.935f),
        end = Offset(width * 0.92f, height * 0.935f),
        strokeWidth = height * 0.014f,
        cap = StrokeCap.Round,
    )

    for ((x, y) in listOf(0.045f to 0.07f, 0.935f to 0.07f, 0.045f to 0.87f, 0.935f to 0.87f)) {
        val center = Offset(width * x, height * y)
        drawCircle(Color(0xFF1C1712), radius = width * 0.016f, center = center)
        drawCircle(
            Color.White.copy(alpha = 0.20f),
            radius = width * 0.016f,
            center = center,
            style = Stroke(width = width * 0.003f),
        )
        drawLine(
            Color(0xFF54463A),
            center - Offset(width * 0.008f, 0f),
            center + Offset(width * 0.008f, 0f),
            strokeWidth = width * 0.004f,
        )
    }

    drawRoundRect(
        brush = Brush.verticalGradient(listOf(CassetteCream, CassetteCreamDark)),
        topLeft = Offset(width * 0.075f, height * 0.075f),
        size = Size(width * 0.83f, height * 0.545f),
        cornerRadius = CornerRadius(width * 0.02f, width * 0.02f),
    )
}

private fun DrawScope.drawCassetteDetails(
    progress: Float,
    rotationDegrees: Float,
    accent: Color,
) {
    val width = size.width
    val height = size.height

    drawRoundRect(
        color = CassetteLabelBorder,
        topLeft = Offset(width * 0.075f, height * 0.075f),
        size = Size(width * 0.83f, height * 0.545f),
        cornerRadius = CornerRadius(width * 0.02f, width * 0.02f),
        style = Stroke(width = width * 0.004f),
    )
    drawRoundRect(
        color = CassetteInk,
        topLeft = Offset(width * 0.17f, height * 0.10f),
        size = Size(width * 0.055f, height * 0.085f),
        cornerRadius = CornerRadius(width * 0.008f, width * 0.008f),
        style = Stroke(width = width * 0.0045f),
    )

    drawRect(
        color = accent,
        topLeft = Offset(width * 0.075f, height * 0.455f),
        size = Size(width * 0.83f, height * 0.105f),
    )
    for (index in 1..3) {
        val y = height * (0.455f + 0.105f * index / 4f)
        drawLine(
            Color.Black.copy(alpha = 0.10f),
            Offset(width * 0.075f, y),
            Offset(width * 0.905f, y),
            strokeWidth = height * 0.006f,
        )
    }
    drawRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(width * 0.075f, height * 0.575f),
        size = Size(width * 0.83f, height * 0.022f),
    )

    val windowTopLeft = Offset(width * 0.285f, height * 0.155f)
    val windowSize = Size(width * 0.43f, height * 0.27f)
    val windowRadius = width * 0.02f
    drawRoundRect(
        color = CassetteWindow,
        topLeft = windowTopLeft,
        size = windowSize,
        cornerRadius = CornerRadius(windowRadius, windowRadius),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = windowTopLeft,
        size = windowSize,
        cornerRadius = CornerRadius(windowRadius, windowRadius),
        style = Stroke(width = width * 0.008f),
    )

    val windowPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = windowTopLeft.x,
                top = windowTopLeft.y,
                right = windowTopLeft.x + windowSize.width,
                bottom = windowTopLeft.y + windowSize.height,
                cornerRadius = CornerRadius(windowRadius, windowRadius),
            )
        )
    }
    val reelY = height * 0.29f
    val leftCenter = Offset(width * 0.385f, reelY)
    val rightCenter = Offset(width * 0.615f, reelY)
    val minimumSpool = width * 0.055f
    val maximumSpool = width * 0.115f
    val leftSpool = minimumSpool + (maximumSpool - minimumSpool) * (1f - progress)
    val rightSpool = minimumSpool + (maximumSpool - minimumSpool) * progress

    clipPath(windowPath) {
        drawLine(
            CassetteSpool,
            Offset(leftCenter.x, windowTopLeft.y + windowSize.height - height * 0.02f),
            Offset(rightCenter.x, windowTopLeft.y + windowSize.height - height * 0.02f),
            strokeWidth = height * 0.028f,
        )
        drawLine(
            Color.White.copy(alpha = 0.10f),
            Offset(leftCenter.x, windowTopLeft.y + windowSize.height - height * 0.026f),
            Offset(rightCenter.x, windowTopLeft.y + windowSize.height - height * 0.026f),
            strokeWidth = height * 0.006f,
        )

        for ((center, spool) in listOf(leftCenter to leftSpool, rightCenter to rightSpool)) {
            drawCircle(CassetteSpool, radius = spool, center = center)
            drawCircle(
                Color.White.copy(alpha = 0.05f),
                radius = spool * 0.85f,
                center = center,
                style = Stroke(width = 1.2f),
            )
            drawCircle(
                Color.White.copy(alpha = 0.05f),
                radius = spool * 0.65f,
                center = center,
                style = Stroke(width = 1.2f),
            )

            rotate(degrees = rotationDegrees, pivot = center) {
                drawCircle(
                    CassetteHub,
                    radius = width * 0.042f,
                    center = center,
                    style = Stroke(width = width * 0.013f),
                )
                for (index in 0 until 6) {
                    val angle = Math.toRadians(index * 60.0)
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(
                        CassetteHub,
                        center + Offset(direction.x * width * 0.016f, direction.y * width * 0.016f),
                        center + Offset(direction.x * width * 0.040f, direction.y * width * 0.040f),
                        strokeWidth = width * 0.011f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawCircle(CassetteWindow, radius = width * 0.012f, center = center)
        }

        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.09f), Color.Transparent, Color.Transparent),
                start = windowTopLeft,
                end = Offset(
                    windowTopLeft.x + windowSize.width * 0.8f,
                    windowTopLeft.y + windowSize.height,
                ),
            ),
            topLeft = windowTopLeft,
            size = windowSize,
            cornerRadius = CornerRadius(windowRadius, windowRadius),
        )
    }

    val trapezoid = Path().apply {
        moveTo(width * 0.30f, height * 0.945f)
        lineTo(width * 0.35f, height * 0.755f)
        lineTo(width * 0.65f, height * 0.755f)
        lineTo(width * 0.70f, height * 0.945f)
    }
    drawPath(
        trapezoid,
        Color(0xFF6B5B49).copy(alpha = 0.65f),
        style = Stroke(width = width * 0.006f),
    )
    for (x in listOf(0.415f, 0.585f)) {
        val center = Offset(width * x, height * 0.85f)
        drawCircle(Color(0xFF17120D), radius = width * 0.016f, center = center)
        drawCircle(
            Color.White.copy(alpha = 0.18f),
            radius = width * 0.016f,
            center = center,
            style = Stroke(width = width * 0.003f),
        )
    }
    for (x in listOf(0.375f, 0.625f)) {
        drawCircle(
            Color(0xFF17120D),
            radius = width * 0.009f,
            center = Offset(width * x, height * 0.79f),
        )
    }

    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset(width * 0.9f, height),
        ),
        topLeft = Offset(width * 0.005f, 0f),
        size = Size(width * 0.97f, height * 0.955f),
        cornerRadius = CornerRadius(width * 0.045f, width * 0.045f),
    )
}

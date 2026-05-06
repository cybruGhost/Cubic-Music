@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.MiniPlayerType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.NowPlayingSongIndicator
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.ui.styling.favoritesOverlay
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.backgroundProgressKey
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.disableClosingPlayerSwipingDownKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.getLikedIcon
import app.it.fast4x.rimusic.utils.getUnlikedIcon
import app.it.fast4x.rimusic.utils.intent
import app.it.fast4x.rimusic.utils.isExplicit
import app.it.fast4x.rimusic.utils.miniPlayerTypeKey
import app.it.fast4x.rimusic.utils.playNext
import app.it.fast4x.rimusic.utils.playPrevious
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.kreate.android.me.knighthat.sync.YouTubeSync
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ─── Liquid crossfade palette (mirrors GetSeekBar.kt) ────────────────────────
private val crossfadeLiquidColors = listOf(
    Color(0xFF123B31),
    Color(0xFF2EE59D),
    Color(0xFF5B2FD9),
    Color(0xFFF472B6),
    Color(0xFFFB7185),
    Color(0xFF381A33),
)

/**
 * Draws a shifting, pulsing rainbow outline — the same liquid gradient logic
 * used in [animatedCrossfadeTimelineModifier] in GetSeekBar.kt — but applied
 * as a border around the MiniPlayer card.
 *
 * Two layers:
 *  1. A wide, soft outer halo (alpha ≈ 0.28 * pulse) for the glow bloom.
 *  2. A tight inner edge (alpha ≈ 0.92 * pulse) for the crisp rainbow border.
 */
private fun Modifier.crossfadeGlowBorder(
    isActive : Boolean,
    shift    : Float,   // -1f → 1f (horizontal sweep, from infiniteTransition)
    pulse    : Float,   // 0.78f ↔ 1f (breathe, from infiniteTransition)
    cornerPx : Float,
    strokePx : Float,
): Modifier {
    if (!isActive) return this
    return drawWithContent {
        drawContent()

        val gradStart = Offset(x = size.width * shift - size.width, y = 0f)
        val gradEnd   = Offset(x = gradStart.x + size.width * 2f,  y = size.height)

        // Layer 1 — wide soft halo
        drawRoundRect(
            brush        = Brush.linearGradient(
                colors = crossfadeLiquidColors,
                start  = gradStart,
                end    = gradEnd
            ),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style        = Stroke(width = strokePx * 5f),
            alpha        = 0.28f * pulse
        )
        // Layer 2 — crisp rainbow edge (reversed palette, complementary sweep)
        drawRoundRect(
            brush        = Brush.linearGradient(
                colors = crossfadeLiquidColors.reversed(),
                start  = Offset(x = size.width - gradEnd.x, y = 0f),
                end    = Offset(x = size.width - gradStart.x, y = size.height)
            ),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style        = Stroke(width = strokePx),
            alpha        = 0.94f * pulse
        )
    }
}

// ─── MiniPlayer ───────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    showPlayer  : () -> Unit,
    hidePlayer  : () -> Unit,
    navController: NavController? = null,
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    val context       = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val displayedPlayerState = rememberDisplayedPlayerState(binder)
    val isCrossfading        = false

    // ── Error state ───────────────────────────────────────────────────────────
    var playerError by remember { mutableStateOf<PlaybackException?>(binder.player.playerError) }
    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playerError = binder.player.playerError
            }
            override fun onPlayerError(error: PlaybackException) {
                playerError = error
            }
        }
    }

    val mediaItem       = displayedPlayerState.mediaItem ?: return
    val shouldBePlaying = displayedPlayerState.shouldBePlaying && playerError == null
    val isBuffering     = displayedPlayerState.isBuffering
    playerError?.let { PlayerError(error = it) }

    // ── Like state ────────────────────────────────────────────────────────────
    val isSongLiked by remember(mediaItem.mediaId) {
        Database.songTable.isLiked(mediaItem.mediaId).distinctUntilChanged()
    }.collectAsState(false, Dispatchers.IO)

    var miniPlayerType by rememberPreference(miniPlayerTypeKey, MiniPlayerType.Modern)

    fun toggleLike() {
        CoroutineScope(Dispatchers.IO).launch { YouTubeSync.toggleSongLike(context, mediaItem) }
    }

    val positionAndDuration = displayedPlayerState.position to displayedPlayerState.duration

    // ── Swipe-to-dismiss ──────────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd ->
                    if (miniPlayerType == MiniPlayerType.Essential) toggleLike()
                    else binder.player.seekToPrevious()
                SwipeToDismissBoxValue.EndToStart -> binder.player.seekToNext()
                else -> Unit
            }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            false
        }
    )

    // ── Preferences ───────────────────────────────────────────────────────────
    val backgroundProgress              by rememberPreference(backgroundProgressKey, BackgroundProgress.MiniPlayer)
    val effectRotationEnabled           by rememberPreference(effectRotationKey, true)
    val disableClosingPlayerSwipingDown by rememberPreference(disableClosingPlayerSwipingDownKey, false)
    val disableScrollingText            by rememberPreference(disableScrollingTextKey, false)

    // ── Play/pause pill shape ─────────────────────────────────────────────────
    val shouldBePlayingTransition = updateTransition(shouldBePlaying, label = "shouldBePlaying")
    val playPauseRoundness by shouldBePlayingTransition.animateDp(
        transitionSpec = { tween(100, easing = LinearEasing) },
        label = "playPauseRoundness",
        targetValueByState = { if (it) 24.dp else 12.dp }
    )

    // ── Icon spin ─────────────────────────────────────────────────────────────
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue    = if (isRotated) 360F else 0f,
        animationSpec  = tween(200),
        label          = "iconRotation"
    )

    // ── Density — hoisted so DrawScope lambdas stay non-Composable ───────────
    val density        = LocalDensity.current
    val strokeWidthPx  = with(density) { 2.dp.toPx()  }
    val cornerRadiusPx = with(density) { 12.dp.toPx() }

    // ── Liquid crossfade glow animation ───────────────────────────────────────
    // rememberInfiniteTransition always runs but the Modifier extension
    // short-circuits immediately (returns `this`) when isCrossfading == false,
    // so the actual drawing never happens during normal playback.
    val glowTransition = rememberInfiniteTransition(label = "miniPlayerCrossfadeGlow")

    val glowShift by glowTransition.animateFloat(
        initialValue = -1f,
        targetValue  =  1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowShift"
    )
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.78f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // ── Shapes & brushes ──────────────────────────────────────────────────────
    val glassShape          = RoundedCornerShape(12.dp)
    val colorPaletteSnap    = colorPalette()

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            colorPaletteSnap.background2.copy(alpha = 0.72f),
            colorPaletteSnap.background1.copy(alpha = 0.55f),
        ),
        start = Offset(0f, 0f),
        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val glassHighlightBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
        start  = Offset(0f, 0f),
        end    = Offset(0f, Float.POSITIVE_INFINITY)
    )

    // ── Root container ────────────────────────────────────────────────────────
    SwipeToDismissBox(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(glassShape),
        state = dismissState,
        backgroundContent = {
            val offset = try { dismissState.requireOffset() } catch (_: Exception) { 0f }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorPalette().background1)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = when {
                    offset > 0 -> Arrangement.Start
                    offset < 0 -> Arrangement.End
                    else       -> Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon: ImageVector? = when {
                    offset > 0 ->
                        if (miniPlayerType == MiniPlayerType.Modern)
                            ImageVector.vectorResource(R.drawable.play_skip_back)
                        else if (isSongLiked)
                            ImageVector.vectorResource(R.drawable.heart)
                        else
                            ImageVector.vectorResource(R.drawable.heart_outline)
                    offset < 0 -> ImageVector.vectorResource(R.drawable.play_skip_forward)
                    else       -> null
                }
                if (icon != null)
                    Icon(imageVector = icon, contentDescription = null,
                        tint = colorPalette().iconButtonPlayer)
            }
        }
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.Top,
            modifier = Modifier
                .combinedClickable(
                    onLongClick = {
                        navController?.navigate(NavRoutes.queue.name)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = { showPlayer() }
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        when {
                            dragAmount < 0  -> showPlayer()
                            dragAmount > 20 ->
                                if (!disableClosingPlayerSwipingDown) {
                                    binder.stopRadio()
                                    binder.player.clearMediaItems()
                                    hidePlayer()
                                    runCatching {
                                        context.stopService(context.intent<PlayerServiceModern>())
                                    }
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    Toaster.i(R.string.player_swiping_down_is_disabled)
                                }
                        }
                    }
                }
                // Frosted glass base
                .background(brush = glassBrush, shape = glassShape)
                // Top-edge light shimmer
                .background(brush = glassHighlightBrush, shape = glassShape)
                // Static glass border — always visible
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.04f),
                        )
                    ),
                    shape = glassShape
                )
                // ✦ Liquid rainbow crossfade glow — only draws during crossfade ✦
                .crossfadeGlowBorder(
                    isActive  = isCrossfading,
                    shift     = glowShift,
                    pulse     = glowPulse,
                    cornerPx  = cornerRadiusPx,
                    strokePx  = strokeWidthPx,
                )
                .fillMaxWidth()
                // Progress fill underlay
                .drawBehind {
                    if (backgroundProgress == BackgroundProgress.Both ||
                        backgroundProgress == BackgroundProgress.MiniPlayer
                    ) {
                        drawRect(
                            color    = colorPaletteSnap.favoritesOverlay,
                            topLeft  = Offset.Zero,
                            size     = Size(
                                width  = positionAndDuration.first.toFloat() /
                                         positionAndDuration.second.absoluteValue * size.width,
                                height = size.maxDimension
                            )
                        )
                    }
                }
        ) {

            Spacer(Modifier.width(2.dp))

            // ── Album art ─────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(Dimensions.miniPlayerHeight)
            ) {
                val currentArtwork = mediaItem.mediaMetadata.artworkUri?.toString()
                val incomingArtwork: String? = null
                val crossfadeArtProgress = 0f
                val thumbnailShape = thumbnailShape()

                Box(
                    modifier = Modifier
                        .clip(thumbnailShape)
                        .size(48.dp)
                ) {
                    ImageCacheFactory.Thumbnail(
                        thumbnailUrl = currentArtwork,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.fillMaxSize()  // Changed from matchParentSize()
                    )
                    if (isCrossfading && !incomingArtwork.isNullOrBlank()) {
                        ImageCacheFactory.Thumbnail(
                            thumbnailUrl = incomingArtwork,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                 .fillMaxSize()  // Changed from matchParentSize()
                                .drawWithContent {
                                    val split = size.width * crossfadeArtProgress
                                    val path = Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(split, 0f)
                                        lineTo(size.width, size.height)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                    clipPath(path) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                        )
                    }
                }
                NowPlayingSongIndicator(mediaItem.mediaId, binder.player)
            }

            // ── Song info ─────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(Dimensions.miniPlayerHeight)
                    .weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    if (mediaItem.isExplicit)
                        app.it.fast4x.rimusic.ui.components.themed.IconButton(
                            icon = R.drawable.explicit, color = colorPalette().text,
                            enabled = true, onClick = {},
                            modifier = Modifier.size(14.dp)
                        )
                    BasicText(
                        text     = cleanPrefix(mediaItem.mediaMetadata.title.toString()),
                        style    = typography().xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.conditional(!disableScrollingText) {
                            basicMarquee(iterations = Int.MAX_VALUE)
                        }
                    )
                }
                BasicText(
                    text     = cleanPrefix(mediaItem.mediaMetadata.artist.toString()),
                    style    = typography().xxs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.conditional(!disableScrollingText) {
                        basicMarquee(iterations = Int.MAX_VALUE)
                    }
                )
            }

            Spacer(Modifier.width(2.dp))

            // ── Playback controls ─────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier.height(Dimensions.miniPlayerHeight)
            ) {
                if (miniPlayerType == MiniPlayerType.Essential)
                    app.it.fast4x.rimusic.ui.components.themed.IconButton(
                        icon = R.drawable.play_skip_back,
                        color = colorPalette().iconButtonPlayer,
                        onClick = {
                            binder.player.playPrevious()
                            if (effectRotationEnabled) isRotated = !isRotated
                        },
                        modifier = Modifier
                            .rotate(rotationAngle)
                            .padding(horizontal = 2.dp, vertical = 8.dp)
                            .size(24.dp)
                    )

                // Play / pause — frosted glass lens
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .clickable {
                            if (shouldBePlaying) binder.gracefulPause()
                            else binder.gracefulPlay()
                            if (effectRotationEnabled) isRotated = !isRotated
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colorPalette().accent.copy(alpha = 0.25f),
                                    colorPalette().background2.copy(alpha = 0.80f),
                                )
                            )
                        )
                        .border(
                            width = 0.6.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(playPauseRoundness)
                        )
                        .size(42.dp)
                ) {
                    if (isBuffering && shouldBePlaying) {
                        CircularWavyProgressIndicator(
                            color       = colorPalette().accent,
                            trackColor  = colorPalette().text,
                            modifier    = Modifier
                                .rotate(rotationAngle)
                                .align(Alignment.Center)
                                .size(24.dp),
                            stroke      = Stroke(strokeWidthPx),
                            trackStroke = Stroke(strokeWidthPx)
                        )
                    } else {
                        Image(
                            painter     = painterResource(
                                if (shouldBePlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette().iconButtonPlayer),
                            modifier    = Modifier
                                .rotate(rotationAngle)
                                .align(Alignment.Center)
                                .size(24.dp)
                        )
                    }
                }

                if (miniPlayerType == MiniPlayerType.Essential)
                    app.it.fast4x.rimusic.ui.components.themed.IconButton(
                        icon = R.drawable.play_skip_forward,
                        color = colorPalette().iconButtonPlayer,
                        onClick = {
                            binder.player.playNext()
                            if (effectRotationEnabled) isRotated = !isRotated
                        },
                        modifier = Modifier
                            .rotate(rotationAngle)
                            .padding(horizontal = 2.dp, vertical = 8.dp)
                            .size(24.dp)
                    )

                if (miniPlayerType == MiniPlayerType.Modern)
                    app.it.fast4x.rimusic.ui.components.themed.IconButton(
                        icon    = if (isSongLiked) getLikedIcon() else getUnlikedIcon(),
                        color   = colorPalette().favoritesIcon,
                        onClick = ::toggleLike,
                        modifier = Modifier
                            .rotate(rotationAngle)
                            .padding(horizontal = 2.dp, vertical = 8.dp)
                            .size(24.dp)
                    )
            }

            Spacer(Modifier.width(2.dp))
        }
    }
}

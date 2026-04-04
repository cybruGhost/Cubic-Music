package app.it.fast4x.rimusic.utils

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.themed.rimusic.screen.player.timeline.DurationIndicator
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.PauseBetweenSongs
import app.it.fast4x.rimusic.enums.PlayerTimelineType
import app.it.fast4x.rimusic.models.ui.UiMedia
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.ProgressPercentage
import app.it.fast4x.rimusic.ui.components.SeekBar
import app.it.fast4x.rimusic.ui.components.SeekBarAudioWaves
import app.it.fast4x.rimusic.ui.components.SeekBarColored
import app.it.fast4x.rimusic.ui.components.SeekBarCustom
import app.it.fast4x.rimusic.ui.components.SeekBarThin
import app.it.fast4x.rimusic.ui.components.SeekBarWaved
import app.it.fast4x.rimusic.ui.components.SeekBarOcean
import app.it.fast4x.rimusic.ui.styling.collapsedPlayerProgressBar
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DURATION_INDICATOR_HEIGHT = 20

@Composable
private fun animatedCrossfadeTimelineModifier(
    enabled: Boolean,
    progressValue: Long,
    durationValue: Long,
): Modifier {
    if (!enabled || durationValue <= 0L || durationValue == C.TIME_UNSET) return Modifier

    val transition = rememberInfiniteTransition(label = "crossfadeTimeline")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crossfadeTimelineShift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crossfadeTimelinePulse"
    )

    return Modifier.drawWithContent {
        val progressFraction = (progressValue.toFloat() / durationValue.toFloat()).coerceIn(0f, 1f)
        val liquidColors = listOf(
            Color(0xFF123B31),
            Color(0xFF2EE59D),
            Color(0xFF5B2FD9),
            Color(0xFFF472B6),
            Color(0xFFFB7185),
            Color(0xFF381A33),
        )
        val barCorner = size.height / 2f
        val gradientStart = Offset(x = size.width * shift - size.width, y = 0f)
        val gradientEnd = Offset(x = gradientStart.x + size.width * 2f, y = size.height)

        drawRoundRect(
            color = Color(0xFF221B22).copy(alpha = 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barCorner, barCorner)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = liquidColors,
                start = gradientStart,
                end = gradientEnd
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barCorner, barCorner),
            alpha = 0.88f * pulse
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = liquidColors.reversed(),
                start = Offset(x = size.width - gradientEnd.x, y = 0f),
                end = Offset(x = size.width - gradientStart.x, y = size.height)
            ),
            size = Size(width = size.width * progressFraction, height = size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barCorner, barCorner),
            alpha = 0.98f
        )

        drawContent()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun GetSeekBar(
    position: Long,
    duration: Long,
    mediaId: String,
    media: UiMedia
    ) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    val crossfadeUiState by binder.crossfadeUiState.collectAsState()
    val playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.FakeAudioBar)
    var scrubbingPosition by remember(mediaId) {
        mutableStateOf<Long?>(null)
    }
    var transparentbar by rememberPreference(transparentbarKey, true)
    val scope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(position.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }

    val compositionLaunched = isCompositionLaunched()
    LaunchedEffect(mediaId) {
        if (compositionLaunched) animatedPosition.animateTo(0f)
    }
    LaunchedEffect(position) {
        if (!isSeeking && !animatedPosition.isRunning)
            animatedPosition.animateTo(
                position.toFloat(), tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
    }

    val crossfadePalette = if (crossfadeUiState.isHighlightActive) {
        listOf(
            Color(0xFF2EE59D),
            Color(0xFF8B5CF6),
            Color(0xFFF472B6),
            Color(0xFFFB7185),
        )
    } else {
        null
    }
    val crossfadeTimelineModifier = animatedCrossfadeTimelineModifier(
        enabled = crossfadeUiState.isHighlightActive,
        progressValue = scrubbingPosition ?: position,
        durationValue = duration
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {

        if (duration == C.TIME_UNSET)
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colorPalette().collapsedPlayerProgressBar
            )

        if (crossfadeUiState.isHighlightActive) {
            SeekBarThin(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = Color.Transparent,
                backgroundColor = Color.Transparent,
                scrubberColor = colorPalette().text,
                scrubberRadius = 5.dp,
                barHeight = 5.dp,
                shape = RoundedCornerShape(999.dp),
                modifier = crossfadeTimelineModifier
            )
        } else if (playerTimelineType != PlayerTimelineType.Default
            && playerTimelineType != PlayerTimelineType.Wavy
            && playerTimelineType != PlayerTimelineType.FakeAudioBar
            && playerTimelineType != PlayerTimelineType.ThinBar
            && playerTimelineType != PlayerTimelineType.ColoredBar
            && playerTimelineType != PlayerTimelineType.Ocean
            )
            SeekBarCustom(
                type = playerTimelineType,
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

        if (playerTimelineType == PlayerTimelineType.Default)
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

        if (playerTimelineType == PlayerTimelineType.ThinBar)
            SeekBarThin(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                //modifier = Modifier.pulsatingEffect(currentValue = scrubbingPosition?.toFloat() ?: position.toFloat(), isVisible = true)
            )

      //  update the Wavy section:
if (playerTimelineType == PlayerTimelineType.Wavy) {
    SeekBarWaved(
        value = scrubbingPosition ?: position,  // Now matches Long parameter
        minimumValue = 0,
        maximumValue = duration,
        onDragStart = {
            scrubbingPosition = it
        },
        onDrag = { delta ->
            scrubbingPosition = if (duration != C.TIME_UNSET) {
                scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
            } else {
                null
            }
        },
        onDragEnd = {
            scrubbingPosition?.let(binder.player::seekTo)
            scrubbingPosition = null
        },
        color = colorPalette().collapsedPlayerProgressBar,
        backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
        scrubberRadius = 6.dp,
        showTooltip = true
    )
}
    // Add Ocean section
        if (playerTimelineType == PlayerTimelineType.Ocean) {
            SeekBarOcean(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                scrubberRadius = 6.dp
            )
        }
        if (playerTimelineType == PlayerTimelineType.FakeAudioBar)
            SeekBarAudioWaves(
                progressPercentage = ProgressPercentage((position.toFloat() / duration.toFloat()).coerceIn(0f,1f)),
                playedColor = colorPalette().accent,
                notPlayedColor = if (transparentbar) Color.Transparent else colorPalette().textSecondary,
                waveInteraction = {
                    scrubbingPosition = (it.value * duration.toFloat()).toLong()
                    binder.player.seekTo(scrubbingPosition!!)
                    scrubbingPosition = null
                },
                modifier = Modifier
                    .height(40.dp)
                    //.pulsatingEffect(currentValue = position.toFloat() / duration.toFloat(), isVisible = true)
            )


        if (playerTimelineType == PlayerTimelineType.ColoredBar)
            SeekBarColored(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette().collapsedPlayerProgressBar,
                backgroundColor = colorPalette().textSecondary,
                shape = RoundedCornerShape(8.dp),
                crossfadePalette = crossfadePalette
            )


    }

    Spacer( modifier = Modifier.height( 8.dp ) )

    DurationIndicator( binder, scrubbingPosition, position, duration )
}

package app.it.fast4x.rimusic.ui.screens.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.service.FakeException
import app.it.fast4x.rimusic.service.LoginRequiredException
import app.it.fast4x.rimusic.service.NoInternetException
import app.it.fast4x.rimusic.service.PlayableFormatNonSupported
import app.it.fast4x.rimusic.service.PlayableFormatNotFoundException
import app.it.fast4x.rimusic.service.TimeoutException
import app.it.fast4x.rimusic.service.UnknownException
import app.it.fast4x.rimusic.service.UnplayableException
import app.it.fast4x.rimusic.service.VideoIdMismatchException
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import app.it.fast4x.rimusic.utils.center
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.currentWindow
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.secondary
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import android.content.Context
fun playbackExceptionMessage(
    context: Context,
    error: PlaybackException,
    isLocal: Boolean,
): String {
    val localMusicFileNotFoundError = context.getString(R.string.error_local_music_not_found)
    val playableFormatNotFoundError =
        context.getString(R.string.error_couldn_t_find_a_playable_audio_format)
    val unplayableError = context.getString(R.string.error_media_cannot_be_played)
    val loginRequiredError = context.getString(R.string.login_required_to_play_this_media)
    val videoIdMismatchError =
        context.getString(R.string.error_the_returned_video_id_doesn_t_match_the_requested_one)
    val noInternetError = context.getString(R.string.error_no_internet)
    val timeoutError = context.getString(R.string.error_timeout)
    val formatUnsupported = context.getString(R.string.error_file_unsupported_format)

    fun deepestCause(throwable: Throwable?): Throwable? {
        var current = throwable
        while (current?.cause != null && current.cause !== current) {
            current = current.cause
        }
        return current
    }

    if (isLocal) return localMusicFileNotFoundError

    return when (deepestCause(error) ?: deepestCause(error.cause) ?: error.cause ?: error) {
        is PlayableFormatNotFoundException -> playableFormatNotFoundError
        is UnplayableException -> unplayableError
        is LoginRequiredException -> loginRequiredError
        is VideoIdMismatchException -> videoIdMismatchError
        is PlayableFormatNonSupported -> formatUnsupported
        is NoInternetException -> noInternetError
        is TimeoutException -> timeoutError
        is UnknownException -> unplayableError
        is FakeException -> unplayableError
        else -> unplayableError
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerError(error: PlaybackException) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val message = playbackExceptionMessage(
        context = context,
        error = error,
        isLocal = binder?.player?.currentWindow?.mediaItem?.isLocal == true,
    )

    LaunchedEffect(error, message) {
        Timber.e("Playback error: ${error.cause?.cause}")
        Toaster.w(message)
    }

}

@Composable
fun PlaybackError(
    isDisplayed: Boolean,
    messageProvider: () -> String,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    actionHint: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box {
        AnimatedVisibility(
            visible = isDisplayed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Spacer(
                modifier = modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onDismiss()
                            }
                        )
                    }
                    .fillMaxSize()
                    .background(Color.Black.copy(0.8f))
            )
        }

        AnimatedVisibility(
            visible = isDisplayed,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = Color(0xE61A1D27),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth()
            ) {
                BasicText(
                    text = messageProvider(),
                    style = typography().xs.medium.color(PureBlackColorPalette.text)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.72f)
                    ) {
                        BasicText(
                            text = stringResource(R.string.tap_anywhere_to_dismiss),
                            style = typography().xxs.secondary.color(PureBlackColorPalette.text.copy(alpha = 0.82f))
                        )
                        if (!actionHint.isNullOrBlank() && onAction != null) {
                            BasicText(
                                text = actionHint,
                                style = typography().xxs.secondary.color(PureBlackColorPalette.text.copy(alpha = 0.72f)),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (!actionLabel.isNullOrBlank() && onAction != null) {
                        TextButton(onClick = onAction) {
                            BasicText(
                                text = actionLabel,
                                style = typography().xxs.medium.color(PureBlackColorPalette.text)
                            )
                        }
                    }
                }
            }
        }
    }
}

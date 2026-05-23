package app.it.fast4x.rimusic.service

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi

enum class PlaybackFailureCategory {
    NETWORK,
    SOURCE,
    UNKNOWN,
}

data class PlaybackFailureInfo(
    val category: PlaybackFailureCategory,
    val isRecoverable: Boolean,
    val canRetryCurrent: Boolean,
)

fun PlaybackException.toPlaybackFailureInfo(): PlaybackFailureInfo {
    val deepestCause = generateSequence(this as Throwable?) { it.cause }.lastOrNull()
    val messageBlob = buildString {
        append(message.orEmpty())
        append(' ')
        append(cause?.message.orEmpty())
        append(' ')
        append(deepestCause?.message.orEmpty())
        append(' ')
        append(deepestCause?.javaClass?.simpleName.orEmpty())
    }

    val networkCodes = setOf(
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    )
    val sourceCodes = setOf(
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        416,
    )

    val isNetwork = errorCode in networkCodes ||
        messageBlob.contains("UnknownHost", ignoreCase = true) ||
        messageBlob.contains("Unable to resolve host", ignoreCase = true) ||
        messageBlob.contains("timeout", ignoreCase = true) ||
        messageBlob.contains("connection", ignoreCase = true)

    val isSource = errorCode in sourceCodes ||
        messageBlob.contains("EOF", ignoreCase = true) ||
        messageBlob.contains("FileNotFound", ignoreCase = true) ||
        messageBlob.contains("No such file", ignoreCase = true) ||
        messageBlob.contains("invalid", ignoreCase = true) ||
        messageBlob.contains("unsupported", ignoreCase = true) ||
        messageBlob.contains("403", ignoreCase = true) ||
        messageBlob.contains("404", ignoreCase = true) ||
        messageBlob.contains("range", ignoreCase = true)

    val category = when {
        isNetwork -> PlaybackFailureCategory.NETWORK
        isSource -> PlaybackFailureCategory.SOURCE
        else -> PlaybackFailureCategory.UNKNOWN
    }

    return PlaybackFailureInfo(
        category = category,
        isRecoverable = category != PlaybackFailureCategory.UNKNOWN || messageBlob.contains("reset", ignoreCase = true),
        canRetryCurrent = category == PlaybackFailureCategory.NETWORK || category == PlaybackFailureCategory.SOURCE,
    )
}

@UnstableApi
class PlayableFormatNotFoundException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
@UnstableApi
class UnplayableException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
@UnstableApi
class LoginRequiredException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
@UnstableApi
class VideoIdMismatchException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
@UnstableApi
class PlayableFormatNonSupported : PlaybackException(null, null, ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
@UnstableApi
class NoInternetException : PlaybackException(null, null, ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
@UnstableApi
class TimeoutException : PlaybackException(null, null, ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
@UnstableApi
class UnknownException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
@UnstableApi
class FakeException : PlaybackException(null, null, ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)

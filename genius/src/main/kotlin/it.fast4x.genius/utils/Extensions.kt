package it.fast4x.genius.utils

import kotlinx.coroutines.CancellationException

internal fun <T> Result<T>.recoverIfCancelled(): Result<T>? {
    return when (exceptionOrNull()) {
        is CancellationException -> null
        else -> this
    }
}
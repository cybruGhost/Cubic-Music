package app.it.fast4x.rimusic.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackContextInfo(
    val source: String = "",
    val detail: String = ""
)

object PlaybackContextStore {
    private val _info = MutableStateFlow(PlaybackContextInfo())
    val info: StateFlow<PlaybackContextInfo> = _info.asStateFlow()

    fun set(source: String, detail: String = "") {
        _info.value = PlaybackContextInfo(
            source = source.trim(),
            detail = detail.trim()
        )
    }

    fun clear() {
        _info.value = PlaybackContextInfo()
    }
}

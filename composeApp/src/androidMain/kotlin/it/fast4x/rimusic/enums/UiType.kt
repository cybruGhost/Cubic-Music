package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import it.fast4x.rimusic.utils.UiTypeKey
import it.fast4x.rimusic.utils.rememberPreference
import me.knighthat.enums.TextView

enum class UiType: TextView {
    RiMusic,
    ViMusic;

    companion object {

        @Composable
        fun current(): UiType = rememberPreference( UiTypeKey, RiMusic ).value
    }

    override val text: String
        @Composable
        get() = when (this) {
            RiMusic -> "Cubic-Music"
            ViMusic -> this.name
        }

    @Composable
    fun isCurrent(): Boolean = current() == this

    @Composable
    fun isNotCurrent(): Boolean = !isCurrent()
}
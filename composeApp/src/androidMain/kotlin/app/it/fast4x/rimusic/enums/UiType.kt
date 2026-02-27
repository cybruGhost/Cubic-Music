package app.it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import app.it.fast4x.rimusic.utils.UiTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.me.knighthat.enums.TextView

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
            ViMusic -> "Jennie"
        }

    @Composable
    fun isCurrent(): Boolean = current() == this

    @Composable
    fun isNotCurrent(): Boolean = !isCurrent()
}
package app.it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import app.kreate.android.me.knighthat.enums.TextView

enum class WallpaperType: TextView {
    Home,
    Lockscreen,
    Both;

    override val text: String
        @Composable
        get() = this.name
}
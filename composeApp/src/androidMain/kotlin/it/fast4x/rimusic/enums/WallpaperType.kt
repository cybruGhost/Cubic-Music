package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

enum class WallpaperType: TextView {
    Home,
    Lockscreen,
    Both;

    override val text: String
        @Composable
        get() = this.name
}
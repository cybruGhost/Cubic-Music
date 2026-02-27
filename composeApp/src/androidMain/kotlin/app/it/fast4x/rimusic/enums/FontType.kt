package app.it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import app.kreate.android.me.knighthat.enums.TextView

enum class FontType: TextView {
    Rubik,
    Poppins;

    override val text: String
        @Composable
        get() = this.name
}
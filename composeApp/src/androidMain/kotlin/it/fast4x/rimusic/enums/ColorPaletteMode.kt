package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import me.knighthat.enums.TextView

enum class ColorPaletteMode(
    @field:StringRes override val textId: Int
): TextView {
    Light( R.string._light ),
    Dark( R.string.dark ),
    System( R.string.theme_mode ),
    PitchBlack( R.string.theme_mode_pitch_black );
}

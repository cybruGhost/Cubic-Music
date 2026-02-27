package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class PlayerPlayButtonType(
    val height: Int,
    val width: Int,
    @field:StringRes override val textId: Int
): TextView {

    Disabled( 60, 60, R.string.vt_disabled ),

    Default( 60, 60, R.string._default ),

    Rectangular( 70, 110, R.string.rectangular ),

    CircularRibbed( 100, 100, R.string.circular_ribbed ),

    Square( 80, 80, R.string.square );
}
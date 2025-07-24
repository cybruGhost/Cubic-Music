package app.kreate.android.constant

import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

enum class Speed(
    private val _text: String,
    val value: Float
): TextView {

    SLOW_QUARTER( "0.25x", .25f ),

    SLOW_HALF( "0.5x", .5f ),

    SLOW_TWO_THIRD( "0.75x", .75f ),

    NORMAL( "1x", 1f ),

    FAST_HALF( "1.5x", 1.5f ),

    FAST_TWO( "2x", 2f ),

    FAST_THREE( "3x", 3f ),

    FAST_FOUR( "4x", 4f ),

    FAST_FIVE( "5x", 5f ),

    FAST_TEN( "10x", 10f );

    override val text: String
        @Composable
        get() = this._text
}
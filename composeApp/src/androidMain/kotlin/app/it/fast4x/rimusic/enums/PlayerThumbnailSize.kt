package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class PlayerThumbnailSize(
    val size: Int,
    @field:StringRes override val textId: Int
): TextView {

    Small( 90, R.string.small ),

    Medium( 55, R.string.medium ),

    Big( 30, R.string.big ),

    Biggest( 20, R.string.biggest ),

    Expanded( 0, R.string.expanded );
}

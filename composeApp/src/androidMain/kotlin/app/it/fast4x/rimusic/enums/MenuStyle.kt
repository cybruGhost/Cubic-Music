package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class MenuStyle(
    @field:StringRes override val textId: Int
): TextView {

    List( R.string.style_list ),
    Grid( R.string.style_grid );
}

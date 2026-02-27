package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class AlbumsType(
    @field:StringRes override val textId: Int
): TextView {

    Favorites( R.string.favorites ),
    Library( R.string.library );
}
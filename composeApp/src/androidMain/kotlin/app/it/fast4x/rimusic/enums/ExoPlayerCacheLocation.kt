package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class ExoPlayerCacheLocation(
    @field:StringRes override val textId: Int
): TextView {

    System( R.string.cache_location_system ),
    Private( R.string.cache_location_private );
}
package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class ThumbnailCoverType(
    @field:StringRes override val textId: Int
): TextView {

    Vinyl (R.string.cover_type_vinyl ),
    CD( R.string.cover_type_cd ),
    CDwithCover( R.string.cover_type_cd_with_cover );
}
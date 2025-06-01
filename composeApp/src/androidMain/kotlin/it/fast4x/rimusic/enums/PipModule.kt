package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import me.knighthat.enums.TextView

enum class PipModule(
    @field:StringRes override val textId: Int
): TextView {

    Cover( R.string.pipmodule_cover )
}
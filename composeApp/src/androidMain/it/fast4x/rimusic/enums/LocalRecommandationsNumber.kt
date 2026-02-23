package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import me.knighthat.enums.TextView
import app.kreate.android.R

enum class LocalRecommandationsNumber(
    @field:StringRes override val textId: Int,
    val value: Int
) : TextView {
    OneQ(R.string.quick_selection, 1),
    TwoQ(R.string.quick_selection, 2),
    ThreeQ(R.string.quick_selection, 3),
    FourQ(R.string.quick_selection, 4),
    FiveQ(R.string.quick_selection, 5),
    SixQ(R.string.quick_selection, 6);
}
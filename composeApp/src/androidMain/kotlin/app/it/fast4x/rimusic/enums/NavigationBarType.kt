package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import app.kreate.android.R
import app.it.fast4x.rimusic.utils.navigationBarTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.me.knighthat.enums.TextView


enum class NavigationBarType(
    @field:StringRes override val textId: Int
): TextView {

    IconAndText( R.string.icon_and_text ),
    IconOnly( R.string.only_icon );

    companion object {

        @Composable
        fun current(): NavigationBarType = rememberPreference( navigationBarTypeKey, NavigationBarType.IconAndText ).value
    }

    @Composable
    fun isCurrent(): Boolean = current() == this
}
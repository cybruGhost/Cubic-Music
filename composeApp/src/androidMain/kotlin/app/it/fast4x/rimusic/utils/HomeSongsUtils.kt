package app.it.fast4x.rimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.DynamicColor
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon

@Composable
fun randomSort(): MenuIcon = object: MenuIcon, DynamicColor, Descriptive {

    override var isFirstColor: Boolean by rememberPreference( autoShuffleKey, false )
    override val iconId: Int = R.drawable.random
    override val messageId: Int = R.string.random_sorting
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override fun onShortClick() { isFirstColor = !isFirstColor }
}
package app.it.fast4x.rimusic.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.MenuState

interface Menu {

    val menuState: MenuState

    var menuStyle:MenuStyle

    @Composable
    fun ListMenu()

    @Composable
    fun GridMenu()

    @Composable
    fun MenuComponent()

    fun openMenu() = menuState.display { MenuComponent() }

    fun closeMenu() = menuState.hide()
}
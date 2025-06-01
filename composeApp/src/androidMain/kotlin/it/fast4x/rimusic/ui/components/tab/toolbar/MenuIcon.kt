package it.fast4x.rimusic.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import me.knighthat.component.menu.GridMenu
import me.knighthat.component.menu.ListMenu

interface MenuIcon: Icon {

    @get:Composable
    val menuIconTitle: String

    @Composable
    fun GridMenuItem() = GridMenu.Entry( menuIconTitle, { ToolBarButton() }, modifier, isEnabled, ::onShortClick )

    @Composable
    fun ListMenuItem() = ListMenu.Entry( menuIconTitle, { ToolBarButton() }, modifier, isEnabled, ::onShortClick )
}
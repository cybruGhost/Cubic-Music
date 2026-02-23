package it.fast4x.rimusic.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import me.knighthat.component.menu.GridMenu
import me.knighthat.component.menu.ListMenu

interface MenuIcon: Icon {

    @get:Composable
    val menuIconTitle: String

    @Composable
    fun GridMenuItem() = GridMenu.Entry(
        text = menuIconTitle,
        icon = { ToolBarButton() },
        modifier = modifier,
        enabled = isEnabled,
        onClick = ::onShortClick,
        onLongClick = if (this is Clickable) ::onLongClick else { {} }
    )

    @Composable
    fun ListMenuItem() = ListMenu.Entry(
        text = menuIconTitle,
        icon = { ToolBarButton() },
        modifier = modifier,
        enabled = isEnabled,
        onClick = ::onShortClick,
        onLongClick = if (this is Clickable) ::onLongClick else { {} }
    )
}
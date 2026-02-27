package app.kreate.android.me.knighthat.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.Drawable
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.kreate.android.me.knighthat.component.menu.GridMenu
import app.kreate.android.me.knighthat.component.menu.ListMenu
import app.kreate.android.me.knighthat.enums.TextView

open class Sort<T: Enum<T>> (
    override val menuState: MenuState,
    sortByState: MutableState<T>,
    sortOrderState: MutableState<SortOrder>,
    styleState: MutableState<MenuStyle>
): MenuIcon, Clickable, Menu {

    companion object {
        @Composable
        inline operator fun<reified T: Enum<T>> invoke(
            sortByPrefKey: Preference.Key<T>,
            sortOrderPrefKey: Preference.Key<SortOrder>
        ) = Sort(
            LocalMenuState.current,
            Preference.remember( sortByPrefKey ),
            Preference.remember( sortOrderPrefKey ),
            rememberPreference( menuStyleKey, MenuStyle.List )
        )
    }

    open val arrowDirection: State<Float>
        @Composable
        get() = animateFloatAsState(
            targetValue = sortOrder.rotationZ,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing),
            label = ""
        )
    override val iconId: Int = R.drawable.arrow_up
    override val menuIconTitle: String
        @Composable
        // TODO: Add string "sort_item"
        get() = stringResource( R.string.sorting_order )

    open var sortBy: T by sortByState
    open var sortOrder: SortOrder by sortOrderState
    override var menuStyle: MenuStyle by styleState

    /** Flip oder. */
    override fun onShortClick() { sortOrder = !sortOrder }

    override fun onLongClick() = openMenu()

    @Composable
    override fun ListMenu() = ListMenu.Menu {
        // Ignore error "Cannot access 'java. lang. constant. Constable' which is a supertype of 'java. lang. Class'"
        sortBy.javaClass.enumConstants.forEach {
            ListMenu.Entry(
                text = if (it is TextView) it.text else it.name,
                icon = {
                    Icon(
                        painter =
                            if( it is Drawable )
                                it.icon
                            else
                                painterResource( R.drawable.close ),
                        contentDescription = it.name,
                        tint = colorPalette().text,
                        modifier = Modifier.size( TabToolBar.TOOLBAR_ICON_SIZE )
                    )
                },
                onClick = {
                    menuState.hide()
                    sortBy = it
                }
            )
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu {
        items(
            // Ignore error "Cannot access 'java. lang. constant. Constable' which is a supertype of 'java. lang. Class'"
            items = sortBy.javaClass.enumConstants,
            key = Enum<T>::ordinal
        ) {
            GridMenu.Entry(
                text = if (it is TextView) it.text else it.name,
                icon = {
                    Icon(
                        painter =
                            if( it is Drawable )
                                it.icon
                            else
                                painterResource( R.drawable.close ),
                        contentDescription = it.name,
                        tint = colorPalette().text,
                        modifier = Modifier.size( TabToolBar.TOOLBAR_ICON_SIZE )
                    )
                },
                onClick = {
                    menuState.hide()
                    sortBy = it
                }
            )
        }
    }

    @Composable
    override fun MenuComponent() =
        app.it.fast4x.rimusic.ui.components.themed.Menu {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding( end = 12.dp )
            ) {
                BasicText(
                    text = menuIconTitle,
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 24.dp
                    )
                )
            }

            if( menuStyle == MenuStyle.List )
                ListMenu()
            else
                GridMenu()
        }

    @Composable
    override fun ToolBarButton() {
        val animatedArrow by arrowDirection

        TabToolBar.Icon(
            icon,
            color,
            sizeDp,
            isEnabled,
            this.modifier.graphicsLayer { rotationZ = animatedArrow },
            this::onShortClick,
            this::onLongClick
        )
    }
}
package app.kreate.android.me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon

/**
 * Behaves exactly like a [MutableList] but with extra functions on top.
 *
 * When [isActive] is set to `false`, all selected items will be cleared
 */
class ItemSelector<E> private constructor(
    private val menuState: MenuState,
    private val activeState: MutableState<Boolean>,
): AbstractMutableList<E>(), MenuIcon, Descriptive {

    companion object {
        @JvmStatic
        @Composable
        operator fun <T> invoke() = ItemSelector<T>(
            LocalMenuState.current,
            // Use remember to let list cleared when screen rotates
            remember { mutableStateOf(false) }
        )
    }

    private val selected: ArrayList<E> = ArrayList()

    var isActive: Boolean = activeState.value
        set(value) {
            activeState.value = value
            field = value

            if( !value )
                selected.clear()
        }

    override val iconId: Int
        get() = if( isActive ) R.drawable.checked_filled else R.drawable.unchecked_outline
    override val messageId: Int
        get() = if ( isActive ) R.string.item_deselect else R.string.item_select
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )
    override val size: Int
        // Use getter to make sure the value is up-to-date
        get() = selected.size

    override fun onShortClick() {
        menuState.hide()
        isActive = !isActive
    }

    override fun get(index: Int): E = selected[index]

    override fun removeAt(index: Int): E = selected.removeAt( index )

    override fun add(index: Int, element: E) = selected.add( index, element )

    override fun set(index: Int, element: E): E = selected.set( index, element )
}
package app.it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties // ADD THIS IMPORT
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.colorPalette

class DropdownMenu(
    val expanded: Boolean,
    val containerColor: Color = Color.Transparent,
    val modifier: Modifier = Modifier,
    val onDismissRequest: () -> Unit,
    val properties: PopupProperties = PopupProperties(focusable = true)
) {
    private val _components: MutableList<@Composable () -> Unit> = mutableListOf()

    @Composable
    fun components() = remember { _components }

    @Composable
    fun add(item: Item) = _components.add { item.Draw() }

    @Composable
    fun add(component: @Composable () -> Unit) = _components.add(component)

    @Composable
    fun Draw() {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            containerColor = containerColor,
            modifier = modifier,
            properties = properties,
            shape = RoundedCornerShape(16.dp),
            content = { components().forEach { it() } }
        )
    }

    class Item(
        val iconId: Int,
        val textId: Int,
        val size: Dp = 24.dp,
        val padding: Dp = Dp.Hairline,
        val colors: MenuItemColors? = null,
        val modifier: Modifier = Modifier,
        val iconType: IconType = IconType.Vector,
        val onClick: () -> Unit
    ) {

        companion object {
            @Composable
            fun colors(): MenuItemColors {
                return MenuItemColors(
                    leadingIconColor = colorPalette().favoritesIcon,
                    trailingIconColor = colorPalette().favoritesIcon,
                    textColor = colorPalette().textSecondary,
                    disabledTextColor = colorPalette().text,
                    disabledLeadingIconColor = colorPalette().text,
                    disabledTrailingIconColor = colorPalette().text,
                )
            }
        }

        @Composable
        fun Draw() {
            val icon: @Composable () -> Unit = {
                when (iconType) {
                    IconType.Vector -> {
                        Icon(
                            painter = painterResource(iconId),
                            contentDescription = null,
                            modifier = modifier.size(size),
                            tint = (colors ?: colors()).leadingIconColor
                        )
                    }
                    IconType.Image -> {
                        Image(
                            painter = painterResource(iconId),
                            contentDescription = null,
                            modifier = modifier.size(size)
                        )
                    }
                }
            }

            DropdownMenuItem(
                enabled = true,
                colors = colors ?: colors(),
                text = { Text(stringResource(textId)) },
                leadingIcon = icon,
                onClick = onClick
            )
        }
    }
    
    enum class IconType {
        Vector,
        Image
    }
}
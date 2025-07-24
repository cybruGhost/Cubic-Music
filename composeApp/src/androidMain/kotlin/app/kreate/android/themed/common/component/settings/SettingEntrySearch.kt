package app.kreate.android.themed.common.component.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kreate.android.themed.common.component.AbstractSearch
import app.kreate.android.themed.rimusic.component.settings.RiMusicAnimatedHeader
import app.kreate.android.themed.vimusic.component.settings.ViMusicAnimatedHeader
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.tab.toolbar.Icon
import it.fast4x.rimusic.utils.bold
import kotlinx.coroutines.launch

class SettingEntrySearch(
    private val scrollableState: ScrollableState,
    @StringRes private val titleId: Int,
    @DrawableRes override val iconId: Int
): AbstractSearch(), Icon {

    @Composable
    override fun DecorationBox( innerTextField: @Composable () -> Unit ) =
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy( DECO_BOX_ITEM_SPACING.dp ),
            modifier = Modifier.fillMaxWidth()
                               .horizontalScroll( rememberScrollState() )
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight( 1f )
            ) {
                Placeholder()

                // Actual text from user
                innerTextField()
            }

            ClearSearchButton()
        }

    @Composable
    fun HeaderIcon( modifier: Modifier ) =
        Icon(
            painter = painterResource( iconId ),
            contentDescription = stringResource( titleId ),
            tint = colorPalette().accent,
            modifier = modifier.size( 22.dp )
                               .clickable( onClick = ::onShortClick )
        )

    @Composable
    fun HeaderText( textAlign: TextAlign, modifier: Modifier = Modifier ) {
        val coroutineScope = rememberCoroutineScope()

        BasicText(
            text = stringResource( titleId ),
            style = TextStyle(
                fontSize = typography().xxl.bold.fontSize,
                fontWeight = typography().xxl.bold.fontWeight,
                color = colorPalette().text,
                textAlign = textAlign
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.clickable {
                coroutineScope.launch {
                    if( scrollableState is LazyListState )
                        scrollableState.animateScrollToItem( 0, 0 )
                    else if( scrollableState is LazyGridState )
                        scrollableState.animateScrollToItem( 0, 0 )
                }
            }
        )
    }

    override fun onShortClick() {
        isVisible = !isVisible
        isFocused = isVisible
    }

    @Composable
    override fun ToolBarButton() {
        // Scroll to top every time search value changes
        LaunchedEffect( input ) {
            if( scrollableState is LazyGridState )
                scrollableState.requestScrollToItem( 0, 0 )
            else if( scrollableState is LazyListState )
                scrollableState.requestScrollToItem( 0, 0 )
        }

        when( UiType.current() ) {
            UiType.RiMusic -> RiMusicAnimatedHeader()
            UiType.ViMusic -> ViMusicAnimatedHeader()
        }
    }
}
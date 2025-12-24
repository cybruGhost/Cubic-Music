package me.knighthat.component.song

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.enums.MaxTopPlaylistItems
import it.fast4x.rimusic.enums.MenuStyle
import it.fast4x.rimusic.enums.StatisticsType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MenuState
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.components.themed.Menu
import it.fast4x.rimusic.ui.components.themed.MenuEntry
import it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import it.fast4x.rimusic.utils.Preference
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold

class PeriodSelector private constructor(
    override val menuState: MenuState,
    periodState: MutableState<StatisticsType>,
    styleState: MutableState<MenuStyle>,
): MenuIcon, Descriptive, Menu {

    companion object {
        @Composable
        operator fun invoke( prefKey: Preference.Key<StatisticsType> ): PeriodSelector =
            PeriodSelector(
                LocalMenuState.current,
                Preference.remember( prefKey ),
                rememberPreference( menuStyleKey, MenuStyle.List )
            )
    }

    var period: StatisticsType by periodState

    override val iconId: Int = period.iconId
    override val messageId: Int = R.string.statistics
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var menuStyle: MenuStyle by styleState

    fun onDismiss( period: StatisticsType ) {
        this.period = period
        menuState.hide()
    }

    override fun onShortClick() = openMenu()

    @Composable
    override fun ListMenu() { /* Does nothing */ }

    @Composable
    override fun GridMenu() { /* Does nothing */ }

    @Composable
    override fun MenuComponent() {
        val size by rememberPreference( MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10` )

        Menu {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .padding(end = 12.dp)
            ) {
                BasicText(
                    text = stringResource( R.string.header_view_top_of, size ),
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 24.dp
                    )
                )
            }

            Spacer( Modifier.height( 8.dp ) )

            StatisticsType.entries.forEach {
                MenuEntry(
                    icon = R.drawable.time,
                    text = it.text,
                    onClick = {
                        onDismiss( it )
                    }
                )
            }
        }
    }
}
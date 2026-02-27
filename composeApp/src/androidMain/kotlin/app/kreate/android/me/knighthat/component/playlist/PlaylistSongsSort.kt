package app.kreate.android.me.knighthat.component.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.PlaylistSongSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.utils.Preference
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.kreate.android.me.knighthat.component.Sort

class PlaylistSongsSort private constructor(
    menuState: MenuState,
    sortByState: MutableState<PlaylistSongSortBy>,
    sortOrderState: MutableState<SortOrder>,
    styleState: MutableState<MenuStyle>
): Sort<PlaylistSongSortBy>(menuState, sortByState, sortOrderState, styleState) {

    companion object {
        @Composable
        operator fun invoke() = PlaylistSongsSort(
            LocalMenuState.current,
            Preference.remember( Preference.PLAYLIST_SONGS_SORT_BY ),
            Preference.remember( Preference.PLAYLIST_SONGS_SORT_ORDER ),
            rememberPreference( menuStyleKey, MenuStyle.List )
        )
    }

    override fun onLongClick() { /* Does nothing */ }

    @Composable
    override fun ToolBarButton() {
        super.ToolBarButton()

        BasicText(
            text = this.sortBy.text,
            style = typography().s.semiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { super.onLongClick() }
        )
    }
}
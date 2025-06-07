package it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.MONTHLY_PREFIX
import it.fast4x.rimusic.PINNED_PREFIX
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.MenuStyle
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MenuState
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import me.knighthat.component.playlist.NewPlaylistDialog
import me.knighthat.component.tab.Search
import me.knighthat.utils.Toaster
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import it.fast4x.rimusic.PIPED_PREFIX

class PlaylistsMenu private constructor(
    private val navController: NavController,
    private val mediaItems: (PlaylistPreview) -> List<MediaItem>,
    private val onFailure: (Throwable, PlaylistPreview) -> Unit,
    private val finalAction: (PlaylistPreview) -> Unit,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
): MenuIcon, Descriptive, Menu {

    companion object {
        @JvmStatic
        @Composable
        fun init(
            navController: NavController,
            mediaItems: (PlaylistPreview) -> List<MediaItem>,
            onFailure: (Throwable, PlaylistPreview) -> Unit,
            finalAction: (PlaylistPreview) -> Unit
        ) = PlaylistsMenu(
            navController,
            mediaItems,
            onFailure,
            finalAction,
            LocalMenuState.current,
            rememberPreference( menuStyleKey, MenuStyle.List )
        )
    }

    override val iconId: Int = R.drawable.add_in_playlist
    override val messageId: Int = R.string.add_to_playlist
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var menuStyle: MenuStyle by styleState

    private fun onAdd( preview: PlaylistPreview ) = Database.asyncTransaction {
        try {
            mapIgnore(preview.playlist, *mediaItems(preview).toTypedArray())
            Toaster.done()
        } catch (e: Throwable) {
            onFailure(e, preview)
        } finally {
            finalAction(preview)
        }
    }

    @Composable
    private fun PlaylistCard( playlistPreview: PlaylistPreview ) {
        val playlist = playlistPreview.playlist

        MenuEntry(
            icon = R.drawable.add_in_playlist,
            text = playlist.name.substringAfter( PINNED_PREFIX ),
            secondaryText = "${playlistPreview.songCount} ${stringResource( R.string.songs )}",
            onClick = {
                onAdd( playlistPreview )
            },
            trailingContent = {
                if (playlistPreview.playlist.name.startsWith(PIPED_PREFIX, 0, true))
                    Image(
                        painter = painterResource(R.drawable.piped_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().red),
                        modifier = Modifier.size(18.dp)
                    )
                if (playlistPreview.playlist.isYoutubePlaylist) {
                    Image(
                        painter = painterResource(R.drawable.ytmusic),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            Color.Red.copy(0.75f).compositeOver(Color.White)
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    icon = R.drawable.open,
                    color = colorPalette().text,
                    onClick = {
                        menuState.hide()
                        navController.navigate(route = "${NavRoutes.localPlaylist.name}/${playlist.id}")
                    },
                    modifier = Modifier.size( 24.dp )
                )
            }
        )
    }

    override fun onShortClick() {
        menuState.hide()
        openMenu()
    }

    @Composable
    override fun ListMenu() { /* Does nothing */ }

    @Composable
    override fun GridMenu() { /* Does nothing */ }

    @Composable
    override fun MenuComponent() {
        val playlistPreviews by remember {
            Database.playlistTable.sortPreviewsByName()
        }.collectAsState( emptyList(), Dispatchers.IO )

        val pinnedPlaylists = playlistPreviews.filter {
            it.playlist.name.startsWith(PINNED_PREFIX, 0, true)
        }
        val unpinnedPlaylists = playlistPreviews.filter {
            !it.playlist.name.startsWith(PINNED_PREFIX, 0, true) &&
                    !it.playlist.name.startsWith(MONTHLY_PREFIX, 0, true)
        }

        val search = Search()
        val filteredPinnedPlaylists = pinnedPlaylists.filter { it.playlist.name.contains(search.inputValue, true) }
        val filteredUnpinnedPlaylists = unpinnedPlaylists.filter { it.playlist.name.contains(search.inputValue, true) }

        val newPlaylistButton = NewPlaylistDialog()
        newPlaylistButton.Render()

        Menu {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = ::onShortClick,
                    icon = R.drawable.chevron_back,
                    color = colorPalette().textSecondary,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .size(20.dp)
                )
                IconButton(
                    onClick = { search.isVisible = !search.isVisible },
                    icon = R.drawable.search_circle,
                    color = colorPalette().favoritesIcon,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .size(24.dp)
                )
                BasicText(
                    text = stringResource(R.string.playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                newPlaylistButton.ToolBarButton()
            }
            search.SearchBar(this)
            if (filteredPinnedPlaylists.isNotEmpty()) {
                BasicText(
                    text = stringResource(R.string.pinned_playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 5.dp)
                )

                filteredPinnedPlaylists.forEach { PlaylistCard(it) }
            }

            if (filteredUnpinnedPlaylists.isNotEmpty()) {
                BasicText(
                    text = stringResource(R.string.playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 5.dp)
                )

                filteredUnpinnedPlaylists.forEach { PlaylistCard(it) }
            }
        }
    }
}
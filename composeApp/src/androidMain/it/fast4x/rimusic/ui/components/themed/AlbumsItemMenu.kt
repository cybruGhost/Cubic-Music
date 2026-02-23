package it.fast4x.rimusic.ui.components.themed

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.enums.MenuStyle
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.rememberPreference
import me.knighthat.component.menu.album.AlbumItemMenu
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.playlistSortByKey
import it.fast4x.rimusic.utils.playlistSortOrderKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import me.knighthat.component.tab.Search
import me.knighthat.utils.Toaster

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun AlbumsItemMenu(
    navController: NavController,
    onDismiss: () -> Unit = {},
    onSelectUnselect: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    onUncheck: (() -> Unit)? = null,
    onChangeAlbumTitle: (() -> Unit)? = null,
    onChangeAlbumAuthors: (() -> Unit)? = null,
    onChangeAlbumCover: (() -> Unit)? = null,
    onDownloadAlbumCover: (() -> Unit)? = null,
    album: Album,
    songs: List<Song> = emptyList(),
    modifier: Modifier = Modifier,
    onPlayNext: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
    onAddToPlaylist: ((PlaylistPreview) -> Unit)? = null,
    onGoToPlaylist: ((Long) -> Unit)? = null,
    onAddToFavourites: (() -> Unit)? = null,
    disableScrollingText: Boolean
) {
    val binder = it.fast4x.rimusic.LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val songs by remember(album.id) {
        Database.songAlbumMapTable
            .allSongsOf(album.id)
            .distinctUntilChanged()
    }.collectAsState(emptyList(), Dispatchers.IO)

    AlbumItemMenu(
        navController = navController,
        album = album,
        songs = songs,
        binder = binder,
        onTitleChange = { newTitle ->
            Database.asyncTransaction {
                albumTable.updateTitle(album.id, newTitle)
            }
        },
        onAuthorsChange = { newAuthors ->
            Database.asyncTransaction {
                albumTable.updateAuthors(album.id, newAuthors)
            }
        },
        onCoverChange = { newCoverUrl ->
            Database.asyncTransaction {
                albumTable.updateCover(album.id, newCoverUrl)
            }
        }
    ).MenuComponent()
}
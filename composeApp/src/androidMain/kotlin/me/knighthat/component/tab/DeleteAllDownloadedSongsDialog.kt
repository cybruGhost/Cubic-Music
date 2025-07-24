package me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.utils.asMediaItem
import me.knighthat.component.MediaDownloadDialog

@UnstableApi
class DeleteAllDownloadedSongsDialog(
    activeState: MutableState<Boolean>,
    getSongs: () -> List<Song>,
    binder: PlayerServiceModern.Binder?
) : MediaDownloadDialog(activeState, getSongs, binder), MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke( getSongs: () -> List<Song> ) =
            DeleteAllDownloadedSongsDialog(
                remember { mutableStateOf(false) },
                getSongs,
                LocalPlayerServiceBinder.current
            )
    }

    override val messageId: Int = R.string.info_remove_all_downloaded_songs
    override val iconId: Int = R.drawable.download
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.do_you_really_want_to_delete_download)
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    // Both [ConfirmDialog] and [Descriptive] require this function,
    // so it must be explicitly stated here to not confuse the compiler
    override fun onShortClick() = super.onShortClick()

    override fun onAction( media: Song ) =
        MyDownloadHelper.removeDownload( appContext(), media.asMediaItem )
}
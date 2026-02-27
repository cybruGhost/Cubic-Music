package app.kreate.android.me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.asMediaItem
import app.cubic.android.core.network.isNetworkAvailable
import app.kreate.android.me.knighthat.component.MediaDownloadDialog

@UnstableApi
class DownloadAllSongsDialog(
    activeState: MutableState<Boolean>,
    getSongs: () -> List<Song>,
    binder: PlayerServiceModern.Binder?
) : MediaDownloadDialog(activeState, getSongs, binder), MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke(
            getSongs: () -> List<Song>
        ) = DownloadAllSongsDialog(
            remember { mutableStateOf(false) },
            getSongs,
            LocalPlayerServiceBinder.current
        )
    }

    override val messageId: Int = R.string.info_download_all_songs
    override val iconId: Int = R.drawable.downloaded
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.do_you_really_want_to_download_all )
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.download )

    // Both [Confirm] and [Descriptive] require this function,
    // so it must be explicitly stated here to not confuse the compiler
    override fun onShortClick() = super.onShortClick()

    override fun onAction( media: Song ) {
        // Starts download only when network is available
        if( appContext().isNetworkAvailable )
            MyDownloadHelper.addDownload( appContext(), media.asMediaItem )
    }
}


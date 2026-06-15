package app.kreate.android.me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.cubic.android.core.network.isNetworkAvailable
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.asMediaItem
import app.kreate.android.R
import app.kreate.android.me.knighthat.component.MediaDownloadDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class DownloadAllSongsDialog(
    activeState: MutableState<Boolean>,
    getSongs: () -> List<Song>,
    private val activeBinder: PlayerServiceModern.Binder?,
    private val redownloadExisting: Boolean = false,
    private val titleId: Int = R.string.do_you_really_want_to_download_all,
    private val menuTitleId: Int = R.string.download,
    private val messageTitleId: Int = R.string.info_download_all_songs,
) : MediaDownloadDialog(activeState, getSongs, activeBinder), MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke(
            getSongs: () -> List<Song>,
            redownloadExisting: Boolean = false,
            titleId: Int = R.string.do_you_really_want_to_download_all,
            menuTitleId: Int = R.string.download,
            messageTitleId: Int = R.string.info_download_all_songs,
        ) = DownloadAllSongsDialog(
            remember { mutableStateOf(false) },
            getSongs,
            LocalPlayerServiceBinder.current,
            redownloadExisting,
            titleId,
            menuTitleId,
            messageTitleId
        )
    }

    override val messageId: Int = messageTitleId
    override val iconId: Int = R.drawable.downloaded

    override val dialogTitle: String
        @Composable
        get() = stringResource(titleId)

    override val menuIconTitle: String
        @Composable
        get() = stringResource(menuTitleId)

    override fun onShortClick() = super.onShortClick()

    override fun onConfirm() {
        val songsToDownload = getSongs()
            .distinctBy(Song::id)
            .filterNot(Song::isLocal)
            .let { songs ->
                if (redownloadExisting) songs else songs.filterNot { song -> MyDownloadHelper.isSongDownloaded(song.id) }
            }

        if (songsToDownload.isEmpty()) {
            onDismiss()
            return
        }

        MyDownloadHelper.startBulkDownloadSession(songsToDownload.map(Song::id))
        onDismiss()

        CoroutineScope(Dispatchers.IO).launch {
            if (!appContext().isNetworkAvailable) return@launch

            songsToDownload.forEachIndexed { index, song ->
                runCatching { activeBinder?.cache?.removeResource(song.id) }
                Database.asyncTransaction {
                    formatTable.deleteBySongId(song.id)
                }

                withContext(Dispatchers.Main) {
                    MyDownloadHelper.addDownload(appContext(), song.asMediaItem)
                }

                if ((index + 1) % 3 == 0) delay(150)
            }
        }
    }

    override fun onAction(media: Song) {
        if (appContext().isNetworkAvailable) {
            MyDownloadHelper.addDownload(appContext(), media.asMediaItem)
        }
    }
}

package app.kreate.android.me.knighthat.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.tab.toolbar.ConfirmDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
abstract class MediaDownloadDialog(
    activeState: MutableState<Boolean>,
    val getSongs: () -> List<Song>,
    private val binder: PlayerServiceModern.Binder?,
): ConfirmDialog {

    override var isActive: Boolean by activeState

    abstract fun onAction( media: Song )

    override fun onConfirm() {
        val songs = getSongs()
        onDismiss()

        CoroutineScope(Dispatchers.IO).launch {
            songs.forEach { song ->
                // binder has to be non-null for remove from cache to work
                val activeBinder = binder ?: return@launch
                runCatching { activeBinder.cache.removeResource(song.id) }

                Database.asyncTransaction {
                    formatTable.deleteBySongId(song.id)
                }

                onAction(song)
            }
        }
    }
}

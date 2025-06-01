package me.knighthat.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.ui.components.tab.toolbar.ConfirmDialog

@UnstableApi
abstract class MediaDownloadDialog(
    activeState: MutableState<Boolean>,
    val getSongs: () -> List<Song>,
    private val binder: PlayerServiceModern.Binder?,
): ConfirmDialog {

    override var isActive: Boolean by activeState

    abstract fun onAction( media: Song )

    override fun onConfirm() {
        getSongs().forEach {
            // binder has to be non-null for remove from cache to work
            if( binder == null ) return
            binder.cache.removeResource( it.id )

            Database.asyncTransaction {
                formatTable.deleteBySongId( it.id )
            }

            onAction( it )
        }

        onDismiss()
    }
}
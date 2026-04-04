package app.kreate.android.me.knighthat.sync

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.innertube.YtMusic.likeVideoOrSong
import it.fast4x.innertube.YtMusic.removelikeVideoOrSong
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import app.it.fast4x.rimusic.utils.isNetworkConnected
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import app.kreate.android.me.knighthat.utils.Toaster

/**
 * Handles YouTube syncing
 */
object YouTubeSync {

    /**
     * Handles toggling like state of a song both locally and remotely.
     *
     * ***Toggle*** only handles 2 out of 3 states of like state:
     * `like` and `neutral`.
     *
     * This function handles these:
     * - Toggle song like state inside database
     * - Download song when liked (if enabled in settings)
     * - Sync like state with YouTube (if applicable)
     *
     * This function must not be called on **main thread**
     */
    @UnstableApi
    suspend fun toggleSongLike( context: Context, mediaItem: MediaItem ) {
        assert( Looper.myLooper() != Looper.getMainLooper() ) {
            "Cannot run YouTubeSync.toggleSongLike on main thread"
        }
        val normalizedMediaId = mediaItem.mediaId.substringAfterLast("/").trim()
        val localMediaId = normalizedMediaId.ifBlank { mediaItem.mediaId.trim() }
        if (localMediaId.isBlank() || localMediaId.startsWith("search:")) {
            Toaster.w(R.string.songs_liked_yt_failed)
            return
        }
        val localMediaItem = if (localMediaId == mediaItem.mediaId) {
            mediaItem
        } else {
            mediaItem.buildUpon().setMediaId(localMediaId).build()
        }

        // TODO: Encapsulate this block in a transaction
        // Always ensure song in database before proceed
        Database.insertIgnore( localMediaItem )
        Database.songTable.toggleLike( localMediaId )

        val likeState = runBlocking {
            Database.songTable.likeState( localMediaId ).first()
        }
        MyDownloadHelper.downloadOnLike( localMediaItem, likeState, context )


        // Stop here if it's not enabled
        if( !isYouTubeSyncEnabled() ) {
            with( mediaItem.mediaMetadata ) {
                // Skip message if title is not present
                if( title == null ) return@with

                val messageId = when( likeState ) {
                    false -> R.string.added_to_dislikes
                    true -> R.string.added_to_favorites
                    null -> R.string.removed_from_dislikes
                }

                Toaster.s( messageId, "\"$title - $artist\"" )
            }

            return
        }

        if( !isNetworkConnected( context ) ) {
            Toaster.noInternet()
            return
        }

        val response =
            if( likeState == true )
                likeVideoOrSong( localMediaId )
            else
                removelikeVideoOrSong( localMediaId )
        val messageId = when {
            likeState == true && response.isSuccess -> R.string.songs_liked_yt
            likeState == true && response.isFailure -> R.string.songs_liked_yt_failed
            likeState == null && response.isSuccess -> R.string.song_unliked_yt
            // likeState == true && response.isFailure
            else                                    -> R.string.songs_unliked_yt_failed
        }
        if( response.isSuccess )
            Toaster.s( messageId )
        else
            Toaster.e( messageId )
    }
}

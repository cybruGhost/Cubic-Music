package it.fast4x.rimusic.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.knighthat.utils.Toaster

data class YouTubeRadio @OptIn(UnstableApi::class) constructor
    (
    private val videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var parameters: String? = null,
    private val isDiscoverEnabled: Boolean = false,
    private val context: Context,
    private val binder: PlayerServiceModern.Binder? = null,
    private val coroutineScope: CoroutineScope
) {
    private var nextContinuation: String? = null

    @OptIn(UnstableApi::class) suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            if (continuation == null) {
                Innertube.nextPage(
                    NextBody(
                        videoId = videoId,
                        playlistId = playlistId,
                        params = parameters,
                        playlistSetVideoId = playlistSetVideoId
                    )
                )?.map { nextResult ->
                    playlistId = nextResult.playlistId
                    parameters = nextResult.params
                    playlistSetVideoId = nextResult.playlistSetVideoId

                    nextResult.itemsPage
                }
            } else {
                Innertube.nextPage(ContinuationBody(continuation = continuation))
            }?.getOrNull()?.let { songsPage ->
                mediaItems = songsPage.items?.map(Innertube.SongItem::asMediaItem)
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }

        }
            //coroutineScope.launch(Dispatchers.Main) {

        fun songsInQueue(mediaId: String): String? {
            var mediaIdFound = false
            runBlocking {
                withContext(Dispatchers.Main) {
                    for (i in 0 until (binder?.player?.mediaItemCount ?: 0) - 1) {
                        if (mediaId == binder?.player?.getMediaItemAt(i)?.mediaId) {
                            mediaIdFound = true
                            return@withContext
                        }
                    }
                }
            }
            if(mediaIdFound){
                return mediaId
            }
            return null
        }


            if (isDiscoverEnabled) {
                val listMediaItems =
                    mediaItems?.filter {
                        Database.songPlaylistMapTable.isMapped( it.mediaId ).first()
                                && Database.songTable.isLiked( it.mediaId ).first()
                                && it.mediaId != songsInQueue( it.mediaId )
                    } ?: emptyList()

                Toaster.s(
                    messageId = R.string.discover_has_been_applied_to_radio,
                    mediaItems?.size?.minus(listMediaItems.size) ?: 0,
                    duration = Toast.LENGTH_SHORT
                )

                mediaItems = listMediaItems
            }

        mediaItems?.distinct()
                  ?.filter {
                      Database.songTable.isLiked( it.mediaId ).first()
                  }
                  ?.also { mediaItems = it }

        return mediaItems ?: emptyList()
    }
}

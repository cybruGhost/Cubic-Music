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

data class YouTubeRadio @OptIn(UnstableApi::class) constructor(
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

    @OptIn(UnstableApi::class)
    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            val result = if (continuation == null) {
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
            }

            result?.getOrNull()?.let { songsPage ->
                mediaItems = songsPage.items?.map(Innertube.SongItem::asMediaItem)
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }
        }

        fun songsInQueue(mediaId: String): String? {
            var mediaIdFound = false
            runBlocking {
                withContext(Dispatchers.Main) {
                    val itemCount = binder?.player?.mediaItemCount ?: 0
                    for (i in 0 until itemCount - 1) {
                        val currentMediaId = binder?.player?.getMediaItemAt(i)?.mediaId
                        if (mediaId == currentMediaId) {
                            mediaIdFound = true
                            return@withContext
                        }
                    }
                }
            }
            return if (mediaIdFound) mediaId else null
        }

        if (isDiscoverEnabled) {
            val filteredMediaItems = mediaItems?.filter { item ->
                val isMapped = Database.songPlaylistMapTable.isMapped(item.mediaId).first()
                val isLiked = Database.songTable.isLiked(item.mediaId).first()
                val notInQueue = item.mediaId != songsInQueue(item.mediaId)
                
                isMapped && isLiked && notInQueue
            } ?: emptyList()

            val removedCount = (mediaItems?.size ?: 0) - filteredMediaItems.size
            if (removedCount > 0) {
                Toaster.s(
                    messageId = R.string.discover_has_been_applied_to_radio,
                    removedCount,
                    duration = Toast.LENGTH_SHORT
                )
            }

            mediaItems = filteredMediaItems
        }

        val finalMediaItems = mediaItems
            ?.distinct()
            ?.filter { item ->
                Database.songTable.isLiked(item.mediaId).first()
            }
            ?: emptyList()

        return finalMediaItems
    }
}

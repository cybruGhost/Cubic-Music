package app.it.fast4x.rimusic.utils

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
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import app.kreate.android.me.knighthat.utils.Toaster

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
                mediaItems = songsPage.items
                    ?.map(Innertube.SongItem::asMediaItem)
                    ?.distinctBy { it.mediaId }
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }
        }

        fun currentQueueIds(): Set<String> {
            var ids = emptySet<String>()
            runBlocking {
                withContext(Dispatchers.Main) {
                    val itemCount = binder?.player?.mediaItemCount ?: 0
                    ids = (0 until itemCount)
                        .mapNotNull { index -> binder?.player?.getMediaItemAt(index)?.mediaId }
                        .toSet()
                }
            }
            return ids
        }

        val queuedIds = currentQueueIds()

        if (isDiscoverEnabled) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val listenedSongIds = Database.eventTable.findSongsMostPlayedBetween(
                from = thirtyDaysAgo,
                limit = 200
            ).first().map { it.id }.toSet()

            val filteredMediaItems = mediaItems?.filter { item ->
                val isMapped = Database.songPlaylistMapTable.isMapped(item.mediaId).first()
                val isLiked = Database.songTable.isLiked(item.mediaId).first()
                val notInQueue = item.mediaId !in queuedIds
                val wasAlreadyListened = item.mediaId in listenedSongIds
                
                !isMapped && !isLiked && !wasAlreadyListened && notInQueue
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
            ?.filter { it.mediaId !in queuedIds }
            ?.distinctBy { it.mediaId }
            ?: emptyList()

        return finalMediaItems
    }
}

package app.it.fast4x.rimusic.utils

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.cubic.android.core.network.isNetworkConnected
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.EXPLICIT_PREFIX
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.service.MyDownloadHelper
import kotlinx.coroutines.flow.first
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

object DiscoverQueueTrigger {
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun request() {
        requests.tryEmit(Unit)
    }

    suspend fun collect(block: () -> Unit) {
        requests.collect { block() }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ApplyDiscoverToQueue() {
    /*   DISCOVER  */
    val discoverIsEnabled by rememberPreference(discoverKey, false)
    if (!discoverIsEnabled) return

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    val player = binder.player

    var queueRevision by remember(player) { mutableIntStateOf(0) }
    var lastAppliedAt by remember(player) { mutableStateOf(0L) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                queueRevision++
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                queueRevision++
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        DiscoverQueueTrigger.collect { queueRevision++ }
    }

    LaunchedEffect(queueRevision, discoverIsEnabled) {
        val now = System.currentTimeMillis()
        if (now - lastAppliedAt < 4_000L) return@LaunchedEffect
        lastAppliedAt = now

        if (!context.isNetworkConnected) {
            Toaster.w(R.string.error_no_internet)
            return@LaunchedEffect
        }

        val currentIndex = player.currentMediaItemIndex
        val queueSnapshot = player.currentTimeline.windows.mapIndexed { index, window ->
            val mediaItem = window.mediaItem
            QueueDiscoverItem(
                index = index,
                mediaId = mediaItem.mediaId,
                normalizedId = mediaItem.mediaId.normalizedDiscoverId(),
                artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            )
        }

        val indicesToRemove = withContext(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val listenedSongIds = Database.eventTable.findSongsMostPlayedBetween(
                from = thirtyDaysAgo,
                limit = 200
            ).first().map { it.id.normalizedDiscoverId() }.toSet()
            val artistCounts = mutableMapOf<String, Int>()
            val downloadedIds = MyDownloadHelper.downloads.value
                .values
                .filter { it.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED }
                .map { it.request.id.normalizedDiscoverId() }
                .toSet()

            queueSnapshot.mapNotNull { item ->
                if (item.index == currentIndex) return@mapNotNull null
                val alreadyPassedInThisQueue = item.index < currentIndex

                val isMappedToAPlaylist =
                    Database.songPlaylistMapTable.isMapped(item.mediaId).first() ||
                        Database.songPlaylistMapTable.isMapped(item.normalizedId).first()
                if (isMappedToAPlaylist) return@mapNotNull null

                val normalizedArtist = item.artist.trim().lowercase()
                val artistRepeatCount = if (normalizedArtist.isBlank()) {
                    0
                } else {
                    artistCounts.merge(normalizedArtist, 1, Int::plus) ?: 1
                }
                val isSameArtistRepeat = normalizedArtist.isNotBlank() && artistRepeatCount > 2
                val isLiked =
                    Database.songTable.isLiked(item.mediaId).first() ||
                        Database.songTable.isLiked(item.normalizedId).first()
                val wasAlreadyListened = item.normalizedId in listenedSongIds
                val isDownloaded =
                    item.normalizedId in downloadedIds ||
                        MyDownloadHelper.isSongDownloaded(item.mediaId) ||
                        MyDownloadHelper.isSongDownloaded(item.normalizedId)

                item.index.takeIf {
                    alreadyPassedInThisQueue ||
                        isLiked ||
                        wasAlreadyListened ||
                        isDownloaded ||
                        isSameArtistRepeat
                }
            }
        }.sortedDescending()

        var removedCount = 0
        indicesToRemove.forEach { index ->
            if (index in 0 until player.mediaItemCount && index != player.currentMediaItemIndex) {
                player.removeMediaItem(index)
                removedCount++
            }
        }

        if (removedCount > 0) {
            Toaster.s(
                R.string.discover_has_been_applied_to_queue,
                removedCount,
                duration = Toast.LENGTH_SHORT
            )
        } else {
            Toaster.s(R.string.discover_queue_no_changes, duration = Toast.LENGTH_SHORT)
        }

        val remainingAhead = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (remainingAhead < 4) {
            val seed = queueSnapshot
                .filter { it.index >= player.currentMediaItemIndex }
                .distinctBy { it.artist.trim().lowercase().ifBlank { it.normalizedId } }
                .shuffled()
                .firstOrNull()
            val seedItem = seed?.index
                ?.takeIf { it in 0 until player.mediaItemCount }
                ?.let(player::getMediaItemAt)
                ?: player.currentMediaItem
            seedItem?.let { binder.startRadio(it, append = true) }
            Toaster.s(R.string.discover_loading_more_songs, duration = Toast.LENGTH_SHORT)
        }
    }

    /*   DISCOVER  */

}

private data class QueueDiscoverItem(
    val index: Int,
    val mediaId: String,
    val normalizedId: String,
    val artist: String
)

private fun String.normalizedDiscoverId(): String =
    removePrefix(EXPLICIT_PREFIX)
        .substringAfterLast("/")
        .substringBefore("?")

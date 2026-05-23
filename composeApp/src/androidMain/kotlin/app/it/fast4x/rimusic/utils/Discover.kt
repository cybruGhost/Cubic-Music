package app.it.fast4x.rimusic.utils

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.cubic.android.core.network.isNetworkConnected
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import kotlinx.coroutines.flow.first
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    LaunchedEffect(queueRevision, discoverIsEnabled) {
        if (!context.isNetworkConnected) {
            Toaster.w(R.string.error_no_internet)
            return@LaunchedEffect
        }

        val currentIndex = player.currentMediaItemIndex
        val queueSnapshot = player.currentTimeline.windows.map { window ->
            window.firstPeriodIndex to window.mediaItem.mediaId
        }

        val indicesToRemove = withContext(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val listenedSongIds = Database.eventTable.findSongsMostPlayedBetween(
                from = thirtyDaysAgo,
                limit = 200
            ).first().map { it.id }.toSet()

            queueSnapshot.mapNotNull { (index, mediaId) ->
                if (index == currentIndex) return@mapNotNull null

                val isMappedToAPlaylist = Database.songPlaylistMapTable.isMapped(mediaId).first()
                val isLiked = Database.songTable.isLiked(mediaId).first()
                val wasAlreadyListened = mediaId in listenedSongIds

                index.takeIf { isMappedToAPlaylist || isLiked || wasAlreadyListened }
            }
        }.sortedDescending()

        indicesToRemove.forEach { index ->
            if (index in 0 until player.mediaItemCount && index != player.currentMediaItemIndex) {
                player.removeMediaItem(index)
            }
        }

        if (indicesToRemove.isNotEmpty()) {
            Toaster.s(
                R.string.discover_has_been_applied_to_queue,
                indicesToRemove.size,
                duration = Toast.LENGTH_SHORT
            )
        }

        val remainingAhead = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (remainingAhead < 4) {
            player.currentMediaItem?.let { binder.startRadio(it, append = true) }
            Toaster.s(R.string.discover_loading_more_songs, duration = Toast.LENGTH_SHORT)
        }
    }

    /*   DISCOVER  */

}

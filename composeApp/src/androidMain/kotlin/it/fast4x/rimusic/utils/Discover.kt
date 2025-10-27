package it.fast4x.rimusic.utils

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import kotlinx.coroutines.flow.first
import me.knighthat.utils.Toaster

@OptIn(UnstableApi::class)
@Composable
fun ApplyDiscoverToQueue() {
    /*   DISCOVER  */
    val discoverIsEnabled by rememberPreference(discoverKey, false)
    if (!discoverIsEnabled) return

    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    val player = binder.player

    var listMediaItemsIndex = remember {
        mutableListOf<Int>()
    }
    val windows by remember {
        mutableStateOf(player.currentTimeline.windows)
    }

    LaunchedEffect(Unit) {
        listMediaItemsIndex.clear()
        
        // Get most played songs - last 30 days
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val mostPlayedSongs = Database.eventTable.findSongsMostPlayedBetween(
            from = thirtyDaysAgo,
            limit = 100 // Get top 100 most played songs
        ).first() // Get the first value from the Flow
        
        // Convert to a list of song IDs for easy lookup
        val mostPlayedSongIds = mostPlayedSongs.map { it.id }.toSet()
        
        windows.forEach { window ->
            if( window.firstPeriodIndex == player.currentMediaItemIndex )
                return@forEach

            val mediaId = window.mediaItem.mediaId
            val isMappedToAPlaylist = Database.songPlaylistMapTable.isMapped( mediaId ).first()
            val isLiked = Database.songTable.isLiked( mediaId ).first()
            
            // Check if this song is in most played list
            val isMostPlayed = mediaId in mostPlayedSongIds

            // Remove if in playlist OR liked OR most played
            if( isMappedToAPlaylist || isLiked || isMostPlayed )
                listMediaItemsIndex.add(window.firstPeriodIndex)

        }.also {
            if (listMediaItemsIndex.isNotEmpty()) {
                val mediacount = listMediaItemsIndex.size - 1
                listMediaItemsIndex.sort()
                for (i in mediacount.downTo(0)) {
                    binder.player.removeMediaItem(listMediaItemsIndex[i])
                }
                Toaster.s(
                    R.string.discover_has_been_applied_to_queue,
                    listMediaItemsIndex.size,
                    duration = Toast.LENGTH_SHORT
                )
                listMediaItemsIndex.clear()
            }
        }

    }

    /*   DISCOVER  */

}
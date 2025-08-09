package it.fast4x.rimusic.utils

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import app.kreate.android.Preferences
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.knighthat.utils.Toaster

@OptIn(UnstableApi::class)
@Composable
fun ApplyDiscoverToQueue() {
    val discoverIsEnabled by Preferences.ENABLE_DISCOVER
    if (!discoverIsEnabled) return

    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val currentWindows = player.currentTimeline.windows
            val itemsToRemove = mutableListOf<Int>()
            val itemsToKeep = mutableListOf<Int>()

            currentWindows.forEachIndexed { index, window ->
                val mediaId = window.mediaItem.mediaId ?: return@forEachIndexed

                val (liked, inPlaylist) = awaitAll(
                    async { Database.songTable.isLiked(mediaId).first() },
                    async { Database.songPlaylistMapTable.isMapped(mediaId).first() }
                )

                // Calculate discovery score (simplified version without playback history)
                val score = calculateDiscoveryScore(
                    isLiked = liked,
                    inPlaylist = inPlaylist
                )

                if (score > DISCOVERY_THRESHOLD) {
                    itemsToRemove.add(index)
                } else {
                    itemsToKeep.add(index)
                }
            }

            // Ensure we keep at least some songs to avoid empty queue
            if (itemsToKeep.isEmpty() && itemsToRemove.isNotEmpty()) {
                itemsToRemove.shuffled().take(3).forEach { 
                    itemsToRemove.remove(it)
                    itemsToKeep.add(it)
                }
            }

            if (itemsToRemove.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    itemsToRemove.sortedDescending().forEach { index ->
                        player.removeMediaItem(index)
                    }

                    Toaster.s(
                        R.string.discover_has_been_applied_to_queue,
                        itemsToRemove.size,
                        duration = Toast.LENGTH_SHORT
                    )
                }
            }
        }
    }
}

private const val DISCOVERY_THRESHOLD = 5

private fun calculateDiscoveryScore(
    isLiked: Boolean,
    inPlaylist: Boolean
): Int {
    var score = 0
    
    // Penalize songs that are already known/liked
    if (isLiked) score += 3
    if (inPlaylist) score += 2
    
    return score
}
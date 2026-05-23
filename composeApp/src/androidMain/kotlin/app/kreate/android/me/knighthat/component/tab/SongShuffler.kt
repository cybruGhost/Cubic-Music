package app.kreate.android.me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.enums.MaxSongs
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.forcePlayFromBeginning
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.maxSongsInQueueKey
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.utils.Toaster

@UnstableApi
class SongShuffler private constructor(
    private val binder: PlayerServiceModern.Binder?,
    private val songs: () -> List<Song>
): MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke( songs: () -> List<Song> ) =
            SongShuffler( LocalPlayerServiceBinder.current, songs )

        @Composable
        operator fun invoke(
            databaseCall: (Int) -> Flow<List<Song>>,
            vararg key: Any?
        ): SongShuffler {
            val songsToShuffle by remember( key ) {
                databaseCall( Int.MAX_VALUE )
            }.collectAsState( emptyList(), Dispatchers.IO )

            return SongShuffler { songsToShuffle }
        }

        /**
         * Play songs with order shuffled.
         */
fun playShuffled(
    binder: PlayerServiceModern.Binder,
    songs: List<Song>
) {
    // Send message saying that there's no song to play
    if( songs.isEmpty() ) {
        Toaster.i( R.string.no_song_to_shuffle )
        return
    }

    val maxSongsInQueue: Int = appContext().preferences
                                           .getEnum( maxSongsInQueueKey, MaxSongs.`500` )
                                           .toInt()

    // Create a properly shuffled list without duplicates
    val shuffledSongs = songs.shuffled().take(maxSongsInQueue)
    
    // Convert to MediaItems
    val songsToPlay = shuffledSongs.map(Song::asMediaItem)
    
    // This is a cautious move, because binder's calls often require to be run on Main thread.
    CoroutineScope( Dispatchers.Main ).launch {
        // Stop any ongoing radio
        binder.stopRadio()
        
        // Set flag to indicate manual shuffle
        binder.isManuallyShuffled = true
        
        // CRITICAL: Disable shuffle mode to prevent interference
        // The queue is already shuffled, so we don't need player's shuffle mode
        if (binder.player.shuffleModeEnabled) {
            binder.player.shuffleModeEnabled = false
        }
        
        // Set the manually shuffled queue
        binder.player.forcePlayFromBeginning(songsToPlay)
        
        // Reset flag after queue is set
        delay(100) // Small delay to ensure queue is processed
        binder.isManuallyShuffled = false
    }
}
    }

    override val iconId: Int = R.drawable.shuffle
    override val messageId: Int = R.string.info_shuffle
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.shuffle )

    override fun onShortClick() {
        playShuffled(
            this.binder ?: return,      // Ensure that [binder] isn't null
            this.songs()
        )
    }
}
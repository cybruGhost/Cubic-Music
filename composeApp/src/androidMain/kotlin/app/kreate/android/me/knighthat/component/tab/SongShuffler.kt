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
                // TODO: add string to strings.xml
                Toaster.i( R.string.no_song_to_shuffle )
                return
            }

            val maxSongsInQueue: Int = appContext().preferences
                                                   .getEnum( maxSongsInQueueKey, MaxSongs.`500` )
                                                   .toInt()

            /**
             * [take] takes up to this amount of item, if [List.size]
             * was smaller than amount it can take, then take everything.
             *
             * If [take] was placed before [shuffled], any items
             * outside the "take" will never be reached.
             */
            val songsToPlay = songs.shuffled()
                                                     .take( maxSongsInQueue )
                                                     .map( Song::asMediaItem )
            // This is a cautious move, because binder's calls often require to be run on Main thread.
            CoroutineScope( Dispatchers.Main ).launch {
                binder.stopRadio()
                binder.player.forcePlayFromBeginning( songsToPlay )
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
package app.kreate.android.me.knighthat.component.song

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.models.Song
import androidx.compose.ui.res.stringResource
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.util.Optional

class GoToArtist(
    private val navController: NavController,
    private val song: Song,
): MenuIcon, Descriptive {

    override val iconId: Int = R.drawable.people
    // TODO: Add string "About this artist"
    override val messageId: Int = R.string.artists
    override val menuIconTitle: String
        @Composable
          get() = stringResource(R.string.more_of) + " ${song.cleanArtistsText()}"

    private var channelId: Optional<String> = Optional.empty()

    init {
        CoroutineScope( Dispatchers.IO ).launch {
            Database.artistTable
                    .findBySongId( song.id )
                    .first()
                    .firstOrNull()
                    ?.id
                    ?.also { channelId = Optional.of( it ) }
        }
    }

    override fun onShortClick() {
        channelId.ifPresentOrElse(
            { NavRoutes.artist.navigateHere( navController, it ) },
            {
                Toaster.n( R.string.looking_up_artist_online, song.cleanArtistsText() )

                CoroutineScope( Dispatchers.IO ).launch {
                    Innertube.nextPage(NextBody(videoId = song.id))
                             ?.onFailure {
                                 Timber.tag("go_to_artist").e( it )
                                 Toaster.e( R.string.failed_to_fetch_artist )
                             }
                             ?.getOrNull()
                             ?.itemsPage
                             ?.items
                             ?.firstOrNull()
                             ?.authors
                             ?.firstOrNull()
                             ?.endpoint
                             ?.takeIf { !it.browseId.isNullOrBlank() }
                             ?.let {
                                 val path = "${it.browseId}?params=${it.params.orEmpty()}"
                                 NavRoutes.artist.navigateHere( navController, path )
                             }
                }
            }
        )
    }
}
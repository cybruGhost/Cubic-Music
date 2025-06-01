package me.knighthat.component.song

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import timber.log.Timber
import java.util.Optional

class GoToAlbum(
    private val navController: NavController,
    private val song: Song
): MenuIcon, Descriptive {

    override val iconId: Int = R.drawable.album
    override val messageId: Int = R.string.go_to_album
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    private var albumId: Optional<String> = Optional.empty()

    init {
        CoroutineScope( Dispatchers.IO ).launch {
            Database.albumTable
                    .findBySongId( song.id )
                    .first()
                    ?.id
                    ?.let { albumId = Optional.of( it ) }
        }
    }


    override fun onShortClick() {
        albumId.ifPresentOrElse(
            { NavRoutes.album.navigateHere( navController, it ) },
            {
                Toaster.n( R.string.looking_up_album_from_the_internet )

                CoroutineScope( Dispatchers.IO ).launch {
                    Innertube.nextPage(NextBody(videoId = song.id))
                             ?.onFailure {
                                 Timber.tag("go_to_album").e(it)
                                 Toaster.e( R.string.failed_to_fetch_original_property )
                             }
                             ?.getOrNull()
                             ?.itemsPage
                             ?.items
                             ?.firstOrNull()
                             ?.album
                             ?.endpoint
                             ?.takeIf { !it.browseId.isNullOrBlank() }
                             ?.let {
                                 val path = "${it.browseId}?params=${it.params.orEmpty()}"
                                 NavRoutes.album.navigateHere( navController, path )
                             }
                }
            }
        )
    }
}
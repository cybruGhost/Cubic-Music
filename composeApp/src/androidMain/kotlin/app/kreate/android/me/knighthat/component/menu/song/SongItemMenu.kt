package app.kreate.android.me.knighthat.component.menu.song

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.ui.components.themed.Enqueue
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.PlayNext
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import app.kreate.android.me.knighthat.component.SongItem
import app.kreate.android.me.knighthat.component.menu.GridMenu
import app.kreate.android.me.knighthat.component.menu.ListMenu
import app.kreate.android.me.knighthat.component.song.ChangeAuthorDialog
import app.kreate.android.me.knighthat.component.song.ExportCacheDialog
import app.kreate.android.me.knighthat.component.song.GoToAlbum
import app.kreate.android.me.knighthat.component.song.GoToArtist
import app.kreate.android.me.knighthat.component.song.RenameSongDialog
import app.kreate.android.me.knighthat.component.song.ResetSongDialog
import app.kreate.android.me.knighthat.component.tab.DeleteSongDialog
import app.kreate.android.me.knighthat.component.tab.LikeComponent
import app.kreate.android.me.knighthat.component.tab.Radio
import app.kreate.android.me.knighthat.sync.YouTubeSync
import timber.log.Timber
import java.util.Optional

@UnstableApi
@ExperimentalFoundationApi
class SongItemMenu private constructor(
    private val navController: NavController,
    private val song: Song,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
): Menu {

    companion object {
        @Composable
        operator fun invoke( navController: NavController, song: Song ) : SongItemMenu =
            SongItemMenu(
                navController = navController,
                song = song,
                menuState = LocalMenuState.current,
                styleState = rememberPreference( menuStyleKey, MenuStyle.List )
            )
    }

    lateinit var buttons: List<Button>
    override var menuStyle: MenuStyle by styleState

    @Composable
    override fun ListMenu() = ListMenu.Menu {
        buttons.forEach {
            if (it is MenuIcon)
                it.ListMenuItem()
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu {
        items(buttons, Button::hashCode) {
            if (it is MenuIcon)
                it.GridMenuItem()
        }
    }

    @Composable
    override fun MenuComponent() {
        val context = LocalContext.current
        val binder = LocalPlayerServiceBinder.current

        /*
         * This big chunk of code is currently running as singleton.
         * While it may not have a big impact on performance but
         * it's there. One way to mitigate this is to setup a
         * pre-defined buttons with each button has a function
         * to update song(s). This way the buttons only init once
         * but the song(s) can be updated as we go
         */
        //<editor-fold defaultstate="collapsed" desc="Buttons">
        val renameSong = RenameSongDialog{ song }
        val changeAuthor = ChangeAuthorDialog{ song }
        val startRadio = Radio { listOf(song) }
        val playNext = PlayNext {
            binder?.player?.addNext( listOf(song.asMediaItem), appContext() )
        }
        val enqueue = Enqueue {
            binder?.player?.enqueue( listOf(song.asMediaItem), appContext() )
        }
        val addToFavorite = LikeComponent { listOf(song) }
        val addToPlaylist = PlaylistsMenu.init(
            navController = navController,
            mediaItems = { _ -> listOf(song.asMediaItem) },
            onFailure = { throwable, preview ->
                Timber.e( "Failed to add songs to playlist ${preview.playlist.name} on HomeSongs" )
                throwable.printStackTrace()
            },
            finalAction = {}
        )
        val deleteSongDialog = DeleteSongDialog().apply {
            song = Optional.of( this@SongItemMenu.song )
        }
        // Reactively collect artists from DB for per-artist "More of" buttons
        val artistsData by remember(song.id) {
            Database.artistTable.findBySongId(song.id)
        }.collectAsState(emptyList(), Dispatchers.IO)

        val goToArtistFallback = remember {
            GoToArtist( navController, song )
        }
        val goToAlbum = remember {
            GoToAlbum( navController, song )
        }
        val resetDialog = ResetSongDialog( song )
        val exportCacheDialog = ExportCacheDialog( binder ) { song }

        buttons = mutableListOf<Button>().apply {
            add( renameSong )
            add( changeAuthor )
            add( startRadio )
            add( playNext )
            add( enqueue )
            add( addToFavorite )
            add( addToPlaylist )
            if( !song.isLocal ) {
                add( goToAlbum )
                // Per-artist "More of" buttons
                if (artistsData.isEmpty()) {
                    // No DB data: split artistsText to create per-artist buttons
                    val artistNames = song.artistsText
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()

                    if (artistNames.size <= 1) {
                        // Single artist - use fallback with Innertube lookup
                        add( goToArtistFallback )
                    } else {
                        artistNames.forEach { artistName ->
                            add(object : MenuIcon, Descriptive, Clickable {
                                override val iconId: Int = R.drawable.people
                                override val messageId: Int = R.string.artists
                                @get:Composable
                                override val menuIconTitle: String get() = stringResource(R.string.more_of) + " $artistName"
                                override fun onShortClick() {
                                    menuState.hide()
                                    CoroutineScope(Dispatchers.IO).launch {
                                        Innertube.nextPage(NextBody(videoId = song.id))
                                            ?.getOrNull()
                                            ?.itemsPage?.items?.firstOrNull()
                                            ?.authors
                                            ?.find { it.name?.equals(artistName, ignoreCase = true) == true }
                                            ?.endpoint
                                            ?.takeIf { !it.browseId.isNullOrBlank() }
                                            ?.let {
                                                val path = "${it.browseId}?params=${it.params.orEmpty()}"
                                                NavRoutes.artist.navigateHere(navController, path)
                                            }
                                    }
                                }
                                override fun onLongClick() {}
                            })
                        }
                    }
                } else {
                    artistsData.forEach { artist ->
                        add(object : MenuIcon, Descriptive, Clickable {
                            override val iconId: Int = R.drawable.people
                            override val messageId: Int = R.string.artists
                            @get:Composable
                            override val menuIconTitle: String get() = stringResource(R.string.more_of) + " ${artist.name ?: ""}"
                            override fun onShortClick() {
                                menuState.hide()
                                navController.navigate("${NavRoutes.artist.name}/${artist.id}")
                            }
                            override fun onLongClick() {}
                        })
                    }
                }
                add( resetDialog )
            }
            add( deleteSongDialog )
            add( exportCacheDialog )
        }
        //</editor-fold>

        //<editor-fold desc="Dialog renders">
        renameSong.Render()
        changeAuthor.Render()
        deleteSongDialog.Render()
        resetDialog.Render()
        exportCacheDialog.Render()
        //</editor-fold>

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background( colorPalette().background1 )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Arrow Down",
                    tint = colorPalette().textSecondary,
                    modifier = Modifier.size( 24.dp )
                )

                SongItem(
                    song = song,
                    modifier = Modifier.padding(
                        top = 5.dp,
                        bottom = 10.dp
                    ),
                    trailingContent = {
                        val isLiked by remember {
                            Database.songTable
                                    .isLiked( song.id )
                                    .distinctUntilChanged()
                        }.collectAsState( false, Dispatchers.IO )

                        Column(
                            Modifier.width( TabToolBar.TOOLBAR_ICON_SIZE )
                        ) {
                            IconButton(
                                icon = if ( isLiked ) R.drawable.heart else R.drawable.heart_outline,
                                color = colorPalette().favoritesIcon,
                                onClick = {
                                    CoroutineScope( Dispatchers.IO ).launch {
                                        YouTubeSync.toggleSongLike( context, song.asMediaItem )
                                    }
                                },
                                modifier = Modifier.padding( all = 4.dp ).size( 20.dp )
                            )

                            if( !song.isLocal )
                                IconButton(
                                    icon = R.drawable.share_social,
                                    color = colorPalette().text,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra( Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}" )
                                        }

                                        context.startActivity(
                                            Intent.createChooser( intent, null )
                                        )
                                    },
                                    modifier = Modifier.padding( all = 4.dp ).size( 20.dp )
                                )
                        }
                    }
                )

                HorizontalDivider( Modifier.height(1.dp) )
            }

            if( menuStyle == MenuStyle.List )
                ListMenu()
            else
                GridMenu()
        }
    }
}
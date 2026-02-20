package me.knighthat.component.menu.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.MenuStyle
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.Info
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MenuState
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.ui.components.themed.SelectorDialog
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.SongItem
import me.knighthat.component.menu.GridMenu
import me.knighthat.component.menu.ListMenu
import me.knighthat.component.song.ChangeAuthorDialog
import me.knighthat.component.song.GoToAlbum
import me.knighthat.component.song.GoToArtist
import me.knighthat.component.song.RenameSongDialog
import me.knighthat.component.tab.LikeComponent
import me.knighthat.component.tab.Radio
import me.knighthat.sync.YouTubeSync
import me.knighthat.utils.Toaster
import timber.log.Timber

@UnstableApi
@ExperimentalFoundationApi
class PlayerItemMenu private constructor(
    private val navController: NavController,
    private val binder: PlayerServiceModern.Binder,
    private val mediaItem: MediaItem,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>,
    private val onDismiss: () -> Unit,
    private val onClosePlayer: () -> Unit,
    private val onShowSleepTimer: () -> Unit
): Menu {

    companion object {
        fun create(
            navController: NavController,
            binder: PlayerServiceModern.Binder,
            mediaItem: MediaItem,
            menuState: MenuState,
            styleState: MutableState<MenuStyle>,
            onDismiss: () -> Unit,
            onClosePlayer: () -> Unit,
            onShowSleepTimer: () -> Unit
        ): PlayerItemMenu =
            PlayerItemMenu(
                navController = navController,
                binder = binder,
                mediaItem = mediaItem,
                menuState = menuState,
                styleState = styleState,
                onDismiss = onDismiss,
                onClosePlayer = onClosePlayer,
                onShowSleepTimer = onShowSleepTimer
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
        val mContext = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()
        val song = remember(mediaItem) { mediaItem.asSong }

        // Reactively collect Album and Artists (like the old menu)
        val albumData by remember(mediaItem.mediaId) {
            Database.albumTable.findBySongId(mediaItem.mediaId)
        }.collectAsState(null, Dispatchers.IO)
        
        val artistsData by remember(mediaItem.mediaId) {
            Database.artistTable.findBySongId(mediaItem.mediaId)
        }.collectAsState(emptyList(), Dispatchers.IO)

        // Pre-create GoTo objects to avoid race condition on channelId lookup
        val goToArtistObj = remember(song) { GoToArtist(navController, song) }
        val goToAlbumObj = remember(song) { GoToAlbum(navController, song) }

        //<editor-fold defaultstate="collapsed" desc="Buttons">
        val renameSong = RenameSongDialog { song }
        val changeAuthor = ChangeAuthorDialog { song }
        val startRadio = Radio { listOf(song) }
        val addToFavorite = LikeComponent { listOf(song) }
        
        val activityResultLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

        val equalizerButton = remember {
            object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.equalizer
                override val messageId: Int = R.string.equalizer
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)

                override fun onShortClick() {
                    try {
                        activityResultLauncher.launch(
                            Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(
                                    AudioEffect.EXTRA_AUDIO_SESSION,
                                    binder.player.audioSessionId
                                )
                                putExtra(
                                    AudioEffect.EXTRA_PACKAGE_NAME,
                                    mContext.packageName
                                )
                                putExtra(
                                    AudioEffect.EXTRA_CONTENT_TYPE,
                                    AudioEffect.CONTENT_TYPE_MUSIC
                                )
                            }
                        )
                    } catch (e: ActivityNotFoundException) {
                        Toaster.w(R.string.info_not_find_application_audio)
                    }
                    menuState.hide()
                }
                override fun onLongClick() {}
            }
        }

        // Custom "Refetch" / "Update Song" button (from PlayerMenu logic)
        var showRefetchDialog by remember { mutableStateOf(false) }
        val refetchButton = remember {
            object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.refresh
                override val messageId: Int = R.string.update
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                
                override fun onShortClick() {
                    showRefetchDialog = true
                }
                override fun onLongClick() {}
            }
        }

        // Sleep Timer
        val sleepTimerButton = remember {
            object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.sleep
                override val messageId: Int = R.string.sleep_timer
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)

                override fun onShortClick() {
                    onShowSleepTimer()
                }
                override fun onLongClick() {}
            }
        }

        // Add to Playlist
        val addToPlaylist = PlaylistsMenu.init(
            navController = navController,
            mediaItems = { _ -> listOf(song.asMediaItem) },
            onFailure = { throwable, preview ->
                Timber.e( "Failed to add songs to playlist ${preview.playlist.name} on PlayerItemMenu" )
                throwable.printStackTrace()
            },
            finalAction = {
                menuState.hide()
            }
        )

        // Listen On
        var showListenOnDialog by remember { mutableStateOf(false) }
        val listenOnButton = remember {
            object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.play
                override val messageId: Int = R.string.listen_on
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)

                override fun onShortClick() {
                    showListenOnDialog = true
                }
                override fun onLongClick() {}
            }
        }

        // Re-order to match screenshot exactly
        buttons = remember(song, albumData, artistsData) {
            mutableListOf<Button>().apply {
                add(renameSong)           // 1
                add(changeAuthor)         // 2
                add(startRadio)           // 3
                add(equalizerButton)      // 4
                add(sleepTimerButton)     // 5
                add(addToFavorite)        // 6
                add(addToPlaylist)        // 7
                
                // Go to Album (Always visible, priority to direct navigation)
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.album
                    override val messageId: Int = R.string.go_to_album
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    
                    override fun onShortClick() {
                        val albumId = albumData?.id
                        if (!albumId.isNullOrBlank()) {
                            onDismiss()
                            onClosePlayer()
                            navController.navigate(NavRoutes.album.name + "/$albumId")
                        } else if (song.title.isNotBlank()) {
                            onDismiss()
                            onClosePlayer()
                            goToAlbumObj.onShortClick()
                        } else {
                            Toaster.w(R.string.album_not_found)
                        }
                    }
                    
                    override fun onLongClick() {}
                })
                
                // Go to Artist (Always visible)
                if (artistsData.isEmpty()) {
                    // No DB data: split artistsText to create per-artist buttons
                    val artistNames = song.artistsText
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()

                    if (artistNames.size <= 1) {
                        // Single artist - use GoToArtist fallback with Innertube lookup
                        add(object : MenuIcon, Descriptive, Clickable {
                            override val iconId: Int = R.drawable.people
                            override val messageId: Int = R.string.artists
                            @get:Composable
                            override val menuIconTitle: String get() = stringResource(R.string.more_of) + " ${song.cleanArtistsText()}"
                            override fun onShortClick() {
                                menuState.hide()
                                onClosePlayer()
                                goToArtistObj.onShortClick()
                            }
                            override fun onLongClick() {}
                        })
                    } else {
                        artistNames.forEach { artistName ->
                            add(object : MenuIcon, Descriptive, Clickable {
                                override val iconId: Int = R.drawable.people
                                override val messageId: Int = R.string.artists
                                @get:Composable
                                override val menuIconTitle: String get() = stringResource(R.string.more_of) + " $artistName"
                                override fun onShortClick() {
                                    menuState.hide()
                                    onClosePlayer()
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
                                onClosePlayer()
                                navController.navigate("${NavRoutes.artist.name}/${artist.id}")
                            }
                            override fun onLongClick() {}
                        })
                    }
                }

                add(listenOnButton)       // 10
                add(refetchButton)        // 11
            }
        }
        //</editor-fold>

        //<editor-fold desc="Dialog renders">
        renameSong.Render()
        changeAuthor.Render()
        
        if (showRefetchDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.update_song),
                onDismiss = { showRefetchDialog = false },
                onConfirm = {
                    showRefetchDialog = false
                    menuState.hide()
                    binder.cache.removeResource(mediaItem.mediaId)
                    binder.downloadCache.removeResource(mediaItem.mediaId)
                    Database.asyncTransaction {
                        Database.songTable.updateTotalPlayTime(mediaItem.mediaId, 0)
                    }
                }
            )
        }

        if (showListenOnDialog) {
             SelectorDialog(
                title = stringResource(R.string.listen_on),
                onDismiss = { showListenOnDialog = false },
                values = listOf(
                    Info(
                        "https://youtube.com/watch?v=${mediaItem.mediaId}",
                        stringResource(R.string.listen_on_youtube)
                    ),
                    Info(
                        "https://music.youtube.com/watch?v=${mediaItem.mediaId}",
                        stringResource(R.string.listen_on_youtube_music)
                    ),
                    Info(
                        "https://piped.kavin.rocks/watch?v=${mediaItem.mediaId}&playerAutoPlay=true",
                        stringResource(R.string.listen_on_piped)
                    ),
                    Info(
                        "https://yewtu.be/watch?v=${mediaItem.mediaId}&autoplay=1",
                        stringResource(R.string.listen_on_invidious)
                    )
                ),
                onValueSelected = {
                    binder.player.pause()
                    showListenOnDialog = false
                    menuState.hide()
                    uriHandler.openUri(it)
                }
            )
        }
        //</editor-fold>
        

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(colorPalette().background1)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Arrow Down",
                    tint = colorPalette().textSecondary,
                    modifier = Modifier.size(24.dp)
                )

                SongItem(
                    song = song,
                    modifier = Modifier.padding(
                        top = 5.dp,
                        bottom = 10.dp
                    ),
                    trailingContent = {
                        val isLiked = Database.songTable
                                .isLiked(song.id)
                                .collectAsState(initial = false, context = Dispatchers.IO)

                        Column {
                            IconButton(
                                icon = if (isLiked.value) R.drawable.heart else R.drawable.heart_outline,
                                color = colorPalette().favoritesIcon,
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    YouTubeSync.toggleSongLike(mContext, song.asMediaItem)
                                }
                            },
                                modifier = Modifier.padding(all = 4.dp).size(20.dp)
                            )

                            IconButton(
                                icon = R.drawable.share_social,
                                color = colorPalette().text,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                                    }

                                    mContext.startActivity(
                                        Intent.createChooser(intent, null)
                                    )
                                },
                                modifier = Modifier.padding(all = 4.dp).size(20.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(Modifier.height(1.dp))
            }

            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}
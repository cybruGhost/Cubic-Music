package me.knighthat.component.menu.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.MenuStyle
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MenuState
import it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.ui.components.themed.PlayNext
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.conditional
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.menuStyleKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.component.menu.GridMenu
import me.knighthat.component.menu.ListMenu
import me.knighthat.utils.Toaster
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import it.fast4x.rimusic.cleanPrefix
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.disableScrollingTextKey
import me.knighthat.component.tab.DownloadAllSongsDialog
import me.knighthat.component.tab.DeleteAllDownloadedSongsDialog

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
class AlbumItemMenu private constructor(
    private val navController: NavController,
    private val album: Album,
    private val songs: List<Song>,
    private val binder: PlayerServiceModern.Binder?,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>,
    private val onTitleChange: (String) -> Unit,
    private val onAuthorsChange: (String) -> Unit,
    private val onCoverChange: (String) -> Unit
) : Menu {

    companion object {
        @Composable
        operator fun invoke(
            navController: NavController,
            album: Album,
            songs: List<Song>,
            binder: PlayerServiceModern.Binder?,
            onTitleChange: (String) -> Unit,
            onAuthorsChange: (String) -> Unit,
            onCoverChange: (String) -> Unit
        ): AlbumItemMenu =
            AlbumItemMenu(
                navController = navController,
                album = album,
                songs = songs,
                binder = binder,
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List),
                onTitleChange = onTitleChange,
                onAuthorsChange = onAuthorsChange,
                onCoverChange = onCoverChange
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
    private fun AlbumItemDisplay(
        album: Album,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .background(colorPalette().background1)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Arrow Down",
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = Dimensions.itemsVerticalPadding,
                        horizontal = 16.dp
                    )
            ) {
                // Album's thumbnail
                Box(
                    Modifier.size(Dimensions.thumbnails.album / 2) // Taille réduite pour le menu
                ) {
                    ImageCacheFactory.Thumbnail(
                        thumbnailUrl = album.thumbnailUrl,
                        modifier = Modifier
                            .size(Dimensions.thumbnails.album / 2)
                            .clip(thumbnailShape())
                    )
                }

                // Album's information
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = cleanPrefix(album.title ?: ""),
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().text,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )

                    album.authorsText?.let {
                        BasicText(
                            text = cleanPrefix(it),
                            style = typography().xs.semiBold.secondary.copy(
                                color = colorPalette().textSecondary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                        )
                    }

                    album.year?.let {
                        BasicText(
                            text = it,
                            style = typography().xxs.semiBold.secondary.copy(
                                color = colorPalette().textSecondary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing content (Bookmark & Share)
                val isBookmarked by remember(album.id) {
                    Database.albumTable
                        .isBookmarked(album.id)
                        .distinctUntilChanged()
                }.collectAsState(false, Dispatchers.IO)

                Column(
                    Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        icon = if (isBookmarked) R.drawable.bookmark else R.drawable.bookmark_outline,
                        color = colorPalette().favoritesIcon,
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                Database.albumTable.toggleBookmark(album.id)
                            }
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.share_social,
                        color = colorPalette().text,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, album.shareUrl ?: "https://music.youtube.com/browse/${album.id}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )
                }
            }

            HorizontalDivider(Modifier.height(1.dp))
        }
    }

    @Composable
    override fun MenuComponent() {
        var showChangeTitleDialog by remember { mutableStateOf(false) }
        var showChangeAuthorsDialog by remember { mutableStateOf(false) }
        var showChangeCoverDialog by remember { mutableStateOf(false) }

        if (showChangeTitleDialog) {
            InputTextDialog(
                onDismiss = { showChangeTitleDialog = false },
                title = stringResource(R.string.update_title),
                value = album.title ?: "",
                placeholder = stringResource(R.string.title),
                setValue = { newValue ->
                    onTitleChange(newValue)
                    showChangeTitleDialog = false
                    menuState.hide()
                }
            )
        }

        if (showChangeAuthorsDialog) {
            InputTextDialog(
                onDismiss = { showChangeAuthorsDialog = false },
                title = stringResource(R.string.update_authors),
                value = album.authorsText ?: "",
                placeholder = stringResource(R.string.artists),
                setValue = { newValue ->
                    onAuthorsChange(newValue)
                    showChangeAuthorsDialog = false
                    menuState.hide()
                }
            )
        }

        if (showChangeCoverDialog) {
            InputTextDialog(
                onDismiss = { showChangeCoverDialog = false },
                title = stringResource(R.string.update_cover),
                value = album.thumbnailUrl ?: "",
                placeholder = stringResource(R.string.cover),
                setValue = { newValue ->
                    onCoverChange(newValue)
                    showChangeCoverDialog = false
                    menuState.hide()
                }
            )
        }

        // Collect artists from the first song if available
        // We observe the songs list to react to its population
        val artistsData by remember(album.id) {
            val songsFlow = Database.songAlbumMapTable.allSongsOf(album.id).distinctUntilChanged()
            songsFlow.flatMapLatest { songList ->
                val firstSongId = songList.firstOrNull()?.id
                if (firstSongId != null) {
                    Database.artistTable.findBySongId(firstSongId)
                } else {
                    flowOf(emptyList())
                }
            }
        }.collectAsState(emptyList(), Dispatchers.IO)

        // Group actions (Download/Delete all)
        val downloadAll = DownloadAllSongsDialog { songs }
        val deleteAll = DeleteAllDownloadedSongsDialog { songs }

        // Initialize buttons
        val playNext = PlayNext {
            binder?.player?.addNext(songs.map { it.asMediaItem }, appContext())
        }
        val enqueue = Enqueue {
            binder?.player?.enqueue(songs.map { it.asMediaItem }, appContext())
        }
        val addToPlaylist = PlaylistsMenu.init(
            navController = navController,
            mediaItems = { songs.map { it.asMediaItem } },
            onFailure = { _, _ -> },
            finalAction = { menuState.hide() }
        )

        val changeTitle = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.title_edit
            override val messageId: Int = R.string.update_title
            @get:Composable
            override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeTitleDialog = true }
        }

        val changeAuthors = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.artists_edit
            override val messageId: Int = R.string.update_authors
            @get:Composable
            override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeAuthorsDialog = true }
        }

        val changeCover = object : MenuIcon, Descriptive, Clickable {
            override val iconId: Int = R.drawable.cover_edit
            override val messageId: Int = R.string.update_cover
            @get:Composable
            override val menuIconTitle: String get() = stringResource(messageId)
            override fun onShortClick() { showChangeCoverDialog = true }
        }

        buttons = mutableListOf<Button>().apply {
            add(playNext)
            add(enqueue)
            add(addToPlaylist)
            add(downloadAll)
            add(deleteAll)
            
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
                })
            }

            add(changeTitle)
            add(changeAuthors)
            add(changeCover)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            downloadAll.Render()
            deleteAll.Render()
            AlbumItemDisplay(album = album)

            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}
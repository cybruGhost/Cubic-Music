package app.it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.PINNED_PREFIX
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.models.PlaylistPreview
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import app.kreate.android.me.knighthat.component.playlist.NewPlaylistDialog
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.utils.Toaster
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import app.it.fast4x.rimusic.PIPED_PREFIX
import app.it.fast4x.rimusic.utils.CShareClient
import kotlinx.coroutines.launch

class PlaylistsMenu private constructor(
    private val navController: NavController,
    private val mediaItems: (PlaylistPreview) -> List<MediaItem>,
    private val onFailure: (Throwable, PlaylistPreview) -> Unit,
    private val finalAction: (PlaylistPreview) -> Unit,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
): MenuIcon, Descriptive, Menu {

    companion object {
        @JvmStatic
        @Composable
        fun init(
            navController: NavController,
            mediaItems: (PlaylistPreview) -> List<MediaItem>,
            onFailure: (Throwable, PlaylistPreview) -> Unit,
            finalAction: (PlaylistPreview) -> Unit
        ) = PlaylistsMenu(
            navController,
            mediaItems,
            onFailure,
            finalAction,
            LocalMenuState.current,
            rememberPreference( menuStyleKey, MenuStyle.List )
        )
    }

    override val iconId: Int = R.drawable.add_in_playlist
    override val messageId: Int = R.string.add_to_playlist
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var menuStyle: MenuStyle by styleState

    private fun onAdd( preview: PlaylistPreview ) = Database.asyncTransaction {
        try {
            mapIgnore(preview.playlist, *mediaItems(preview).toTypedArray())
            Toaster.done()
        } catch (e: Throwable) {
            onFailure(e, preview)
        } finally {
            finalAction(preview)
        }
    }

    @Composable
    private fun PlaylistCard( playlistPreview: PlaylistPreview ) {
        val playlist = playlistPreview.playlist
        var showCShare by remember { mutableStateOf(false) }

        if (showCShare) CShareDialog(playlistPreview = playlistPreview, onDismiss = { showCShare = false })
        MenuEntry(
            icon = R.drawable.add_in_playlist,
            text = playlist.name.substringAfter( PINNED_PREFIX ),
            secondaryText = "${playlistPreview.songCount} ${stringResource( R.string.songs )}",
            onClick = {
                onAdd( playlistPreview )
            },
            trailingContent = {
                if (playlistPreview.playlist.name.startsWith(PIPED_PREFIX, 0, true))
                    Image(
                        painter = painterResource(R.drawable.piped_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().red),
                        modifier = Modifier.size(18.dp)
                    )
                if (playlistPreview.playlist.isYoutubePlaylist) {
                    Image(
                        painter = painterResource(R.drawable.ytmusic),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            Color.Red.copy(0.75f).compositeOver(Color.White)
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette().accent,
                    onClick = { showCShare = true },
                    modifier = Modifier.size(24.dp)
                )
                IconButton(
                    icon = R.drawable.open,
                    color = colorPalette().text,
                    onClick = {
                        menuState.hide()
                        navController.navigate(route = "${NavRoutes.localPlaylist.name}/${playlist.id}")
                    },
                    modifier = Modifier.size( 24.dp )
                )
            }
        )
    }

    override fun onShortClick() {
        menuState.hide()
        openMenu()
    }

    @Composable
    override fun ListMenu() { /* Does nothing */ }

    @Composable
    override fun GridMenu() { /* Does nothing */ }

    @Composable
    override fun MenuComponent() {
        val playlistPreviews by remember {
            Database.playlistTable.sortPreviewsByName()
        }.collectAsState( emptyList(), Dispatchers.IO )

        val pinnedPlaylists = playlistPreviews.filter {
            it.playlist.name.startsWith(PINNED_PREFIX, 0, true)
        }
        val unpinnedPlaylists = playlistPreviews.filter {
            !it.playlist.name.startsWith(PINNED_PREFIX, 0, true) &&
                    !it.playlist.name.startsWith(MONTHLY_PREFIX, 0, true)
        }

        val search = Search()
        val filteredPinnedPlaylists = pinnedPlaylists.filter { it.playlist.name.contains(search.inputValue, true) }
        val filteredUnpinnedPlaylists = unpinnedPlaylists.filter { it.playlist.name.contains(search.inputValue, true) }

        val newPlaylistButton = NewPlaylistDialog()
        newPlaylistButton.Render()
        var showImportCShare by remember { mutableStateOf(false) }
        if (showImportCShare) CShareImportDialog(onDismiss = { showImportCShare = false })

        Menu {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = ::onShortClick,
                    icon = R.drawable.chevron_back,
                    color = colorPalette().textSecondary,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .size(20.dp)
                )
                IconButton(
                    onClick = { search.isVisible = !search.isVisible },
                    icon = R.drawable.search_circle,
                    color = colorPalette().favoritesIcon,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .size(24.dp)
                )
                BasicText(
                    text = stringResource(R.string.playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(
                    onClick = { showImportCShare = true },
                    icon = R.drawable.share_social,
                    color = colorPalette().accent,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .size(24.dp)
                )
                newPlaylistButton.ToolBarButton()
            }
            search.SearchBar(this)
            if (filteredPinnedPlaylists.isNotEmpty()) {
                BasicText(
                    text = stringResource(R.string.pinned_playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 5.dp)
                )

                filteredPinnedPlaylists.forEach { PlaylistCard(it) }
            }

            if (filteredUnpinnedPlaylists.isNotEmpty()) {
                BasicText(
                    text = stringResource(R.string.playlists),
                    style = typography().m.semiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 5.dp)
                )

                filteredUnpinnedPlaylists.forEach { PlaylistCard(it) }
            }
        }
    }
}

@Composable
fun CShareDialog(
    playlistPreview: PlaylistPreview,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CShareClient.ShareResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val createFailed = stringResource(R.string.cshare_error_create_failed)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette().background1,
        titleContentColor = colorPalette().text,
        textContentColor = colorPalette().textSecondary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(stringResource(R.string.cshare_title)) },
        text = {
            Column {
                BasicText(
                    text = stringResource(R.string.cshare_create_description),
                    style = typography().xs.copy(color = colorPalette().textSecondary)
                )
                result?.let { share ->
                    Column(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorPalette().background2)
                            .clickable { clipboard.setText(AnnotatedString(share.shareText)) }
                            .padding(10.dp)
                    ) {
                        BasicText(stringResource(R.string.cshare_tap_to_copy), style = typography().xxs.copy(color = colorPalette().accent))
                        BasicText(share.shareText, style = typography().xs.copy(color = colorPalette().text))
                        BasicText(stringResource(R.string.cshare_expires_at, share.expiresAt), style = typography().xxs.copy(color = colorPalette().textSecondary))
                        BasicText(stringResource(R.string.cshare_song_count, share.songCount), style = typography().xxs.copy(color = colorPalette().textSecondary))
                    }
                }
                error?.let {
                    BasicText(
                        text = it,
                        style = typography().xs.copy(color = colorPalette().red),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        result = runCatching { CShareClient.sharePlaylist(playlistPreview.playlist) }
                            .onSuccess { clipboard.setText(AnnotatedString(it.shareText)) }
                            .onFailure { error = it.message ?: createFailed }
                            .getOrNull()
                        loading = false
                    }
                }
            ) {
                Text(if (loading) stringResource(R.string.cshare_creating) else stringResource(R.string.cshare_create_code))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun CShareImportDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<CShareClient.SharedPlaylist?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val readFailed = stringResource(R.string.cshare_error_read_failed)
    val importFailed = stringResource(R.string.cshare_error_import_failed)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette().background1,
        titleContentColor = colorPalette().text,
        textContentColor = colorPalette().textSecondary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(stringResource(R.string.cshare_import_title)) },
        text = {
            Column {
                BasicText(
                    text = stringResource(R.string.cshare_import_description),
                    style = typography().xs.copy(color = colorPalette().textSecondary)
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(stringResource(R.string.cshare_link_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorPalette().text,
                        unfocusedTextColor = colorPalette().text,
                        focusedContainerColor = colorPalette().background2,
                        unfocusedContainerColor = colorPalette().background2,
                        focusedBorderColor = colorPalette().accent,
                        unfocusedBorderColor = colorPalette().textSecondary.copy(alpha = 0.45f),
                        cursorColor = colorPalette().accent,
                        focusedLabelColor = colorPalette().accent,
                        unfocusedLabelColor = colorPalette().textSecondary
                    )
                )
                preview?.let {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorPalette().background2)
                            .padding(12.dp)
                    ) {
                        BasicText(it.name, style = typography().s.semiBold.copy(color = colorPalette().text))
                        BasicText(stringResource(R.string.cshare_song_count, it.songs.size), style = typography().xs.copy(color = colorPalette().textSecondary))
                        it.expiresAt?.let { expiry ->
                            BasicText(stringResource(R.string.cshare_expires_at, expiry), style = typography().xxs.copy(color = colorPalette().textSecondary))
                        }
                    }
                }
                error?.let {
                    BasicText(it, style = typography().xs.copy(color = colorPalette().red), modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        val loaded = preview ?: runCatching { CShareClient.importShare(link) }
                            .onFailure { error = it.message ?: readFailed }
                            .getOrNull()
                        if (preview == null) {
                            preview = loaded
                        } else if (loaded != null) {
                            runCatching { CShareClient.importIntoLibrary(loaded) }
                                .onSuccess {
                                    Toaster.done()
                                    onDismiss()
                                }
                                .onFailure { error = it.message ?: importFailed }
                        }
                        loading = false
                    }
                }
            ) {
                Text(if (loading) stringResource(R.string.working) else if (preview == null) stringResource(R.string.preview) else stringResource(R.string.import_playlist))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

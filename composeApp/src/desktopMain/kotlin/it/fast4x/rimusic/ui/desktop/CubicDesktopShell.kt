package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal object CubicRoutes {
    const val Browse = "browse"
    const val NewReleases = "new_releases"
    const val Songs = "songs"
    const val Albums = "albums"
    const val Artists = "artists"
    const val Recent = "recent"
    const val Favorites = "favorites"
    const val Playlists = "playlists"
    const val Search = "search"
    const val Album = "album"
    const val Artist = "artist"
    const val Playlist = "playlist"
    const val Mood = "mood"
    const val Cached = "cached"
    const val Profile = "profile"
    const val Settings = "settings"

    val primary = setOf(Browse, NewReleases, Songs, Albums, Artists, Recent, Favorites, Playlists, Cached, Profile, Settings)
}

private data class SidebarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val libraryItems = listOf(
    SidebarItem(CubicRoutes.Browse, "Browse", Icons.Rounded.Explore),
    SidebarItem(CubicRoutes.Songs, "Songs", Icons.Rounded.MusicNote),
    SidebarItem(CubicRoutes.Albums, "Albums", Icons.Rounded.Album),
    SidebarItem(CubicRoutes.Artists, "Artists", Icons.Rounded.Person),
    SidebarItem(CubicRoutes.Playlists, "Playlists", Icons.Rounded.PlaylistPlay)
)

private val personalItems = listOf(
    SidebarItem(CubicRoutes.Recent, "Recently played", Icons.Rounded.History),
    SidebarItem(CubicRoutes.Favorites, "Favorite songs", Icons.Rounded.FavoriteBorder),
    SidebarItem(CubicRoutes.Cached, "Cached songs", Icons.Rounded.DownloadDone)
)

@Composable
internal fun CubicSidebar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(244.dp)
            .fillMaxHeight()
            .background(CubicColors.Sidebar)
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            listOf(Color(0xFFFF605C), Color(0xFFFFBD44), Color(0xFF00CA4E)).forEach { color ->
                Box(Modifier.size(11.dp).background(color, CircleShape))
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigate(CubicRoutes.Profile) }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CubicLogo(38.dp)
            Column {
                Text(
                    text = "Cubic Music",
                    color = CubicColors.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text("DESKTOP", color = CubicColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
            }
        }

        Spacer(Modifier.height(34.dp))
        SidebarSection("LIBRARY", libraryItems, currentRoute, onNavigate)
        Spacer(Modifier.height(24.dp))
        SidebarSection("MY MUSIC", personalItems, currentRoute, onNavigate)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SidebarSection(
    title: String,
    items: List<SidebarItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Text(title, color = CubicColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(9.dp))
    items.forEach { item ->
        val isSelected = currentRoute == item.route ||
            (item.route == CubicRoutes.Browse && currentRoute !in CubicRoutes.primary)
        SidebarRow(item, isSelected) { onNavigate(item.route) }
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
private fun SidebarRow(item: SidebarItem, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> CubicColors.Selection
            hovered -> CubicColors.PanelRaised
            else -> Color.Transparent
        }
    )
    val contentColor by animateColorAsState(if (selected) CubicColors.Accent else CubicColors.TextSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(item.icon, null, tint = contentColor, modifier = Modifier.size(19.dp))
        Text(item.label, color = contentColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
internal fun CubicTopBar(
    currentRoute: String,
    canGoBack: Boolean,
    searchQuery: String,
    hasLibrarySongs: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigate: (String) -> Unit,
    onShuffle: () -> Unit
) {
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(320)
        searchFocusRequester.requestFocus()
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp).background(CubicColors.PanelRaised, CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = CubicColors.Text)
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .width(if (canGoBack) 260.dp else 300.dp)
                .height(56.dp)
                .focusRequester(searchFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        onSearch()
                        true
                    } else false
                },
            singleLine = true,
            placeholder = { Text("Search music", color = CubicColors.TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = CubicColors.TextSecondary, modifier = Modifier.size(20.dp)) },
            textStyle = TextStyle(color = CubicColors.Text, fontSize = 13.sp),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, "Run search", tint = CubicColors.Accent, modifier = Modifier.size(18.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CubicColors.PanelRaised,
                unfocusedContainerColor = CubicColors.PanelRaised,
                focusedTextColor = CubicColors.Text,
                unfocusedTextColor = CubicColors.Text,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = CubicColors.Accent
            )
        )

        Spacer(Modifier.weight(1f))
        TopLink("New releases", currentRoute == CubicRoutes.NewReleases) { onNavigate(CubicRoutes.NewReleases) }
        TopLink("Playlists", currentRoute == CubicRoutes.Playlists) { onNavigate(CubicRoutes.Playlists) }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = hasLibrarySongs, onClick = onShuffle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = if (hasLibrarySongs) CubicColors.Accent else CubicColors.TextMuted, modifier = Modifier.size(17.dp))
            Text("Shuffle play", color = if (hasLibrarySongs) CubicColors.TextSecondary else CubicColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Box(Modifier.clip(CircleShape).clickable { onNavigate(CubicRoutes.Profile) }) { CubicLogo(36.dp) }
    }
}

@Composable
private fun TopLink(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        color = if (selected) CubicColors.Accent else CubicColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}

@Composable
internal fun CubicLogo(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.size(size).background(CubicColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = CubicColors.Background, modifier = Modifier.size(size * 0.58f))
    }
}

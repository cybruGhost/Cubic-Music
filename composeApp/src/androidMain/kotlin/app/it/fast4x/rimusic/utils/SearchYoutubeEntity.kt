package app.it.fast4x.rimusic.utils

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ContentType
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.Title
import app.it.fast4x.rimusic.ui.screens.searchresult.OnlineSearchList
import app.kreate.android.R

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun SearchYoutubeEntity(
    navController: NavController,
    onDismiss: () -> Unit,
    query: String,
    disableScrollingText: Boolean
) {
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val normalizedQuery = query.trim()
    val tabs = listOf(
        YoutubeSearchTab(R.string.songs, R.drawable.musical_notes, 0),
        YoutubeSearchTab(R.string.videos, R.drawable.video, 3),
        YoutubeSearchTab(R.string.artists, R.drawable.artist, 2),
        YoutubeSearchTab(R.string.playlists, R.drawable.playlist, 4)
    )

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(WindowInsets.systemBars.asPaddingValues())
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    YoutubeSearchTabButton(
                        tab = tab,
                        selected = selectedTab == index,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = index }
                    )
                }
            }

            if (normalizedQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_youtube_hint),
                        style = typography().s,
                        color = colorPalette().textSecondary
                    )
                }
                return@Column
            }

            OnlineSearchList(
                query = normalizedQuery,
                tabIndex = tabs[selectedTab].searchResultTabIndex,
                filterContentType = ContentType.All,
                navController = navController,
                disableScrollingText = disableScrollingText,
                headerContent = {
                    Title(
                        title = stringResource(R.string.youtube_search),
                        verticalPadding = 4.dp
                    )
                    Title(
                        title = normalizedQuery,
                        verticalPadding = 4.dp
                    )
                },
                emptyItemsText = stringResource(R.string.no_results_found)
            )
        }
    }
}

private data class YoutubeSearchTab(
    val titleRes: Int,
    val iconRes: Int,
    val searchResultTabIndex: Int
)

@Composable
private fun YoutubeSearchTabButton(
    tab: YoutubeSearchTab,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) colorPalette().accent.copy(alpha = 0.18f) else colorPalette().background1
    val foreground = if (selected) colorPalette().accent else colorPalette().textSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = stringResource(tab.titleRes),
            style = typography().xs,
            color = foreground,
            maxLines = 1
        )
    }
}

package app.it.fast4x.rimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.SearchType
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.items.PlaylistItem
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.ui.styling.px

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSearch(
    onSearchType: (SearchType) -> Unit,
    disableScrollingText: Boolean
) {
    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px
    val lazyGridState = rememberLazyGridState()
    
    // Animation states for interactive effects
    val (selectedCard, setSelectedCard) = remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorPalette().background0,
                        colorPalette().background1,
                        colorPalette().background0
                    )
                )
            )
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues()
                    .calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
        ) {
            item(key = "header", contentType = 0, span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 24.dp, horizontal = 8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp),
                        tint = colorPalette().text
                    )
                    Text(
                        text = stringResource(R.string.search),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorPalette().text,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose where to search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorPalette().textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Online Search Card
            item(key = "online") {
                val isSelected = selectedCard == "online"
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 2.dp,
                    label = "card_elevation"
                )
                
                SearchOptionCard(
                    icon = R.drawable.globe,
                    title = "${stringResource(R.string.search)} ${stringResource(R.string.online)}",
                    subtitle = "Search across the web",
                    gradientColors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    ),
                    isSelected = isSelected,
                    elevation = elevation,
                    onClick = {
                        setSelectedCard("online")
                        onSearchType(SearchType.Online)
                    }
                )
            }

            // Library Search Card
            item(key = "library") {
                val isSelected = selectedCard == "library"
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 2.dp,
                    label = "card_elevation"
                )
                
                SearchOptionCard(
                    icon = R.drawable.library,
                    title = "${stringResource(R.string.search)} ${stringResource(R.string.library)}",
                    subtitle = "Search your local library",
                    gradientColors = listOf(
                        Color(0xFFf093fb),
                        Color(0xFFf5576c)
                    ),
                    isSelected = isSelected,
                    elevation = elevation,
                    onClick = {
                        setSelectedCard("library")
                        onSearchType(SearchType.Library)
                    }
                )
            }

            // Go to Link Card
            item(key = "gotolink") {
                val isSelected = selectedCard == "gotolink"
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 2.dp,
                    label = "card_elevation"
                )
                
                SearchOptionCard(
                    icon = R.drawable.query_stats,
                    title = stringResource(R.string.go_to_link),
                    subtitle = "Paste and navigate directly",
                    gradientColors = listOf(
                        Color(0xFF4facfe),
                        Color(0xFF00f2fe)
                    ),
                    isSelected = isSelected,
                    elevation = elevation,
                    onClick = {
                        setSelectedCard("gotolink")
                        onSearchType(SearchType.Gotolink)
                    }
                )
            }

            // Add more search options in the future
            item(key = "spacer", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SearchOptionCard(
    icon: Int,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    isSelected: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        label = "card_elevation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = animatedElevation,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorPalette().background1
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp),
                    tint = Color.White
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

// Alternative compact version using your existing PlaylistItem but enhanced
@Composable
fun EnhancedPlaylistItem(
    icon: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    disableScrollingText: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorPalette().background1
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorPalette().text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = colorPalette().text,
                textAlign = TextAlign.Center
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorPalette().textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
package it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.compose.persist.persist
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchSuggestionsBody
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.searchSuggestionsWithItems
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.SearchDisplayOrder
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.models.SearchQuery
import it.fast4x.rimusic.models.toUiMood
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.Header
import me.knighthat.component.menu.search.SearchItemMenu
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.rimusic.ui.components.themed.TitleMiniSection
import it.fast4x.rimusic.ui.items.AlbumItem
import it.fast4x.rimusic.ui.items.ArtistItem
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.align
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.isNowPlaying
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.pauseSearchHistoryKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.searchDisplayOrderKey
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun OnlineSearch(
    navController: NavController,
    textFieldValue: TextFieldValue,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
) {
    // Settings
    val isHistoryPaused by rememberPreference( pauseSearchHistoryKey, false )
    val searchDisplayOrder by rememberPreference( searchDisplayOrderKey, SearchDisplayOrder.SuggestionsFirst )
    
    // State for toggling between Discover and History
    var showDiscover by remember { mutableStateOf(true) }

    var reloadHistory by remember {
        mutableStateOf(false)
    }

    val history by remember( textFieldValue.text, isHistoryPaused, reloadHistory ) {
        if( isHistoryPaused ) return@remember flowOf()

        Database.searchTable
            .findAllContain( textFieldValue.text )
            .distinctUntilChanged()
            .map{ list -> list.reversed() }
    }.collectAsState( emptyList(), Dispatchers.IO )

    var suggestionsResult by remember {
        mutableStateOf<Result<Innertube.SearchSuggestions>?>(null)
    }

    // Fetch moods and genres for search screen
    var discoverPage by persist<Result<Innertube.DiscoverPage>>("search/moods")

    LaunchedEffect(Unit) {
        if (discoverPage == null) {
            discoverPage = Innertube.discoverPage()
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isNotEmpty()) {
            delay(200)
            suggestionsResult =
                Innertube.searchSuggestionsWithItems(SearchSuggestionsBody(input = textFieldValue.text))
        }
    }

    val rippleIndication = ripple(bounded = false)
    val timeIconPainter = painterResource(R.drawable.search_circle)
    val closeIconPainter = painterResource(R.drawable.trash)
    val historyIconPainter = painterResource(R.drawable.history)

    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember {
        FocusRequester()
    }

    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val lazyListState = rememberLazyListState()

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val binder = LocalPlayerServiceBinder.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    titleContent = {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = onTextFieldValueChanged,
                            textStyle = typography().l.medium.align(TextAlign.Start),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if ( textFieldValue.text.isNotEmpty() )
                                        onSearch( textFieldValue.text )
                                }
                            ),
                            cursorBrush = SolidColor(colorPalette().text),
                            decorationBox = decorationBox,
                            modifier = Modifier
                                .background(
                                    colorPalette().background1,
                                    shape = thumbnailRoundness.shape
                                )
                                .padding(all = 4.dp)
                                .focusRequester(focusRequester)
                                .fillMaxWidth()
                        )
                    },
                    actionsContent = {}
                )
            }

            // Show content based on search field
            if (textFieldValue.text.isEmpty()) {
                // Toggle button between Discover and History
                item(key = "view_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDiscover) 
                                stringResource(R.string.discover) 
                            else 
                                stringResource(R.string.searches_saved_searches),
                            style = typography().m.semiBold.copy(fontSize = 20.sp),
                            color = colorPalette().text
                        )
                        
                        // History/Discover toggle button
                        IconButton(
                            onClick = { showDiscover = !showDiscover },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = historyIconPainter,
                                contentDescription = null,
                                tint = if (showDiscover) colorPalette().textDisabled else colorPalette().text,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (showDiscover) {
                    // Show Discover (All Moods & Genres) - Use items directly without nested LazyVerticalGrid
                    discoverPage?.getOrNull()?.let { page ->
                        if (page.moods.isNotEmpty()) {
                            // Create a grid using LazyColumn items with a custom layout
                            val moods = page.moods.sortedBy { it.title }
                            val chunkedMoods = moods.chunked(2) // Split into pairs for 2-column grid
                            
                            chunkedMoods.forEachIndexed { index, rowItems ->
                                item(key = "mood_row_$index") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { mood ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(80.dp)
                                            ) {
                                                MoodAndGenreCard(
                                                    mood = mood,
                                                    onClick = {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("mood", mood.toUiMood())
                                                        navController.navigate(NavRoutes.mood.name)
                                                    }
                                                )
                                            }
                                        }
                                        // If odd number of items, add a spacer to maintain layout
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Show Search History
                    if (history.isNotEmpty()) {
                        item(key = "history_list_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        Database.asyncTransaction {
                                            history.also(searchTable::delete)
                                        }
                                        reloadHistory = !reloadHistory
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.trash),
                                        contentDescription = null,
                                        tint = colorPalette().textDisabled,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        items(
                            items = history,
                            key = SearchQuery::id
                        ) { searchQuery ->
                            ModernHistoryItem(
                                searchQuery = searchQuery,
                                onSearchClick = { onSearch(searchQuery.query.replace("/", "", true)) },
                                onDeleteClick = {
                                    Database.asyncTransaction {
                                        searchTable.delete(searchQuery)
                                    }
                                },
                                onEditClick = {
                                    onTextFieldValueChanged(
                                        TextFieldValue(
                                            text = searchQuery.query,
                                            selection = TextRange(searchQuery.query.length)
                                        )
                                    )
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                }
                            )
                        }
                    } else {
                        item(key = "empty_history") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.searches_no_suggestions),
                                    style = typography().s.secondary,
                                    color = colorPalette().textDisabled
                                )
                            }
                        }
                    }
                }
            } else {
                // Search results section (unchanged)
                when (searchDisplayOrder) {
                    SearchDisplayOrder.SuggestionsFirst -> {
                        // Suggestions section
                        suggestionsResult?.getOrNull()?.let { suggestions ->
                            item {
                                TitleMiniSection(
                                    title = stringResource(R.string.searches_suggestions),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }

                            suggestions.recommendedSong.let {
                                item{
                                    it?.asMediaItem?.let { mediaItem ->
                                        SongItem(
                                            song = mediaItem,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            onThumbnailContent = {
                                                NowPlayingSongIndicator(mediaItem.mediaId, binder?.player)
                                            },
                                            onDownloadClick = {},
                                            downloadState = downloadState,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onLongClick = {
                                                         hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.display {
                                                             SearchItemMenu(
                                                               navController = navController,
                                                               song = mediaItem.asSong
                                                        ).MenuComponent()
                                                    }
                                                },
                                                    onClick = {
                                                        binder?.player?.forcePlay(mediaItem)
                                                    }
                                                ),
                                            disableScrollingText = disableScrollingText,
                                            isNowPlaying = binder?.player?.isNowPlaying(mediaItem.mediaId) ?: false
                                        )
                                    }
                                }
                            }
                            suggestions.recommendedAlbum.let {
                                item{
                                    it?.let { album ->
                                        AlbumItem(
                                            yearCentered = false,
                                            album = album,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            modifier = Modifier
                                                .clickable {
                                                    navController.navigate(route = "${NavRoutes.album.name}/${album.key}")
                                                },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                }
                            }
                            suggestions.recommendedArtist.let {
                                item{
                                    it?.let { artist ->
                                        ArtistItem(
                                            artist = artist,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            modifier = Modifier
                                                .clickable {
                                                    navController.navigate(route = "${NavRoutes.artist.name}/${artist.key}")
                                                },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                }
                            }

                            items(items = suggestions.queries) { query ->
                                ModernSearchSuggestionItem(
                                    query = query,
                                    onSearchClick = { onSearch(query.replace("/", "", true)) },
                                    onEditClick = {
                                        onTextFieldValueChanged(
                                            TextFieldValue(
                                                text = query,
                                                selection = TextRange(query.length)
                                            )
                                        )
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        } ?: suggestionsResult?.exceptionOrNull()?.let {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    TitleMiniSection(title = stringResource(R.string.searches_no_suggestions),
                                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        // History section (always shown when searching)
                        if (history.isNotEmpty()) {
                            item {
                                TitleMiniSection(
                                    title = stringResource(R.string.searches_saved_searches),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }

                            items(
                                items = history,
                                key = SearchQuery::id
                            ) { searchQuery ->
                                ModernHistoryItem(
                                    searchQuery = searchQuery,
                                    onSearchClick = { onSearch(searchQuery.query.replace("/", "", true)) },
                                    onDeleteClick = {
                                        Database.asyncTransaction {
                                            searchTable.delete(searchQuery)
                                        }
                                    },
                                    onEditClick = {
                                        onTextFieldValueChanged(
                                            TextFieldValue(
                                                text = searchQuery.query,
                                                selection = TextRange(searchQuery.query.length)
                                            )
                                        )
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    SearchDisplayOrder.SavedSearchesFirst -> {
                        // History section first
                        if (history.isNotEmpty()) {
                            item {
                                TitleMiniSection(
                                    title = stringResource(R.string.searches_saved_searches),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }

                            items(
                                items = history,
                                key = SearchQuery::id
                            ) { searchQuery ->
                                ModernHistoryItem(
                                    searchQuery = searchQuery,
                                    onSearchClick = { onSearch(searchQuery.query.replace("/", "", true)) },
                                    onDeleteClick = {
                                        Database.asyncTransaction {
                                            searchTable.delete(searchQuery)
                                        }
                                    },
                                    onEditClick = {
                                        onTextFieldValueChanged(
                                            TextFieldValue(
                                                text = searchQuery.query,
                                                selection = TextRange(searchQuery.query.length)
                                            )
                                        )
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        }

                        // Suggestions section
                        suggestionsResult?.getOrNull()?.let { suggestions ->
                            item {
                                TitleMiniSection(title = stringResource(R.string.searches_suggestions),
                                    modifier = Modifier.padding(start = 12.dp).padding(vertical = 10.dp)
                                )
                            }

                            suggestions.recommendedSong.let {
                                item{
                                    it?.asMediaItem?.let { mediaItem ->
                                        SongItem(
                                            song = mediaItem,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            onThumbnailContent = {
                                                NowPlayingSongIndicator(mediaItem.mediaId, binder?.player)
                                            },
                                            onDownloadClick = {},
                                            downloadState = downloadState,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onLongClick = {
                                                         hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.display {
                                                            SearchItemMenu(
                                                            navController = navController,
                                                            song = mediaItem.asSong
                                                        ).MenuComponent()
                                                    }
                                                },
                                                    onClick = {
                                                        binder?.player?.forcePlay(mediaItem)
                                                    }
                                                ),
                                            disableScrollingText = disableScrollingText,
                                            isNowPlaying = binder?.player?.isNowPlaying(mediaItem.mediaId) ?: false
                                        )
                                    }
                                }
                            }
                            suggestions.recommendedAlbum.let {
                                item{
                                    it?.let { album ->
                                        AlbumItem(
                                            yearCentered = false,
                                            album = album,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            modifier = Modifier
                                                .clickable {
                                                    navController.navigate(route = "${NavRoutes.album.name}/${album.key}")
                                                },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                }
                            }
                            suggestions.recommendedArtist.let {
                                item{
                                    it?.let { artist ->
                                        ArtistItem(
                                            artist = artist,
                                            thumbnailSizePx = songThumbnailSizePx,
                                            thumbnailSizeDp = songThumbnailSizeDp,
                                            modifier = Modifier
                                                .clickable {
                                                    navController.navigate(route = "${NavRoutes.artist.name}/${artist.key}")
                                                },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                }
                            }

                            items(items = suggestions.queries) { query ->
                                ModernSearchSuggestionItem(
                                    query = query,
                                    onSearchClick = { onSearch(query.replace("/", "", true)) },
                                    onEditClick = {
                                        onTextFieldValueChanged(
                                            TextFieldValue(
                                                text = query,
                                                selection = TextRange(query.length)
                                            )
                                        )
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        } ?: suggestionsResult?.exceptionOrNull()?.let {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    TitleMiniSection(title = stringResource(R.string.searches_no_suggestions),
                                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
}

@Composable
fun MoodAndGenreCard(
    mood: Innertube.Mood.Item,
    onClick: () -> Unit
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val moodColor by remember { derivedStateOf { Color(mood.stripeColor) } }
    val imageResource = remember(mood.title) { MoodImages.getImageResource(mood.title) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(thumbnailRoundness.shape)
            .background(moodColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
        )

        Text(
            text = mood.title,
            style = typography().xs.semiBold.copy(color = Color.White, fontSize = 13.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}
@Composable
fun ModernSearchSuggestionItem(
    query: String,
    onSearchClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSearchClick,
                onLongClick = { /* optional long press */ },
                indication = ripple(
                    bounded = true,  // 👈 CHANGE to bounded ripple
                    color = colorPalette().accent.copy(alpha = 0.3f)  // 👈 Custom ripple color
                ),
                interactionSource = interactionSource
            )
            .then(
                if (isPressed) Modifier.background(colorPalette().accent.copy(alpha = 0.1f)) // 👈 Visual feedback
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = colorPalette().textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = query,
            style = typography().s.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f)
        )

        // Keep edit button separate so it doesn't trigger the main click
        Box(
            modifier = Modifier
                .clickable(
                    indication = ripple(bounded = true, radius = 16.dp),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onEditClick
                )
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.pencil),
                contentDescription = null,
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ModernHistoryItem(
    searchQuery: SearchQuery,
    onSearchClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSearchClick,
                indication = ripple(
                    bounded = true,  // 👈 FIXED
                    color = colorPalette().accent.copy(alpha = 0.3f)
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.history),
            contentDescription = null,
            tint = colorPalette().textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = searchQuery.query,
            style = typography().s.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f)
        )

        // Edit button with its own click handler
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.pencil),
                contentDescription = null,
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Delete button with its own click handler
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                tint = colorPalette().textDisabled,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
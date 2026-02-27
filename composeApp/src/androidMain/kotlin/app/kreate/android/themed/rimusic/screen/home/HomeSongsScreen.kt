package app.kreate.android.themed.rimusic.screen.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.themed.rimusic.screen.home.onDevice.OnDeviceSong
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.BuiltInPlaylist
import app.it.fast4x.rimusic.enums.CacheType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.ButtonsRow
import app.it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import app.it.fast4x.rimusic.ui.components.tab.TabHeader
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.themed.CacheSpaceIndicator
import app.it.fast4x.rimusic.ui.components.themed.Enqueue
import app.it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.it.fast4x.rimusic.ui.components.themed.HeaderInfo
import app.it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import app.it.fast4x.rimusic.ui.components.themed.PlayNext
import app.it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.addNext
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.builtInPlaylistKey
import app.it.fast4x.rimusic.utils.enqueue
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey
import app.it.fast4x.rimusic.utils.showMyTopPlaylistKey
import app.kreate.android.me.knighthat.component.ResetCache
import app.kreate.android.me.knighthat.component.tab.ImportSongsFromCSV
import app.kreate.android.me.knighthat.component.tab.ItemSelector
import app.kreate.android.me.knighthat.component.tab.LikeComponent
import app.kreate.android.me.knighthat.component.tab.Locator
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.component.tab.SongShuffler
import app.kreate.android.me.knighthat.component.tab.SmartShuffle
import timber.log.Timber
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.enums.RecommendationsNumber
import app.it.fast4x.rimusic.utils.recommendationsNumberKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.semiBold
// Add these to your existing imports:
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip

@UnstableApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSongsScreen(navController: NavController ) {
    // Essentials
    val binder = LocalPlayerServiceBinder.current
    val lazyListState = rememberLazyListState()

    var builtInPlaylist by rememberPreference( builtInPlaylistKey, BuiltInPlaylist.Favorites )
    var isRecommendationEnabled by remember { mutableStateOf(false) }
    val recommendationsNumber by rememberPreference( recommendationsNumberKey, RecommendationsNumber.Adaptive )
    var recommendationCount by remember { mutableStateOf(0) }
    var isRecommendationsLoading by remember { mutableStateOf(false) }

    val itemsOnDisplayState = remember { mutableStateListOf<Song>() }

    val itemSelector = ItemSelector<Song>()
    fun getSongs() = itemSelector.ifEmpty { itemsOnDisplayState }.toList()
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    val search = Search(lazyListState)
    val locator = Locator( lazyListState, ::getSongs )
    val import = ImportSongsFromCSV()
    val shuffle = SongShuffler(::getSongs)
    val smartShuffle = SmartShuffle(
        isRecommendationEnabled = { isRecommendationEnabled },
        isRecommendationsLoading = { isRecommendationsLoading },
        onToggleRecommendation = { isRecommendationEnabled = !isRecommendationEnabled }
    )
    val playNext = PlayNext {
        binder?.player?.addNext( getMediaItems(), appContext() )

        // Turn of selector clears the selected list
        itemSelector.isActive = false
    }
    val enqueue = Enqueue {
        binder?.player?.enqueue( getMediaItems(), appContext() )

        // Turn of selector clears the selected list
        itemSelector.isActive = false
    }
    val addToFavorite = LikeComponent(::getSongs)
    val addToPlaylist = PlaylistsMenu.init(
        navController = navController,
        mediaItems = { _ -> getMediaItems() },
        onFailure = { throwable, preview ->
            Timber.e( "Failed to add songs to playlist ${preview.playlist.name} on HomeSongs" )
            throwable.printStackTrace()
        },
        finalAction = {
            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    )
    val resetCache = ResetCache( ::getSongs )

    val buttons = remember( builtInPlaylist ) {
        
        // Disable checkboxes when category has changed
        itemSelector.isActive = false

        mutableStateListOf<Button>() .apply {
            this.add( search )
            this.add( locator )
            this.add( shuffle )
            this.add( smartShuffle )
            this.add( itemSelector )
            this.add( playNext )
            this.add( enqueue )
            this.add( addToFavorite )
            this.add( addToPlaylist )
            this.add( import )
            if( builtInPlaylist != BuiltInPlaylist.OnDevice )
                this.add( resetCache )
        }
    }

    Box(
        modifier = Modifier.background( colorPalette().background0 )
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column( Modifier.fillMaxSize() ) {
                // Sticky tab's title
            TabHeader( R.string.songs ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderInfo( itemsOnDisplayState.size.toString(), R.drawable.musical_notes )
                    }
                    if (isRecommendationEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.smart_shuffle),
                                contentDescription = null,
                                tint = colorPalette().textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            if (isRecommendationsLoading) {
                                Spacer(modifier = Modifier.width(4.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = colorPalette().textSecondary
                                )
                            } else if (recommendationCount > 0) {
                                BasicText(
                                    text = recommendationCount.toString(),
                                    style = TextStyle(
                                        color = colorPalette().textSecondary,
                                        fontStyle = typography().xxxs.semiBold.fontStyle,
                                        fontWeight = typography().xxxs.semiBold.fontWeight,
                                        fontSize = typography().xxxs.semiBold.fontSize
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding( start = 4.dp )
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    }
                }
            }

            // ============ COMPACT FILTER SECTION (FIRST) ============
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background0)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Compact filter row with search
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Filter label (compact)
                    BasicText(
                        text = "Filter:",
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().textSecondary
                        ),
                        maxLines = 1
                    )
                    
                    // Filter chips (compact)
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        val showFavoritesPlaylist by rememberPreference(showFavoritesPlaylistKey, true)
                        val showCachedPlaylist by rememberPreference(showCachedPlaylistKey, true)
                        val showMyTopPlaylist by rememberPreference(showMyTopPlaylistKey, true)
                        val showDownloadedPlaylist by rememberPreference(showDownloadedPlaylistKey, true)
                        val showOnDeviceChip by rememberPreference(showOnDevicePlaylistKey, true)
                        
                        val chips = remember(
                            showFavoritesPlaylist,
                            showCachedPlaylist,
                            showMyTopPlaylist,
                            showDownloadedPlaylist,
                            showOnDeviceChip
                        ) {
                            buildList {
                                add(BuiltInPlaylist.All)
                                if (showFavoritesPlaylist) add(BuiltInPlaylist.Favorites)
                                if (showCachedPlaylist) add(BuiltInPlaylist.Offline)
                                if (showDownloadedPlaylist) add(BuiltInPlaylist.Downloaded)
                                if (showMyTopPlaylist) add(BuiltInPlaylist.Top)
                                if (showOnDeviceChip) add(BuiltInPlaylist.OnDevice)
                            }
                        }
                        
                        ButtonsRow(
                            chips = chips,
                            currentValue = builtInPlaylist,
                            onValueUpdate = { builtInPlaylist = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Cache indicator when relevant (compact)
                when (builtInPlaylist) {
                    BuiltInPlaylist.Downloaded, BuiltInPlaylist.Offline -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        CacheSpaceIndicator(
                            cacheType = when (builtInPlaylist) {
                                BuiltInPlaylist.Downloaded -> CacheType.DownloadedSongs
                                BuiltInPlaylist.Offline -> CacheType.CachedSongs
                                else -> CacheType.CachedSongs
                            }
                        )
                    }
                    else -> {}
                }
            }

            // Sticky tab's tool bar (AFTER FILTER)
            TabToolBar.Buttons( buttons )

            // Sticky search bar
            search.SearchBar( this )

            when( builtInPlaylist ) {
                BuiltInPlaylist.OnDevice -> OnDeviceSong( navController, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs )
                else                     -> HomeSongs( navController, builtInPlaylist, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs, onRecommendationCountChange = { count -> recommendationCount = count }, onRecommendationsLoadingChange = { loading -> isRecommendationsLoading = loading }, isRecommendationEnabled = isRecommendationEnabled )
            }
        }
        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

        val showFloatingIcon by rememberPreference( showFloatingIconKey, false )
        if( UiType.ViMusic.isCurrent() && showFloatingIcon )
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = {
                    navController.navigate(NavRoutes.search.name)
                },
                onClickSettings = {
                    navController.navigate(NavRoutes.settings.name)
                },
                onClickSearch = {
                    navController.navigate(NavRoutes.search.name)
                }
            )
    }
}
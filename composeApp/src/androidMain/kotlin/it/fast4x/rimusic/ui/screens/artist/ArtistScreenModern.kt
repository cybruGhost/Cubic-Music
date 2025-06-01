package it.fast4x.rimusic.ui.screens.artist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFirstOrNull
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.screens.artist.ArtistDetails
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.requests.ArtistPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.PlayerPosition
import it.fast4x.rimusic.enums.TransitionEffect
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.playerPositionKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.transitionEffectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.utils.PropUtils

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun ArtistScreenModern(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {},
) {
    // Essentials
    val saveableStateHolder = rememberSaveableStateHolder()

    // Settings
    val transitionEffect by rememberPreference( transitionEffectKey, TransitionEffect.Scale )
    val playerPosition by rememberPreference( playerPositionKey, PlayerPosition.Bottom )

    var localArtist: Artist? by remember { mutableStateOf( null ) }
    LaunchedEffect( Unit ) {
        Database.artistTable
                .findById( browseId )
                .flowOn( Dispatchers.IO )
                .collect { localArtist = it }
    }
    var artistPage: ArtistPage? by remember { mutableStateOf( null ) }

    LaunchedEffect( Unit ) {
        YtMusic.getArtistPage( browseId )
               .onSuccess { online ->
                   artistPage = online

                   Database.asyncTransaction {
                       val onlineArtist = Artist(
                           id = browseId,
                           name =  PropUtils.retainIfModified( localArtist?.name, online.artist.title ),
                           thumbnailUrl = PropUtils.retainIfModified( localArtist?.thumbnailUrl, online.artist.thumbnail?.url ),
                           timestamp = localArtist?.timestamp ?: System.currentTimeMillis(),
                           bookmarkedAt = localArtist?.bookmarkedAt,
                           isYoutubeArtist = localArtist?.isYoutubeArtist == true
                       )
                       artistTable.upsert( onlineArtist )

                       online.sections
                             .fastFirstOrNull { section ->
                                 section.items.fastAll { it is Innertube.SongItem }
                             }
                             ?.items
                             ?.map { (it as Innertube.SongItem).asMediaItem }
                             ?.also {
                                 mapIgnore( onlineArtist, *it.toTypedArray() )
                             }
                   }
               }
    }

    val thumbnailPainter = ImageCacheFactory.Painter( localArtist?.thumbnailUrl )

    androidx.compose.material3.Scaffold(
        modifier = Modifier,
        containerColor = colorPalette().background0,
        topBar = {
            if( UiType.RiMusic.isCurrent() )
                AppHeader( navController ).Draw()
        }
    ) {
        Box(
            modifier = Modifier.padding(it)
                               .fillMaxSize()
        ) {

            Row(
                Modifier.fillMaxSize()
                        .background( colorPalette().background0 )
            ) {
                val topPadding = if ( UiType.ViMusic.isCurrent() ) 30.dp else 0.dp

                AnimatedContent(
                    targetState = 0,
                    transitionSpec = {
                        when (transitionEffect) {
                            TransitionEffect.None -> EnterTransition.None togetherWith ExitTransition.None
                            TransitionEffect.Expand -> expandIn(
                                animationSpec = tween(
                                    350,
                                    easing = LinearOutSlowInEasing
                                ), expandFrom = Alignment.BottomStart
                            ).togetherWith(
                                shrinkOut(
                                    animationSpec = tween(
                                        350,
                                        easing = FastOutSlowInEasing
                                    ), shrinkTowards = Alignment.CenterStart
                                )
                            )

                            TransitionEffect.Fade -> fadeIn(animationSpec = tween(350)).togetherWith(
                                fadeOut(animationSpec = tween(350))
                            )

                            TransitionEffect.Scale -> scaleIn(animationSpec = tween(350)).togetherWith(
                                scaleOut(animationSpec = tween(350))
                            )

                            TransitionEffect.SlideHorizontal, TransitionEffect.SlideVertical -> {
                                val slideDirection = when (targetState > initialState) {
                                    true -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Left
                                        else AnimatedContentTransitionScope.SlideDirection.Up
                                    }

                                    false -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Right
                                        else AnimatedContentTransitionScope.SlideDirection.Down
                                    }
                                }

                                val animationSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = Spring.StiffnessLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )

                                slideIntoContainer(
                                    slideDirection,
                                    animationSpec
                                ) togetherWith
                                        slideOutOfContainer(slideDirection, animationSpec)
                            }
                        }
                    },
                    label = "",
                    modifier = Modifier.fillMaxHeight()
                        .padding( top = topPadding )
                ) { currentTabIndex ->
                    saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                        when (currentTabIndex) {
                            0 -> ArtistDetails( navController, localArtist, artistPage, thumbnailPainter )
                        }
                    }
                }
            }

            //**
            Box(
                modifier = Modifier.padding( vertical = 5.dp )
                                   .align(
                                       if( playerPosition == PlayerPosition.Top )
                                           Alignment.TopCenter
                                       else
                                           Alignment.BottomCenter
                                   )
            ) {
                miniPlayer.invoke()
            }
        }
    }
}
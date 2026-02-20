package it.fast4x.rimusic.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFirstOrNull
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.screens.artist.ArtistDetails
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.requests.ArtistPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.PlayerPosition
import it.fast4x.rimusic.enums.TransitionEffect
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.playerPositionKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.transitionEffectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.utils.PropUtils
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import app.kreate.android.R
import it.fast4x.rimusic.ui.components.Skeleton

import it.fast4x.innertube.models.bodies.QueueBody
import it.fast4x.innertube.requests.queue

@OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
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


    var selectedTabIndex by remember { mutableStateOf(0) }

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

                   // Batch fetch durations for videos and songs missing it
                   val itemsToFetch = online.sections.flatMap { it.items }
                       .filter { (it is Innertube.VideoItem && it.durationText == null) || (it is Innertube.SongItem && it.durationText == null) }
                       .map { it.key }
                       .distinct()

                   if (itemsToFetch.isNotEmpty()) {
                       Innertube.queue(QueueBody(videoIds = itemsToFetch))?.onSuccess { queueItems ->
                           val durationsMap = queueItems?.associate { it.key to it.durationText } ?: emptyMap()
                           artistPage = artistPage?.withUpdatedVideoDurations(durationsMap)
                       }
                   }
               }
    }

    val thumbnailPainter = ImageCacheFactory.Painter( localArtist?.thumbnailUrl )

    Skeleton(
        navController = navController,
        tabIndex = selectedTabIndex,
        onTabChanged = { selectedTabIndex = it },
        miniPlayer = miniPlayer,
        navBarContent = { Item ->
            Item(0, stringResource(R.string.overview), R.drawable.artist)
            Item(1, stringResource(R.string.library), R.drawable.library)
        }
    ) { currentTabIndex ->
        when (currentTabIndex) {
            0 -> ArtistDetails(navController, localArtist, artistPage, thumbnailPainter)
            1 -> ArtistLocalSongs(navController, localArtist, artistPage, thumbnailPainter)
        }
    }
}
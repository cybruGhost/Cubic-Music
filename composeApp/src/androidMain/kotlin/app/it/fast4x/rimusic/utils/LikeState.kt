package app.it.fast4x.rimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.it.fast4x.rimusic.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun getLikeState(mediaId: String): Int {
    val songLikeState by remember( mediaId ) {
        Database.songTable
                .likeState( mediaId )
                .distinctUntilChanged()
    }.collectAsState( false, Dispatchers.IO )

    return when( songLikeState ) {
        false -> getDislikedIcon()
        null -> getUnlikedIcon()
        true -> getLikedIcon()
    }
}

fun setLikeState(likedAt: Long?): Long? {
    val current =
     when (likedAt) {
        -1L -> null
        null -> System.currentTimeMillis()
        else -> -1L
    }
    //println("mediaItem setLikeState: $current")
    return current

}


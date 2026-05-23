package app.it.fast4x.rimusic.models

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.utils.resolveLocalMediaUri
import app.it.fast4x.rimusic.utils.sanitizePlaybackUri
import java.io.Serializable

data class PersistentQueue(
    val title: String?,
    val songMediaItems: List<PersistentSong>,
    val mediaItemIndex: Int,
    val position: Long,
) : Serializable

data class PersistentSong(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0
) : Serializable

val PersistentSong.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (id.startsWith(LOCAL_KEY_PREFIX)) resolveLocalMediaUri(id) else sanitizePlaybackUri(id)
        )
        .setCustomCacheKey(id)
        .build()

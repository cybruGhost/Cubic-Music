package app.it.fast4x.rimusic.utils


import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Timeline
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.enums.DurationInMinutes
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.util.ArrayDeque

var GlobalVolume: Float = 0.5f

private val youtubeIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

fun String.isYouTubeVideoId(): Boolean = youtubeIdRegex.matches(trim())

fun MediaItem.playbackVideoIdOrNull(): String? =
    mediaId
        .trim()
        .split("/")
        .lastOrNull()
        ?.takeIf { it.isYouTubeVideoId() }
        ?: localConfiguration?.uri
            ?.toString()
            ?.trim()
            ?.takeIf { it.isYouTubeVideoId() }

fun MediaItem.isPlayable(): Boolean {
    val id = mediaId.trim()
    val uri = localConfiguration?.uri
    if (id.isBlank() && uri == null) return false
    if (id.startsWith(LOCAL_KEY_PREFIX, ignoreCase = true)) {
        return uri != null
    }
    if (id.startsWith("http", ignoreCase = true)) return true
    if (uri != null) {
        val uriText = uri.toString().trim()
        return uri.scheme in setOf("http", "https", "content", "file") || uriText.isYouTubeVideoId()
    }
    if (id.contains("/", ignoreCase = true)) return id.split("/").lastOrNull()?.isYouTubeVideoId() == true
    return id.isYouTubeVideoId()
}

private fun MediaItem.isPlaybackSourceValid(): Boolean {
    val id = mediaId.trim()
    val uri = sanitizePlaybackUri(
        localConfiguration?.uri?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: id
    )
    return id.isNotBlank() || uri != Uri.EMPTY
}

private fun Player.sanitizedPlaybackQueue(mediaItems: List<MediaItem>): List<MediaItem> =
    mediaItems.mapNotNull { item ->
        val cleanedItem = item.cleaned
        if (!cleanedItem.isPlayable() || !cleanedItem.isPlaybackSourceValid()) {
            Timber.w("Dropping invalid media item from queue mediaId=%s", item.mediaId)
            null
        } else {
            cleanedItem.buildUpon()
                .setUri(
                    sanitizePlaybackUri(
                        cleanedItem.localConfiguration?.uri?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: cleanedItem.mediaId
                    )
                )
                .build()
        }
    }.fastDistinctBy(MediaItem::mediaId)

private fun Player.findPlayableStartIndex(
    mediaItems: List<MediaItem>,
    preferredIndex: Int
): Int {
    if (mediaItems.isEmpty()) return -1
    if (preferredIndex in mediaItems.indices) return preferredIndex
    return mediaItems.indexOfFirst { it.isPlayable() && it.isPlaybackSourceValid() }
}

fun Player.skipBrokenMediaItem(reason: String? = null): Boolean {
    val brokenId = currentMediaItem?.mediaId.orEmpty()
    Timber.w("Skipping broken media item mediaId=%s reason=%s", brokenId, reason)
    return when {
        hasNextMediaItem() -> {
            runCatching { seekToNextMediaItem() }
                .onFailure { Timber.e(it, "skipBrokenMediaItem failed seeking next for mediaId=%s", brokenId) }
                .isSuccess.also { success ->
                    if (success) {
                        safePrepare()
                        restoreGlobalVolume()
                        playWhenReady = true
                    }
                }
        }
        mediaItemCount > 1 && currentMediaItemIndex in 0 until mediaItemCount -> {
            val indexToRemove = currentMediaItemIndex
            runCatching { removeMediaItem(indexToRemove) }
                .onFailure { Timber.e(it, "skipBrokenMediaItem failed removing mediaId=%s", brokenId) }
                .isSuccess.also { success ->
                    if (success && mediaItemCount > 0) {
                        val nextIndex = indexToRemove.coerceAtMost(mediaItemCount - 1)
                        runCatching { seekTo(nextIndex, C.TIME_UNSET) }
                        safePrepare()
                        restoreGlobalVolume()
                        playWhenReady = true
                    } else if (success) {
                        pause()
                    }
                }
        }
        else -> {
            pause()
            false
        }
    }
}

private fun MediaItem.hasOfflinePlayableSource(context: Context): Boolean {
    val rawUri = localConfiguration?.uri?.toString().orEmpty()
    if (mediaId.isBlank() && rawUri.isBlank()) return false
    val parsedUri = runCatching { Uri.parse(rawUri) }.getOrNull()
    val scheme = parsedUri?.scheme.orEmpty()
    if (
        scheme.equals("file", ignoreCase = true) ||
        scheme.equals("content", ignoreCase = true) ||
        mediaId.startsWith("/") ||
        mediaId.startsWith("content://", ignoreCase = true) ||
        mediaId.startsWith("file://", ignoreCase = true)
    ) {
        return true
    }

    val songId = mediaId.substringAfterLast("/").ifBlank {
        rawUri.substringAfter("v=", "").substringBefore("&").ifBlank { mediaId }
    }

    if (songId.isBlank()) return false

    val isDownloaded = MyDownloadHelper.isSongDownloaded(songId)
    val cachedBytes = runCatching {
        MyDownloadHelper.getDownloadCache(context).getCachedBytes(songId, 0, -1)
    }.getOrDefault(0L)
    val hasStoredFormat = runCatching {
        runBlocking(Dispatchers.IO) {
            Database.formatTable.findBySongId(songId).first()?.contentLength != null
        }
    }.getOrDefault(false)

    return isDownloaded || cachedBytes > 0L || hasStoredFormat
}

private fun ensurePlayableOrNotify(mediaItem: MediaItem): Boolean {
    val context = appContext()
    if (mediaItem.mediaId.isBlank() && mediaItem.localConfiguration?.uri == null) {
        Toaster.w("This song has an invalid source")
        Timber.w("Blocked playback for invalid media item with empty mediaId and URI")
        return false
    }
    if (isNetworkConnected(context)) return true
    if (mediaItem.hasOfflinePlayableSource(context)) return true
    Toaster.w("No internet and this song is not cached yet")
    Timber.w("Blocked playback for uncached media item while offline: ${mediaItem.mediaId}")
    return false
}

fun Player.safePrepare(): Boolean =
    runCatching { prepare() }
        .onFailure { Timber.e(it, "safePrepare failed for mediaId=%s", currentMediaItem?.mediaId) }
        .isSuccess

fun Player.safeSetMediaItem(mediaItem: MediaItem, resetPosition: Boolean = true): Boolean =
    runCatching { setMediaItem(mediaItem, resetPosition) }
        .onFailure { Timber.e(it, "safeSetMediaItem failed for mediaId=%s", mediaItem.mediaId) }
        .isSuccess

fun Player.safeSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): Boolean =
    run {
        val sanitizedItems = sanitizedPlaybackQueue(mediaItems)
        val safeIndex = findPlayableStartIndex(sanitizedItems, startIndex)
        if (sanitizedItems.isEmpty() || safeIndex < 0) {
            Timber.w("safeSetMediaItems blocked empty or invalid queue requestedSize=%s startIndex=%s", mediaItems.size, startIndex)
            return@run false
        }
        runCatching { setMediaItems(sanitizedItems, safeIndex, startPositionMs) }
            .onFailure { Timber.e(it, "safeSetMediaItems failed at index=%s size=%s", safeIndex, sanitizedItems.size) }
            .isSuccess
    }

fun Player.safeSeekTo(positionMs: Long): Boolean =
    runCatching { seekTo(positionMs.coerceAtLeast(0L)) }
        .onFailure { Timber.e(it, "safeSeekTo failed for mediaId=%s position=%s", currentMediaItem?.mediaId, positionMs) }
        .isSuccess

fun Player.safeRelease(): Boolean =
    runCatching { release() }
        .onFailure { Timber.e(it, "safeRelease failed") }
        .isSuccess

fun Player.restoreGlobalVolume() {
    volume = GlobalVolume
}

fun Player.saveGlobalVolume() {
    GlobalVolume = volume
}

fun Player.setGlobalVolume(v: Float) {
    GlobalVolume = v
}

fun Player.getGlobalVolume(): Float {
    return GlobalVolume
}

fun Player.isNowPlaying(mediaId: String): Boolean {
val cleanId = mediaId.split("/").lastOrNull() ?: mediaId
    val cleanCurrentId = currentMediaItem?.mediaId?.split("/")?.lastOrNull() ?: currentMediaItem?.mediaId
    return cleanId == cleanCurrentId
}

val Player.currentWindow: Timeline.Window?
    get() = if (mediaItemCount == 0) null else currentTimeline.getWindow(currentMediaItemIndex, Timeline.Window())

val Timeline.mediaItems: List<MediaItem>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window()).mediaItem
    }

inline val Timeline.windows: List<Timeline.Window>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window())
    }

val Player.shouldBePlaying: Boolean
    get() = !(playbackState == Player.STATE_ENDED || !playWhenReady)

fun Player.removeMediaItems(range: IntRange) = removeMediaItems(range.first, range.last + 1)

//fun Player.seamlessPlay(mediaItem: MediaItem) {
//    if (mediaItem.mediaId == currentMediaItem?.mediaId) {
//        if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
//        if (currentMediaItemIndex < mediaItemCount - 1) removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
//    } else {
//        forcePlay(mediaItem)
//    }
//}

fun Player.seamlessPlay(mediaItem: MediaItem) {
    if (mediaItem.mediaId == currentMediaItem?.mediaId) {
        if (currentMediaItemIndex > 0) removeMediaItems(0 until currentMediaItemIndex)
        if (currentMediaItemIndex < mediaItemCount - 1)
            removeMediaItems(currentMediaItemIndex + 1 until mediaItemCount)
    } else forcePlay(mediaItem)
}


fun Player.shuffleQueue() {
    val mediaItems = currentTimeline.mediaItems.toMutableList().apply { removeAt(currentMediaItemIndex) }
    if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
    if (currentMediaItemIndex < mediaItemCount - 1) removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
    addMediaItems(mediaItems.shuffled())
}

@SuppressLint("Range")
@UnstableApi
fun Player.playAtMedia(mediaItems: List<MediaItem>, mediaId: String) {
    Log.d("mediaItem-playAtMedia","${mediaItems.size}")
    if (mediaItems.isEmpty()) return
    val itemIndex = findMediaItemIndexById(mediaId)

    Log.d("mediaItem-playAtMedia",itemIndex.toString())
    if (!safeSetMediaItems(mediaItems, itemIndex, C.TIME_UNSET)) return
    if (!safePrepare()) return
    restoreGlobalVolume()
    playWhenReady = true

}

fun Player.forcePlay(mediaItem: MediaItem) {
    if (!ensurePlayableOrNotify(mediaItem)) return
    val cleanedMediaItem = mediaItem.cleaned
    if (!cleanedMediaItem.isPlaybackSourceValid()) {
        skipBrokenMediaItem("forcePlay invalid source")
        return
    }
    val safeMediaItem = cleanedMediaItem.buildUpon()
        .setUri(
            sanitizePlaybackUri(
                cleanedMediaItem.localConfiguration?.uri?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: cleanedMediaItem.mediaId
            )
        )
        .build()
    val existingIndex = findMediaItemIndexById(safeMediaItem.mediaId)

    if (existingIndex >= 0 && mediaItemCount > 0) {
        val remainingQueue = (existingIndex until mediaItemCount)
            .map(::getMediaItemAt)
            .map { queuedItem ->
                queuedItem.cleaned.buildUpon()
                    .setUri(
                        sanitizePlaybackUri(
                            queuedItem.localConfiguration?.uri?.toString()
                                ?.takeIf { it.isNotBlank() }
                                ?: queuedItem.mediaId
                        )
                    )
                    .build()
            }
            .fastDistinctBy(MediaItem::mediaId)

        if (!safeSetMediaItems(remainingQueue, 0, C.TIME_UNSET)) return
    } else {
        if (!safeSetMediaItem(safeMediaItem, true)) return
    }

    if (!safePrepare()) return
    restoreGlobalVolume()
    playWhenReady = true
}

fun Player.playVideo(mediaItem: MediaItem) {
    if (!mediaItem.cleaned.isPlaybackSourceValid()) {
        skipBrokenMediaItem("playVideo invalid source")
        return
    }
    val safeMediaItem = mediaItem.cleaned.buildUpon()
        .setUri(
            sanitizePlaybackUri(
                mediaItem.localConfiguration?.uri?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: mediaItem.mediaId
            )
        )
        .build()
    if (!safeSetMediaItem(safeMediaItem, true)) return
    pause()
}

fun Player.playAtIndex(mediaItemIndex: Int) {
    runCatching { seekTo(mediaItemIndex, C.TIME_UNSET) }
        .onFailure { Timber.e(it, "playAtIndex seek failed at index=%s", mediaItemIndex) }
        .getOrElse { return }
    if (!safePrepare()) return
    restoreGlobalVolume()
    playWhenReady = true
}

@SuppressLint("Range")
@UnstableApi
fun Player.forcePlayAtIndex(mediaItems: List<MediaItem>, mediaItemIndex: Int) {
    val cleanedMediaItems = sanitizedPlaybackQueue(mediaItems)
    if ( cleanedMediaItems.isEmpty() ) return
    val safeIndex = findPlayableStartIndex(cleanedMediaItems, mediaItemIndex)
    if (safeIndex < 0) return
    val selectedItem = cleanedMediaItems.getOrNull(safeIndex) ?: return
    if (!ensurePlayableOrNotify(selectedItem)) return

    if (!safeSetMediaItems(cleanedMediaItems, safeIndex, C.TIME_UNSET)) return
    if (!safePrepare()) return
    restoreGlobalVolume()
    playWhenReady = true
}
@UnstableApi
fun Player.forcePlayFromBeginning(mediaItems: List<MediaItem>) =
    forcePlayAtIndex(mediaItems, 0)

fun Player.forceSeekToPrevious() {
    if (hasPreviousMediaItem() || currentPosition > maxSeekToPreviousPosition) {
        seekToPrevious()
    } else if (mediaItemCount > 0) {
        seekTo(mediaItemCount - 1, C.TIME_UNSET)
    }
}

fun Player.forceSeekToNext() =
    if (hasNextMediaItem()) seekToNext() else seekTo(0, C.TIME_UNSET)

fun Player.playNext() {
    seekToNextMediaItem()
    //seekToNext()
    safePrepare()
    restoreGlobalVolume()
    playWhenReady = true
    play()
}

fun Player.playPrevious() {
    seekToPreviousMediaItem()
    //seekToPrevious()
    safePrepare()
    restoreGlobalVolume()
    playWhenReady = true
    play()
}

@UnstableApi
fun Player.addNext(mediaItem: MediaItem, context: Context? = null) {
    if (context != null && excludeMediaItem(mediaItem, context)) return
    if (!mediaItem.cleaned.isPlaybackSourceValid()) return

    val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
    if (itemIndex >= 0) removeMediaItem(itemIndex)

    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlay(mediaItem)
    } else {
        addMediaItem((currentMediaItemIndex + 1).coerceAtMost(mediaItemCount), mediaItem.cleaned)
    }
}

@UnstableApi
fun Player.addNext(mediaItems: List<MediaItem>, context: Context? = null) {
    val filteredMediaItems = if (context != null) excludeMediaItems(mediaItems, context)
    else mediaItems
    val cleanedMediaItems = sanitizedPlaybackQueue(filteredMediaItems)

    cleanedMediaItems.forEach { mediaItem ->
        val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
        if (itemIndex >= 0) removeMediaItem(itemIndex)
    }

    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        safeSetMediaItems(cleanedMediaItems, 0, C.TIME_UNSET)

        if( playbackState == Player.STATE_IDLE )
            prepare()

        play()
    } else {
        addMediaItems(currentMediaItemIndex + 1, cleanedMediaItems)
    }

}


fun Player.enqueue(mediaItem: MediaItem, context: Context? = null) {
     if (context != null && excludeMediaItem(mediaItem, context)) return
    if (!mediaItem.cleaned.isPlaybackSourceValid()) return

    val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
    if (itemIndex >= 0) removeMediaItem(itemIndex)

    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlay(mediaItem)
    } else {
        addMediaItem(mediaItemCount, mediaItem.cleaned)
    }
}


@UnstableApi
fun Player.enqueue(mediaItems: List<MediaItem>, context: Context? = null) {
    val filteredMediaItems = if (context != null) excludeMediaItems(mediaItems, context)
    else mediaItems
    val cleanedMediaItems = sanitizedPlaybackQueue(filteredMediaItems)

    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlayFromBeginning(cleanedMediaItems)
    } else {
        addMediaItems(mediaItemCount, cleanedMediaItems)
    }
}

/*
fun Player.findNextMediaItemById(mediaId: String): MediaItem? {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) {
            return getMediaItemAt(i)
        }
    }
    return null
}
*/

fun Player.findNextMediaItemById(mediaId: String): MediaItem? = runCatching {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) return getMediaItemAt(i)
    }
    return null
}.getOrNull()

fun Player.findMediaItemIndexById(mediaId: String): Int {
     // Strip prefix for robust comparison
    val cleanSearchId = mediaId.split("/").lastOrNull() ?: mediaId
    
    for (i in currentMediaItemIndex until mediaItemCount) {
        val item = getMediaItemAt(i)
            val cleanItemId = item.mediaId.split("/").lastOrNull() ?: item.mediaId
            if (cleanItemId == cleanSearchId) {
            return i
        }
    }
    return -1
}

fun Player.excludeMediaItems(mediaItems: List<MediaItem>, context: Context): List<MediaItem> {
    var filteredMediaItems = mediaItems
    runCatching {
        val preferences = context.preferences
        val excludeSongWithDurationLimit =
            preferences.getEnum(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)

        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            filteredMediaItems = mediaItems.filter {
                it.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
                    durationTextToMillis(it1)
                }!! < excludeSongWithDurationLimit.asMillis
            }

            val excludedSongs = mediaItems.size - filteredMediaItems.size
            if (excludedSongs > 0)
                Toaster.n( R.string.message_excluded_s_songs, arrayOf( excludedSongs ) )
        }
    }.onFailure {
        Timber.e(it.message)
    }

    return filteredMediaItems
}
fun Player.excludeMediaItem(mediaItem: MediaItem, context: Context): Boolean {
    runCatching {
        val preferences = context.preferences
        val excludeSongWithDurationLimit =
            preferences.getEnum(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)
        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            val excludedSong = mediaItem.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
                    durationTextToMillis(it1)
                }!! <= excludeSongWithDurationLimit.asMillis

            if (excludedSong)
                Toaster.n( R.string.message_excluded_s_songs, arrayOf( 1 ) )

            return excludedSong
        }
    }.onFailure {
        //it.printStackTrace()
        Timber.e(it.message)
        return false
    }

    return false

}

val Player.mediaItems: List<MediaItem>
    get() = object : AbstractList<MediaItem>() {
        override val size: Int
            get() = mediaItemCount

        override fun get(index: Int): MediaItem = getMediaItemAt(index)
    }

fun Player.getCurrentQueueIndex(): Int {
    if (currentTimeline.isEmpty) {
        return -1
    }
    var index = 0
    var currentMediaItemIndex = currentMediaItemIndex
    while (currentMediaItemIndex != C.INDEX_UNSET) {
        currentMediaItemIndex = currentTimeline.getPreviousWindowIndex(currentMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
        if (currentMediaItemIndex != C.INDEX_UNSET) {
            index++
        }
    }
    return index
}

fun Player.togglePlayPause() {
    if (!playWhenReady && playbackState == Player.STATE_IDLE) {
        prepare()
    }
    playWhenReady = !playWhenReady
}

fun Player.toggleRepeatMode() {
    repeatMode = when (repeatMode) {
        REPEAT_MODE_OFF -> REPEAT_MODE_ALL
        REPEAT_MODE_ALL -> REPEAT_MODE_ONE
        REPEAT_MODE_ONE -> REPEAT_MODE_OFF
        else -> throw IllegalStateException()
    }
}

fun Player.toggleShuffleMode() {
    shuffleModeEnabled = !shuffleModeEnabled
}

fun Player.getQueueWindows(): List<Timeline.Window> {
    val timeline = currentTimeline
    if (timeline.isEmpty) {
        return emptyList()
    }
    val queue = ArrayDeque<Timeline.Window>()
    val queueSize = timeline.windowCount

    val currentMediaItemIndex: Int = currentMediaItemIndex
    queue.add(timeline.getWindow(currentMediaItemIndex, Timeline.Window()))

    var firstMediaItemIndex = currentMediaItemIndex
    var lastMediaItemIndex = currentMediaItemIndex
    val shuffleModeEnabled = shuffleModeEnabled
    while ((firstMediaItemIndex != C.INDEX_UNSET || lastMediaItemIndex != C.INDEX_UNSET) && queue.size < queueSize) {
        if (lastMediaItemIndex != C.INDEX_UNSET) {
            lastMediaItemIndex = timeline.getNextWindowIndex(lastMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
            if (lastMediaItemIndex != C.INDEX_UNSET) {
                queue.add(timeline.getWindow(lastMediaItemIndex, Timeline.Window()))
            }
        }
        if (firstMediaItemIndex != C.INDEX_UNSET && queue.size < queueSize) {
            firstMediaItemIndex = timeline.getPreviousWindowIndex(firstMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
            if (firstMediaItemIndex != C.INDEX_UNSET) {
                queue.addFirst(timeline.getWindow(firstMediaItemIndex, Timeline.Window()))
            }
        }
    }
    return queue.toList()
}

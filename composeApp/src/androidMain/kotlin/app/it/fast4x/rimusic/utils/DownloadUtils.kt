package app.it.fast4x.rimusic.utils


import app.cubic.android.core.network.isNetworkAvailable
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalDownloadHelper
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.enums.DownloadedStateMedia
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.service.modern.isLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import androidx.compose.ui.unit.dp

const val DOWNLOAD_INDICATOR_SIZE_NORMAL = 18
const val DOWNLOAD_INDICATOR_SIZE_SWIPE = 20
const val DOWNLOAD_INDICATOR_STROKE_WIDTH = 2

@UnstableApi
@Composable
fun InitDownloader() {
    val context = LocalContext.current
    MyDownloadHelper.getDownloadManager(context)
    MyDownloadHelper.getDownloads()
}


@UnstableApi
@Composable
fun downloadedStateMedia(mediaId: String): DownloadedStateMedia {
    val binder = LocalPlayerServiceBinder.current

    val cachedBytes = remember(mediaId, binder?.cache?.cacheSpace) {
        binder?.cache?.getCachedBytes(mediaId, 0, -1) ?: 0L
    }

    val isDownloaded by remember(mediaId) {
        MyDownloadHelper.getDownload(mediaId).map { download ->
            download?.state == Download.STATE_COMPLETED
        }
    }.collectAsState(initial = false, context = Dispatchers.IO)
    val isDownloadCached = remember(mediaId, binder?.downloadCache?.cacheSpace) {
        MyDownloadHelper.isDownloadCached(mediaId)
    }
    val isCached by remember(mediaId, cachedBytes) {
        Database.formatTable.findBySongId( mediaId ).map {
            cachedBytes > 0L || (
                it?.contentLength != null &&
                    it.contentLength > 0L &&
                    cachedBytes >= it.contentLength
                )
        }
    }.collectAsState( false, Dispatchers.IO )

    return when {
        isDownloaded && (isCached || isDownloadCached) -> DownloadedStateMedia.CACHED_AND_DOWNLOADED
        isDownloaded -> DownloadedStateMedia.DOWNLOADED
        isCached || isDownloadCached -> DownloadedStateMedia.CACHED
        else -> DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED
    }
}

@UnstableApi
@Composable
fun getDownloadStateMedia(
    binder: PlayerServiceModern.Binder,
    songId: String
): DownloadedStateMedia {
    if( songId.startsWith( LOCAL_KEY_PREFIX, true ) )
        return DownloadedStateMedia.DOWNLOADED

    val isDownloaded by remember(songId) {
        MyDownloadHelper.getDownload(songId)
            .map { download -> download?.state == Download.STATE_COMPLETED }
    }.collectAsState(initial = false, context = Dispatchers.IO)
    val isDownloadCached = remember(songId, binder.downloadCache.cacheSpace) {
        MyDownloadHelper.isDownloadCached(songId)
    }
    val cachedBytes = remember(songId, binder.cache.cacheSpace) {
        binder.cache.getCachedBytes(songId, 0, -1)
    }
    val isCached by remember(songId, cachedBytes) {
        Database.formatTable
            .findBySongId( songId )
            .map {
                cachedBytes > 0L || (
                    it?.contentLength != null &&
                        it.contentLength > 0L &&
                        binder.cache.isCached( it.songId, 0, it.contentLength )
                    )
            }
    }.collectAsState( false, Dispatchers.IO )

    return when {
        isDownloaded && (isCached || isDownloadCached) -> DownloadedStateMedia.CACHED_AND_DOWNLOADED
        isDownloaded -> DownloadedStateMedia.DOWNLOADED
        isCached || isDownloadCached -> DownloadedStateMedia.CACHED
        // !isDownloaded && !isCached
        else                      -> DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED
    }
}

@UnstableApi
fun manageDownload(
    context: android.content.Context,
    mediaItem: MediaItem,
    downloadState: Boolean = false
) {

    if (mediaItem.isLocal) return

    if (downloadState) {
        MyDownloadHelper.removeDownload(context = context, mediaItem = mediaItem)
    }
    else {
        if (context.isNetworkAvailable) {
            MyDownloadHelper.addDownload(context = context, mediaItem = mediaItem)
        }
    }

}


@UnstableApi
@Composable
fun getDownloadState(mediaId: String): Int {
    val downloader = LocalDownloadHelper.current

    val downloadFlow = remember(mediaId) { downloader.getDownload(mediaId) }
    return downloadFlow.collectAsState(initial = null as Download?).value?.state
        ?: Download.STATE_STOPPED
}

@UnstableApi
@Composable
fun getDownloadProgress(mediaId: String): Float {
    val downloader = LocalDownloadHelper.current
    return downloader.progresses.collectAsState().value[mediaId] ?: 0f
}

@OptIn(UnstableApi::class)
@Composable
fun isDownloadedSong(mediaId: String): Boolean {
    return when (downloadedStateMedia(mediaId)) {
        DownloadedStateMedia.CACHED_AND_DOWNLOADED,
        DownloadedStateMedia.DOWNLOADED -> true
        else -> false
    }
}

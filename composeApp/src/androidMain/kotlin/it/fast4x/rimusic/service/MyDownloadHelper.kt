package it.fast4x.rimusic.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import app.kreate.android.service.createDataSourceFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.AudioQualityFormat
import it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import it.fast4x.rimusic.enums.ExoPlayerDiskCacheMaxSize
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.modern.isLocal
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.audioQualityFormatKey
import it.fast4x.rimusic.utils.autoDownloadSongKey
import it.fast4x.rimusic.utils.autoDownloadSongWhenAlbumBookmarkedKey
import it.fast4x.rimusic.utils.autoDownloadSongWhenLikedKey
import it.fast4x.rimusic.utils.download
import it.fast4x.rimusic.utils.downloadSyncedLyrics
import it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import it.fast4x.rimusic.utils.exoPlayerCustomCacheKey
import it.fast4x.rimusic.utils.exoPlayerDiskDownloadCacheMaxSizeKey
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.removeDownload
import it.fast4x.rimusic.utils.thumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.knighthat.coil.ImageCacheFactory
import me.knighthat.utils.Toaster
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory

@UnstableApi
object MyDownloadHelper {
    private val executor = Executors.newCachedThreadPool()
    private val coroutineScope = CoroutineScope(
        executor.asCoroutineDispatcher() +
                SupervisorJob() +
                CoroutineName("MyDownloadService-Executor-Scope")
    )

    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    const val CACHE_DIRNAME = "exo_downloads"
    const val ROOT_DOWNLOAD_DIR = "RiMusic/Downloads"

    private lateinit var databaseProvider: DatabaseProvider
    lateinit var downloadCache: Cache

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper
    private lateinit var downloadManager: DownloadManager
    lateinit var audioQualityFormat: AudioQualityFormat

    var downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    fun getDownload(songId: String): Flow<Download?> {
        return downloads.map { it[songId] }
    }

    @SuppressLint("LongLogTag")
    @Synchronized
    fun getDownloads() {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context?): DownloadNotificationHelper {
        if (!MyDownloadHelper::downloadNotificationHelper.isInitialized) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context!!, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager
    }

    @Synchronized
    private fun initDownloadCache(context: Context): SimpleCache {
        val cacheSize = context.preferences.getEnum(exoPlayerDiskDownloadCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)

        val cacheEvictor = when(cacheSize) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            ExoPlayerDiskCacheMaxSize.Custom -> {
                val customCacheSize = context.preferences.getInt(exoPlayerCustomCacheKey, 32) * 1000 * 1000L
                LeastRecentlyUsedCacheEvictor(customCacheSize)
            }
            else -> LeastRecentlyUsedCacheEvictor(cacheSize.bytes)
        }

        val cacheDir = when(cacheSize) {
            ExoPlayerDiskCacheMaxSize.Disabled -> createTempDirectory(CACHE_DIRNAME).toFile()
            else -> {
                // FIXED: Use app-specific storage that works on all Android versions
                when(context.preferences.getEnum(exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System)) {
                    ExoPlayerCacheLocation.System -> {
                        // Use app-specific external storage (works on all Android versions)
                        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: 
                        context.filesDir.resolve(CACHE_DIRNAME)
                    }
                    ExoPlayerCacheLocation.Private -> context.filesDir.resolve(CACHE_DIRNAME)
                }
            }
        }

        // Ensure this location exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        return SimpleCache(cacheDir, cacheEvictor, getDatabaseProvider(context))
    }

    @Synchronized
    fun getDownloadCache(context: Context): Cache {
        if (!MyDownloadHelper::downloadCache.isInitialized)
            downloadCache = initDownloadCache(context)

        return downloadCache
    }

    // FIXED: Removed the problematic cacheDir access
    fun getDownloadedFilePath(context: Context, songId: String): String? {
        return try {
            val cache = getDownloadCache(context)
            val cacheSpans = cache.getCachedSpans(songId)
            
            if (cacheSpans.isNotEmpty()) {
                // Instead of trying to access private cacheDir, reconstruct the path
                val cacheSize = context.preferences.getEnum(exoPlayerDiskDownloadCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)
                val cacheDir = when(cacheSize) {
                    ExoPlayerDiskCacheMaxSize.Disabled -> createTempDirectory(CACHE_DIRNAME).toFile()
                    else -> {
                        when(context.preferences.getEnum(exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System)) {
                            ExoPlayerCacheLocation.System -> {
                                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: 
                                context.filesDir.resolve(CACHE_DIRNAME)
                            }
                            ExoPlayerCacheLocation.Private -> context.filesDir.resolve(CACHE_DIRNAME)
                        }
                    }
                }
                cacheDir.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e("Error getting downloaded file path: ${e.message}")
            null
        }
    }

    // FIXED: Proper URI handling for downloaded content
    fun getDownloadedSongUri(context: Context, songId: String): Uri? {
        val download = downloads.value[songId]
        return if (download?.state == Download.STATE_COMPLETED) {
            // ExoPlayer handles cached content automatically - return original URI
            Uri.parse("https://music.youtube.com/watch?v=$songId")
        } else {
            null
        }
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        audioQualityFormat =
            context.preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)

        if (!MyDownloadHelper::downloadManager.isInitialized) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                createDataSourceFactory(),
                executor
            ).apply {
                maxParallelDownloads = 3
                minRetryCount = 2
                requirements = Requirements(Requirements.NETWORK)

                addListener(
                    object : DownloadManager.Listener {
                        override fun onDownloadChanged(
                            downloadManager: DownloadManager,
                            download: Download,
                            finalException: Exception?
                        ) = run {
                            syncDownloads(download)
                        }

                        override fun onDownloadRemoved(
                            downloadManager: DownloadManager,
                            download: Download
                        ) = run {
                            syncDownloads(download)
                        }
                    }
                )
            }
        }
    }

    @Synchronized
    private fun syncDownloads(download: Download) {
        downloads.update { map ->
            map.toMutableMap().apply {
                set(download.request.id, download)
            }
        }
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (!MyDownloadHelper::databaseProvider.isInitialized) databaseProvider =
            StandaloneDatabaseProvider(context)
        return databaseProvider
    }

    fun addDownload(context: Context, mediaItem: MediaItem) {
        if (mediaItem.isLocal) return

        if(!isNetworkConnected(context)) {
            Toaster.noInternet()
            return
        }

        val downloadRequest = DownloadRequest
            .Builder(
                /* id      = */ mediaItem.mediaId,
                /* uri     = */ mediaItem.requestMetadata.mediaUri
                    ?: Uri.parse("https://music.youtube.com/watch?v=${mediaItem.mediaId}")
            )
            .setCustomCacheKey(mediaItem.mediaId)
            .setData("${mediaItem.mediaMetadata.artist.toString()} - ${mediaItem.mediaMetadata.title.toString()}".encodeToByteArray())
            .build()

        Database.asyncTransaction {
            insertIgnore(mediaItem)
        }

        val imageUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(1200)

        coroutineScope.launch {
            context.download<MyDownloadService>(downloadRequest).exceptionOrNull()?.let {
                if (it is CancellationException) throw it
                Timber.e("MyDownloadHelper scheduleDownload exception ${it.stackTraceToString()}")
                println("MyDownloadHelper scheduleDownload exception ${it.stackTraceToString()}")
            }
            downloadSyncedLyrics(mediaItem.asSong)
            ImageCacheFactory.LOADER.execute(
                ImageRequest.Builder(context)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .data(imageUrl)
                    .size(1200)
                    .diskCacheKey(imageUrl.toString())
                    .build()
            )
        }
    }

    fun removeDownload(context: Context, mediaItem: MediaItem) {
        if (mediaItem.isLocal) return

        coroutineScope.launch {
            context.removeDownload<MyDownloadService>(mediaItem.mediaId).exceptionOrNull()?.let {
                if (it is CancellationException) throw it
                Timber.e(it.stackTraceToString())
                println("MyDownloadHelper removeDownload exception ${it.stackTraceToString()}")
            }
        }
    }

    fun resumeDownloads(context: Context) {
        DownloadService.sendResumeDownloads(
            context,
            MyDownloadService::class.java,
            false
        )
    }

    fun autoDownload(context: Context, mediaItem: MediaItem) {
        if (context.preferences.getBoolean(autoDownloadSongKey, false)) {
            if (downloads.value[mediaItem.mediaId]?.state != Download.STATE_COMPLETED)
                addDownload(context, mediaItem)
        }
    }

    fun autoDownloadWhenLiked(context: Context, mediaItem: MediaItem) {
        if (context.preferences.getBoolean(autoDownloadSongWhenLikedKey, false)) {
            Database.asyncQuery {
                runBlocking {
                    if(songTable.isLiked(mediaItem.mediaId).first())
                        autoDownload(context, mediaItem)
                    else
                        removeDownload(context, mediaItem)
                }
            }
        }
    }

    fun downloadOnLike(mediaItem: MediaItem, likeState: Boolean?, context: Context) {
        val isSettingEnabled = context.preferences.getBoolean(autoDownloadSongWhenLikedKey, false)
        if(!isSettingEnabled || !isNetworkConnected(context))
            return

        if(likeState == true)
            autoDownload(context, mediaItem)
        else
            removeDownload(context, mediaItem)
    }

    fun autoDownloadWhenAlbumBookmarked(context: Context, mediaItems: List<MediaItem>) {
        if (context.preferences.getBoolean(autoDownloadSongWhenAlbumBookmarkedKey, false)) {
            mediaItems.forEach { mediaItem ->
                autoDownload(context, mediaItem)
            }
        }
    }

    fun handleDownload(context: Context, song: Song, removeIfDownloaded: Boolean = false) {
        if(song.isLocal) return

        val isDownloaded =
            downloads.value.values.any{ it.state == Download.STATE_COMPLETED && it.request.id == song.id }

        if(isDownloaded && removeIfDownloaded)
            removeDownload(context, song.asMediaItem)
        else if(!isDownloaded)
            addDownload(context, song.asMediaItem)
    }

    fun isSongDownloaded(songId: String): Boolean {
        val download = downloads.value[songId]
        return download?.state == Download.STATE_COMPLETED
    }

    fun getDownloadedSongsCount(): Int {
        return downloads.value.count { it.value.state == Download.STATE_COMPLETED }
    }

    fun getDownloadedSongIds(): List<String> {
        return downloads.value.filter { it.value.state == Download.STATE_COMPLETED }.keys.toList()
    }
}
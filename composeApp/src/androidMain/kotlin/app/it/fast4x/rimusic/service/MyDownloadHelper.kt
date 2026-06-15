package app.it.fast4x.rimusic.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import app.it.fast4x.rimusic.enums.ExoPlayerDiskCacheMaxSize
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.audioQualityFormatKey
import app.it.fast4x.rimusic.utils.autoDownloadSongKey
import app.it.fast4x.rimusic.utils.autoDownloadSongWhenAlbumBookmarkedKey
import app.it.fast4x.rimusic.utils.autoDownloadSongWhenLikedKey
import app.it.fast4x.rimusic.utils.download
import app.it.fast4x.rimusic.utils.downloadSyncedLyrics
import app.it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import app.it.fast4x.rimusic.utils.exoPlayerCustomCacheKey
import app.it.fast4x.rimusic.utils.exoPlayerDiskDownloadCacheMaxSizeKey
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.isNetworkConnected
import app.it.fast4x.rimusic.utils.playbackVideoIdOrNull
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.removeDownload
import app.it.fast4x.rimusic.utils.sanitizePlaybackUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import app.cubic.android.core.coil.thumbnail
import java.io.File
import androidx.core.net.toUri
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory
import app.it.fast4x.rimusic.utils.ExternalUris

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
    const val CUSTOM_DOWNLOAD_URI_KEY = "custom_download_uri"
    const val CUSTOM_DOWNLOAD_PATH_KEY = "custom_download_path"

    private lateinit var databaseProvider: DatabaseProvider
    lateinit var downloadCache: Cache

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper
    private lateinit var downloadManager: DownloadManager
    lateinit var audioQualityFormat: AudioQualityFormat

    var downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    private val mutableProgresses = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progresses = mutableProgresses.asStateFlow()
    private val mutableBulkDownloadIds = MutableStateFlow<Set<String>>(emptySet())
    val bulkDownloadIds = mutableBulkDownloadIds.asStateFlow()
    private var progressLoopStarted = false
    private val mirrorJobs = mutableSetOf<String>()

    fun getDownload(songId: String): Flow<Download?> {
        return downloads.map { it[songId] }
    }

    fun startBulkDownloadSession(songIds: Collection<String>) {
        mutableBulkDownloadIds.value = songIds
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    }

    fun clearBulkDownloadSession() {
        mutableBulkDownloadIds.value = emptySet()
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
            else -> getDownloadCacheDirectory(context)
        }

        // Ensure this location exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        return SimpleCache(cacheDir, cacheEvictor, getDatabaseProvider(context))
    }

    fun getDownloadCacheDirectory(context: Context): File {
        return defaultDownloadRootDirectory(context)
    }

    fun getDownloadRootDirectory(context: Context): File {
        return defaultDownloadRootDirectory(context)
    }

    fun hasCustomDownloadStorage(context: Context): Boolean =
        context.preferences.getString(CUSTOM_DOWNLOAD_URI_KEY, "").isNullOrBlank().not()

    fun clearCustomDownloadStorage(context: Context) {
        context.preferences.edit()
            .remove(CUSTOM_DOWNLOAD_URI_KEY)
            .remove(CUSTOM_DOWNLOAD_PATH_KEY)
            .apply()
    }

    fun getCustomDownloadTreeUri(context: Context): Uri? =
        context.preferences.getString(CUSTOM_DOWNLOAD_URI_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?.toUri()

    fun getCustomDownloadFolderLabel(context: Context): String =
        context.preferences.getString(CUSTOM_DOWNLOAD_PATH_KEY, "").orEmpty()

private fun defaultDownloadRootDirectory(context: Context): File {
    val selectedLocation = context.preferences.getEnum(exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System)
    val preferredDir = when (selectedLocation) {
        ExoPlayerCacheLocation.System -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        ExoPlayerCacheLocation.Private -> context.filesDir
    }

    if (preferredDir.looksLikeExoCacheDirectory()) return preferredDir

    val alternateDir = when (selectedLocation) {
        ExoPlayerCacheLocation.System -> context.filesDir
        ExoPlayerCacheLocation.Private -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
    }
    if (alternateDir.looksLikeExoCacheDirectory()) return alternateDir

    val legacyDir = File(Environment.getExternalStorageDirectory(), ROOT_DOWNLOAD_DIR)
    if (legacyDir.looksLikeExoCacheDirectory()) return legacyDir

    return preferredDir
}

private fun File.looksLikeExoCacheDirectory(): Boolean =
    exists() && isDirectory && (listFiles()?.any { file ->
        file.isFile && (
            file.name.endsWith(".exo", ignoreCase = true) ||
            file.name.startsWith("cached_content_index", ignoreCase = true) ||
            file.name.endsWith(".uid", ignoreCase = true)
        )
    } == true)

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
                getDownloadCacheDirectory(context).absolutePath
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
            downloadManager = createDownloadManager(context)
            getDownloads()
            if (!progressLoopStarted) {
                progressLoopStarted = true
                coroutineScope.launch {
                    while (isActive) {
                        val currentDownloads = downloadManager.currentDownloads
                        if (currentDownloads.isNotEmpty()) {
                            mutableProgresses.update { progresses ->
                                progresses.toMutableMap().apply {
                                    currentDownloads.forEach { download ->
                                        val progress = if (download.contentLength > 0) {
                                            download.bytesDownloaded.toFloat() / download.contentLength
                                        } else {
                                            0f
                                        }
                                        put(download.request.id, progress)
                                    }
                                }
                            }
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    @Synchronized
    fun reinitializeDownloadStorage(context: Context) {
        audioQualityFormat =
            context.preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)
        runCatching {
            if (MyDownloadHelper::downloadManager.isInitialized) {
                downloadManager.pauseDownloads()
            }
        }.onFailure {
            Timber.w(it, "Failed to pause downloads before storage refresh")
        }
        runCatching {
            if (MyDownloadHelper::downloadCache.isInitialized) {
                downloadCache.release()
            }
        }.onFailure {
            Timber.w(it, "Failed to release old download cache during storage refresh")
        }
        downloadCache = initDownloadCache(context)
        downloadManager = createDownloadManager(context)
        getDownloads()
    }

    private fun createDownloadManager(context: Context): DownloadManager =
        DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                createDataSourceFactory(),
                executor
            ).apply {
                maxParallelDownloads = 3
                minRetryCount = 1
                requirements = Requirements(Requirements.NETWORK)

                addListener(
                    object : DownloadManager.Listener {
                        override fun onDownloadChanged(
                            downloadManager: DownloadManager,
                            download: Download,
                        finalException: Exception?
                        ) = run {
                            syncDownloads(download)
                            if (download.state == Download.STATE_COMPLETED) {
                                maybeMirrorCompletedDownload(context, download)
                            }
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

    private fun maybeMirrorCompletedDownload(context: Context, download: Download) {
        if (!hasCustomDownloadStorage(context)) return
        synchronized(mirrorJobs) {
            if (!mirrorJobs.add(download.request.id)) return
        }
        coroutineScope.launch {
            runCatching {
                mirrorDownloadedSongToCustomStorage(
                    context = context,
                    songId = download.request.id,
                    fallbackName = download.request.data?.decodeToString()?.trim().orEmpty()
                )
            }.onFailure {
                Timber.w(it, "Failed mirroring downloaded song %s to custom folder", download.request.id)
            }
            synchronized(mirrorJobs) {
                mirrorJobs.remove(download.request.id)
            }
        }
    }

    suspend fun mirrorDownloadedSongToCustomStorage(
        context: Context,
        songId: String,
        fallbackName: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val treeUri = getCustomDownloadTreeUri(context) ?: return@withContext false
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
        val spans = getDownloadCache(context)
            .getCachedSpans(songId)
            .sortedBy(CacheSpan::position)
            .mapNotNull(CacheSpan::file)

        if (spans.isEmpty()) return@withContext false

        val outputName = buildMirroredDownloadFileName(songId, fallbackName)
        root.findFile(outputName)?.delete()
        val outputFile = root.createFile("audio/mp4", outputName)
            ?: return@withContext false

        context.contentResolver.openOutputStream(outputFile.uri, "w")?.use { outputStream ->
            spans.forEach { cacheFile ->
                cacheFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } ?: return@withContext false

        true
    }

    fun getCustomDownloadFileCount(context: Context): Int {
        val treeUri = getCustomDownloadTreeUri(context) ?: return 0
        return runCatching {
            DocumentFile.fromTreeUri(context, treeUri)
                ?.listFiles()
                ?.count { it.isFile }
                ?: 0
        }.getOrDefault(0)
    }

    private fun buildMirroredDownloadFileName(songId: String, fallbackName: String): String {
        val cleanedName = fallbackName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { songId }
        return "$cleanedName.m4a"
    }

    @Synchronized
    private fun syncDownloads(download: Download) {
        downloads.update { map ->
            map.toMutableMap().apply {
                set(download.request.id, download)
            }
        }
        trimBulkDownloadSession()
    }

    private fun trimBulkDownloadSession() {
        val currentIds = mutableBulkDownloadIds.value
        if (currentIds.isEmpty()) return

        val activeIds = currentIds.filterTo(mutableSetOf()) { songId ->
            when (downloads.value[songId]?.state) {
                Download.STATE_QUEUED,
                Download.STATE_DOWNLOADING,
                Download.STATE_RESTARTING,
                Download.STATE_STOPPED -> true
                else -> false
            }
        }

        if (activeIds != currentIds) {
            mutableBulkDownloadIds.value = activeIds
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

        val videoId = mediaItem.playbackVideoIdOrNull()
        if (videoId.isNullOrBlank()) {
            Timber.w("Ignoring download request with non-video mediaId=%s", mediaItem.mediaId)
            return
        }

        val safeMediaItem = mediaItem.buildUpon()
            .setMediaId(videoId)
            .setUri(sanitizePlaybackUri(videoId))
            .setCustomCacheKey(videoId)
            .build()

        val downloadRequest = DownloadRequest
            .Builder(
                /* id      = */ videoId,
                /* uri     = */ safeMediaItem.requestMetadata.mediaUri
                    ?: Uri.parse(ExternalUris.youtubeMusic(videoId))
            )
            .setCustomCacheKey(videoId)
            .setData("${safeMediaItem.mediaMetadata.artist ?: ""} - ${safeMediaItem.mediaMetadata.title ?: ""}".encodeToByteArray()) // Title in notification
            .build()

       coroutineScope.launch(Dispatchers.IO) {
    Database.upsert(safeMediaItem)
}

         val imageUrl = mediaItem.mediaMetadata.artworkUri?.toString()?.thumbnail(1000)?.toUri()

coroutineScope.launch {
    // 1. Enqueue the download (non‑blocking)
    context.download<MyDownloadService>(downloadRequest).exceptionOrNull()?.let {
        if (it is CancellationException) throw it
        Timber.e("MyDownloadHelper scheduleDownload exception ${it.stackTraceToString()}")
    }

    // 2. Do the rest in a separate coroutine – they don’t need to finish for the download to start
    launch {
        downloadSyncedLyrics(safeMediaItem.asSong)
        ImageCacheFactory.preloadImage(safeMediaItem.mediaMetadata.artworkUri?.toString())
    }
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

    fun redownloadSong(context: Context, mediaItem: MediaItem) {
        if (mediaItem.isLocal) return

        if (!isNetworkConnected(context)) {
            Toaster.noInternet()
            return
        }

        coroutineScope.launch {
            runCatching { getDownloadCache(context).removeResource(mediaItem.mediaId) }
                .onFailure { Timber.w(it, "Failed clearing corrupt cache for %s", mediaItem.mediaId) }

            Database.asyncTransaction {
                formatTable.deleteBySongId(mediaItem.mediaId)
            }

            context.removeDownload<MyDownloadService>(mediaItem.mediaId).exceptionOrNull()?.let {
                if (it is CancellationException) throw it
                Timber.w(it, "Failed removing old download before re-download for %s", mediaItem.mediaId)
            }

            delay(350)
            addDownload(context, mediaItem)
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

    fun isDownloadCached(songId: String): Boolean {
        if (!MyDownloadHelper::downloadCache.isInitialized) return false
        return runCatching { downloadCache.getCachedSpans(songId).isNotEmpty() }.getOrDefault(false)
    }

    fun getDownloadedSongsCount(): Int {
        return downloads.value.count { it.value.state == Download.STATE_COMPLETED }
    }

    fun getDownloadedSongIds(): List<String> {
        return downloads.value.filter { it.value.state == Download.STATE_COMPLETED }.keys.toList()
    }
}

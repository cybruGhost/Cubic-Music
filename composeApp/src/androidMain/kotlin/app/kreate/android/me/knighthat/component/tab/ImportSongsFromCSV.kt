package app.kreate.android.me.knighthat.component.tab

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.kreate.android.R
import app.kreate.android.exception.InvalidHeaderException
import app.kreate.android.me.knighthat.component.ImportFromFile
import app.kreate.android.me.knighthat.utils.DurationUtils
import app.kreate.android.me.knighthat.utils.csv.CSVLocaleManager
import app.kreate.android.me.knighthat.utils.csv.SongCSV
import androidx.documentfile.provider.DocumentFile
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private enum class CsvImportEntryStatus {
    Converting,
    Success,
    Fallback,
    Failed
}

private data class CsvImportConversionEntry(
    val index: Int,
    val title: String,
    val artists: String,
    val status: CsvImportEntryStatus,
    val message: String
)

private data class CsvImportConversionState(
    val isVisible: Boolean = false,
    val isMinimized: Boolean = false,
    val isRunning: Boolean = false,
    val fileName: String = "",
    val playlistName: String = "",
    val detectedLanguage: String = "",
    val bannerMessage: String = "",
    val total: Int = 0,
    val processed: Int = 0,
    val successCount: Int = 0,
    val fallbackCount: Int = 0,
    val failedCount: Int = 0,
    val entries: List<CsvImportConversionEntry> = emptyList()
)

private object CsvImportConversionTracker {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(CsvImportConversionState())
    val state: StateFlow<CsvImportConversionState> = _state.asStateFlow()

    suspend fun start(fileName: String, total: Int, detectedLanguage: String) = mutex.withLock {
        _state.value = CsvImportConversionState(
            isVisible = true,
            isMinimized = false,
            isRunning = true,
            fileName = fileName,
            detectedLanguage = detectedLanguage,
            total = total
        )
    }

    suspend fun setPlaylistName(playlistName: String) = mutex.withLock {
        _state.value = _state.value.copy(playlistName = playlistName)
    }

    suspend fun markConverting(index: Int, title: String, artists: String, message: String) = mutex.withLock {
        _state.value = _state.value.copy(
            entries = _state.value.entries.upsert(
                CsvImportConversionEntry(
                    index = index,
                    title = title,
                    artists = artists,
                    status = CsvImportEntryStatus.Converting,
                    message = message
                )
            )
        )
    }

    suspend fun markFinished(
        index: Int,
        title: String,
        artists: String,
        status: CsvImportEntryStatus,
        message: String
    ) = mutex.withLock {
        val current = _state.value
        _state.value = current.copy(
            processed = current.processed + 1,
            successCount = current.successCount + if (status == CsvImportEntryStatus.Success) 1 else 0,
            fallbackCount = current.fallbackCount + if (status == CsvImportEntryStatus.Fallback) 1 else 0,
            failedCount = current.failedCount + if (status == CsvImportEntryStatus.Failed) 1 else 0,
            entries = current.entries.upsert(
                CsvImportConversionEntry(
                    index = index,
                    title = title,
                    artists = artists,
                    status = status,
                    message = message
                )
            )
        )
    }

    suspend fun finish() = mutex.withLock {
        _state.value = _state.value.copy(isRunning = false, isVisible = true)
    }

    suspend fun setBannerMessage(message: String) = mutex.withLock {
        _state.value = _state.value.copy(bannerMessage = message)
    }

    fun minimize() {
        _state.value = _state.value.copy(isMinimized = true, isVisible = true)
    }

    fun expand() {
        _state.value = _state.value.copy(isMinimized = false, isVisible = true)
    }

    fun dismiss() {
        _state.value = CsvImportConversionState()
    }

    fun closePanel() {
        _state.value = _state.value.copy(isMinimized = true, isVisible = true)
    }

    private fun List<CsvImportConversionEntry>.upsert(entry: CsvImportConversionEntry): List<CsvImportConversionEntry> {
        val mutable = toMutableList()
        val existingIndex = mutable.indexOfFirst { it.index == entry.index }
        if (existingIndex >= 0) mutable[existingIndex] = entry else mutable += entry
        return mutable.sortedBy { it.index }
    }
}

private enum class CsvYoutubeMatchSource {
    Song,
    Video,
    Omada
}

private data class CsvYoutubeMatch(
    val song: Song,
    val source: CsvYoutubeMatchSource
)

private data class CsvParseResult(
    val songs: List<SongCSV>,
    val successCount: Int,
    val fallbackCount: Int,
    val failedCount: Int
)

class ImportSongsFromCSV(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : ImportFromFile(launcher), MenuIcon, Descriptive {

    companion object {
        private fun sanitizeCsvFileName(fileName: String): String =
            fileName.substringAfterLast('/')
                .substringAfterLast(':')
                .removeSuffix(".csv")
                .replace('_', ' ')
                .trim()
                .ifBlank { "Imported playlist" }

        private fun hasInternetConnection(): Boolean = try {
            val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "HEAD"
            connection.connect()
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (_: Exception) {
            false
        }

        private suspend fun fetchYoutubeVideoId(query: String, maxRetries: Int = 3): String? {
            if (!hasInternetConnection()) {
                throw IllegalStateException("No internet connection")
            }

            var lastException: Exception? = null
            repeat(maxRetries) { attempt ->
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val connection = (URL("https://yt.omada.cafe/api/v1/search?q=$encoded&type=video").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15_000
                        readTimeout = 15_000
                    }

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("HTTP ${connection.responseCode}")
                    }

                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val results = org.json.JSONArray(responseText)
                    if (results.length() == 0) {
                        throw IllegalStateException("No video found for: $query")
                    }

                    return results.getJSONObject(0).optString("videoId").takeIf { it.isNotBlank() }
                } catch (error: Exception) {
                    lastException = error
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(2_000L * (attempt + 1))
                    }
                }
            }

            throw lastException ?: IllegalStateException("Failed to convert: $query")
        }

        private suspend fun searchInnertubeMatch(query: String, maxRetries: Int = 2): CsvYoutubeMatch? {
            var lastError: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    val songPage = Innertube.searchPage(
                        body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                        fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
                    )?.getOrThrow()

                    val songItem = songPage?.items
                        ?.filterIsInstance<Innertube.SongItem>()
                        ?.firstOrNull { it.key.isNotBlank() && !it.title.isNullOrBlank() }

                    if (songItem != null) {
                        return CsvYoutubeMatch(
                            song = songItem.asSong.copy(title = cleanPrefix(songItem.asSong.title)),
                            source = CsvYoutubeMatchSource.Song
                        )
                    }

                    val videoPage = Innertube.searchPage(
                        body = SearchBody(query = query, params = Innertube.SearchFilter.Video.value),
                        fromMusicShelfRendererContent = { content -> Innertube.VideoItem.from(content) }
                    )?.getOrThrow()

                    val videoItem = videoPage?.items
                        ?.filterIsInstance<Innertube.VideoItem>()
                        ?.firstOrNull { it.key.isNotBlank() && !it.title.isNullOrBlank() }

                    if (videoItem != null) {
                        return CsvYoutubeMatch(
                            song = videoItem.asSong.copy(title = cleanPrefix(videoItem.asSong.title)),
                            source = CsvYoutubeMatchSource.Video
                        )
                    }
                } catch (error: Exception) {
                    lastError = error
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(1_000L * (attempt + 1))
                    }
                }
            }

            if (lastError != null) {
                throw lastError
            }
            return null
        }

        private suspend fun parseFromCsvFile(inputStream: InputStream, fileName: String): CsvParseResult {
            val rows = csvReader { skipEmptyLine = true }.readAllWithHeader(inputStream)
            CSVLocaleManager.initialize(appContext())

            val translatedRows = rows.map(CSVLocaleManager::normalizeRow)
            val originalHeaders = rows.firstOrNull()?.keys.orEmpty()
            val detectedLocale = CSVLocaleManager.detectLocale(originalHeaders)
            val detectedLocaleName = detectedLocale?.let(CSVLocaleManager::getSimpleLocaleName).orEmpty()

            CsvImportConversionTracker.start(
                fileName = fileName,
                total = translatedRows.size,
                detectedLanguage = detectedLocaleName
            )

            val headers = translatedRows.firstOrNull()?.keys.orEmpty()
            val hasCustomFormat = headers.containsAll(
                setOf("PlaylistBrowseId", "PlaylistName", "MediaId", "Title", "Artists", "Duration")
            )
            val hasSpotifyFormat = headers.containsAll(
                setOf("Track Name", "Artist Name(s)")
            )
            val hasExportifyFormat = headers.containsAll(
                setOf("Track URI", "Track Name", "Artist Name(s)", "Album Name")
            )
            val hasYourFormat = headers.containsAll(
                setOf("PlaylistBrowseId", "PlaylistName", "MediaId", "Title", "Artists", "Duration", "ThumbnailUrl", "AlbumId", "AlbumTitle", "ArtistIds")
            )

            if (!hasCustomFormat && !hasSpotifyFormat && !hasExportifyFormat && !hasYourFormat) {
                CsvImportConversionTracker.setBannerMessage(
                    appContext().getString(R.string.error_message_unsupported_local_playlist)
                )
                CsvImportConversionTracker.finish()
                throw InvalidHeaderException("Unsupported CSV format")
            }

            val converted = mutableListOf<SongCSV>()
            var successCount = 0
            var fallbackCount = 0
            var failCount = 0

            val csvPlaylistName = translatedRows.firstOrNull()?.get("PlaylistName") ?: ""
            val playlistName = if (csvPlaylistName.isNotBlank()) {
                csvPlaylistName
            } else {
                sanitizeCsvFileName(fileName)
            }

            CsvImportConversionTracker.setPlaylistName(playlistName)

            translatedRows.forEachIndexed { index, row ->
                val rowTitle = row["Title"].orEmpty().ifBlank { row["Track Name"].orEmpty() }
                val rowArtists = row["Artists"].orEmpty().ifBlank { row["Artist Name(s)"].orEmpty() }

                try {
                    val isSpotifyFormat = row.containsKey("Track Name") && row.containsKey("Artist Name(s)")
                    val isExportifyFormat = row.containsKey("Track URI") && row.containsKey("Track Name")
                    val isYourFormat = row.containsKey("PlaylistBrowseId") && row.containsKey("PlaylistName") &&
                        row.containsKey("MediaId") && row.containsKey("Title") &&
                        row.containsKey("Artists") && row.containsKey("Duration") &&
                        row.containsKey("ThumbnailUrl") && row.containsKey("AlbumId") &&
                        row.containsKey("AlbumTitle") && row.containsKey("ArtistIds")
                    val isCustomFormat = row.containsKey("PlaylistBrowseId") && row.containsKey("PlaylistName") &&
                        row.containsKey("MediaId") && row.containsKey("Title") &&
                        row.containsKey("Artists") && row.containsKey("Duration")

                    if (isExportifyFormat || isSpotifyFormat) {
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val title = row["Track Name"].orEmpty()
                        val artists = row["Artist Name(s)"].orEmpty()
                        val cleanArtists = artists.split(", ")
                            .joinToString(", ") { artist -> artist.split("spotify:artist:").last().trim() }
                        val query = "$title $cleanArtists".trim()

                        if (query.isBlank() || title.isBlank()) {
                            failCount++
                            CsvImportConversionTracker.markFinished(
                                index = index,
                                title = rowTitle,
                                artists = rowArtists,
                                status = CsvImportEntryStatus.Failed,
                                message = appContext().getString(R.string.csv_import_status_failed_missing_query)
                            )
                            return@forEachIndexed
                        }

                        CsvImportConversionTracker.markConverting(
                            index = index,
                            title = title,
                            artists = cleanArtists,
                            message = appContext().getString(
                                R.string.csv_import_status_converting_row,
                                index + 1,
                                translatedRows.size
                            )
                        )

                        try {
                            val omadaVideoId = runCatching { fetchYoutubeVideoId(query) }.getOrNull()
                            val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                            val convertedDuration = if (rawDurationMs > 0) formatAsDuration(rawDurationMs) else "0"

                            when {
                                !omadaVideoId.isNullOrBlank() -> {
                                    converted += SongCSV(
                                        songId = omadaVideoId,
                                        playlistBrowseId = "",
                                        playlistName = playlistName,
                                        title = explicitPrefix + title,
                                        artists = cleanArtists,
                                        duration = convertedDuration,
                                        thumbnailUrl = "https://yt.omada.cafe/vi/$omadaVideoId/hqdefault.jpg"
                                    )
                                    successCount++
                                    CsvImportConversionTracker.markFinished(
                                        index = index,
                                        title = title,
                                        artists = cleanArtists,
                                        status = CsvImportEntryStatus.Success,
                                        message = appContext().getString(R.string.csv_import_status_success_omada)
                                    )
                                }

                                else -> {
                                    val innertubeMatch = searchInnertubeMatch(query)
                                        ?: throw IllegalStateException("No conversion match found")

                                    converted += SongCSV(
                                        songId = innertubeMatch.song.id,
                                        playlistBrowseId = "",
                                        playlistName = playlistName,
                                        title = explicitPrefix + title,
                                        artists = cleanArtists,
                                        duration = convertedDuration,
                                        thumbnailUrl = innertubeMatch.song.thumbnailUrl.orEmpty()
                                    )
                                    successCount++
                                    val messageRes = when (innertubeMatch.source) {
                                        CsvYoutubeMatchSource.Song -> R.string.csv_import_status_success_song
                                        CsvYoutubeMatchSource.Video -> R.string.csv_import_status_success_video
                                        CsvYoutubeMatchSource.Omada -> R.string.csv_import_status_success_omada
                                    }
                                    CsvImportConversionTracker.markFinished(
                                        index = index,
                                        title = title,
                                        artists = cleanArtists,
                                        status = CsvImportEntryStatus.Success,
                                        message = appContext().getString(messageRes)
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            val encodedSearch = URLEncoder.encode(query, "UTF-8")
                            val fallbackSongId = "search:$encodedSearch"
                            val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                            val convertedDuration = if (rawDurationMs > 0) formatAsDuration(rawDurationMs) else "0"

                            converted += SongCSV(
                                songId = fallbackSongId,
                                playlistBrowseId = "",
                                playlistName = playlistName,
                                title = explicitPrefix + title,
                                artists = cleanArtists,
                                duration = convertedDuration,
                                thumbnailUrl = row["Album Image URL"].orEmpty()
                            )
                            fallbackCount++
                            CsvImportConversionTracker.markFinished(
                                index = index,
                                title = title,
                                artists = cleanArtists,
                                status = CsvImportEntryStatus.Fallback,
                                message = appContext().getString(R.string.csv_import_status_fallback_search)
                            )
                        }

                        kotlinx.coroutines.delay(1_000L)
                    } else if (isYourFormat || isCustomFormat) {
                        var browseId = row["PlaylistBrowseId"].orEmpty()
                        if (browseId.toLongOrNull() != null) browseId = ""

                        val rawDuration = row["Duration"].orEmpty()
                        val convertedDuration = when {
                            rawDuration.isBlank() -> "0"
                            !DurationUtils.isHumanReadable(rawDuration) -> formatAsDuration(rawDuration.toLong().times(1000))
                            else -> rawDuration
                        }

                        val mediaId = row["MediaId"].orEmpty()
                        if (mediaId.isNotBlank()) {
                            converted += SongCSV(
                                songId = mediaId,
                                playlistBrowseId = browseId,
                                playlistName = playlistName,
                                title = row["Title"].orEmpty(),
                                artists = row["Artists"].orEmpty(),
                                duration = convertedDuration,
                                thumbnailUrl = row["ThumbnailUrl"].orEmpty()
                            )
                            successCount++
                            CsvImportConversionTracker.markFinished(
                                index = index,
                                title = row["Title"].orEmpty(),
                                artists = row["Artists"].orEmpty(),
                                status = CsvImportEntryStatus.Success,
                                message = appContext().getString(R.string.csv_import_status_kept_media_id)
                            )
                        } else {
                            failCount++
                            CsvImportConversionTracker.markFinished(
                                index = index,
                                title = row["Title"].orEmpty(),
                                artists = row["Artists"].orEmpty(),
                                status = CsvImportEntryStatus.Failed,
                                message = appContext().getString(R.string.csv_import_status_failed_missing_media_id)
                            )
                        }
                    }
                } catch (_: Exception) {
                    failCount++
                    CsvImportConversionTracker.markFinished(
                        index = index,
                        title = rowTitle,
                        artists = rowArtists,
                        status = CsvImportEntryStatus.Failed,
                        message = appContext().getString(R.string.csv_import_status_failed_generic)
                    )
                }
            }

            CsvImportConversionTracker.finish()

            return CsvParseResult(
                songs = converted,
                successCount = successCount,
                fallbackCount = fallbackCount,
                failedCount = failCount
            )
        }

        private fun getSimpleLocaleName(locale: String): String = when (locale) {
            "de" -> "Deutsch"
            "en" -> "English"
            "es" -> "Español"
            "fr" -> "Français"
            "it" -> "Italiano"
            "nl" -> "Nederlands"
            "pt" -> "Português"
            "sv" -> "Svenska"
            "ar" -> "العربية"
            "ja" -> "日本語"
            "tr" -> "Türkçe"
            else -> locale
        }

        private fun processSongs(songs: List<SongCSV>): Map<Pair<String, String>, List<Song>> =
            songs.fastFilter { it.songId.isNotBlank() }
                .groupBy { it.playlistName to it.playlistBrowseId }
                .mapValues { (_, groupedSongs) ->
                    groupedSongs.fastMap {
                        Song(
                            id = it.songId,
                            title = it.title,
                            artistsText = it.artists,
                            thumbnailUrl = it.thumbnailUrl,
                            durationText = it.duration,
                            totalPlayTimeMs = 1L
                        )
                    }
                }

        @Composable
        operator fun invoke(): ImportSongsFromCSV {
            val scope = rememberCoroutineScope()

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult

                val fileName = DocumentFile.fromSingleUri(appContext(), uri)?.name
                    ?: uri.lastPathSegment
                    ?: "imported_playlist.csv"

                scope.launch {
                    CsvImportConversionTracker.start(
                        fileName = fileName,
                        total = 1,
                        detectedLanguage = ""
                    )
                    CsvImportConversionTracker.setBannerMessage(
                        appContext().getString(R.string.csv_import_conversion_running)
                    )
                }

                scope.launch(Dispatchers.IO) {
                    val straySongs = mutableListOf<Song>()
                    val combos = mutableMapOf<Playlist, List<Song>>()

                    try {
                        val parseResult = appContext().contentResolver
                            .openInputStream(uri)
                            ?.use { stream -> parseFromCsvFile(stream, fileName) }
                            ?: CsvParseResult(emptyList(), 0, 0, 0)

                        if (parseResult.songs.isEmpty()) {
                            CsvImportConversionTracker.setBannerMessage(
                                appContext().getString(R.string.csv_import_no_valid_songs)
                            )
                            CsvImportConversionTracker.finish()
                            return@launch
                        }

                        val processedSongs = processSongs(parseResult.songs)
                        processedSongs.forEach { (playlist, songs) ->
                            if (playlist.first.isNotBlank()) {
                                combos[Playlist(name = playlist.first, browseId = playlist.second)] = songs
                            } else {
                                straySongs.addAll(songs)
                            }
                        }

                        if (combos.isEmpty() && straySongs.isEmpty()) {
                            CsvImportConversionTracker.setBannerMessage(
                                appContext().getString(R.string.csv_import_no_valid_songs)
                            )
                            CsvImportConversionTracker.finish()
                            return@launch
                        }

                        Database.asyncTransaction {
                            val allSongs = straySongs + combos.values.flatten()
                            songTable.upsert(allSongs)
                            combos.forEach { (playlist, songs) ->
                                mapIgnore(playlist, *songs.toTypedArray())
                            }
                        }

                        val allSongs = straySongs + combos.values.flatten()
                        val playableSongs = allSongs.count { it.id.isNotBlank() && !it.id.startsWith("search:") }
                        val playlistName = combos.keys.firstOrNull()?.name ?: sanitizeCsvFileName(fileName)

                        CsvImportConversionTracker.setBannerMessage(
                            appContext().getString(
                                R.string.csv_import_summary_complete,
                                playlistName,
                                playableSongs,
                                allSongs.size
                            )
                        )
                        CsvImportConversionTracker.finish()
                    } catch (error: Exception) {
                        when (error) {
                            is InvalidHeaderException -> CsvImportConversionTracker.setBannerMessage(
                                appContext().getString(R.string.error_message_unsupported_local_playlist)
                            )
                            else -> CsvImportConversionTracker.setBannerMessage(
                                appContext().getString(
                                    R.string.csv_import_failed_with_reason,
                                    error.message ?: appContext().getString(R.string.csv_import_failed_unknown_reason)
                                )
                            )
                        }
                        CsvImportConversionTracker.finish()
                    }
                }
            }

            return remember(launcher) { ImportSongsFromCSV(launcher) }
        }
    }

    override val supportedMimes: Array<String> = arrayOf("text/csv", "text/comma-separated-values")
    override val iconId: Int = R.drawable.import_outline
    override val messageId: Int = R.string.import_playlist
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)
}

@Composable
fun CsvImportConversionHost() {
    val conversionState by CsvImportConversionTracker.state.collectAsState()
    CsvImportConversionOverlay(conversionState)
}

@Composable
private fun CsvImportConversionOverlay(state: CsvImportConversionState) {
    if (!state.isVisible) return

    if (state.isMinimized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Card(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 96.dp)
                    .zIndex(20f)
                    .clickable { CsvImportConversionTracker.expand() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.import_outline),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.csv_import_conversion_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.csv_import_progress_compact, state.processed, state.total),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!state.isRunning) {
                        TextButton(onClick = { CsvImportConversionTracker.dismiss() }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .zIndex(20f),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, bottom = 96.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.csv_import_conversion_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (state.isRunning) {
                        stringResource(R.string.csv_import_conversion_running)
                    } else {
                        stringResource(R.string.csv_import_conversion_finished)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.fileName.isNotBlank()) {
                    Text(
                        text = state.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (state.playlistName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.csv_import_playlist_name, state.playlistName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.detectedLanguage.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.csv_import_detected_language, state.detectedLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.bannerMessage.isNotBlank()) {
                    Text(
                        text = state.bannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val progress = if (state.total == 0) 0f else state.processed.toFloat() / state.total.toFloat()
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.csv_import_progress_full, state.processed, state.total),
                    style = MaterialTheme.typography.bodySmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CsvImportSummaryChip(
                        label = stringResource(R.string.csv_import_success_count, state.successCount),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    CsvImportSummaryChip(
                        label = stringResource(R.string.csv_import_fallback_count, state.fallbackCount),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    CsvImportSummaryChip(
                        label = stringResource(R.string.csv_import_failed_count, state.failedCount),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }

                val latestEntry = state.entries.lastOrNull()
                Text(
                    text = latestEntry?.message ?: stringResource(R.string.csv_import_no_entries_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { CsvImportConversionTracker.minimize() }) {
                        Text(stringResource(R.string.csv_import_minimize))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!state.isRunning) {
                        TextButton(onClick = { CsvImportConversionTracker.closePanel() }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvImportSummaryChip(
    label: String,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

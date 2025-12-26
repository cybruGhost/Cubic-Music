package me.knighthat.component.tab

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import app.kreate.android.R
import app.kreate.android.exception.InvalidHeaderException
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.utils.formatAsDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.ImportFromFile
import me.knighthat.utils.DurationUtils
import me.knighthat.utils.Toaster
import me.knighthat.utils.csv.SongCSV
import org.json.JSONArray
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ImportSongsFromCSV(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : ImportFromFile(launcher), MenuIcon, Descriptive {

    companion object {

        /** 🔹 Check internet connection */
        private fun hasInternetConnection(): Boolean {
            return try {
                val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "HEAD"
                connection.connect()
                connection.responseCode == HttpURLConnection.HTTP_OK
            } catch (e: Exception) {
                false
            }
        }

        /** 🔹 Fetch YouTube video ID with retry mechanism - USING WORKING API */
        private suspend fun fetchYoutubeVideoId(query: String, maxRetries: Int = 3): String? {
            // Check internet first
            if (!hasInternetConnection()) {
                throw Exception("No internet connection")
            }

            var lastException: Exception? = null
            
            for (attempt in 1..maxRetries) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    // Use the same API endpoint that works in your web app
                    val url = URL("https://yt.omada.cafe/api/v1/search?q=$encoded&type=video")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    
                    val code = connection.responseCode
                    if (code != HttpURLConnection.HTTP_OK) {
                        throw Exception("HTTP $code - API unavailable")
                    }

                    connection.inputStream.bufferedReader().use { reader ->
                        val json = reader.readText()
                        val array = JSONArray(json)
                        if (array.length() == 0) {
                            throw Exception("No video found for: $query")
                        }
                        val video = array.getJSONObject(0)
                        return video.getString("videoId")
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(2000L * attempt)
                    }
                }
            }
            
            throw lastException ?: Exception("Failed to convert: $query")
        }

        /** 🔹 Parse CSV and convert songs one by one with YouTube conversion */
        private suspend fun parseFromCsvFile(inputStream: InputStream, fileName: String): List<SongCSV> {
            val rows = csvReader { skipEmptyLine = true }.readAllWithHeader(inputStream)
            
            // Check CSV format
            val headers = rows.firstOrNull()?.keys.orEmpty()
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
                throw InvalidHeaderException("Unsupported CSV format")
            }

            val converted = mutableListOf<SongCSV>()
            var successCount = 0
            var failCount = 0

            // Get playlist name from CSV - use PlaylistName from CSV if available, otherwise use filename
            val csvPlaylistName = rows.firstOrNull()?.get("PlaylistName") ?: ""
            val playlistName = if (csvPlaylistName.isNotBlank()) {
                csvPlaylistName
            } else {
                fileName.replace(".csv", "").replace("_", " ").trim()
            }

            // Process rows one by one
            for ((index, row) in rows.withIndex()) {
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
                        // Handle Spotify/Exportify formats with YouTube conversion
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val title = row["Track Name"].orEmpty()
                        val artists = row["Artist Name(s)"].orEmpty()
                        
                        // Clean up artist names (remove Spotify URIs if present)
                        val cleanArtists = artists.split(", ")
                            .joinToString(", ") { artist ->
                                artist.split("spotify:artist:").last().trim()
                            }
                        
                        val query = "$title $cleanArtists".trim()

                        if (query.isNotBlank() && title.isNotBlank()) {
                            // Show progress
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.i("Converting ${index + 1}/${rows.size}: $title")
                            }

                            try {
                                // Convert to YouTube URL
                                val videoId = fetchYoutubeVideoId(query)
                                
                                // Use the videoId as MediaId (no YouTube: prefix)
                                val mediaId = videoId ?: ""
                                val songId = mediaId // Use raw video ID as song ID

                                // Convert duration from ms to proper format
                                val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                                val convertedDuration = if (rawDurationMs > 0) {
                                    formatAsDuration(rawDurationMs)
                                } else {
                                    "0"
                                }

                                // Create proper thumbnail URL
                                val thumbnailUrl = "https://yt.omada.cafe/vi/$videoId/hqdefault.jpg"

                                converted.add(
                                    SongCSV(
                                        songId = songId,
                                        playlistBrowseId = "",
                                        playlistName = playlistName,
                                        title = explicitPrefix + title,
                                        artists = cleanArtists,
                                        duration = convertedDuration,
                                        thumbnailUrl = thumbnailUrl
                                    )
                                )
                                successCount++
                                
                                // Update progress
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toaster.i("✅ $title converted")
                                }
                                
                            } catch (e: Exception) {
                                failCount++
                                // Fallback to search format if YouTube conversion fails
                                val encodedSearch = URLEncoder.encode(query, "UTF-8")
                                val fallbackSongId = "search:$encodedSearch"
                                
                                val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                                val convertedDuration = if (rawDurationMs > 0) {
                                    formatAsDuration(rawDurationMs)
                                } else {
                                    "0"
                                }
                                
                                converted.add(
                                    SongCSV(
                                        songId = fallbackSongId,
                                        playlistBrowseId = "",
                                        playlistName = playlistName,
                                        title = explicitPrefix + title,
                                        artists = cleanArtists,
                                        duration = convertedDuration,
                                        thumbnailUrl = row["Album Image URL"].orEmpty()
                                    )
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toaster.e("❌ YouTube failed: $title - using search fallback")
                                }
                            }
                        }

                        // Add delay between API calls
                        kotlinx.coroutines.delay(1000)

                    } else if (isYourFormat || isCustomFormat) {
                        // Handle compatible CSV formats (no conversion needed) - USE EXACT MEDIAID FROM CSV
                        var browseId = row["PlaylistBrowseId"].orEmpty()
                        if (browseId.toLongOrNull() != null)
                            browseId = ""

                        val rawDuration = row["Duration"].orEmpty()
                        val convertedDuration =
                            if (rawDuration.isBlank())
                                "0"
                            else if (!DurationUtils.isHumanReadable(rawDuration))
                                formatAsDuration(rawDuration.toLong().times(1000))
                            else
                                rawDuration

                        // Use the EXACT MediaId from the CSV - don't modify it!
                        val mediaId = row["MediaId"].orEmpty()
                        val songId = mediaId // Use the MediaId exactly as it appears in CSV

                        // Skip empty MediaIds
                        if (mediaId.isNotBlank()) {
                            converted.add(
                                SongCSV(
                                    songId = songId,
                                    playlistBrowseId = browseId,
                                    playlistName = playlistName,
                                    title = row["Title"].orEmpty(),
                                    artists = row["Artists"].orEmpty(),
                                    duration = convertedDuration,
                                    thumbnailUrl = row["ThumbnailUrl"].orEmpty()
                                )
                            )
                            successCount++
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.i("✅ Added: ${row["Title"].orEmpty()}")
                            }
                        } else {
                            failCount++
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.e("❌ Skipped: ${row["Title"].orEmpty()} - No MediaId")
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    failCount++
                    println("Error processing row $index: ${e.message}")
                }
            }

            // Final result
            CoroutineScope(Dispatchers.Main).launch {
                if (successCount > 0) {
                    Toaster.i("✅ Successfully processed $successCount songs for '$playlistName'")
                    if (failCount > 0) {
                        Toaster.i("⚠️ $failCount songs had issues")
                    }
                } else {
                    Toaster.e("❌ No songs could be processed")
                }
            }
            return converted
        }

        private fun processSongs(songs: List<SongCSV>): Map<Pair<String, String>, List<Song>> =
            songs.fastFilter { it.songId.isNotBlank() }
                .groupBy { it.playlistName to it.playlistBrowseId }
                .mapValues { (_, songs) ->
                    songs.fastMap {
                        // Create proper Song objects - use MediaId exactly as provided
                        Song(
                            id = it.songId, // This should be the raw MediaId like "1pEe7-tWv2M"
                            title = it.title,
                            artistsText = it.artists,
                            thumbnailUrl = it.thumbnailUrl,
                            durationText = it.duration,
                            totalPlayTimeMs = 1L
                        )
                    }
                }

        @Composable
        operator fun invoke() = ImportSongsFromCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult

                CoroutineScope(Dispatchers.IO).launch {
                    val straySongs = mutableListOf<Song>()
                    val combos = mutableMapOf<Playlist, List<Song>>()

                    try {
                        // Get filename from URI - handle null case
                        val fileName = uri.lastPathSegment ?: "imported_playlist"

                        // Check internet before starting conversion
                        if (!hasInternetConnection()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.e("❌ No internet connection. Required for YouTube conversion.")
                            }
                            return@launch
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            Toaster.i("📥 Starting CSV import for '$fileName'...")
                        }

                        val csvSongs = appContext().contentResolver
                            .openInputStream(uri)
                            ?.use { stream ->
                                // Call the suspend function with filename
                                parseFromCsvFile(stream, fileName)
                            }
                            ?: emptyList()

                        if (csvSongs.isEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.e("❌ No valid songs found in CSV")
                            }
                            return@launch
                        }

                        val processedSongs = processSongs(csvSongs)
                        
                        processedSongs.forEach { (playlist, songs) ->
                            if (playlist.first.isNotBlank()) {
                                val realPlaylist = Playlist(name = playlist.first, browseId = playlist.second)
                                combos[realPlaylist] = songs
                            } else {
                                straySongs.addAll(songs)
                            }
                        }

                        if (combos.isEmpty() && straySongs.isEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toaster.e("❌ No valid songs could be processed")
                            }
                            return@launch
                        }

                        Database.asyncTransaction {
                            // Insert all songs first
                            val allSongs = straySongs + combos.values.flatten()
                            songTable.upsert(allSongs)

                            // Then map songs to playlists using the original mapIgnore function
                            combos.forEach { (playlist, songs) ->
                                mapIgnore(playlist, *songs.toTypedArray())
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                val playableSongs = allSongs.count { it.id.isNotBlank() && !it.id.startsWith("search:") }
                                val totalSongs = allSongs.size
                                val playlistName = combos.keys.firstOrNull()?.name ?: fileName.replace(".csv", "")
                                Toaster.i("🎉 Created playlist '$playlistName' with $playableSongs/$totalSongs playable songs")
                            }
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is InvalidHeaderException -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toaster.e(R.string.error_message_unsupported_local_playlist)
                                }
                            }
                            else -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toaster.e("❌ Import failed: ${e.message ?: "Unknown error"}")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    override val supportedMimes: Array<String> = arrayOf("text/csv", "text/comma-separated-values")
    override val iconId: Int = R.drawable.import_outline
    override val messageId: Int = R.string.import_playlist
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)
}
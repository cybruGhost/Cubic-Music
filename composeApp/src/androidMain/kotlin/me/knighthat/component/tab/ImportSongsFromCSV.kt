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
import java.io.InputStream
import java.net.URLEncoder

class ImportSongsFromCSV(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : ImportFromFile(launcher), MenuIcon, Descriptive {

    companion object {
        private fun parseFromCsvFile(inputStream: InputStream): List<SongCSV> =
            csvReader { skipEmptyLine = true }
                .readAllWithHeader(inputStream)
                .also { rows ->
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
                }
                .fastMap { row ->
                    val isSpotifyFormat = row.containsKey("Track Name") && row.containsKey("Artist Name(s)")
                    val isExportifyFormat = row.containsKey("Track URI") && row.containsKey("Track Name")
                    val isYourFormat = row.containsKey("PlaylistBrowseId") && row.containsKey("PlaylistName") && 
                                      row.containsKey("MediaId") && row.containsKey("Title") && 
                                      row.containsKey("Artists") && row.containsKey("Duration") &&
                                      row.containsKey("ThumbnailUrl") && row.containsKey("AlbumId") &&
                                      row.containsKey("AlbumTitle") && row.containsKey("ArtistIds")
                    
                    if (isExportifyFormat || isSpotifyFormat) {
                        // Handle both Spotify and Exportify CSV formats
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val title = row["Track Name"].orEmpty()
                        val artists = row["Artist Name(s)"].orEmpty()
                        
                        // Clean up artist names (remove Spotify URIs if present)
                        val cleanArtists = artists.split(", ")
                            .joinToString(", ") { artist ->
                                artist.split("spotify:artist:").last().trim()
                            }
                        
                        // Create search query for YouTube Music
                        val searchQuery = "$title $cleanArtists".trim()
                        val encodedSearch = URLEncoder.encode(searchQuery, "UTF-8")
                        
                        // Use search-based ID format that YouTube Music can understand
                        val songId = "search:$encodedSearch"

                        // Convert duration from ms to proper format
                        val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                        val convertedDuration = if (rawDurationMs > 0) {
                            formatAsDuration(rawDurationMs)
                        } else {
                            "0"
                        }

                        SongCSV(
                            songId = songId,
                            playlistBrowseId = "",
                            playlistName = if (isExportifyFormat) "Imported from Exportify" else "Imported from Spotify",
                            title = explicitPrefix + title,
                            artists = cleanArtists,
                            duration = convertedDuration,
                            thumbnailUrl = row["Album Image URL"].orEmpty()
                        )
                    } else if (isYourFormat) {
                        // Handle your specific CSV format
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

                        SongCSV(
                            songId = row["MediaId"].orEmpty(),
                            playlistBrowseId = browseId,
                            playlistName = row["PlaylistName"].orEmpty(),
                            title = row["Title"].orEmpty(),
                            artists = row["Artists"].orEmpty(),
                            duration = convertedDuration,
                            thumbnailUrl = row["ThumbnailUrl"].orEmpty()
                        )
                    } else {
                        // Handle custom CSV format (your app's original format)
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

                        SongCSV(
                            songId = row["MediaId"].orEmpty(),
                            playlistBrowseId = browseId,
                            playlistName = row["PlaylistName"].orEmpty(),
                            title = row["Title"].orEmpty(),
                            artists = row["Artists"].orEmpty(),
                            duration = convertedDuration,
                            thumbnailUrl = row["ThumbnailUrl"].orEmpty()
                        )
                    }
                }
                .toList()

        private fun processSongs(songs: List<SongCSV>): Map<Pair<String, String>, List<Song>> =
            songs.fastFilter { it.songId.isNotBlank() }
                .groupBy { it.playlistName to it.playlistBrowseId }
                .mapValues { (_, songs) ->
                    songs.fastMap {
                        // Create proper Song objects
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
        operator fun invoke() = ImportSongsFromCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult

                CoroutineScope(Dispatchers.IO).launch {
                    val straySongs = mutableListOf<Song>()
                    val combos = mutableMapOf<Playlist, List<Song>>()

                    try {
                        appContext().contentResolver
                            .openInputStream(uri)
                            ?.use(::parseFromCsvFile)
                            ?.let(::processSongs)
                            ?.forEach { (playlist, songs) ->
                                if (playlist.first.isNotBlank()) {
                                    val realPlaylist = Playlist(name = playlist.first, browseId = playlist.second)
                                    combos[realPlaylist] = songs
                                } else {
                                    straySongs.addAll(songs)
                                }
                            }

                        Database.asyncTransaction {
                            // Insert all songs first
                            val allSongs = straySongs + combos.values.flatten()
                            songTable.upsert(allSongs)

                            // Then map songs to playlists
                            combos.forEach { (playlist, songs) ->
                                mapIgnore(playlist, *songs.toTypedArray())
                            }

                            Toaster.done()
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is InvalidHeaderException -> Toaster.e(R.string.error_message_unsupported_local_playlist)
                            else -> Toaster.e(R.string.error_message_import_local_playlist_failed)
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
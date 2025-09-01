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
                    
                    if (!hasCustomFormat && !hasSpotifyFormat && !hasExportifyFormat) {
                        throw InvalidHeaderException("Unsupported CSV format")
                    }
                }
                .fastMap { row ->
                    val isSpotifyFormat = row.containsKey("Track Name") && row.containsKey("Artist Name(s)")
                    val isExportifyFormat = row.containsKey("Track URI") && row.containsKey("Track Name")
                    
                    if (isExportifyFormat) {
                        // Handle Exportify CSV format (Spotify export)
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val title = row["Track Name"].orEmpty()
                        val artists = row["Artist Name(s)"].orEmpty()
                        val trackUri = row["Track URI"].orEmpty()
                        
                        // Extract the actual media ID from the Spotify URI (spotify:track:ABC123)
                        val mediaId = if (trackUri.startsWith("spotify:track:")) {
                            trackUri.substringAfter("spotify:track:")
                        } else {
                            // Fallback: generate a pseudo ID if URI format is unexpected
                            (title + artists).filter { it.isLetterOrDigit() }.take(20)
                        }

                        val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                        val convertedDuration = if (rawDurationMs > 0) {
                            formatAsDuration(rawDurationMs)
                        } else {
                            "0"
                        }

                        SongCSV(
                            songId = mediaId, // Use the actual media ID
                            playlistBrowseId = "",
                            playlistName = "Imported from Exportify",
                            title = explicitPrefix + title,
                            artists = artists,
                            duration = convertedDuration,
                            thumbnailUrl = row["Album Image URL"].orEmpty()
                        )
                    } else if (isSpotifyFormat) {
                        // Handle Spotify CSV format
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val title = row["Track Name"].orEmpty()
                        val artists = row["Artist Name(s)"].orEmpty()
                        
                        // For Spotify format, we need to search for the song later
                        // Create a searchable format that the app can recognize
                        val searchQuery = "$title $artists".trim()
                        val pseudoMediaId = "search:$searchQuery"

                        val rawDurationMs = row["Track Duration (ms)"]?.toLongOrNull() ?: 0L
                        val convertedDuration = if (rawDurationMs > 0) {
                            formatAsDuration(rawDurationMs)
                        } else {
                            "0"
                        }

                        SongCSV(
                            songId = pseudoMediaId,
                            playlistBrowseId = "",
                            playlistName = "Imported from Spotify",
                            title = explicitPrefix + title,
                            artists = artists,
                            duration = convertedDuration,
                            thumbnailUrl = row["Album Image URL"].orEmpty()
                        )
                    } else {
                        // Handle custom CSV format
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
                        Song(
                            id = it.songId,
                            title = it.title,
                            artistsText = it.artists,
                            thumbnailUrl = it.thumbnailUrl,
                            durationText = it.duration,
                            totalPlayTimeMs = 1L,
                            // Add mediaId field which is crucial for playback
                            mediaId = if (it.songId.startsWith("search:")) {
                                // For search-based IDs, we'll need to resolve them later
                                ""
                            } else {
                                it.songId
                            }
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
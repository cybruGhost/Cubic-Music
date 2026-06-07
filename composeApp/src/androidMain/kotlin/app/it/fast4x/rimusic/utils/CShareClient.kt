package app.it.fast4x.rimusic.utils

import android.util.Base64
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CShareClient {
    private const val SHARE_PREFIX = "cshare://"
    private val youtubeIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

    data class ShareResult(val shareUrl: String, val expiresAt: String, val songCount: Int) {
        val shareText: String
            get() = encodeShareUrl(shareUrl)
    }
    data class SharedPlaylist(val name: String, val expiresAt: String?, val songs: List<Song>)

    fun encodeShareUrl(url: String): String {
        val encoded = Base64.encodeToString(url.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return "$SHARE_PREFIX$encoded"
    }

    suspend fun sharePlaylist(playlist: Playlist): ShareResult = withContext(Dispatchers.IO) {
        val songs = Database.songPlaylistMapTable.sortSongsByPosition(playlist.id).first()
        val body = JSONObject()
            .put("playlistName", playlist.name)
            .put("source", "cubic-android")
            .put("songs", songs.toShareJson())

        val response = postJson(SecureApiConfig.cShareEndpoint, body)
        ShareResult(
            shareUrl = response.optString("shareUrl"),
            expiresAt = response.optString("expiresAt"),
            songCount = songs.size
        )
    }

    suspend fun importShare(linkOrId: String): SharedPlaylist = withContext(Dispatchers.IO) {
        val decoded = decodeShareInput(linkOrId)
        val id = decoded.substringAfterLast("/").substringBefore("?").substringBefore("#")
        require(id.isNotBlank()) { "Paste a valid C-Share link or id." }
        val url = "${SecureApiConfig.cShareEndpoint}/$id"
        val json = getJson(url)
        val songs = json.optJSONArray("songs").orEmptySongs()
        SharedPlaylist(
            name = json.optString("playlistName").ifBlank { "C-Share Playlist" },
            expiresAt = json.optString("expiresAt").takeIf { it.isNotBlank() },
            songs = songs
        )
    }

    suspend fun importIntoLibrary(shared: SharedPlaylist): Long = withContext(Dispatchers.IO) {
        val playlistId = Database.playlistTable.insert(Playlist(name = shared.name))
        shared.songs.forEach { song ->
            Database.songTable.insertIgnore(song)
            Database.songPlaylistMapTable.map(song.id, playlistId)
        }
        playlistId
    }

    private fun decodeShareInput(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.startsWith(SHARE_PREFIX, ignoreCase = true)) return trimmed
        val encoded = trimmed.substringAfter(SHARE_PREFIX)
        return runCatching {
            String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrElse {
            error("That C-Share code is not valid.")
        }
    }

    private fun List<Song>.toShareJson(): JSONArray = JSONArray().also { array ->
        forEach { song ->
            array.put(
                JSONObject()
                    .put("songId", song.id)
                    .put("title", song.title)
                    .put("artists", song.artistsText.orEmpty())
                    .put("duration", song.durationText.orEmpty())
                    .put("thumbnailUrl", song.thumbnailUrl.orEmpty())
            )
        }
    }

    private suspend fun JSONArray?.orEmptySongs(): List<Song> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("songId").ifBlank { item.optString("id") }
                val rawTitle = item.optString("title")
                if (id.isBlank() || rawTitle.isBlank()) continue
                val inferred = inferTitleAndArtist(
                    title = rawTitle,
                    artists = item.optString("artists").ifBlank { item.optString("artist") }
                )
                val title = inferred.first
                val artists = inferred.second
                val playableId = id.takeIf { it.isYoutubeVideoId() }
                    ?: resolveImportedSongId(title, artists)
                    ?: id
                add(
                    Song(
                        id = playableId,
                        title = title,
                        artistsText = artists,
                        durationText = item.optString("duration").ifBlank { null },
                        thumbnailUrl = item.optString("thumbnailUrl")
                            .ifBlank { item.optString("thumbnail") }
                            .ifBlank {
                                playableId.takeIf { it.isYoutubeVideoId() }
                                    ?.let { "https://yt.omada.cafe/vi/$it/hqdefault.jpg" }
                                    .orEmpty()
                            }
                    )
                )
            }
        }
    }

    private fun String.isYoutubeVideoId(): Boolean = youtubeIdRegex.matches(this)

    private fun inferTitleAndArtist(title: String, artists: String): Pair<String, String> {
        if (artists.isNotBlank()) return title to artists
        val separator = listOf(" - ", " – ", " — ").firstOrNull { title.contains(it) } ?: return title to artists
        val artist = title.substringBefore(separator).trim()
        val cleanTitle = title.substringAfter(separator).trim()
        return if (artist.isNotBlank() && cleanTitle.isNotBlank()) cleanTitle to artist else title to artists
    }

    private suspend fun resolveImportedSongId(title: String, artists: String): String? {
        val query = listOf(title, artists).filter { it.isNotBlank() }.joinToString(" ").trim()
        if (query.isBlank()) return null
        return runCatching {
            OmadaSearchClient.search(query, type = "video").getOrNull()
                ?.firstOrNull { it.type.contains("video") && it.id.isYoutubeVideoId() }
                ?.id
        }.getOrNull()
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        return connection.readJsonResponse()
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return connection.readJsonResponse()
    }

    private fun HttpURLConnection.readJsonResponse(): JSONObject {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (responseCode == 410) error("This C-Share link has expired.")
        if (responseCode == 404) error("C-Share link was not found.")
        if (responseCode !in 200..299) error(JSONObject(text).optString("error").ifBlank { "C-Share failed: HTTP $responseCode" })
        return JSONObject(text)
    }
}

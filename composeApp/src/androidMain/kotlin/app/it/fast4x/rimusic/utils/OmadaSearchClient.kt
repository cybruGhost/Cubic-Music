package app.it.fast4x.rimusic.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OmadaSearchResult(
    val id: String,
    val type: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationText: String?,
    val viewCountText: String?,
    val description: String? = null,
    val authorVerified: Boolean = false,
)

object OmadaSearchClient {
    suspend fun search(query: String, type: String? = null): Result<List<OmadaSearchResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = SecureApiConfig.resolveOmadaSearchApi()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val typeParam = type?.takeIf { it.isNotBlank() }?.let {
                    "&type=${URLEncoder.encode(it, "UTF-8")}"
                }.orEmpty()
                val connection = (URL("$endpoint?q=$encodedQuery$typeParam").openConnection() as HttpURLConnection)
                    .apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 12_000
                    }

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        error("Omada HTTP ${connection.responseCode}")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResults(response)
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun parseResults(response: String): List<OmadaSearchResult> {
        val array = runCatching { JSONArray(response) }.getOrElse {
            val root = JSONObject(response)
            root.optJSONArray("results")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("data")
                ?: JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = item.optString("type")
                    .ifBlank { item.optString("kind") }
                    .ifBlank { "video" }
                    .lowercase()
                val id = when {
                    type.contains("channel") || type.contains("artist") ->
                        item.firstString("channelId", "authorId", "id")
                    type.contains("playlist") ->
                        item.firstString("playlistId", "id")
                    else ->
                        item.firstString("videoId", "id")
                } ?: continue
                val title = when {
                    type.contains("channel") || type.contains("artist") ->
                        item.firstString("title", "name", "author", "channelHandle")
                    else ->
                        item.firstString("title", "name")
                }.orEmpty()
                if (title.isBlank()) continue

                add(
                    OmadaSearchResult(
                        id = id,
                        type = type,
                        title = title,
                        author = item.firstString("author", "uploader", "channel", "channelName").orEmpty(),
                        thumbnailUrl = item.firstString("thumbnail", "thumbnailUrl", "image", "avatar", "playlistThumbnail")
                            ?: item.bestThumbnailUrl("videoThumbnails")
                            ?: item.bestThumbnailUrl("authorThumbnails")
                            ?: item.bestThumbnailUrl("thumbnails"),
                        durationText = item.firstString("duration", "durationText", "lengthText")
                            ?: item.optLong("lengthSeconds").takeIf { it > 0L }?.toDurationText(),
                        viewCountText = item.firstString("views", "viewCountText")
                            ?: item.optLong("viewCount").takeIf { it > 0L }?.let { "$it views" }
                            ?: item.optLong("subCount").takeIf { it > 0L }?.let { "$it subscribers" },
                        description = item.firstString("description", "channelHandle"),
                        authorVerified = item.optBoolean("authorVerified", false)
                    )
                )
            }
        }
    }

    private fun JSONObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            optString(key).takeIf { it.isNotBlank() && it != "null" }
        }

    private fun JSONObject.bestThumbnailUrl(key: String): String? {
        val thumbnails = optJSONArray(key) ?: return null
        for (index in thumbnails.length() - 1 downTo 0) {
            val url = thumbnails.optJSONObject(index)?.optString("url")
            if (!url.isNullOrBlank()) return url
        }
        return null
    }

    private fun Long.toDurationText(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        val seconds = this % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}

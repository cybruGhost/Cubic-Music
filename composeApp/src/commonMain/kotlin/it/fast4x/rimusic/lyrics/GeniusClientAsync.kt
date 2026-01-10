import com.google.gson.Gson
import it.fast4x.rimusic.models.GeniusSearchResponse
import it.fast4x.rimusic.models.LyricsResult

import kotlinx.coroutines.*

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class GeniusClientAsync(private val apiToken: String) {

    private val baseUrl = "https://api.genius.com"
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun searchSong(query: String): GeniusSearchResponse? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?q=${query.replace(" ", "%20")}"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val body = response.body?.string()
                gson.fromJson(body, GeniusSearchResponse::class.java)
            }
        } catch (e: Exception) {
            println("Error searching song: ${e.message}")
            null
        }
    }

    suspend fun getLyrics(artist: String, title: String): LyricsResult? = withContext(Dispatchers.IO) {
        val query = "$artist $title"
        val searchResponse = searchSong(query) ?: return@withContext null

        val firstHit = searchResponse.response.hits.firstOrNull() ?: run {
            println("No results found for: $query")
            return@withContext null
        }

        val song = firstHit.result
        val lyrics = scrapeLyricsFromUrl(song.url) ?: return@withContext null

        LyricsResult(
            title = song.title,
            artist = song.primary_artist.name,
            lyrics = lyrics,
            url = song.url
        )
    }

    private suspend fun scrapeLyricsFromUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val lyricsContainers = doc.select("div[data-lyrics-container='true']")

            if (lyricsContainers.isEmpty()) {
                println("Lyrics container not found on page")
                return@withContext null
            }

            lyricsContainers.joinToString("\n\n") { container ->
                container.wholeText().trim()
            }
        } catch (e: Exception) {
            println("Error scraping lyrics from $url: ${e.message}")
            null
        }
    }
}
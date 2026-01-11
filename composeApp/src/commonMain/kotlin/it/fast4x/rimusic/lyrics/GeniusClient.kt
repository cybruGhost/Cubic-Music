package it.fast4x.rimusic.lyrics

import com.google.gson.Gson
import it.fast4x.rimusic.models.GeniusSearchResponse
import it.fast4x.rimusic.models.LyricsResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class GeniusClient(private val apiToken: String) {

    private val baseUrl = "https://api.genius.com"
    private val client = OkHttpClient()
    private val gson = Gson()

    fun searchSong(query: String): GeniusSearchResponse? {
        val url = "$baseUrl/search?q=${query.replace(" ", "%20")}"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiToken")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if(!response.isSuccessful) {
                    throw IOException("Unexpected Searching Song: $response")
                }

                val body = response.body?.string()
                gson.fromJson(body, GeniusSearchResponse::class.java)
            }
        } catch (e: Exception) {
            println("Error Searching song: ${e.message}")
            null
        }
    }

    fun getLyrics(artist: String, title: String): LyricsResult? {
        val query = "$artist $title"
        val searchResponse = searchSong(query) ?: return null

        val firstHit = searchResponse.response.hits.firstOrNull() ?: run {
            println("No result found for: $query")
            return null
        }

        val song = firstHit.result
        val lyrics = scrapeLyricsFromUrl(song.url) ?: return null

        return LyricsResult(
            title = song.title,
            artist = song.primary_artist.name,
            lyrics = lyrics,
            url = song.url
        )

    }

    private fun scrapeLyricsFromUrl(url: String): String? {
        return try {
            val doc = Jsoup.connect(url).get()

            val lyricsContainers = doc.select("div[data-lyrics-container='true']")

            if (lyricsContainers.isEmpty()) {
                println("Lyrics container not found on page")
                return null
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
package it.fast4x.genius

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.genius.models.GeniusSearchResponse
import it.fast4x.genius.utils.recoverIfCancelled
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

object Genius {
    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient(apiToken: String) = HttpClient(OkHttp) {
        BrowserUserAgent()

        expectSuccess = true

        install(ContentNegotiation) {
            val feature = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            }

            json(feature)
        }

        install(ContentEncoding) {
            gzip()
            deflate()
        }

        defaultRequest {
            url("https://api.genius.com")
            header("Authorization", "Bearer $apiToken")
        }
    }

    suspend fun lyrics(
        apiToken: String,
        artist: String,
        title: String
    ): Result<Lyrics?>? {
        return runCatching {
            val client = createClient(apiToken)

            val query = "$artist $title"
            val searchResponse = client.get("/search") {
                parameter("q", query)
            }.body<GeniusSearchResponse>()

            val firstHit = searchResponse.response.hits.firstOrNull()
                ?: return@runCatching null

            val song = firstHit.result
            scrapeLyricsFromUrl(song.url)?.let { lyricsText ->
                Lyrics(lyricsText)
            }
        }.recoverIfCancelled()
    }

    private fun scrapeLyricsFromUrl(url: String): String? {
        return try {
            val doc = Jsoup.connect(url).get()
            val lyricsContainers = doc.select("div[data-lyrics-container='true']")

            if (lyricsContainers.isEmpty()) {
                return null
            }

            lyricsContainers.joinToString("\n\n") { container ->
                container.wholeText().trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    @JvmInline
    value class Lyrics(val value: String) : CharSequence by value
}
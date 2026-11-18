package it.fast4x.kugou

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.decodeBase64String
import it.fast4x.kugou.models.DownloadLyricsResponse
import it.fast4x.kugou.models.SearchLyricsResponse
import it.fast4x.kugou.models.SearchSongResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object KuGou {
    private const val DURATION_TOLERANCE_SECONDS = 3L
    private const val TIMELINE_GRACE_MS = 5_000L
    private val featuredArtistClause = Regex(
        """(?i)[\(\[\{]\s*(?:feat(?:uring)?|ft)\.?\s+([^\)\]\}]+)[\)\]\}]"""
    )
    private val lrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[\.:](\d{1,3}))?]""")
    private val lrcOffset = Regex("""(?im)^\[offset:([+-]?\d+)]\s*$""")

    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }

                json(feature)
                json(feature, ContentType.Text.Html)
                json(feature, ContentType.Text.Plain)
            }

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            defaultRequest {
                url("https://krcs.kugou.com")
            }
        }
    }

    suspend fun lyrics(
        artist: String,
        title: String,
        duration: Long,
        album: String? = null,
    ): Result<Lyrics?>? {
        return runCatching {
            if (artist.isBlank() || title.isBlank() || duration <= 0L) return@runCatching null

            val matchingSongs = searchSong(keyword(artist, title))
                .asSequence()
                .filter { it.matches(artist, title, duration, album) }
                .sortedBy { abs(it.duration - duration) }
                .toList()

            for (song in matchingSongs) {
                val candidates = searchLyricsByHash(song.hash)
                    .filter { it.matchesDuration(duration) }

                for (candidate in candidates) {
                    val downloaded = downloadLyrics(candidate.id, candidate.accessKey)
                    if (!downloaded.hasConservativeTiming(duration)) continue
                    return@runCatching downloaded.applyTimingOffset().normalize()
                }
            }

            null
        }.recoverIfCancelled()
    }

    private fun SearchSongResponse.Data.Info.matches(
        artist: String,
        title: String,
        duration: Long,
        album: String?,
    ): Boolean {
        if (songname.isBlank() || singername.isBlank()) return false
        if (titleIdentity(songname) != titleIdentity(title)) return false
        if (artistIdentity(singername, songname) != artistIdentity(artist, title)) return false
        album?.takeIf(String::isNotBlank)?.let { expectedAlbum ->
            if (albumName.isBlank() || albumIdentity(albumName) != albumIdentity(expectedAlbum)) return false
        }
        return this.duration > 0L && abs(this.duration - duration) <= DURATION_TOLERANCE_SECONDS
    }

    private fun SearchLyricsResponse.Candidate.matchesDuration(duration: Long): Boolean {
        if (this.duration <= 0L) return false
        val candidateSeconds = if (this.duration > 10_000L) {
            (this.duration + 500L) / 1_000L
        } else {
            this.duration
        }
        return abs(candidateSeconds - duration) <= DURATION_TOLERANCE_SECONDS
    }

    private fun normalizedWords(value: String): List<String> =
        Normalizer.normalize(value.replace(Regex("<[^>]+>"), " "), Normalizer.Form.NFKD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter(String::isNotBlank)

    private fun titleIdentity(value: String): String =
        normalizedWords(value.replace(featuredArtistClause, " ")).joinToString(" ")

    private fun artistIdentity(artist: String, title: String): Set<String> {
        val featuredArtists = featuredArtistClause.findAll(title)
            .map { it.groupValues[1] }
            .joinToString(" ")
        return normalizedWords("$artist $featuredArtists")
            .filterNot { it == "feat" || it == "featuring" || it == "ft" }
            .toSet()
    }

    private fun albumIdentity(value: String): String = normalizedWords(value).joinToString(" ")

    private suspend fun downloadLyrics(id: Long, accessKey: String): Lyrics {
        return client.get("/download") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("fmt", "lrc")
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>().content.decodeBase64String().let(::Lyrics)
    }

    private suspend fun searchLyricsByHash(hash: String): List<SearchLyricsResponse.Candidate> {
        return client.get("/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "mobi")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>().candidates
    }

    private suspend fun searchSong(keyword: String): List<SearchSongResponse.Data.Info> {
        return client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", 8)
            parameter("showtype", 0)
            url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
        }.body<SearchSongResponse>().data.info
    }

    private fun keyword(artist: String, title: String): String {
        val (newTitle, featuring) = title.extract(" (feat. ", ')')

        val newArtist = (if (featuring.isEmpty()) artist else "$artist, $featuring")
            .replace(", ", "、")
            .replace(" & ", "、")
            .replace(".", "")

        return "$newArtist - $newTitle"
    }

    private fun String.extract(startDelimiter: String, endDelimiter: Char): Pair<String, String> {
        val startIndex = indexOf(startDelimiter)

        if (startIndex == -1) return this to ""

        val endIndex = indexOf(endDelimiter, startIndex)

        if (endIndex == -1) return this to ""

        return removeRange(
            startIndex,
            endIndex + 1
        ) to substring(startIndex + startDelimiter.length, endIndex)
    }

    @JvmInline
    value class Lyrics(val value: String) : CharSequence by value {
        val sentences: List<Pair<Long, String>>
            get() = mutableListOf(0L to "").apply {
                for (line in value.trim().lines()) {
                    try {
                        val position = line.take(10).run {
                            get(8).digitToInt() * 10L +
                                get(7).digitToInt() * 100 +
                                get(5).digitToInt() * 1000 +
                                get(4).digitToInt() * 10000 +
                                get(2).digitToInt() * 60 * 1000 +
                                get(1).digitToInt() * 600 * 1000
                        }

                        add(position to line.substring(10))
                    } catch (_: Throwable) {
                    }
                }
            }

        internal fun hasConservativeTiming(durationSeconds: Long): Boolean {
            val timestamps = lrcTimestamp.findAll(value).mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                if (seconds !in 0L..59L) return@mapNotNull null
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                minutes * 60_000L + seconds * 1_000L + fraction
            }.toList()
            if (timestamps.isEmpty() || durationSeconds <= 0L) return false
            val offsetMs = lrcOffset.find(value)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            return timestamps.min() + offsetMs >= -TIMELINE_GRACE_MS &&
                timestamps.max() + offsetMs <= durationSeconds * 1_000L + TIMELINE_GRACE_MS
        }

        internal fun applyTimingOffset(): Lyrics {
            val offsetMs = lrcOffset.find(value)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return this

            val shifted = lrcTimestamp.replace(value) { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@replace match.value
                val seconds = match.groupValues[2].toLongOrNull() ?: return@replace match.value
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                val adjustedMs = (minutes * 60_000L + seconds * 1_000L + fraction + offsetMs)
                    .coerceAtLeast(0L)
                val adjustedMinutes = adjustedMs / 60_000L
                val adjustedSeconds = (adjustedMs % 60_000L) / 1_000L
                val adjustedCentiseconds = (adjustedMs % 1_000L) / 10L
                String.format(
                    Locale.US,
                    "[%02d:%02d.%02d]",
                    adjustedMinutes,
                    adjustedSeconds,
                    adjustedCentiseconds,
                )
            }
            return Lyrics(
                shifted.lineSequence()
                    .filterNot { lrcOffset.matches(it.trim()) }
                    .joinToString("\n")
            )
        }

        fun normalize(): Lyrics {
            var toDrop = 0
            var maybeToDrop = 0

            val text = value.replace("\r\n", "\n").trim()

            for (line in text.lineSequence()) {
                if (line.startsWith("[ti:") ||
                    line.startsWith("[ar:") ||
                    line.startsWith("[al:") ||
                    line.startsWith("[by:") ||
                    line.startsWith("[hash:") ||
                    line.startsWith("[sign:") ||
                    line.startsWith("[qq:") ||
                    line.startsWith("[total:") ||
                    line.startsWith("[offset:") ||
                    line.startsWith("[id:") ||
                    line.containsAt("]Written by：", 9) ||
                    line.containsAt("]Lyrics by：", 9) ||
                    line.containsAt("]Composed by：", 9) ||
                    line.containsAt("]Producer：", 9) ||
                    line.containsAt("]作曲 : ", 9) ||
                    line.containsAt("]作词 : ", 9)
                ) {
                    toDrop += line.length + 1 + maybeToDrop
                    maybeToDrop = 0
                } else {
                    if (maybeToDrop == 0) {
                        maybeToDrop = line.length + 1
                    } else {
                        maybeToDrop = 0
                        break
                    }
                }
            }

            return Lyrics(text.drop(toDrop + maybeToDrop).removeHtmlEntities())
        }

        private fun String.containsAt(charSequence: CharSequence, startIndex: Int): Boolean =
            regionMatches(startIndex, charSequence, 0, charSequence.length)

        private fun String.removeHtmlEntities(): String =
            replace("&apos;", "'")
    }
}

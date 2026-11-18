package it.fast4x.lrclib.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration

@Serializable
data class Track(
    val id: Long,
    val name: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    @SerialName("duration") val tDuration: JsonElement,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
) {
    val duration: Long
        get() = (tDuration as? JsonPrimitive)?.toString()?.substringBefore(".")?.toLong()
            ?: (tDuration as JsonArray).first().jsonPrimitive.toString().substringBefore(".").toLong()
}

private const val DURATION_TOLERANCE_SECONDS = 3L
private const val TIMELINE_GRACE_MS = 5_000L
private val featuredArtistClause = Regex(
    """(?i)[\(\[\{]\s*(?:feat(?:uring)?|ft)\.?\s+([^\)\]\}]+)[\)\]\}]"""
)
private val lrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[\.:](\d{1,3}))?]""")
private val lrcOffset = Regex("""(?im)^\[offset:([+-]?\d+)]\s*$""")

private fun normalizedWords(value: String): List<String> =
    Normalizer.normalize(value, Normalizer.Form.NFKD)
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

fun Track.matchesMetadataFor(
    artist: String,
    title: String,
    durationSeconds: Long,
    album: String? = null,
): Boolean {
    if (artist.isBlank() || title.isBlank() || durationSeconds <= 0L) return false
    if (titleIdentity(trackName) != titleIdentity(title)) return false
    if (artistIdentity(artistName, trackName) != artistIdentity(artist, title)) return false
    album?.takeIf(String::isNotBlank)?.let { expectedAlbum ->
        if (albumName.isBlank() || albumIdentity(albumName) != albumIdentity(expectedAlbum)) return false
    }
    return duration > 0L && abs(duration - durationSeconds) <= DURATION_TOLERANCE_SECONDS
}

fun Track.hasConservativeSyncedTiming(durationSeconds: Long): Boolean {
    val lyrics = syncedLyrics?.takeIf(String::isNotBlank) ?: return false
    val timestamps = lrcTimestamp.findAll(lyrics).mapNotNull { match ->
        val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        if (seconds !in 0L..59L) return@mapNotNull null
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        (minutes * 60_000L) + (seconds * 1_000L) + fraction
    }.toList()
    if (timestamps.isEmpty() || durationSeconds <= 0L) return false

    val offsetMs = lrcOffset.find(lyrics)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    return timestamps.min() + offsetMs >= -TIMELINE_GRACE_MS &&
        timestamps.max() + offsetMs <= durationSeconds * 1_000L + TIMELINE_GRACE_MS
}

fun Track.syncedLyricsWithAppliedOffset(): String? {
    val lyrics = syncedLyrics?.takeIf(String::isNotBlank) ?: return null
    val offsetMs = lrcOffset.find(lyrics)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?: return lyrics

    val shifted = lrcTimestamp.replace(lyrics) { match ->
        val minutes = match.groupValues[1].toLongOrNull() ?: return@replace match.value
        val seconds = match.groupValues[2].toLongOrNull() ?: return@replace match.value
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        val adjustedMs = (minutes * 60_000L + seconds * 1_000L + fraction + offsetMs)
            .coerceAtLeast(0L)
        String.format(
            Locale.US,
            "[%02d:%02d.%02d]",
            adjustedMs / 60_000L,
            (adjustedMs % 60_000L) / 1_000L,
            (adjustedMs % 1_000L) / 10L,
        )
    }
    return shifted.lineSequence()
        .filterNot { lrcOffset.matches(it.trim()) }
        .joinToString("\n")
        .trim()
}

internal fun List<Track>.bestMatchingFor(
    artist: String,
    title: String,
    duration: Duration,
    album: String? = null,
): Track? {
    val durationSeconds = duration.inWholeSeconds
    return asSequence()
        .filter { it.matchesMetadataFor(artist, title, durationSeconds, album) }
        .filter { it.hasConservativeSyncedTiming(durationSeconds) }
        .minWithOrNull(compareBy<Track> { abs(it.duration - durationSeconds) }.thenBy(Track::id))
}

package app.it.fast4x.rimusic.ui.screens.album

import app.it.fast4x.rimusic.extensions.youtubelogin.YtmAlbumTrack
import app.it.fast4x.rimusic.models.Song
import it.fast4x.innertube.Innertube
import java.util.Locale

private val albumIdentityWhitespace = Regex("\\s+")
private val albumPresentationSuffix = Regex(
    "(?i)\\s*(?:[-:]\\s*)?(?:\\[|\\()?\\s*(?:official\\s+)?(?:(?:music|lyric)\\s+)?(?:video|audio)(?:\\s+clip)?\\s*(?:\\]|\\))?\\s*$"
)
private val albumVideoSuffix = Regex(
    "(?i)\\s*(?:[-:]\\s*)?(?:\\[|\\()?\\s*(?:official\\s+)?(?:(?:music|lyric)\\s+)?video(?:\\s+clip)?\\s*(?:\\]|\\))?\\s*$"
)
private val albumArtistChannelSuffix = Regex("(?i)(?:\\s*-\\s*topic|\\s*vevo|\\s+official)$")

private fun String?.albumIdentityPart(): String =
    this.orEmpty()
        .trim()
        .lowercase(Locale.ROOT)
        .replace(albumIdentityWhitespace, " ")

private fun String?.albumTitleIdentity(): String =
    albumIdentityPart()
        .replace(albumPresentationSuffix, "")
        .trim()

private fun String?.albumArtistIdentity(): String =
    albumIdentityPart()
        .replace(albumArtistChannelSuffix, "")
        .trim()

private fun String?.isAlbumVideoVersion(): Boolean = albumVideoSuffix.containsMatchIn(orEmpty().trim())

private inline fun <T> List<T>.deduplicateAlbumVersionsBy(
    identity: (T) -> String,
    preference: (T) -> Int = { 0 }
): List<T> {
    val selectedByIdentity = linkedMapOf<String, Pair<Int, T>>()
    val passthrough = mutableListOf<Pair<Int, T>>()

    forEachIndexed { index, item ->
        val currentIdentity = identity(item)
        if (currentIdentity.isBlank()) {
            passthrough += index to item
        } else {
            val candidate = preference(item) to item
            val selected = selectedByIdentity[currentIdentity]
            if (selected == null || candidate.first < selected.first) {
                selectedByIdentity[currentIdentity] = candidate
            }
        }
    }

    val resolved = ArrayList<T>(selectedByIdentity.size + passthrough.size)
    val passthroughByIndex = passthrough.associate { it.first to it.second }
    val consumedIdentities = mutableSetOf<String>()

    forEachIndexed { index, item ->
        passthroughByIndex[index]?.let {
            resolved += it
            return@forEachIndexed
        }

        val currentIdentity = identity(item)
        if (currentIdentity.isBlank() || currentIdentity in consumedIdentities) return@forEachIndexed

        selectedByIdentity[currentIdentity]?.second?.let(resolved::add)
        consumedIdentities += currentIdentity
    }

    return resolved
}

internal fun List<YtmAlbumTrack>.deduplicateAlbumTracks(
    fallbackArtist: String
): List<YtmAlbumTrack> = deduplicateAlbumVersionsBy(
    identity = { track ->
        listOf(
            track.title.albumTitleIdentity(),
            track.artistsText.ifBlank { fallbackArtist }.albumArtistIdentity()
        ).joinToString("|")
    },
    preference = { track -> if (track.title.isAlbumVideoVersion()) 2 else 0 }
)

internal fun List<Innertube.SongItem>.deduplicateAlbumItems(): List<Innertube.SongItem> =
    deduplicateAlbumVersionsBy(
        identity = { item ->
            listOf(
                item.title.albumTitleIdentity(),
                item.authors.orEmpty()
                    .joinToString(",") { it.name.orEmpty() }
                    .albumArtistIdentity()
            ).joinToString("|")
        },
        preference = { item ->
            when (
                item.info
                    ?.endpoint
                    ?.watchEndpointMusicSupportedConfigs
                    ?.watchEndpointMusicConfig
                    ?.musicVideoType
            ) {
                "MUSIC_VIDEO_TYPE_ATV" -> 0
                "MUSIC_VIDEO_TYPE_OMV", "MUSIC_VIDEO_TYPE_UGC" -> 2
                else -> if (item.title.isAlbumVideoVersion()) 2 else 1
            }
        }
    )

internal fun List<Song>.deduplicateAlbumSongs(): List<Song> =
    deduplicateAlbumVersionsBy(
        identity = { song ->
            listOf(
                song.title.albumTitleIdentity(),
                song.artistsText.albumArtistIdentity()
            ).joinToString("|")
        },
        preference = { song -> if (song.title.isAlbumVideoVersion()) 2 else 0 }
    )

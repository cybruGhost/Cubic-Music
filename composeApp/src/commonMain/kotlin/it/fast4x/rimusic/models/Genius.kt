package it.fast4x.rimusic.models


data class GeniusSearchResponse(
    val response: Response
) {
    data class Response(
        val hits: List<Hit>
    )

    data class Hit(
        val result: Song
    )

    data class Song(
        val id: Long,
        val title: String,
        val url: String,
        val primary_artist: Artist,
        val song_art_image_url: String?
    )

    data class Artist(
        val name: String
    )
}

data class LyricsResult(
    val title: String,
    val artist: String,
    val lyrics: String,
    val url: String
)

package it.fast4x.genius.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeniusSearchResponse(
    val response: Response
) {
    @Serializable
    data class Response(
        val hits: List<Hit>
    )

    @Serializable
    data class Hit(
        val result: Song
    )

    @Serializable
    data class Song(
        val id: Long,
        val title: String,
        val url: String,
        @SerialName("primary_artist")
        val primaryArtist: Artist
    )

    @Serializable
    data class Artist(
        val name: String
    )
}
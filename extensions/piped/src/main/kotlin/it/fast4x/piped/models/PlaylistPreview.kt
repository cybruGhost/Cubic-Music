package it.fast4x.piped.models

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class CreatedPlaylist(
    @SerialName("playlistId")
    val id: UUIDString
)

@Serializable
data class PlaylistPreview(
    val id: UUIDString,
    val name: String,
    @SerialName("shortDescription")
    val description: String? = null,
    @SerialName("thumbnail")
    val thumbnailUrl: UrlString,
    @SerialName("videos")
    val videoCount: Int
) {
    //val UUIDtoBrowseId: String
    //    get() = id.toString().replace("-", "")


}

@Serializable
data class Playlist(
    val name: String,
    val thumbnailUrl: UrlString,
    val description: String? = null,
    val bannerUrl: UrlString? = null,
    @SerialName("videos")
    val videoCount: Int,
    @SerialName("relatedStreams")
    val videos: List<Video>
) {
    @Serializable
    data class Video(
        val url: String, // not a real url, why?
        val title: String,
        @SerialName("thumbnail")
        val thumbnailUrl: UrlString,
        val uploaderName: String,
        val uploaderUrl: String, // not a real url either
        @SerialName("uploaderAvatar")
        val uploaderAvatarUrl: UrlString? = null,
        @SerialName("duration")
        val durationSeconds: Long
    ) {
        val cleanTitle: String
            get() = title.split("-", ignoreCase = true).let {
                println("pipedInfo: $title $it ${it.size}")
                return if (it.size > 1) it[1].trim()
                else title
            }

        val cleanArtists: String
            get() = title.split("-", ignoreCase = true).let {
                println("pipedInfo: $title $it ${it.size}")
                return if (it.size > 1) it[0].trim()
                else title
            }

        val id
            get() = if (url.startsWith("/watch?v=")) url.substringAfter("/watch?v=")
            else Url(url).parameters["v"]?.firstOrNull()?.toString()

        val uploaderId
            get() = if (uploaderUrl.startsWith("/channel/")) uploaderUrl.substringAfter("/channel/")
            else Url(uploaderUrl).rawSegments.lastOrNull()

        val duration get() = durationSeconds.seconds
        val durationText: String
            get() {
                val minutes = duration.inWholeMinutes
                val seconds = duration.inWholeSeconds % 60
                return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
            }
    }
}

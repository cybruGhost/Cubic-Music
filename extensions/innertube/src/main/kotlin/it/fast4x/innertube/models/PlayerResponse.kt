package it.fast4x.innertube.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private fun JsonElement?.intValue(): Int? = when (this) {
    null -> null
    is JsonPrimitive -> contentOrNull?.toDoubleOrNull()?.toInt()
    else -> null
}

private fun JsonElement?.longValue(): Long? = when (this) {
    null -> null
    is JsonPrimitive -> contentOrNull?.toDoubleOrNull()?.toLong()
    else -> null
}

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
    @SerialName("playbackTracking")
    val playbackTracking: PlaybackTracking?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?,
        val reason: String?
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            val loudnessDb: Float?
        ) {
            val normalizedLoudnessDb: Float?
                get() = loudnessDb?.plus(7)
        }
    }

    @Serializable
    data class StreamingData(
        val formats: List<Format>?,
        val adaptiveFormats: List<Format>?,
        val expiresInSeconds: JsonElement?,
    ) {
        val autoMaxQualityFormat: Format?
            get() = adaptiveFormats?.filter { it.url != null || it.signatureCipher != null }
                ?.let { formats ->
                    listOf(251, 774, 141, 140, 250, 249, 139, 171)
                        .firstNotNullOfOrNull { preferredItag -> formats.firstOrNull { it.itagValue == preferredItag } }
                        ?: formats.maxByOrNull { it.bitrateValue ?: 0 }
                }

        val highestQualityFormat: Format?
            get() = adaptiveFormats?.filter { it.url != null || it.signatureCipher != null }
                ?.let { formats ->
                    listOf(251, 774, 141, 140)
                        .firstNotNullOfOrNull { preferredItag -> formats.firstOrNull { it.itagValue == preferredItag } }
                        ?: formats.maxByOrNull { it.bitrateValue ?: 0 }
                }

        val mediumQualityFormat: Format?
            get() = adaptiveFormats?.filter { it.url != null || it.signatureCipher != null }
                ?.let { formats ->
                    listOf(140, 250)
                        .firstNotNullOfOrNull { preferredItag -> formats.firstOrNull { it.itagValue == preferredItag } }
                        ?: formats.maxByOrNull { it.bitrateValue ?: 0 }
                }

        val lowestQualityFormat: Format?
            get() = adaptiveFormats?.filter { it.url != null || it.signatureCipher != null }
                ?.let { formats ->
                    listOf(139, 249, 171)
                        .firstNotNullOfOrNull { preferredItag -> formats.firstOrNull { it.itagValue == preferredItag } }
                        ?: formats.minByOrNull { it.bitrateValue ?: Int.MAX_VALUE }
                }

        @Serializable
        data class Format(
            val itag: JsonElement?,
            val url: String?,
            val mimeType: String,
            val bitrate: JsonElement?,
            val width: JsonElement?,
            val height: JsonElement?,
            val contentLength: JsonElement?,
            val quality: String,
            val fps: JsonElement?,
            val qualityLabel: String?,
            val averageBitrate: JsonElement?,
            val audioQuality: String?,
            val approxDurationMs: String?,
            val audioSampleRate: JsonElement?,
            val audioChannels: JsonElement?,
            val loudnessDb: Double?,
            val lastModified: JsonElement?,
            val signatureCipher: String?,
        ) {
            val itagValue: Int?
                get() = itag.intValue()

            val bitrateValue: Int?
                get() = bitrate.intValue()

            val widthValue: Int?
                get() = width.intValue()

            val heightValue: Int?
                get() = height.intValue()

            val contentLengthValue: Long?
                get() = contentLength.longValue()

            val fpsValue: Int?
                get() = fps.intValue()

            val averageBitrateValue: Int?
                get() = averageBitrate.intValue()

            val audioSampleRateValue: Int?
                get() = audioSampleRate.intValue()

            val audioChannelsValue: Int?
                get() = audioChannels.intValue()

            val lastModifiedValue: Long?
                get() = lastModified.longValue()

            val isAudio: Boolean
                get() = mimeType.startsWith("audio/", ignoreCase = true)

            val isVideo: Boolean
                get() = mimeType.startsWith("video/", ignoreCase = true)
        }
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?,
        val title: String?,
        val author: String?,
        val channelId: String?,
        val authorAvatar: String?,
        val authorSubCount: String?,
        val lengthSeconds: String?,
        val musicVideoType: String?,
        val viewCount: String?,
        val thumbnail: Thumbnails?,
        val description: String?,
    )

    @Serializable
    data class PlaybackTracking(
        @SerialName("videostatsPlaybackUrl")
        val videostatsPlaybackUrl: VideostatsPlaybackUrl?,
        @SerialName("videostatsWatchtimeUrl")
        val videostatsWatchtimeUrl: VideostatsWatchtimeUrl?,
        @SerialName("atrUrl")
        val atrUrl: AtrUrl?,
    ) {
        @Serializable
        data class VideostatsPlaybackUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )

        @Serializable
        data class VideostatsWatchtimeUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
        @Serializable
        data class AtrUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
    }
}

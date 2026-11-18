package it.fast4x.innertube

import it.fast4x.innertube.clients.YouTubeClient
import it.fast4x.innertube.clients.YouTubeLocale
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.MusicResponsiveListItemRenderer
import it.fast4x.innertube.requests.AlbumPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class InnertubeAudioRequestTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val locale = YouTubeLocale(gl = "US", hl = "en")

    @Test
    fun normalPlaybackRetainsHeaderUserAgentWithoutAddingItToBody() {
        val client = YouTubeClient.IOS.toContext(locale, "visitor").client
        assertEquals(YouTubeClient.IOS.userAgent, client.userAgent)
        val body = json.encodeToJsonElement(Context.Client.serializer(), client).jsonObject
        assertFalse("userAgent" in body)
    }

    @Test
    fun innertubeXProfileSerializesItsUserAgentAndRetainsItOnContextCopy() {
        val profile = YouTubeClient.ANDROID_VR_1_65_10
        val client = profile.toContext(locale, "visitor").client
            .toContext(locale, "next-visitor").client
        assertEquals(profile.userAgent, client.userAgent)
        val body = json.encodeToJsonElement(Context.Client.serializer(), client).jsonObject
        assertEquals(profile.userAgent, body["userAgent"]?.jsonPrimitive?.content)
    }

    @Test
    fun albumPrefersAnAudioReleaseEndpointOverItsVideoVariant() {
        val renderer = json.decodeFromString<MusicResponsiveListItemRenderer>(
            """
            {
              "playlistItemData": {"videoId": "video123456"},
              "navigationEndpoint": {"watchEndpoint": {"videoId": "video123456"}},
              "flexColumns": [{
                "musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{
                  "text": "Track",
                  "navigationEndpoint": {"watchEndpoint": {
                    "videoId": "audio123456",
                    "watchEndpointMusicSupportedConfigs": {
                      "watchEndpointMusicConfig": {"musicVideoType": "MUSIC_VIDEO_TYPE_ATV"}
                    }
                  }}
                }]}}
              }]
            }
            """.trimIndent()
        )
        val song = AlbumPage.getSong(renderer)
        assertEquals("audio123456", song.info.endpoint?.videoId)
        assertEquals("MUSIC_VIDEO_TYPE_ATV", song.info.endpoint?.type)
        assertNull(song.thumbnail)
    }

    @Test
    fun albumKeepsItsOriginalIdWhenNoAudioAlternativeWasSupplied() {
        val renderer = json.decodeFromString<MusicResponsiveListItemRenderer>(
            """{"flexColumns": [], "playlistItemData": {"videoId": "track123456"}}"""
        )
        val song = AlbumPage.getSong(renderer)
        assertEquals("track123456", song.info.endpoint?.videoId)
        assertNull(song.thumbnail)
    }
}

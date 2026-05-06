package app.it.fast4x.rimusic.utils

import android.net.Uri

object ExternalUris {
    private const val CUBIC_PACKAGE_NAME = "com.Cubic.music"
    private const val CUBIC_FALLBACK_BASE_URL = "https://thecub.netlify.app/cubicmusic"

    fun youtube(videoId: String) = "https://youtube.com/watch?v=$videoId"
    fun youtubeMusic(videoId: String) = "https://music.youtube.com/watch?v=$videoId"
    fun piped(videoId: String) = "https://piped.kavin.rocks/watch?v=$videoId&playerAutoPlay=true"
    fun invidious(videoId: String) = "https://yewtu.be/watch?v=$videoId&autoplay=1"

    fun youtubeMusicPlaylist(listId: String) = "https://music.youtube.com/playlist?list=$listId"
    fun youtubePlaylist(listId: String) = "https://youtube.com/playlist?list=$listId"
    fun youtubeMusicChannel(channelId: String) = "https://music.youtube.com/channel/$channelId"

    fun cubicMusicFallback(videoId: String): String =
        "$CUBIC_FALLBACK_BASE_URL?v=$videoId"

    fun cubicMusicSong(videoId: String): String {
        val fallbackUrl = Uri.encode(cubicMusicFallback(videoId))
        return "intent://cubicmusic/song/$videoId#Intent;" +
            "scheme=https;" +
            "package=$CUBIC_PACKAGE_NAME;" +
            "S.browser_fallback_url=$fallbackUrl;" +
            "end"
    }
}

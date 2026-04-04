package app.it.fast4x.rimusic.extensions.youtubelogin

data class YoutubeSession(
    val sessionId: String = "",
    val cookie: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val authUser: String = "",
    val pageId: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val accountThumbnail: String = "",
    val isPreferred: Boolean = false,
    val lastUsedAt: Long = 0L,
    val lastAccountRefreshAt: Long = 0L
)

data class YtmCloudSession(
    val hasSession: Boolean = false,
    val cookie: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val accountThumbnail: String = "",
    val expiresAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class YtmAccountInfo(
    val hasSession: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val accountThumbnail: String = ""
)

data class YtmApiSession(
    val hasSession: Boolean = false,
    val cookie: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val authUser: String = "",
    val pageId: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val accountThumbnail: String = ""
)

data class YtmLinkedAccount(
    val accountName: String = "",
    val accountEmail: String = "",
    val channelHandle: String = "",
    val accountThumbnail: String = "",
    val subscribers: String = "",
    val isSelected: Boolean = false,
    val authUser: String = "",
    val pageId: String = ""
)

data class YtmPlaylist(
    val playlistId: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val songCount: String = "",
    val subtitle: String = ""
)

data class YtmSong(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val thumbnail: String = "",
    val duration: String = ""
)

data class YtmArtist(
    val browseId: String = "",
    val name: String = "",
    val thumbnail: String = "",
    val subscribers: String = ""
)

data class YtmAlbum(
    val browseId: String = "",
    val playlistId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnail: String = "",
    val year: String = "",
    val type: String = ""
)

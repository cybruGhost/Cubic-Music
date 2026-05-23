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
    val authUser: String = "",
    val pageId: String = "",
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
    val browseId: String = "",
    val rawPlaylistId: String = "",
    val title: String = "",
    val name: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val songCount: String = "",
    val subtitle: String = ""
)

data class YtmArtistRef(
    val id: String = "",
    val name: String = ""
)

data class YtmSong(
    val id: String = "",
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val artistsText: String = "",
    val artistId: String = "",
    val artistIds: List<String> = emptyList(),
    val artists: List<YtmArtistRef> = emptyList(),
    val album: String = "",
    val albumId: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val duration: String = "",
    val durationText: String = "",
    val setVideoId: String = "",
    val position: Int = -1,
    val dateAdded: String = "",
    val isAvailable: Boolean = true
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

data class YtmArtistTrack(
    val videoId: String = "",
    val title: String = "",
    val album: String = "",
    val albumId: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val duration: String = ""
)

data class YtmArtistAlbumRef(
    val browseId: String = "",
    val playlistId: String = "",
    val title: String = "",
    val year: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val type: String = ""
)

data class YtmRelatedArtist(
    val browseId: String = "",
    val name: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val subscribers: String = ""
)

data class YtmArtistDetail(
    val browseId: String = "",
    val name: String = "",
    val description: String = "",
    val subscribers: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val songs: List<YtmArtistTrack> = emptyList(),
    val albums: List<YtmArtistAlbumRef> = emptyList(),
    val singles: List<YtmArtistAlbumRef> = emptyList(),
    val videos: List<YtmArtistTrack> = emptyList(),
    val related: List<YtmRelatedArtist> = emptyList()
)

data class YtmAlbumTrack(
    val videoId: String = "",
    val title: String = "",
    val artistsText: String = "",
    val artistIds: List<String> = emptyList(),
    val duration: String = "",
    val trackNumber: Int = 0,
    val thumbnail: String = "",
    val thumbnailUrl: String = ""
)

data class YtmAlbumDetail(
    val browseId: String = "",
    val playlistId: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val year: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val trackCount: Int = 0,
    val durationText: String = "",
    val tracks: List<YtmAlbumTrack> = emptyList()
)

data class YtmParseError(
    val where: String = "",
    val reason: String = "",
    val sample: String? = null
)

data class YtmHomeSection(
    val title: String = "",
    val subtitle: String = "",
    val browseId: String = "",
    val params: String = "",
    val type: String = "",
    val itemCount: Int = 0,
    val hasMore: Boolean = false,
    val items: List<YtmHomeSectionItem> = emptyList()
)

data class YtmHomeFeed(
    val sections: List<YtmHomeSection> = emptyList(),
    val continuation: String? = null,
    val hasMore: Boolean = false,
    val pagesFetched: Int = 0,
    val rawJson: String? = null,
    val parseErrors: List<YtmParseError> = emptyList()
)

data class YtmHomeSectionItem(
    val id: String = "",
    val videoId: String = "",
    val playlistId: String = "",
    val browseId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val artistsText: String = "",
    val artistId: String = "",
    val artistIds: List<String> = emptyList(),
    val album: String = "",
    val albumId: String = "",
    val thumbnail: String = "",
    val thumbnailUrl: String = "",
    val type: String = ""
)

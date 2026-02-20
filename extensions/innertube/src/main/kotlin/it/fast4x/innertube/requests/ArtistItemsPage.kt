package it.fast4x.innertube.requests

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.MusicResponsiveListItemRenderer
import it.fast4x.innertube.models.MusicTwoRowItemRenderer
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.getContinuation
import it.fast4x.innertube.models.oddElements
import it.fast4x.innertube.utils.from

data class ArtistItemsPage(
    val title: String,
    val items: List<Innertube.Item>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): Innertube.SongItem? {
            return Innertube.SongItem(
                info = Innertube.Info(
                    name = renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                        ?.text ?: "",
                    endpoint = NavigationEndpoint.Endpoint.Watch(
                        videoId = renderer.playlistItemData?.videoId
                    )
                ),
                authors = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.oddElements()
                    ?.map {
                        Innertube.Info(
                            name = it.text,
                            endpoint = it.navigationEndpoint?.browseEndpoint
                        )
                    } ?: emptyList(),
                album = renderer.flexColumns.getOrNull(3)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.let {
                        Innertube.Info(
                            name = it.text,
                            endpoint = it.navigationEndpoint?.browseEndpoint
                        )
                    },
                durationText = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()
                    ?.text,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                //endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): Innertube.Item? {
            return when {
                renderer.isAlbum -> Innertube.AlbumItem(
                    info = Innertube.Info(
                        renderer.title?.runs?.firstOrNull()?.text,
                        renderer.navigationEndpoint?.browseEndpoint
                    ),
                    authors = null,
                    year = renderer.subtitle?.runs?.lastOrNull()?.text,
                    thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
//                    explicit = renderer.subtitleBadges?.find {
//                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
//                    } != null
                )
                // Video
                renderer.isSong -> {
                    val subtitleParts = renderer.subtitle?.splitBySeparator() ?: emptyList()
                    Innertube.VideoItem(
                        info = Innertube.Info(
                            renderer.title?.runs?.firstOrNull()?.text,
                            renderer.navigationEndpoint?.watchEndpoint
                        ),
                        authors = subtitleParts.getOrNull(0)?.map {
                            Innertube.Info(
                                name = it.text,
                                endpoint = it.navigationEndpoint?.browseEndpoint
                            )
                        },
                        durationText = subtitleParts.getOrNull(
                            if (subtitleParts.size >= 3) subtitleParts.lastIndex else -1
                        )?.firstOrNull()?.text,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                        viewsText = subtitleParts.getOrNull(
                            if (subtitleParts.size >= 3) subtitleParts.lastIndex - 1
                            else if (subtitleParts.size == 2) 1
                            else -1
                        )?.firstOrNull()?.text,
                    )
                }
                renderer.isPlaylist -> Innertube.PlaylistItem(
                    info = Innertube.Info(
                        renderer.title?.runs?.firstOrNull()?.text,
                        renderer.navigationEndpoint?.browseEndpoint
                    ),
                    songCount = renderer.subtitle?.runs?.getOrNull(4)?.text?.toInt(),
                    thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                    channel = null,
                    isEditable = false
                )
                else -> null
            }
        }

        fun fromMusicShelfRenderer(renderer: it.fast4x.innertube.models.MusicShelfRenderer): ArtistItemsPage? {
            return ArtistItemsPage(
                title = renderer.title?.runs?.firstOrNull()?.text.orEmpty(),
                items = renderer.contents?.mapNotNull { content ->
                    val songItem = content.musicResponsiveListItemRenderer?.let { fromMusicResponsiveListItemRenderer(it) }
                    if (songItem != null && songItem.album == null) {
                        Innertube.VideoItem.from(content) ?: songItem
                    } else {
                        songItem ?: Innertube.VideoItem.from(content)
                    }
                }.orEmpty(),
                continuation = renderer.continuations?.getContinuation()
            )
        }
    }
}
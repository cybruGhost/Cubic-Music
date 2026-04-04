package it.fast4x.innertube.requests

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.innertube.models.MusicCarouselShelfRenderer
import it.fast4x.innertube.models.MusicResponsiveListItemRenderer
import it.fast4x.innertube.models.MusicTwoRowItemRenderer
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.oddElements
import it.fast4x.innertube.models.splitBySeparator
import kotlinx.serialization.Serializable

@Serializable
data class HomePage(
    val chips: List<Chip>? = null,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    @Serializable
    data class Chip(
        val title: String,
        val endpoint: NavigationEndpoint.Endpoint.Browse?,
        val deselectEndPoint: NavigationEndpoint.Endpoint.Browse?,
        val isSelected: Boolean = false,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: it.fast4x.innertube.models.SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                val chipRenderer = renderer.chipCloudChipRenderer
                return Chip(
                    title = chipRenderer.text?.runs?.firstOrNull()?.text ?: return null,
                    endpoint = chipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = chipRenderer.onDeselectedCommand?.browseEndpoint,
                    isSelected = chipRenderer.isSelected
                )
            }
        }
    }

    @Serializable
    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<Innertube.Item?>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                val header = renderer.header?.musicCarouselShelfBasicHeaderRenderer ?: return null
                val title = header.title?.runs?.firstOrNull()?.text ?: return null

                val items = buildList {
                    renderer.contents.mapNotNull { fromMusicTwoRowItemRenderer(it.musicTwoRowItemRenderer) }.let(::addAll)
                    renderer.contents.mapNotNull { fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer) }.let(::addAll)
                }

                if (items.isEmpty()) return null

                return Section(
                    title = title,
                    label = header.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = header.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = header.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.let {
                        BrowseEndpoint(
                            browseId = it.browseId ?: return@let null,
                            params = it.params,
                        )
                    },
                    items = items,
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer?): Innertube.Item? {
                renderer ?: return null

                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator().orEmpty()
                        Innertube.SongItem(
                            info = Innertube.Info(
                                renderer.title?.runs?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = subtitleRuns.firstOrNull()?.oddElements()?.map {
                                Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                            },
                            album = subtitleRuns.getOrNull(1)?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.let { endpoint ->
                                Innertube.Info(
                                    name = subtitleRuns.getOrNull(1)?.firstOrNull()?.text,
                                    endpoint = endpoint
                                )
                            },
                            durationText = subtitleRuns.lastOrNull()?.firstOrNull()?.text,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }

                    renderer.isVideo -> {
                        val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator().orEmpty()
                        Innertube.VideoItem(
                            info = Innertube.Info(
                                renderer.title?.runs?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = subtitleRuns.firstOrNull()?.oddElements()?.map {
                                Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                            },
                            viewsText = subtitleRuns.lastOrNull()?.firstOrNull()?.text,
                            durationText = null,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                        )
                    }

                    renderer.isAlbum -> Innertube.AlbumItem(
                        info = Innertube.Info(
                            renderer.title?.runs?.firstOrNull()?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        authors = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                            Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                        },
                        year = renderer.subtitle?.runs?.lastOrNull()?.text,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    renderer.isPlaylist -> Innertube.PlaylistItem(
                        info = Innertube.Info(
                            renderer.title?.runs?.firstOrNull()?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        channel = renderer.subtitle?.runs?.firstOrNull()?.let {
                            Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                        },
                        songCount = null,
                        isEditable = false,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    renderer.isArtist -> Innertube.ArtistItem(
                        info = Innertube.Info(
                            renderer.title?.runs?.firstOrNull()?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        subscribersCountText = renderer.subtitle?.runs?.lastOrNull()?.text,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    else -> null
                }
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer?): Innertube.Item? {
                renderer ?: return null

                return when {
                    renderer.isVideo -> {
                        val secondaryRuns = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.splitBySeparator()
                            .orEmpty()

                        Innertube.VideoItem(
                            info = Innertube.Info(
                                renderer.flexColumns.firstOrNull()
                                    ?.musicResponsiveListItemFlexColumnRenderer
                                    ?.text
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = secondaryRuns.firstOrNull()?.oddElements()?.map {
                                Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                            },
                            viewsText = secondaryRuns.lastOrNull()?.firstOrNull()?.text,
                            durationText = renderer.fixedColumns?.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                        )
                    }

                    renderer.isSong -> {
                        val secondaryRuns = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.splitBySeparator()
                            ?: return null

                        Innertube.SongItem(
                            info = Innertube.Info(
                                renderer.flexColumns.firstOrNull()
                                    ?.musicResponsiveListItemFlexColumnRenderer
                                    ?.text
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = secondaryRuns.firstOrNull()?.oddElements()?.map {
                                Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
                            },
                            album = secondaryRuns.getOrNull(1)?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.let { endpoint ->
                                Innertube.Info(
                                    name = secondaryRuns.getOrNull(1)?.firstOrNull()?.text,
                                    endpoint = endpoint
                                )
                            },
                            durationText = renderer.fixedColumns?.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: secondaryRuns.lastOrNull()?.firstOrNull()?.text,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                            explicit = renderer.badges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            setVideoId = renderer.playlistItemData?.playlistSetVideoId
                        )
                    }

                    renderer.isAlbum -> Innertube.AlbumItem(
                        info = Innertube.Info(
                            renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        authors = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.oddElements()
                            ?.map { Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint) },
                        year = renderer.flexColumns.getOrNull(2)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.lastOrNull()
                            ?.text,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    renderer.isPlaylist -> Innertube.PlaylistItem(
                        info = Innertube.Info(
                            renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        channel = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.let { Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint) },
                        songCount = null,
                        isEditable = false,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    renderer.isArtist -> Innertube.ArtistItem(
                        info = Innertube.Info(
                            renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text,
                            renderer.navigationEndpoint?.browseEndpoint
                        ),
                        subscribersCountText = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    )

                    else -> null
                }
            }
        }
    }
}

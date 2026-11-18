package it.fast4x.innertube

import io.ktor.client.call.body
import it.fast4x.innertube.Innertube.getBestQuality
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.innertube.models.BrowseResponse
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.CreatePlaylistResponse
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.getContinuation
import it.fast4x.innertube.models.oddElements
import it.fast4x.innertube.requests.AlbumPage
import it.fast4x.innertube.requests.ArtistItemsContinuationPage
import it.fast4x.innertube.requests.ArtistItemsPage
import it.fast4x.innertube.requests.ArtistPage
import it.fast4x.innertube.requests.HistoryPage
import it.fast4x.innertube.requests.HomePage
import it.fast4x.innertube.requests.NewReleaseAlbumPage
import it.fast4x.innertube.requests.PlaylistContinuationPage
import it.fast4x.innertube.requests.PlaylistPage

object YtMusic {

    const val PLAYLIST_SIZE_LIMIT = 5000

    suspend fun createPlaylist(title: String) = runCatching {
        Innertube.createPlaylist(Context.DefaultWeb.client, title).body<CreatePlaylistResponse>().playlistId
    }.onFailure {
        println("YtMusic createPlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun deletePlaylist(playlistId: String) = runCatching {
        Innertube.deletePlaylist(Context.DefaultWeb.client, playlistId)
    }.onFailure {
        println("YtMusic deletePlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = runCatching {
        Innertube.renamePlaylist(Context.DefaultWeb.client, playlistId, name)
    }.onFailure {
        println("YtMusic renamePlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) = runCatching {
        Innertube.addToPlaylist(Context.DefaultWeb.client, playlistId, videoId)
    }.onFailure {
        println("YtMusic addToPlaylist(single) error: ${it.stackTraceToString()}")
    }

    suspend fun addToPlaylist(playlistId: String, videoIds: List<String>) = runCatching {
        val requestedVideoIds = videoIds.take(PLAYLIST_SIZE_LIMIT)
        val difference = videoIds.size - requestedVideoIds.size
        if (difference > 0) {
            println("YtMusic addToPlaylist warning: only adding (at most) $PLAYLIST_SIZE_LIMIT ids, (surpassed limit by $difference)")
        }
        Innertube.addToPlaylist(Context.DefaultWeb.client, playlistId, requestedVideoIds)
    }.onFailure {
        println("YtMusic addToPlaylist (list of size ${videoIds.size}) error: ${it.stackTraceToString()}")
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String? = null) = runCatching {
        println("YtMusic removeFromPlaylist params: playlistId: $playlistId, videoId: $videoId, setVideoId: $setVideoId")
            Innertube.removeFromPlaylist(Context.DefaultWeb.client, playlistId, videoId, setVideoId)
        }.onFailure {
            println("YtMusic removeFromPlaylist error: ${it.stackTraceToString()}")
        }

    suspend fun addPlaylistToPlaylist(playlistId: String, videoId: String) = runCatching {
        Innertube.addPlaylistToPlaylist(Context.DefaultWeb.client, playlistId, videoId)
    }.onFailure {
        println("YtMusic addPlaylistToPlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoIds: List<String?>) = runCatching {
        Innertube.removeFromPlaylist(Context.DefaultWeb.client, playlistId, videoId, setVideoIds)
    }.onFailure {
        println("YtMusic removeFromPlaylist (list of size ${setVideoIds.size}) error: ${it.stackTraceToString()}")
    }

    suspend fun subscribeChannel(channelId: String) = runCatching {
        println("YtMusic subscribeChannel channelId: $channelId")
        Innertube.subscribeChannel(channelId)
    }.onFailure {
        println("YtMusic subscribeChannel error: ${it.stackTraceToString()}")
    }

    suspend fun unsubscribeChannel(channelId: String) = runCatching {
        println("YtMusic unsubscribeChannel channelId: $channelId")
        Innertube.unsubscribeChannel(channelId)
    }.onFailure {
        println("YtMusic unsubscribeChannel error: ${it.stackTraceToString()}")
    }

    suspend fun likePlaylistOrAlbum(playlistId: String) = runCatching {
        println("YtMusic likePlaylistOrAlbum playlistId: $playlistId")
        Innertube.likePlaylistOrAlbum(playlistId)
    }.onFailure {
        println("YtMusic likePlaylistOrAlbum error: ${it.stackTraceToString()}")
    }

    suspend fun removelikePlaylistOrAlbum(playlistId: String) = runCatching {
        println("YtMusic removelikePlaylistOrAlbum playlistId: $playlistId")
        Innertube.removelikePlaylistOrAlbum(playlistId)
    }.onFailure {
        println("YtMusic removelikePlaylistOrAlbum error: ${it.stackTraceToString()}")
    }

    suspend fun likeVideoOrSong(VideoId: String) = runCatching {
        println("YtMusic likeVideoOrSong VideoId: $VideoId")
        Innertube.likeVideoOrSong(VideoId)
    }.onFailure {
        println("YtMusic likeVideoOrSong error: ${it.stackTraceToString()}")
    }

    suspend fun removelikeVideoOrSong(VideoId: String) = runCatching {
        println("YtMusic removelikeVideoOrSong playlistIdId: $VideoId")
        Innertube.removelikeVideoOrSong(VideoId)
    }.onFailure {
        println("YtMusic removelikeVideoOrSong error: ${it.stackTraceToString()}")
    }

    suspend fun getHomePage(setLogin: Boolean = false, params: String? = null): Result<HomePage> = runCatching {

        var response = Innertube.browse(
            browseId = "FEmusic_home",
            params = params,
            setLogin = setLogin
        ).body<BrowseResponse>()

        println("homePage() response sections: ${response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents}" )


        var continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()

        val sectionListRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer

        val sections = sectionListRenderer?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()
        val chips = sectionListRenderer.header?.chipCloudRenderer?.chips?.mapNotNull {
            HomePage.Chip.fromChipCloudChipRenderer(it)
        }
        while (continuation != null) {
            println("gethomePage() continuation before:  ${continuation}" )
            response = Innertube.browse(continuation = continuation).body<BrowseResponse>()
            continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
            println("gethomePage() continuation after:  ${continuation}" )

            sections += response.continuationContents?.sectionListContinuation?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }.orEmpty()

        }
        HomePage(
            chips = chips,
            sections = sections.distinctBy { it.title to it.label },
            continuation = continuation
        )
    }

    suspend fun getHistory(setLogin: Boolean = false): Result<HistoryPage> = runCatching {

        val response = Innertube.browse(browseId = "FEmusic_history", setLogin = setLogin)
            .body<BrowseResponse>()

        println("getHistory() response sections: ${response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents}" )

        HistoryPage(
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull {
                    it.musicShelfRenderer?.let { musicShelfRenderer ->
                        HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                    }
                }
        )

    }

    suspend fun getArtistPage(browseId: String, setLogin: Boolean = false): Result<ArtistPage> = runCatching {
        val response = Innertube.browse(browseId = browseId, setLogin = setLogin).body<BrowseResponse>()
        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!

        ArtistPage(
            artist = Innertube.ArtistItem(
                info = Innertube.Info(
                    name = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                        params = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.params
                    )
                ),
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                channelId = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId,
                subscribersCountText = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscriberCountText?.runs?.firstOrNull()?.text,
            ),
            sections = sections,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text,
            subscribers = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.longSubscriberCountText?.text,
            shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
            radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
        )
    }

    suspend fun getArtistItemsPage(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = Innertube.browse(browseId = endpoint.browseId, params = endpoint.params).body<BrowseResponse>()

        val contents = (response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: response.contents?.sectionListRenderer?.contents
            ?: emptyList())

        val gridRenderer = contents.firstNotNullOfOrNull { it.gridRenderer }
        val musicShelfRenderer = contents.firstNotNullOfOrNull { it.musicShelfRenderer }
        val musicPlaylistShelfRenderer = contents.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

        when {
            gridRenderer != null -> {
                ArtistItemsPage(
                    title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = gridRenderer.items!!.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                        }
                    },
                    continuation = gridRenderer.continuations?.getContinuation()
                )
            }
            musicShelfRenderer != null -> {
                ArtistItemsPage.fromMusicShelfRenderer(musicShelfRenderer)!!
            }
            musicPlaylistShelfRenderer != null -> {
                ArtistItemsPage(
                    title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = musicPlaylistShelfRenderer.contents?.mapNotNull {
                        it.musicResponsiveListItemRenderer?.let { it1 ->
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it1)
                        }
                    }!!,
                    continuation = musicPlaylistShelfRenderer.contents?.lastOrNull()
                        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                )
            }
            else -> {
                // Fallback or empty
                ArtistItemsPage(
                    title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = emptyList(),
                    continuation = null
                )
            }
        }
    }.onFailure {
        println("YtMusic getArtistItemsPage() error: ${it.stackTraceToString()}")
    }

    suspend fun getPlaylist(playlistId: String): Result<PlaylistPage> = runCatching {
        val playlistIdChecked = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        println("YtMusic getPlaylist playlistId: $playlistId Checked: $playlistIdChecked")
        val response = Innertube.browse(
            browseId = playlistIdChecked,
            setLogin = true
        ).body<BrowseResponse>()


        if (response.contents?.twoColumnBrowseResultsRenderer != null)
            getPlaylistNewMode(playlistIdChecked, response)
        else
            getPlaylistPreviousMode(playlistIdChecked, response)
    }.onFailure {
        println("YtMusic getPlaylist error: ${it.stackTraceToString()}")
    }

    private fun getPlaylistPreviousMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.header?.musicDetailHeaderRenderer ?:
            response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer


        val editable = response.header?.musicEditablePlaylistDetailHeaderRenderer != null
        val songSection = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer
        val songShelf = songSection?.contents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                songCount = 0, //header.secondSubtitle.runs?.firstOrNull()?.text,
                thumbnail = header?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null,
                isEditable = editable,
//                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
//                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
//                    ?.musicPlaylistShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer
//                    ?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.menu.menuRenderer.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
//                radioEndpoint = header.menu.menuRenderer.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,

            ),
            description = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
                ?.description?.musicDescriptionShelfRenderer?.description?.text,
            songs = songShelf?.contents.orEmpty().mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                },
            songsContinuation = songShelf?.continuations?.getContinuation()
                ?: songShelf?.contents?.lastOrNull()?.continuationItemRenderer
                    ?.continuationEndpoint?.continuationCommand?.token
                ?: songSection?.continuations?.getContinuation(),
            continuation = songSection?.continuations?.getContinuation()
        )
    }

    private fun getPlaylistNewMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
            ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

        val isEditable = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicEditablePlaylistDetailHeaderRenderer != null
        val songSection = response.contents?.twoColumnBrowseResultsRenderer
            ?.secondaryContents?.sectionListRenderer
        val songShelf = songSection?.contents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

        println("getPlaylist new mode editable : ${isEditable}")

//        println("getPlaylist new mode description: ${response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
//            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.description?.musicDescriptionShelfRenderer?.description}")

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                songCount = 0,//header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = response.background?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null,
                isEditable = isEditable,
//                playEndpoint = header.buttons.getOrNull(1)?.musicPlayButtonRenderer
//                    ?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
//                radioEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
            ),
            description = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
                ?.description?.musicDescriptionShelfRenderer?.description?.text,
            songs = songShelf?.contents.orEmpty().mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                },
            songsContinuation = songShelf?.continuations?.getContinuation()
                ?: songShelf?.contents?.lastOrNull()?.continuationItemRenderer
                    ?.continuationEndpoint?.continuationCommand?.token
                ?: songSection?.continuations?.getContinuation(),
            continuation = songSection?.continuations?.getContinuation(),
            isEditable = isEditable
        )
    }

    suspend fun getPlaylistContinuation(continuation: String) = runCatching {
        val response = Innertube.browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()
        val actionItems = response.onResponseReceivedActions.orEmpty()
            .flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }
        val playlistShelf = response.continuationContents?.musicPlaylistShelfContinuation
        val musicShelf = response.continuationContents?.musicShelfContinuation
        val section = response.continuationContents?.sectionListContinuation
        val sectionShelves = section?.contents.orEmpty().flatMap { content ->
            content.musicPlaylistShelfRenderer?.contents.orEmpty() +
                content.musicShelfRenderer?.contents.orEmpty()
        }
        val songs = (
            actionItems.mapNotNull { it.musicResponsiveListItemRenderer } +
                playlistShelf?.contents.orEmpty().mapNotNull { it.musicResponsiveListItemRenderer } +
                musicShelf?.contents.orEmpty().mapNotNull { it.musicResponsiveListItemRenderer } +
                sectionShelves.mapNotNull { it.musicResponsiveListItemRenderer }
            ).map(PlaylistPage::fromMusicResponsiveListItemRenderer)
            .distinctBy(Innertube.SongItem::key)

        val next = actionItems.asReversed().firstNotNullOfOrNull {
            it.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                ?.takeIf(String::isNotBlank)
        } ?: playlistShelf?.continuations?.getContinuation()
            ?: musicShelf?.continuations?.getContinuation()
            ?: section?.continuations?.getContinuation()
            ?: sectionShelves.asReversed().firstNotNullOfOrNull {
                it.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                    ?.takeIf(String::isNotBlank)
            }

        PlaylistContinuationPage(songs = songs, continuation = next)

    }.onFailure {
        println("YtMusic getPlaylistContinuation error: ${it.stackTraceToString()}")
    }

    suspend fun getArtistItemsContinuation(continuation: String) = runCatching {
        val response = Innertube.browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        response.onResponseReceivedActions?.map {
            it.appendContinuationItemsAction?.continuationItems?.mapNotNull { it1 ->
                it1.musicResponsiveListItemRenderer?.let { it2 ->
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(
                        it2
                    )
                }
            }
        }?.let {
            it.firstOrNull()?.let { it1 ->
                ArtistItemsContinuationPage(
                    items = it1,
                    continuation = response.onResponseReceivedActions.firstOrNull()
                        ?.appendContinuationItemsAction?.continuationItems?.lastOrNull()
                        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                )
            }
        }

    }.onFailure {
        println("YtMusic getArtistItemsContinuation error: ${it.stackTraceToString()}")
    }

    suspend fun getAlbum(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = Innertube.browse(browseId = browseId).body<BrowseResponse>()
        val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!

        AlbumPage(
            album = Innertube.AlbumItem(
                playlistId = playlistId,
                info = Innertube.Info(
                    name = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                    )
                ),
                authors = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.oddElements()
                    ?.map {
                        Innertube.Info(
                            name = it.text,
                            endpoint = it.navigationEndpoint?.browseEndpoint,
                        )
                    }!!,
                year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text,
                thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
            ),
            songs = if (withSongs) getAlbumSongs(playlistId).getOrThrow() else emptyList(),
            otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
                1
            )?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.map(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty(),
            url = response.microformat.microformatDataRenderer.urlCanonical,
            description = response.contents.twoColumnBrowseResultsRenderer.tabs
                .firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicResponsiveHeaderRenderer
                ?.description
                ?.musicDescriptionShelfRenderer
                ?.description?.text,
        )
    }

    suspend fun getAlbumSongs(playlistId: String): Result<List<Innertube.SongItem>> = runCatching {
        val response = Innertube.browse(browseId = "VL$playlistId").body<BrowseResponse>()

        val singleSection = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer
        val twoColumnSection = response.contents?.twoColumnBrowseResultsRenderer
            ?.secondaryContents?.sectionListRenderer
        val section = listOfNotNull(singleSection, twoColumnSection).firstOrNull { candidate ->
            candidate.contents?.any { it.musicPlaylistShelfRenderer != null } == true
        } ?: singleSection ?: twoColumnSection
        val shelf = section?.contents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

        val songs = shelf?.contents.orEmpty().mapNotNull { item ->
            item.musicResponsiveListItemRenderer?.let { AlbumPage.getSong(it) }
        }.toMutableList()
        var continuation = shelf?.continuations?.getContinuation()
            ?: section?.continuations?.getContinuation()
            ?: shelf?.contents?.lastOrNull()?.continuationItemRenderer
                ?.continuationEndpoint?.continuationCommand?.token
        val visited = mutableSetOf<String>()
        var pageCount = 0

        // Album playlists can extend beyond the first shelf. A failed continuation must not
        // discard the tracks already loaded from the initial page.
        while (pageCount < 10) {
            val token = continuation?.takeIf { it.isNotBlank() && it !in visited } ?: break
            visited += token
            pageCount++
            val page = runCatching {
                Innertube.browse(continuation = token).body<BrowseResponse>()
            }.getOrNull() ?: break
            val continuationShelf = page.continuationContents?.musicPlaylistShelfContinuation
            val continuationSection = page.continuationContents?.sectionListContinuation
            val sectionShelf = continuationSection?.contents
                ?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }
            val shelfItems = continuationShelf?.contents ?: sectionShelf?.contents
            val actionItems = page.onResponseReceivedActions.orEmpty()
                .flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }

            if (shelfItems != null) {
                songs += shelfItems.mapNotNull { item ->
                    item.musicResponsiveListItemRenderer?.let { AlbumPage.getSong(it) }
                }
            } else {
                songs += actionItems.mapNotNull { item ->
                    item.musicResponsiveListItemRenderer?.let { AlbumPage.getSong(it) }
                }
            }
            continuation = continuationShelf?.continuations?.getContinuation()
                ?: sectionShelf?.continuations?.getContinuation()
                ?: continuationSection?.continuations?.getContinuation()
                ?: shelfItems?.lastOrNull()?.continuationItemRenderer
                    ?.continuationEndpoint?.continuationCommand?.token
                ?: actionItems.lastOrNull()?.continuationItemRenderer
                    ?.continuationEndpoint?.continuationCommand?.token
        }

        songs.filter { !it.info?.endpoint?.videoId.isNullOrBlank() }.distinctBy { it.key }
    }

}

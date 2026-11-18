package it.fast4x.innertube.requests


import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.BrowseResponse
import it.fast4x.innertube.models.MusicCarouselShelfRenderer
import it.fast4x.innertube.models.NextResponse
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.utils.findSectionByStrapline
import it.fast4x.innertube.utils.findSectionByTitle
import it.fast4x.innertube.utils.from
import it.fast4x.innertube.utils.runCatchingNonCancellable



suspend fun Innertube.relatedPage(body: NextBody) = runCatchingNonCancellable {
    val nextResponse = client.post(next) {
        setBody(body)
        mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)")
    }.body<NextResponse>()

    val relatedTabs = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        .orEmpty()

    val browseId = relatedTabs
        .mapNotNull { it.tabRenderer }
        .firstNotNullOfOrNull { renderer ->
            renderer.endpoint
                ?.browseEndpoint
                ?.browseId
                ?.takeIf { it.startsWith("MPTR") }
        }
        ?: relatedTabs
            .mapNotNull { it.tabRenderer }
            .firstNotNullOfOrNull { renderer ->
                renderer.endpoint
                    ?.browseEndpoint
                    ?.browseId
                    ?.takeIf { renderer.title.orEmpty().contains("related", ignoreCase = true) }
            }
        ?: relatedTabs
            .getOrNull(2)
            ?.tabRenderer
            ?.endpoint
            ?.browseEndpoint
            ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(browse) {
        setBody(BrowseBody(browseId = browseId))
        mask("contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title,strapline),contents($musicResponsiveListItemRendererMask,$musicTwoRowItemRendererMask))")
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    val carouselContents = sectionListRenderer
        ?.contents
        .orEmpty()
        .mapNotNull { content -> content.musicCarouselShelfRenderer?.contents }
        .flatten()
    val responsiveItems = carouselContents
        .mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
    val twoRowItems = carouselContents
        .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)

    val titledSongs = sectionListRenderer
        ?.findSectionByTitle("You might also like")
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull(Innertube.SongItem::from)
        .orEmpty()
    val titledAlbums = sectionListRenderer
        ?.findSectionByStrapline("MORE FROM")
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.AlbumItem::from)
        .orEmpty()
    val titledArtists = sectionListRenderer
        ?.findSectionByTitle("Similar artists")
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.ArtistItem::from)
        .orEmpty()
    val titledPlaylists = sectionListRenderer
        ?.findSectionByTitle("Recommended playlists")
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.PlaylistItem::from)
        .orEmpty()

    val classifiedPlaylists = twoRowItems
        .mapNotNull(Innertube.PlaylistItem::from)
        .filter { playlist ->
            playlist.key.startsWith("VL") ||
                playlist.key.startsWith("PL") ||
                playlist.key.startsWith("RD")
        }
    val classifiedAlbums = twoRowItems
        .mapNotNull(Innertube.AlbumItem::from)
        .filter { album -> album.key.startsWith("MPR") }
    val classifiedArtists = twoRowItems
        .mapNotNull(Innertube.ArtistItem::from)
        .filter { artist -> artist.key.startsWith("UC") }

    Innertube.RelatedPage(
        songs = titledSongs.ifEmpty {
            responsiveItems.mapNotNull(Innertube.SongItem::from)
        }.distinctBy { it.key },
        playlists = (titledPlaylists + classifiedPlaylists)
            .filter { playlist ->
                playlist.key.startsWith("VL") ||
                    playlist.key.startsWith("PL") ||
                    playlist.key.startsWith("RD")
            }
            .distinctBy { it.key }
            .sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = (titledAlbums + classifiedAlbums)
            .filter { album -> album.key.startsWith("MPR") }
            .distinctBy { it.key },
        artists = (titledArtists + classifiedArtists)
            .filter { artist -> artist.key.startsWith("UC") }
            .distinctBy { it.key },
    )
}?.onFailure {
    println("ERROR in Innertube Failed relatedPage ${it.stackTraceToString()}")
}

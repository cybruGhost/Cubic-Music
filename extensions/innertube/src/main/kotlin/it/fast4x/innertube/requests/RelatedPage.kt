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

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(2)
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

    println("mediaItem Innertube RelatedPage sectionListRenderer ${sectionListRenderer
        ?.findSectionByTitle("You might also like")
        ?.musicCarouselShelfRenderer
        ?.contents}")

    Innertube.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.ArtistItem::from),
    )
}?.onFailure {
    println("ERROR in Innertube Failed relatedPage ${it.stackTraceToString()}")
}

package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.Thumbnail
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.models.v0624.podcasts.BrowsePodcastsResponse0624
import it.fast4x.innertube.models.v0624.podcasts.MusicShelfContinuation
import it.fast4x.innertube.models.v0624.podcasts.MusicShelfRendererContent

suspend fun Innertube.podcastPage(body: BrowseBody) = runCatching {
    val response = client.post(browse) {
        setBody(body)
        body.context.apply()
    }.body<BrowsePodcastsResponse0624>()
    //println("mediaItem podcastPage response $response")

    /*
    println("mediaItem podcastPage response ${
        response
            .contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents?.firstOrNull()
            ?.musicShelfRenderer
            ?.contents
    }")
     */

    val listEpisode = arrayListOf<Innertube.Podcast.EpisodeItem>()
    val thumbnail =
        response.background?.musicThumbnailRenderer?.thumbnail?.thumbnails
            ?.map {
                Thumbnail(
                    url = it.url ?: "",
                    width = it.width?.toInt(),
                    height = it.height?.toInt()
                )
            }
    val title =
        response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
    val author =
        response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicResponsiveHeaderRenderer?.let {
                it.straplineTextOne?.runs?.firstOrNull()?.text ?: ""
                /*
                Innertube.ArtistItem(
                    Innertube.Info(
                        name = it.straplineTextOne?.runs?.firstOrNull()?.text ?: "",
                        endpoint = NavigationEndpoint.Endpoint.Browse(
                            browseId = it.straplineTextOne?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseID
                        )
                ),
                    subscribersCountText = null,
                    thumbnail = it.straplineThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url?.let { it1 ->
                        Thumbnail(
                            url = it1,
                            width = it.straplineThumbnail.musicThumbnailRenderer.thumbnail.thumbnails.lastOrNull()?.width?.toInt(),
                            height = it.straplineThumbnail.musicThumbnailRenderer.thumbnail.thumbnails.lastOrNull()?.height?.toInt()
                        )
                    }
                )
                */
            }
    val authorThumbnail =
        response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicResponsiveHeaderRenderer?.let {
                it.straplineThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
                ?.url
            }
    val description =
        response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
            ?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
            ?.description?.musicDescriptionShelfRenderer?.description?.runs?.joinToString("") {
                it.text.toString()
            }
    val data =
        response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicShelfRenderer?.contents
    println("mediaItem podcastPage contents count ${data?.size}")
    parsePodcastData(data, author).let {
        listEpisode.addAll(it)
    }

    var continueParam =
        response.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicShelfRenderer
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation

    println("mediaItem podcastPage first continueParam $continueParam")

    while (continueParam != null) {
        val continueData = browse(
            continuation = continueParam,
            browseId = null,
            setLogin = true
        ).body<BrowsePodcastsResponse0624>()

        parseContinuationPodcastEpisodes(
            continueData.continuationContents?.musicShelfContinuation?.contents,
            author,
        ).let {
            listEpisode.addAll(it)
        }

        continueParam =
            continueData.continuationContents
                ?.musicShelfContinuation
                ?.continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation

        println("mediaItem podcastPage other continueParam $continueParam")
    }

    //println("mediaItem podcastPage listEpisode ${listEpisode.size}")
    Innertube.Podcast(
        title = title ?: "",
        //author = author ?: Innertube.ArtistItem(info = Innertube.Info(name = "", endpoint = null), thumbnail = null, subscribersCountText = null),
        author = author,
        authorThumbnail = authorThumbnail,
        thumbnail = thumbnail ?: emptyList(),
        description = description ?: "",
        listEpisode = listEpisode
    )


}.onFailure {
    println("mediaItem ERROR IN Innertube podcastsPage " + it.message)
}

fun parsePodcastData(
    listContent: List<MusicShelfRendererContent>?,
    //author: Innertube.ArtistItem?
    author: String?
): List<Innertube.Podcast.EpisodeItem> {
    //if (listContent == null) return emptyList()
    //else {
        val listEpisode: ArrayList<Innertube.Podcast.EpisodeItem> = arrayListOf()
        //println("mediaItem parsePodcastData listContent size ${listContent?.size}")
        listContent?.forEach { content ->
            listEpisode.add(
                Innertube.Podcast.EpisodeItem(
                    title = content.musicMultiRowListItemRenderer?.title?.runs?.firstOrNull()?.text
                        ?: "",
                    //author = author ?: Innertube.ArtistItem(info = Innertube.Info(name = "", endpoint = null), thumbnail = null, subscribersCountText = null),
                    author = author,
                    description = content.musicMultiRowListItemRenderer?.description?.runs?.joinToString(
                        separator = ""
                    ) { it.text.toString() } ?: "",
                    thumbnail = content.musicMultiRowListItemRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                        ?.map {
                            Thumbnail(
                                url = it.url ?: "",
                                width = it.width?.toInt(),
                                height = it.height?.toInt()
                            )
                        }
                        ?: emptyList(),
                    createdDay = content.musicMultiRowListItemRenderer?.subtitle?.runs?.firstOrNull()?.text
                        ?: "",
                    durationString = content.musicMultiRowListItemRenderer?.subtitle?.runs?.getOrNull(
                        1
                    )?.text ?: "",
                    //videoId = content.musicMultiRowListItemRenderer?.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseID ?: "",
                    videoId = content.musicMultiRowListItemRenderer?.onTap?.watchEndpoint?.videoID ?: ""
                    //    ?: "",
                    //endpoint = NavigationEndpoint.Endpoint.Browse(
                    //    browseId = content.musicMultiRowListItemRenderer?.onTap?.watchEndpoint?.videoID
                    //        ?: ""
                    //)
                )
            )
        }

        return listEpisode
    //}
}

fun List<Thumbnail>.toListThumbnail(): List<Thumbnail> {
    val list = mutableListOf<Thumbnail>()
    this.forEach {
        list.add(it.toThumbnail())
    }
    return list
}

fun Thumbnail.toThumbnail(): Thumbnail {
    return Thumbnail(
        height = this.height ?: 0,
        url = this.url,
        width = this.width ?: 0
    )
}

fun parseContinuationPodcastEpisodes(
    listContent: List<MusicShelfContinuation.Content>?,
    author: String?,
): List<Innertube.Podcast.EpisodeItem> {
    if (listContent == null || author == null) {
        return emptyList()
    } else {
        val listEpisode: ArrayList<Innertube.Podcast.EpisodeItem> = arrayListOf()
        listContent.forEach { content ->
            listEpisode.add(
                Innertube.Podcast.EpisodeItem(
                    title =
                        content.musicMultiRowListItemRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?: "",
                    author = author,
                    description =
                        content.musicMultiRowListItemRenderer?.description?.runs?.joinToString(
                            separator = "",
                        ) { it.text.toString() } ?: "",
                    thumbnail =
                        content.musicMultiRowListItemRenderer
                            ?.thumbnail
                            ?.musicThumbnailRenderer
                            ?.thumbnail
                            ?.thumbnails
                            ?.map {
                                Thumbnail(
                                    url = it.url ?: "",
                                    width = it.width?.toInt(),
                                    height = it.height?.toInt()
                                )
                            }
                            ?: emptyList<Thumbnail>(),
                    createdDay =
                        content.musicMultiRowListItemRenderer
                            ?.subtitle
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?: "",
                    durationString =
                        content.musicMultiRowListItemRenderer
                            ?.subtitle
                            ?.runs
                            ?.lastOrNull()
                            ?.text
                            ?: "",
                    videoId =
                        content.musicMultiRowListItemRenderer
                            ?.onTap
                            ?.watchEndpoint
                            ?.videoID
                            ?: "",
                ),
            )
        }
        return listEpisode
    }
}
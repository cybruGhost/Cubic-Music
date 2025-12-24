package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.GetQueueResponse
import it.fast4x.innertube.models.bodies.QueueBody
import it.fast4x.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.queue(body: QueueBody) = runCatchingNonCancellable {
    val response = client.post(queue) {
        setBody(body)
        mask("queueDatas.content.$playlistPanelVideoRendererMask")
    }.body<GetQueueResponse>()

    response
        .queueDatas
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.playlistPanelVideoRenderer
                ?.let(Innertube.SongItem::parse)
        }
}

suspend fun Innertube.song(videoId: String): Result<Innertube.SongItem?>? =
    queue(QueueBody(videoIds = listOf(videoId)))?.map { it?.firstOrNull() }

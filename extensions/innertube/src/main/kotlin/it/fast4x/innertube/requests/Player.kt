package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.PlayerBody
import it.fast4x.innertube.utils.runCatchingCancellable

suspend fun Innertube.player(
    videoId: String,
    poToken: String? = null,
    playlistId: String? = null,
) = runCatchingCancellable {
    client.post(player) {
        setLogin(Context.DefaultWeb.client, setLogin = cookie != null)
        setBody(
            PlayerBody(
                context = Context.DefaultWeb,
                videoId = videoId,
                playlistId = playlistId,
                serviceIntegrityDimensions = poToken?.let(PlayerBody::ServiceIntegrityDimensions)
            )
        )
    }.body<PlayerResponse>()
}

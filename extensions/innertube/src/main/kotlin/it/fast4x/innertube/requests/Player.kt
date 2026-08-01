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
    context: Context = Context.DefaultWeb,
    signatureTimestamp: Int? = null,
) = runCatchingCancellable {
    client.post(player) {
        setLogin(context.client, setLogin = cookie != null)
        setBody(
            PlayerBody(
                context = context,
                videoId = videoId,
                playlistId = playlistId,
                playbackContext = if (context.client.useSignatureTimestamp) PlayerBody.PlaybackContext(
                    PlayerBody.PlaybackContext.ContentPlaybackContext(
                        signatureTimestamp = signatureTimestamp ?: 20110
                    )
                ) else null,
                serviceIntegrityDimensions = if (context.client.useWebPoTokens && poToken != null) PlayerBody.ServiceIntegrityDimensions(poToken) else null
            )
        )
    }.body<PlayerResponse>()
}

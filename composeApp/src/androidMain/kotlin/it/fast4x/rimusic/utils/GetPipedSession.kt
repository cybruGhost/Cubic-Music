package it.fast4x.rimusic.utils

import app.kreate.android.Preferences
import io.ktor.http.Url
import it.fast4x.rimusic.models.PipedSession
import timber.log.Timber

/*
@Composable
fun getPipedSession(): PipedSession {
    val pipedUsername by rememberEncryptedPreference(pipedUsernameKey, "")
    val pipedInstanceName by rememberEncryptedPreference(pipedInstanceNameKey, "")
    val pipedApiBaseUrl by rememberEncryptedPreference(pipedApiBaseUrlKey, "")
    val pipedApiToken by rememberEncryptedPreference(pipedApiTokenKey, "")
    return PipedSession(
        instanceName = pipedInstanceName,
        apiBaseUrl = Url(pipedApiBaseUrl),
        token = pipedApiToken,
        username = pipedUsername
    )
}
*/


fun getPipedSession(): PipedSession {

    val pipedSession = PipedSession(
        instanceName = "",
        apiBaseUrl = Url(""),
        token = "",
        username = ""
    )
    if ( Preferences.ENABLE_PIPED.value && isAtLeastAndroid7 ) {
        runCatching {
            pipedSession.username = Preferences.PIPED_USERNAME.value
            pipedSession.instanceName = Preferences.PIPED_PASSWORD.value
            pipedSession.apiBaseUrl = Url(Preferences.PIPED_INSTANCE_NAME.value)
            pipedSession.token = Preferences.PIPED_API_BASE_URL.value
        }.onFailure {
            Timber.e("GetPipedSession get encryptedPreferences error ${it.stackTraceToString()}")
        }
    }

    return pipedSession

}
package app.cubic.android.lastfm

const val lastFmEnabledKey = "lastFmEnabled"
const val lastFmNowPlayingEnabledKey = "lastFmNowPlayingEnabled"
const val lastFmScrobblingEnabledKey = "lastFmScrobblingEnabled"
const val lastFmSessionKey = "lastFmSessionKey"
const val lastFmUsernameKey = "lastFmUsername"
const val lastFmAvatarUrlKey = "lastFmAvatarUrl"
const val lastFmLastScrobbleAtKey = "lastFmLastScrobbleAt"

internal fun isLastFmSetupKey(key: String?): Boolean =
    key == lastFmEnabledKey || key == lastFmSessionKey

internal fun isLastFmRuntimeKey(key: String?): Boolean =
    key == lastFmNowPlayingEnabledKey || key == lastFmScrobblingEnabledKey

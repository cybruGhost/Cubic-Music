package it.fast4x.rimusic.ui.screens.settings

import androidx.compose.runtime.getValue
import app.kreate.android.Preferences
import it.fast4x.innertube.utils.parseCookieString

fun isYouTubeLoginEnabled(): Boolean = Preferences.YOUTUBE_LOGIN.value

fun isYouTubeSyncEnabled(): Boolean {
    val isYouTubeSyncEnabled by Preferences.YOUTUBE_PLAYLISTS_SYNC
    return isYouTubeSyncEnabled && isYouTubeLoggedIn() && isYouTubeLoginEnabled()
}

fun isYouTubeLoggedIn(): Boolean {
    val cookie by Preferences.YOUTUBE_COOKIES
    val isLoggedIn = cookie?.let { parseCookieString(it) }?.contains("SAPISID") == true
    return isLoggedIn
}






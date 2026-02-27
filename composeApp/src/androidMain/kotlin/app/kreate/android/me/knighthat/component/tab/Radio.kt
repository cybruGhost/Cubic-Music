package app.kreate.android.me.knighthat.component.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@UnstableApi
class Radio private constructor(
    private val binder: PlayerServiceModern.Binder?,
    private val menuState: MenuState,
    private val songs: () -> List<Song>
): MenuIcon, Descriptive {

    // Track recently played songs to avoid repetition
    private val recentlyPlayed = mutableStateListOf<Song>()
    private val maxRecentSize = 5 // Keep last 5 played songs to avoid repetition
    private var retryCount = 0
    private val maxRetries = 3 // Maximum number of auto-retry attempts

    companion object {
        @Composable
        operator fun invoke( songs: () -> List<Song> ): Radio =
            Radio(
                LocalPlayerServiceBinder.current,
                LocalMenuState.current,
                songs
            )
    }

    override val iconId: Int = R.drawable.radio
    override val messageId: Int = R.string.info_start_radio
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    @OptIn(DelicateCoroutinesApi::class)
    override fun onShortClick() {
        retryCount = 0 // Reset retry counter when user manually clicks
        startRadioWithRetry()
    }

    /**
     * Main radio start function with auto-retry capability
     */
    private fun startRadioWithRetry() {
        GlobalScope.launch {
            try {
                val availableSongs = songs()
                
                // Error handling for empty song list
                if (availableSongs.isEmpty()) {
                    handleRadioError("No songs available", shouldRetry = false)
                    return@launch
                }

                // Error handling for null binder
                if (binder == null) {
                    handleRadioError("Player service not available", shouldRetry = true)
                    return@launch
                }

                val songToPlay = selectNonRepeatedSong(availableSongs)
                
                // Log the radio start attempt
                logRadioStart(songToPlay)
                
                binder.startRadio(songToPlay)
                
                // Add to recently played to prevent repetition
                updateRecentlyPlayed(songToPlay)
                
                // Reset retry count on success
                retryCount = 0
                
                menuState.hide()
                
            } catch (e: Exception) {
                // Error handling for radio start failure with auto-retry
                handleRadioError("Failed to start radio: ${e.message}", shouldRetry = true, exception = e)
            }
        }
    }

    /**
     * Handles errors and automatically retries if applicable
     */
    private fun handleRadioError(message: String, shouldRetry: Boolean, exception: Exception? = null) {
        showError(message)
        logError("Radio start failed: $message", exception)

        if (shouldRetry && retryCount < maxRetries) {
            retryCount++
            logError("Auto-retrying... Attempt $retryCount of $maxRetries")
            
            // Auto-retry after a short delay (1 second)
            GlobalScope.launch {
                kotlinx.coroutines.delay(1000)
                startRadioWithRetry()
            }
        } else if (retryCount >= maxRetries) {
            logError("Max retry attempts ($maxRetries) reached. Giving up.")
            retryCount = 0 // Reset for next attempt
        }
    }

    /**
     * Selects a song that hasn't been played recently to avoid repetition
     */
    private fun selectNonRepeatedSong(availableSongs: List<Song>): Song {
        // Filter out recently played songs
        val freshSongs = availableSongs.filter { it !in recentlyPlayed }
        
        return if (freshSongs.isNotEmpty()) {
            // If we have fresh songs, choose randomly from them
            freshSongs.random()
        } else {
            // If all songs have been played recently, clear the recent list and choose any
            recentlyPlayed.clear()
            availableSongs.random()
        }
    }

    /**
     * Update the recently played list
     */
    private fun updateRecentlyPlayed(song: Song) {
        recentlyPlayed.add(0, song)
        // Keep only the most recent songs
        if (recentlyPlayed.size > maxRecentSize) {
            recentlyPlayed.removeAt(recentlyPlayed.size - 1)
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        // TODO: Implement your error display mechanism
        // This could be a Toast, Snackbar, or custom error state
        println("Radio Error: $message")
    }

    /**
     * Log radio start for debugging
     */
    private fun logRadioStart(song: Song) {
        println("Radio: Starting radio with song: ${song.title}")
        println("Radio: Recently played: ${recentlyPlayed.size} songs")
    }

    /**
     * Log errors for debugging
     */
    private fun logError(message: String, exception: Exception? = null) {
        println("Radio Error: $message")
        exception?.printStackTrace()
    }

    /**
     * Clear recently played history - useful for manual refresh
     */
    fun clearHistory() {
        recentlyPlayed.clear()
        println("Radio: Cleared recently played history")
    }
}
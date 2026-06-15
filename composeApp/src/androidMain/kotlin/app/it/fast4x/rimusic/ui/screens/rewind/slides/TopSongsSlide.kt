package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong

@Composable
fun TopSongsSlide(songs: List<TopSong>, onNext: () -> Unit) {
    val accent = Color(0xFFE056B5)
    RewindStoryFrame(header = stringResource(R.string.rewind_top_songs_title), accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            songs.take(10).forEachIndexed { index, song ->
                RewindListRow(
                    rank = index + 1,
                    title = song.song.title,
                    subtitle = song.song.artistsText ?: stringResource(R.string.unknown_artist),
                    meta = "${song.minutes} minutes • ${song.playCount} plays",
                    imageUrl = song.song.thumbnailUrl,
                    featured = index == 0,
                    accent = accent
                )
            }
        }
        RewindAction("See all songs", accent, onNext)
    }
}

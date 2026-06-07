package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist

@Composable
fun TopArtistsSlide(artists: List<TopArtist>, onNext: () -> Unit) {
    val accent = Color(0xFF40D9C8)
    RewindStoryFrame(header = "Top Artists", accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            artists.take(10).forEachIndexed { index, artist ->
                RewindListRow(
                    rank = index + 1,
                    title = artist.artist.name ?: stringResource(R.string.unknown_artist),
                    subtitle = "${artist.songCount} songs",
                    meta = "${artist.minutes} minutes",
                    imageUrl = artist.artist.thumbnailUrl,
                    featured = index == 0,
                    accent = accent
                )
            }
        }
        RewindAction("See all artists", accent, onNext)
    }
}

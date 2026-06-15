package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.TopAlbum

@Composable
fun TopAlbumsSlide(albums: List<TopAlbum>, onNext: () -> Unit) {
    val accent = Color(0xFFFF9900)
    RewindStoryFrame(header = "Top Albums", accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            albums.take(10).forEachIndexed { index, album ->
                RewindListRow(
                    rank = index + 1,
                    title = album.album.title ?: stringResource(R.string.unknown_title),
                    subtitle = album.album.authorsText ?: stringResource(R.string.unknown_artist),
                    meta = "${album.minutes} minutes",
                    imageUrl = album.album.thumbnailUrl,
                    featured = index == 0,
                    accent = accent
                )
            }
        }
        RewindAction("See all albums", accent, onNext)
    }
}

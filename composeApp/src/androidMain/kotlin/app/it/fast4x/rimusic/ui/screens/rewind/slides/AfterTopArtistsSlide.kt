package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist

@Composable
fun AfterTopArtistsSlide(topArtist: TopArtist? = null, onNext: () -> Unit) {
    val accent = Color(0xFF40D9C8)
    RewindStoryFrame(header = "Your artist", accent = accent) {
        RewindHeroArt(topArtist?.artist?.thumbnailUrl, topArtist?.artist?.name.orEmpty())
        Spacer(Modifier.height(8.dp))
        Text(topArtist?.artist?.name ?: stringResource(R.string.unknown_artist), color = Color.White, fontSize = 30.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${topArtist?.minutes ?: 0} minutes • ${topArtist?.songCount ?: 0} songs", color = accent, fontSize = 15.sp)
        RewindAction("You kept coming back", accent, onNext)
    }
}

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
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong

@Composable
fun AfterTopSongsSlide(topSong: TopSong? = null, onNext: () -> Unit) {
    val accent = Color(0xFFE056B5)
    RewindStoryFrame(header = stringResource(R.string.rewind_top_song_title), accent = accent) {
        RewindHeroArt(topSong?.song?.thumbnailUrl, topSong?.song?.title.orEmpty())
        Spacer(Modifier.height(8.dp))
        Text(topSong?.song?.title ?: stringResource(R.string.rewind_no_songs_title), color = Color.White, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(topSong?.song?.artistsText ?: stringResource(R.string.unknown_artist), color = accent, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        RewindAction("${topSong?.playCount ?: 0} plays • ${topSong?.minutes ?: 0} minutes", accent, onNext)
    }
}

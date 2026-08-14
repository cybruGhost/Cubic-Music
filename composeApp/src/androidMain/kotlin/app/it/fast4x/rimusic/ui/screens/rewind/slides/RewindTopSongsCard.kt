package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong
import app.kreate.android.R

@Composable
fun RewindTopSongsCard(
    songs: List<TopSong>,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_top_songs_title),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindPurple,
        secondary = RewindBlue,
        backgroundImageUrl = songs.firstOrNull()?.song?.thumbnailUrl,
        onClick = onNext
    ) {
        Text(
            text = stringResource(R.string.rewind_card_songs_headline),
            color = RewindCream,
            fontSize = 28.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.weight(1f))
        if (songs.isEmpty()) {
            RewindEmptyState(
                title = stringResource(R.string.rewind_no_songs_title),
                body = stringResource(R.string.rewind_no_songs_subtitle),
                accent = RewindPurple
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                songs.take(6).forEachIndexed { index, song ->
                    RewindRankRow(
                        rank = index + 1,
                        title = song.song.cleanTitle(),
                        subtitle = firstNonBlank(song.song.cleanArtistsText(), stringResource(R.string.rewind_unknown_artist)),
                        meta = stringResource(R.string.rewind_card_song_plays_meta, song.playCount),
                        imageUrl = song.song.thumbnailUrl,
                        accent = RewindPurple,
                        featured = index == 0
                    )
                }
            }
        }
    }
}

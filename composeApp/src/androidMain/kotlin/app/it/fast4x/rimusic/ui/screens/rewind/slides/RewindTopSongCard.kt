package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong
import app.kreate.android.R

@Composable
fun RewindTopSongCard(
    topSong: TopSong?,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_top_song_title),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindPink,
        secondary = RewindBlue,
        backgroundImageUrl = topSong?.song?.thumbnailUrl,
        onClick = onNext
    ) {
        if (topSong == null) {
            Spacer(Modifier.weight(1f))
            RewindEmptyState(
                title = stringResource(R.string.rewind_no_songs_title),
                body = stringResource(R.string.rewind_no_songs_subtitle),
                accent = RewindPink
            )
            Spacer(Modifier.weight(1f))
            return@RewindCanvasCard
        }

        Spacer(Modifier.height(20.dp))
        RewindArtwork(
            imageUrl = topSong.song.thumbnailUrl,
            contentDescription = topSong.song.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(292.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, RewindCream.copy(alpha = 0.36f), RoundedCornerShape(30.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = topSong.song.cleanTitle(),
                color = RewindCream,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = firstNonBlank(topSong.song.cleanArtistsText(), stringResource(R.string.rewind_unknown_artist)),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RewindMetricPill(
                    value = formatRewindNumber(topSong.playCount.toLong()),
                    label = stringResource(R.string.rewind_plays_label),
                    accent = RewindPink,
                    modifier = Modifier.weight(1f)
                )
                RewindMetricPill(
                    value = formatRewindMinutes(topSong.minutes),
                    label = stringResource(R.string.rewind_minutes_label),
                    accent = RewindBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

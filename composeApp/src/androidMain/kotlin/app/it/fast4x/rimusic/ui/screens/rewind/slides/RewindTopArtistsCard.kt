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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist
import app.kreate.android.R

@Composable
fun RewindTopArtistsCard(
    artists: List<TopArtist>,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_artists_title),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindRed,
        secondary = RewindOrange,
        backgroundImageUrl = artists.firstOrNull()?.artist?.thumbnailUrl,
        onClick = onNext
    ) {
        if (artists.isEmpty()) {
            Spacer(Modifier.weight(1f))
            RewindEmptyState(
                title = stringResource(R.string.rewind_card_artists_empty_title),
                body = stringResource(R.string.rewind_empty_line),
                accent = RewindRed
            )
            Spacer(Modifier.weight(1f))
            return@RewindCanvasCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_artists_headline),
                color = RewindCream,
                fontSize = 28.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = artists.first().artist.cleanName(),
                color = RewindRed,
                fontSize = 28.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            artists.take(5).forEachIndexed { index, artist ->
                RewindRankRow(
                    rank = index + 1,
                    title = artist.artist.cleanName(),
                    subtitle = stringResource(R.string.rewind_card_artist_song_count, artist.songCount),
                    meta = compactMetaMinutes(artist.minutes),
                    imageUrl = artist.artist.thumbnailUrl,
                    accent = RewindRed,
                    circular = true,
                    featured = index == 0
                )
            }
        }
    }
}

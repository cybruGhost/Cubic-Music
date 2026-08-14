package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

@Composable
fun RewindDiscoveryCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    val images = buildList {
        data.topSongs.take(3).forEach { add(it.song.thumbnailUrl) }
        data.topAlbums.take(2).forEach { add(it.album.thumbnailUrl) }
    }

    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_discovery_title),
        year = data.year,
        page = page,
        pageCount = pageCount,
        accent = RewindBlue,
        secondary = RewindGreen,
        onClick = onNext
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_discovery_headline),
                color = RewindCream,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = formatRewindNumber(data.totalUniqueSongs.toLong()),
                color = RewindBlue,
                fontSize = 64.sp,
                lineHeight = 66.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.rewind_unique_songs).lowercase(),
                color = RewindCream.copy(alpha = 0.78f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        RewindOrbitalArt(
            images = images,
            accent = RewindBlue,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RewindMetricPill(
                value = formatRewindNumber(data.totalUniqueArtists.toLong()),
                label = stringResource(R.string.rewind_unique_artists),
                accent = RewindGreen,
                modifier = Modifier.weight(1f)
            )
            RewindMetricPill(
                value = formatRewindNumber(data.totalUniqueAlbums.toLong()),
                label = stringResource(R.string.rewind_unique_albums),
                accent = RewindPurple,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(R.string.rewind_card_discovery_footer),
            color = RewindCream.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

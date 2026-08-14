package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopAlbum
import app.kreate.android.R

@Composable
fun RewindTopAlbumCard(
    topAlbum: TopAlbum?,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_card_album_title),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindPurple,
        secondary = RewindPink,
        backgroundImageUrl = topAlbum?.album?.thumbnailUrl,
        onClick = onNext
    ) {
        if (topAlbum == null) {
            Spacer(Modifier.weight(1f))
            RewindEmptyState(
                title = stringResource(R.string.rewind_card_album_empty_title),
                body = stringResource(R.string.rewind_empty_line),
                accent = RewindPurple
            )
            Spacer(Modifier.weight(1f))
            return@RewindCanvasCard
        }

        Spacer(Modifier.height(20.dp))
        RewindArtwork(
            imageUrl = topAlbum.album.thumbnailUrl,
            contentDescription = topAlbum.album.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(318.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, RewindCream.copy(alpha = 0.34f), RoundedCornerShape(30.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rewind_card_album_headline),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = topAlbum.album.cleanTitle(),
                color = RewindCream,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = topAlbum.album.cleanAuthorsText(),
                color = RewindCream.copy(alpha = 0.72f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RewindMetricPill(
                value = compactMetaMinutes(topAlbum.minutes),
                label = stringResource(R.string.rewind_card_album_minutes),
                accent = RewindPurple,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

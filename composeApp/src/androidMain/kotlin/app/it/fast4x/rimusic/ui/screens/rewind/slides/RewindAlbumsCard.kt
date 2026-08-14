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
import app.it.fast4x.rimusic.ui.screens.rewind.TopAlbum
import app.kreate.android.R

@Composable
fun RewindAlbumsCard(
    albums: List<TopAlbum>,
    year: Int,
    page: Int,
    pageCount: Int,
    onNext: () -> Unit
) {
    RewindCanvasCard(
        title = stringResource(R.string.rewind_slide_top_albums),
        year = year,
        page = page,
        pageCount = pageCount,
        accent = RewindBlue,
        secondary = RewindPurple,
        backgroundImageUrl = albums.firstOrNull()?.album?.thumbnailUrl,
        onClick = onNext
    ) {
        Text(
            text = stringResource(R.string.rewind_card_albums_headline),
            color = RewindCream,
            fontSize = 28.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.weight(1f))
        if (albums.isEmpty()) {
            RewindEmptyState(
                title = stringResource(R.string.rewind_card_album_empty_title),
                body = stringResource(R.string.rewind_empty_line),
                accent = RewindBlue
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                albums.take(5).forEachIndexed { index, album ->
                    RewindRankRow(
                        rank = index + 1,
                        title = album.album.cleanTitle(),
                        subtitle = firstNonBlank(album.album.cleanAuthorsText(), stringResource(R.string.rewind_unknown_artist)),
                        meta = compactMetaMinutes(album.minutes),
                        imageUrl = album.album.thumbnailUrl,
                        accent = RewindBlue,
                        featured = index == 0
                    )
                }
            }
        }
    }
}

package it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.screens.home.MoodGridItemColored
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.semiBold

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun SearchMoodsGrid(
    moods: List<Innertube.Mood.Item>,
    onMoodClick: (Innertube.Mood.Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnailSizeDp = Dimensions.thumbnails.album + 24.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // "Discover something new" title
        BasicText(
            text = stringResource(R.string.discover),
            style = typography().m.semiBold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(thumbnailSizeDp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height((thumbnailSizeDp + 16.dp) * 2) // 2 rows impl
        ) {
            items(
                items = moods.take(6), // Show only first 6 moods
                key = { it.endpoint.params ?: it.title }
            ) { mood ->
                MoodGridItemColored(
                    mood = mood,
                    onClick = { onMoodClick(mood) },
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
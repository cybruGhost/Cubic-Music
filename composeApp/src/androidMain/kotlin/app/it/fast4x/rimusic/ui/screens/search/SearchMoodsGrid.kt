package app.it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun SearchMoodsGrid(
    moods: List<Innertube.Mood.Item>,
    onMoodClick: (Innertube.Mood.Item) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(top = 8.dp, bottom = 8.dp)
        )

        // Moods grid - Fixed 2 columns to show exactly 6 moods (3 rows x 2 cols)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp) // Compact height for 3 rows
        ) {
            items(
                items = moods.take(6), // Show only first 6 moods
                key = { it.endpoint.params ?: it.title }
            ) { mood ->
                SearchMoodCard(
                    mood = mood,
                    onClick = { onMoodClick(mood) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SearchMoodCard(
    mood: Innertube.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val moodColor by remember { derivedStateOf { Color(mood.stripeColor) } }

    // Get drawable resource for this mood
    val imageResource = remember(mood.title) {
        MoodImages.getImageResource(mood.title)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(thumbnailRoundness.shape)
            .background(moodColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
                .graphicsLayer {
                    rotationZ = -15f // Tilt the image slightly
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                }
        )

        BasicText(
            text = mood.title,
            style = TextStyle(
                color = Color.White,
                fontStyle = typography().s.semiBold.fontStyle,
                fontWeight = typography().s.semiBold.fontWeight,
                fontSize = typography().s.fontSize
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        )
    }
}
package it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import coil.compose.AsyncImage
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.thumbnailRoundnessKey

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

    // Generate image URL based on mood title
    val imageUrl = remember(mood.title) {
        getMoodImageUrl(mood.title)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f) // Rectangular card like Spotify
            .clip(thumbnailRoundness.shape)
            .background(moodColor)
            .clickable { onClick() }
    ) {
        // Background image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomEnd)
                .padding(start = 80.dp) // Image on right side
        )

        // Mood title with background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(moodColor.copy(alpha = 0.7f))
        ) {
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
                    .align(Alignment.CenterStart)
                    .padding(12.dp)
            )
        }
    }
}

private fun getMoodImageUrl(moodTitle: String): String {
    val keyword = when {
        moodTitle.contains("Rock", ignoreCase = true) -> "rock-music-concert"
        moodTitle.contains("Pop", ignoreCase = true) -> "pop-music-concert"
        moodTitle.contains("Hip Hop", ignoreCase = true) || moodTitle.contains("Rap", ignoreCase = true) -> "hip-hop-concert"
        moodTitle.contains("Jazz", ignoreCase = true) -> "jazz-saxophone"
        moodTitle.contains("Classical", ignoreCase = true) -> "orchestra-classical"
        moodTitle.contains("Electronic", ignoreCase = true) || moodTitle.contains("EDM", ignoreCase = true) -> "edm-dj-concert"
        moodTitle.contains("Country", ignoreCase = true) -> "country-guitar"
        moodTitle.contains("R&B", ignoreCase = true) || moodTitle.contains("Soul", ignoreCase = true) -> "rnb-singer"
        moodTitle.contains("Metal", ignoreCase = true) -> "metal-concert"
        moodTitle.contains("Reggae", ignoreCase = true) -> "reggae-jamaica"
        moodTitle.contains("Blues", ignoreCase = true) -> "blues-guitar"
        moodTitle.contains("Folk", ignoreCase = true) -> "folk-acoustic-guitar"
        moodTitle.contains("Latin", ignoreCase = true) -> "latin-dance"
        moodTitle.contains("Indie", ignoreCase = true) -> "indie-band"
        moodTitle.contains("Workout", ignoreCase = true) || moodTitle.contains("Gym", ignoreCase = true) -> "workout-fitness"
        moodTitle.contains("Chill", ignoreCase = true) || moodTitle.contains("Relax", ignoreCase = true) -> "chill-relax"
        moodTitle.contains("Party", ignoreCase = true) -> "party-celebration"
        moodTitle.contains("Sleep", ignoreCase = true) -> "sleep-peaceful"
        moodTitle.contains("Focus", ignoreCase = true) || moodTitle.contains("Study", ignoreCase = true) -> "focus-study"
        moodTitle.contains("Happy", ignoreCase = true) || moodTitle.contains("Feel Good", ignoreCase = true) -> "happy-smile"
        moodTitle.contains("Sad", ignoreCase = true) -> "sad-rain"
        moodTitle.contains("Romance", ignoreCase = true) || moodTitle.contains("Love", ignoreCase = true) -> "romance-love"
        moodTitle.contains("Travel", ignoreCase = true) -> "travel-journey"
        moodTitle.contains("K-Pop", ignoreCase = true) -> "kpop-concert"
        moodTitle.contains("Afrobeats", ignoreCase = true) -> "afrobeats-dance"
        else -> "music-concert"
    }

    return "https://source.unsplash.com/400x300/?$keyword"
}
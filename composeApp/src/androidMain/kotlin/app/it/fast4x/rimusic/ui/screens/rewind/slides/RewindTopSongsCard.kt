package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong

@Composable
fun RewindTopSongsCard(
    songs: List<TopSong>,
    year: Int,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val topFive = songs.take(5)

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindLime,
        progressColor = RewindInk,
        onNext = onNext,
        backgroundArt = {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = RewindPink,
                    radius = size.width * 0.15f,
                    center = Offset(size.width * 0.96f, size.height * 0.10f)
                )
            }
        }
    ) {
        if (topFive.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                RewindEmptyState(
                    title = "NO TRACK CHART YET",
                    body = "Your top five will appear here once there is enough listening history.",
                    foreground = RewindInk,
                    accent = RewindPink
                )
            }
            return@RewindStoryShell
        }

        Column(modifier = Modifier.fillMaxSize()) {
            RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                RewindKicker("YOUR TOP FIVE • $year", RewindInk, RewindCream)
            }

            Spacer(Modifier.height(11.dp))

            RewindReveal(active, 110, direction = RewindRevealDirection.Left) {
                Text(
                    text = "YOUR TOP 5\nSONGS.",
                    color = RewindInk,
                    fontSize = 35.sp,
                    lineHeight = 33.sp,
                    letterSpacing = (-1.9).sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                topFive.forEachIndexed { index, song ->
                    RewindRankRow(
                        rank = index + 1,
                        title = song.song.cleanTitle(),
                        subtitle = firstNonBlank(song.song.cleanArtistsText(), "Unknown artist"),
                        meta = "${formatRewindNumber(song.playCount.toLong())} plays",
                        imageUrl = song.song.thumbnailUrl,
                        foreground = RewindInk,
                        accent = if (index == 0) RewindPink else RewindInk,
                        active = active,
                        delayMillis = 240 + index * 120
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            RewindReveal(active, 980) {
                Text(
                    text = "Next: songs 6–10.",
                    color = RewindInk.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

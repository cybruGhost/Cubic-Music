package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong

@Composable
fun RewindDeepCutsCard(
    songs: List<TopSong>,
    year: Int,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val deepCuts = songs.drop(5).take(5)

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindCream,
        progressColor = RewindInk,
        onNext = onNext
    ) {
        if (deepCuts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                RewindEmptyState(
                    title = "YOUR TOP TEN IS STILL FORMING",
                    body = "Songs ranked 6–10 will appear here once there is enough listening history.",
                    foreground = RewindInk,
                    accent = RewindPurple
                )
            }
            return@RewindStoryShell
        }

        Column(modifier = Modifier.fillMaxSize()) {
            RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                RewindKicker("DEEP CUTS • $year", RewindPurple, RewindCream)
            }

            Spacer(Modifier.height(11.dp))

            RewindReveal(active, 110, direction = RewindRevealDirection.Left) {
                Text(
                    text = "SONGS 6–10.",
                    color = RewindInk,
                    fontSize = 34.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-1.8).sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                deepCuts.forEachIndexed { index, song ->
                    val rank = index + 6
                    RewindReveal(
                        active = active,
                        delayMillis = 260 + index * 105,
                        direction = if (index % 2 == 0) RewindRevealDirection.Left else RewindRevealDirection.Right,
                        scaleFrom = 0.92f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) RewindInk else RewindPurple,
                                    RoundedCornerShape(11.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = rank.toString().padStart(2, '0'),
                                color = RewindLime,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.graphicsLayer { rotationZ = if (index % 2 == 0) -2f else 2f }
                            )
                            RewindArtworkWithFallback(
                                imageUrl = song.song.thumbnailUrl,
                                title = song.song.cleanTitle(),
                                modifier = Modifier.size(46.dp),
                                background = RewindPink,
                                foreground = RewindInk
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.song.cleanTitle(),
                                    color = RewindCream,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = firstNonBlank(song.song.cleanArtistsText(), "Unknown artist"),
                                    color = RewindCream.copy(alpha = 0.58f),
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${formatRewindNumber(song.playCount.toLong())}×",
                                color = RewindCream.copy(alpha = 0.76f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            RewindReveal(active, 900) {
                Text(
                    text = "Your next five most-played songs.",
                    color = RewindInk.copy(alpha = 0.60f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

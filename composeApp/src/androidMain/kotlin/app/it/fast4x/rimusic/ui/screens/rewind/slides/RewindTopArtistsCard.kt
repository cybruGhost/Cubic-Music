package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist

@Composable
fun RewindTopArtistsCard(
    artists: List<TopArtist>,
    year: Int,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val topFive = artists.take(5)
    val top = topFive.firstOrNull()

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindInk,
        progressColor = RewindCream,
        onNext = onNext
    ) {
        if (top == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                RewindEmptyState(
                    title = "NO ARTIST CHART YET",
                    body = "Your top five artists will appear here once there is enough listening history.",
                    foreground = RewindCream,
                    accent = RewindLime
                )
            }
            return@RewindStoryShell
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val heroWidth = if (compact) 112.dp else 132.dp
            val heroHeight = if (compact) 142.dp else 166.dp
            val rowArt = if (compact) 38.dp else 44.dp

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("TOP ARTISTS • $year", RewindLime)
                }

                Spacer(Modifier.height(10.dp))

                RewindReveal(active, 120, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOUR TOP 5\nARTISTS.",
                        color = RewindCream,
                        fontSize = if (compact) 34.sp else 40.sp,
                        lineHeight = if (compact) 32.sp else 37.sp,
                        letterSpacing = (-1.9).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.height(15.dp))

                RewindReveal(active, 360, direction = RewindRevealDirection.Left, distance = 34.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewindCream.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RewindArtistArtwork(
                            artistName = top.artist.cleanName(),
                            primaryUrl = top.artist.thumbnailUrl,
                            preferWikipedia = true,
                            circular = false,
                            modifier = Modifier
                                .width(heroWidth)
                                .height(heroHeight)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "01",
                                color = RewindLime,
                                fontSize = 38.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2.0).sp
                            )
                            Text(
                                text = top.artist.cleanName(),
                                color = RewindCream,
                                fontSize = if (compact) 18.sp else 21.sp,
                                lineHeight = if (compact) 19.sp else 22.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                text = compactMetaMinutes(top.minutes),
                                color = RewindCream.copy(alpha = 0.50f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (compact) 8.dp else 10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                ) {
                    topFive.drop(1).forEachIndexed { index, artist ->
                        val rank = index + 2
                        RewindReveal(
                            active = active,
                            delayMillis = 620 + index * 120,
                            direction = RewindRevealDirection.Right,
                            distance = 24.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = rank.toString().padStart(2, '0'),
                                    color = RewindLime,
                                    fontSize = 18.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(29.dp)
                                )
                                RewindArtistArtwork(
                                    artistName = artist.artist.cleanName(),
                                    primaryUrl = artist.artist.thumbnailUrl,
                                    preferWikipedia = artist.artist.thumbnailUrl.isNullOrBlank(),
                                    circular = false,
                                    modifier = Modifier.size(rowArt)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.artist.cleanName(),
                                        color = RewindCream,
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = compactMetaMinutes(artist.minutes),
                                        color = RewindCream.copy(alpha = 0.42f),
                                        fontSize = 8.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

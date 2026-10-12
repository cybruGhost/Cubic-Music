package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopSong
import kotlinx.coroutines.delay

@Composable
fun RewindTopSongCard(
    songs: List<TopSong>,
    year: Int,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val contenders = remember(songs) { songs.take(10).shuffled() }
    val winner = songs.firstOrNull()
    var phase by remember { mutableStateOf(0) }
    var teaserIndex by remember { mutableStateOf(0) }

    LaunchedEffect(active, contenders) {
        phase = 0
        teaserIndex = 0
        if (!active || contenders.isEmpty()) return@LaunchedEffect

        repeat(contenders.size.coerceAtLeast(4)) { index ->
            teaserIndex = index % contenders.size
            delay(if (index < 3) 220L else 170L)
        }
        phase = 1
        delay(720L)
        phase = 2
    }

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindPink,
        progressColor = RewindInk,
        onNext = if (phase >= 2 || winner == null) onNext else null,
        backgroundArt = {
            // The final artwork lives in a frame, so the page itself stays flat and editorial.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 150.dp, end = 12.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(7.dp)
                        .background(RewindLime)
                )
            }
        }
    ) {
        if (winner == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                RewindEmptyState(
                    title = "NO #1 YET",
                    body = "Your #1 song will appear here once there is enough listening history.",
                    foreground = RewindInk,
                    accent = RewindLime
                )
            }
            return@RewindStoryShell
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val frameWidth = if (compact) 218.dp else 252.dp

            Column(modifier = Modifier.fillMaxSize()) outerColumn@{
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("YOUR #1 SONG • $year", RewindLime)
                }

                Spacer(Modifier.height(12.dp))

                if (phase < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (phase == 0) "TEN SONGS.\nONE WINNER." else "AND THEN\nTHERE WAS ONE.",
                                color = RewindInk,
                                fontSize = if (compact) 35.sp else 42.sp,
                                lineHeight = if (compact) 33.sp else 39.sp,
                                letterSpacing = (-2.0).sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(26.dp))

                            if (phase == 0 && contenders.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (compact) 128.dp else 150.dp)
                                        .background(RewindInk, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Crossfade(
                                        targetState = teaserIndex,
                                        animationSpec = tween(140),
                                        label = "top_song_teaser"
                                    ) { index ->
                                        val contender = contenders[index.coerceIn(0, contenders.lastIndex)]
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = contender.song.cleanTitle(),
                                                color = RewindCream,
                                                fontSize = if (compact) 24.sp else 29.sp,
                                                lineHeight = if (compact) 24.sp else 29.sp,
                                                fontWeight = FontWeight.Black,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(7.dp))
                                            Text(
                                                text = firstNonBlank(contender.song.cleanArtistsText(), "Unknown artist"),
                                                color = RewindLime,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "#1",
                                    color = RewindLime,
                                    fontSize = if (compact) 92.sp else 116.sp,
                                    lineHeight = if (compact) 84.sp else 104.sp,
                                    letterSpacing = (-7).sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .background(RewindInk, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 30.dp, vertical = 18.dp)
                                )
                            }
                        }
                    }
                    return@outerColumn
                }

                RewindReveal(active, 0, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOUR MOST-PLAYED\nSONG.",
                        color = RewindInk,
                        fontSize = if (compact) 34.sp else 40.sp,
                        lineHeight = if (compact) 32.sp else 37.sp,
                        letterSpacing = (-1.9).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RewindReveal(active, 170, scaleFrom = 0.78f, durationMillis = 720) {
                        TopSongWallFrame(
                            song = winner,
                            year = year,
                            frameWidth = frameWidth,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = -2.4f
                                rotationY = 5.5f
                                cameraDistance = 18f * density
                            }
                        )
                    }

                    RewindReveal(
                        active = active,
                        delayMillis = 560,
                        scaleFrom = 0.50f,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "#1",
                            color = RewindInk,
                            fontSize = 34.sp,
                            lineHeight = 34.sp,
                            letterSpacing = (-2.0).sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(RewindLime, RoundedCornerShape(4.dp))
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .graphicsLayer { rotationZ = 5f }
                        )
                    }
                }

                RewindReveal(active, 670) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = winner.song.cleanTitle(),
                            color = RewindInk,
                            fontSize = if (compact) 24.sp else 28.sp,
                            lineHeight = if (compact) 24.sp else 28.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = firstNonBlank(winner.song.cleanArtistsText(), "Unknown artist"),
                            color = RewindInk.copy(alpha = 0.66f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(11.dp))

                RewindReveal(active, 790) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        TopSongStat("PLAYS", formatRewindNumber(winner.playCount.toLong()), Modifier.weight(1f))
                        TopSongStat("MINUTES", formatRewindMinutes(winner.minutes), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSongWallFrame(
    song: TopSong,
    year: Int,
    frameWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(frameWidth)
            .shadow(20.dp, RoundedCornerShape(3.dp))
            .background(RewindInk, RoundedCornerShape(3.dp))
            .padding(9.dp)
            .background(RewindCream, RoundedCornerShape(1.dp))
            .padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RewindArtworkWithFallback(
            imageUrl = song.song.thumbnailUrl,
            title = song.song.cleanTitle(),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(1.dp)),
            background = RewindPurple,
            foreground = RewindCream
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RewindInk, RoundedCornerShape(1.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MOST PLAYED",
                        color = RewindLime,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = year.toString(),
                        color = RewindCream,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "01",
                    color = RewindCream,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun TopSongStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(RewindInk, RoundedCornerShape(9.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            color = RewindCream,
            fontSize = 21.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = RewindLime,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.TopArtist

@Composable
fun RewindTopArtistSpotlightCard(
    artist: TopArtist?,
    year: Int,
    page: Int,
    pageCount: Int,
    active: Boolean,
    onNext: () -> Unit
) {
    val artistName = artist?.artist?.cleanName().orEmpty()
    val wiki = rememberArtistWikiMetadata(artistName, artist?.artist?.id)

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindInk,
        progressColor = RewindCream,
        onNext = onNext,
        backgroundArt = {
            Canvas(Modifier.fillMaxSize()) {
                val pinkSlash = Path().apply {
                    moveTo(size.width * 0.77f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.20f)
                    lineTo(size.width * 0.93f, size.height * 0.16f)
                    close()
                }
                drawPath(pinkSlash, RewindPink)

                drawRect(
                    color = RewindLime,
                    topLeft = Offset(0f, size.height * 0.79f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.05f, size.height * 0.13f)
                )
            }
        }
    ) {
        if (artist == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                RewindEmptyState(
                    title = "NO TOP ARTIST YET",
                    body = "Your #1 artist will appear here once there is enough listening history.",
                    foreground = RewindCream,
                    accent = RewindLime
                )
            }
            return@RewindStoryShell
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val heroHeight = if (compact) 238.dp else 292.dp
            val nameSize = when {
                artistName.length > 20 -> if (compact) 31.sp else 36.sp
                artistName.length > 13 -> if (compact) 37.sp else 43.sp
                else -> if (compact) 43.sp else 50.sp
            }

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("YOUR #1 ARTIST • $year", RewindLime)
                }

                Spacer(Modifier.height(12.dp))

                RewindReveal(active, 180, scaleFrom = 0.90f, durationMillis = 760) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                            .background(RewindPurple)
                    ) {
                        RewindArtistArtwork(
                            artistName = artistName,
                            primaryUrl = artist.artist.thumbnailUrl,
                            preferWikipedia = false,
                            circular = false,
                            enableSlideshow = true,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Fade the portrait naturally into the black page instead of cropping it
                        // into a circle. This also makes portrait/landscape source images behave.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0.00f to RewindInk,
                                        0.12f to RewindInk.copy(alpha = 0.70f),
                                        0.28f to Color.Transparent,
                                        0.74f to Color.Transparent,
                                        0.91f to RewindInk.copy(alpha = 0.72f),
                                        1.00f to RewindInk
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.00f to RewindInk.copy(alpha = 0.15f),
                                        0.60f to Color.Transparent,
                                        1.00f to RewindInk
                                    )
                                )
                        )

                        RewindReveal(
                            active = active,
                            delayMillis = 620,
                            scaleFrom = 0.55f,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 13.dp, end = 12.dp)
                        ) {
                            Text(
                                text = "#1",
                                color = RewindInk,
                                fontSize = 30.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .background(RewindLime, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                RewindReveal(active, 520, direction = RewindRevealDirection.Left, distance = 30.dp) {
                    Text(
                        text = artistName.uppercase(),
                        color = RewindCream,
                        fontSize = nameSize,
                        lineHeight = (nameSize.value * 0.90f).sp,
                        letterSpacing = (-2.3).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))

                RewindReveal(active, 760) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpotlightMetric(
                            label = "MINUTES",
                            value = formatRewindNumber(artist.minutes),
                            modifier = Modifier.weight(1f)
                        )
                        SpotlightMetric(
                            label = "SONGS",
                            value = formatRewindNumber(artist.songCount.toLong()),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                val bio = wiki?.bio ?: wiki?.description
                if (!bio.isNullOrBlank()) {
                    RewindReveal(active, 960) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "ABOUT",
                                color = RewindLime,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.0.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = bio,
                                color = RewindCream.copy(alpha = 0.78f),
                                fontSize = if (compact) 10.sp else 11.sp,
                                lineHeight = if (compact) 14.sp else 15.sp,
                                maxLines = if (compact) 5 else 7,
                                overflow = TextOverflow.Clip
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = if (wiki?.wikipediaUrl != null) "WIKIPEDIA" else "INNER TUBE",
                                color = RewindCream.copy(alpha = 0.35f),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(RewindCream, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            color = RewindInk,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = RewindInk.copy(alpha = 0.58f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

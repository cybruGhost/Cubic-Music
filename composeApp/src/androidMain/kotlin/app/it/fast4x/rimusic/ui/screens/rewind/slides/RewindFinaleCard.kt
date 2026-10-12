package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData

@Composable
fun RewindFinaleCard(
    data: RewindData,
    username: String,
    page: Int,
    pageCount: Int,
    active: Boolean,
    shareMode: Boolean,
    onShare: () -> Unit,
    onRestart: () -> Unit
) {
    val topArtist = data.topArtists.firstOrNull()
    val topSong = data.topSongs.firstOrNull()
    val badge = calculateListenerBadge(data)

    RewindStoryShell(
        page = page,
        pageCount = pageCount,
        background = RewindInk,
        progressColor = RewindCream,
        onNext = null,
        showProgress = !shareMode,
        showBrand = true,
        backgroundArt = {
            Canvas(Modifier.fillMaxSize()) {
                val purpleShape = Path().apply {
                    moveTo(size.width * 0.62f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.38f)
                    lineTo(size.width * 0.82f, size.height * 0.30f)
                    close()
                }
                drawPath(purpleShape, RewindPurple)

                val pinkShape = Path().apply {
                    moveTo(0f, size.height * 0.70f)
                    lineTo(size.width * 0.34f, size.height * 0.76f)
                    lineTo(size.width * 0.52f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(pinkShape, RewindPink)

                drawCircle(
                    color = RewindLime,
                    radius = size.width * 0.12f,
                    center = Offset(size.width * 0.90f, size.height * 0.60f)
                )
                drawCircle(
                    color = RewindOrange,
                    radius = size.width * 0.055f,
                    center = Offset(size.width * 0.12f, size.height * 0.18f)
                )
            }
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            val statSize = if (compact) 19.sp else 23.sp

            Column(modifier = Modifier.fillMaxSize()) {
                RewindReveal(active, 40, direction = RewindRevealDirection.Left) {
                    RewindKicker("CUBIC MUSIC • REWIND ${data.year}", RewindLime)
                }

                Spacer(Modifier.height(10.dp))

                RewindReveal(active, 110, direction = RewindRevealDirection.Left) {
                    Text(
                        text = "YOUR ${data.year}\nREWIND.",
                        color = RewindCream,
                        fontSize = if (compact) 40.sp else 48.sp,
                        lineHeight = if (compact) 37.sp else 44.sp,
                        letterSpacing = (-2.4).sp,
                        fontWeight = FontWeight.Black
                    )
                }

                RewindReveal(active, 190) {
                    Text(
                        text = "$username • ${badge.title}",
                        color = RewindLime,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinaleStatTile(
                            label = "MINUTES",
                            value = formatRewindMinutes(data.stats.totalMinutes),
                            background = RewindLime,
                            foreground = RewindInk,
                            valueSize = statSize,
                            active = active,
                            delayMillis = 260,
                            modifier = Modifier.weight(1f)
                        )
                        FinaleStatTile(
                            label = "PLAYS",
                            value = formatRewindNumber(data.stats.totalPlays.toLong()),
                            background = RewindPink,
                            foreground = RewindInk,
                            valueSize = statSize,
                            active = active,
                            delayMillis = 340,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinaleStatTile(
                            label = "DAYS",
                            value = formatRewindNumber(data.daysWithMusic.toLong()),
                            background = RewindBlue,
                            foreground = RewindCream,
                            valueSize = statSize,
                            active = active,
                            delayMillis = 420,
                            modifier = Modifier.weight(1f)
                        )
                        FinaleStatTile(
                            label = "UNIQUE SONGS",
                            value = formatRewindNumber(data.totalUniqueSongs.toLong()),
                            background = RewindOrange,
                            foreground = RewindInk,
                            valueSize = statSize,
                            active = active,
                            delayMillis = 500,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 9.dp else 12.dp))

                RewindReveal(active, 590, scaleFrom = 0.94f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewindCream, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LISTENER LEVEL",
                                color = RewindInk.copy(alpha = 0.54f),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = badge.title,
                                color = RewindInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = badge.index.toString(),
                            color = RewindInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(RewindLime, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 8.dp else 11.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RewindReveal(
                        active = active,
                        delayMillis = 690,
                        direction = RewindRevealDirection.Left,
                        modifier = Modifier.weight(1f)
                    ) {
                        FinaleFeature(
                            label = "TOP ARTIST",
                            title = topArtist?.artist?.cleanName() ?: "—",
                            subtitle = topArtist?.let { compactMetaMinutes(it.minutes) } ?: "",
                            imageUrl = topArtist?.artist?.thumbnailUrl,
                            circular = true,
                            artistName = topArtist?.artist?.cleanName(),
                            background = RewindPurple
                        )
                    }

                    RewindReveal(
                        active = active,
                        delayMillis = 770,
                        direction = RewindRevealDirection.Right,
                        modifier = Modifier.weight(1f)
                    ) {
                        FinaleFeature(
                            label = "TOP SONG",
                            title = topSong?.song?.cleanTitle() ?: "—",
                            subtitle = topSong?.let { "${formatRewindNumber(it.playCount.toLong())} plays" } ?: "",
                            imageUrl = topSong?.song?.thumbnailUrl,
                            circular = false,
                            artistName = null,
                            background = RewindRed
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (shareMode) {
                    RewindReveal(active, 860) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CUBIC MUSIC REWIND ${data.year}",
                                color = RewindLime,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.0.sp
                            )
                            Text(
                                text = "${badge.title} • ${badge.index}",
                                color = RewindCream.copy(alpha = 0.52f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RewindReveal(active, 880) {
                            Text(
                                text = "SHARE REWIND  ↗",
                                color = RewindInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.7.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RewindLime, RoundedCornerShape(100.dp))
                                    .clickable(onClick = onShare)
                                    .padding(vertical = 12.dp)
                            )
                        }
                        RewindReveal(active, 960) {
                            Text(
                                text = "PLAY AGAIN  ↻",
                                color = RewindCream,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onRestart)
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinaleStatTile(
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    valueSize: TextUnit,
    active: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    RewindReveal(active, delayMillis, scaleFrom = 0.86f, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Text(
                text = value,
                color = foreground,
                fontSize = valueSize,
                lineHeight = valueSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = foreground.copy(alpha = 0.66f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
private fun FinaleFeature(
    label: String,
    title: String,
    subtitle: String,
    imageUrl: String?,
    circular: Boolean,
    artistName: String?,
    background: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (circular && !artistName.isNullOrBlank()) {
            RewindArtistArtwork(
                artistName = artistName,
                primaryUrl = imageUrl,
                preferWikipedia = true,
                modifier = Modifier.size(44.dp)
            )
        } else {
            RewindArtworkWithFallback(
                imageUrl = imageUrl,
                title = title,
                modifier = Modifier.size(44.dp),
                circular = circular,
                background = RewindInk,
                foreground = RewindCream
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = RewindLime,
                fontSize = 6.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
            Text(
                text = title,
                color = RewindCream,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = RewindCream.copy(alpha = 0.56f),
                fontSize = 7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R

private val PassportInk = Color(0xFF101015)
private val PassportLine = Color(0xFF72718A)
private val PassportTag = Color(0xFFD9FF33)
private val PassportPink = Color(0xFFFF3F63)
private val PassportWhite = Color(0xFFF8F4FF)

@Composable
fun RewindFinaleCard(
    data: RewindData,
    page: Int,
    pageCount: Int,
    onShare: () -> Unit
) {
    val topArtists = data.topArtists.take(5)
    val topSongs = data.topSongs.take(5)
    val topArtist = topArtists.firstOrNull()
    val topSong = topSongs.firstOrNull()
    val topAlbum = data.topAlbums.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF80AEFF),
                        Color(0xFFE5C4FF),
                        Color(0xFFFF6F96)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FinaleHeader(year = data.year)
            Spacer(Modifier.height(30.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top
            ) {
                FinaleTopBlock(
                    title = stringResource(R.string.rewind_passport_top_artists),
                    imageUrl = topArtist?.artist?.thumbnailUrl,
                    circular = true,
                    names = topArtists.map { it.artist.cleanName() },
                    modifier = Modifier.weight(1f)
                )
                FinaleTopBlock(
                    title = stringResource(R.string.rewind_passport_top_songs),
                    imageUrl = topSong?.song?.thumbnailUrl,
                    circular = false,
                    names = topSongs.map { it.song.cleanTitle() },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(34.dp))
            FinaleDividerTitle(text = stringResource(R.string.rewind_passport_title))
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinalePassportStat(
                    label = stringResource(R.string.rewind_passport_top_artists),
                    imageUrl = topArtist?.artist?.thumbnailUrl,
                    count = data.totalUniqueArtists,
                    modifier = Modifier.weight(1f)
                )
                FinalePassportStat(
                    label = stringResource(R.string.rewind_passport_top_songs),
                    imageUrl = topSong?.song?.thumbnailUrl,
                    count = data.totalUniqueSongs,
                    modifier = Modifier.weight(1f)
                )
                FinalePassportStat(
                    label = stringResource(R.string.albums),
                    imageUrl = topAlbum?.album?.thumbnailUrl,
                    count = data.totalUniqueAlbums,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(42.dp))
            Text(
                text = stringResource(R.string.rewind_passport_listening_time),
                color = PassportInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
            Text(
                text = formatRewindMinutes(data.stats.totalMinutes),
                color = PassportInk,
                fontSize = 68.sp,
                lineHeight = 70.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.rewind_passport_minutes),
                color = PassportInk,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(30.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.rewindlogo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.thumbnail_share_app_name),
                    color = PassportInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                FinaleReplayButton(onClick = onShare)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp)
                .fillMaxWidth(0.34f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (index == page) 3.dp else 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (index == page) PassportPink else PassportInk.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
private fun FinaleHeader(year: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(PassportLine.copy(alpha = 0.7f))
        )
        Text(
            text = stringResource(R.string.rewind_passport_recap_title, year),
            color = PassportInk,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(PassportLine.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun FinaleTopBlock(
    title: String,
    imageUrl: String?,
    circular: Boolean,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            RewindArtwork(
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(if (circular) CircleShape else RoundedCornerShape(28.dp))
                    .border(2.dp, PassportWhite.copy(alpha = 0.9f), if (circular) CircleShape else RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                color = PassportInk,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PassportTag)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        FinaleRankList(names = names)
    }
}

@Composable
private fun FinaleRankList(names: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        names.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}",
                    color = PassportInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = cleanPrefix(name).ifBlank { "-" },
                    color = PassportInk,
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinaleDividerTitle(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            color = PassportInk,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(2.dp)
                    .background(PassportLine.copy(alpha = 0.65f))
            )
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(2.dp)
                    .background(PassportLine.copy(alpha = 0.65f))
            )
        }
    }
}

@Composable
private fun FinalePassportStat(
    label: String,
    imageUrl: String?,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = PassportInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PassportWhite.copy(alpha = 0.86f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.18f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, PassportWhite.copy(alpha = 0.86f), RoundedCornerShape(16.dp))
        ) {
            RewindArtwork(
                imageUrl = imageUrl,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = count.coerceAtLeast(0).toString(),
            color = PassportInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun FinaleReplayButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(PassportPink)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.share_social),
            contentDescription = null,
            tint = PassportWhite,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.rewind_card_finale_share),
            color = PassportWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.rimusic.ui.screens.rewind.*
import it.fast4x.rimusic.ui.screens.rewind.components.dayNames
import it.fast4x.rimusic.ui.screens.rewind.components.getMonthName
import it.fast4x.rimusic.ui.screens.rewind.components.hourDescriptions

@Composable
fun BestOfAllSlide(data: RewindData, onNext: () -> Unit) {
    val topSong = data.topSongs.firstOrNull()
    val topArtist = data.topArtists.firstOrNull()
    val topAlbum = data.topAlbums.firstOrNull()
    val topMonth = data.monthlyStats.maxByOrNull { it.minutes }
    val topDay = data.dailyStats.maxByOrNull { it.minutes }
    val topHour = data.hourlyStats.maxByOrNull { it.minutes }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏆 BEST OF EVERYTHING",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        Text(
            text = "Your Year in Highlights",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 30.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Song
            BestOfItem(
                icon = "🎵",
                title = "TOP SONG",
                value = topSong?.song?.title ?: "N/A",
                detail = topSong?.song?.artistsText ?: "",
                extra = "${topSong?.playCount ?: 0} plays"
            )
            
            // Artist
            BestOfItem(
                icon = "⭐",
                title = "TOP ARTIST",
                value = topArtist?.artist?.name ?: "N/A",
                detail = "${topArtist?.songCount ?: 0} songs",
                extra = "${topArtist?.minutes ?: 0} min"
            )
            
            // Album
            BestOfItem(
                icon = "💿",
                title = "TOP ALBUM",
                value = topAlbum?.album?.title ?: "N/A",
                detail = topAlbum?.album?.authorsText ?: "",
                extra = "${topAlbum?.minutes ?: 0} min"
            )
            
            // Month
            BestOfItem(
                icon = "📈",
                title = "PEAK MONTH",
                value = topMonth?.let { getMonthName(it.month) } ?: "N/A",
                detail = "${topMonth?.plays ?: 0} plays",
                extra = "${topMonth?.minutes ?: 0} min"
            )
            
            // Day
            BestOfItem(
                icon = "📅",
                title = "TOP DAY",
                value = topDay?.let { dayNames[it.dayOfWeek] ?: it.dayOfWeek } ?: "N/A",
                detail = "${topDay?.plays ?: 0} plays",
                extra = "${topDay?.minutes ?: 0} min"
            )
            
            // Hour
            BestOfItem(
                icon = "⏰",
                title = "PEAK HOUR",
                value = topHour?.hour ?: "N/A",
                detail = hourDescriptions[topHour?.hour] ?: "",
                extra = "${topHour?.minutes ?: 0} min"
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            ),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎶 TOTAL LISTENING",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${data.stats.totalMinutes}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "minutes • ${data.stats.totalPlays} plays",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Swipe → for Support & Donate",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun BestOfItem(
    icon: String,
    title: String,
    value: String,
    detail: String,
    extra: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Text(
                text = extra,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
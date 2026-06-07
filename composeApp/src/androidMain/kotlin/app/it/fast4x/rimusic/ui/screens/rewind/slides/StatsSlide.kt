package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.it.fast4x.rimusic.ui.styling.LocalAppearance

@Composable
fun StatsSlide(
    username: String,
    data: RewindData,
    userRanking: String,
    userPercentile: String,
    showMinutesInHours: Boolean,
    onNext: () -> Unit
) {
    val accent = LocalAppearance.current.colorPalette.accent
    RewindStoryFrame(header = "Overview", accent = accent) {
        Text(
            text = "You listened a lot\nin ${data.year}",
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF101836), accent.copy(alpha = 0.28f))))
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(data.stats.totalMinutes.toString(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.rewind_total_minutes).lowercase(), color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp)
                Text(userPercentile, color = accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RewindMetricCard(data.stats.totalPlays.toString(), stringResource(R.string.rewind_total_plays).lowercase(), Modifier.weight(1f), accent)
            RewindMetricCard(data.totalUniqueArtists.toString(), stringResource(R.string.rewind_unique_artists).lowercase(), Modifier.weight(1f), accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RewindMetricCard(data.totalUniqueSongs.toString(), stringResource(R.string.rewind_unique_songs).lowercase(), Modifier.weight(1f), accent)
            RewindMetricCard(data.totalUniqueAlbums.toString(), stringResource(R.string.rewind_unique_albums).lowercase(), Modifier.weight(1f), accent)
        }
        Spacer(Modifier.height(4.dp))
        RewindAction("See your top songs", accent, onNext)
    }
}

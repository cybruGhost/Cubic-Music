package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData

@Composable
fun BestOfAllSlide(data: RewindData, onNext: () -> Unit) {
    val accent = Color(0xFF8B5CF6)
    RewindStoryFrame(header = "Your Cubic Music rewind", accent = accent) {
        RewindAction(stringResource(R.string.rewind_brand_title), accent, onNext)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RewindMetricCard(data.stats.totalMinutes.toString(), "minutes of music", Modifier.weight(1f), accent)
                RewindMetricCard(data.stats.totalPlays.toString(), "plays", Modifier.weight(1f), accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RewindMetricCard(data.totalUniqueSongs.toString(), "songs", Modifier.weight(1f), accent)
                RewindMetricCard(data.totalUniqueArtists.toString(), "artists", Modifier.weight(1f), accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RewindMetricCard(data.totalUniqueAlbums.toString(), "albums", Modifier.weight(1f), accent)
                RewindMetricCard(data.daysWithMusic.toString(), "days", Modifier.weight(1f), accent)
            }
        }
        RewindAction("Thank you for making ${data.year} unforgettable", accent, onNext)
    }
}

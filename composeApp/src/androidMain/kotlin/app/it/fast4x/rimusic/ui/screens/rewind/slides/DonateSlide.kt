package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R

@Composable
fun DonateSlide(onNext: () -> Unit) {
    val accent = Color(0xFFE056D8)
    RewindStoryFrame(header = "Share your rewind", accent = accent) {
        Text("Share your story\n#CubicRewind", color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF15172F), Color.Black)))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REWIND\n2026", color = Color.White, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Image(painterResource(R.drawable.rewindlogo), null, modifier = Modifier.size(126.dp).clip(RoundedCornerShape(36.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.height(20.dp))
                Text("Made with love by Cubic Music", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
        }
        RewindAction(stringResource(R.string.rewind_donate_button), accent, onNext)
    }
}

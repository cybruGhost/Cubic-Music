package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WelcomeSlide(
    username: String,
    year: Int,
    onNext: () -> Unit
) {
    val (colorPalette, _, _) = LocalAppearance.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(10.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.36f), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.28f)
            repeat(5) { ring ->
                val radius = size.minDimension * (0.22f + ring * 0.035f)
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            colorPalette.accent.copy(alpha = 0.86f),
                            colorPalette.blue.copy(alpha = 0.78f),
                            Color(0xFFFF5ACD).copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 18f + ring * 22f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            repeat(48) { index ->
                val angle = index * 0.42f
                val distance = size.minDimension * (0.18f + (index % 9) * 0.023f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f + (index % 4) * 0.05f),
                    radius = 1.2.dp.toPx(),
                    center = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance)
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f), Color.Black),
                    startY = size.height * 0.36f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(96.dp))
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colorPalette.accent, Color(0xFFFF5ACD), colorPalette.blue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("<<", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(34.dp))
            Text(
                text = stringResource(R.string.rewind_brand_title).uppercase(),
                color = Color.White,
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Text(
                text = year.toString(),
                color = colorPalette.accent,
                fontSize = 40.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )

            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.rewind_greeting, username),
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.rewind_wrapped_subtitle),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 5.dp)
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(onClick = onNext)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE75CD8), colorPalette.accent, colorPalette.blue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.rewind_start),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "  >",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (index == 0) 0.95f else 0.28f))
                    )
                }
            }
        }
    }
}

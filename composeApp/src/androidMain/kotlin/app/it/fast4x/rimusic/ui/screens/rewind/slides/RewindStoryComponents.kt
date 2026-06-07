package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.ui.screens.rewind.components.NetworkImage
import app.it.fast4x.rimusic.ui.styling.LocalAppearance

@Composable
internal fun RewindStoryFrame(
    header: String,
    accent: Color = LocalAppearance.current.colorPalette.accent,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAppearance.current.colorPalette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(10.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(accent.copy(alpha = 0.22f), size.minDimension * 0.55f, Offset(size.width * 0.82f, size.height * 0.12f))
            drawCircle(palette.blue.copy(alpha = 0.14f), size.minDimension * 0.5f, Offset(size.width * 0.12f, size.height * 0.72f))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f), Color.Black)))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("<", color = Color.White, fontSize = 22.sp)
                Text(
                    text = header,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text("↗", color = Color.White.copy(alpha = 0.82f), fontSize = 16.sp)
            }
            content()
            Spacer(Modifier.weight(1f))
            RewindDots()
        }
    }
}

@Composable
internal fun RewindMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalAppearance.current.colorPalette.accent
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .padding(vertical = 15.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun RewindListRow(
    rank: Int,
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String?,
    featured: Boolean = false,
    accent: Color = LocalAppearance.current.colorPalette.accent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (featured) 16.dp else 10.dp))
            .background(if (featured) accent.copy(alpha = 0.18f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = if (featured) 10.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rank.toString(), color = if (featured) Color.White else accent, fontSize = if (featured) 30.sp else 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(34.dp))
        NetworkImage(
            url = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(if (featured) 96.dp else 48.dp)
                .clip(RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cleanPrefix(title), color = Color.White, fontSize = if (featured) 18.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(cleanPrefix(subtitle), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = Color.White.copy(alpha = 0.64f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun RewindHeroArt(imageUrl: String?, title: String, modifier: Modifier = Modifier) {
    NetworkImage(
        url = imageUrl,
        contentDescription = title,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
internal fun RewindAction(
    text: String,
    accent: Color = LocalAppearance.current.colorPalette.accent,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(accent.copy(alpha = 0.2f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(25.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun RewindDots() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == 1) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (index == 1) 0.88f else 0.28f))
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

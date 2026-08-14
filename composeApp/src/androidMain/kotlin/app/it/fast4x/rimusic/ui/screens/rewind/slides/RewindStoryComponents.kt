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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import java.text.NumberFormat
import kotlin.math.cos
import kotlin.math.sin

internal val RewindInk = Color(0xFF050508)
internal val RewindSurface = Color(0xFF101018)
internal val RewindCream = Color(0xFFFFF4EA)
internal val RewindRed = Color(0xFFFF3157)
internal val RewindOrange = Color(0xFFFF8A00)
internal val RewindPink = Color(0xFFFF7AC8)
internal val RewindPurple = Color(0xFFB36BFF)
internal val RewindBlue = Color(0xFF74C7FF)
internal val RewindGreen = Color(0xFF20E070)
internal val RewindYellow = Color(0xFFFFD447)

@Composable
internal fun RewindCanvasCard(
    title: String,
    year: Int,
    page: Int,
    pageCount: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    secondary: Color = RewindPurple,
    backgroundImageUrl: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RewindInk)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(10.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(28.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        backgroundImageUrl?.let { imageUrl ->
            RewindArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RewindInk.copy(alpha = 0.28f),
                                RewindInk.copy(alpha = 0.76f),
                                RewindInk.copy(alpha = 0.97f)
                            )
                        )
                    )
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = if (backgroundImageUrl == null) 0.28f else 0.14f),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.88f, size.height * 0.10f)
            )
            drawCircle(
                color = secondary.copy(alpha = if (backgroundImageUrl == null) 0.22f else 0.10f),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 0.06f, size.height * 0.78f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, RewindInk.copy(alpha = 0.18f), RewindInk.copy(alpha = 0.55f)),
                    startY = size.height * 0.48f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RewindCardHeader(title = title, year = year, page = page, pageCount = pageCount, accent = accent)
            content()
        }
    }
}

@Composable
internal fun RewindCardHeader(
    title: String,
    year: Int,
    page: Int,
    pageCount: Int,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rewind",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = year.toString(),
                color = RewindCream.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (index == page) 3.dp else 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (index == page) accent else RewindCream.copy(alpha = 0.24f))
                )
            }
            Text(
                text = "${page + 1}/$pageCount",
                color = RewindCream.copy(alpha = 0.76f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
        Text(
            text = title,
            color = RewindCream,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RewindArtwork(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    ImageCacheFactory.AsyncImage(
        thumbnailUrl = imageUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
internal fun RewindRankRow(
    rank: Int,
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String?,
    accent: Color,
    circular: Boolean = false,
    featured: Boolean = false
) {
    val shape = if (featured) RoundedCornerShape(22.dp) else RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (featured) accent.copy(alpha = 0.22f) else RewindCream.copy(alpha = 0.055f))
            .border(1.dp, RewindCream.copy(alpha = if (featured) 0.16f else 0.08f), shape)
            .padding(horizontal = 10.dp, vertical = if (featured) 12.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = rank.toString(),
            color = if (featured) RewindCream else accent,
            fontSize = if (featured) 34.sp else 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp)
        )
        RewindArtwork(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(if (featured) 72.dp else 48.dp)
                .clip(if (circular) CircleShape else RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = cleanPrefix(title).ifBlank { "-" },
                color = RewindCream,
                fontSize = if (featured) 17.sp else 14.sp,
                lineHeight = if (featured) 19.sp else 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = if (featured) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cleanPrefix(subtitle).ifBlank { "-" },
                color = RewindCream.copy(alpha = 0.68f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = meta,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(54.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RewindMetricPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(RewindCream.copy(alpha = 0.075f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = RewindCream.copy(alpha = 0.72f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RewindShareButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.24f))
            .border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = RewindCream,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            painter = painterResource(R.drawable.share_social),
            contentDescription = null,
            tint = RewindCream,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun RewindEmptyState(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(RewindCream.copy(alpha = 0.075f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rewind_color_cover),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(54.dp)
        )
        Text(
            text = title,
            color = RewindCream,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            color = RewindCream.copy(alpha = 0.68f),
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun RewindRadialMeter(
    value: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(56) { index ->
                val angle = Math.toRadians((index * 360f / 56f - 90f).toDouble())
                val active = index / 56f <= value.coerceIn(0f, 1f)
                val inner = radius - if (index % 4 == 0) 15.dp.toPx() else 9.dp.toPx()
                val outer = radius
                drawLine(
                    color = if (active) accent else RewindCream.copy(alpha = 0.18f),
                    start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                    end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                    strokeWidth = if (active) 2.2.dp.toPx() else 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = RewindCream.copy(alpha = 0.06f),
                radius = radius - 42.dp.toPx(),
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent), center, radius),
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
        }
        label()
    }
}

@Composable
internal fun RewindWaveform(
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    bars: Int = 42
) {
    val heights = remember(bars) {
        List(bars) { index ->
            0.22f + ((sin(index * 0.72f) + 1f) / 2f) * 0.62f
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index % 3 == 0) secondary else accent)
            )
        }
    }
}

@Composable
internal fun RewindOrbitalArt(
    images: List<String?>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(4) { ring ->
                val radius = size.minDimension * (0.25f + ring * 0.07f)
                drawCircle(
                    color = Color.Transparent,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawArc(
                    color = accent.copy(alpha = 0.22f + ring * 0.06f),
                    startAngle = ring * 28f,
                    sweepAngle = 92f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        val slots = listOf(
            Triple(Alignment.TopStart, 92.dp, -10f),
            Triple(Alignment.TopEnd, 76.dp, 8f),
            Triple(Alignment.BottomStart, 78.dp, 11f),
            Triple(Alignment.BottomEnd, 94.dp, -7f),
            Triple(Alignment.Center, 132.dp, 0f)
        )
        slots.forEachIndexed { index, slot ->
            val image = images.getOrNull(index)
            Box(
                modifier = Modifier
                    .align(slot.first)
                    .size(slot.second)
                    .clip(RoundedCornerShape(if (index == 4) 28.dp else 18.dp))
                    .border(1.dp, RewindCream.copy(alpha = 0.36f), RoundedCornerShape(if (index == 4) 28.dp else 18.dp))
            ) {
                RewindArtwork(image, null, Modifier.fillMaxSize(), ContentScale.Crop)
            }
        }
    }
}

internal fun formatRewindNumber(value: Long): String = NumberFormat.getIntegerInstance().format(value)

internal fun formatRewindMinutes(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0)
    return formatRewindNumber(safe)
}

internal fun formatRewindHours(minutes: Long): String {
    val hours = minutes / 60
    return if (hours > 0) "${formatRewindNumber(hours)}h" else "${formatRewindNumber(minutes)}m"
}

internal fun compactMetaMinutes(minutes: Long): String = "${formatRewindNumber(minutes.coerceAtLeast(0))} min"

internal fun firstNonBlank(vararg values: String?): String = values.firstOrNull { !it.isNullOrBlank() && it != "null" }.orEmpty()

// Compatibility wrappers for older slide files that still compile.
@Composable
internal fun RewindStoryFrame(
    header: String,
    accent: Color = LocalAppearance.current.colorPalette.accent,
    content: @Composable ColumnScope.() -> Unit
) {
    RewindCanvasCard(
        title = header,
        year = java.time.LocalDate.now().year,
        page = 0,
        pageCount = 1,
        accent = accent,
        content = content
    )
}

@Composable
internal fun RewindMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalAppearance.current.colorPalette.accent
) {
    RewindMetricPill(value = value, label = label, accent = accent, modifier = modifier)
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
    RewindRankRow(rank, title, subtitle, meta, imageUrl, accent, featured = featured)
}

@Composable
internal fun RewindHeroArt(imageUrl: String?, title: String, modifier: Modifier = Modifier) {
    RewindArtwork(
        imageUrl = imageUrl,
        contentDescription = title,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
internal fun RewindAction(
    text: String,
    accent: Color = LocalAppearance.current.colorPalette.accent,
    onClick: (() -> Unit)? = null
) {
    RewindShareButton(text = text, accent = accent, onClick = { onClick?.invoke() })
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
                    .background(RewindCream.copy(alpha = if (index == 1) 0.88f else 0.28f))
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

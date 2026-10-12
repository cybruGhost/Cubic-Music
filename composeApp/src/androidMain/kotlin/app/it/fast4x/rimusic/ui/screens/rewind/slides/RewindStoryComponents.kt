package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import it.fast4x.innertube.YtMusic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

internal val RewindInk = Color(0xFF08070C)
internal val RewindCream = Color(0xFFFFF6E6)
internal val RewindPurple = Color(0xFF461CF4)
internal val RewindPink = Color(0xFFFF4B98)
internal val RewindRed = Color(0xFFE83B2F)
internal val RewindOrange = Color(0xFFFF5A2E)
internal val RewindLime = Color(0xFFD9FF31)
internal val RewindBlue = Color(0xFF2369EB)
internal val RewindYellow = Color(0xFFFFD72E)
internal val RewindMuted = Color(0xFFB7B0C1)

internal enum class RewindRevealDirection {
    Up,
    Down,
    Left,
    Right
}

/**
 * Full-screen Rewind story shell.
 *
 * Important layout rule: slide content never owns the very bottom edge. That space is
 * reserved for the Cubic Music signature so the brand cannot land on top of slide copy.
 */
@Composable
internal fun RewindStoryShell(
    page: Int,
    pageCount: Int,
    background: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    showProgress: Boolean = true,
    showBrand: Boolean = true,
    backgroundArt: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .clickable(enabled = onNext != null) { onNext?.invoke() }
    ) {
        backgroundArt()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = if (showBrand) 38.dp else 18.dp)
        ) {
            RewindStoryProgress(
                page = page,
                pageCount = pageCount,
                color = progressColor,
                visible = showProgress
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }

        if (showBrand) {
            RewindBrandBug(
                foreground = progressColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun RewindStoryProgress(
    page: Int,
    pageCount: Int,
    color: Color,
    visible: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(pageCount) { index ->
            val alpha = if (!visible) 0f else when {
                index < page -> 0.82f
                index == page -> 1f
                else -> 0.22f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

@Composable
internal fun RewindBrandBug(
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "Cubic Music",
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Text(
            text = "CUBIC MUSIC",
            color = foreground.copy(alpha = 0.68f),
            fontSize = 7.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.9.sp
        )
    }
}

/**
 * One-shot reveal. Nothing loops: when a page becomes active the element enters once,
 * settles, and stays still until the user leaves the page.
 */
@Composable
internal fun RewindReveal(
    active: Boolean,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    distance: Dp = 24.dp,
    direction: RewindRevealDirection = RewindRevealDirection.Up,
    scaleFrom: Float = 0.98f,
    durationMillis: Int = 520,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (!active) {
            progress.snapTo(0f)
        } else {
            progress.snapTo(0f)
            if (delayMillis > 0) delay(delayMillis.toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            val travel = distance.toPx() * (1f - progress.value)
            when (direction) {
                RewindRevealDirection.Up -> translationY = travel
                RewindRevealDirection.Down -> translationY = -travel
                RewindRevealDirection.Left -> translationX = travel
                RewindRevealDirection.Right -> translationX = -travel
            }
            scaleX = scaleFrom + (1f - scaleFrom) * progress.value
            scaleY = scaleFrom + (1f - scaleFrom) * progress.value
        }
    ) {
        content()
    }
}

@Composable
internal fun RewindAnimatedNumber(
    value: Long,
    active: Boolean,
    color: Color,
    fontSize: Int,
    delayMillis: Int = 220,
    modifier: Modifier = Modifier,
    durationMillis: Int = 1050
) {
    val anim = remember(value) { Animatable(0f) }

    LaunchedEffect(active, value) {
        if (!active) {
            anim.snapTo(0f)
        } else {
            anim.snapTo(0f)
            delay(delayMillis.toLong())
            anim.animateTo(
                targetValue = value.coerceAtLeast(0).toFloat(),
                animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
            )
        }
    }

    Text(
        text = formatRewindNumber(anim.value.toLong()),
        color = color,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 0.90f).sp,
        letterSpacing = (-3.2).sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
internal fun RewindTypewriterText(
    text: String,
    active: Boolean,
    color: Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    lineHeight: Int = fontSize,
    maxLines: Int = 2,
    charDelayMillis: Long = 34L,
    letterSpacing: Float = -1.2f
) {
    var visibleCharacters by remember(text) { mutableStateOf(0) }

    LaunchedEffect(active, text) {
        visibleCharacters = 0
        if (!active) return@LaunchedEffect
        delay(delayMillis.toLong())
        text.indices.forEach { index ->
            visibleCharacters = index + 1
            delay(charDelayMillis)
        }
    }

    Text(
        text = text.take(visibleCharacters),
        color = color,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
        fontWeight = FontWeight.Black,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
internal fun RewindArtwork(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onError: (() -> Unit)? = null
) {
    ImageCacheFactory.AsyncImage(
        thumbnailUrl = imageUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onError = {
            if (!imageUrl.isNullOrBlank()) onError?.invoke()
        }
    )
}

@Composable
internal fun RewindArtworkWithFallback(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    background: Color = RewindPurple,
    foreground: Color = RewindCream,
    onError: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(if (circular) CircleShape else RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.trim().take(2).uppercase().ifBlank { "♪" },
            color = foreground.copy(alpha = 0.70f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        RewindArtwork(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            onError = onError
        )
    }
}

@Composable
internal fun RewindArtistArtwork(
    artistName: String,
    primaryUrl: String?,
    modifier: Modifier = Modifier,
    preferWikipedia: Boolean = false,
    circular: Boolean = true,
    enableSlideshow: Boolean = false
) {
    var wikipediaUrl by remember(artistName) { mutableStateOf<String?>(null) }
    var failedUrls by remember(artistName, primaryUrl) { mutableStateOf(emptySet<String>()) }
    var activeIndex by remember(artistName, primaryUrl) { mutableStateOf(0) }

    LaunchedEffect(artistName, primaryUrl, preferWikipedia, enableSlideshow) {
        wikipediaUrl = null
        failedUrls = emptySet()
        activeIndex = 0
        if (artistName.isBlank()) return@LaunchedEffect
        if (enableSlideshow || preferWikipedia || primaryUrl.isNullOrBlank()) {
            wikipediaUrl = withContext(Dispatchers.IO) {
                WikipediaArtistImageResolver.findImage(artistName)
            }
        }
    }

    val orderedUrls = if (preferWikipedia) {
        listOf(wikipediaUrl, primaryUrl)
    } else {
        listOf(primaryUrl, wikipediaUrl)
    }
    val candidates = orderedUrls
        .filterNotNull()
        .filter { it.isNotBlank() && it !in failedUrls }
        .distinct()

    LaunchedEffect(candidates, enableSlideshow) {
        activeIndex = activeIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0))
        if (enableSlideshow && candidates.size > 1) {
            while (true) {
                delay(7_000)
                activeIndex = (activeIndex + 1) % candidates.size
            }
        }
    }

    val selectedUrl = candidates.getOrNull(activeIndex) ?: candidates.firstOrNull()
    Crossfade(
        targetState = selectedUrl,
        animationSpec = tween(durationMillis = 900),
        label = "rewindArtistArtwork"
    ) { imageUrl ->
        RewindArtworkWithFallback(
            imageUrl = imageUrl,
            title = artistName,
            modifier = modifier,
            circular = circular,
            background = RewindPurple,
            foreground = RewindCream,
            onError = {
                if (!imageUrl.isNullOrBlank()) failedUrls = failedUrls + imageUrl
            }
        )
    }
}

internal data class ArtistWikiMetadata(
    val imageUrl: String?,
    val description: String?,
    val bio: String? = null,
    val wikipediaUrl: String? = null
)

@Composable
internal fun rememberArtistDescription(artistName: String): String? {
    var description by remember(artistName) { mutableStateOf<String?>(null) }
    LaunchedEffect(artistName) {
        description = null
        if (artistName.isBlank()) return@LaunchedEffect
        description = withContext(Dispatchers.IO) {
            WikipediaArtistMetadataResolver.find(artistName)?.description
        }
    }
    return description
}

/**
 * Full Wikipedia metadata (portrait, short description, longer bio paragraph and the article
 * URL) for a single artist. Used by the Top Artist spotlight page. Returns null while loading
 * or when nothing plausible was found — callers should render gracefully without it.
 */
@Composable
internal fun rememberArtistWikiMetadata(
    artistName: String,
    fallbackBrowseId: String? = null
): ArtistWikiMetadata? {
    var metadata by remember(artistName, fallbackBrowseId) { mutableStateOf<ArtistWikiMetadata?>(null) }
    LaunchedEffect(artistName, fallbackBrowseId) {
        metadata = null
        if (artistName.isBlank()) return@LaunchedEffect
        metadata = withContext(Dispatchers.IO) {
            val wikipedia = WikipediaArtistMetadataResolver.find(artistName)
            if (wikipedia?.hasUsableDescription() == true) {
                wikipedia
            } else {
                fallbackBrowseId
                    ?.takeIf(String::isNotBlank)
                    ?.let { browseId ->
                        YtMusic.getArtistPage(browseId).getOrNull()?.let { page ->
                            ArtistWikiMetadata(
                                imageUrl = page.artist.thumbnail?.url,
                                description = page.description,
                                bio = page.description
                            )
                        }
                    }
            }
        }
    }
    return metadata
}

private fun ArtistWikiMetadata.hasUsableDescription(): Boolean {
    val text = (bio ?: description).orEmpty().trim()
    return text.isNotBlank() &&
        !text.contains("may refer to", ignoreCase = true) &&
        !text.contains("disambiguation", ignoreCase = true)
}

private object WikipediaArtistImageResolver {
    fun findImage(artistName: String): String? =
        WikipediaArtistMetadataResolver.find(artistName)?.imageUrl
}

private object WikipediaArtistMetadataResolver {
    private const val MissCacheTtlMs = 5L * 60L * 1000L
    private val cache = ConcurrentHashMap<String, ArtistWikiMetadata>()
    private val misses = ConcurrentHashMap<String, Long>()

    fun find(artistName: String): ArtistWikiMetadata? {
        val key = artistName.trim().lowercase()
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        val now = System.currentTimeMillis()
        misses[key]?.let { missedAt ->
            if (now - missedAt < MissCacheTtlMs) return null
            misses.remove(key, missedAt)
        }

        val candidates = listOf(
            "$artistName musician",
            "$artistName singer",
            "$artistName rapper",
            artistName
        )

        val resolved = candidates.firstNotNullOfOrNull { query ->
            runCatching { requestMetadata(artistName, query) }.getOrNull()
        }
        if (resolved != null) {
            cache[key] = resolved
            misses.remove(key)
        } else {
            misses[key] = now
        }
        return resolved
    }

    private fun requestMetadata(artistName: String, query: String): ArtistWikiMetadata? {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val endpoint =
            "https://en.wikipedia.org/w/api.php?action=query&generator=search" +
                "&gsrsearch=$encoded&gsrnamespace=0&gsrlimit=5" +
                "&prop=pageimages|description|extracts|info&pithumbsize=1000" +
                "&exintro=1&explaintext=1&exchars=480&inprop=url" +
                "&format=json&formatversion=2&redirects=1"

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 7000
            connection.setRequestProperty("User-Agent", "CubicMusic-Rewind/1.0")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val pages = JSONObject(body)
                .optJSONObject("query")
                ?.optJSONArray("pages")
                ?: return null

            val normalized = artistName.trim().lowercase()
            val results = buildList {
                for (i in 0 until pages.length()) {
                    val page = pages.optJSONObject(i) ?: continue
                    val title = page.optString("title").trim().lowercase()
                    val source = page.optJSONObject("thumbnail")?.optString("source").orEmpty()
                    val description = page.optString("description")
                        .takeIf { it.isNotBlank() && !it.contains("may refer to", ignoreCase = true) }
                    val bio = page.optString("extract")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .takeIf {
                            it.isNotBlank() &&
                                !it.contains("may refer to", ignoreCase = true) &&
                                !it.contains("disambiguation", ignoreCase = true)
                        }
                    val pageUrl = page.optString("fullurl").takeIf { it.isNotBlank() }
                    if (source.isBlank() && description.isNullOrBlank() && bio.isNullOrBlank()) continue
                    val score = when {
                        title == normalized -> 0
                        title.startsWith("$normalized (") -> 1
                        title.contains(normalized) -> 2
                        else -> 3
                    }
                    add(score to ArtistWikiMetadata(source.ifBlank { null }, description, bio, pageUrl))
                }
            }
            results.minByOrNull { it.first }?.second
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
internal fun RewindKicker(
    text: String,
    background: Color,
    foreground: Color = RewindInk,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = foreground,
        fontSize = 9.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.0.sp,
        modifier = modifier
            .background(background, RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
internal fun RewindRankRow(
    rank: Int,
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String?,
    foreground: Color,
    accent: Color,
    active: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    circularArt: Boolean = false
) {
    RewindReveal(
        active = active,
        delayMillis = delayMillis,
        direction = RewindRevealDirection.Left,
        distance = 18.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = rank.toString(),
                color = accent,
                fontSize = 25.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(27.dp),
                textAlign = TextAlign.Center
            )

            RewindArtworkWithFallback(
                imageUrl = imageUrl,
                title = title,
                modifier = Modifier.size(44.dp),
                circular = circularArt,
                background = accent.copy(alpha = 0.28f),
                foreground = foreground
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanPrefix(title).ifBlank { "—" },
                    color = foreground,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cleanPrefix(subtitle).ifBlank { "—" },
                    color = foreground.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = meta,
                color = foreground.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(60.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun RewindEmptyState(
    title: String,
    body: String,
    foreground: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RewindKicker("REWIND", accent)
        Text(
            text = title,
            color = foreground,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.7).sp
        )
        Text(
            text = body,
            color = foreground.copy(alpha = 0.66f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

internal data class ListenerBadge(
    val title: String,
    val subtitle: String,
    val index: Int,
    val tier: Int
)

/**
 * Cubic listening index. This is deliberately NOT called a percentile because the app only
 * has the listener's local history, not a global population. The reference point is calibrated
 * so ~45k minutes / 14k plays / 201 days / 5.6k unique songs lands around index 70, leaving
 * several genuinely harder tiers above it.
 */
internal fun calculateListenerBadge(data: RewindData): ListenerBadge {
    val minuteRatio = (data.stats.totalMinutes.toDouble() / 45_000.0).coerceIn(0.0, 2.4)
    val playRatio = (data.stats.totalPlays.toDouble() / 14_000.0).coerceIn(0.0, 2.4)
    val dayRatio = (data.daysWithMusic.toDouble() / 201.0).coerceIn(0.0, 1.82)
    val uniqueRatio = (data.totalUniqueSongs.toDouble() / 5_612.0).coerceIn(0.0, 2.4)

    val intensity = (
        minuteRatio * 0.38 +
            playRatio * 0.25 +
            dayRatio * 0.22 +
            uniqueRatio * 0.15
        )
    val index = (intensity * 70.0).roundToInt().coerceIn(0, 170)

    return when {
        index < 15 -> ListenerBadge("CURIOUS", "Still finding your sound.", index, 0)
        index < 30 -> ListenerBadge("EXPLORER", "You tried a lot this year.", index, 1)
        index < 45 -> ListenerBadge("IN ROTATION", "Music, most days.", index, 2)
        index < 60 -> ListenerBadge("DEDICATED", "You kept coming back.", index, 3)
        index < 75 -> ListenerBadge("HEAVY ROTATION", "Music ran through your whole year.", index, 4)
        index < 90 -> ListenerBadge("RELENTLESS", "You barely hit pause.", index, 5)
        index < 110 -> ListenerBadge("SOUND MACHINE", "Top-tier listening hours.", index, 6)
        index < 130 -> ListenerBadge("TIME BENDER", "An absurd number of hours.", index, 7)
        else -> ListenerBadge("BEYOND REPEAT", "One long, uninterrupted queue.", index, 8)
    }
}

internal fun formatRewindNumber(value: Long): String =
    NumberFormat.getIntegerInstance().format(value.coerceAtLeast(0))

internal fun formatRewindMinutes(minutes: Long): String =
    formatRewindNumber(minutes.coerceAtLeast(0))

internal fun compactMetaMinutes(minutes: Long): String =
    "${formatRewindNumber(minutes.coerceAtLeast(0))} min"

internal fun firstNonBlank(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() && it != "null" }.orEmpty()

internal fun formatHourLabel(hour: String?): String {
    val raw = hour?.substringBefore(':')?.toIntOrNull() ?: return "—"
    val normalized = when {
        raw == 0 -> 12
        raw > 12 -> raw - 12
        else -> raw
    }
    return "$normalized ${if (raw < 12) "AM" else "PM"}"
}
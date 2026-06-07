@file:Suppress("DEPRECATION")

package app.it.fast4x.rimusic.utils

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.styling.ColorPalette
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.ui.styling.Typography
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
//  WEATHER PALETTE — accent tints layered over app ColorPalette
// ─────────────────────────────────────────────────────────────────

data class WeatherPalette(
    val accent    : Color,   // progress bars, badges, highlights
    val accentSoft: Color,   // secondary accent (muted)
    val glowColor : Color,   // drawBehind radial orb
    val tintLayer : Color    // subtle bg tint in hero card
)

fun resolveWeatherPalette(condition: String, isNight: Boolean): WeatherPalette = when {
    condition == "thunderstorm" -> WeatherPalette(
        accent     = Color(0xFFB388FF),
        accentSoft = Color(0xFF7E57C2),
        glowColor  = Color(0xFF7C4DFF),
        tintLayer  = Color(0xFF1A0A2E)
    )
    condition == "rain" || condition == "drizzle" -> WeatherPalette(
        accent     = Color(0xFF4FC3F7),
        accentSoft = Color(0xFF0288D1),
        glowColor  = Color(0xFF0277BD),
        tintLayer  = Color(0xFF051829)
    )
    condition == "snow" -> WeatherPalette(
        accent     = Color(0xFFB2EBF2),
        accentSoft = Color(0xFF4DD0E1),
        glowColor  = Color(0xFF00ACC1),
        tintLayer  = Color(0xFF0A1A29)
    )
    condition == "mist" -> WeatherPalette(
        accent     = Color(0xFF90A4AE),
        accentSoft = Color(0xFF607D8B),
        glowColor  = Color(0xFF546E7A),
        tintLayer  = Color(0xFF0D1219)
    )
    condition == "clouds" -> WeatherPalette(
        accent     = Color(0xFFFFCA28),
        accentSoft = Color(0xFFFFA000),
        glowColor  = Color(0xFFFF8F00),
        tintLayer  = Color(0xFF0D1520)
    )
    isNight -> WeatherPalette(
        accent     = Color(0xFF7986CB),
        accentSoft = Color(0xFF3F51B5),
        glowColor  = Color(0xFF283593),
        tintLayer  = Color(0xFF03051A)
    )
    else -> WeatherPalette( // clear / sunny
        accent     = Color(0xFFFFD54F),
        accentSoft = Color(0xFFFFB300),
        glowColor  = Color(0xFFFF8F00),
        tintLayer  = Color(0xFF061020)
    )
}

// ─────────────────────────────────────────────────────────────────
//  MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────────

@Composable
fun WeatherForecastPopup(
    weatherData     : WeatherData,
    username        : String,
    onDismiss       : () -> Unit,
    onCityChange    : () -> Unit,
    temperatureUnit : String = "celsius"
) {
    val uriHandler   = LocalUriHandler.current
    val appearance   = LocalAppearance.current
    val cp           = appearance.colorPalette   // ColorPalette
    val typography   = appearance.typography     // Typography

    val isCelsius    = temperatureUnit == "celsius"
    val localTime    = getAccurateLocalTimeGMT3(weatherData.timezoneOffset)
    val localHour    = localTime.get(Calendar.HOUR_OF_DAY)
    val isNight      = localHour >= 20 || localHour < 6
    val condition    = normalizeCondition(weatherData.condition)
    val wp           = resolveWeatherPalette(condition, isNight)
    val rainPct      = calculateRealRainProbability(
        weatherData.humidity, condition, weatherData.cloudCover, weatherData.pressure
    )

    var showSports     by remember { mutableStateOf(false) }
    var sportsList     by remember { mutableStateOf<List<LiveSport>>(emptyList()) }
    var loadingSports  by remember { mutableStateOf(false) }
    var selectedLeague by remember { mutableStateOf("eng.1") }

    // Shared infinite transitions — same pattern as DonateScreen
    val inf = rememberInfiniteTransition(label = "weather")
    val glowAlpha by inf.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )
    val orbAngle by inf.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "orb"
    )

    LaunchedEffect(selectedLeague, showSports) {
        if (showSports) {
            loadingSports = true
            sportsList = fetchLiveSportsFromESPN(selectedLeague)
            loadingSports = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = cp.background0,
        shape            = RoundedCornerShape(24.dp),
        title = {
            WeatherDialogHeader(
                username      = username,
                isCelsius     = isCelsius,
                loadingSports = loadingSports,
                wp            = wp,
                cp            = cp,
                onSports      = { showSports = true },
                onCityChange  = onCityChange
            )
        },
        text = {
            Box {
                // Rotating ambient orbs — DonateScreen Canvas pattern
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 0..2) {
                        rotate(orbAngle + i * 120f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        wp.glowColor.copy(alpha = glowAlpha * 0.09f),
                                        Color.Transparent
                                    ),
                                    center = Offset(
                                        size.width  * if (i % 2 == 0) 0.15f else 0.85f,
                                        size.height * if (i == 0) 0.08f else 0.55f
                                    ),
                                    radius = size.minDimension * 0.7f
                                ),
                                radius = size.minDimension * 0.7f,
                                center = Offset(size.width * 0.5f, size.height * 0.5f)
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding      = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        HeroWeatherCard(
                            weatherData, localTime, isNight, condition,
                            rainPct, isCelsius, wp, cp, glowAlpha
                        )
                    }

                    if (shouldShowRainPrediction(rainPct, weatherData.humidity, condition)) {
                        item {
                            RainCard(rainPct, condition, weatherData.humidity,
                                weatherData.cloudCover, weatherData.pressure, wp, cp)
                        }
                    }

                    item { InsightCard(weatherData, condition, rainPct, localHour, wp, cp) }
                    item { ActivityCard(weatherData, localHour, isNight, rainPct, condition, wp, cp) }
                    item { MetricsCard(weatherData, isCelsius, wp, cp) }
                    item { HydrationCard(cp) }
                    item {
                        CubeSportsCard(cp, glowAlpha) {
                            uriHandler.openUri("https://thecub4.vercel.app/Cubesports")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = wp.accent),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Close",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = if (wp.accent.luminance() > 0.35f) Color(0xFF111111) else Color.White
                )
            }
        }
    )

    if (showSports) {
        SportsDialog(
            sports         = sportsList,
            selectedLeague = selectedLeague,
            onLeagueChange = { selectedLeague = it },
            onDismiss      = { showSports = false },
            isLoading      = loadingSports,
            wp             = wp,
            cp             = cp,
            onOpenSite     = { uriHandler.openUri("https://thecub4.vercel.app/Cubesports") }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
//  DIALOG HEADER
// ─────────────────────────────────────────────────────────────────

@Composable
private fun WeatherDialogHeader(
    username     : String,
    isCelsius    : Boolean,
    loadingSports: Boolean,
    wp           : WeatherPalette,
    cp           : ColorPalette,
    onSports     : () -> Unit,
    onCityChange : () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Hi, $username 👋",
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = cp.text
            )
            Text(
                if (isCelsius) "Showing °C" else "Showing °F",
                fontSize = 11.sp,
                color    = cp.textSecondary
            )
        }

        // Sports button — same style as DonateScreen back button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cp.background2.copy(alpha = 0.6f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSports() },
            contentAlignment = Alignment.Center
        ) {
            Text(if (loadingSports) "⚙️" else "⚽", fontSize = 18.sp)
        }

        Spacer(Modifier.width(8.dp))

        // City button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cp.background2.copy(alpha = 0.6f))
                .border(0.7.dp, wp.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCityChange() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Change city",
                tint = wp.accent, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  HERO CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroWeatherCard(
    weatherData : WeatherData,
    localTime   : Calendar,
    isNight     : Boolean,
    condition   : String,
    rainPct     : Int,
    isCelsius   : Boolean,
    wp          : WeatherPalette,
    cp          : ColorPalette,
    glowAlpha   : Float
) {
    val inf = rememberInfiniteTransition(label = "hero")
    val emojiPulse by inf.animateFloat(
        initialValue = 0.94f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "emoji"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(wp.tintLayer, cp.background0))
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    listOf(wp.accent.copy(alpha = 0.35f), wp.glowColor.copy(alpha = 0.08f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            // Glow orb — exact drawBehind pattern from DonateScreen profile image
            .drawBehind {
                drawCircle(
                    color  = wp.glowColor.copy(alpha = glowAlpha * 0.22f),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.82f, size.height * 0.18f)
                )
            }
    ) {
        Column(Modifier.padding(18.dp)) {
            // Location + date row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(weatherData.city, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = cp.text)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(wp.accent.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(formatFullDateGMT3(localTime), fontSize = 10.sp, color = wp.accent)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Emoji + temperature
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = getDynamicWeatherEmoji(condition, isNight),
                    fontSize = 60.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = emojiPulse
                        scaleY = emojiPulse
                    }
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        formatTemperature(weatherData.temp, isCelsius),
                        fontSize   = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color      = cp.text,
                        lineHeight = 48.sp
                    )
                    Text(
                        getAccurateWeatherDescription(condition, weatherData),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = wp.accent
                    )
                    Text(
                        "Feels like ${formatTemperature(weatherData.feelsLike, isCelsius)}",
                        fontSize = 12.sp,
                        color    = cp.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Accent divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(wp.accent.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(12.dp))

            // Greeting + time badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(cp.background2.copy(alpha = 0.5f))
                    .border(0.5.dp, wp.accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${getTimeOfDayGreeting(localTime.get(Calendar.HOUR_OF_DAY))} · ${formatAccurateLocalTimeGMT3(localTime)}",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = wp.accentSoft
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                getSmartWeatherAnalysis(weatherData, condition, rainPct),
                fontSize   = 12.sp,
                color      = cp.textSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  SHARED SECTION SHELL
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(cp: ColorPalette, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cp.background2.copy(alpha = 0.55f))
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun SectionTitle(emoji: String, title: String, wp: WeatherPalette, cp: ColorPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(cp.background3.copy(alpha = 0.8f))
                .border(0.5.dp, wp.accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 13.sp) }
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = cp.text)
    }
}

// ─────────────────────────────────────────────────────────────────
//  RAIN CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun RainCard(
    rainPct   : Int,
    condition : String,
    humidity  : Int,
    cloudCover: Int?,
    pressure  : Int,
    wp        : WeatherPalette,
    cp        : ColorPalette
) {
    val progress by animateFloatAsState(
        targetValue   = rainPct / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "rain"
    )
    val barColor = when {
        rainPct >= 70 -> Color(0xFFEF5350)
        rainPct >= 40 -> Color(0xFFFFB300)
        else          -> Color(0xFF66BB6A)
    }

    SectionCard(cp) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SectionTitle(getRainEmoji(condition, rainPct), "Precipitation", wp, cp)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor.copy(alpha = 0.14f))
                        .border(0.5.dp, barColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$rainPct%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = barColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = barColor,
                trackColor = cp.background0
            )
            Spacer(Modifier.height(10.dp))
            Text(
                getRealRainAnalysis(rainPct, condition, humidity, cloudCover, pressure),
                fontSize = 12.sp, color = cp.textSecondary, lineHeight = 17.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  INSIGHT CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun InsightCard(
    weatherData: WeatherData,
    condition  : String,
    rainPct    : Int,
    localHour  : Int,
    wp         : WeatherPalette,
    cp         : ColorPalette
) {
    SectionCard(cp) {
        Column {
            SectionTitle("🧠", "Weather Intelligence", wp, cp)
            Spacer(Modifier.height(8.dp))
            Text(getSmartForecast(weatherData, condition, rainPct, localHour),
                fontSize = 12.sp, color = cp.textSecondary, lineHeight = 17.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  ACTIVITY CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ActivityCard(
    weatherData: WeatherData,
    localHour  : Int,
    isNight    : Boolean,
    rainPct    : Int,
    condition  : String,
    wp         : WeatherPalette,
    cp         : ColorPalette
) {
    SectionCard(cp) {
        Column {
            SectionTitle("🎯", "Something you can do", wp, cp)
            Spacer(Modifier.height(8.dp))
            Text(getLogicalActivities(weatherData, localHour, isNight, rainPct, condition),
                fontSize = 12.sp, color = cp.textSecondary, lineHeight = 17.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  METRICS 2×3 GRID
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MetricsCard(
    weatherData: WeatherData,
    isCelsius  : Boolean,
    wp         : WeatherPalette,
    cp         : ColorPalette
) {
    val metrics = listOf(
        Triple("💧", "Humidity",
            "${weatherData.humidity}%  ·  ${getHumidityAnalysis(weatherData.humidity)}"),
        Triple("☁️", "Cloud Cover",
            "${getSmartCloudCover(weatherData.cloudCover, weatherData.condition)}%  ·  ${getCloudCoverAnalysis(getSmartCloudCover(weatherData.cloudCover, weatherData.condition))}"),
        Triple("💨", "Wind",
            "${weatherData.windSpeed} m/s  ·  ${getWindAnalysis(weatherData.windSpeed)}"),
        Triple("🌡", "Pressure",
            "${weatherData.pressure} hPa  ·  ${getPressureAnalysis(weatherData.pressure)}"),
        Triple("👁", "Visibility",
            "${weatherData.visibility / 1000} km  ·  ${getVisibilityAnalysis(weatherData.visibility)}"),
        Triple("↕", "Min / Max",
            "${formatTemperature(weatherData.minTemp, isCelsius)} / ${formatTemperature(weatherData.maxTemp, isCelsius)}")
    )

    SectionCard(cp) {
        Column {
            SectionTitle("📊", "Weather Details", wp, cp)
            Spacer(Modifier.height(12.dp))
            metrics.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (emoji, label, value) ->
                        MetricTile(emoji, label, value, wp, cp, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(
    emoji   : String,
    label   : String,
    value   : String,
    wp      : WeatherPalette,
    cp      : ColorPalette,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cp.background0.copy(alpha = 0.6f))
            .border(0.5.dp, wp.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text("$emoji  $label", fontSize = 10.sp, color = cp.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = cp.text, maxLines = 2, lineHeight = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  HYDRATION CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HydrationCard(cp: ColorPalette) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0D47A1).copy(alpha = 0.22f),
                        Color(0xFF006064).copy(alpha = 0.12f)
                    )
                )
            )
            .border(0.5.dp, Color(0xFF29B6F6).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💧", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Stay Hydrated",
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF81D4FA))
                Text("Drink water throughout the day, whatever the weather.",
                    fontSize = 11.sp, color = cp.textSecondary, lineHeight = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  CUBESPORTS PROMO CARD
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CubeSportsCard(cp: ColorPalette, glowAlpha: Float, onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cp.background2.copy(alpha = 0.5f))
            .border(0.6.dp, Color(0xFF4CAF50).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .drawBehind {
                drawCircle(
                    color  = Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.12f),
                    radius = size.minDimension * 0.6f,
                    center = Offset(size.width * 0.88f, size.height * 0.3f)
                )
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onOpen() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚽", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("CubeSports", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = Color(0xFF81C784))
                Text("Live scores, streams & match details",
                    fontSize = 11.sp, color = cp.textSecondary, lineHeight = 15.sp)
            }
            Icon(Icons.Outlined.OpenInNew, contentDescription = null,
                tint = Color(0xFF4CAF50).copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  SPORTS DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SportsDialog(
    sports        : List<LiveSport>,
    selectedLeague: String,
    onLeagueChange: (String) -> Unit,
    onDismiss     : () -> Unit,
    isLoading     : Boolean,
    wp            : WeatherPalette,
    cp            : ColorPalette,
    onOpenSite    : () -> Unit
) {
    val leagues = listOf(
        "eng.1"          to "Premier League",
        "esp.1"          to "La Liga",
        "ita.1"          to "Serie A",
        "ger.1"          to "Bundesliga",
        "fra.1"          to "Ligue 1",
        "uefa.champions" to "UCL"
    )

    val inf = rememberInfiniteTransition(label = "sd")
    val glowAlpha by inf.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sdGlow"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = cp.background0,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glow behind football — DonateScreen profile image pattern
                Box(
                    Modifier
                        .size(40.dp)
                        .drawBehind {
                            drawCircle(
                                color  = Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.3f),
                                radius = size.minDimension / 2 + 10f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) { Text("⚽", fontSize = 22.sp) }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text("Live Football", fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = cp.text)
                    Text("Want more data?", fontSize = 11.sp, color = cp.textSecondary)
                }

                // CubeSports link button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                        .border(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onOpenSite() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CubeSports", fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF81C784))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null,
                            tint = Color(0xFF81C784), modifier = Modifier.size(11.dp))
                    }
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // League chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding        = PaddingValues(vertical = 8.dp)
                ) {
                    items(leagues) { (code, name) ->
                        val selected = selectedLeague == code
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) wp.accent.copy(alpha = 0.18f)
                                    else cp.background2.copy(alpha = 0.5f)
                                )
                                .border(
                                    0.6.dp,
                                    if (selected) wp.accent.copy(alpha = 0.45f) else cp.background2,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onLeagueChange(code) }
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Text(
                                name,
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected) wp.accent else cp.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                when {
                    isLoading       -> SportsLoadingState(wp, cp)
                    sports.isEmpty() -> SportsEmptyState(cp)
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding      = PaddingValues(vertical = 2.dp)
                    ) {
                        items(sports) { sport -> MatchCard(sport, wp, cp) }
                    }
                }
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onOpenSite,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape    = RoundedCornerShape(12.dp)
                ) { Text("⚽  CubeSports", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White) }

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = cp.background2),
                    shape    = RoundedCornerShape(12.dp)
                ) { Text("Close", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = cp.textSecondary) }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
//  MATCH CARD — real logos (AsyncImage / coil3), live pulse dot
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MatchCard(sport: LiveSport, wp: WeatherPalette, cp: ColorPalette) {
    val isLive  = sport.status == "in"
    val isFinal = sport.status == "final"

    val inf = rememberInfiniteTransition(label = "dot_${sport.id}")
    val dotAlpha by inf.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cp.background2.copy(alpha = 0.55f))
            .border(
                0.6.dp,
                if (isLive) Color(0xFFEF5350).copy(alpha = 0.35f) else cp.background2,
                RoundedCornerShape(16.dp)
            )
            .drawBehind {
                if (isLive) drawCircle(
                    color  = Color(0xFFEF5350).copy(alpha = dotAlpha * 0.06f),
                    radius = size.minDimension * 0.7f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                )
            }
            .padding(12.dp)
    ) {
        Column {
            // League + status
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(sport.league, fontSize = 10.sp,
                    color = cp.textSecondary.copy(alpha = 0.55f))
                LiveBadge(sport.status, sport.time, sport.clock, dotAlpha, wp, cp)
            }

            Spacer(Modifier.height(12.dp))

            // Home — Score — Away
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TeamColumn(sport.homeTeam, sport.homeLogo, cp, Modifier.weight(1f), alignEnd = false)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sport.status != "scheduled") {
                        Text(
                            "${sport.homeScore} — ${sport.awayScore}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = if (isLive) Color(0xFFEF9A9A) else cp.text
                        )
                        if (isLive && sport.clock.isNotBlank()) {
                            Text(sport.clock, fontSize = 10.sp, color = Color(0xFFEF5350))
                        }
                    } else {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(cp.background0.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(sport.time, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, color = wp.accent)
                        }
                    }
                }

                TeamColumn(sport.awayTeam, sport.awayLogo, cp, Modifier.weight(1f), alignEnd = true)
            }

            if (sport.venue.isNotBlank() && sport.venue != "TBD") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "📍  ${sport.venue}",
                    fontSize = 10.sp,
                    color    = cp.textSecondary.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(
    name    : String,
    logoUrl : String,
    cp      : ColorPalette,
    modifier: Modifier,
    alignEnd: Boolean
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        // Real ESPN logo via AsyncImage — same import as DonateScreen
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cp.background0.copy(alpha = 0.5f))
                .border(0.5.dp, cp.background2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model              = logoUrl,
                    contentDescription = name,
                    modifier           = Modifier.size(30.dp),
                    contentScale       = ContentScale.Fit
                )
            } else {
                Text(name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cp.textSecondary)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            name,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 11.sp,
            color      = cp.text,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = if (alignEnd) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
private fun LiveBadge(
    status  : String,
    time    : String,
    clock   : String,
    dotAlpha: Float,
    wp      : WeatherPalette,
    cp      : ColorPalette
) {
    val isLive  = status == "in"
    val isFinal = status == "final"

    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                when {
                    isLive  -> Color(0xFFEF5350).copy(alpha = 0.14f)
                    isFinal -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                    else    -> cp.background2.copy(alpha = 0.5f)
                }
            )
            .border(
                0.5.dp,
                when {
                    isLive  -> Color(0xFFEF5350).copy(alpha = 0.35f)
                    isFinal -> Color(0xFF4CAF50).copy(alpha = 0.25f)
                    else    -> cp.background2
                },
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLive) {
                Box(Modifier.size(5.dp).clip(CircleShape)
                    .background(Color(0xFFEF5350).copy(alpha = dotAlpha)))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text       = when { isLive -> "LIVE"; isFinal -> "FT"; else -> time },
                fontWeight = FontWeight.Bold,
                fontSize   = 10.sp,
                color      = when {
                    isLive  -> Color(0xFFEF9A9A)
                    isFinal -> Color(0xFFA5D6A7)
                    else    -> cp.textSecondary
                }
            )
        }
    }
}

@Composable
private fun SportsLoadingState(wp: WeatherPalette, cp: ColorPalette) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cp.background2.copy(alpha = 0.4f))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = wp.accent, strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("Fetching matches…", fontSize = 12.sp, color = cp.textSecondary)
        }
    }
}

@Composable
private fun SportsEmptyState(cp: ColorPalette) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cp.background2.copy(alpha = 0.4f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏟", fontSize = 34.sp)
            Spacer(Modifier.height(8.dp))
            Text("No matches right now", fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, color = cp.text)
            Text("Check CubeSports for more", fontSize = 11.sp,
                color = cp.textSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  ESPN API
// ─────────────────────────────────────────────────────────────────

private suspend fun fetchLiveSportsFromESPN(leagueCode: String): List<LiveSport> =
    withContext(Dispatchers.IO) {
        try {
            val cal = Calendar.getInstance()
            val end = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val start = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)

            val json = JSONObject(
                URL("https://site.api.espn.com/apis/site/v2/sports/soccer/$leagueCode/scoreboard?dates=$start-$end")
                    .readText()
            )
            if (!json.has("events")) return@withContext emptyList<LiveSport>()

            val events = json.getJSONArray("events")
            val list   = mutableListOf<LiveSport>()

            for (i in 0 until events.length()) {
                try {
                    val event       = events.getJSONObject(i)
                    val comps       = event.optJSONArray("competitions") ?: continue
                    if (comps.length() == 0) continue
                    val comp        = comps.getJSONObject(0)
                    val competitors = comp.optJSONArray("competitors") ?: continue
                    if (competitors.length() < 2) continue

                    val leagueName = comp.optJSONObject("league")?.optString("name")
                        ?: event.optJSONObject("league")?.optString("name") ?: "Football"
                    val venue      = comp.optJSONObject("venue")?.optString("fullName") ?: ""

                    val homeObj = competitors.findJSONObject { it.optString("homeAway") == "home" }
                        ?: competitors.getJSONObject(0)
                    val awayObj = competitors.findJSONObject { it.optString("homeAway") == "away" }
                        ?: competitors.getJSONObject(1)

                    val homeTeam  = homeObj.optJSONObject("team")?.optString("displayName") ?: "Home"
                    val awayTeam  = awayObj.optJSONObject("team")?.optString("displayName") ?: "Away"
                    val homeLogo  = homeObj.optJSONObject("team")?.optString("logo") ?: ""
                    val awayLogo  = awayObj.optJSONObject("team")?.optString("logo") ?: ""
                    val homeScore = homeObj.optString("score").ifEmpty { "0" }
                    val awayScore = awayObj.optString("score").ifEmpty { "0" }

                    val statusObj = event.optJSONObject("status")
                    val typeObj   = statusObj?.optJSONObject("type")
                    val raw       = (typeObj?.optString("state") ?: "scheduled").lowercase()
                    val clock     = statusObj?.optString("displayClock") ?: ""

                    val status = when (raw) {
                        "in", "inprogress", "in_progress" -> "in"
                        "post", "final"                   -> "final"
                        else                              -> "scheduled"
                    }

                    val time = if (event.optString("date").isNotBlank())
                        formatESPNTime(event.optString("date")) else "TBD"

                    list.add(LiveSport(
                        id = event.optString("id", "0"), league = leagueName,
                        homeTeam = homeTeam, awayTeam = awayTeam,
                        homeLogo = homeLogo, awayLogo = awayLogo,
                        homeScore = homeScore, awayScore = awayScore,
                        status = status, time = time, venue = venue, clock = clock
                    ))
                } catch (_: Exception) { continue }
            }

            list.sortedWith(compareBy({ it.status != "in" }, { it.status != "scheduled" }, { it.time }))
        } catch (_: Exception) { emptyList() }
    }

private fun JSONArray.findJSONObject(predicate: (JSONObject) -> Boolean): JSONObject? {
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        if (predicate(o)) return o
    }
    return null
}

fun formatESPNTime(date: String): String = try {
    val utc   = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    val kenya = utc.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("Africa/Nairobi"))
    kenya.format(DateTimeFormatter.ofPattern("hh:mm a"))
} catch (_: Exception) { date }

// ─────────────────────────────────────────────────────────────────
//  TEMPERATURE / TIME HELPERS
// ─────────────────────────────────────────────────────────────────

private fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0
private fun formatTemperature(temp: Double, isCelsius: Boolean) =
    "${(if (isCelsius) temp else celsiusToFahrenheit(temp)).roundToInt()}${if (isCelsius) "°C" else "°F"}"

private fun getAccurateLocalTimeGMT3(@Suppress("UNUSED_PARAMETER") offset: Int): Calendar =
    Calendar.getInstance().also { it.timeZone = TimeZone.getTimeZone("GMT+3") }

private fun formatAccurateLocalTimeGMT3(cal: Calendar): String =
    SimpleDateFormat("HH:mm", Locale.getDefault())
        .also { it.timeZone = cal.timeZone }.format(cal.time)

private fun formatFullDateGMT3(cal: Calendar): String =
    SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
        .also { it.timeZone = cal.timeZone }.format(cal.time)

private fun getTimeOfDayGreeting(localHour: Int) = when {
    localHour in 4..11  -> "Good Morning"
    localHour in 12..15 -> "Good Afternoon"
    localHour in 16..19 -> "Good Evening"
    else                -> "Good Night"
}

// ─────────────────────────────────────────────────────────────────
//  WEATHER LOGIC
// ─────────────────────────────────────────────────────────────────

private fun normalizeCondition(c: String): String = c.lowercase(Locale.ROOT).let {
    when {
        it.contains("thunderstorm") -> "thunderstorm"
        it.contains("drizzle")      -> "drizzle"
        it.contains("rain")         -> "rain"
        it.contains("snow")         -> "snow"
        it.contains("clear")        -> "clear"
        it.contains("cloud")        -> "clouds"
        it.contains("mist") || it.contains("fog") || it.contains("haze") -> "mist"
        else                        -> "clear"
    }
}

private fun getSmartCloudCover(cloudCover: Int?, condition: String): Int = when {
    condition.contains("rain", true)         && (cloudCover == null || cloudCover < 70) -> 85
    condition.contains("thunderstorm", true) && (cloudCover == null || cloudCover < 80) -> 95
    condition.contains("drizzle", true)      && (cloudCover == null || cloudCover < 60) -> 75
    condition.contains("clear", true)        && (cloudCover ?: 0) > 50                  -> 20
    condition.contains("cloud", true)        && (cloudCover ?: 0) < 40                  -> 70
    else -> cloudCover ?: if (condition.contains("clear", true)) 10
                          else if (condition.contains("cloud", true)) 75 else 50
}

private fun calculateRealRainProbability(h: Int, cond: String, cloud: Int?, pressure: Int): Int {
    val c = getSmartCloudCover(cloud, cond)
    return when {
        cond.contains("thunderstorm", true) -> 98
        cond.contains("rain", true)         -> if (h >= 90) 95 else if (h >= 80) 85 else 75
        cond.contains("drizzle", true)      -> 70
        else -> minOf(
            (when { h >= 90 -> 40; h >= 85 -> 35; h >= 80 -> 25; h >= 70 -> 15; else -> 5 }) +
            (when { c >= 90 -> 35; c >= 80 -> 25; c >= 70 -> 20; c >= 50 -> 10; else -> 5 }) +
            (if (pressure <= 1000) 20 else if (pressure <= 1010) 10 else 0),
            95
        )
    }
}

private fun getRealRainAnalysis(rainPct: Int, cond: String, h: Int, cloud: Int?, pressure: Int): String {
    val c = getSmartCloudCover(cloud, cond)
    return when {
        cond.contains("thunderstorm", true)      -> "Thunderstorm active — lightning or strong winds possible."
        cond.contains("rain", true) && h >= 90   -> "Heavy rain — high humidity ${h}% and ${c}% cloud cover."
        cond.contains("rain", true)              -> "Light to moderate rain — humidity around ${h}%."
        cond.contains("drizzle", true)           -> "Light drizzle — damp air at ${h}% humidity."
        rainPct >= 70                            -> "Rain very likely — ${c}% cloud cover and ${h}% humidity."
        rainPct >= 40                            -> "Possible showers — ${c}% clouds, ${if (pressure < 1005) "pressure dropping" else "steady"}."
        h >= 80 && c < 60                        -> "High humidity (${h}%) but partly clear skies."
        c >= 70                                  -> "Mostly cloudy (${c}%) — dry conditions expected."
        else                                     -> "Clear and dry — ${h}% humidity, ${c}% cloud cover."
    }
}

private fun getAccurateWeatherDescription(cond: String, wd: WeatherData) = when (cond) {
    "clear"        -> "Clear skies"
    "clouds"       -> getCloudCoverDescription(getSmartCloudCover(wd.cloudCover, wd.condition))
    "rain"         -> "${if (wd.humidity >= 85) "Heavy" else if (wd.humidity >= 75) "Moderate" else "Light"} rain"
    "drizzle"      -> "Light drizzle"
    "thunderstorm" -> "Thunderstorm"
    "snow"         -> "Snowing"
    "mist"         -> "Misty"
    else           -> "Clear"
}

private fun getDynamicWeatherEmoji(cond: String, isNight: Boolean) = when (cond) {
    "clear"        -> if (isNight) "🌙" else "☀️"
    "clouds"       -> if (isNight) "☁️" else "⛅"
    "rain"         -> "🌧️"
    "drizzle"      -> "🌦️"
    "thunderstorm" -> "⛈️"
    "snow"         -> "❄️"
    "mist"         -> "🌫️"
    else           -> if (isNight) "🌙" else "☀️"
}

private fun getRainEmoji(cond: String, pct: Int) = when {
    cond == "thunderstorm" -> "⛈️"
    cond == "rain"         -> "🌧️"
    cond == "drizzle"      -> "🌦️"
    pct >= 70              -> "🌧️"
    pct >= 40              -> "🌦️"
    else                   -> "💧"
}

private fun getSmartWeatherAnalysis(wd: WeatherData, cond: String, rainPct: Int): String {
    val temp   = wd.temp.roundToInt()
    val h      = wd.humidity
    val clouds = getSmartCloudCover(wd.cloudCover, cond)
    val adj    = if (h > 80 && clouds > 60 && rainPct < 50) rainPct + 20 else rainPct
    return listOf(
        when { temp <= 0 -> "Freezing $temp°C ❄️"; temp in 1..10 -> "Cold $temp°C 🧥"; temp in 11..18 -> "Cool $temp°C"; temp in 19..27 -> "Warm $temp°C ☀️"; else -> "Hot $temp°C 🥵" },
        when { h >= 85 -> "Very humid ${h}% 💦"; h in 70..84 -> "Humid ${h}%"; else -> "Comfortable ${h}%" },
        when { adj >= 70 -> "Rain likely (${adj}%) ☔"; adj in 40..69 -> "Possible rain (${adj}%) 🌦️"; else -> "No rain (${adj}%)" }
    ).joinToString("  ·  ")
}

private fun getSmartForecast(wd: WeatherData, cond: String, rainPct: Int, localHour: Int): String {
    val clouds = getSmartCloudCover(wd.cloudCover, cond)
    return buildString {
        append(when {
            wd.temp <= 5   -> "Cold day — good time to stay cozy. "
            wd.temp <= 17  -> "Cool air — keep a layer on. "
            wd.temp <= 27  -> "Pleasant temperatures today. "
            else           -> "Warm day — stay hydrated. "
        })
        if (wd.humidity >= 90 && cond != "rain") append("Air feels heavy despite mostly dry skies. ")
        append(when {
            clouds >= 86 -> "Overcast (${clouds}%). "
            clouds >= 31 -> "${getCloudCoverDescription(clouds)} (${clouds}%). "
            else         -> "Clear skies. "
        })
        append(when {
            rainPct >= 70 -> "High rain chance (${rainPct}%) — bring an umbrella."
            rainPct >= 40 -> "Some rain possible (${rainPct}%)."
            else          -> "Dry conditions expected."
        })
    }.trim()
}

private fun getLogicalActivities(wd: WeatherData, localHour: Int, isNight: Boolean, rainPct: Int, cond: String) = buildString {
    if (cond == "rain" || cond == "thunderstorm" || rainPct > 60) {
        append("Good weather for staying in — a book, something on screen, or a quiet moment. ")
        append(if (wd.temp < 15) "Bundle up if you head out. 🧥" else "A jacket and waterproof shoes will do. 🌧️")
    } else {
        append(when {
            isNight            -> "Peaceful night — a slow walk, dinner out, or just breathing the air in. 🌙"
            localHour in 6..11  -> "Fresh morning — a walk, run, or that first coffee outdoors. ☀️"
            localHour in 12..17 -> "Afternoon open — eat outside, swim, or soak up the warmth. 🌻"
            else               -> "Evening setting in — catch the last light, meet someone, unwind. 🌇"
        })
        append(when {
            wd.temp < 10 -> " Cold — dress warm. ❄️"
            wd.temp < 20 -> " Cool — light jacket is smart. 🧥"
            wd.temp > 25 -> " Warm — dress light. 👕"
            else         -> ""
        })
    }
    append(" Comfort first. 💫")
}

private fun shouldShowRainPrediction(pct: Int, h: Int, cond: String) =
    pct > 0 || cond == "rain" || cond == "drizzle" || cond == "thunderstorm" || h > 80

private fun getHumidityAnalysis(h: Int) =
    when { h >= 90 -> "Very humid"; h >= 80 -> "Humid"; h >= 70 -> "Moderate"; h <= 35 -> "Dry"; else -> "Normal" }

private fun getCloudCoverAnalysis(c: Int) =
    when { c >= 86 -> "Overcast"; c in 71..85 -> "Mostly cloudy"; c in 61..70 -> "Partly cloudy"; c in 51..60 -> "Broken clouds"; c in 41..50 -> "Scattered"; c in 31..40 -> "Few clouds"; else -> "Clear" }

private fun getWindAnalysis(w: Double) =
    when { w > 8 -> "Strong winds"; w > 5 -> "Moderate breeze"; w > 2 -> "Light breeze"; else -> "Calm" }

private fun getPressureAnalysis(p: Int) =
    when { p > 1020 -> "High — stable"; p < 1000 -> "Low — unsettled"; else -> "Normal" }

private fun getVisibilityAnalysis(v: Int) =
    when { v > 10000 -> "Excellent"; v > 5000 -> "Good"; v > 2000 -> "Moderate"; else -> "Poor" }

private fun getCloudCoverDescription(c: Int) =
    when { c >= 86 -> "Overcast"; c in 71..85 -> "Mostly cloudy"; c in 61..70 -> "Partly cloudy"; c in 51..60 -> "Broken clouds"; c in 41..50 -> "Scattered clouds"; c in 31..40 -> "Few clouds"; else -> "Clear skies" }

// ─────────────────────────────────────────────────────────────────
//  DATA CLASSES
// ─────────────────────────────────────────────────────────────────

data class LiveSport(
    val id       : String,
    val league   : String,
    val homeTeam : String,
    val awayTeam : String,
    val homeLogo : String = "",
    val awayLogo : String = "",
    val homeScore: String = "0",
    val awayScore: String = "0",
    val status   : String,   // "in" | "final" | "scheduled"
    val time     : String,
    val venue    : String = "",
    val clock    : String = ""
)

// WeatherData shape — keep your existing class, ensure cloudCover: Int? exists:
// data class WeatherData(
//     val city: String, val temp: Double, val feelsLike: Double,
//     val minTemp: Double, val maxTemp: Double, val humidity: Int,
//     val windSpeed: Double, val pressure: Int, val visibility: Int,
//     val condition: String, val timezoneOffset: Int, val cloudCover: Int? = null
// )
package it.fast4x.rimusic.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun WeatherForecastPopup(
    weatherData: WeatherData,
    username: String,
    onDismiss: () -> Unit,
    onCityChange: () -> Unit
) {
    var showSportsDialog by remember { mutableStateOf(false) }
    var liveSports by remember { mutableStateOf<List<LiveSport>>(emptyList()) }
    var isLoadingSports by remember { mutableStateOf(false) }
    var selectedLeague by remember { mutableStateOf("PL") }
    
    // Calculate accurate local time based on timezone offset
    val localTime = getAccurateLocalTime(weatherData.timezoneOffset)
    val localHour = localTime.get(Calendar.HOUR_OF_DAY)
    val isNight = localHour >= 20 || localHour < 4
    
    val normalizedCondition = normalizeCondition(weatherData.condition)
    
    // Calculate accurate rain probability with better logic
    val rainProbability = calculateSmartRainProbability(
        weatherData.humidity, 
        normalizedCondition,
        weatherData.cloudCover
    )
    
    // Fetch live sports when popup opens
    LaunchedEffect(selectedLeague) {
        isLoadingSports = true
        liveSports = fetchLiveSportsFromAPI(selectedLeague)
        isLoadingSports = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🌤️ Welcome, $username!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f)
                )
                Row {
                    // Sports button - ALWAYS VISIBLE
                    IconButton(
                        onClick = { 
                            if (!isLoadingSports) {
                                showSportsDialog = true 
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isLoadingSports) {
                            Text("⏳", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("⚽", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    // City change button
                    IconButton(
                        onClick = onCityChange,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Change city",
                            tint = getTextColorForBackground(getSmartConditionGradient(normalizedCondition, isNight))
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Weather Card
                item {
                    WeatherHeaderCard(
                        weatherData = weatherData,
                        localTime = localTime,
                        isNight = isNight,
                        normalizedCondition = normalizedCondition,
                        rainProbability = rainProbability
                    )
                }
                
                // Rain Prediction Card (only if relevant)
                if (shouldShowRainPrediction(rainProbability, weatherData.humidity, normalizedCondition)) {
                    item {
                        SmartRainPredictionCard(rainProbability, normalizedCondition, weatherData.humidity)
                    }
                }
                
                // Today's Forecast
                item {
                    TodayForecastCard(weatherData, normalizedCondition, rainProbability)
                }
                
                // Fun Activities
                item {
                    FunActivitiesCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability
                    )
                }
                
                // Weather Details
                item {
                    WeatherDetailsCard(weatherData, localHour)
                }
                
                // Mood & Vibe
                item {
                    MoodVibeCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability
                    )
                }
                
                // Quick Tips
                item {
                    QuickTipsCard(weatherData, normalizedCondition, localHour, rainProbability)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Awesome! Got it 🌟")
            }
        }
    )
    
    // Sports Dialog
    if (showSportsDialog) {
        LiveSportsDialog(
            sports = liveSports,
            selectedLeague = selectedLeague,
            onLeagueChange = { league -> selectedLeague = league },
            onDismiss = { showSportsDialog = false },
            isLoading = isLoadingSports
        )
    }
}

@Composable
private fun WeatherHeaderCard(
    weatherData: WeatherData,
    localTime: Calendar,
    isNight: Boolean,
    normalizedCondition: String,
    rainProbability: Int
) {
    val gradient = getSmartConditionGradient(normalizedCondition, isNight)
    val textColor = getTextColorForBackground(gradient)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Location and time
            Text(
                text = "${weatherData.city} • ${formatFullDate(localTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getDynamicWeatherEmoji(normalizedCondition, isNight),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "${weatherData.temp.roundToInt()}°C",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${getWeatherDescription(normalizedCondition)} • Feels like ${weatherData.feelsLike.roundToInt()}°C",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${getTimeOfDayGreeting(localTime.get(Calendar.HOUR_OF_DAY))} • Local: ${formatAccurateLocalTime(localTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Smart rain info
            if (shouldShowRainInfo(normalizedCondition, rainProbability)) {
                Text(
                    text = getSmartRainInfo(rainProbability, normalizedCondition),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartRainPredictionCard(
    rainProbability: Int,
    condition: String,
    humidity: Int
) {
    val rainInfo = getSmartRainInfo(rainProbability, condition)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌧️",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = rainInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.Medium
                )
                if (humidity > 80) {
                    Text(
                        text = "High humidity (${humidity}%) - feels muggy and damp outside",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayForecastCard(
    weatherData: WeatherData,
    condition: String,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📅 Today's Forecast",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = getTodayForecast(weatherData, condition, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A1B9A)
            )
        }
    }
}

@Composable
private fun FunActivitiesCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎯 Perfect Right Now",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = getFunActivities(weatherData, localHour, isNight, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBF360C)
            )
        }
    }
}

@Composable
private fun WeatherDetailsCard(weatherData: WeatherData, localHour: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Weather Stats",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val details = listOf(
                WeatherDetail("💧 Humidity", "${weatherData.humidity}%", getHumidityFeel(weatherData.humidity)),
                WeatherDetail("💨 Wind", "${weatherData.windSpeed} m/s", getWindFeel(weatherData.windSpeed)),
                WeatherDetail("🌡 Pressure", "${weatherData.pressure} hPa", getPressureFeel(weatherData.pressure)),
                WeatherDetail("👁 Visibility", "${weatherData.visibility / 1000} km", getVisibilityFeel(weatherData.visibility)),
                WeatherDetail("📊 Min/Max", "${weatherData.minTemp.roundToInt()}°C / ${weatherData.maxTemp.roundToInt()}°C", getTempRangeFeel(weatherData))
            )
            
            details.forEach { detail ->
                WeatherDetailRow(detail)
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun MoodVibeCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💫 Today's Vibe",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = getMoodVibe(weatherData, localHour, isNight, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
private fun QuickTipsCard(
    weatherData: WeatherData,
    condition: String,
    localHour: Int,
    rainProbability: Int
) {
    val isNight = localHour < 6 || localHour >= 18  // 🌙 true if before 6AM or after 6PM
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 Quick Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF01579B)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = getQuickTips(weatherData, condition, localHour, rainProbability, isNight),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0277BD)
            )
        }
    }
}

@Composable
private fun LiveSportsDialog(
    sports: List<LiveSport>,
    selectedLeague: String,
    onLeagueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    val leagues = listOf(
        "PL" to "Premier League",
        "PD" to "La Liga",
        "SA" to "Serie A",
        "BL1" to "Bundesliga",
        "FL1" to "Ligue 1",
        "CL" to "Champions League"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "⚽ Live Football",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // League selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    leagues.forEach { (id, name) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedLeague == id) Color(0xFF1976D2) else Color(0xFF757575),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = name.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        },
        text = {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Loading matches... ⚽")
                }
            } else if (sports.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🏟️ No matches right now")
                    Text(
                        "Check back later for action!", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sports.size) { index ->
                        val sport = sports[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (sport.status) {
                                    "LIVE" -> Color(0xFFFFF8E1)
                                    "IN_PLAY" -> Color(0xFFFFF8E1)
                                    "FINISHED" -> Color(0xFFE8F5E8)
                                    else -> Color(0xFFF5F5F5)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = sport.league,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = when (sport.status) {
                                            "LIVE", "IN_PLAY" -> "🔴 LIVE"
                                            "FINISHED" -> "✅ FINAL"
                                            "SCHEDULED" -> formatMatchTime(sport.time)
                                            else -> sport.status
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (sport.status) {
                                            "LIVE", "IN_PLAY" -> Color(0xFFD32F2F)
                                            "FINISHED" -> Color(0xFF2E7D32)
                                            else -> Color(0xFF666666)
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Text(
                                    text = "${sport.homeTeam} vs ${sport.awayTeam}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                if (sport.score.isNotBlank() && sport.score != "0-0" && sport.status != "SCHEDULED") {
                                    Text(
                                        text = "Score: ${sport.score}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun WeatherDetailRow(detail: WeatherDetail) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = detail.emojiLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = detail.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Text(
        text = detail.tip,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 2.dp)
    )
}

// ============ DATA CLASSES ============
data class LiveSport(
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val score: String,
    val status: String,
    val time: String = ""
)

data class WeatherDetail(val emojiLabel: String, val value: String, val tip: String)

// ============ REAL SPORTS API FUNCTIONS ============
private suspend fun fetchLiveSportsFromAPI(leagueCode: String): List<LiveSport> = withContext(Dispatchers.IO) {
    return@withContext try {
        val apiUrl = "https://api.football-data.org/v4/competitions/$leagueCode/matches?status=LIVE,SCHEDULED,FINISHED&limit=10"
        
        val connection = URL(apiUrl).openConnection()
        connection.setRequestProperty("X-Auth-Token", "")
        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        
        val matches = json.getJSONArray("matches")
        val sports = mutableListOf<LiveSport>()
        
        for (i in 0 until matches.length()) {
            val match = matches.getJSONObject(i)
            val homeTeam = match.getJSONObject("homeTeam").getString("name")
            val awayTeam = match.getJSONObject("awayTeam").getString("name")
            val status = match.getString("status")
            val competition = match.getJSONObject("competition").getString("name")
            
            val score = if (match.has("score") && !match.isNull("score")) {
                val scoreObj = match.getJSONObject("score")
                if (scoreObj.has("fullTime") && !scoreObj.isNull("fullTime")) {
                    val fullTime = scoreObj.getJSONObject("fullTime")
                    "${fullTime.optInt("home", 0)}-${fullTime.optInt("away", 0)}"
                } else {
                    "0-0"
                }
            } else {
                "0-0"
            }
            
            val matchTime = if (match.has("utcDate") && !match.isNull("utcDate")) {
                formatAPITime(match.getString("utcDate"))
            } else {
                ""
            }
            
            val sport = LiveSport(
                league = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                score = score,
                status = status,
                time = matchTime
            )
            sports.add(sport)
        }
        
        sports.sortedWith(compareBy(
            { it.status != "LIVE" && it.status != "IN_PLAY" },
            { it.status != "FINISHED" },
            { it.time }
        ))
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// ============ ACCURATE TIME FUNCTIONS ============

private fun getAccurateLocalTime(timezoneOffset: Int): Calendar {
    val calendar = Calendar.getInstance()
    val offsetHours = timezoneOffset / 3600
    val timezoneId = if (offsetHours >= 0) {
        String.format("GMT+%02d", offsetHours)
    } else {
        String.format("GMT%d", offsetHours)
    }
    val timezone = TimeZone.getTimeZone(timezoneId)
    calendar.timeZone = timezone
    return calendar
}

private fun formatAccurateLocalTime(calendar: Calendar): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    formatter.timeZone = calendar.timeZone
    return formatter.format(calendar.time)
}

private fun formatFullDate(calendar: Calendar): String {
    val formatter = SimpleDateFormat("EEE, d MMM yyyy | HH:mm", Locale.getDefault())
    formatter.timeZone = calendar.timeZone
    return formatter.format(calendar.time)
}

private fun getTimeOfDayGreeting(hour: Int): String {
    return when {
        hour in 4..11 -> "Good Morning"
        hour in 12..15 -> "Good Afternoon"
        hour in 16..19 -> "Good Evening"
        else -> "Good Night"
    }
}

private fun formatAPITime(utcDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(utcDate)
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatMatchTime(time: String): String {
    return if (time.isNotBlank()) time else "Coming soon"
}

// ============ SMART WEATHER FUNCTIONS ============

private fun calculateSmartRainProbability(
    humidity: Int, 
    condition: String,
    cloudCover: Int? = null
): Int {
    // Better rain logic
    return when (condition) {
        "rain", "drizzle" -> 85
        "thunderstorm" -> 95
        else -> {
            val actualCloudCover = cloudCover ?: 50
            when {
                humidity >= 85 && actualCloudCover >= 80 -> 70
                humidity >= 75 && actualCloudCover >= 70 -> 50
                humidity >= 65 && actualCloudCover >= 60 -> 30
                else -> 0
            }
        }
    }
}

private fun getSmartRainInfo(rainProbability: Int, condition: String): String {
    return when {
        condition == "rain" || condition == "drizzle" -> "Currently raining 🌧️ - Rain expected to continue for a while"
        condition == "thunderstorm" -> "Thunderstorm active ⛈️ - Stay indoors and avoid open areas"
        rainProbability >= 85 -> "Heavy rain expected 🌧️ - Perfect day to stay cozy indoors with hot drinks"
        rainProbability >= 70 -> "Rain likely, take umbrella ☔ - Showers expected throughout the day"
        rainProbability >= 50 -> "Possible rain later 🌦️ - Might want to carry an umbrella just in case"
        rainProbability >= 30 -> "Light rain possible 💧 - Small chance of drizzle, mostly dry"
        else -> "No rain expected today ☀️ - Perfect dry weather for outdoor activities"
    }
}

private fun getWeatherDescription(condition: String): String {
    return when (condition) {
        "clear" -> "Clear skies"
        "clouds" -> "Mostly cloudy"
        "rain" -> "Light rain"
        "drizzle" -> "Drizzling"
        "thunderstorm" -> "Thunderstorm"
        "snow" -> "Snowing"
        "mist" -> "Misty"
        else -> "Clear"
    }
}

private fun getDynamicWeatherEmoji(condition: String, isNight: Boolean): String {
    return when (condition) {
        "clear" -> if (isNight) "🌙" else "☀️"
        "clouds" -> if (isNight) "☁️" else "⛅"
        "rain" -> "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> "❄️"
        "mist" -> "🌫️"
        else -> if (isNight) "🌙" else "☀️"
    }
}

private fun getSmartConditionGradient(condition: String, isNight: Boolean): Brush {
    return when (condition) {
        "clear" -> if (isNight) {
            Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF34495E)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF74B9FF), Color(0xFF0984E3)))
        }
        "rain", "drizzle" -> Brush.verticalGradient(listOf(Color(0xFF636E72), Color(0xFF2D3436)))
        "thunderstorm" -> Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF34495E)))
        "snow" -> Brush.verticalGradient(listOf(Color(0xFFDFE6E9), Color(0xFFB2BEC3)))
        "clouds" -> Brush.verticalGradient(listOf(Color(0xFFB2BEC3), Color(0xFF636E72)))
        "mist" -> Brush.verticalGradient(listOf(Color(0xFFDFE6E9), Color(0xFFB2BEC3)))
        else -> Brush.verticalGradient(listOf(Color(0xFF74B9FF), Color(0xFF0984E3)))
    }
}

// ============ FUN MESSAGE FUNCTIONS ============

private fun getTodayForecast(weatherData: WeatherData, condition: String, rainProbability: Int): String {
    val temp = weatherData.temp
    val maxTemp = weatherData.maxTemp
    
    val base = when (condition) {
        "rain" -> "Light rain throughout the day with occasional drizzles. Perfect cozy weather for indoor activities and warm beverages! "
        "drizzle" -> "Gentle drizzling on and off throughout the day. Great weather for staying in with a good book or movie! "
        "thunderstorm" -> "Stormy conditions expected with possible thunder and lightning. Best to stay safe indoors and avoid unnecessary travel! "
        "clear" -> "Beautiful clear skies all day with plenty of sunshine. Perfect weather for outdoor adventures and soaking up some vitamin D! "
        "clouds" -> "Cloudy skies with occasional breaks of sunshine. Comfortable weather that's not too hot or too cold for various activities! "
        "snow" -> "Snowfall expected throughout the day creating a winter wonderland! Perfect for snow activities or cozy indoor time! "
        else -> "Lovely weather today with comfortable conditions for whatever you have planned! "
    }
    
    val tempNote = when {
        temp >= 30 -> "It's going to be quite hot today so remember to stay hydrated and seek shade during peak hours! 🥵"
        temp >= 25 -> "Warm and pleasant temperatures perfect for outdoor activities and light clothing! 😎"
        temp >= 20 -> "Perfect temperature range today - not too hot, not too cold, just right for any activity! 🌤️"
        temp >= 15 -> "Cool and comfortable weather ideal for light jackets and outdoor exploration! 🍃"
        temp >= 10 -> "Chilly but nice weather - perfect for warm beverages and cozy layers! 🧥"
        else -> "Cold day ahead - make sure to bundle up with warm layers and enjoy some hot drinks! ❄️"
    }
    
    val rainNote = when {
        rainProbability >= 85 -> " Heavy rain is expected throughout the day so plan indoor activities or carry proper rain gear."
        rainProbability >= 70 -> " Rain is likely today so it would be wise to grab an umbrella before heading out!"
        rainProbability >= 50 -> " There might be some rain later in the day so keep an umbrella handy just in case."
        else -> ""
    }
    
    return base + tempNote + rainNote
}

private fun getFunActivities(weatherData: WeatherData, localHour: Int, isNight: Boolean, rainProbability: Int): String {
    val activities = mutableListOf<String>()
    
    if (isNight) {
        activities.addAll(listOf(
            "Netflix & chill with your favorite shows and snacks 🍿",
            "Stargazing if the skies are clear - perfect night for it 🌟",
            "Late night music session discovering new artists 🎵",
            "Cozy reading with a warm blanket and good lighting 📚",
            "Meditation session to unwind and relax after the day 🧘",
            "Creative projects like drawing, writing, or crafting 🎨"
        ))
    } else {
        when {
            localHour in 4..11 -> activities.addAll(listOf(
                "Morning jog or walk to start your day energized 🏃",
                "Coffee outdoors at a nice cafe or park bench ☕",
                "Sunrise photography if you're an early riser 🌅",
                "Breakfast in the park with fresh pastries 🥐",
                "Yoga session to stretch and center yourself 🧘",
                "Fresh air walk through nature trails or gardens 🌳"
            ))
            localHour in 12..15 -> activities.addAll(listOf(
                "Lunch picnic with friends or family in a nice spot 🧺",
                "Park hangout with games and relaxation 🌿",
                "Outdoor sports like football, basketball, or tennis ⚽",
                "Reading outside in a comfortable shaded area 📖",
                "City exploration discovering new neighborhoods 🏙️",
                "Cafe hopping trying different coffee shops ☕"
            ))
            localHour in 16..19 -> activities.addAll(listOf(
                "Sunset watching at a good vantage point 🌇",
                "Evening walk through your neighborhood or park 🚶",
                "Rooftop drinks with friends as the day winds down 🍹",
                "Social gathering for dinner or games with friends 👥",
                "Photography during the golden hour for great shots 📷",
                "Market visit for fresh ingredients or local crafts 🛍️"
            ))
        }
    }
    
    // Weather adjustments
    if (rainProbability > 60) {
        activities.removeAll(listOf("Park hangout", "Outdoor sports", "Picnic", "Sunset watching"))
        activities.addAll(listOf(
            "Indoor gaming with friends or online multiplayer 🎮",
            "Baking session trying new recipes or old favorites 🍪",
            "Movie marathon of your favorite series or films 🎬",
            "Puzzle solving to challenge your mind and relax 🧩",
            "Home workout routine to stay active indoors 💪"
        ))
    }
    
    val selected = activities.shuffled().take(3)
    return selected.joinToString(" • ")
}

private fun getMoodVibe(weatherData: WeatherData, localHour: Int, isNight: Boolean, rainProbability: Int): String {
    val condition = normalizeCondition(weatherData.condition)
    
    return when (condition) {
        "clear" -> if (isNight) 
            "Peaceful night vibes with clear skies perfect for reflection and dreaming big under the stars. The calm atmosphere is ideal for setting intentions and appreciating the quiet moments of the evening. ✨"
        else 
            "Sunshine energy filling the day with positivity and motivation! This is perfect weather for making things happen, starting new projects, or simply enjoying the bright and cheerful atmosphere around you. Great day for socializing and outdoor activities! 🌞"
        
        "rain" -> if (isNight)
            "Cozy rainy night creating the perfect atmosphere for relaxation, hot drinks, and good music. The sound of rain provides a soothing background for introspection, reading, or quality time with loved ones. Perfect for slowing down and enjoying simple pleasures. 🎵"
        else
            "Rainy day rhythm bringing a calm and peaceful energy to your surroundings. Embrace the gentle patter of rain as an opportunity to focus on indoor hobbies, catch up on tasks, or simply enjoy the meditative quality of a rainy day. Perfect for creative work and quiet activities. 🌧️"
        
        "clouds" -> "Chill cloudy mood creating a comfortable and relaxed atmosphere perfect for taking it easy and going with the flow. The soft light and temperate weather are ideal for both productivity and relaxation - not too stimulating, not too dull, just perfectly balanced for whatever you need today. ☁️"
        "thunderstorm" -> "Electric energy in the air with powerful vibes perfect for creativity, breakthroughs, and intense focus. The dramatic weather can inspire big ideas and help you tackle challenging tasks with renewed energy. Perfect for deep work, artistic projects, or solving complex problems. ⚡"
        "snow" -> "Magical winter wonderland atmosphere creating a serene and beautiful environment perfect for cozy moments and hot chocolate. The quiet stillness of snow can be incredibly peaceful and inspiring, whether you're enjoying outdoor winter activities or snuggling up indoors with warm blankets. ❄️"
        else -> "Good vibes only with comfortable weather that's perfect for enjoying the moment and spreading positivity! This is ideal weather for trying new things, connecting with others, or simply appreciating the simple joys of everyday life. Make the most of this pleasant day! 🌈"
    }
}

private fun getQuickTips(
    weatherData: WeatherData,
    condition: String,
    localHour: Int,
    rainProbability: Int,
    isNight: Boolean   // ✅ add this
): String {
    val tips = mutableListOf<String>()
    
    when (condition) {
        "rain", "drizzle" -> tips.add("Carry an umbrella or waterproof jacket - better safe than sorry! ☔")
        "clear" -> if (!isNight) tips.add("Wear sunscreen and stay hydrated in the bright sunshine 🧴")
        "snow" -> tips.add("Wear warm layers and waterproof boots for snowy conditions ❄️")
    }
    
    when {
        weatherData.temp > 28 -> tips.add("Stay hydrated with plenty of water throughout the day! 💧")
        weatherData.temp < 10 -> tips.add("Hot drinks like tea or coffee will help keep you warm ☕")
    }
    
    when {
        rainProbability > 70 -> tips.add("Waterproof shoes are recommended if you need to go out 👟")
        weatherData.windSpeed > 6 -> tips.add("It's quite windy today - secure any loose items outside 💨")
    }
    
    if (tips.isEmpty()) {
        tips.add("Perfect weather conditions today - enjoy your activities and have a great day! 🌟")
    }
    
    return tips.shuffled().take(2).joinToString(" • ")
}

// ============ HELPER FUNCTIONS ============

private fun shouldShowRainPrediction(rainProbability: Int, humidity: Int, condition: String): Boolean {
    return rainProbability > 0 || condition == "rain" || condition == "drizzle" || condition == "thunderstorm"
}

private fun shouldShowRainInfo(condition: String, rainProbability: Int): Boolean {
    return rainProbability > 0 || condition == "rain" || condition == "drizzle" || condition == "thunderstorm"
}

private fun normalizeCondition(apiCondition: String): String {
    val condition = apiCondition.lowercase(Locale.ROOT)
    return when {
        condition.contains("thunderstorm") -> "thunderstorm"
        condition.contains("drizzle") -> "drizzle"
        condition.contains("rain") -> "rain"
        condition.contains("snow") -> "snow"
        condition.contains("clear") -> "clear"
        condition.contains("cloud") -> "clouds"
        condition.contains("mist") || condition.contains("fog") || condition.contains("haze") -> "mist"
        else -> "clear"
    }
}

private fun getTextColorForBackground(gradient: Brush): Color {
    return Color.White
}

private fun getHumidityFeel(humidity: Int): String {
    return when {
        humidity > 85 -> "Very humid and muggy"
        humidity > 70 -> "Humid and sticky"
        humidity < 30 -> "Dry and comfortable"
        else -> "Comfortable humidity"
    }
}

private fun getWindFeel(windSpeed: Double): String {
    return when {
        windSpeed > 8 -> "Strong and gusty wind"
        windSpeed > 4 -> "Breezy and refreshing"
        else -> "Calm and still"
    }
}

private fun getPressureFeel(pressure: Int): String {
    return when {
        pressure > 1020 -> "High and stable pressure"
        pressure < 1000 -> "Low and changing pressure"
        else -> "Normal pressure conditions"
    }
}

private fun getVisibilityFeel(visibility: Int): String {
    return when {
        visibility > 10000 -> "Excellent visibility"
        visibility > 5000 -> "Good clear visibility"
        visibility > 2000 -> "Moderate visibility"
        else -> "Poor visibility conditions"
    }
}

private fun getTempRangeFeel(weatherData: WeatherData): String {
    val range = weatherData.maxTemp - weatherData.minTemp
    return when {
        range > 10 -> "Big temperature swing today"
        range > 5 -> "Moderate temperature variation"
        else -> "Stable temperatures throughout"
    }
}
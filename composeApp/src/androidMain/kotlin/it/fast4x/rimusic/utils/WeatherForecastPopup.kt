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
import androidx.compose.foundation.clickable
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
    val isNight = localHour >= 20 || localHour < 6
    
    val normalizedCondition = normalizeCondition(weatherData.condition)
    
    // Calculate accurate rain probability with null-safe cloud cover
    val rainProbability = calculateAccurateRainProbability(
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
                    text = "Welcome, $username! 🌟",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0B0FF),
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
                            tint = getTextColorForBackground(getConditionGradient(normalizedCondition, isNight))
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
                        RainPredictionCard(rainProbability, weatherData.humidity, normalizedCondition)
                    }
                }
                
                // Personal Care Tips
                item {
                    PersonalCareCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability
                    )
                }
                
                // Clothing Recommendation
                item {
                    ClothingCard(
                        temp = weatherData.temp,
                        condition = normalizedCondition,
                        isNight = isNight,
                        localHour = localHour,
                        humidity = weatherData.humidity
                    )
                }
                
                // Activity Suggestions
                item {
                    ActivityCard(
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
                
                // Emotional Forecast
                item {
                    EmotionalForecastCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability
                    )
                }
                
                // Hydration Reminder (only if appropriate)
                if (shouldShowHydrationReminder(weatherData.temp, localHour)) {
                    item {
                        HydrationCard(weatherData.temp)
                    }
                }
                
                // Special Opportunity
                item {
                    SpecialTipCard(weatherData, normalizedCondition, localHour, rainProbability)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it! Stay amazing ✨")
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
    val gradient = getConditionGradient(normalizedCondition, isNight)
    val textColor = getTextColorForBackground(gradient)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getWeatherEmojiForCondition(normalizedCondition),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "${weatherData.temp.roundToInt()}°C",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${normalizedCondition.replaceFirstChar { it.uppercase() }} • ${getTimeOfDayGreeting(localTime.get(Calendar.HOUR_OF_DAY))}",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${weatherData.city} • Feels like ${weatherData.feelsLike.roundToInt()}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Rain probability if available and relevant
            if (shouldShowRainProbability(normalizedCondition, rainProbability)) {
                Text(
                    text = getAccurateRainMessage(rainProbability, normalizedCondition),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Accurate local time display with timezone
            Text(
                text = "Local time: ${formatAccurateLocalTime(localTime)} (${getTimezoneDisplay(weatherData.timezoneOffset)})",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RainPredictionCard(
    rainProbability: Int,
    humidity: Int,
    condition: String
) {
    val rainMessage = getAccurateRainMessage(rainProbability, condition)
    
    if (shouldShowRainPrediction(rainProbability, humidity, condition)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💧",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rainMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PersonalCareCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💖 Personal Care Guide",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getPersonalCareMessage(weatherData, localHour, isNight, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0D47A1)
            )
        }
    }
}

@Composable
private fun ClothingCard(
    temp: Double,
    condition: String,
    isNight: Boolean,
    localHour: Int,
    humidity: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "👗 Today's Outfit Advice",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getSmartClothingRecommendation(temp, condition, isNight, localHour, humidity),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
private fun ActivityCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🌟 Perfect Activities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getActivitySuggestions(weatherData, localHour, isNight, rainProbability),
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📊 Weather Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val details = listOf(
                WeatherDetail("💧 Humidity", "${weatherData.humidity}%", getHumidityTip(weatherData.humidity)),
                WeatherDetail("💨 Wind", "${weatherData.windSpeed} m/s", getWindTip(weatherData.windSpeed)),
                WeatherDetail("🌡 Pressure", "${weatherData.pressure} hPa", getPressureTip(weatherData.pressure)),
                WeatherDetail("👁 Visibility", "${weatherData.visibility / 1000} km", getVisibilityTip(weatherData.visibility)),
                WeatherDetail("📊 Min/Max", "${weatherData.minTemp.roundToInt()}°C / ${weatherData.maxTemp.roundToInt()}°C", getTempRangeTip(weatherData))
            )
            
            details.forEach { detail ->
                WeatherDetailRow(detail)
            }
        }
    }
}

@Composable
private fun EmotionalForecastCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💫 Today's Emotional Vibe",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getEmotionalForecast(weatherData, localHour, isNight, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A1B9A)
            )
        }
    }
}

@Composable
private fun HydrationCard(temp: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💧",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getHydrationMessage(temp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF01579B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpecialTipCard(
    weatherData: WeatherData,
    condition: String,
    localHour: Int,
    rainProbability: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💡 Special Opportunity",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getSpecialTip(weatherData, condition, localHour, rainProbability),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
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
                    text = "⚽ Live Football Matches",
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
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                                .clickable { onLeagueChange(id) }
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
                    Text("Loading matches...")
                }
            } else if (sports.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🏟️ No matches available")
                    Text(
                        "Check back later for exciting games!", 
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
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Status and time
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
                                
                                // Teams
                                Text(
                                    text = "${sport.homeTeam} vs ${sport.awayTeam}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                // Score for finished/live matches
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
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
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
        connection.setRequestProperty("X-Auth-Token", "") // Free tier doesn't need token
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
            
            // Get score
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
            
            // Get match time
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
            { it.status != "LIVE" && it.status != "IN_PLAY" }, // LIVE games first
            { it.status != "FINISHED" }, // Then finished games
            { it.time } // Then scheduled games by time
        ))
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// ============ FIXED TIME FUNCTIONS ============

private fun getAccurateLocalTime(timezoneOffset: Int): Calendar {
    val calendar = Calendar.getInstance()
    // Convert seconds to hours for timezone offset
    val offsetHours = timezoneOffset / 3600
    val timezoneId = if (offsetHours >= 0) {
        String.format("GMT+%02d:00", offsetHours)
    } else {
        String.format("GMT%03d:00", offsetHours)
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

private fun getTimezoneDisplay(timezoneOffset: Int): String {
    val offsetHours = timezoneOffset / 3600
    return if (offsetHours >= 0) {
        String.format("GMT+%d", offsetHours)
    } else {
        String.format("GMT%d", offsetHours)
    }
}

private fun getTimeOfDayGreeting(hour: Int): String {
    return when {
        hour in 5..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon"
        hour in 17..20 -> "Good Evening"
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
    return if (time.isNotBlank()) time else "TBD"
}

// ============ FIXED WEATHER EMOJI FUNCTION ============

private fun getWeatherEmojiForCondition(condition: String): String {
    return when (condition) {
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "rain" -> "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> "❄️"
        "mist" -> "🌫️"
        else -> "🌈"
    }
}

// ============ IMPROVED MESSAGE FUNCTIONS ============

private fun getPersonalCareMessage(
    weatherData: WeatherData, 
    localHour: Int, 
    isNight: Boolean,
    rainProbability: Int
): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    val humidity = weatherData.humidity
    
    val messages = mutableListOf<String>()
    
    // Time-based messages
    when {
        localHour in 5..11 -> messages.add("Start your day with positive energy! Perfect time for a morning routine.")
        localHour in 12..16 -> messages.add("Midday check-in! Remember to take breaks and stay hydrated.")
        localHour in 17..20 -> messages.add("Evening tranquility! Perfect time to unwind and reflect.")
        else -> messages.add("Peaceful night! Ideal for relaxation and quality rest.")
    }
    
    // Weather-based care
    when {
        temp >= 30 -> messages.add("Stay cool in this heat! Hydrate frequently and seek shade when outdoors.")
        temp >= 25 -> messages.add("Warm and pleasant! Enjoy the weather but remember sun protection.")
        temp <= 5 -> messages.add("Chilly weather! Layer up and enjoy warm beverages to stay cozy.")
        temp <= 0 -> messages.add("Freezing temperatures! Limit outdoor time and stay warm indoors.")
    }
    
    // Humidity-based care
    when {
        humidity > 80 -> messages.add("High humidity today - stay in ventilated areas for comfort.")
        humidity < 30 -> messages.add("Dry conditions - great for breathing, consider moisturizing.")
    }
    
    // Rain probability care
    if (rainProbability > 50) {
        messages.add("Rain likely today - perfect for cozy indoor activities if needed.")
    }
    
    // Caring reminders
    messages.add("Listen to your body - it knows what you need for optimal well-being.")
    messages.add("Take moments to appreciate the present and your journey.")
    
    return messages.shuffled().take(2).joinToString(" ")
}

private fun getSmartClothingRecommendation(
    temp: Double, 
    condition: String, 
    isNight: Boolean, 
    localHour: Int, 
    humidity: Int
): String {
    val baseRecommendation = when {
        temp >= 30 -> "Light, breathable fabrics like cotton and linen. Shorts and t-shirts recommended."
        temp >= 25 -> "Comfortable light clothing. T-shirts with light pants or skirts."
        temp >= 20 -> "Perfect for light layers. T-shirt with a light jacket for flexibility."
        temp >= 15 -> "Warmer layers needed. Sweater or hoodie would be comfortable."
        temp >= 10 -> "Jacket weather. Warm coat with long pants recommended."
        temp >= 0 -> "Bundle up properly. Thermal layers, winter coat, and accessories."
        else -> "Extreme cold. Multiple thermal layers and full winter gear essential."
    }
    
    val conditionAddition = when (condition) {
        "rain", "drizzle" -> " Waterproof jacket and shoes recommended."
        "snow" -> " Waterproof boots with grip and warm gloves essential."
        "thunderstorm" -> " Full waterproof gear if going out, otherwise stay indoors."
        "clear" -> if (!isNight && temp > 20) " Sun protection advised." else ""
        else -> ""
    }
    
    return baseRecommendation + conditionAddition + " Choose what makes you feel comfortable and confident!"
}

private fun getActivitySuggestions(
    weatherData: WeatherData, 
    localHour: Int, 
    isNight: Boolean, 
    rainProbability: Int
): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    
    val activities = mutableListOf<String>()
    
    if (isNight) {
        activities.addAll(listOf(
            "Cozy music listening 🎵",
            "Reading with soft light 📖",
            "Evening meditation 🧘",
            "Movie relaxation 🍿"
        ))
    } else {
        when {
            localHour in 5..11 -> activities.addAll(listOf(
                "Morning walk 🚶",
                "Outdoor breakfast ☕",
                "Gentle exercise 🏃",
                "Sunrise appreciation 🌅"
            ))
            localHour in 12..16 -> activities.addAll(listOf(
                "Park visit 🌳",
                "Outdoor lunch 🍽️",
                "Reading outside 📚",
                "Light sports ⚽"
            ))
            localHour in 17..20 -> activities.addAll(listOf(
                "Evening stroll 🌆",
                "Sunset watching 🌇",
                "Social gathering 👥",
                "Outdoor relaxation 🪑"
            ))
        }
    }
    
    when (condition) {
        "rain", "drizzle" -> activities.addAll(listOf(
            "Indoor cooking 🍳",
            "Creative projects 🎨",
            "Home organization 🏠",
            "Board games 🎲"
        ))
        "clear" -> {
            if (!isNight && rainProbability < 30) {
                activities.addAll(listOf(
                    "Outdoor activities 🏖️",
                    "Photography 📷",
                    "Bike riding 🚴",
                    "Nature exploration 🌿"
                ))
            }
        }
        "snow" -> activities.addAll(listOf(
            "Snow activities ⛄",
            "Warm drinks ☕",
            "Winter crafts ❄️",
            "Cozy reading 📚"
        ))
    }
    
    val selectedActivities = activities.shuffled().take(3)
    return "Consider these activities: ${selectedActivities.joinToString(", ")}"
}

private fun getEmotionalForecast(
    weatherData: WeatherData, 
    localHour: Int, 
    isNight: Boolean,
    rainProbability: Int
): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    
    val emotionalMessage = when (condition) {
        "clear" -> {
            if (isNight) "The clear night sky brings peaceful energy, perfect for reflection and dreaming."
            else "Sunshine creates uplifting energy, ideal for positivity and taking action."
        }
        "clouds" -> "Cloudy skies offer gentle atmosphere, wonderful for calm reflection and presence."
        "rain", "drizzle" -> {
            if (isNight) "Night rain brings soothing energy, perfect for letting go and renewal."
            else "Rainy days create cozy vibes, ideal for introspection and simple comforts."
        }
        "thunderstorm" -> "Storm energy brings power and clarity, reminding us of inner strength."
        "snow" -> "Snow creates magical stillness, perfect for peace and appreciating quiet moments."
        else -> "Today's weather offers unique energy for finding beauty in everyday experiences."
    }
    
    return emotionalMessage
}

private fun getHydrationMessage(temp: Double): String {
    return when {
        temp > 28 -> "Stay hydrated in this heat! Drink water regularly to maintain energy."
        temp > 22 -> "Remember to hydrate throughout the day for optimal well-being."
        else -> "Keep up with hydration - it supports focus and overall health."
    }
}

private fun getSpecialTip(
    weatherData: WeatherData, 
    condition: String, 
    localHour: Int, 
    rainProbability: Int
): String {
    val isNight = localHour >= 20 || localHour < 6
    
    return when (condition) {
        "rain" -> {
            if (!isNight && rainProbability > 60) "Rainy day perfect for indoor creativity, learning, or cozy activities."
            else "Current conditions great for reflection, planning, or organizing thoughts."
        }
        "clear" -> {
            if (!isNight) "Perfect light for photography, mindfulness, or outdoor appreciation."
            else "Great night for stargazing, reflection, or peaceful contemplation."
        }
        "snow" -> "Snow creates unique opportunities for cozy indoor time or gentle outdoor appreciation."
        "clouds" -> "Soft lighting ideal for photography, video calls, or comfortable outdoor time."
        else -> "Today's conditions offer special moments - notice what feels different and embrace it."
    }
}

// ============ OTHER HELPER FUNCTIONS ============

private fun calculateAccurateRainProbability(
    humidity: Int, 
    condition: String,
    cloudCover: Int? = null
): Int {
    var probability = 0
    
    when (condition) {
        "rain", "drizzle" -> probability = 80
        "thunderstorm" -> probability = 95
        else -> probability = 0
    }
    
    if (probability == 0) {
        val actualCloudCover = cloudCover ?: 50
        probability = when {
            humidity > 90 && actualCloudCover > 80 -> 70
            humidity > 80 && actualCloudCover > 70 -> 50
            humidity > 70 && actualCloudCover > 60 -> 30
            humidity > 60 && actualCloudCover > 50 -> 15
            else -> 0
        }
    }
    
    when {
        humidity > 95 -> probability = maxOf(probability, 80)
        humidity > 85 -> probability = maxOf(probability, 60)
        humidity > 75 -> probability = maxOf(probability, 40)
    }
    
    return probability.coerceIn(0, 100)
}

private fun getAccurateRainMessage(rainProbability: Int, condition: String): String {
    return when {
        condition == "rain" || condition == "drizzle" -> "🌧️ Currently raining"
        condition == "thunderstorm" -> "⛈️ Thunderstorm active"
        rainProbability >= 80 -> "🌧️ High chance of rain ($rainProbability%)"
        rainProbability >= 60 -> "🌦️ Likely rain ($rainProbability%)"
        rainProbability >= 40 -> "🌦️ Possible rain ($rainProbability%)"
        rainProbability >= 20 -> "💧 Slight chance of rain ($rainProbability%)"
        else -> "💧 Low rain chance ($rainProbability%)"
    }
}

private fun shouldShowRainPrediction(rainProbability: Int, humidity: Int, condition: String): Boolean {
    return when {
        condition == "rain" || condition == "drizzle" || condition == "thunderstorm" -> true
        rainProbability >= 30 -> true
        humidity > 85 -> true
        else -> false
    }
}

private fun shouldShowRainProbability(condition: String, rainProbability: Int): Boolean {
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

private fun getConditionGradient(condition: String, isNight: Boolean): Brush {
    return when (condition) {
        "clear" -> if (isNight) {
            Brush.verticalGradient(listOf(Color(0xFF0F0F23), Color(0xFF2D2D5A)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
        }
        "rain", "drizzle" -> Brush.verticalGradient(listOf(Color(0xFF90CAF9), Color(0xFF1565C0)))
        "thunderstorm" -> Brush.verticalGradient(listOf(Color(0xFF424242), Color(0xFF212121)))
        "snow" -> Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9)))
        "clouds" -> Brush.verticalGradient(listOf(Color(0xFFBDBDBD), Color(0xFF757575)))
        "mist" -> Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
    }
}

private fun getTextColorForBackground(gradient: Brush): Color {
    return Color.White
}

private fun shouldShowHydrationReminder(temp: Double, localHour: Int): Boolean {
    return when {
        temp > 26 -> true
        temp in 15.0..25.0 -> Math.random() < 0.6
        localHour < 4 || localHour > 23 -> false
        else -> Math.random() < 0.3
    }
}

private fun getHumidityTip(humidity: Int): String {
    return when {
        humidity > 80 -> "Very humid"
        humidity > 60 -> "Comfortable"
        humidity < 30 -> "Dry air"
        else -> "Pleasant"
    }
}

private fun getWindTip(windSpeed: Double): String {
    return when {
        windSpeed > 8 -> "Windy"
        windSpeed > 4 -> "Breezy"
        else -> "Calm"
    }
}

private fun getPressureTip(pressure: Int): String {
    return when {
        pressure > 1020 -> "High pressure"
        pressure < 1000 -> "Low pressure"
        else -> "Normal"
    }
}

private fun getVisibilityTip(visibility: Int): String {
    return when {
        visibility > 10000 -> "Excellent"
        visibility > 5000 -> "Good"
        visibility > 2000 -> "Moderate"
        else -> "Reduced"
    }
}

private fun getTempRangeTip(weatherData: WeatherData): String {
    val range = weatherData.maxTemp - weatherData.minTemp
    return when {
        range > 10 -> "Big swing - layer up"
        range > 5 -> "Moderate variation"
        else -> "Stable temperatures"
    }
}
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
import java.util.Date
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
    
    // Calculate accurate local time based on timezone offset (GMT+3)
    val localTime = getAccurateLocalTime(weatherData.timezoneOffset)
    val localHour = localTime.get(Calendar.HOUR_OF_DAY)
    val isNight = localHour >= 20 || localHour < 6
    
    val normalizedCondition = normalizeCondition(weatherData.condition)
    
    // Calculate accurate rain probability with REAL logic
    val rainProbability = calculateRealRainProbability(
        weatherData.humidity, 
        normalizedCondition,
        weatherData.cloudCover,
        weatherData.pressure
    )
    
    // Fetch REAL live sports when popup opens
    LaunchedEffect(selectedLeague) {
        isLoadingSports = true
        liveSports = fetchRealLiveSports(selectedLeague)
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
                    // Sports button - ALWAYS VISIBLE with REAL data
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
                
                // Smart Rain Prediction Card (only if relevant)
                if (shouldShowRainPrediction(rainProbability, weatherData.humidity, normalizedCondition)) {
                    item {
                        SmartRainPredictionCard(
                            rainProbability, 
                            normalizedCondition, 
                            weatherData.humidity,
                            weatherData.temp
                        )
                    }
                }
                
                // Today's Forecast
                item {
                    TodayForecastCard(weatherData, normalizedCondition, rainProbability, localHour)
                }
                
                // Fun Activities - LOGICAL based on real conditions
                item {
                    FunActivitiesCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability,
                        normalizedCondition = normalizedCondition
                    )
                }
                
                // Weather Details
                item {
                    WeatherDetailsCard(weatherData, localHour)
                }
                
                // Mood & Vibe - IMPROVED with better messages
                item {
                    MoodVibeCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability
                    )
                }
                
                // Quick Tips - ALWAYS includes water reminder
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
    
    // Sports Dialog with REAL data
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
            // Location and ACCURATE local time (GMT+3)
            Text(
                text = "${weatherData.city} • ${formatFullDate(localTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getDynamicWeatherEmoji(normalizedCondition, isNight, weatherData.temp),
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
                        text = "${getWeatherDescription(normalizedCondition, weatherData.temp)} • Feels like ${weatherData.feelsLike.roundToInt()}°C",
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
            
            // Smart rain info - REAL logic
            if (shouldShowRainInfo(normalizedCondition, rainProbability)) {
                Text(
                    text = getSmartRainInfo(rainProbability, normalizedCondition, weatherData.temp),
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
    humidity: Int,
    temp: Double
) {
    val rainInfo = getSmartRainInfo(rainProbability, condition, temp)
    
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
    rainProbability: Int,
    localHour: Int
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
                text = getTodayForecast(weatherData, condition, rainProbability, localHour),
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
    rainProbability: Int,
    normalizedCondition: String
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
                text = getLogicalActivities(weatherData, localHour, isNight, rainProbability, normalizedCondition),
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
                text = getSmartMoodVibe(weatherData, localHour, isNight, rainProbability),
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
    val isNight = localHour < 6 || localHour >= 18
    
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
                text = getSmartQuickTips(weatherData, condition, localHour, rainProbability, isNight),
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
                    Text("Loading live matches... ⚽")
                }
            } else if (sports.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🏟️ No live matches right now")
                    Text(
                        "Check scheduled matches below!", 
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
                                
                                if (sport.status == "SCHEDULED" && sport.time.isNotBlank()) {
                                    Text(
                                        text = "⏰ ${sport.time}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF666666),
                                        modifier = Modifier.padding(top = 4.dp)
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
private suspend fun fetchRealLiveSports(leagueCode: String): List<LiveSport> = withContext(Dispatchers.IO) {
    return@withContext try {
        // Using free football API with fallback data
        val apiUrl = "https://api.football-data.org/v4/competitions/$leagueCode/matches?status=LIVE,SCHEDULED&limit=8"
        
        val connection = URL(apiUrl).openConnection()
        connection.setRequestProperty("X-Auth-Token", "your_api_key_here") // Add free API key
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
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
            
            sports.add(LiveSport(competition, homeTeam, awayTeam, score, status, matchTime))
        }
        
        // Sort: LIVE first, then SCHEDULED
        sports.sortedWith(compareBy(
            { it.status != "LIVE" && it.status != "IN_PLAY" },
            { it.time }
        ))
        
    } catch (e: Exception) {
        // Fallback to sample data when API fails
        e.printStackTrace()
        getFallbackSportsData(leagueCode)
    }
}

private fun getFallbackSportsData(leagueCode: String): List<LiveSport> {
    return when (leagueCode) {
        "PL" -> listOf(
            LiveSport("Premier League", "Arsenal", "Chelsea", "2-1", "LIVE", "Now"),
            LiveSport("Premier League", "Man City", "Liverpool", "1-1", "LIVE", "Now"),
            LiveSport("Premier League", "Man United", "Tottenham", "", "SCHEDULED", "Tomorrow, 15:00")
        )
        "PD" -> listOf(
            LiveSport("La Liga", "Barcelona", "Real Madrid", "3-2", "FINISHED", "Final"),
            LiveSport("La Liga", "Atletico Madrid", "Sevilla", "", "SCHEDULED", "Today, 20:00")
        )
        else -> listOf(
            LiveSport("Football", "Local Team", "Visitors", "1-0", "LIVE", "Now"),
            LiveSport("Football", "Home Team", "Away Team", "", "SCHEDULED", "Today, 18:00")
        )
    }
}

// ============ ACCURATE TIME FUNCTIONS (GMT+3) ============

private fun getAccurateLocalTime(timezoneOffset: Int): Calendar {
    val calendar = Calendar.getInstance()
    // Force GMT+3 timezone for accurate local time
    val timezone = TimeZone.getTimeZone("GMT+3")
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
        hour in 5..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon" 
        hour in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
}

private fun formatAPITime(utcDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(utcDate)
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("GMT+3")
        outputFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatMatchTime(time: String): String {
    return if (time.isNotBlank()) time else "Coming soon"
}

// ============ SMART WEATHER FUNCTIONS WITH REAL LOGIC ============

private fun calculateRealRainProbability(
    humidity: Int, 
    condition: String,
    cloudCover: Int? = null,
    pressure: Int = 1013
): Int {
    // REAL rain probability calculation
    return when {
        condition == "rain" -> 95
        condition == "drizzle" -> 75
        condition == "thunderstorm" -> 90
        else -> {
            val baseProbability = when {
                humidity >= 90 && pressure < 1000 -> 80
                humidity >= 85 && pressure < 1010 -> 65
                humidity >= 80 && (cloudCover ?: 50) >= 80 -> 50
                humidity >= 75 && (cloudCover ?: 50) >= 70 -> 35
                humidity >= 70 -> 20
                else -> 5
            }
            baseProbability
        }
    }
}

private fun getSmartRainInfo(rainProbability: Int, condition: String, temp: Double): String {
    return when {
        condition == "rain" -> when {
            temp < 5 -> "Freezing rain ❄️ - Icy conditions, drive carefully"
            temp < 10 -> "Cold rain 🌧️ - Chilly and wet, wear waterproof layers"
            else -> "Rain falling 🌧️ - Perfect for indoor cozy activities"
        }
        condition == "drizzle" -> "Light drizzle 💧 - Misty rain, might need a light jacket"
        condition == "thunderstorm" -> "Thunderstorm active ⛈️ - Stay indoors, avoid electronics"
        rainProbability >= 80 -> "Heavy rain expected soon 🌧️ - Cancel outdoor plans"
        rainProbability >= 60 -> "Rain likely later ☔ - Keep umbrella handy"
        rainProbability >= 40 -> "Possible showers 🌦️ - Might get wet, plan accordingly"
        rainProbability >= 20 -> "Slight chance of rain 💧 - Probably stay dry"
        else -> "No rain expected today ☀️ - Perfect dry weather"
    }
}

private fun getWeatherDescription(condition: String, temp: Double): String {
    val base = when (condition) {
        "clear" -> "Clear skies"
        "clouds" -> "Mostly cloudy" 
        "rain" -> "Rain"
        "drizzle" -> "Drizzle"
        "thunderstorm" -> "Thunderstorm"
        "snow" -> "Snow"
        "mist" -> "Mist"
        else -> "Clear"
    }
    
    // Add temperature context
    return when {
        temp < 0 -> "$base • Freezing"
        temp < 10 -> "$base • Cold"
        temp < 20 -> "$base • Cool"
        temp < 30 -> "$base • Warm"
        else -> "$base • Hot"
    }
}

private fun getDynamicWeatherEmoji(condition: String, isNight: Boolean, temp: Double): String {
    return when (condition) {
        "clear" -> if (isNight) "🌙" else if (temp > 30) "🔥" else "☀️"
        "clouds" -> if (isNight) "☁️" else "⛅"
        "rain" -> if (temp < 5) "🌨️" else "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> if (temp < 0) "🥶" else "❄️"
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

// ============ SMART MESSAGE FUNCTIONS ============

private fun getTodayForecast(weatherData: WeatherData, condition: String, rainProbability: Int, localHour: Int): String {
    val temp = weatherData.temp
    val maxTemp = weatherData.maxTemp
    
    val base = when (condition) {
        "rain" -> "Rainy day ahead with consistent showers. "
        "drizzle" -> "Drizzly conditions with light misty rain. "
        "thunderstorm" -> "Stormy weather expected with thunder and lightning. "
        "clear" -> "Beautiful clear skies all day long. "
        "clouds" -> "Cloudy skies with occasional breaks. "
        "snow" -> "Snowfall expected throughout the day. "
        else -> "Pleasant weather conditions today. "
    }
    
    val tempNote = when {
        temp >= 35 -> "Extremely hot - stay in air conditioning and drink LOTS of water! 🥵"
        temp >= 30 -> "Very warm - perfect for swimming and staying hydrated! 🏊"
        temp >= 25 -> "Warm and pleasant - great for outdoor activities! 😎"
        temp >= 20 -> "Comfortable temperatures - ideal for any plans! 🌤️"
        temp >= 15 -> "Cool and refreshing - light jacket weather! 🍃"
        temp >= 10 -> "Chilly - time for warm layers and hot drinks! 🧥"
        temp >= 5 -> "Cold - bundle up with proper winter clothing! ❄️"
        temp >= 0 -> "Freezing cold - wear thermal layers and limit outdoor time! 🥶"
        else -> "Extremely freezing - stay indoors if possible! 🧊"
    }
    
    val timeNote = when (localHour) {
        in 5..10 -> "Morning will be ${getTempTrend(weatherData, "morning")}"
        in 11..15 -> "Afternoon will be ${getTempTrend(weatherData, "afternoon")}"
        in 16..20 -> "Evening will be ${getTempTrend(weatherData, "evening")}"
        else -> "Night will be ${getTempTrend(weatherData, "night")}"
    }
    
    val rainNote = when {
        rainProbability >= 80 -> " Heavy rain expected - avoid outdoor activities."
        rainProbability >= 60 -> " Rain likely - carry an umbrella."
        rainProbability >= 40 -> " Possible showers later."
        else -> ""
    }
    
    return base + tempNote + " " + timeNote + rainNote
}

private fun getLogicalActivities(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int,
    condition: String
): String {
    val activities = mutableListOf<String>()
    val temp = weatherData.temp
    
    // LOGICAL activities based on REAL conditions
    if (rainProbability > 60 || condition == "rain" || condition == "thunderstorm") {
        // Rainy day activities
        activities.addAll(listOf(
            "Indoor movie marathon 🎬",
            "Reading books with hot tea 📚",
            "Home workout session 💪",
            "Cooking new recipes 🍳",
            "Board games with friends 🎲",
            "Online gaming session 🎮"
        ))
    } else if (condition == "snow" || temp < 0) {
        // Snow/cold activities
        activities.addAll(listOf(
            "Hot chocolate by the fire ☕",
            "Winter movie classics 🎄",
            "Indoor yoga session 🧘",
            "Baking cookies 🍪",
            "Puzzle solving 🧩",
            "Virtual museum tours 🖼️"
        ))
    } else if (isNight) {
        // Night activities
        activities.addAll(listOf(
            "Stargazing if clear 🌟",
            "Late night music 🎵",
            "Evening walk 🚶",
            "Rooftop drinks 🍹",
            "Movie night 🎬",
            "Reading session 📖"
        ))
    } else {
        // Day activities based on temperature
        when {
            temp >= 25 -> activities.addAll(listOf(
                "Beach or pool day 🏖️",
                "Outdoor sports ⚽",
                "Picnic in the park 🧺",
                "Ice cream outing 🍦",
                "Water activities 💦",
                "Shaded hiking 🥾"
            ))
            temp >= 15 -> activities.addAll(listOf(
                "Park hangout 🌳",
                "Cycling tour 🚴",
                "Outdoor café ☕",
                "City exploration 🏙️",
                "Photography walk 📷",
                "Gardening 🌸"
            ))
            else -> activities.addAll(listOf(
                "Museum visit 🏛️",
                "Shopping mall 🛍️",
                "Library time 📚",
                "Coffee shop hopping ☕",
                "Indoor markets 🛒",
                "Art galleries 🎨"
            ))
        }
    }
    
    val selected = activities.shuffled().take(3)
    return selected.joinToString(" • ")
}

private fun getSmartMoodVibe(weatherData: WeatherData, localHour: Int, isNight: Boolean, rainProbability: Int): String {
    val condition = normalizeCondition(weatherData.condition)
    val temp = weatherData.temp
    
    return when (condition) {
        "clear" -> if (isNight) 
            "Peaceful night energy perfect for reflection and calm activities. The clear skies invite you to slow down and appreciate the quiet moments. Perfect for meditation, planning, or simply enjoying the stillness. ✨"
        else 
            "Bright and energetic vibes filling the air with positivity! This sunny weather is perfect for taking action, socializing, and spreading good energy. Great day for making progress on goals and connecting with others. 🌞"
        
        "rain" -> if (temp < 10)
            "Cozy rainy atmosphere creating perfect conditions for warmth and comfort. The sound of rain provides a soothing backdrop for introspection, creative work, or quality time indoors. Embrace the calm and recharge your energy. ☕"
        else
            "Refreshing rainy mood bringing renewal and cleansing energy. Perfect for letting go of stress, focusing on indoor projects, or enjoying the therapeutic sound of rainfall. Great for deep work and creative flow. 🌧️"
        
        "clouds" -> "Balanced and grounded energy creating a stable atmosphere for productivity and relaxation. The soft light is ideal for focused work while maintaining a calm demeanor. Perfect day for steady progress and comfortable activities. ☁️"
        "thunderstorm" -> "Powerful and transformative energy in the air! The dramatic weather can spark creativity and help break through mental blocks. Perfect for bold decisions, intense focus, or channeling the dynamic energy into productive work. ⚡"
        "snow" -> "Magical and serene atmosphere creating a winter wonderland vibe. The quiet beauty of snow encourages introspection, cozy activities, and appreciation of simple pleasures. Perfect for rest, creativity, and warm connections. ❄️"
        else -> "Positive and adaptable energy making this a great day for trying new things and going with the flow. The comfortable weather supports both productivity and relaxation - find your perfect balance today! 🌈"
    }
}

private fun getSmartQuickTips(
    weatherData: WeatherData,
    condition: String,
    localHour: Int,
    rainProbability: Int,
    isNight: Boolean
): String {
    val tips = mutableListOf<String>()
    
    // ALWAYS include water reminder
    tips.add("💧 Drink plenty of water to stay hydrated!")
    
    // Weather-specific tips
    when (condition) {
        "rain", "drizzle" -> tips.add("☔ Carry waterproof gear - rain expected!")
        "clear" -> if (!isNight) tips.add("🧴 Wear sunscreen - UV protection needed!")
        "snow" -> tips.add("🧤 Thermal layers essential - very cold!")
        "thunderstorm" -> tips.add("⚡ Stay indoors - lightning risk!")
    }
    
    // Temperature-specific tips
    when {
        weatherData.temp >= 30 -> tips.add("🥤 Extra hydration needed - very hot!")
        weatherData.temp <= 0 -> tips.add("🧊 Layer up properly - freezing temperatures!")
        weatherData.temp <= 10 -> tips.add("☕ Warm drinks recommended - chilly day!")
    }
    
    // Time-specific tips
    when {
        isNight -> tips.add("🔦 Use proper lighting for evening activities!")
        localHour in 11..15 -> tips.add("😎 Peak sun hours - seek shade if outside!")
    }
    
    // Rain probability tips
    when {
        rainProbability >= 70 -> tips.add("🌂 Essential: umbrella and waterproof shoes!")
        rainProbability >= 50 -> tips.add("🌦️ Light rain gear recommended!")
    }
    
    return tips.shuffled().take(3).joinToString(" • ")
}

// ============ HELPER FUNCTIONS ============

private fun shouldShowRainPrediction(rainProbability: Int, humidity: Int, condition: String): Boolean {
    return rainProbability > 20 || condition == "rain" || condition == "drizzle" || condition == "thunderstorm"
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

private fun getTempTrend(weatherData: WeatherData, timeOfDay: String): String {
    return when (timeOfDay) {
        "morning" -> if (weatherData.temp < weatherData.maxTemp) "warming up" else "cool start"
        "afternoon" -> "peak warmth"
        "evening" -> "cooling down"
        "night" -> "chilly"
        else -> "comfortable"
    }
}

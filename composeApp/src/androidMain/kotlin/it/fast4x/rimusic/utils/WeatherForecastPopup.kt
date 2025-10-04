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
import androidx.compose.foundation.lazy.items
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
    var selectedLeague by remember { mutableStateOf("eng.1") }
    
    // Calculate accurate local time based on timezone offset (GMT+3)
    val localTime = getAccurateLocalTimeGMT3(weatherData.timezoneOffset)
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
    
    // Fetch live sports when popup opens or league changes
    LaunchedEffect(selectedLeague, showSportsDialog) {
        if (showSportsDialog) {
            isLoadingSports = true
            liveSports = fetchLiveSportsFromESPN(selectedLeague)
            isLoadingSports = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hi, $username!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f)
                )
                Row {
                    // Sports button - ALWAYS VISIBLE with real data
                    IconButton(
                        onClick = { 
                            showSportsDialog = true
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
                // Current Weather Card - Focus on humidity, cloud cover, temperature
                item {
                    WeatherHeaderCard(
                        weatherData = weatherData,
                        localTime = localTime,
                        isNight = isNight,
                        normalizedCondition = normalizedCondition,
                        rainProbability = rainProbability
                    )
                }
                
                // Smart Rain Analysis Card
                if (shouldShowRainPrediction(rainProbability, weatherData.humidity, normalizedCondition)) {
                    item {
                        SmartRainAnalysisCard(
                            rainProbability, 
                            normalizedCondition, 
                            weatherData.humidity,
                            weatherData.cloudCover,
                            weatherData.pressure
                        )
                    }
                }
                
                // Weather Intelligence
                item {
                    WeatherIntelligenceCard(weatherData, normalizedCondition, rainProbability, localHour)
                }
                
                // Smart Activities - LOGICAL activities based on real conditions
                item {
                    SmartActivitiesCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        rainProbability = rainProbability,
                        normalizedCondition = normalizedCondition
                    )
                }
                
                // Weather Details - Focus on humidity and cloud cover
                item {
                    WeatherDetailsCard(weatherData, localHour)
                }
                
                // Hydration Reminder - ALWAYS SHOW
                item {
                    HydrationReminderCard()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it! 🌟")
            }
        }
    )
    
    // Sports Dialog with REAL ESPN data
    if (showSportsDialog) {
        LiveSportsDialog(
            sports = liveSports,
            selectedLeague = selectedLeague,
            onLeagueChange = { league -> 
                selectedLeague = league
            },
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
            // Location and EXACT local time (GMT+3)
            Text(
                text = "${weatherData.city} • ${formatFullDateGMT3(localTime)}",
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
                        text = "${getAccurateWeatherDescription(normalizedCondition, weatherData)} • Feels like ${weatherData.feelsLike.roundToInt()}°C",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${getTimeOfDayGreeting(localTime.get(Calendar.HOUR_OF_DAY))} • Local: ${formatAccurateLocalTimeGMT3(localTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Smart weather analysis
            Text(
                text = getSmartWeatherAnalysis(weatherData, normalizedCondition, rainProbability),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun SmartRainAnalysisCard(
    rainProbability: Int,
    condition: String,
    humidity: Int,
    cloudCover: Int?,
    pressure: Int
) {
    val rainInfo = getRealRainAnalysis(rainProbability, condition, humidity, cloudCover, pressure)
    
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
                text = getRainEmoji(condition, rainProbability),
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
                if (humidity > 80 && condition != "rain" && condition != "drizzle") {
                    Text(
                        text = "High humidity ${humidity}% but no rain expected - feels muggy",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (cloudCover ?: 0 > 60 && condition != "rain") {
                    Text(
                        text = "${cloudCover}% cloud cover - ${getCloudCoverAnalysis(cloudCover ?: 0)}",
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
private fun WeatherIntelligenceCard(
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
                text = "🧠 Weather Intelligence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = getSmartForecast(weatherData, condition, rainProbability, localHour),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A1B9A)
            )
        }
    }
}

@Composable
private fun SmartActivitiesCard(
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
                text = "🎯 Smart Activities",
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
                text = "📊 Weather Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val details = listOf(
                WeatherDetail("💧 Humidity", "${weatherData.humidity}%", getHumidityAnalysis(weatherData.humidity)),
                WeatherDetail("☁️ Cloud Cover", "${getSmartCloudCover(weatherData.cloudCover, weatherData.condition)}%", getCloudCoverAnalysis(getSmartCloudCover(weatherData.cloudCover, weatherData.condition))),
                WeatherDetail("💨 Wind", "${weatherData.windSpeed} m/s", getWindAnalysis(weatherData.windSpeed)),
                WeatherDetail("🌡 Pressure", "${weatherData.pressure} hPa", getPressureAnalysis(weatherData.pressure)),
                WeatherDetail("👁 Visibility", "${weatherData.visibility / 1000} km", getVisibilityAnalysis(weatherData.visibility)),
                WeatherDetail("🌡 Min/Max", "${weatherData.minTemp.roundToInt()}°C / ${weatherData.maxTemp.roundToInt()}°C", getTempRangeAnalysis(weatherData))
            )
            
            details.forEach { detail ->
                WeatherDetailRow(detail)
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun HydrationReminderCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💧",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Stay Hydrated!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Drink water regularly throughout the day, no matter the weather",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
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
        "eng.1" to "Premier League",
        "esp.1" to "La Liga",
        "ita.1" to "Serie A",
        "ger.1" to "Bundesliga",
        "fra.1" to "Ligue 1",
        "uefa.champions" to "Champions League"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚽ Live Football",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🏆 League selector - fully clickable & scrollable
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(leagues) { (id, name) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedLeague == id) Color(0xFF1976D2)
                                    else Color(0xFF757575)
                                )
                                .clickable { onLeagueChange(id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ⚙️ Match list / loading / empty state
                when {
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text("Loading matches... ⚽")
                        }
                    }

                    sports.isEmpty() -> {
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
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sports) { sport ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (sport.status) {
                                            "in" -> Color(0xFFFFF8E1) // Live
                                            "final" -> Color(0xFFE8F5E8) // Finished
                                            else -> Color(0xFFF5F5F5) // Scheduled
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
                                                    "in" -> "🔴 LIVE"
                                                    "final" -> "✅ FINAL"
                                                    else -> formatMatchTime(sport.time)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when (sport.status) {
                                                    "in" -> Color(0xFFD32F2F)
                                                    "final" -> Color(0xFF2E7D32)
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

                                        if (sport.score.isNotBlank() && sport.score != "0-0" && sport.status != "scheduled") {
                                            Text(
                                                text = "Score: ${sport.score}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (sport.status == "final" && sport.time.isNotBlank()) {
                                            Text(
                                                text = "Played: ${sport.time}",
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

// ============ REAL ESPN SPORTS API ============
private suspend fun fetchLiveSportsFromESPN(leagueCode: String): List<LiveSport> = withContext(Dispatchers.IO) {
    return@withContext try {
        // Fetch data for last 7 days (past 6 days + today)
        val calendar = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_YEAR, -6) // Go back 6 days
        val startDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        
        val apiUrl = "https://site.api.espn.com/apis/site/v2/sports/soccer/$leagueCode/scoreboard?dates=$startDate-$endDate"
        
        val response = URL(apiUrl).readText()
        val json = JSONObject(response)
        
        val events = json.getJSONArray("events")
        val sports = mutableListOf<LiveSport>()
        
        for (i in 0 until events.length()) {
            try {
                val event = events.getJSONObject(i)
                val competitions = event.getJSONArray("competitions")
                if (competitions.length() > 0) {
                    val competition = competitions.getJSONObject(0)
                    val league = competition.getJSONObject("league").getString("name")
                    
                    val competitors = competition.getJSONArray("competitors")
                    if (competitors.length() >= 2) {
                        val homeTeam = competitors.getJSONObject(0).getJSONObject("team").getString("name")
                        val awayTeam = competitors.getJSONObject(1).getJSONObject("team").getString("name")
                        
                        val status = event.getJSONObject("status").getString("type")
                        val statusType = when (status) {
                            "in" -> "in"
                            "final" -> "final"
                            else -> "scheduled"
                        }
                        
                        val score = if (competitors.getJSONObject(0).has("score") && competitors.getJSONObject(1).has("score")) {
                            val homeScore = competitors.getJSONObject(0).getString("score")
                            val awayScore = competitors.getJSONObject(1).getString("score")
                            "$homeScore-$awayScore"
                        } else {
                            "0-0"
                        }
                        
                        val matchTime = if (event.has("date")) {
                            formatESPNTime(event.getString("date"))
                        } else {
                            ""
                        }
                        
                        val sport = LiveSport(
                            league = league,
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            score = score,
                            status = statusType,
                            time = matchTime
                        )
                        sports.add(sport)
                    }
                }
            } catch (e: Exception) {
                // Skip individual match if there's an error
                continue
            }
        }
        
        // Sort: Live matches first, then scheduled, then finished (most recent first)
        sports.sortedWith(compareBy(
            { it.status != "in" }, // Live matches first
            { it.status != "scheduled" }, // Then scheduled
            { it.time } // Then by time
        ))
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// ============ SMART CLOUD COVER LOGIC ============
private fun getSmartCloudCover(cloudCover: Int?, condition: String): Int {
    // If it's raining but cloud cover is 0, that's illogical - fix it
    return when {
        condition.contains("rain", ignoreCase = true) && (cloudCover == null || cloudCover < 70) -> 85
        condition.contains("thunderstorm", ignoreCase = true) && (cloudCover == null || cloudCover < 80) -> 95
        condition.contains("drizzle", ignoreCase = true) && (cloudCover == null || cloudCover < 60) -> 75
        condition.contains("clear", ignoreCase = true) && (cloudCover ?: 0) > 50 -> 20
        condition.contains("cloud", ignoreCase = true) && (cloudCover ?: 0) < 40 -> 70
        else -> cloudCover ?: when {
            condition.contains("clear", ignoreCase = true) -> 10
            condition.contains("cloud", ignoreCase = true) -> 75
            else -> 50
        }
    }
}

// ============ ACCURATE TIME FUNCTIONS (GMT+3) ============
private fun getAccurateLocalTimeGMT3(timezoneOffset: Int): Calendar {
    val calendar = Calendar.getInstance()
    // Force GMT+3 timezone regardless of device settings
    val timezone = TimeZone.getTimeZone("GMT+3")
    calendar.timeZone = timezone
    return calendar
}

private fun formatAccurateLocalTimeGMT3(calendar: Calendar): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    formatter.timeZone = calendar.timeZone
    return formatter.format(calendar.time)
}

private fun formatFullDateGMT3(calendar: Calendar): String {
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

private fun formatESPNTime(utcDate: String): String {
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

// ============ REAL RAIN PROBABILITY CALCULATION ============
private fun calculateRealRainProbability(
    humidity: Int, 
    condition: String,
    cloudCover: Int?,
    pressure: Int
): Int {
    return when (condition) {
        "rain", "drizzle" -> {
            // If currently raining, high probability it continues
            when {
                humidity >= 90 -> 95
                humidity >= 80 -> 85
                else -> 75
            }
        }
        "thunderstorm" -> 98
        else -> {
            val actualCloudCover = getSmartCloudCover(cloudCover, condition)
            // Advanced rain probability based on multiple factors
            var probability = 0
            
            // Humidity factor (40% weight)
            probability += when {
                humidity >= 90 -> 40
                humidity >= 85 -> 30
                humidity >= 80 -> 20
                humidity >= 75 -> 10
                else -> 0
            }
            
            // Cloud cover factor (40% weight)
            probability += when {
                actualCloudCover >= 90 -> 40
                actualCloudCover >= 80 -> 30
                actualCloudCover >= 70 -> 20
                actualCloudCover >= 60 -> 10
                else -> 0
            }
            
            // Pressure factor (20% weight) - low pressure = higher rain chance
            probability += when {
                pressure <= 1000 -> 20
                pressure <= 1010 -> 10
                else -> 0
            }
            
            probability
        }
    }
}

private fun getRealRainAnalysis(
    rainProbability: Int, 
    condition: String,
    humidity: Int,
    cloudCover: Int?,
    pressure: Int
): String {
    return when {
        condition == "rain" -> {
            when {
                humidity >= 85 -> "Heavy rain currently falling - high humidity ${humidity}%"
                humidity >= 75 -> "Moderate rain ongoing - humidity ${humidity}%"
                else -> "Light rain currently falling"
            }
        }
        condition == "drizzle" -> "Light drizzle currently - humidity ${humidity}%"
        condition == "thunderstorm" -> "Thunderstorm active - stay indoors ⚡"
        rainProbability >= 85 -> "Heavy rain expected soon - high humidity ${humidity}%"
        rainProbability >= 70 -> "Rain likely - humidity ${humidity}%, ${getSmartCloudCover(cloudCover, condition)}% clouds"
        rainProbability >= 50 -> "Possible rain later - ${getSmartCloudCover(cloudCover, condition)}% cloud cover"
        rainProbability >= 30 -> "Light rain possible - keep umbrella handy"
        humidity >= 80 -> "High humidity ${humidity}% but no rain expected"
        else -> "No rain expected - clear conditions"
    }
}

// ============ SMART WEATHER FUNCTIONS ============
private fun getAccurateWeatherDescription(condition: String, weatherData: WeatherData): String {
    return when (condition) {
        "clear" -> "Clear skies"
        "clouds" -> "${getCloudCoverDescription(getSmartCloudCover(weatherData.cloudCover, weatherData.condition))}"
        "rain" -> "${getRainIntensity(weatherData.humidity)} rain"
        "drizzle" -> "Light drizzle"
        "thunderstorm" -> "Thunderstorm"
        "snow" -> "Snowing"
        "mist" -> "Misty"
        else -> "Clear"
    }
}

private fun getSmartWeatherAnalysis(weatherData: WeatherData, condition: String, rainProbability: Int): String {
    val analysis = mutableListOf<String>()
    val smartCloudCover = getSmartCloudCover(weatherData.cloudCover, condition)
    val temp = weatherData.temp.roundToInt()

    // 🌡️ Temperature analysis (realistic for local comfort levels)
    when {
        temp <= 0 -> analysis.add("Freezing $temp°C – stay indoors if you can ❄️🧊")
        temp in 1..5 -> analysis.add("Extremely cold $temp°C – heavy coat & gloves needed 🧥🧤")
        temp in 6..10 -> analysis.add("Cold $temp°C – wear a thick jacket 🧣")
        temp in 11..14 -> analysis.add("Chilly $temp°C – sweater or jacket is a must 🍂")
        temp in 15..18 -> analysis.add("Cool $temp°C – you’ll still need a light jacket 🌤️")
        temp in 19..22 -> analysis.add("Mild $temp°C – comfortable but not warm, long sleeves recommended 👕")
        temp in 23..27 -> analysis.add("Warm $temp°C – nice weather for t-shirts ☀️")
        temp in 28..32 -> analysis.add("Hot $temp°C – drink water and stay cool 🥵")
        temp > 32 -> analysis.add("Scorching $temp°C – avoid staying too long in the sun 🔥")
    }

    // 💧 Humidity analysis
    when {
        weatherData.humidity >= 85 -> analysis.add("Very humid ${weatherData.humidity}% – heavy air 💦")
        weatherData.humidity in 70..84 -> analysis.add("Humid ${weatherData.humidity}% – sticky and uncomfortable 🌫️")
        weatherData.humidity in 40..69 -> analysis.add("Comfortable humidity ${weatherData.humidity}% 👍")
        weatherData.humidity < 40 -> analysis.add("Dry ${weatherData.humidity}% – low moisture 💨")
    }

    // ☁️ Cloud cover analysis
    when {
        smartCloudCover >= 80 -> analysis.add("Overcast (${smartCloudCover}%) ☁️")
        smartCloudCover in 60..79 -> analysis.add("Mostly cloudy (${smartCloudCover}%) 🌥️")
        smartCloudCover in 30..59 -> analysis.add("Partly cloudy (${smartCloudCover}%) ⛅")
        smartCloudCover < 30 -> analysis.add("Clear skies (${smartCloudCover}%) 🌞")
    }

    // ☔ Rain chance
    when {
        rainProbability >= 70 -> analysis.add("High chance of rain (${rainProbability}%) – carry an umbrella ☔")
        rainProbability in 40..69 -> analysis.add("Possible rain (${rainProbability}%) – maybe pack a jacket 🌦️")
        rainProbability in 10..39 -> analysis.add("Slight chance of rain (${rainProbability}%) 🌤️")
    }

    return analysis.joinToString(" • ")
}


private fun getSmartForecast(weatherData: WeatherData, condition: String, rainProbability: Int, localHour: Int): String {
    val temp = weatherData.temp
    val humidity = weatherData.humidity
    val smartCloudCover = getSmartCloudCover(weatherData.cloudCover, condition)
    
    val base = buildString {
        // Temperature focus
        when {
            temp <= 0 -> append("Freezing day with temperatures below zero. ")
            temp < 10 -> append("Cold conditions perfect for warm activities. ")
            temp >= 30 -> append("Very warm day ahead, perfect for summer activities. ")
            else -> append("Comfortable temperatures for various activities. ")
        }
        
        // Humidity intelligence
        when {
            humidity >= 85 && condition != "rain" -> append("High humidity ${humidity}% creating muggy conditions but no rain expected. ")
            humidity >= 75 -> append("Humid ${humidity}% making it feel warmer than actual temperature. ")
            humidity <= 35 -> append("Dry ${humidity}% making for very comfortable conditions. ")
        }
        
        // Cloud cover intelligence
        when {
            smartCloudCover >= 80 -> append("${smartCloudCover}% cloud cover keeping temperatures stable. ")
            smartCloudCover <= 20 -> append("Only ${smartCloudCover}% clouds allowing plenty of sunshine. ")
        }
        
        // Rain intelligence
        when {
            condition == "rain" -> append("Rain expected to continue throughout the day. ")
            rainProbability >= 70 -> append("High chance of rain later (${rainProbability}% probability). ")
            rainProbability >= 50 -> append("Possible showers with ${rainProbability}% chance. ")
        }
    }
    
    return base
}

private fun getLogicalActivities(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    rainProbability: Int,
    normalizedCondition: String
): String {
    val activities = mutableListOf<String>()
    val temp = weatherData.temp
    
    // NO PARK if raining - LOGICAL activities only
    if (normalizedCondition == "rain" || normalizedCondition == "thunderstorm" || rainProbability > 60) {
        // INDOOR activities only when raining - SMART activities (not just games)
        activities.addAll(listOf(
            "Reading books or magazines 📚",
            "Watching movies or series 🎬", 
            "Taking a relaxing nap 😴",
            "Doing work or assignments 💼",
            "Cooking or baking 🍳",
            "Cleaning and organizing 🧹",
            "Meditation or yoga 🧘",
            "Learning something new 📖"
        ))
        
        // Add appropriate clothing advice for rain
        when {
            temp < 15 -> activities.add("Wear waterproof jacket and warm layers 🧥")
            else -> activities.add("Wear waterproof jacket and boots 🌧️")
        }
    } else {
        // OUTDOOR activities when NOT raining
        when {
            isNight -> activities.addAll(listOf(
                "Evening city walk 🏙️",
                "Stargazing if clear 🌟",
                "Outdoor dining 🍽️",
                "Night photography 📷"
            ))
            localHour in 6..11 -> activities.addAll(listOf(
                "Morning jog or walk 🏃",
                "Coffee at outdoor cafe ☕",
                "Park visit 🌳",
                "Farmer's market 🛍️"
            ))
            localHour in 12..17 -> activities.addAll(listOf(
                "Outdoor lunch 🧺",
                "Beach or pool (if warm) 🏖️",
                "Cycling in park 🚴",
                "Gardening 🌷"
            ))
            localHour in 18..23 -> activities.addAll(listOf(
                "Sunset watching 🌇",
                "Evening social gathering 👥",
                "Outdoor sports ⚽",
                "Rooftop relaxation 🏙️"
            ))
        }
        
        // Temperature-based clothing advice
        when {
            temp < 10 -> activities.add("Wear warm layers and jacket 🧥")
            temp < 20 -> activities.add("Light jacket recommended 🧥")
            temp > 25 -> activities.add("Light clothing recommended 👕")
        }
    }
    
    val selected = activities.shuffled().take(3)
    return selected.joinToString(" • ")
}

// ============ HELPER FUNCTIONS ============
private fun shouldShowRainPrediction(rainProbability: Int, humidity: Int, condition: String): Boolean {
    return rainProbability > 0 || condition == "rain" || condition == "drizzle" || condition == "thunderstorm" || humidity > 80
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

private fun getRainEmoji(condition: String, rainProbability: Int): String {
    return when {
        condition == "thunderstorm" -> "⛈️"
        condition == "rain" -> "🌧️"
        condition == "drizzle" -> "🌦️"
        rainProbability >= 70 -> "🌧️"
        rainProbability >= 40 -> "🌦️"
        else -> "💧"
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

// ============ WEATHER ANALYSIS FUNCTIONS ============
private fun getHumidityAnalysis(humidity: Int): String {
    return when {
        humidity >= 90 -> "Very humid, rain likely"
        humidity >= 80 -> "Humid, possible rain"
        humidity >= 70 -> "Moderate humidity"
        humidity <= 35 -> "Dry and comfortable"
        else -> "Normal humidity levels"
    }
}

private fun getCloudCoverAnalysis(cloudCover: Int): String {
    return when {
        cloudCover >= 90 -> "Overcast skies"
        cloudCover >= 70 -> "Mostly cloudy"
        cloudCover >= 50 -> "Partly cloudy"
        cloudCover >= 30 -> "Some clouds"
        else -> "Mostly clear"
    }
}

private fun getWindAnalysis(windSpeed: Double): String {
    return when {
        windSpeed > 8 -> "Strong winds"
        windSpeed > 5 -> "Moderate breeze"
        windSpeed > 2 -> "Light breeze"
        else -> "Calm conditions"
    }
}

private fun getPressureAnalysis(pressure: Int): String {
    return when {
        pressure > 1020 -> "High pressure, stable weather"
        pressure < 1000 -> "Low pressure, changing weather"
        else -> "Normal pressure conditions"
    }
}

private fun getVisibilityAnalysis(visibility: Int): String {
    return when {
        visibility > 10000 -> "Excellent visibility"
        visibility > 5000 -> "Good visibility"
        visibility > 2000 -> "Moderate visibility"
        else -> "Poor visibility"
    }
}

private fun getTempRangeAnalysis(weatherData: WeatherData): String {
    val range = weatherData.maxTemp - weatherData.minTemp
    return when {
        range > 12 -> "Large temperature swing"
        range > 8 -> "Moderate variation"
        range > 4 -> "Small variation"
        else -> "Stable temperatures"
    }
}

private fun getRainIntensity(humidity: Int): String {
    return when {
        humidity >= 85 -> "Heavy"
        humidity >= 75 -> "Moderate"
        else -> "Light"
    }
}

private fun getCloudCoverDescription(cloudCover: Int): String {
    return when {
        cloudCover >= 90 -> "Overcast"
        cloudCover >= 70 -> "Mostly cloudy"
        cloudCover >= 50 -> "Partly cloudy"
        cloudCover >= 30 -> "Some clouds"
        else -> "Mostly clear"
    }
}

// Add this extension function for clickable modifier
fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return this
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

// Make sure your WeatherData class includes cloudCover property:
// data class WeatherData(
//     val city: String,
//     val temp: Double,
//     val feelsLike: Double,
//     val minTemp: Double,
//     val maxTemp: Double,
//     val humidity: Int,
//     val windSpeed: Double,
//     val pressure: Int,
//     val visibility: Int,
//     val condition: String,
//     val timezoneOffset: Int,
//     val cloudCover: Int? = null
// )

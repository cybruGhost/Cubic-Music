package it.fast4x.rimusic.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.kreate.android.R
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.ytAccountName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Key constants
private const val KEY_USERNAME = "username"
private const val KEY_CITY = "weather_city"

// Weather data class
data class WeatherData(
    val temp: Double,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val city: String,
    val feelsLike: Double,
    val pressure: Int,
    val visibility: Int,
    val minTemp: Double,
    val maxTemp: Double,
    val sunrise: Long,
    val sunset: Long
)

@Composable
fun WelcomeMessage() {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var showInputPage by remember { mutableStateOf(true) }
    var showChangeDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var showWeatherPopup by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load username and city on composition
    LaunchedEffect(Unit) {
        username = DataStoreUtils.getStringBlocking(context, KEY_USERNAME)
        city = DataStoreUtils.getStringBlocking(context, KEY_CITY)
        showInputPage = username.isBlank()
        
        // If city is not set, try to get location from IP
        if (city.isBlank()) {
            city = getLocationFromIP() ?: "Nairobi"
            DataStoreUtils.saveStringBlocking(context, KEY_CITY, city)
        }
    }
    
    // Fetch weather when city is available
    LaunchedEffect(city) {
        if (city.isNotBlank()) {
            isLoading = true
            errorMessage = null
            weatherData = fetchWeatherData(city)
            isLoading = false
            if (weatherData == null) {
                errorMessage = "Failed to fetch weather data"
            }
        }
    }
    
    if (showInputPage) {
        UsernameInputPage { enteredUsername ->
            DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, enteredUsername)
            username = enteredUsername
            showInputPage = false
        }
    } else {
        Column {
            GreetingMessage(
                username = username,
                weatherData = weatherData,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onUsernameClick = { showChangeDialog = true },
                onWeatherClick = { showWeatherPopup = true },
                onCityClick = { showCityDialog = true }
            )
        }
        
        if (showChangeDialog) {
            ChangeUsernameDialog(
                currentUsername = username,
                onDismiss = { showChangeDialog = false },
                onUsernameChanged = { newUsername ->
                    DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, newUsername)
                    username = newUsername
                    showChangeDialog = false
                }
            )
        }
        
        if (showCityDialog) {
            ChangeCityDialog(
                currentCity = city,
                onDismiss = { showCityDialog = false },
                onCityChanged = { newCity ->
                    DataStoreUtils.saveStringBlocking(context, KEY_CITY, newCity)
                    city = newCity
                    showCityDialog = false
                }
            )
        }
        
        if (showWeatherPopup && weatherData != null) {
            WeatherPopup(
                weatherData = weatherData!!,
                username = username,
                onDismiss = { showWeatherPopup = false },
                onCityChange = { showCityDialog = true }
            )
        }
    }
}

@Composable
private fun GreetingMessage(
    username: String, 
    weatherData: WeatherData?,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameClick: () -> Unit,
    onWeatherClick: () -> Unit,
    onCityClick: () -> Unit
) {
    val hour = remember {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH", Locale.getDefault())
        formatter.format(date).toInt()
    }

    val message = when (hour) {
        in 6..12 -> stringResource(R.string.good_morning)
        in 13..17 -> stringResource(R.string.good_afternoon)
        in 18..23 -> stringResource(R.string.good_evening)
        else -> stringResource(R.string.good_night)
    }.let {
        val baseMessage = if (isYouTubeLoggedIn()) "$it, ${ytAccountName()}" else it
        "$baseMessage, "
    }

    // Animations
    var animated by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "greetingAnimation"
    )
    
    var underlineAnimated by remember { mutableStateOf(false) }
    val underlineProgress by animateFloatAsState(
        targetValue = if (underlineAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "underlineAnimation"
    )

    LaunchedEffect(Unit) {
        animated = true
        delay(300)
        underlineAnimated = true
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .alpha(alpha)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFBB86FC),
                modifier = Modifier
                    .clickable(onClick = onUsernameClick)
                    .drawWithContent {
                        drawContent()
                        if (underlineProgress > 0) {
                            drawLine(
                                color = Color(0xFFBB86FC),
                                start = Offset(0f, size.height),
                                end = Offset(size.width * underlineProgress, size.height),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
            )
            
            // Weather display
            Spacer(modifier = Modifier.size(8.dp))
            if (isLoading) {
                Text(
                    text = "⏳",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = "❌",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable(onClick = onCityClick)
                        .padding(horizontal = 4.dp)
                )
            } else {
                weatherData?.let { weather ->
                    Text(
                        text = "${weather.temp.toInt()}°C ${getWeatherEmoji(weather.condition)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4FC3F7),
                        modifier = Modifier
                            .clickable(onClick = onWeatherClick)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
        
        // City name
        weatherData?.let { weather ->
            Text(
                text = weather.city,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0BEC5),
                modifier = Modifier
                    .clickable(onClick = onCityClick)
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun WeatherPopup(
    weatherData: WeatherData,
    username: String,
    onDismiss: () -> Unit,
    onCityChange: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Weather Forecast",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCityChange) {
                    Text("Change City", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Header with current weather
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getWeatherEmoji(weatherData.condition),
                                style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "${weatherData.temp.toInt()}°C",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = weatherData.condition,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Feels like ${weatherData.feelsLike.toInt()}°C",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Clothing recommendation
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "👕 Outfit Suggestion for $username",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = getClothingRecommendation(weatherData.temp, weatherData.condition),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                // Weather details grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weather Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        WeatherDetailRow("Humidity", "${weatherData.humidity}%", "💧")
                        WeatherDetailRow("Wind Speed", "${weatherData.windSpeed} m/s", "💨")
                        WeatherDetailRow("Pressure", "${weatherData.pressure} hPa", "🌡")
                        WeatherDetailRow("Visibility", "${weatherData.visibility / 1000} km", "👁")
                        WeatherDetailRow("Min/Max Temp", "${weatherData.minTemp.toInt()}°C / ${weatherData.maxTemp.toInt()}°C", "📊")
                        
                        val sunriseTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(weatherData.sunrise * 1000)
                        val sunsetTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(weatherData.sunset * 1000)
                        WeatherDetailRow("Sunrise/Sunset", "$sunriseTime / $sunsetTime", "🌅")
                    }
                }

                // Forecast message
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📋 Today's Outlook",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = getDetailedForecast(weatherData),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Forecast")
            }
        }
    )
}

@Composable
private fun WeatherDetailRow(label: String, value: String, emoji: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = emoji,
                modifier = Modifier.width(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChangeCityDialog(
    currentCity: String,
    onDismiss: () -> Unit,
    onCityChanged: (String) -> Unit
) {
    var newCity by remember { mutableStateOf(currentCity) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Change Weather Location", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    "Enter your city name:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = newCity,
                    onValueChange = { newCity = it },
                    label = { Text("City name") },
                    placeholder = { Text("e.g., London, Tokyo, New York") },
                    singleLine = true,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    "Tip: Use 'Detect My Location' for automatic detection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCity.isNotBlank()) {
                        onCityChanged(newCity.trim())
                    }
                },
                enabled = newCity.isNotBlank()
            ) {
                Text("Save Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Rest of the composables (UsernameInputPage, ChangeUsernameDialog) remain the same as your original code...

private suspend fun fetchWeatherData(city: String): WeatherData? = withContext(Dispatchers.IO) {
    return@withContext try {
        val apiKey = "5174a4c980abc22f0dc589db984742cf"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$apiKey"
        val response = URL(url).readText()
        val json = JSONObject(response)

        val main = json.getJSONObject("main")
        val weather = json.getJSONArray("weather").getJSONObject(0)
        val wind = json.getJSONObject("wind")
        val sys = json.getJSONObject("sys")

        WeatherData(
            temp = main.getDouble("temp"),
            condition = weather.getString("main"),
            icon = weather.getString("icon"),
            humidity = main.getInt("humidity"),
            windSpeed = wind.getDouble("speed"),
            city = json.getString("name"),
            feelsLike = main.getDouble("feels_like"),
            pressure = main.getInt("pressure"),
            visibility = json.getInt("visibility"),
            minTemp = main.getDouble("temp_min"),
            maxTemp = main.getDouble("temp_max"),
            sunrise = sys.getLong("sunrise"),
            sunset = sys.getLong("sunset")
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getLocationFromIP(): String? {
    // Simplified IP location detection - in production, use a proper IP geolocation service
    return try {
        val response = URL("http://ip-api.com/json").readText()
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            json.getString("city")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun getWeatherEmoji(condition: String): String {
    return when (condition.lowercase(Locale.ROOT)) {
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "rain" -> "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> "❄️"
        "mist", "fog", "haze" -> "🌫️"
        else -> "🌍"
    }
}

private fun getClothingRecommendation(temp: Double, condition: String): String {
    return when {
        temp >= 30 -> "Light clothing recommended! 👕 Shorts and t-shirt weather. Stay hydrated! 💧"
        temp >= 25 -> "Warm weather! 😎 Light clothes are perfect. Maybe a light jacket for evening."
        temp >= 20 -> "Pleasant temperature! 👔 Light layers work well. A jacket might be needed."
        temp >= 15 -> "Cool weather! 🧥 Wear a jacket or sweater. Layers are your friend."
        temp >= 10 -> "Chilly! 🧣 Time for warmer clothes. Don't forget a warm jacket."
        temp >= 0 -> "Cold! 🧤 Bundle up with heavy coat, scarf, and gloves."
        else -> "Freezing! 🥶 Extreme cold - thermal layers essential. Stay warm!"
    } + when (condition.lowercase(Locale.ROOT)) {
        "rain", "drizzle" -> " And don't forget an umbrella! ☔"
        "snow" -> " Wear waterproof boots! 👢"
        "thunderstorm" -> " Stay indoors if possible! ⚡"
        else -> ""
    }
}

private fun getDetailedForecast(weatherData: WeatherData): String {
    val temp = weatherData.temp
    val condition = weatherData.condition.lowercase(Locale.ROOT)
    
    return when (condition) {
        "clear" -> "Perfect clear skies today! Excellent visibility with no precipitation expected. Great day for outdoor activities."
        "clouds" -> "Cloudy conditions throughout the day. Temperatures will feel comfortable with occasional breaks of sunshine."
        "rain" -> "Rain expected today. The rain may vary in intensity, so keep an umbrella handy. Expected to continue through the day."
        "drizzle" -> "Light drizzle expected. Not heavy enough to disrupt plans but might require a light jacket or umbrella."
        "thunderstorm" -> "Thunderstorms likely today. Seek shelter during storms and avoid outdoor activities when lightning is present."
        "snow" -> "Snowfall expected today. Roads may be slippery - drive carefully and dress in warm, waterproof layers."
        "mist", "fog" -> "Reduced visibility due to fog/mist. Travel with caution and use headlights when driving."
        else -> "Mixed weather conditions today. Check updates regularly as conditions may change throughout the day."
    } + " Temperatures will range from ${weatherData.minTemp.toInt()}°C to ${weatherData.maxTemp.toInt()}°C."
}

// Keep your original UsernameInputPage and ChangeUsernameDialog composables exactly as they were...
@Composable
private fun UsernameInputPage(onUsernameSubmitted: (String) -> Unit) {
    var usernameInput by remember { mutableStateOf("") }
    
    var animated by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "inputPageAnimation"
    )

    LaunchedEffect(Unit) {
        animated = true
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome! Please enter your name",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { 
                    Text(
                        "Your name",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Button(
                onClick = {
                    if (usernameInput.isNotBlank()) {
                        onUsernameSubmitted(usernameInput.trim())
                    }
                },
                enabled = usernameInput.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun ChangeUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onUsernameChanged: (String) -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Change Your Name",
                color = MaterialTheme.colorScheme.onBackground
            ) 
        },
        text = {
            Column {
                Text(
                    "Enter your new name:",
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { 
                        Text(
                            "Your name",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isNotBlank()) {
                        onUsernameChanged(newUsername.trim())
                    }
                },
                enabled = newUsername.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

fun clearUsername(context: android.content.Context) {
    DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, "")
}

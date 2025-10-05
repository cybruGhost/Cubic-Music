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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import app.kreate.android.R
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.ytAccountName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import it.fast4x.rimusic.utils.getWeatherEmoji
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Key constants
private const val KEY_USERNAME = "username"
private const val KEY_CITY = "weather_city"

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
            WeatherForecastPopup(
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
                    text = "⚙️",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = "💤",
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
private fun ChangeCityDialog(
    currentCity: String,
    condition: String, // 🌦️ Add this to apply weather-based theme
    onDismiss: () -> Unit,
    onCityChanged: (String) -> Unit
) {
    var newCity by remember { mutableStateOf(currentCity) }

    // 🌤️ Define color themes based on condition
    val (bgGradient, textColor) = when (condition.lowercase()) {
        "rain", "drizzle" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF4A5568), Color(0xFF2C5282))
        ) to Color(0xFFE2E8F0)
        "clouds", "overcast" -> Brush.verticalGradient(
            colors = listOf(Color(0xFFCBD5E0), Color(0xFFA0AEC0))
        ) to Color(0xFF2D3748)
        "clear", "sunny" -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFB74D))
        ) to Color(0xFF3E2723)
        "snow" -> Brush.verticalGradient(
            colors = listOf(Color(0xFFE0F7FA), Color(0xFFB3E5FC))
        ) to Color(0xFF01579B)
        "thunderstorm" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2C3E50), Color(0xFF000000))
        ) to Color(0xFFE0E0E0)
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFFD1C4E9), Color(0xFF9575CD))
        ) to Color(0xFF212121)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🌍 Change Weather Location",
                style = MaterialTheme.typography.titleMedium.copy(color = textColor),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .background(bgGradient, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    Text(
                        "Enter your city name:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { Text("City name") },
                        placeholder = { Text("e.g., London, Tokyo, Nairobi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Tip: Use 'Detect My Location' for automatic detection",
                        style = MaterialTheme.typography.labelSmall.copy(color = textColor.copy(alpha = 0.7f)),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Text(
                        text = "by Cyberghost @2025 Cubic Music",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = textColor.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCity.isNotBlank()) {
                        onCityChanged(newCity.trim())
                    }
                },
                enabled = newCity.isNotBlank(),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
    )
}


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
        val clouds = json.optJSONObject("clouds")  // 🌥️ Fetch cloud cover if available

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
            sunset = sys.getLong("sunset"),
            cloudCover = clouds?.optInt("all") // ☁️ Add cloud cover safely
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


// --- keep this here ---
private fun getLocationFromIP(): String? {
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

fun getWeatherEmoji(condition: String): String {
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

fun clearUsername(context: Context) {
    DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, "")
}

package app.it.fast4x.rimusic.utils

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import app.kreate.android.R
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.ytAccountName
import app.it.fast4x.rimusic.utils.getWeatherEmoji

// Key constants
private const val KEY_USERNAME = "username"
private const val KEY_CITY = "weather_city"
private const val PREF_TEMP_UNIT = "temperature_unit"
private const val DEFAULT_TEMP_UNIT = "celsius"

// Temperature unit management
private fun getSavedTemperatureUnit(context: Context): String {
    return DataStoreUtils.getStringBlocking(context, PREF_TEMP_UNIT).ifEmpty { DEFAULT_TEMP_UNIT }
}

private fun saveTemperatureUnit(context: Context, unit: String) {
    DataStoreUtils.saveStringBlocking(context, PREF_TEMP_UNIT, unit)
}

private fun celsiusToFahrenheit(celsius: Double): Double {
    return (celsius * 9/5) + 32
}

private fun formatTemperature(temp: Double, isCelsius: Boolean): String {
    val displayTemp = if (isCelsius) temp else celsiusToFahrenheit(temp)
    val unit = if (isCelsius) "°C" else "°F"
    return "${displayTemp.roundToInt()}$unit"
}

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
    var temperatureUnit by remember { mutableStateOf(getSavedTemperatureUnit(context)) }
    val isCelsius = temperatureUnit == "celsius"
    
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
                onCityClick = { showCityDialog = true },
                isCelsius = isCelsius
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
                condition = weatherData?.condition ?: "clear",
                onDismiss = { showCityDialog = false },
                onCityChanged = { newCity ->
                    // FIX: Save the city to DataStore persistently
                    DataStoreUtils.saveStringBlocking(context, KEY_CITY, newCity)
                    city = newCity
                    showCityDialog = false
                },
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChanged = { newUnit ->
                    temperatureUnit = newUnit
                    saveTemperatureUnit(context, newUnit)
                }
            )
        }

        
        if (showWeatherPopup && weatherData != null) {
            WeatherForecastPopup(
                weatherData = weatherData!!,
                username = username,
                onDismiss = { showWeatherPopup = false },
                onCityChange = { showCityDialog = true },
                temperatureUnit = temperatureUnit
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
    onCityClick: () -> Unit,
    isCelsius: Boolean
) {
    val hour = remember {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH", Locale.getDefault())
        formatter.format(date).toInt()
    }

    val message = when (hour) {
        in 4..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        in 17..20 -> stringResource(R.string.good_evening)
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
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4FC3F7)
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
                        text = "${formatTemperature(weather.temp, isCelsius)} ${getWeatherEmoji(weather.condition)}",
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
fun ChangeCityDialog(
    currentCity: String,
    condition: String,
    onDismiss: () -> Unit,
    onCityChanged: (String) -> Unit,
    temperatureUnit: String,
    onTemperatureUnitChanged: (String) -> Unit
) {
    var newCity by remember { mutableStateOf(currentCity) }
    val isCelsius = temperatureUnit == "celsius"

    // Better color scheme with improved contrast
    val (bgGradient, textColor) = when (condition.lowercase()) {
        "rain", "drizzle" -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF4A6572), Color(0xFF344955))),
            Color(0xFFE1F5FE)
        )
        "clouds", "overcast" -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF546E7A), Color(0xFF37474F))),
            Color(0xFFECEFF1)
        )
        "clear", "sunny" -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00))),
            Color(0xFF212121)
        )
        "snow" -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF81D4FA))),
            Color(0xFF01579B)
        )
        "thunderstorm" -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238))),
            Color(0xFFE0F7FA)
        )
        else -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF7E57C2), Color(0xFF5E35B1))),
            Color(0xFFEDE7F6)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🌍 Change Weather Location",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.Black, // 👈 make text black
                    fontWeight = FontWeight.SemiBold
                ),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Temperature unit toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Temperature Unit:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF424242), shape = RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCelsius) Color(0xFF4CAF50) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onTemperatureUnitChanged("celsius") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "°C",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isCelsius) Color.White else textColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (!isCelsius) Color(0xFF4CAF50) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onTemperatureUnitChanged("fahrenheit") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "°F",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (!isCelsius) Color.White else textColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "Enter your city name:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { 
                            Text(
                                "City name", 
                                color = textColor.copy(alpha = 0.8f)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "e.g., London, Tokyo, Nairobi",
                                color = textColor.copy(alpha = 0.6f)
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor,
                            focusedBorderColor = textColor.copy(alpha = 0.8f),
                            unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor.copy(alpha = 0.7f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "💡 Tip: Weather conditions might not be very accurate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = textColor.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Cyberghost @2026 Cubic Music",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = textColor.copy(alpha = 0.7f),
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCity.isNotBlank()) onCityChanged(newCity.trim())
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
        animationSpec = tween(durationMillis = 800),
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
                .padding(40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome icon/emoji for visual appeal
            Text(
                text = "👋",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Welcome",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Let's get started with your name",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { 
                    Text(
                        "Enter your name",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (usernameInput.isNotBlank()) {
                            onUsernameSubmitted(usernameInput.trim())
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
            
            Button(
                onClick = {
                    if (usernameInput.isNotBlank()) {
                        onUsernameSubmitted(usernameInput.trim())
                    }
                },
                enabled = usernameInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Continue",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
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
    val maxChars = 14

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_username_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.change_username_prompt,
                        maxChars
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = {
                        if (it.length <= maxChars) newUsername = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.username_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(
                    text = stringResource(
                        R.string.character_count,
                        newUsername.length,
                        maxChars
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (newUsername.length >= maxChars)
                        Color.Red
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private suspend fun fetchWeatherData(city: String): WeatherData? = withContext(Dispatchers.IO) {
    return@withContext try {
        // Fetch API key dynamically from hosted JSON
        val configUrl = "https://zesty-medovik-e88f42.netlify.app/wantamkilasikuhehe.json" // ww ruto ww kasongo ..wantam
        val configResponse = URL(configUrl).readText()
        val configJson = JSONObject(configResponse)
        val apiKey = configJson.getString("weather_api_key")

        // Then use the fetched key normally
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$apiKey"
        val response = URL(url).readText()
        val json = JSONObject(response)

        val main = json.getJSONObject("main")
        val weather = json.getJSONArray("weather").getJSONObject(0)
        val wind = json.getJSONObject("wind")
        val sys = json.getJSONObject("sys")
        val clouds = json.optJSONObject("clouds")

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
            cloudCover = clouds?.optInt("all")
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// FIXED: Using the new ipapi.co API with proper error handling
private suspend fun getLocationFromIP(): String? {
    return try {
        val url = URL("https://ipinfo.io/json/")
        val connection = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            val json = JSONObject(response)
            json.optString("city", "invalid city")
        } else "API failed, just key in your city"
    } catch (e: Exception) {
        e.printStackTrace()
        "failure happened, just key in ur city"
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
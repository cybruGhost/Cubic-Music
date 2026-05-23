package app.it.fast4x.rimusic.utils

import android.content.Context
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ytAccountName
import app.it.fast4x.rimusic.utils.getWeatherEmoji

// Key constants
private const val KEY_USERNAME = "username"
private const val KEY_CITY = "weather_city"
private const val KEY_NAME_SOURCE = "welcome_name_source"
private const val PREF_TEMP_UNIT = "temperature_unit"
private const val DEFAULT_TEMP_UNIT = "celsius"
private const val NAME_SOURCE_CUSTOM = "custom"
private const val NAME_SOURCE_YT = "yt"
private const val WEATHER_REFRESH_INTERVAL_MS = 10 * 60 * 1000L

private object WeatherSessionCache {
    private var cachedCity: String? = null
    private var cachedWeather: WeatherData? = null
    private var fetchedAtMillis: Long = 0L

    fun get(city: String, forceRefresh: Boolean = false): WeatherData? {
        if (forceRefresh) return null
        if (cachedCity != city) return null
        if (cachedWeather == null) return null
        if (System.currentTimeMillis() - fetchedAtMillis > WEATHER_REFRESH_INTERVAL_MS) return null
        return cachedWeather
    }

    fun isStale(city: String): Boolean {
        if (cachedCity != city) return true
        if (cachedWeather == null) return true
        return System.currentTimeMillis() - fetchedAtMillis > WEATHER_REFRESH_INTERVAL_MS
    }

    fun update(city: String, weatherData: WeatherData?) {
        cachedCity = city
        cachedWeather = weatherData
        fetchedAtMillis = System.currentTimeMillis()
    }
}

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

private fun sanitizedYouTubeAccountName(): String? {
    val value = ytAccountName()?.trim().orEmpty()
    if (value.isBlank()) return null
    if (value.length > 80) return null
    if (value.contains(';')) return null
    if (value.contains("SAPISID=", ignoreCase = true)) return null
    if (value.contains("SID=", ignoreCase = true)) return null
    if (value.contains("__Secure-", ignoreCase = true)) return null
    if (value.contains("LOGIN_INFO", ignoreCase = true)) return null
    return value
}

private fun joinedSinceLabel(context: Context): String {
    val installedAt = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrDefault(System.currentTimeMillis())

    val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    return "Joined ${formatter.format(java.util.Date(installedAt))} - today"
}

@Composable
fun WelcomeMessage(
    onOpenAccountsSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var nameSource by remember { mutableStateOf(NAME_SOURCE_CUSTOM) }
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
        nameSource = DataStoreUtils.getStringBlocking(context, KEY_NAME_SOURCE).ifBlank { NAME_SOURCE_CUSTOM }
        city = DataStoreUtils.getStringBlocking(context, KEY_CITY)
        showInputPage = username.isBlank()
        
        // If city is not set, try to get location from IP
        if (city.isBlank()) {
            city = getLocationFromIP() ?: "Nairobi"
            DataStoreUtils.saveStringBlocking(context, KEY_CITY, city)
        }
    }

    val youtubeAccountName = sanitizedYouTubeAccountName()
    val displayName = if (nameSource == NAME_SOURCE_YT && !youtubeAccountName.isNullOrBlank()) {
        youtubeAccountName
    } else {
        username.ifBlank { youtubeAccountName.orEmpty() }
    }

    suspend fun refreshWeather(forceRefresh: Boolean = false) {
        if (city.isBlank()) return

        val cachedWeather = WeatherSessionCache.get(city, forceRefresh)
        if (cachedWeather != null) {
            weatherData = cachedWeather
            isLoading = false
            errorMessage = null
            return
        }

        isLoading = true
        errorMessage = null
        val fetchedWeather = fetchWeatherData(city)
        weatherData = fetchedWeather
        WeatherSessionCache.update(city, fetchedWeather)
        isLoading = false
        if (fetchedWeather == null) {
            errorMessage = "Failed to fetch weather data"
        }
    }
    
    // Fetch weather when city is available
    LaunchedEffect(city) {
        if (city.isNotBlank()) {
            refreshWeather(forceRefresh = false)
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, city) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && city.isNotBlank() && WeatherSessionCache.isStale(city)) {
                coroutineScope.launch {
                    refreshWeather(forceRefresh = true)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                username = displayName,
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
                currentNameSource = nameSource,
                youtubeAccountName = youtubeAccountName,
                onOpenAccountsSettings = onOpenAccountsSettings,
                onDismiss = { showChangeDialog = false },
                onUsernameChanged = { newUsername, newSource ->
                    DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, newUsername)
                    DataStoreUtils.saveStringBlocking(context, KEY_NAME_SOURCE, newSource)
                    username = newUsername
                    nameSource = newSource
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
                    WeatherSessionCache.update(newCity, null)
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
                username = displayName,
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
    val palette = colorPalette()
    val type = typography()
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
        val baseMessage = it
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
                color = palette.text,
                style = type.s
            )
            Text(
                text = username,
                style = type.s.copy(fontWeight = FontWeight.SemiBold),
                color = palette.accent,
                modifier = Modifier
                    .clickable(onClick = onUsernameClick)
                    .drawWithContent {
                        drawContent()
                        if (underlineProgress > 0) {
                            drawLine(
                                color = palette.accent,
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
                    color = palette.accent
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
                        style = type.s.copy(fontWeight = FontWeight.Medium),
                        color = palette.accent,
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
                style = type.xxs,
                color = palette.textSecondary,
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
    currentNameSource: String,
    youtubeAccountName: String?,
    onOpenAccountsSettings: (() -> Unit)?,
    onDismiss: () -> Unit,
    onUsernameChanged: (String, String) -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var selectedSource by remember { mutableStateOf(currentNameSource) }
    val maxChars = 14
    val hasYouTubeAccount = !youtubeAccountName.isNullOrBlank()
    val isYouTubeConnected = isYouTubeLoggedIn()
    val context = LocalContext.current
    val palette = colorPalette()
    val type = typography()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.background1,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = stringResource(R.string.change_username_title),
                style = type.l.copy(fontWeight = FontWeight.SemiBold),
                color = palette.text
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.change_username_subtitle),
                    style = type.xs,
                    color = palette.textSecondary
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.accent.copy(alpha = if (palette.isDark) 0.18f else 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(R.drawable.ytmusic),
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.change_username_source_title),
                                    style = type.s.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.text
                                )
                                Text(
                                    text = if (hasYouTubeAccount) {
                                        stringResource(R.string.change_username_source_connected)
                                    } else {
                                        stringResource(R.string.change_username_source_disconnected)
                                    },
                                    style = type.xxs,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Text(
                            text = joinedSinceLabel(context),
                            style = type.xxs,
                            color = palette.textSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selectedSource == NAME_SOURCE_CUSTOM) {
                                    palette.accent.copy(alpha = if (palette.isDark) 0.24f else 0.16f)
                                } else {
                                    palette.background2.copy(alpha = 0.92f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSource = NAME_SOURCE_CUSTOM }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = stringResource(R.string.name_source_custom),
                                        style = type.xs.copy(fontWeight = FontWeight.SemiBold),
                                        color = palette.text
                                    )
                                    Text(
                                        text = stringResource(R.string.name_source_custom_description),
                                        style = type.xxs,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selectedSource == NAME_SOURCE_YT && hasYouTubeAccount) {
                                    palette.accent.copy(alpha = if (palette.isDark) 0.24f else 0.16f)
                                } else {
                                    palette.background2.copy(alpha = 0.92f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (hasYouTubeAccount) {
                                            selectedSource = NAME_SOURCE_YT
                                        } else if (!isYouTubeConnected) {
                                            onOpenAccountsSettings?.invoke()
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = stringResource(R.string.name_source_yt_account),
                                        style = type.xs.copy(fontWeight = FontWeight.SemiBold),
                                        color = palette.text
                                    )
                                    Text(
                                        text = if (hasYouTubeAccount) {
                                            youtubeAccountName.orEmpty()
                                        } else {
                                            stringResource(R.string.change_username_try_ytm_login)
                                        },
                                        style = type.xxs,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = if (selectedSource == NAME_SOURCE_YT && hasYouTubeAccount) {
                        stringResource(R.string.using_youtube_name, youtubeAccountName.orEmpty())
                    } else {
                        stringResource(R.string.using_custom_name)
                    },
                    style = type.xxs,
                    color = palette.textSecondary
                )

                if (!isYouTubeConnected) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.background2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenAccountsSettings?.invoke()
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(R.drawable.ytmusic),
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.change_username_try_ytm_login),
                                style = type.xs,
                                color = palette.text
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = {
                        if (it.length <= maxChars) newUsername = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.username_label),
                            color = palette.textSecondary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        focusedContainerColor = palette.background0,
                        unfocusedContainerColor = palette.background0,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.background3,
                        cursorColor = palette.accent
                    )
                )

                Text(
                    text = stringResource(
                        R.string.character_count,
                        newUsername.length,
                        maxChars
                    ),
                    style = type.xxs,
                    color = if (newUsername.length >= maxChars)
                        palette.red
                    else
                        palette.textSecondary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSource == NAME_SOURCE_YT && hasYouTubeAccount) {
                        onUsernameChanged(newUsername.trim(), selectedSource)
                    } else if (newUsername.isNotBlank()) {
                        onUsernameChanged(newUsername.trim(), selectedSource)
                    }
                },
                enabled = (selectedSource == NAME_SOURCE_YT && hasYouTubeAccount) || newUsername.isNotBlank()
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
        val configUrl = SecureApiConfig.weatherConfigUrl // ww ruto ww kasongo ..wantam
        val configResponse = URL(configUrl).readText()
        val configJson = JSONObject(configResponse)
        val apiKey = configJson.getString("weather_api_key")

        // Then use the fetched key normally
        val url = "${SecureApiConfig.weatherApiBaseUrl}?q=$city&units=metric&appid=$apiKey"
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
        val url = URL(SecureApiConfig.ipInfoUrl)
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

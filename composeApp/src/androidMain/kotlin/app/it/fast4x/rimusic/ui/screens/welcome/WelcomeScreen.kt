// ui/screens/welcome/WelcomeScreen.kt -
package app.it.fast4x.rimusic.ui.screens.welcome

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalUriHandler
import android.net.Uri

// Key constants matching your WelcomeMessage.kt
private const val KEY_USERNAME = "username"
private const val KEY_CITY = "weather_city"
private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"

// Copy the getLocationFromIP function here since it's private in utils
private suspend fun getLocationFromIP(): String? = withContext(Dispatchers.IO) {
    val endpoints = listOf(
        "https://ipapi.co/json/" to "city",
        "https://ipinfo.io/json/" to "city"
    )

    for ((endpoint, cityKey) in endpoints) {
        val detectedCity = runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CubicMusic/Android")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                JSONObject(response).optString(cityKey)
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        if (!detectedCity.isNullOrBlank()) {
            return@withContext detectedCity
        }
    }

    return@withContext null
}

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    var showWelcome by remember { mutableStateOf(true) }
    var welcomeBackgroundIndex by remember { mutableStateOf(0) }
    var detectedWelcomeCity by remember { mutableStateOf("") }
    var userHasSeenWelcome by remember { 
        mutableStateOf(false)
    }
    
    // Load initial state asynchronously
    LaunchedEffect(Unit) {
        val hasSeen = DataStoreUtils.getStringBlocking(context, KEY_HAS_SEEN_WELCOME, "")
        userHasSeenWelcome = hasSeen == "true"
        
        // If city is not set, try to get location from IP
        if (!userHasSeenWelcome) {
            withContext(Dispatchers.IO) {
                val currentCity = DataStoreUtils.getStringBlocking(context, KEY_CITY, "")
                if (currentCity.isBlank()) {
                    try {
                        getLocationFromIP()?.let { detectedCity ->
                            DataStoreUtils.saveStringBlocking(context, KEY_CITY, detectedCity)
                            detectedWelcomeCity = detectedCity
                        }
                    } catch (e: Exception) {
                        // Silently fail
                    }
                } else {
                    detectedWelcomeCity = currentCity
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6500)
            welcomeBackgroundIndex = 1 - welcomeBackgroundIndex
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        WelcomeImageBackground(
            index = welcomeBackgroundIndex,
            modifier = Modifier.fillMaxSize()
        )
        
        if (userHasSeenWelcome && showWelcome) {
            // User has already completed welcome
            LaunchedEffect(Unit) {
                delay(300)
                navController.navigate(NavRoutes.home.name) {
                    popUpTo(NavRoutes.welcome.name) { inclusive = true }
                }
            }
            
            // Loading screen
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6BCB),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome back!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (showWelcome) {
            // Show welcome screen
            WelcomeContent(
                initialCity = detectedWelcomeCity,
                onComplete = { name, city ->
                    DataStoreUtils.saveStringBlocking(context, KEY_HAS_SEEN_WELCOME, "true")
                    DataStoreUtils.saveStringBlocking(context, KEY_USERNAME, name)
                    if (city.isNotBlank()) {
                        DataStoreUtils.saveStringBlocking(context, KEY_CITY, city)
                    }
                    showWelcome = false
                }
            )
        } else {
            // Transition to home
            LaunchedEffect(Unit) {
                delay(300)
                navController.navigate(NavRoutes.home.name) {
                    popUpTo(NavRoutes.welcome.name) { inclusive = true }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6BCB),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Getting everything ready...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WelcomeImageBackground(
    index: Int,
    modifier: Modifier = Modifier
) {
    val imageRes = if (index == 0) R.drawable.welcome_cubic_bg_one else R.drawable.welcome_cubic_bg_two
    Box(modifier = modifier.background(Color(0xFF050713))) {
        Crossfade(
            targetState = imageRes,
            animationSpec = tween(durationMillis = 1600),
            label = "welcomeBackground"
        ) { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f),
                        0.42f to Color(0xFF09051A).copy(alpha = 0.34f),
                        0.78f to Color(0xFF09020F).copy(alpha = 0.70f),
                        1f to Color.Black.copy(alpha = 0.92f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF4FD8).copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        ),
                        center = Offset(0.18f, 0.12f),
                        radius = 920f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.035f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeContent(
    initialCity: String = "",
    onComplete: (String, String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(initialCity) }
    var animated by remember { mutableStateOf(false) }
    
    // ADDED: Auto-scroll state
    val scrollState = rememberScrollState()
    
    // ADDED: TV navigation focus management
    val (nameFocusRequester, cityFocusRequester, buttonFocusRequester) = remember {
        FocusRequester.createRefs()
    }
    var focusedField by remember { mutableStateOf<Int?>(null) }
    
    val alpha by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "welcomeAnimation"
    )
    
    val cardElevation by animateDpAsState(
        targetValue = if (animated) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 1000)
    )
    
    // ADDED: Auto-scroll when keyboard appears
    LaunchedEffect(focusedField) {
        // When a field gets focus (keyboard likely appears), scroll to show it
        when (focusedField) {
            1 -> { // Name field focused
                // Scroll to show name field better
                delay(100) // Small delay for keyboard animation
                scrollState.animateScrollTo((scrollState.maxValue * 0.38f).roundToInt())
            }
            2 -> { // City field focused
                // Scroll down a bit to show city field better
                delay(100)
                scrollState.animateScrollTo((scrollState.maxValue * 0.58f).roundToInt())
            }
            3 -> { // Button focused
                // Scroll to bottom to show button
                delay(100)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        animated = true
        city = initialCity.ifBlank { DataStoreUtils.getStringBlocking(context, KEY_CITY, "") }
        
        // AUTO-FOCUS for TV: Start with name field focused
        delay(350)
        nameFocusRequester.requestFocus()
        focusedField = 1
    }

    LaunchedEffect(initialCity) {
        if (city.isBlank() && initialCity.isNotBlank()) {
            city = initialCity
        }
    }
    
    // ADDED: Main scrollable column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // ADDED: Enable vertical scrolling
            .padding(horizontal = 24.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top spacer for keyboard avoidance - reduced height for small screens
        Spacer(modifier = Modifier.height(20.dp))
        
        // Logo/Icon section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            // Animated gradient orb - reduced size for small screens
            Box(
                modifier = Modifier
                    .size(100.dp) // Reduced from 120dp
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6BCB),
                                Color(0xFF9C27B0),
                                Color(0xFF1A0036),
                                Color(0xFF0A0A1A)
                            ),
                            center = Offset(0.3f, 0.3f),
                            radius = 200f
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6BCB).copy(alpha = 0.8f),
                                Color(0xFF9C27B0).copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cubic),
                    contentDescription = "Cubic Logo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp) // Reduced from 56dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp)) // Reduced from 24dp
            
            // Title section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp) // Reduced padding
            ) {
                Text(
                    text = "CUBIC MUSIC",
                    color = Color.White,
                    fontSize = 18.sp, // Reduced from 20sp
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
                
                Text(
                    text = "Let's personalize your experience",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp, // Reduced from 16sp
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        
        // Input Card - Added more flexible sizing
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp) // Added vertical padding
                .graphicsLayer {
                    translationY = cardElevation.value * -0.3f
                    shadowElevation = cardElevation.value
                },
            shape = RoundedCornerShape(24.dp), // Reduced from 28dp
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0x1AFFFFFF),
                contentColor = Color.White
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(
                defaultElevation = cardElevation
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp) // Reduced from 24dp
            ) {
                // Name Input - Updated text
                Text(
                    text = "What should I call you😍?",
                    color = Color.White,
                    fontSize = 16.sp, // Reduced from 18sp
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp), // Reduced
                    lineHeight = 20.sp
                )
                
                // ADDED: TV navigation support for name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        if (it.length <= 20) {
                            name = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester)
                        .focusable()
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusedField = 1
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionDown, Key.Tab -> {
                                        cityFocusRequester.requestFocus()
                                        focusedField = 2
                                        return@onKeyEvent true
                                    }
                                    Key.Enter -> {
                                        // On TV remote, Enter should work like Tab
                                        cityFocusRequester.requestFocus()
                                        focusedField = 2
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    placeholder = { 
                        Text(
                            "Enter your 👻 nickname",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp // Added font size
                        ) 
                    },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF6BCB),
                        focusedBorderColor = Color(0xFFFF6BCB),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        errorBorderColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(14.dp), // Reduced from 16dp
                    supportingText = {
                        Text(
                            text = "${name.length}/20",
                            color = if (name.length > 20) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp // Reduced
                        )
                    },
                    isError = name.length > 20,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp) // Added text style
                )
                
                Spacer(modifier = Modifier.height(16.dp)) // Reduced from 20dp
                
                // City Input - Clearer purpose
                Text(
                    text = "Your City",
                    color = Color.White,
                    fontSize = 16.sp, // Reduced from 18sp
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp), // Reduced
                    lineHeight = 20.sp
                )
                
                // ADDED: TV navigation support for city field
                OutlinedTextField(
                    value = city,
                    onValueChange = { 
                        if (it.length <= 20) {
                            city = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(cityFocusRequester)
                        .focusable()
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusedField = 2
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionUp -> {
                                        nameFocusRequester.requestFocus()
                                        focusedField = 1
                                        return@onKeyEvent true
                                    }
                                    Key.DirectionDown, Key.Tab -> {
                                        buttonFocusRequester.requestFocus()
                                        focusedField = 3
                                        return@onKeyEvent true
                                    }
                                    Key.Enter -> {
                                        // On TV remote, Enter should work like Tab
                                        buttonFocusRequester.requestFocus()
                                        focusedField = 3
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    placeholder = { 
                        Text(
                            "e.g., Tokyo, London, New York",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp // Added font size
                        ) 
                    },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF9C27B0),
                        focusedBorderColor = Color(0xFF9C27B0),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp), // Reduced from 16dp
                    supportingText = {
                        Text(
                            text = "Used only for weather updates",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp // Reduced
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp) // Added text style
                )
                
                Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
                
                // Helper text
                Text(
                    text = "📍All features work without retaining user data.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp, // Reduced from 13sp
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }
        }
        
        // Bottom section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 30.dp, top = 8.dp) // Reduced bottom padding, added top
        ) {
            // Continue Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (name.isNotBlank() && name.length <= 20) {
                        onComplete(name.trim(), city.trim())
                    }
                },
                enabled = name.isNotBlank() && name.length <= 20,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp) // Reduced from 56dp
                    .clip(RoundedCornerShape(18.dp)) // Reduced from 20dp
                    .focusRequester(buttonFocusRequester)
                    .focusable()
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedField = 3
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    cityFocusRequester.requestFocus()
                                    focusedField = 2
                                    return@onKeyEvent true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    // On TV, Enter or D-pad center should click the button
                                    keyboardController?.hide()
                                    if (name.isNotBlank() && name.length <= 20) {
                                        onComplete(name.trim(), city.trim())
                                    }
                                    return@onKeyEvent true
                                }
                            }
                        }
                        false
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6BCB),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0x1AFFFFFF),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Text(
                    "Let's Start Listening",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, // Reduced from 17sp
                    letterSpacing = 0.3.sp, // Reduced
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp)) // Reduced from 20dp
            
            // Terms and Privacy
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you agree to our",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp, // Reduced from 12sp
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
                
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            uriHandler.openUri("https://thecub4.vercel.app/legal")
                        }
                    )
                ) {
                    Text(
                        text = "Terms of Service • Privacy Policy",
                        color = Color(0xFFFF6BCB),
                        fontSize = 11.sp, // Reduced from 12sp
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16dp
                
                // Mandatory notice
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)) // Reduced from 10dp
                        .background(
                            Color(0xFF9C27B0).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp) // Reduced padding
                ) {
                    Text(
                        text = "✨ Almost there",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp, // Reduced from 11sp
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // ADDED: Extra spacer at the bottom for better scrolling
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedFloatingElements() {
    // Simple floating elements removed for cleaner design
}

@Composable
fun CustomLiquidBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .drawWithCache {
                val width = size.width
                val height = size.height
                val time = System.currentTimeMillis() / 1500f
                
                onDrawWithContent {
                    // Base dark gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0A1A),  // Deep space black
                                Color(0xFF1A0036),  // Deep purple
                                Color(0xFF2D0049),  // Medium purple
                                Color(0xFF0A0A1A)   // Back to deep space
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )
                    
                    // Complex liquid waves with better blending
                    val waveCount = 4
                    val waveAmplitude = 35f
                    val waveSpeed = 0.25f
                    
                    for (waveIndex in 0 until waveCount) {
                        val waveOffset = waveIndex * 100f
                        val wavePhase = waveIndex * 0.7f
                        val waveHeight = height * (0.6f + waveIndex * 0.08f)
                        
                        val path = Path().apply {
                            moveTo(0f, height)
                            
                            // Create smooth wave with cubic bezier for more organic feel
                            val segments = 40
                            val segmentWidth = width / segments
                            
                            for (segment in 0..segments) {
                                val x = segment * segmentWidth
                                val progress = x / width
                                
                                // Multi-frequency sine wave for organic movement
                                val y = waveHeight + 
                                       sin(x * 0.01f + time * waveSpeed + wavePhase) * waveAmplitude +
                                       sin(x * 0.023f + time * waveSpeed * 1.3f + wavePhase) * waveAmplitude * 0.5f +
                                       cos(x * 0.005f + time * waveSpeed * 0.7f + wavePhase) * waveAmplitude * 0.3f
                                
                                if (segment == 0) {
                                    moveTo(x, y)
                                } else {
                                    // Use quadratic bezier for smoother curves
                                    val prevX = (segment - 1) * segmentWidth
                                    val prevY = waveHeight + 
                                               sin(prevX * 0.01f + time * waveSpeed + wavePhase) * waveAmplitude +
                                               sin(prevX * 0.023f + time * waveSpeed * 1.3f + wavePhase) * waveAmplitude * 0.5f +
                                               cos(prevX * 0.005f + time * waveSpeed * 0.7f + wavePhase) * waveAmplitude * 0.3f
                                    
                                    val controlX = (prevX + x) / 2
                                    val controlY = (prevY + y) / 2 - waveAmplitude * 0.5f
                                    
                                    quadraticTo(controlX, controlY, x, y)
                                }
                            }
                            
                            lineTo(width, height)
                            close()
                        }
                        
                        // Gradient wave colors
                        val waveGradient = Brush.verticalGradient(
                            0.0f to when (waveIndex) {
                                0 -> Color(0xFFFF6BCB).copy(alpha = 0.15f)
                                1 -> Color(0xFF9C27B0).copy(alpha = 0.12f)
                                2 -> Color(0xFF673AB7).copy(alpha = 0.09f)
                                else -> Color(0xFF4A1B6D).copy(alpha = 0.06f)
                            },
                            1.0f to Color.Transparent
                        )
                        
                        drawPath(
                            path = path,
                            brush = waveGradient,
                            blendMode = BlendMode.Screen
                        )
                    }
                    
                    // Floating orbs/bubbles
                    for (i in 0..15) {
                        val radius = (10 + Math.random() * 40).toFloat()
                        val x = (Math.random() * width).toFloat()
                        val y = (Math.random() * height * 0.7f).toFloat()
                        val orbTime = time * (0.5f + Math.random().toFloat() * 0.5f)
                        val floatX = x + sin(orbTime * 0.5f + i) * 30f
                        val floatY = y + cos(orbTime * 0.3f + i) * 20f
                        val pulse = 0.7f + sin(orbTime * 0.8f) * 0.3f
                        
                        val orbColor = when (i % 3) {
                            0 -> Color(0xFFFF6BCB).copy(alpha = 0.08f * pulse)
                            1 -> Color(0xFF9C27B0).copy(alpha = 0.06f * pulse)
                            else -> Color(0xFF4A1B6D).copy(alpha = 0.04f * pulse)
                        }
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    orbColor,
                                    orbColor.copy(alpha = 0f)
                                ),
                                center = Offset(floatX, floatY),
                                radius = radius * 1.5f
                            ),
                            radius = radius,
                            center = Offset(floatX, floatY),
                            blendMode = BlendMode.Screen
                        )
                    }
                    
                    // Subtle glow effect in center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6BCB).copy(alpha = 0.05f),
                                Color(0xFF9C27B0).copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(width / 2, height / 2),
                            radius = width * 0.4f
                        ),
                        radius = width * 0.4f,
                        center = Offset(width / 2, height / 2),
                        blendMode = BlendMode.Screen
                    )
                    
                    // Noise texture for depth
                    for (i in 0..80) {
                        val x = (Math.random() * width).toFloat()
                        val y = (Math.random() * height).toFloat()
                        val size = (Math.random() * 2f).toFloat()
                        val alpha = (0.01 + Math.random() * 0.03).toFloat()
                        
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = size,
                            center = Offset(x, y),
                            blendMode = BlendMode.Overlay
                        )
                    }
                    
                    // Vignette effect
                    val vignette = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF000000).copy(alpha = 0.3f)
                        ),
                        center = Offset(width / 2, height / 2),
                        radius = width * 0.8f
                    )
                    
                    drawRect(
                        brush = vignette,
                        blendMode = BlendMode.Overlay
                    )
                }
            }
    )
}

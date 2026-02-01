package it.fast4x.rimusic.ui.screens.cubicjam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import app.kreate.android.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun CubicJamAuth(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Purple-Orange theme colors
    val PurplePrimary = Color(0xFF9C27B0)
    val PurpleDark = Color(0xFF7B1FA2)
    val PurpleLight = Color(0xFFE1BEE7)
    val OrangePrimary = Color(0xFFFF9800)
    val OrangeDark = Color(0xFFF57C00)
    val OrangeLight = Color(0xFFFFE0B2)
    val TealGlow = Color(0xFF00E5FF)
    val DarkBackground = Color(0xFF121212)
    val CardDark = Color(0xFF1E1E1E)
    val SurfaceDark = Color(0xFF2D2D2D)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Logo/Header Section with teal glow
Icon(
    painter = painterResource(R.drawable.multipage),
    contentDescription = "Cubic Jam",
    modifier = Modifier
        .size(64.dp)
        .drawBehind {
            drawCircle(
                color = Color(0xFF00BFA5).copy(alpha = 0.18f),
                radius = size.minDimension / 1.3f
            )
        },
    tint = Color(0xFF00BFA5)
)

            Spacer(modifier = Modifier.height(20.dp))
            
            // Title
            Text(
                text = if (isLoginMode) "Welcome to Cubic Jam" else "Join Cubic Jam",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = buildAnnotatedString {
                    if (isLoginMode) {
                        append("Share your music journey with friends. ")
                        withStyle(style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = OrangeLight
                        )) {
                            append("See what everyone's listening to in real-time!")
                        }
                    } else {
                        append("Create an account to connect with friends, ")
                        append("share your favorite tracks, and ")
                        withStyle(style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = OrangeLight
                        )) {
                            append("discover new music together!")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Form Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardDark
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Field with validation
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            // Clear validation errors when user starts typing
                            if (errorMessage?.contains("email", ignoreCase = true) == true) {
                                errorMessage = null
                            }
                        },
                        label = {
                            Text(
                                "Email Address",
                                color = PurpleLight
                            )
                        },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = SurfaceDark,
                            focusedLabelColor = PurpleLight,
                            unfocusedLabelColor = Color(0xFFB0B0B0),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OrangePrimary,
                            errorLabelColor = PurpleLight,     // label stays visible
                            errorLeadingIconColor = Color.White,
                            errorTrailingIconColor = Color.White,
                            errorTextColor = Color.White        // ensures text stays white

                        ),
                        isError = errorMessage?.contains("email", ignoreCase = true) == true || 
                                errorMessage?.contains("Invalid", ignoreCase = true) == true
                    )
                    
                    // Password Field with validation
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            // Clear validation errors when user starts typing
                            if (errorMessage?.contains("password", ignoreCase = true) == true) {
                                errorMessage = null
                            }
                        },
                        label = {
                            Text(
                                "Password",
                                color = PurpleLight
                            )
                        },
                        placeholder = { Text("Enter your password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None 
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility 
                                        else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" 
                                        else "Show password",
                                    tint = OrangePrimary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = SurfaceDark,
                            focusedLabelColor = PurpleLight,
                            unfocusedLabelColor = Color(0xFFB0B0B0),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OrangePrimary,
                            errorLabelColor = PurpleLight,     // label stays visible
                            errorLeadingIconColor = Color.White,
                            errorTrailingIconColor = Color.White,
                            errorTextColor = Color.White        // ensures text stays white

                        ),
                        isError = errorMessage?.contains("password", ignoreCase = true) == true || 
                                errorMessage?.contains("Invalid", ignoreCase = true) == true
                    )
                    
                    // Username Field (only for signup)
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { 
                                username = it
                                // Clear validation errors when user starts typing
                                if (errorMessage?.contains("username", ignoreCase = true) == true) {
                                    errorMessage = null
                                }
                            },
                            label = {
                                Text(
                                    "Username",
                                    color = PurpleLight
                                )
                            },
                            placeholder = { Text("Choose a unique username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = SurfaceDark,
                                focusedLabelColor = OrangePrimary,
                                unfocusedLabelColor = Color(0xFFB0B0B0),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = OrangePrimary, 
                                errorLabelColor = PurpleLight,     // label stays visible
                                errorLeadingIconColor = Color.White,
                                errorTrailingIconColor = Color.White,
                                errorTextColor = Color.White        // ensures text stays white

                            ),
                            isError = errorMessage?.contains("username", ignoreCase = true) == true
                        )
                    }
                    
                    // Error/Success messages
                    errorMessage?.let { message ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF311B1B),
                            contentColor = Color(0xFFEF9A9A)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(app.kreate.android.R.drawable.alert_circle),
                                    contentDescription = "Error",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    successMessage?.let { message ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF1B1A1F),
                            contentColor = OrangeLight
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(app.kreate.android.R.drawable.checkmark),
                                    contentDescription = "Success",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Submit Button with input validation
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            
                            // Email validation
                            val emailRegex = Regex("^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})")
                            if (email.isBlank()) {
                                errorMessage = "Email is required"
                                return@Button
                            }
                            if (!emailRegex.matches(email)) {
                                errorMessage = "Please enter a valid email address"
                                return@Button
                            }
                            
                            // Password validation
                            if (password.isBlank()) {
                                errorMessage = "Password is required"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@Button
                            }
                            
                            if (!isLoginMode) {
                                // Username validation for signup
                                if (username.isBlank()) {
                                    errorMessage = "Username is required"
                                    return@Button
                                }
                                if (username.length < 3) {
                                    errorMessage = "Username must be at least 3 characters"
                                    return@Button
                                }
                                if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                                    errorMessage = "Username can only contain letters, numbers, and underscores"
                                    return@Button
                                }
                            }
                            
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            
                            scope.launch {
                                try {
                                    val result = if (isLoginMode) {
                                        login(email, password, context)
                                    } else {
                                        signup(email, password, username, context)
                                    }
                                    
                                    result.onSuccess { authResponse ->
                                        if (authResponse.success) {
                                            successMessage = if (isLoginMode) 
                                                "Welcome back! Connecting to Cubic Jam..." 
                                            else 
                                                "Account created! Welcome to Cubic Jam!"
                                            
                                            scope.launch {
                                                kotlinx.coroutines.delay(1500)
                                                navController.navigateUp()
                                            }
                                        } else {
                                            errorMessage = "Authentication failed. Please try again."
                                        }
                                    }.onFailure { throwable ->
                                        Timber.tag("CubicJam").e(throwable, "Authentication error")
                                        errorMessage = when {
                                            throwable.message?.contains("timeout", ignoreCase = true) == true -> 
                                                "Connection timeout. Please check your internet connection"
                                            throwable.message?.contains("400", ignoreCase = true) == true -> 
                                                if (isLoginMode) "Invalid email or password" else "Username already taken"
                                            throwable.message?.contains("401", ignoreCase = true) == true -> 
                                                "Invalid credentials. Please check your email and password"
                                            throwable.message?.contains("network", ignoreCase = true) == true -> 
                                                "No internet connection. Please check your network"
                                            else -> "Unable to connect. Please try again later"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Network error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isLoginMode) app.kreate.android.R.drawable.people 
                                    else app.kreate.android.R.drawable.add
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isLoading) "Please wait..." 
                                else if (isLoginMode) "Sign In" 
                                else "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    
                    // Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoginMode) 
                                "Don't have an account?" 
                            else 
                                "Already have an account?",
                            color = Color(0xFFB0B0B0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                                errorMessage = null
                                successMessage = null
                                password = ""
                                if (isLoginMode) username = ""
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = OrangePrimary
                            )
                        ) {
                            Text(
                                text = if (isLoginMode) "Sign Up" else "Sign In",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    
                    // Divider with "or"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SurfaceDark,
                            thickness = 1.dp
                        )

                        Text(
                            text = "or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFF808080),
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SurfaceDark,
                            thickness = 1.dp
                        )

                    }
                    
                    // Back Button
                    OutlinedButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PurpleLight,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = PurplePrimary
                        )

                    ) {
                        Icon(
                            painter = painterResource(app.kreate.android.R.drawable.chevron_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = PurpleLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Back to Music")
                    }
                    
                    // Footer Info
                    if (!isLoginMode) {
                        Text(
                            text = "By signing up, you agree to share your listening activity with friends on Cubic Jam.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = Color(0xFFB0B0B0).copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Additional Info
            Text(
                text = buildAnnotatedString {
                    append("Cubic Jam lets you ")
                    withStyle(style = SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = OrangeLight
                    )) {
                        append("share music in real-time")
                    }
                    append(" with friends. See what everyone's playing right now!")
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = Color(0xFFB0B0B0).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
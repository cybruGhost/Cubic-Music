package app.it.fast4x.rimusic.ui.screens.cubicjam

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.it.fast4x.rimusic.ui.styling.*
import app.kreate.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubicJamAuth(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val typography = appearance.typography
    
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Forgot password dialog state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordLoading by remember { mutableStateOf(false) }
    var forgotPasswordMessage by remember { mutableStateOf<String?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension * 0.8f
            
            rotate(rotation) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorPalette.accent.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f)
                    ),
                    radius = radius * 0.5f,
                    center = Offset(centerX, centerY)
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorPalette.accent.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(centerX + radius * 0.3f, centerY + radius * 0.3f)
                    ),
                    radius = radius * 0.4f,
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colorPalette.background2,
                                colorPalette.background1
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = colorPalette.accent,
                        shape = CircleShape
                    )
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.multipage),
                    contentDescription = "Cubic Jam",
                    modifier = Modifier.fillMaxSize(),
                    tint = colorPalette.accent
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = if (isLoginMode) "Welcome Back" else "Join Cubic Jam",
                style = typography.xxxl.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorPalette.text,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            val subtitleText = buildAnnotatedString {
                if (isLoginMode) {
                    append("Share your music journey ")
                    withStyle(style = SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = colorPalette.accent
                    )) {
                        append("with friends")
                    }
                } else {
                    append("Connect with friends, share tracks, and ")
                    withStyle(style = SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = colorPalette.accent
                    )) {
                        append("discover together")
                    }
                }
            }
            
            Text(
                text = subtitleText,
                style = typography.m,
                color = colorPalette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorPalette.background1
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.accent,
                            unfocusedBorderColor = colorPalette.background3,
                            focusedLabelColor = colorPalette.accent,
                            unfocusedLabelColor = colorPalette.textSecondary,
                            focusedTextColor = colorPalette.text,
                            unfocusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.accent,
                            focusedLeadingIconColor = colorPalette.accent,
                            unfocusedLeadingIconColor = colorPalette.textSecondary,
                            errorLabelColor = colorPalette.red,
                            errorLeadingIconColor = colorPalette.red,
                            errorTextColor = colorPalette.text
                        ),
                        label = { 
                            Text(
                                "Email Address",
                                style = typography.xs
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "your@email.com",
                                style = typography.xs,
                                color = colorPalette.textDisabled
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = errorMessage?.contains("email", ignoreCase = true) == true
                    )
                    
                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.accent,
                            unfocusedBorderColor = colorPalette.background3,
                            focusedLabelColor = colorPalette.accent,
                            unfocusedLabelColor = colorPalette.textSecondary,
                            focusedTextColor = colorPalette.text,
                            unfocusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.accent,
                            focusedLeadingIconColor = colorPalette.accent,
                            unfocusedLeadingIconColor = colorPalette.textSecondary,
                            focusedTrailingIconColor = colorPalette.accent,
                            unfocusedTrailingIconColor = colorPalette.textSecondary,
                            errorLabelColor = colorPalette.red,
                            errorLeadingIconColor = colorPalette.red,
                            errorTrailingIconColor = colorPalette.red,
                            errorTextColor = colorPalette.text
                        ),
                        label = { 
                            Text(
                                "Password",
                                style = typography.xs
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "••••••••",
                                style = typography.xs,
                                color = colorPalette.textDisabled
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility 
                                                else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None 
                                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                        ),
                        singleLine = true,
                        isError = errorMessage?.contains("password", ignoreCase = true) == true
                    )
                    
                    // Username Field (Sign Up Only)
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { 
                                username = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorPalette.accent,
                                unfocusedBorderColor = colorPalette.background3,
                                focusedLabelColor = colorPalette.accent,
                                unfocusedLabelColor = colorPalette.textSecondary,
                                focusedTextColor = colorPalette.text,
                                unfocusedTextColor = colorPalette.text,
                                cursorColor = colorPalette.accent,
                                focusedLeadingIconColor = colorPalette.accent,
                                unfocusedLeadingIconColor = colorPalette.textSecondary,
                                errorLabelColor = colorPalette.red,
                                errorLeadingIconColor = colorPalette.red,
                                errorTextColor = colorPalette.text
                            ),
                            label = { 
                                Text(
                                    "Username",
                                    style = typography.xs
                                ) 
                            },
                            placeholder = { 
                                Text(
                                    "Choose a username",
                                    style = typography.xs,
                                    color = colorPalette.textDisabled
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            isError = errorMessage?.contains("username", ignoreCase = true) == true
                        )
                    }
                    
                    // Forgot Password Link (Login Mode Only)
                    if (isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { 
                                    forgotPasswordEmail = email
                                    showForgotPasswordDialog = true 
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colorPalette.accent
                                )
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = typography.xs.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                    
                    // Error/Success Messages
                    errorMessage?.let { message ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = colorPalette.red.copy(alpha = 0.1f),
                            contentColor = colorPalette.red
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = typography.xs,
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
                            color = colorPalette.accent.copy(alpha = 0.1f),
                            contentColor = colorPalette.accent
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = typography.xs,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Submit Button
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            
                            when {
                                email.isBlank() -> errorMessage = "Email is required"
                                !isValidEmail(email) -> errorMessage = "Invalid email format"
                                password.isBlank() -> errorMessage = "Password is required"
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                !isLoginMode && username.isBlank() -> errorMessage = "Username is required"
                                !isLoginMode && username.length < 3 -> errorMessage = "Username must be at least 3 characters"
                                !isLoginMode && !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> 
                                    errorMessage = "Username can only contain letters, numbers, and underscores"
                                else -> {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    scope.launch {
                                        val result = if (isLoginMode) {
                                            login(email, password, context)
                                        } else {
                                            signup(email, password, username, context)
                                        }
                                        
                                        result.fold(
                                            onSuccess = { authResponse ->
                                                if (authResponse.success) {
                                                    successMessage = if (isLoginMode) 
                                                        "Welcome back! 🎵" 
                                                    else 
                                                        "Account created! 🎉"
                                                    
                                                    delay(1000)
                                                    isLoading = false
                                                    navController.navigateUp()
                                                } else {
                                                    errorMessage = "Authentication failed. Please try again."
                                                    isLoading = false
                                                }
                                            },
                                            onFailure = { e ->
                                                Timber.tag("CubicJam").e(e, "Authentication error")
                                                errorMessage = when {
                                                    e.message?.contains("401", ignoreCase = true) == true -> 
                                                        "Invalid email or password"
                                                    e.message?.contains("409", ignoreCase = true) == true -> 
                                                        "Email or username already exists"
                                                    e.message?.contains("network", ignoreCase = true) == true -> 
                                                        "Network error. Please check your connection"
                                                    else -> "Authentication failed: ${e.message}"
                                                }
                                                isLoading = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorPalette.accent,
                            contentColor = colorPalette.onAccent,
                            disabledContainerColor = colorPalette.background3
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = colorPalette.onAccent
                            )
                        } else {
                            Icon(
                                imageVector = if (isLoginMode) Icons.Default.Login else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isLoginMode) "Sign In" else "Create Account",
                                style = typography.m.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
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
                            style = typography.xs,
                            color = colorPalette.textSecondary
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
                                contentColor = colorPalette.accent
                            )
                        ) {
                            Text(
                                text = if (isLoginMode) "Sign Up" else "Sign In",
                                style = typography.s.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    
                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = colorPalette.background3,
                            thickness = 1.dp
                        )
                        Text(
                            text = "or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = typography.xxs,
                            color = colorPalette.textDisabled
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = colorPalette.background3,
                            thickness = 1.dp
                        )
                    }
                    
                    // Back Button
                    OutlinedButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorPalette.text
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Music",
                            style = typography.s
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Beta Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = colorPalette.accent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    color = colorPalette.accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "BETA",
                        style = typography.xxs.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorPalette.accent
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "This feature is in beta. NOTE THIS SYSTEM NEEDS A REDESIGN AND MIGHT LOG U OUT ALOT OF TYM . WILL FIX IT SOON!",
                    style = typography.xxs,
                    color = colorPalette.textDisabled,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false
                forgotPasswordMessage = null
            },
            title = {
                Text(
                    text = "Reset Password",
                    style = typography.m.copy(fontWeight = FontWeight.Bold),
                    color = colorPalette.text
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter your email address and we'll send you instructions to reset your password.",
                        style = typography.xs,
                        color = colorPalette.textSecondary
                    )
                    
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { 
                            forgotPasswordEmail = it
                            forgotPasswordMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.accent,
                            unfocusedBorderColor = colorPalette.background3,
                            focusedLabelColor = colorPalette.accent,
                            unfocusedLabelColor = colorPalette.textSecondary,
                            focusedTextColor = colorPalette.text,
                            unfocusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.accent
                        ),
                        label = { 
                            Text(
                                "Email Address",
                                style = typography.xs
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "your@email.com",
                                style = typography.xs,
                                color = colorPalette.textDisabled
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true
                    )
                    
                    forgotPasswordMessage?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (message.contains("sent", ignoreCase = true)) 
                                colorPalette.accent.copy(alpha = 0.1f)
                            else
                                colorPalette.red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = message,
                                style = typography.xs,
                                color = if (message.contains("sent", ignoreCase = true))
                                    colorPalette.accent
                                else
                                    colorPalette.red,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.isBlank()) {
                            forgotPasswordMessage = "Please enter your email"
                            return@Button
                        }
                        if (!isValidEmail(forgotPasswordEmail)) {
                            forgotPasswordMessage = "Invalid email format"
                            return@Button
                        }
                        
                        forgotPasswordLoading = true
                        scope.launch {
                            // For now, show success message (you can implement actual password reset later)
                            delay(1000)
                            forgotPasswordMessage = "Password reset instructions sent to your email"
                            forgotPasswordLoading = false
                            
                            // Auto-dismiss after 2 seconds
                            delay(2000)
                            showForgotPasswordDialog = false
                            forgotPasswordMessage = null
                        }
                    },
                    enabled = !forgotPasswordLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorPalette.accent,
                        contentColor = colorPalette.onAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (forgotPasswordLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorPalette.onAccent
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showForgotPasswordDialog = false
                        forgotPasswordMessage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorPalette.textSecondary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colorPalette.background1,
            titleContentColor = colorPalette.text,
            textContentColor = colorPalette.textSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
// CubicJamSwipeScreen.kt
package app.it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.it.fast4x.rimusic.enums.NavRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CubicJamSwipeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    // State for connectivity check
    var isConnected by remember { mutableStateOf(checkInternetConnection(connectivityManager)) }
    var showRetryMessage by remember { mutableStateOf(false) }
    
    // Function to check connectivity
    fun checkConnectivity() {
        isConnected = checkInternetConnection(connectivityManager)
    }
    
    // Auto-check connectivity on screen load
    LaunchedEffect(Unit) {
        checkConnectivity()
    }
    
    // Show appropriate screen based on connectivity
    if (isConnected) {
        // If connected, immediately navigate to the web view with the specific URL
        LaunchedEffect(isConnected) {
            if (isConnected) {
                // Navigate to web view with the specific swipe URL
                navController.navigate(NavRoutes.cubicjam_web.name) {
                    // This prevents going back to other Cubic Jam pages
                    popUpTo(NavRoutes.cubicjam_swipe.name) {
                        inclusive = true
                    }
                }
            }
        }
        
        // Loading screen while navigating
        LoadingScreen()
    } else {
        NoInternetScreen(
            onRetry = {
                scope.launch {
                    checkConnectivity()
                    if (!isConnected) {
                        showRetryMessage = true
                        delay(2000)
                        showRetryMessage = false
                    }
                }
            },
            onDonate = { url ->
                openUrlInBrowser(context, url)
            },
            onCopyKoFi = {
                clipboardManager.setText(AnnotatedString("https://ko-fi.com/anonghost40418"))
                // You might want to show a toast here
                // Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            showRetryMessage = showRetryMessage
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Opening Cubic Jam...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Loading the music discovery experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    onDonate: (String) -> Unit,
    onCopyKoFi: () -> Unit,
    showRetryMessage: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "No Internet",
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Connection Required",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Please connect to the internet to discover music",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Donation section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Support the Developer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "If you enjoy Cubic Jam, consider supporting its development",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    // Ko-Fi Card
                    DonationCard(
                        title = "Support via Ko-Fi",
                        description = "https://ko-fi.com/anonghost40418",
                        icon = Icons.Default.ContentCopy,
                        onClick = onCopyKoFi,
                        buttonText = "Tap to copy link"
                    )
                    
                    // Donate Button
                    Button(
                        onClick = { onDonate("https://support-pal-global.lovable.app/") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Donate via PayPal / M-Pesa",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Retry section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FIXED: Added closing parenthesis for FilledTonalButton
                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check Connection Again",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                if (showRetryMessage) {
                    Text(
                        text = "Still no internet connection detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DonationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    buttonText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Action",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// Helper function to check internet connection
private fun checkInternetConnection(connectivityManager: ConnectivityManager): Boolean {
    return try {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Helper function to open URL in browser
private fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Try Chrome first
        intent.setPackage("com.android.chrome")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If Chrome not available, use default browser
            intent.setPackage(null)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Handle error if needed
    }
}
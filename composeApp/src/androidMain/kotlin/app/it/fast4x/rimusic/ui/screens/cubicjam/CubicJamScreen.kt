package app.it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubicJamScreen(
    navController: NavController,
    cubicJamManager: CubicJamManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }
    
    // Load from SharedPreferences
    val preferences = remember { 
        context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE) 
    }
    
    // State variables
    var isLoggedIn by remember { 
        mutableStateOf(preferences.getString("bearer_token", null) != null) 
    }
    var username by remember { 
        mutableStateOf(preferences.getString("username", null)) 
    }
    var displayName by remember { 
        mutableStateOf(preferences.getString("display_name", null)) 
    }
    var friendCode by remember { 
        mutableStateOf(preferences.getString("friend_code", null)) 
    }
    var avatarUrl by remember { 
        mutableStateOf(preferences.getString("avatar_url", null)) 
    }
    
    // Friends state
    var friendsActivity by remember { mutableStateOf<List<FriendActivity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Stats state
    var onlineFriendsCount by remember { mutableStateOf(0) }
    var totalFriendsCount by remember { mutableStateOf(0) }
    var listeningFriendsCount by remember { mutableStateOf(0) }
    
    // Function to fetch friends
    suspend fun fetchFriends() {
        if (!isLoggedIn) {
            errorMessage = "Not logged in"
            return
        }
        
        try {
            isLoading = true
            errorMessage = null
            
            val result = refreshFriendsActivity(preferences, json)
            if (result != null && result.success) {
                // Get the activity list from the API response
                val activities = result.activity
                
                // Update stats from API response
                onlineFriendsCount = activities.count { it.is_online }
                totalFriendsCount = result.total_friends
                listeningFriendsCount = result.friends_listening
                
                // Convert FriendActivityResponseItem to FriendActivity objects
                val friendActivities = activities.map { activityItem ->
                    FriendActivity(
                        profile = activityItem.profile,
                        activity = Activity(
                            track_id = activityItem.track_id,
                            title = activityItem.title,
                            artist = activityItem.artist,
                            album = activityItem.album,
                            artwork_url = activityItem.artwork_url,
                            is_playing = activityItem.is_playing,
                            position_ms = activityItem.position_ms,
                            duration_ms = activityItem.duration_ms,
                            updated_at = activityItem.updated_at
                        ),
                        is_online = activityItem.is_online
                    )
                }
                
                // Filter out current user if we have username
                val filteredFriends = username?.let { currentUsername ->
                    friendActivities.filter { 
                        it.profile.username != currentUsername 
                    }
                } ?: friendActivities
                
                // Sort: online first, then by most recent activity
                friendsActivity = filteredFriends.sortedByDescending { friend ->
                    (if (friend.is_online) 1000 else 0) + 
                    (friend.activity?.updated_at?.let { 
                        try {
                            Instant.parse(it).toEpochMilli()
                        } catch (e: Exception) {
                            0L
                        }
                    } ?: 0)
                }
                
                if (filteredFriends.isEmpty()) {
                    errorMessage = "No friends found. Add friends to see their activity!"
                } else {
                    errorMessage = null
                    Timber.tag("CubicJam").d("Fetched ${filteredFriends.size} friends, ${onlineFriendsCount} online, ${listeningFriendsCount} listening")
                }
            } else {
                errorMessage = "Failed to load friends. Check your connection."
                Timber.tag("CubicJam").e("fetchFriends: Result is null or not successful")
            }
        } catch (e: Exception) {
            Timber.tag("CubicJam").e(e, "fetchFriends: Failed completely")
            errorMessage = "Error: ${e.message}"
            friendsActivity = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    // Function to logout
    fun logout() {
        preferences.edit().clear().apply()
        refreshJob?.cancel()
        isLoggedIn = false
        username = null
        displayName = null
        friendCode = null
        friendsActivity = emptyList()
        onlineFriendsCount = 0
        totalFriendsCount = 0
        listeningFriendsCount = 0
        showLogoutDialog = false
        Timber.tag("CubicJam").d("User logged out")
    }
    
    // Initial fetch
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            fetchFriends()
            
            // Start periodic refresh
            refreshJob = scope.launch {
                while (true) {
                    delay(30000) // 30 seconds
                    if (isLoggedIn) {
                        fetchFriends()
                    } else {
                        break
                    }
                }
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            refreshJob?.cancel()
            refreshJob = null
        }
    }
    
    // If not logged in, show auth screen
    if (!isLoggedIn) {
        CubicJamAuth(navController = navController)
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(cubicJamBackground, cubicJamSurface)
                )
            )
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(cubicJamPrimary, cubicJamSecondary, cubicJamTertiary),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                // User header
                UserHeaderSection(
                    displayName = displayName,
                    username = username,
                    friendCode = friendCode,
                    avatarUrl = avatarUrl,
                    onLogoutClick = { showLogoutDialog = true },
                    onProfileClick = {
                        username?.let { name ->
                            navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/$name")
                        }
                    }
                )
                
                // Stats cards - use counts from API response
                StatsSection(
                    onlineFriends = onlineFriendsCount,
                    totalFriends = totalFriendsCount,
                    listeningFriends = listeningFriendsCount
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (errorMessage != null) {
                item {
                    ErrorCard(
                        message = errorMessage ?: "",
                        onDismiss = { errorMessage = null }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            if (isLoading && friendsActivity.isEmpty()) {
                item {
                    LoadingSection()
                }
            } else if (friendsActivity.isEmpty()) {
                item {
                    EmptyFriendsSection(
                        onAddFriendClick = { 
                            navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/feed/") 
                        }
                    )
                }
            } else {
                // Online friends section
                val onlineFriends = friendsActivity.filter { it.is_online }
                if (onlineFriends.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Online Now",
                            subtitle = "${onlineFriends.size} friends online",
                            icon = R.drawable.wifi
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Use items function correctly
                    items(onlineFriends) { friend ->
                        FriendCard(
                            friend = friend,
                            onClick = {
                                navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/${friend.profile.username}")
                            },
                            isOnline = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Offline friends section
                val offlineFriends = friendsActivity.filter { !it.is_online }
                if (offlineFriends.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            title = "Offline",
                            subtitle = "${offlineFriends.size} friends offline",
                            icon = R.drawable.alert_circle // Use available icon
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Use items function correctly
                    items(offlineFriends) { friend ->
                        FriendCard(
                            friend = friend,
                            onClick = {
                                navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/profile/${friend.profile.username}")
                            },
                            isOnline = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        // Floating Action Buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
               .padding(end = 24.dp, bottom = 80.dp), // more bottom padding to raise FABs
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically // align row contents to center
            ) {
                // Refresh button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            fetchFriends()
                        }
                    },
                    containerColor = cubicJamSurface,
                    contentColor = cubicJamOnSurface,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = cubicJamTertiary
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.refresh),
                            contentDescription = "Refresh",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Add friend button - use available icon
                FloatingActionButton(
                    onClick = { navController.navigate("cubicjam_web?url=https://jam-wave-connect.lovable.app/feed/") },
                    containerColor = cubicJamPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add), // Use available 'add' icon instead of person_add
                        contentDescription = "Add Friend",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = cubicJamOnSurface) },
            text = { Text("Are you sure you want to logout?", color = cubicJamOnSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = { logout() },
                    colors = ButtonDefaults.textButtonColors(contentColor = cubicJamError)
                ) {
                    Text("LOGOUT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = cubicJamOnSurfaceVariant)
                ) {
                    Text("CANCEL")
                }
            },
            containerColor = cubicJamSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// Add this function to help with date parsing in the composable
@Composable
fun rememberFormattedTime(updatedAt: String?): String? {
    return remember(updatedAt) {
        updatedAt?.let {
            try {
                val instant = Instant.parse(it)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                dateTime.format(formatter)
            } catch (e: Exception) {
                null
            }
        }
    }
}
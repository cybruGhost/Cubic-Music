package app.it.fast4x.rimusic.ui.screens.cubicjam

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import app.kreate.android.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable

// Add this NEW data class that matches your API response
@Serializable
data class FriendsActivityAPIResponse(
    val success: Boolean,
    val activity: List<FriendActivityResponseItem>,
    val friends_listening: Int,
    val total_friends: Int
)

@Serializable
data class FriendActivityResponseItem(
    val id: String,
    val user_id: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artwork_url: String? = null,
    val duration_ms: Long,
    val position_ms: Long,
    val is_playing: Boolean,
    val started_at: String,
    val updated_at: String,
    val profile: FriendProfile,
    val is_online: Boolean
)

// Keep all your existing data classes below
@Serializable
data class FriendActivity(
    val profile: FriendProfile,
    val activity: Activity?,
    val is_online: Boolean
)

@Serializable
data class FriendProfile(
    val id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class Activity(
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artwork_url: String? = null,
    val is_playing: Boolean,
    val position_ms: Long? = null,
    val duration_ms: Long? = null,
    val updated_at: String
)

@Serializable
data class UserProfileResponse(
    val profile: UserProfile,
    val recent_tracks: List<RecentTrack>,
    val current_activity: CurrentActivity? = null,
    val is_friend: Boolean
)

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val friend_code: String,
    val is_activity_public: Boolean
)

@Serializable
data class RecentTrack(
    val id: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artwork_url: String? = null,
    val listened_at: String
)

@Serializable
data class CurrentActivity(
    val track_id: String,
    val title: String,
    val artist: String,
    val is_playing: Boolean,
    val artwork_url: String? = null
)

// You can keep this for backward compatibility if needed elsewhere
@Serializable
data class FriendsActivityResponse(
    val friends: List<FriendActivity>
)

@Serializable
data class FriendRequest(
    val action: String,
    val friend_code: String? = null,
    val friendship_id: String? = null
)

@Serializable
data class FriendResponse(
    val success: Boolean,
    val message: String? = null,
    val friendship: Map<String, String>? = null
)

// Authentication data classes
@Serializable
data class AuthResponse(
    val success: Boolean,
    val user: UserInfo? = null,
    val session: SessionInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val profile_id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val friend_code: String
)

@Serializable
data class SessionInfo(
    val access_token: String,
    val refresh_token: String,
    val expires_at: Long,
    val expires_in: Int,
    val refresh_at: Long? = null,
    val token_type: String = "Bearer"
)

@Serializable
data class RefreshResponse(
    val success: Boolean,
    val session: SessionInfo
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class ActivityUpdate(
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artwork_url: String? = null,
    val duration_ms: Long,
    val position_ms: Long,
    val is_playing: Boolean
)

@Serializable
data class ActivityResponse(
    val success: Boolean,
    val activity: ActivityInfo? = null
)

@Serializable
data class ActivityInfo(
    val id: String,
    val user_id: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val is_playing: Boolean,
    val updated_at: String
)

// Add FriendsListResponse and related data classes
@Serializable
data class FriendsListResponse(
    val friends: List<FriendShip>,
    val pending_received: List<PendingRequest>,
    val pending_sent: List<PendingRequest>
)

@Serializable
data class FriendShip(
    val id: String,
    val friend: FriendProfile,
    val status: String,
    val created_at: String
)

@Serializable
data class PendingRequest(
    val id: String,
    val from: FriendProfile,
    val created_at: String
)
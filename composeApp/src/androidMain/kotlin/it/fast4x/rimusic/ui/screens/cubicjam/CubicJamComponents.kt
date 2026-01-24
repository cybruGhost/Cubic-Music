package it.fast4x.rimusic.ui.screens.cubicjam

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

// Data classes for API responses
import kotlinx.serialization.Serializable

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
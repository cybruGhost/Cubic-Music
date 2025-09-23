package it.fast4x.piped.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Instance(
    val name: String,
    @SerialName("api_url")
    val apiBaseUrl: UrlString,
    @SerialName("locations")
    val locationsFormatted: String,
    val version: String,
    @SerialName("up_to_date")
    val upToDate: Boolean,
    @SerialName("cdn")
    val isCdn: Boolean,
    @SerialName("registered")
    val userCount: Long,
    @SerialName("last_checked")
    val lastChecked: DateTimeSeconds,
    @SerialName("cache")
    val hasCache: Boolean,
    @SerialName("s3_enabled")
    val usesS3: Boolean,
    @SerialName("image_proxy_url")
    val imageProxyBaseUrl: UrlString,
    @SerialName("registration_disabled")
    val registrationDisabled: Boolean
)

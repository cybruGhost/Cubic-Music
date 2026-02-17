package me.knighthat.coil

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.kreate.android.R
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import coil3.toBitmap
import coil3.request.allowHardware
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.State
import coil3.compose.rememberAsyncImagePainter
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.enums.CoilDiskCacheMaxSize
import it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import it.fast4x.rimusic.thumbnail
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.utils.coilCustomDiskCacheKey
import it.fast4x.rimusic.utils.coilDiskCacheMaxSizeKey
import it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import it.fast4x.rimusic.utils.isConnectionMeteredEnabledKey
import it.fast4x.rimusic.utils.imageQualityFormatKey
import it.fast4x.rimusic.enums.ImageQualityFormat
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.sync.Mutex
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import android.content.Context
import java.security.MessageDigest

@OptIn(ExperimentalCoilApi::class)
object ImageCacheFactory {

    private const val TAG = "ImageCacheFactory"

    private val DISK_CACHE: DiskCache by lazy {
        val preferences = appContext().preferences
        val diskSize = preferences.getEnum( coilDiskCacheMaxSizeKey, CoilDiskCacheMaxSize.`128MB` )

        val cacheLocation = preferences.getEnum( exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System )

        val cacheDir = when( cacheLocation ) {
            ExoPlayerCacheLocation.System -> appContext().cacheDir
            ExoPlayerCacheLocation.Private -> appContext().filesDir
        }.resolve( "coil" )

        val maxSizeBytes = when( diskSize ) {
            CoilDiskCacheMaxSize.Custom -> {
                val customSize = preferences.getInt( coilCustomDiskCacheKey, 128 )
                val bytes = customSize.times( 1000L ).times( 1000 )
                bytes
            }
            else -> {
                diskSize.bytes
            }
        }

        val diskCache = DiskCache.Builder()
                 .directory( cacheDir.toPath().toOkioPath() )
                 .maxSizeBytes( maxSizeBytes )
                 .build()
        
        diskCache
    }

    // 900 is too small for some devices, 1200 is a good compromise
    const val THUMBNAIL_SIZE = 1200;

    enum class NetworkQuality(val size: Int, val ttl: Long) {
        LOW(300, TimeUnit.HOURS.toMillis(1)),       // 1 hour
        MEDIUM(720, TimeUnit.HOURS.toMillis(24)),   // 24 hours
        HIGH(1200, TimeUnit.DAYS.toMillis(14))       // 14 days
    }

    data class CacheMetadata(
        val quality: NetworkQuality,
        val timestamp: Long
    )

    data class DownloadDecision(
        val useNetwork: Boolean,
        val quality: NetworkQuality
    )

    // Cooldown to prevent download loops (30 minutes)
    private const val COOLDOWN_MS = 30 * 60 * 1000L
    private val cooldownMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private object CacheMetadataStore {
        private const val PREFS_NAME = "image_cache_metadata"
        
        private fun getPrefs() = appContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun save(url: String, quality: NetworkQuality) {
            try {
                val key = url.hashCode().toString()
                val value = "${quality.name}:${System.currentTimeMillis()}"
                getPrefs().edit().putString(key, value).apply()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error saving metadata")
            }
        }

        fun get(url: String): CacheMetadata? {
            try {
                val key = url.hashCode().toString()
                val value = getPrefs().getString(key, null) ?: return null
                val parts = value.split(":")
                if (parts.size != 2) return null
                
                val quality = NetworkQuality.valueOf(parts[0])
                val timestamp = parts[1].toLong()
                return CacheMetadata(quality, timestamp)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error getting metadata")
                return null
            }
        }
    }

    val LOADER: ImageLoader by lazy {
        // HTTP client configuration with timeout of 15 seconds and retry
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Retry Automatic in case of connection failure
            .build()
        
        ImageLoader.Builder( appContext() )
                   .crossfade( true )
                   .diskCachePolicy( CachePolicy.ENABLED )
                   .networkCachePolicy( CachePolicy.ENABLED )
                   .memoryCachePolicy( CachePolicy.ENABLED )
                   .diskCache( DISK_CACHE )
                   .components {
                       add(OkHttpNetworkFetcherFactory(httpClient))
                   }
                   .build()
    }

    // Cache pour les clés MD5 pour éviter de recalculer
    private val cacheKeyMutex = Mutex()
    private val cacheKeyMap = mutableMapOf<String, String>()

    /**
     * Generate a stable cache key for URLs with caching
     */
    private suspend fun generateCacheKey(url: String?, quality: NetworkQuality): String {
        return generateCacheKeySync(url, quality)
    }

    /**
     * Synchronous version of generateCacheKey for Composable functions
     */
    private fun generateCacheKeySync(url: String?, quality: NetworkQuality): String {
        if (url.isNullOrBlank()) {
            return "empty"
        }
        
        val size = quality.size
        
        val key = try {
            val processedUrl = url.thumbnail(size) ?: url
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(processedUrl.toByteArray())
            val result = digest.fold("") { str, it -> str + "%02x".format(it) }
            result
        } catch (e: Exception) {
            val fallbackKey = "${url.hashCode()}_${size}"
            Timber.tag(TAG).e(e, "Error generating MD5 key, using fallback: $fallbackKey")
            fallbackKey
        }
        
        return key
    }

    /**
     * Get network quality level
     */
    private fun getNetworkQuality(): NetworkQuality {
        val context = appContext()
        val isMeteredEnabled = context.preferences.getBoolean(isConnectionMeteredEnabledKey, false)
        val forcedQuality = context.preferences.getEnum(imageQualityFormatKey, ImageQualityFormat.Auto)
        
        // If user forced a quality, we use it directly (respecting manual override)
        if (forcedQuality != ImageQualityFormat.Auto) {
            return when(forcedQuality) {
                ImageQualityFormat.High -> NetworkQuality.HIGH
                ImageQualityFormat.Medium -> NetworkQuality.MEDIUM
                ImageQualityFormat.Low -> NetworkQuality.LOW
                else -> NetworkQuality.LOW
            }
        }

        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return NetworkQuality.LOW
            
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkQuality.LOW
            
            // Basic connectivity check
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            if (!hasInternet || !isValidated) {
                return NetworkQuality.LOW
            }
            
            // Transport type check
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            
            // If user enabled "Metered Connection" setting and we are on cellular (or actually metered), force LOW/MEDIUM
            // Actually, "isConnectionMeteredEnabled" in UI usually means "Treat cellular as metered" or "Save data"
            // Let's assume if it's true, we should be conservative on cellular.
            // But wait, the key name is `isConnectionMeteredEnabledKey`. In RimUSic it usually means "Limit data usage".
            // Let's check capabilities.
            val isMeteredNetwork = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            
            if (isMeteredEnabled && (isCellular || isMeteredNetwork)) {
                 // User wants to save data on metered networks.
                 // We cap at MEDIUM or LOW? Let's cap at MEDIUM for 4G, LOW for others.
                 // Or just downgrade everything by one step?
                 // Simple approach: On metered connections with "Data Saver" on, never go above MEDIUM.
                 // If bandwidth is low, go LOW.
            }

            // Link speed check (approx)
            val bandwidthKbps = capabilities.linkDownstreamBandwidthKbps
            
            // Connection quality assessment
            var quality = when {
                isWifi -> {
                    val notMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val notRoaming = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    val basicWifiCheck = notMetered || notRoaming
                    
                    if (basicWifiCheck && bandwidthKbps > 50_000) NetworkQuality.HIGH
                    else if (basicWifiCheck && bandwidthKbps > 5_000) NetworkQuality.MEDIUM
                    else NetworkQuality.LOW
                }
                isCellular -> {
                    val notRoaming = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    
                    if (notRoaming && bandwidthKbps > 50_000) NetworkQuality.HIGH // 5G
                    else if (notRoaming && bandwidthKbps > 5_000) NetworkQuality.MEDIUM // 4G/LTE
                    else NetworkQuality.LOW // 3G/2G or roaming
                }
                else -> NetworkQuality.LOW
            }
            
            // Apply User Preference Override
            if (isMeteredEnabled && (isCellular || isMeteredNetwork)) {
                if (quality == NetworkQuality.HIGH) {
                    quality = NetworkQuality.MEDIUM
                }
            }
            
            quality
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking network quality")
            NetworkQuality.LOW
        }
    }

    fun getCurrentNetworkQuality(): NetworkQuality {
        return getNetworkQuality()
    }

    /**
     * Determine if we should use network and which quality to target
     */
    fun getDownloadDecision(thumbnailUrl: String?): DownloadDecision {
        if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            return DownloadDecision(false, NetworkQuality.LOW)
        }
        
        // Cooldown check (memory cache)
        val lastAttempt = cooldownMap[thumbnailUrl]
        val now = System.currentTimeMillis()
        if (lastAttempt != null && (now - lastAttempt) < COOLDOWN_MS) {
             val cached = CacheMetadataStore.get(thumbnailUrl)
             return if (cached != null) {
                 DownloadDecision(false, cached.quality)
             } else {
                 DownloadDecision(false, NetworkQuality.LOW)
             }
        }
        
        val currentQuality = getNetworkQuality()
        val cachedMetadata = CacheMetadataStore.get(thumbnailUrl)
        
        // Case 1: No metadata
        if (cachedMetadata == null) {
            val connectivityManager = appContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val hasNetwork = network != null
            
            if (!hasNetwork) {
                return DownloadDecision(false, NetworkQuality.LOW) 
            }
            
            // Mark attempt
            cooldownMap[thumbnailUrl] = now
            return DownloadDecision(true, currentQuality)
        }
        
        // Check for TTL expiration
        val age = now - cachedMetadata.timestamp
        val isExpired = age > cachedMetadata.quality.ttl
        
        if (isExpired) {
            cooldownMap[thumbnailUrl] = now
            return DownloadDecision(true, currentQuality)
        }
        
        // Case 2: Upgrade check
        if (currentQuality.ordinal > cachedMetadata.quality.ordinal) {
            cooldownMap[thumbnailUrl] = now
            return DownloadDecision(true, currentQuality)
        }
        
        // Case 3: Use Cache
        return DownloadDecision(false, cachedMetadata.quality)
    }

    @Composable
    fun Thumbnail(
        thumbnailUrl: String?,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.FillBounds,
        modifier: Modifier = Modifier.clip( thumbnailShape() )
                                     .fillMaxSize()
    ) {
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") null else thumbnailUrl
        val decision = getDownloadDecision(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
               .data( validUrl?.thumbnail( decision.quality.size ) )
               .diskCacheKey( generateCacheKeySync(validUrl, decision.quality) )
               .diskCachePolicy( CachePolicy.ENABLED )
               .networkCachePolicy( if (decision.useNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
               .memoryCachePolicy( CachePolicy.ENABLED )
               .listener(
                   onSuccess = { _, _ ->
                       if (validUrl != null && decision.useNetwork) {
                           CacheMetadataStore.save(validUrl, decision.quality)
                       }
                   }
               )
               .build()

        AsyncImage(
            model = request,
            imageLoader = LOADER,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            placeholder = painterResource(R.drawable.loader),
            error = painterResource( R.drawable.ic_launcher_box ),
            fallback = painterResource( R.drawable.ic_launcher_box )
        )
    }

    @Composable
    fun Painter(
        thumbnailUrl: String?,
        contentScale: ContentScale = ContentScale.FillBounds,
        @DrawableRes placeholder: Int? = null,
        @DrawableRes error: Int = R.drawable.ic_launcher_box,
        @DrawableRes fallback: Int = R.drawable.ic_launcher_box,
        onLoading: ((State.Loading) -> Unit)? = null,
        onSuccess: ((State.Success) -> Unit)? = null,
        onError: ((State.Error) -> Unit)? = null
    ): AsyncImagePainter {
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") null else thumbnailUrl
        val decision = getDownloadDecision(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
                  .data( validUrl?.thumbnail( decision.quality.size ) )
                  .diskCacheKey( generateCacheKeySync(validUrl, decision.quality) )
                  .diskCachePolicy( CachePolicy.ENABLED )
                  .networkCachePolicy( if (decision.useNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
                  .memoryCachePolicy( CachePolicy.ENABLED )
                  .listener(
                      onSuccess = { _, _ ->
                          if (validUrl != null && decision.useNetwork) {
                              CacheMetadataStore.save(validUrl, decision.quality)
                          }
                      }
                  )
                  .build()

        return rememberAsyncImagePainter(
            model = request,
            imageLoader = LOADER,
            contentScale = contentScale,
            placeholder = painterResource( placeholder ?: R.drawable.loader ),
            error = painterResource( error ),
            fallback = painterResource( fallback ),
            onLoading = { state -> onLoading?.invoke(state) },
            onSuccess = { state -> onSuccess?.invoke(state) },
            onError = { state ->
                Timber.tag(TAG).e("Painter onError called: ${state.result}")
                onError?.invoke(state)
            }
        )
    }

    suspend fun loadBitmap(url: String?, allowHardware: Boolean = false): Bitmap? {
        if (url.isNullOrBlank() || url == "null") return null
        
        val decision = getDownloadDecision(url)

        val request = ImageRequest.Builder(appContext())
            .data(url.thumbnail(decision.quality.size))
            .diskCacheKey(generateCacheKeySync(url, decision.quality))
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(if (decision.useNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(allowHardware)
            .listener(
                onSuccess = { _, _ ->
                   if (decision.useNetwork) {
                       CacheMetadataStore.save(url, decision.quality)
                   }
                }
            )
            .build()
            
        val result = LOADER.execute(request)
        return result.image?.toBitmap()
    }

    @Composable
    fun AsyncImage(
        thumbnailUrl: String?,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.FillBounds,
        modifier: Modifier = Modifier,
        onLoading: ((State.Loading) -> Unit)? = null,
        onSuccess: ((State.Success) -> Unit)? = null,
        onError: ((State.Error) -> Unit)? = null
    ) {
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") null else thumbnailUrl
        val decision = getDownloadDecision(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
                  .data( validUrl?.thumbnail( decision.quality.size ) )
                  .diskCacheKey( generateCacheKeySync(validUrl, decision.quality) )
                  .diskCachePolicy( CachePolicy.ENABLED )
                  .networkCachePolicy( if (decision.useNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
                  .memoryCachePolicy( CachePolicy.ENABLED )
                  .listener(
                      onSuccess = { _, _ ->
                          if (validUrl != null && decision.useNetwork) {
                              CacheMetadataStore.save(validUrl, decision.quality)
                          }
                      }
                  )
                  .build()

        AsyncImage(
            model = request,
            imageLoader = LOADER,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            placeholder = painterResource( R.drawable.loader ),
            error = painterResource( R.drawable.ic_launcher_box ),
            fallback = painterResource( R.drawable.ic_launcher_box ),
            onLoading = { state -> onLoading?.invoke(state) },
            onSuccess = { state -> onSuccess?.invoke(state) },
            onError = { state ->
                Timber.tag(TAG).e("AsyncImage onError called: ${state.result}")
                onError?.invoke(state)
            }
        )
    }

    fun preloadImage(thumbnailUrl: String?) {
        try {
            if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") return
            
            val decision = getDownloadDecision(thumbnailUrl)
            
            val request = ImageRequest.Builder(appContext())
                .data(thumbnailUrl.thumbnail(decision.quality.size))
                .diskCacheKey(generateCacheKeySync(thumbnailUrl, decision.quality))
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .listener(
                    onSuccess = { _, _ ->
                        if (decision.useNetwork) {
                            CacheMetadataStore.save(thumbnailUrl, decision.quality)
                        }
                    }
                )
                .build()
            
            LOADER.enqueue(request)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Preload failed")
        }
    }

    fun isImageCached(thumbnailUrl: String?): Boolean {
         if (thumbnailUrl.isNullOrBlank()) return false
         return CacheMetadataStore.get(thumbnailUrl) != null
    }

    fun shouldUseNetwork(thumbnailUrl: String?): Boolean {
        return getDownloadDecision(thumbnailUrl).useNetwork
    }

    fun clearImageCache() {
        try {
            DISK_CACHE.clear()
            LOADER.memoryCache?.clear()
            cacheKeyMap.clear()
            cooldownMap.clear()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cache clearing failed")
        }
    }

    fun getCacheSize(): Long {
        return try {
            DISK_CACHE.size
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting cache size")
            0L
        }
    }

    fun getDiskCache(): DiskCache? {
        return try {
            DISK_CACHE
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting disk cache")
            null
        }
    }
}
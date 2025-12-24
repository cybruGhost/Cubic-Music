package me.knighthat.coil

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.kreate.android.R
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.State
import coil3.compose.rememberAsyncImagePainter
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.enums.CoilDiskCacheMaxSize
import it.fast4x.rimusic.thumbnail
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.utils.coilCustomDiskCacheKey
import it.fast4x.rimusic.utils.coilDiskCacheMaxSizeKey
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.preferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import timber.log.Timber
import okio.Path.Companion.toOkioPath

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
    private suspend fun generateCacheKey(url: String?): String {
        if (url.isNullOrBlank()) {
            return "empty"
        }
        
        return cacheKeyMutex.withLock {
            cacheKeyMap[url]?.let { 
                return it 
            }
            
            val key = try {
                val processedUrl = url.thumbnail(THUMBNAIL_SIZE) ?: url
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(processedUrl.toByteArray())
                val result = digest.fold("") { str, it -> str + "%02x".format(it) }
                result
            } catch (e: Exception) {
                val fallbackKey = url.hashCode().toString()
                Timber.tag(TAG).e(e, "Error generating MD5 key, using fallback: $fallbackKey")
                fallbackKey
            }
            
            cacheKeyMap[url] = key
            key
        }
    }

    /**
     * Check if network connection is good with enhanced detection
     * Now includes connection quality assessment
     */
    private fun isNetworkConnectionGood(): Boolean {
        return try {
            val connectivityManager = appContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            if (network == null) {
                return false
            }
            
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                return false
            }
            
            // Basic connectivity check
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            if (!hasInternet || !isValidated) {
                return false
            }
            
            // Transport type check
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            
            // Vérifier le débit de la connexion pour tous les types
            val hasGoodBandwidth = capabilities.linkDownstreamBandwidthKbps >= 1000 // 1 Mbps minimum
            val hasGoodLatency = capabilities.linkDownstreamBandwidthKbps > 0
            
            // Connection quality assessment
            val isGoodConnection = when {
                isWifi -> {
                    val notMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val notRoaming = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    val basicWifiCheck = notMetered || notRoaming
                    
                    // WiFi + bon débit
                    basicWifiCheck && hasGoodBandwidth && hasGoodLatency
                }
                isCellular -> {
                    val notRoaming = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    val notMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    
                    // In roaming, accept if the bandwidth is good
                    if (!notRoaming) {
                        // Mode roaming : check only the bandwidth
                        hasGoodBandwidth && hasGoodLatency
                    } else {
                        // Mode normal : existing logic + bandwidth
                        val basicCellularCheck = notRoaming && !notMetered
                        basicCellularCheck && hasGoodBandwidth && hasGoodLatency
                    }
                }
                else -> {
                    false
                }
            }
            
            isGoodConnection
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking network connection")
            false
        }
    }

    @Composable
    fun Thumbnail(
        thumbnailUrl: String?,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.FillBounds,
        modifier: Modifier = Modifier.clip( thumbnailShape() )
                                     .fillMaxSize()
    ) {
        // Check if the URL is valid before creating the request
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            null
        } else {
            thumbnailUrl
        }
        
        // Check if we should use the network based on connection quality and cache availability
        val shouldUseNetwork = shouldUseNetwork(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
                                               .data( validUrl?.thumbnail( THUMBNAIL_SIZE ) )
                                               .diskCacheKey( generateCacheKeySync(validUrl) )
                                               .diskCachePolicy( CachePolicy.ENABLED )
                                               .networkCachePolicy( if (shouldUseNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
                                               .memoryCachePolicy( CachePolicy.ENABLED )
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
        // Check if the URL is valid before creating the request
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            null
        } else {
            thumbnailUrl
        }
        
        // Check if we should use the network based on connection quality and cache availability
        val shouldUseNetwork = shouldUseNetwork(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
                                                  .data( validUrl?.thumbnail( THUMBNAIL_SIZE ) )
                                                  .diskCacheKey( generateCacheKeySync(validUrl) )
                                                  .diskCachePolicy( CachePolicy.ENABLED )
                                                  .networkCachePolicy( if (shouldUseNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
                                                  .memoryCachePolicy( CachePolicy.ENABLED )
                                                  .build()

        return rememberAsyncImagePainter(
            model = request,
            imageLoader = LOADER,
            contentScale = contentScale,
            placeholder = painterResource( placeholder ?: R.drawable.loader ),
            error = painterResource( error ),
            fallback = painterResource( fallback ),
            onLoading = { state ->
                onLoading?.invoke(state)
            },
            onSuccess = { state ->
                onSuccess?.invoke(state)
            },
            onError = { state ->
                Timber.tag(TAG).e("Painter onError called: ${state.result}")
                onError?.invoke(state)
            }
        )
    }

    /**
     * Synchronous version of generateCacheKey for Composable functions
     */
    private fun generateCacheKeySync(url: String?): String {
        if (url.isNullOrBlank()) {
            return "empty"
        }
        
        return cacheKeyMap[url] ?: run {
            val key = try {
                val processedUrl = url.thumbnail(THUMBNAIL_SIZE) ?: url
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(processedUrl.toByteArray())
                val result = digest.fold("") { str, it -> str + "%02x".format(it) }
                result
            } catch (e: Exception) {
                val fallbackKey = url.hashCode().toString()
                Timber.tag(TAG).e(e, "Error generating MD5 key, using fallback: $fallbackKey")
                fallbackKey
            }
            
            cacheKeyMap[url] = key
            key
        }
    }

    /**
     * Check if an image is in local cache with better error handling
     */
    fun isImageCached(thumbnailUrl: String?): Boolean {
        return try {
            if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
                return false
            }
            
            val cacheKey = generateCacheKeySync(thumbnailUrl)
            val snapshot = DISK_CACHE.openSnapshot(cacheKey)
            val isCached = snapshot != null
            isCached
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking if image is cached")
            false
        }
    }

    /**
     * Determine if we should use network based on connection quality and cache availability
     */
    fun shouldUseNetwork(thumbnailUrl: String?): Boolean {
        val isCached = isImageCached(thumbnailUrl)
        val isGoodConnection = isNetworkConnectionGood()
        
        // if the image is cached, do not use the network
        if (isCached) {
            return false
        }
        
        // if the connection is bad, do not use the network
        if (!isGoodConnection) {
            return false
        }
        
        return true
    }

    /**
     * Preload an image into cache with enhanced error handling
     */
    fun preloadImage(thumbnailUrl: String?) {
        try {
            if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
                return
            }
            
            val request = ImageRequest.Builder(appContext())
                .data(thumbnailUrl.thumbnail(THUMBNAIL_SIZE))
                .diskCacheKey(generateCacheKeySync(thumbnailUrl))
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            
            LOADER.enqueue(request)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Preload failed")
        }
    }

    /**
     * Clean the image cache completely
     * Clears both disk cache and memory cache
     */
    fun clearImageCache() {
        try {
            // Clear disk cache
            DISK_CACHE.clear()
            
            // Clear memory cache if available
            LOADER.memoryCache?.clear()
            
            // Clear cache key map
            cacheKeyMap.clear()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cache clearing failed")
        }
    }

    /**
     * Get the size of the image cache
     */
    fun getCacheSize(): Long {
        return try {
            val size = DISK_CACHE.size
            size
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting cache size")
            0L
        }
    }

    /**
     * Get the disk cache instance
     */
    fun getDiskCache(): DiskCache? {
        return try {
            DISK_CACHE
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting disk cache")
            null
        }
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
        // Check if the URL is valid before creating the request
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            null
        } else {
            thumbnailUrl
        }
        
        // Check if we should use the network based on connection quality and cache availability
        val shouldUseNetwork = shouldUseNetwork(validUrl)
        
        val request = ImageRequest.Builder( appContext() )
                                                  .data( validUrl?.thumbnail( THUMBNAIL_SIZE ) )
                                                  .diskCacheKey( generateCacheKeySync(validUrl) )
                                                  .diskCachePolicy( CachePolicy.ENABLED )
                                                  .networkCachePolicy( if (shouldUseNetwork) CachePolicy.ENABLED else CachePolicy.DISABLED )
                                                  .memoryCachePolicy( CachePolicy.ENABLED )
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
            onLoading = { state ->
                onLoading?.invoke(state)
            },
            onSuccess = { state ->
                onSuccess?.invoke(state)
            },
            onError = { state ->
                Timber.tag(TAG).e("AsyncImage onError called: ${state.result}")
                onError?.invoke(state)
            }
        )
    }
}
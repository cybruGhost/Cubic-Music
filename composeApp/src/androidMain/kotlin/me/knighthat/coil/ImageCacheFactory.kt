package me.knighthat.coil

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import app.kreate.android.R
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
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import it.fast4x.innertube.models.Thumbnail
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.enums.CoilDiskCacheMaxSize
import it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import it.fast4x.rimusic.enums.ImageQualityFormat
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.utils.coilCustomDiskCacheKey
import it.fast4x.rimusic.utils.coilDiskCacheMaxSizeKey
import it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.imageQualityFormatKey
import it.fast4x.rimusic.utils.isConnectionMeteredEnabledKey
import it.fast4x.rimusic.utils.preferences
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoilApi::class)
object ImageCacheFactory {

    private const val TAG = "ImageCacheFactory"

    val DISK_CACHE: DiskCache by lazy {
        val preferences = appContext().preferences
        val diskSize = preferences.getEnum(coilDiskCacheMaxSizeKey, CoilDiskCacheMaxSize.`128MB`)
        val cacheLocation = preferences.getEnum(exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System)
        val cacheDir = when (cacheLocation) {
            ExoPlayerCacheLocation.System -> appContext().cacheDir
            ExoPlayerCacheLocation.Private -> appContext().filesDir
        }.resolve("coil")
        val maxSizeBytes = when (diskSize) {
            CoilDiskCacheMaxSize.Custom -> {
                val customSize = preferences.getInt(coilCustomDiskCacheKey, 128)
                customSize.times(1000L).times(1000)
            }
            else -> diskSize.bytes
        }
        DiskCache.Builder()
            .directory(cacheDir.toPath().toOkioPath())
            .maxSizeBytes(maxSizeBytes)
            .build()
    }

    enum class NetworkQuality(val size: Int, val ttl: Long) {
        LOW(300, TimeUnit.HOURS.toMillis(1)),
        MEDIUM(720, TimeUnit.HOURS.toMillis(24)),
        HIGH(1200, TimeUnit.DAYS.toMillis(14))
    }

    data class DownloadDecision(val useNetwork: Boolean, val quality: NetworkQuality)

    private const val COOLDOWN_MS = 10 * 1000L
    private val cooldownMap = ConcurrentHashMap<String, Long>()
    private val cacheKeyMap = ConcurrentHashMap<String, String>()

    private object CacheMetadataStore {
        private const val PREFS_NAME = "image_cache_metadata"
        private fun getPrefs() = appContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun save(url: String, quality: NetworkQuality) {
            try {
                val key = url.hashCode().toString()
                getPrefs().edit().putString(key, "${quality.name}:${System.currentTimeMillis()}").apply()
            } catch (e: Exception) {}
        }

        fun get(url: String): NetworkQuality? {
            return try {
                val key = url.hashCode().toString()
                val value = getPrefs().getString(key, null) ?: return null
                NetworkQuality.valueOf(value.substringBefore(":"))
            } catch (e: Exception) { null }
        }

        fun remove(url: String) {
            try { getPrefs().edit().remove(url.hashCode().toString()).apply() } catch (e: Exception) {}
        }
    }

    val LOADER: ImageLoader by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        ImageLoader.Builder(appContext())
            .crossfade(true)
            .memoryCache { MemoryCache.Builder().maxSizePercent(appContext(), 0.15).strongReferencesEnabled(true).build() }
            .diskCache(DISK_CACHE)
            .components {
                add(OkHttpNetworkFetcherFactory(httpClient))
            }
            .build()
    }

    fun generateCacheKeySync(url: String?, quality: NetworkQuality): String {
        if (url.isNullOrBlank()) return "empty"
        val cacheKey = "${url}_${quality.size}"
        return cacheKeyMap.getOrPut(cacheKey) {
            try {
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(url.toByteArray())
                digest.fold("") { str, it -> str + "%02x".format(it) }
            } catch (e: Exception) { "${url.hashCode()}_${quality.size}" }
        }
    }

    fun getNetworkQuality(): NetworkQuality {
        val context = appContext()
        val forcedQuality = context.preferences.getEnum(imageQualityFormatKey, ImageQualityFormat.Auto)
        if (forcedQuality != ImageQualityFormat.Auto) {
            val quality = when (forcedQuality) {
                ImageQualityFormat.High -> NetworkQuality.HIGH
                ImageQualityFormat.Medium -> NetworkQuality.MEDIUM
                else -> NetworkQuality.LOW
            }
            Timber.tag(TAG).d("[NETWORK] Forced quality: $quality")
            return quality
        }

        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetworkQuality.LOW
            val bandwidth = capabilities.linkDownstreamBandwidthKbps
            
            val quality = when {
                bandwidth > 20000 -> NetworkQuality.HIGH
                bandwidth > 5000 -> NetworkQuality.MEDIUM
                else -> NetworkQuality.LOW
            }
            Timber.tag(TAG).v("[NETWORK] Auto quality: $quality (bandwidth=${bandwidth}kbps)")
            quality
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[NETWORK] Error detecting quality, fallback to LOW")
            NetworkQuality.LOW
        }
    }

    fun getCurrentNetworkQuality(): NetworkQuality = getNetworkQuality()

    fun getDownloadDecision(thumbnailUrl: String?): DownloadDecision {
        if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            Timber.tag(TAG).d("[DECISION] Invalid URL, no download")
            return DownloadDecision(false, NetworkQuality.LOW)
        }
        
        val lastAttempt = cooldownMap[thumbnailUrl]
        val now = System.currentTimeMillis()
        if (lastAttempt != null && (now - lastAttempt) < COOLDOWN_MS) {
            val cachedQuality = CacheMetadataStore.get(thumbnailUrl) ?: NetworkQuality.LOW
            Timber.tag(TAG).d("[DECISION] Cooldown active, use cache: $cachedQuality")
            return DownloadDecision(false, cachedQuality)
        }

        val currentQuality = getNetworkQuality()
        val cachedQuality = CacheMetadataStore.get(thumbnailUrl)

        if (cachedQuality == null) {
            cooldownMap[thumbnailUrl] = now
            Timber.tag(TAG).d("[DECISION] No cache metadata, download: $currentQuality")
            return DownloadDecision(true, currentQuality)
        }

        if (currentQuality.ordinal > cachedQuality.ordinal) {
            cooldownMap[thumbnailUrl] = now
            Timber.tag(TAG).d("[DECISION] Upgrade: $cachedQuality -> $currentQuality")
            return DownloadDecision(true, currentQuality)
        }

        Timber.tag(TAG).v("[DECISION] Use cached: $cachedQuality")
        return DownloadDecision(false, cachedQuality)
    }

    @Composable
    fun Thumbnail(
        thumbnailUrl: String?,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.FillBounds,
        modifier: Modifier = Modifier.clip(thumbnailShape()).fillMaxSize()
    ) {
        val validUrl = if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") null else thumbnailUrl
        val decision = getDownloadDecision(validUrl)
        
        val finalUrl = validUrl?.thumbnail(decision.quality.size)
        Timber.tag(TAG).v("[START] Thumbnail: $validUrl -> size=${decision.quality.size}")
        
        val request = ImageRequest.Builder(appContext())
            .data(finalUrl)
            .diskCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .memoryCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .listener(
                onSuccess = { _, result ->
                    Timber.tag(TAG).i("[SUCCESS] Thumbnail from ${result.dataSource}")
                    if (validUrl != null && decision.useNetwork) {
                        CacheMetadataStore.save(validUrl, decision.quality)
                    }
                },
                onError = { _, result ->
                    Timber.tag(TAG).e("[ERROR] Thumbnail: ${result.throwable.message}")
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
            error = painterResource(R.drawable.ic_launcher_box),
            fallback = painterResource(R.drawable.ic_launcher_box)
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
        
        val finalUrl = validUrl?.thumbnail(decision.quality.size)
        Timber.tag(TAG).v("[START] Painter: $validUrl -> size=${decision.quality.size}")
        
        val request = ImageRequest.Builder(appContext())
            .data(finalUrl)
            .diskCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .memoryCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .listener(
                onSuccess = { _, result ->
                    Timber.tag(TAG).i("[SUCCESS] Painter from ${result.dataSource}")
                    if (validUrl != null && decision.useNetwork) {
                        CacheMetadataStore.save(validUrl, decision.quality)
                    }
                },
                onError = { _, result ->
                    Timber.tag(TAG).e("[ERROR] Painter: ${result.throwable.message}")
                }
            )
            .build()

        return rememberAsyncImagePainter(
            model = request,
            imageLoader = LOADER,
            contentScale = contentScale,
            placeholder = painterResource(placeholder ?: R.drawable.loader),
            error = painterResource(error),
            fallback = painterResource(fallback),
            onLoading = onLoading,
            onSuccess = onSuccess,
            onError = { state ->
                Timber.tag(TAG).e("[UI_ERROR] Painter: ${state.result.throwable.message}")
                onError?.invoke(state)
            }
        )
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
        
        val finalUrl = validUrl?.thumbnail(decision.quality.size)
        Timber.tag(TAG).v("[START] AsyncImage: $validUrl -> size=${decision.quality.size}")
        
        val request = ImageRequest.Builder(appContext())
            .data(finalUrl)
            .diskCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .memoryCacheKey(generateCacheKeySync(validUrl, decision.quality))
            .listener(
                onSuccess = { _, result ->
                    Timber.tag(TAG).i("[SUCCESS] AsyncImage from ${result.dataSource}")
                    if (validUrl != null && decision.useNetwork) {
                        CacheMetadataStore.save(validUrl, decision.quality)
                    }
                },
                onError = { _, result ->
                    Timber.tag(TAG).e("[ERROR] AsyncImage: ${result.throwable.message}")
                }
            )
            .build()

        coil3.compose.AsyncImage(
            model = request,
            imageLoader = LOADER,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            placeholder = painterResource(R.drawable.loader),
            error = painterResource(R.drawable.ic_launcher_box),
            fallback = painterResource(R.drawable.ic_launcher_box),
            onLoading = onLoading,
            onSuccess = onSuccess,
            onError = { state ->
                Timber.tag(TAG).e("[UI_ERROR] AsyncImage: ${state.result.throwable.message}")
                onError?.invoke(state)
            }
        )
    }

    suspend fun loadBitmap(url: String?, allowHardware: Boolean = false): Bitmap? {
        if (url.isNullOrBlank() || url == "null") {
            Timber.tag(TAG).d("[LOAD] Invalid URL, returning null")
            return null
        }
        
        val decision = getDownloadDecision(url)
        val finalUrl = url.thumbnail(decision.quality.size)
        Timber.tag(TAG).v("[START] loadBitmap: $url -> size=${decision.quality.size}")
        
        val request = ImageRequest.Builder(appContext())
            .data(finalUrl)
            .diskCacheKey(generateCacheKeySync(url, decision.quality))
            .memoryCacheKey(generateCacheKeySync(url, decision.quality))
            .allowHardware(allowHardware)
            .listener(
                onSuccess = { _, result ->
                    Timber.tag(TAG).i("[SUCCESS] loadBitmap from ${result.dataSource}")
                    if (decision.useNetwork) {
                        CacheMetadataStore.save(url, decision.quality)
                    }
                },
                onError = { _, result ->
                    Timber.tag(TAG).e("[ERROR] loadBitmap: ${result.throwable.message}")
                }
            )
            .build()
            
        val result = LOADER.execute(request)
        return result.image?.toBitmap()
    }

    fun preloadImage(thumbnailUrl: String?) {
        if (thumbnailUrl.isNullOrBlank() || thumbnailUrl == "null") {
            Timber.tag(TAG).d("[PRELOAD] Invalid URL, skipping")
            return
        }
        
        val decision = getDownloadDecision(thumbnailUrl)
        val finalUrl = thumbnailUrl.thumbnail(decision.quality.size)
        Timber.tag(TAG).v("[START] preloadImage: $thumbnailUrl -> size=${decision.quality.size}")
        
        val request = ImageRequest.Builder(appContext())
            .data(finalUrl)
            .diskCacheKey(generateCacheKeySync(thumbnailUrl, decision.quality))
            .memoryCacheKey(generateCacheKeySync(thumbnailUrl, decision.quality))
            .listener(
                onSuccess = { _, result ->
                    Timber.tag(TAG).i("[SUCCESS] preloadImage from ${result.dataSource}")
                    if (decision.useNetwork) {
                        CacheMetadataStore.save(thumbnailUrl, decision.quality)
                    }
                },
                onError = { _, result ->
                    Timber.tag(TAG).e("[ERROR] preloadImage: ${result.throwable.message}")
                }
            )
            .build()
        
        LOADER.enqueue(request)
    }

    fun isImageCached(thumbnailUrl: String?): Boolean = CacheMetadataStore.get(thumbnailUrl ?: "") != null

    fun getDiskCache(): DiskCache = DISK_CACHE

    fun clearImageCache() {
        try {
            DISK_CACHE.clear()
            LOADER.memoryCache?.clear()
            cacheKeyMap.clear()
            cooldownMap.clear()
        } catch (e: Exception) {}
    }

    fun getCacheSize(): Long = try { DISK_CACHE.size } catch (e: Exception) { 0L }
}

fun String.resize(width: Int? = null, height: Int? = null): String {
    if (width == null && height == null) return this
    
    if (contains("googleusercontent.com")) {
        val regex = "googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        regex.find(this)?.groupValues?.let { group ->
            if (group.size >= 3) {
                val W = group[1].toIntOrNull() ?: 0
                val H = group[2].toIntOrNull() ?: 0
                if (W > 0 && H > 0) {
                    var w = width
                    var h = height
                    if (w != null && h == null) h = (w / W) * H
                    if (w == null && h != null) w = (h / H) * W
                    return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
                }
            }
        }
        val w = width ?: height ?: 0
        val h = height ?: width ?: 0
        return replace(Regex("([=-])w\\d+(?![a-zA-Z])"), "$1w$w").replace(Regex("([=-])h\\d+(?![a-zA-Z])"), "$1h$h")
    }
    
    if (startsWith("https://yt3.ggpht.com")) {
        val s = width ?: height ?: 0
        return replace(Regex("([=-])s\\d+(?![a-zA-Z])"), "$1s$s")
    }
    
    return this
}

fun String?.thumbnail(size: Int): String? {
    if (this == null) return null
    return when {
        startsWith("https://lh3.googleusercontent.com") -> "$this-w$size-h$size"
        startsWith("https://yt3.ggpht.com") -> "$this-w$size-h$size-s$size"
        else -> this
    }
}

fun String?.thumbnail(): String? = this

fun Uri?.thumbnail(size: Int): Uri? = this?.toString()?.thumbnail(size)?.toUri()

fun Thumbnail.size(size: Int): String = url.thumbnail(size) ?: url
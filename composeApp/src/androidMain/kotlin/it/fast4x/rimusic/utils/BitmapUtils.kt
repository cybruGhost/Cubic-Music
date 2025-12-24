package it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.toBitmap

suspend fun getBitmapFromUrl(context: Context, url: String): Bitmap {
    if (url.isBlank() || url == "null") {
        throw IllegalArgumentException("URL is empty or null")
    }
    
    try {
        val loader = SingletonImageLoader.get(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(false) // Disable hardware bitmaps
            .build()
        val result = loader.execute(request)
        
        if (result is ErrorResult) {
            throw result.throwable
        }
        
        val successResult = result as SuccessResult
        val bitmap = successResult.image.toBitmap()
        
        if (bitmap.width > 0 && bitmap.height > 0 && !bitmap.isRecycled) {
            return bitmap
        } else {
            throw IllegalStateException("Invalid bitmap: width=${bitmap.width}, height=${bitmap.height}, recycled=${bitmap.isRecycled}")
        }
    } catch (e: Exception) {
        throw e
    }
}



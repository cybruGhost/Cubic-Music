package it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.CachePolicy
import me.knighthat.coil.ImageCacheFactory

suspend fun getBitmapFromUrl(context: Context, url: String): Bitmap {
    if (url.isBlank() || url == "null") {
        throw IllegalArgumentException("URL is empty or null")
    }
    
    try {
        val loading = ImageCacheFactory.LOADER
        val request = ImageRequest.Builder(context)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        val result = loading.execute(request)
        
        if (result is ErrorResult) {
            throw result.throwable
        }
        
        val successResult = result as coil3.request.SuccessResult
        val drawable = successResult.image as? BitmapDrawable
        val bitmap = drawable?.bitmap
        
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0 && !bitmap.isRecycled) {
            return bitmap
        } else {
            throw IllegalStateException("Invalid bitmap: width=${bitmap?.width}, height=${bitmap?.height}, recycled=${bitmap?.isRecycled}")
        }
    } catch (e: Exception) {
        // Log l'erreur pour le débogage
        println("getBitmapFromUrl error for URL '$url': ${e.message}")
        throw e
    }
}



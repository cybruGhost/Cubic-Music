package it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import me.knighthat.coil.ImageCacheFactory

suspend fun getBitmapFromUrl(context: Context, url: String): Bitmap {
    if (url.isBlank() || url == "null") {
        throw IllegalArgumentException("URL is empty or null")
    }
    
    val bitmap: Bitmap? = ImageCacheFactory.loadBitmap(url, allowHardware = false)
    
    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0 && !bitmap.isRecycled) {
        return bitmap
    } else {
        throw IllegalStateException("Failed to load bitmap: width=${bitmap?.width}, height=${bitmap?.height}, recycled=${bitmap?.isRecycled}")
    }
}


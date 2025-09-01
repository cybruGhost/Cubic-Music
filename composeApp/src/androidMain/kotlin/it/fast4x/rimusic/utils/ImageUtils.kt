package it.fast4x.rimusic.utils

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import me.knighthat.coil.ImageCacheFactory


fun cacheImage(context: Context, url: String, key: String) {

    val listener = object : ImageRequest.Listener {
        override fun onError(request: ImageRequest, result: ErrorResult) {
            super.onError(request, result)
        }

        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            super.onSuccess(request, result)
        }
    }
    val imageRequest = ImageRequest.Builder(context)
        .data(url.thumbnail(256))
        .listener(listener)
        .memoryCacheKey(url.thumbnail(256))
        .diskCacheKey(url.thumbnail(256))
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()

            ImageCacheFactory.LOADER.enqueue(imageRequest)

}
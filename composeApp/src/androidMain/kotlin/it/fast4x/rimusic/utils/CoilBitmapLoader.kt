package it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import me.knighthat.coil.ImageCacheFactory
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import me.knighthat.utils.Toaster

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val bitmapSize: Int,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            val imageRequest = ImageRequest.Builder(context)
                .data(uri.toString())
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .allowHardware(false)
                .size(bitmapSize, bitmapSize)
                .build()
            
            val result = ImageCacheFactory.LOADER.execute(imageRequest)
            result.image?.toBitmap() ?: error("Could not load image")
        }
}

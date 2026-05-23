package app.it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val bitmapSize: Int,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)?.takeIf(::isUsableBitmap)
                ?: fallbackBitmap()
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            ImageCacheFactory.loadBitmap(uri.toString(), allowHardware = false)
                ?.takeIf(::isUsableBitmap)
                ?: fallbackBitmap()
        }

    private fun fallbackBitmap(): Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
            ?.takeIf(::isUsableBitmap)
            ?: Bitmap.createBitmap(bitmapSize.coerceAtLeast(1), bitmapSize.coerceAtLeast(1), Bitmap.Config.ARGB_8888)

    private fun isUsableBitmap(bitmap: Bitmap?): Boolean =
        bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0
}

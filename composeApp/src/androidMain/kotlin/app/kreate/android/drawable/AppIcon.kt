package app.kreate.android.drawable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.kreate.android.R
import app.it.fast4x.rimusic.appContext

// Due to the complexity of rasterized image, it must be converted into
// bitmap before rendering.
val APP_ICON_BITMAP: Bitmap by lazy {
    BitmapFactory.decodeResource( appContext().resources, R.drawable.ic_launcher )
}

val APP_ICON_IMAGE_BITMAP: ImageBitmap by lazy {
    APP_ICON_BITMAP.asImageBitmap()
}
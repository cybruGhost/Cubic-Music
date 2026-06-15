package app.it.fast4x.rimusic.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.it.fast4x.rimusic.cleanPrefix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val THUMBNAIL_SHARE_SIZE = 1080

suspend fun shareThumbnailCard(
    context: Context,
    mediaId: String,
    title: String,
    artist: String,
    artworkUrl: String?,
    includeSongLink: Boolean,
    songLinkOverride: String? = null,
): Result<Unit> = runCatching {
    val file = withContext(Dispatchers.IO) {
        val artwork = artworkUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ImageCacheFactory.loadBitmap(it, allowHardware = false) }.getOrNull() }
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
        val bitmap = buildThumbnailShareBitmap(context, artwork, logo, title, artist)
        val dir = File(context.cacheDir, "shared_thumbnail_cards").also { cacheDir ->
            if (!cacheDir.exists()) cacheDir.mkdirs()
            cacheDir.listFiles()
                ?.sortedByDescending(File::lastModified)
                ?.drop(8)
                ?.forEach(File::delete)
        }
        val file = File(dir, "thumbnail_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        logo?.takeIf { !it.isRecycled }?.recycle()
        file
    }

    val text = if (includeSongLink) {
        listOf(
            cleanPrefix(title).ifBlank { context.getString(R.string.unknown_title) },
            cleanPrefix(artist).ifBlank { context.getString(R.string.rewind_unknown_artist) },
            songLinkOverride?.takeIf { it.isNotBlank() } ?: buildThumbnailShareLink(mediaId)
        ).joinToString("\n")
    } else null

    withContext(Dispatchers.Main) {
        shareThumbnailImageFile(context, file, text)
    }
}

private fun buildThumbnailShareBitmap(
    context: Context,
    artwork: Bitmap?,
    logo: Bitmap?,
    title: String,
    artist: String,
): Bitmap {
    val bitmap = Bitmap.createBitmap(THUMBNAIL_SHARE_SIZE, THUMBNAIL_SHARE_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val baseColor = artwork?.let(::sampleAverageColor) ?: Color.parseColor("#8B5CF6")
    val deepColor = darken(baseColor, 0.45f)
    val accentColor = lighten(baseColor, 0.22f)
    val textColor = Color.WHITE
    val mutedText = Color.argb(205, 255, 255, 255)
    val glassColor = Color.argb(84, 255, 255, 255)
    val strokeColor = Color.argb(74, 255, 255, 255)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            THUMBNAIL_SHARE_SIZE.toFloat(),
            THUMBNAIL_SHARE_SIZE.toFloat(),
            intArrayOf(deepColor, baseColor, Color.parseColor("#080612")),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, THUMBNAIL_SHARE_SIZE.toFloat(), THUMBNAIL_SHARE_SIZE.toFloat(), bgPaint)

    artwork?.let {
        val blurred = Bitmap.createScaledBitmap(it, 160, 160, true)
        val big = Bitmap.createScaledBitmap(blurred, THUMBNAIL_SHARE_SIZE, THUMBNAIL_SHARE_SIZE, true)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 92 }.also { paint ->
            canvas.drawBitmap(big, 0f, 0f, paint)
        }
        blurred.recycle()
        big.recycle()
    }

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
    }
    canvas.drawCircle(120f, 120f, 280f, glowPaint)
    canvas.drawCircle(960f, 260f, 180f, glowPaint)

    val cardRect = RectF(76f, 76f, 1004f, 1004f)
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(74, 12, 10, 24) }
    val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(cardRect, 54f, 54f, cardPaint)
    canvas.drawRoundRect(cardRect, 54f, 54f, cardStroke)

    val cleanedTitle = cleanPrefix(title).ifBlank { context.getString(R.string.unknown_title) }
    val cleanedArtist = cleanPrefix(artist).ifBlank { context.getString(R.string.rewind_unknown_artist) }
    val isLongTitle = cleanedTitle.length > 34
    val artRect = if (isLongTitle) RectF(154f, 126f, 926f, 748f) else RectF(154f, 142f, 926f, 914f)
    val artPath = Path().apply { addRoundRect(artRect, 44f, 44f, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(artPath)
    if (artwork != null) {
        canvas.drawBitmap(artwork, null, artRect, Paint(Paint.ANTI_ALIAS_FLAG))
    } else {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = glassColor }.also {
            canvas.drawRoundRect(artRect, 44f, 44f, it)
        }
    }
    canvas.restore()

    if (!isLongTitle) {
        val overlayRect = RectF(154f, 708f, 926f, 914f)
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                overlayRect.top,
                0f,
                overlayRect.bottom,
                Color.TRANSPARENT,
                Color.argb(210, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }.also { canvas.drawRoundRect(overlayRect, 44f, 44f, it) }
    }

    val titleTypeface = ResourcesCompat.getFont(context, R.font.poppins_w700)
        ?: Typeface.create("sans-serif", Typeface.BOLD)
    val bodyTypeface = ResourcesCompat.getFont(context, R.font.poppins_w500)
        ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val brandTypeface = ResourcesCompat.getFont(context, R.font.rubik_w500)
        ?: Typeface.create("sans-serif-medium", Typeface.BOLD)

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = if (isLongTitle) 39f else 50f
        typeface = titleTypeface
    }
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedText
        textSize = if (isLongTitle) 27f else 31f
        typeface = bodyTypeface
    }
    val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 27f
        typeface = brandTypeface
    }

    val titleY = if (isLongTitle) 784f else 748f
    val artistY = if (isLongTitle) 884f else 842f
    canvas.save()
    canvas.translate(202f, titleY)
    createTextLayout(cleanedTitle, 676, titlePaint, if (isLongTitle) 3 else 2).draw(canvas)
    canvas.restore()
    canvas.save()
    canvas.translate(202f, artistY)
    createTextLayout(cleanedArtist, 640, artistPaint, 1).draw(canvas)
    canvas.restore()

    logo?.let {
        canvas.drawBitmap(it, null, RectF(188f, 940f, 236f, 988f), null)
    }
    canvas.drawText(context.getString(R.string.thumbnail_share_app_name), 252f, 973f, brandPaint)
    return bitmap
}

private fun createTextLayout(text: String, width: Int, paint: TextPaint, maxLines: Int): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1f)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .build()

private fun shareThumbnailImageFile(context: Context, file: File, text: String?) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.thumbnail_share_chooser)).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val launchContext = context.findActivityContext() ?: context
    if (launchContext === context && launchContext !is android.app.Activity) {
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchContext.startActivity(chooserIntent)
}

fun buildThumbnailShareLink(mediaId: String): String {
    val id = mediaId.removePrefix("e:")
        .substringAfterLast("watch?v=")
        .substringAfterLast("/")
        .substringBefore("&")
        .substringBefore("?")
    return "https://youtu.be/${Uri.encode(id)}"
}

private fun Context.findActivityContext(): android.app.Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

private fun sampleAverageColor(bitmap: Bitmap): Int {
    val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
    var red = 0
    var green = 0
    var blue = 0
    var count = 0
    for (x in 0 until scaled.width) {
        for (y in 0 until scaled.height) {
            val color = scaled.getPixel(x, y)
            red += Color.red(color)
            green += Color.green(color)
            blue += Color.blue(color)
            count++
        }
    }
    if (scaled !== bitmap) scaled.recycle()
    return Color.rgb(red / count, green / count, blue / count)
}

private fun darken(color: Int, factor: Float): Int =
    Color.rgb(
        max(0, (Color.red(color) * (1f - factor)).toInt()),
        max(0, (Color.green(color) * (1f - factor)).toInt()),
        max(0, (Color.blue(color) * (1f - factor)).toInt())
    )

private fun lighten(color: Int, factor: Float): Int =
    Color.rgb(
        min(255, (Color.red(color) + (255 - Color.red(color)) * factor).toInt()),
        min(255, (Color.green(color) + (255 - Color.green(color)) * factor).toInt()),
        min(255, (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt())
    )

package app.it.fast4x.rimusic.utils

import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.kreate.android.R
import app.it.fast4x.rimusic.cleanPrefix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private const val LYRICS_SHARE_WIDTH = 1080
private const val LYRICS_SHARE_HEIGHT = 1920
private const val MAX_SHARE_LINES = 6
private const val MAX_SHARE_VISUAL_LINES = 8
private const val SHARE_BASE_URL = "https://thecub.netlify.app/cubicmusic"

suspend fun shareLyricsCard(
    context: Context,
    mediaId: String,
    title: String,
    artist: String,
    lyricsText: String,
    artworkUrl: String?,
    deeplinkUrl: String = buildLyricsShareLink(mediaId, title, artist),
): Result<Unit> = runCatching {
    val shareLines = buildLyricsShareLines(lyricsText)
    val clippedLyrics = buildLyricsShareSlice(shareLines)

    val artwork = artworkUrl
        ?.takeIf { it.isNotBlank() }
        ?.let {
            runCatching { getBitmapFromUrl(context, it) }.getOrNull()
        }
    val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)

    val file = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(LYRICS_SHARE_WIDTH, LYRICS_SHARE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val baseColor = artwork?.let(::sampleAverageColor) ?: Color.parseColor("#7A96BA")
        val darkBaseColor = darkenColor(baseColor, 0.25f)
        val cardColor = adjustAlpha(darkenColor(baseColor, 0.10f), 0.88f)
        val useLightText = isDarkColor(baseColor)
        val accentColor = if (useLightText) Color.WHITE else Color.parseColor("#0B1320")
        val bodyTextColor = if (useLightText) Color.WHITE else Color.parseColor("#0B1320")
        val outlineColor = adjustAlpha(if (useLightText) Color.WHITE else Color.BLACK, 0.18f)
        val titleTypeface = ResourcesCompat.getFont(context, R.font.poppins_w600)
            ?: Typeface.create("sans-serif-medium", Typeface.BOLD)
        val subtitleTypeface = ResourcesCompat.getFont(context, R.font.poppins_w400)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        val bodyTypeface = ResourcesCompat.getFont(context, R.font.poppins_w700)
            ?: Typeface.create("sans-serif-medium", Typeface.BOLD)
        val brandTypeface = ResourcesCompat.getFont(context, R.font.rubik_w500)
            ?: Typeface.create("sans-serif-medium", Typeface.BOLD)

        canvas.drawColor(baseColor)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkBaseColor
        }
        canvas.drawRect(
            0f,
            (LYRICS_SHARE_HEIGHT * 0.82f),
            LYRICS_SHARE_WIDTH.toFloat(),
            LYRICS_SHARE_HEIGHT.toFloat(),
            footerPaint
        )

        val cardLeft = 100f
        val cardTop = 340f
        val cardRight = LYRICS_SHARE_WIDTH - 100f
        val cardBottom = 1388f
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardColor
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)
        canvas.drawRoundRect(cardRect, 36f, 36f, strokePaint)

        val artworkSize = 168
        val artworkLeft = (cardLeft + 42f).toInt()
        val artworkTop = (cardTop + 42f).toInt()
        artwork?.let {
            val scaled = Bitmap.createScaledBitmap(it, artworkSize, artworkSize, true)
            val artworkRect = RectF(
                artworkLeft.toFloat(),
                artworkTop.toFloat(),
                (artworkLeft + artworkSize).toFloat(),
                (artworkTop + artworkSize).toFloat()
            )
            val artworkPath = Path().apply {
                addRoundRect(artworkRect, 28f, 28f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(artworkPath)
            canvas.drawBitmap(scaled, null, artworkRect, null)
            canvas.restore()
            if (scaled !== it) scaled.recycle()
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(accentColor, 0.95f)
            textSize = 34f
            isFakeBoldText = true
            typeface = titleTypeface
        }
        val subMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(accentColor, 0.72f)
            textSize = 26f
            typeface = subtitleTypeface
        }
        val lyricsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bodyTextColor
            textSize = 61f
            isFakeBoldText = true
            typeface = bodyTypeface
        }
        val footerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(accentColor, 0.68f)
            textSize = 21f
            typeface = subtitleTypeface
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(accentColor, 0.9f)
            textSize = 28f
            isFakeBoldText = true
            typeface = brandTypeface
        }
        val textStartX = artworkLeft + artworkSize + 28f
        val titleLayout = createShareLayout(
            text = cleanPrefix(title).ifBlank { "Unknown title" },
            width = ((cardRight - textStartX) - 42f).toInt(),
            paint = metaPaint,
            textSize = metaPaint.textSize,
            maxLines = 2
        )
        canvas.save()
        canvas.translate(textStartX, artworkTop + 6f)
        titleLayout.draw(canvas)
        canvas.restore()
        val artistBaseline = artworkTop + 6f + titleLayout.height + 22f
        canvas.drawText(
            cleanPrefix(artist).ifBlank { "Unknown artist" },
            textStartX,
            artistBaseline,
            subMetaPaint
        )

        val maxTextWidth = (cardRect.width() - 110f).toInt()
        val lyricsTop = cardTop + 264f
        val lyricsBottom = cardBottom - 152f
        val availableLyricsHeight = (lyricsBottom - lyricsTop).toInt()
        val lyricsLayout = buildShareLyricsLayout(
            text = clippedLyrics,
            width = maxTextWidth,
            availableHeight = availableLyricsHeight,
            basePaint = lyricsPaint
        )

        canvas.save()
        canvas.translate(cardLeft + 50f, lyricsTop)
        lyricsLayout.draw(canvas)
        canvas.restore()

        logo?.let {
            val logoSize = 44
            val logoRect = RectF(
                cardLeft + 40f,
                cardBottom - 94f,
                cardLeft + 40f + logoSize,
                cardBottom - 94f + logoSize
            )
            canvas.drawBitmap(it, null, logoRect, null)
        }
        canvas.drawText("Cubic Music", cardLeft + 98f, cardBottom - 56f, brandPaint)
        canvas.drawText(
            "\"${takeShareQuote(clippedLyrics)}\"",
            110f,
            LYRICS_SHARE_HEIGHT - 124f,
            footerTextPaint
        )

        val shareDir = File(context.cacheDir, "shared_lyrics_cards").also { dir ->
            if (!dir.exists()) dir.mkdirs()
            dir.listFiles()
                ?.sortedByDescending(File::lastModified)
                ?.drop(5)
                ?.forEach(File::delete)
        }
        val file = File(shareDir, "lyrics_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        bitmap.recycle()
        logo?.takeIf { !it.isRecycled }?.recycle()
        file
    }

    withContext(Dispatchers.Main) {
        shareImageFile(context, file)
    }
}

private fun shareImageFile(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Share lyrics card").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val launchContext = context.findActivityContext() ?: context
    if (launchContext === context && launchContext !is android.app.Activity) {
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchContext.startActivity(chooserIntent)
}

fun buildLyricsShareLink(
    mediaId: String,
    title: String,
    artist: String
): String = "$SHARE_BASE_URL?id=${Uri.encode(mediaId)}&title=${Uri.encode(cleanPrefix(title))}&artist=${Uri.encode(cleanPrefix(artist))}"

fun buildLyricsShareLines(text: String): List<String> {
    val normalizedText = text
        .lines()
        .map { line ->
            line
                .replace(Regex("""\[[^\]]*]"""), "")
                .replace(Regex("""<[^>]*>"""), "")
                .replace(Regex("""\b[a-zA-Z0-9_]+:\s*"""), "")
                .trim()
        }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .ifBlank { "No lyrics available yet." }

    return normalizedText
        .lineSequence()
        .flatMap { line -> wrapLyricsLine(line).asSequence() }
        .toList()
        .ifEmpty { listOf("No lyrics available yet.") }
}

fun buildLyricsShareSlice(
    lines: List<String>,
    startIndex: Int = 0,
    maxLines: Int = MAX_SHARE_LINES
): String {
    if (lines.isEmpty()) return "No lyrics available yet."
    val safeStartIndex = startIndex.coerceIn(0, lines.lastIndex)
    val selectedLines = lines
        .drop(safeStartIndex)
        .take(maxLines)
        .toMutableList()

    while (selectedLines.size < maxLines) {
        selectedLines += ""
    }

    return selectedLines.joinToString("\n").ifBlank { "No lyrics available yet." }
}

private fun wrapLyricsLine(
    line: String,
    maxCharsPerLine: Int = 26
): List<String> {
    val trimmed = line.trim()
    if (trimmed.isBlank()) return listOf("")

    val words = trimmed.split(Regex("\\s+"))
    val wrapped = mutableListOf<String>()
    var current = ""

    fun push() {
        if (current.isNotBlank()) {
            wrapped += current
            current = ""
        }
    }

    words.forEach { word ->
        if (word.length > maxCharsPerLine) {
            push()
            word.chunked(maxCharsPerLine).forEach { chunk -> wrapped += chunk }
            return@forEach
        }

        val candidate = if (current.isBlank()) word else "$current $word"
        if (candidate.length <= maxCharsPerLine) {
            current = candidate
        } else {
            push()
            current = word
        }
    }
    push()

    return wrapped.ifEmpty { listOf(trimmed) }
}

private tailrec fun Context.findActivityContext(): Context? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext?.findActivityContext()
    else -> null
}

private fun sampleAverageColor(bitmap: Bitmap): Int {
    val sampleSize = min(bitmap.width, bitmap.height).coerceAtLeast(1)
    val step = (sampleSize / 16).coerceAtLeast(1)
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L

    for (x in 0 until bitmap.width step step) {
        for (y in 0 until bitmap.height step step) {
            val pixel = bitmap.getPixel(x, y)
            red += Color.red(pixel)
            green += Color.green(pixel)
            blue += Color.blue(pixel)
            count++
        }
    }

    if (count == 0L) return Color.parseColor("#7A96BA")
    return Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
}

private fun darkenColor(color: Int, amount: Float): Int {
    val factor = 1f - amount.coerceIn(0f, 0.95f)
    return Color.rgb(
        (Color.red(color) * factor).toInt(),
        (Color.green(color) * factor).toInt(),
        (Color.blue(color) * factor).toInt()
    )
}

private fun adjustAlpha(color: Int, alphaFraction: Float): Int {
    val alpha = (255 * alphaFraction.coerceIn(0f, 1f)).toInt()
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

private fun isDarkColor(color: Int): Boolean {
    val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    return darkness >= 0.45
}

private fun takeShareQuote(text: String): String {
    val firstLine = text
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trim()

    return when {
        firstLine.length <= 68 -> firstLine
        else -> firstLine.take(65).trimEnd() + "..."
    }
}

private fun buildShareLyricsLayout(
    text: String,
    width: Int,
    availableHeight: Int,
    basePaint: TextPaint
): StaticLayout {
    var textSize = basePaint.textSize
    var layout = createShareLayout(
        text = text,
        width = width,
        paint = basePaint,
        textSize = textSize,
        maxLines = MAX_SHARE_VISUAL_LINES
    )

    while (layout.height > availableHeight && textSize > 46f) {
        textSize -= 2f
        layout = createShareLayout(
            text = text,
            width = width,
            paint = basePaint,
            textSize = textSize,
            maxLines = MAX_SHARE_VISUAL_LINES
        )
    }

    return layout
}

private fun createShareLayout(
    text: String,
    width: Int,
    paint: TextPaint,
    textSize: Float,
    maxLines: Int = Int.MAX_VALUE
): StaticLayout {
    val adjustedPaint = TextPaint(paint).apply {
        this.textSize = textSize
    }
    return StaticLayout.Builder
        .obtain(text, 0, text.length, adjustedPaint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(8f, 1.0f)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .build()
}

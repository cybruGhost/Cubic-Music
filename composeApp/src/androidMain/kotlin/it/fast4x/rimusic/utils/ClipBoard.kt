package it.fast4x.rimusic.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.kreate.android.R
import me.knighthat.utils.Toaster
import timber.log.Timber

fun textCopyToClipboard(textCopied: String, context: Context) {
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    runCatching {
        val clip = ClipData.newPlainText("", textCopied)
        clipboardManager.setPrimaryClip(clip)
        
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toaster.s(R.string.value_copied)
        }
    }.onFailure {
        Timber.e("Failed to copy text to clipboard: ${it.stackTraceToString()}")
        Toaster.e("Failed to copy text to clipboard, try again")
    }
}

@Composable
fun textCopyFromClipboard(context: Context): String {
    var textCopied by remember { mutableStateOf("") }
    
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    runCatching {
        val clipData = clipboardManager.primaryClip
        textCopied = clipData?.let { clip ->
            if (clip.itemCount > 0) {
                clip.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
            } else {
                ""
            }
        } ?: ""
        
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && textCopied.isNotEmpty()) {
            Toaster.i(R.string.value_copied)
        }
    }.onFailure {
        Timber.e("Failed to get text from clipboard: ${it.stackTraceToString()}")
        Toaster.e("Failed to get text from clipboard, try again")
    }
    
    return textCopied
}
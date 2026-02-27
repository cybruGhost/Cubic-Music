package app.it.fast4x.rimusic.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Composable version (for use inside composable functions)
@Composable
fun SmartMessage(
    message: String,
    context: Context = LocalContext.current,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(context, message, duration).show()
}

@Composable
fun SmartMessage(
    @StringRes messageRes: Int,
    context: Context = LocalContext.current,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(context, context.getString(messageRes), duration).show()
}

// Non-composable version (for use in listeners and lambdas)
fun showToast(
    context: Context,
    message: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(context, message, duration).show()
}

fun showToast(
    context: Context,
    @StringRes messageRes: Int,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(context, context.getString(messageRes), duration).show()
}
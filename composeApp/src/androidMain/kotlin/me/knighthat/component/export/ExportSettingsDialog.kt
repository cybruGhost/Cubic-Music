package me.knighthat.component.export

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import app.kreate.android.BuildConfig
import app.kreate.android.R
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.ExportToFileDialog

class ExportSettingsDialog private constructor(
    private val launcher: ManagedActivityResultLauncher<String, Uri?>,
    private val context: Context
) {
    companion object {
        private fun onExport(
            uri: Uri,
            context: Context
        ) = CoroutineScope( Dispatchers.IO ).launch {
            val entries: List<Triple<String, String, Any>> = context.preferences
                .all
                .map {
                    val value = it.value ?: Unit
                    val type = value::class.simpleName ?: "null"
                    Triple( type, it.key, value )
                }
                .filter { it.first != "null" && it.third !== Unit }

            context.contentResolver
                .openOutputStream( uri )
                ?.use { outStream ->
                    csvWriter().open( outStream ) {
                        writeRow( "Type", "Key", "Value" )
                        flush()
                        entries.forEach {
                            writeRow( it.first, it.second, it.third )
                        }
                        close()
                    }
                }
        }

        @Composable
        operator fun invoke( context: Context ): ExportSettingsDialog =
            ExportSettingsDialog(
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument( "text/csv" )
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    onExport( uri, context )
                },
                context
            )
    }

    fun export() {
        val fileName = "${BuildConfig.APP_NAME}_settings"
        launcher.launch("$fileName.csv")
    }
}
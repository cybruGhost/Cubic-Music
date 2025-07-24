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
    valueState: MutableState<TextFieldValue>,
    activeState: MutableState<Boolean>,
    launcher: ManagedActivityResultLauncher<String, Uri?>
): ExportToFileDialog(valueState, activeState, launcher) {

    companion object {
        private fun onExport(
            uri: Uri,
            context: Context
        ) = CoroutineScope( Dispatchers.IO ).launch {       // Run in background to prevent UI thread from freezing due to large file.
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
                   ?.use { outStream ->         // Use [use] because it closes stream on exit
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
                remember {
                    mutableStateOf( TextFieldValue() )
                },
                rememberSaveable { mutableStateOf(false) },
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument( "text/csv" )
                ) { uri ->
                    // [uri] must be non-null (meaning path exists) in order to work
                    uri ?: return@rememberLauncherForActivityResult
                    onExport( uri, context )
                }
            )
    }

    override val extension: String = "xml"
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.title_export_settings )

    override fun defaultFileName(): String = "${BuildConfig.APP_NAME}_settings"
}
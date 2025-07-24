package me.knighthat.component.import

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastForEach
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.ImportFromFile
import me.knighthat.component.dialog.RestartAppDialog
import java.io.InputStream

class ImportSettings private constructor(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): ImportFromFile(launcher) {

    companion object {
        fun onImport( context: Context, inStream: InputStream ) =
            csvReader().readAllWithHeader( inStream )
                       .fastForEach { row -> // Experimental, revert back to [forEach] if needed
                           val type = row["Type"] ?: ""
                           val key = row["Key"] ?: ""
                           val value = row["Value"] ?: ""

                           val editor = context.preferences.edit()
                           when( type.lowercase() ) {
                               "string" -> editor.putString( key, value )
                               "int" -> editor.putInt( key, value.toInt() )
                               "long" -> editor.putLong( key, value.toLong() )
                               "float" -> editor.putFloat( key, value.toFloat() )
                               "boolean" -> editor.putBoolean( key, value.toBoolean() )
                           }
                           // Important! No action is allowed during this process to
                           // prevent race-condition. [commit] blocks thread until it's done
                           editor.commit()
                       }

        @Composable
        operator fun invoke( context: Context ): ImportSettings =
            ImportSettings(
                rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    // [uri] must be non-null (meaning path exists) in order to work
                    uri ?: return@rememberLauncherForActivityResult

                    // Run in background to prevent UI thread
                    // from freezing due to large file.
                    CoroutineScope(Dispatchers.IO).launch {
                        context.contentResolver
                               .openInputStream( uri )
                               ?.use { inStream ->         // Use [use] because it closes stream on exit
                                   onImport( context, inStream )
                               }

                        RestartAppDialog.showDialog()
                    }
                }
            )
    }

    override val supportedMimes: Array<String> = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}
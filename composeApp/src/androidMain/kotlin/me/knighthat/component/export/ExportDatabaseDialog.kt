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
import it.fast4x.rimusic.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.ExportToFileDialog
import me.knighthat.utils.TimeDateUtils
import java.io.FileInputStream

class ExportDatabaseDialog private constructor(
    private val launcher: ManagedActivityResultLauncher<String, Uri?>
) {
    companion object {
        @Composable
        operator fun invoke(context: Context): ExportDatabaseDialog =
            ExportDatabaseDialog(
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/vnd.sqlite3")
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    CoroutineScope(Dispatchers.IO).launch {
                        Database.checkpoint()
                        context.applicationContext
                            .contentResolver
                            .openOutputStream(uri)
                            ?.use { outStream ->
                                val dbFile = context.getDatabasePath(Database.FILE_NAME)
                                FileInputStream(dbFile).use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                    }
                }
            )
    }

    fun export() {
        val fileName = "${BuildConfig.APP_NAME}_database_${TimeDateUtils.localizedDateNoDelimiter()}_${TimeDateUtils.timeNoDelimiter()}"
        launcher.launch("$fileName.sqlite")
    }
}
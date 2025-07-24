package app.kreate.android.themed.common.screens.settings.other

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.utils.textCopyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import java.io.File

private const val RUNTIME_LOGS_FILENAME = "runtime.log"
private const val CRASH_LOGS_FILENAME =  "crash.log"

private fun readLogs( context: Context, isCrashLog: Boolean ): List<String> {
    val filename = if( isCrashLog ) "RiMusic_crash_log.txt" else "RiMusic_log.txt"
    val file = File(context.filesDir.resolve( "logs" ), filename)

    return if ( file.exists() && file.canRead() )
        file.readLines()
    else
        emptyList()
}

@Composable
private fun Entry(
    context: Context,
    @StringRes titleId: Int,
    @StringRes subtitleId: Int,
    onExport: () -> Unit,
    onReadLogs: () -> List<String>
) =
    SettingComponents.Text(
        stringResource( titleId ),
        onExport,
        subtitle = stringResource( subtitleId ),
        isEnabled = Preferences.DEBUG_LOG.value
    ) {
        Icon(
            painter = painterResource( R.drawable.copy ),
            contentDescription = stringResource( R.string.copy_log_to_clipboard ),
            tint = colorPalette().background4,
            modifier = Modifier.size( 24.dp )
                               .clickable( enabled = Preferences.DEBUG_LOG.value ) {
                                   val logs = onReadLogs()
                                   if ( logs.isEmpty() )
                                       Toaster.w( R.string.no_log_available )
                                   else
                                       textCopyToClipboard( logs.joinToString( "\n" ), context )
                               }
        )
    }

@Composable
fun DebugLogs( context: Context, search: SettingEntrySearch ) {
    var isCrashLog by remember { mutableStateOf( false ) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument( "text/plain" )
    ) {
        if( it == null ) return@rememberLauncherForActivityResult

        // Write in background to prevent UI from freezing up when there's a large file
        CoroutineScope( Dispatchers.IO ).launch {
            val logs = readLogs( context, isCrashLog )
            if( logs.isEmpty() ) {
                Toaster.w( R.string.no_log_available )
                // Delete file if no log available
                context.contentResolver.delete( it, null, null )
            } else
                context.contentResolver.openOutputStream( it )?.use { outStream ->
                    outStream.writer().use { writer ->
                        logs.fastForEach { line ->
                            writer.write( line )
                            writer.write( "\n" )
                        }
                    }
                }
        }
    }

    if( search appearsIn R.string.setting_entry_logs )
        Entry(
            context,
            R.string.setting_entry_logs,
            R.string.setting_description_copy_or_export_logs,
            {
                isCrashLog = false
                launcher.launch( RUNTIME_LOGS_FILENAME )
            },
            { readLogs( context, false ) }
        )

    if( search appearsIn R.string.setting_entry_crash_log )
        Entry(
            context,
            R.string.setting_entry_crash_log,
            R.string.setting_description_copy_or_export_logs,
            {
                isCrashLog = true
                launcher.launch( CRASH_LOGS_FILENAME )
            },
            { readLogs( context, true ) }
        )
}
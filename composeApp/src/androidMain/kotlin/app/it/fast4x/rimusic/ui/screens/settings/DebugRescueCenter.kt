package app.it.fast4x.rimusic.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.isDebugModeEnabled
import app.it.fast4x.rimusic.typography
import app.kreate.android.me.knighthat.component.export.ExportDatabaseDialog
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DebugRescueCenterLauncher(
    onOpenDebugSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val debugEnabled = isDebugModeEnabled()
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .clickable {
                    if (debugEnabled) {
                        showDialog = true
                    } else {
                        onOpenDebugSettings()
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = colorPalette().background2.copy(alpha = if (debugEnabled) 0.88f else 0.5f)
            ),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colorPalette().accent.copy(alpha = if (debugEnabled) 0.24f else 0.08f),
                                colorPalette().background1.copy(alpha = 0.94f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = colorPalette().textSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorPalette().accent.copy(alpha = if (debugEnabled) 0.18f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rescue),
                        contentDescription = null,
                        tint = if (debugEnabled) colorPalette().accent else colorPalette().textDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Rescue Center",
                        style = typography().m,
                        color = if (debugEnabled) colorPalette().text else colorPalette().textDisabled
                    )
                    Text(
                        text = if (debugEnabled) {
                            "Crash, playback and runtime logs"
                        } else {
                            "Open Misc to enable debug logs first"
                        },
                        style = typography().xs,
                        color = if (debugEnabled) colorPalette().textSecondary else colorPalette().textDisabled
                    )
                }
            }
        }
    }

    if (showDialog) {
        DebugRescueCenterDialog(
            onDismiss = { showDialog = false },
            context = context
        )
    }
}

@Composable
fun DebugRescueCenterDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val exportDbDialog = ExportDatabaseDialog(context)
    var refreshToken by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf(RescueLogReport(emptyList())) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshToken) {
        isLoading = true
        val loadedReport = withContext(Dispatchers.IO) { loadRescueLogReport(context) }
        report = loadedReport
        selectedDate = loadedReport.days.firstOrNull()?.date
        isLoading = false
    }

    val selectedDay = report.days.firstOrNull { it.date == selectedDate }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colorPalette().background1.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            val contentScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(contentScroll)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorPalette().accent.copy(alpha = 0.12f),
                                colorPalette().background1.copy(alpha = 0.98f),
                                colorPalette().background0.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorPalette().accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rescue),
                            contentDescription = null,
                            tint = colorPalette().accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rescue Center", style = typography().l, color = colorPalette().text)
                        Text(
                            "Sorted by day. Export only the log slice you need.",
                            style = typography().xs,
                            color = colorPalette().textSecondary
                        )
                    }
                    RescueActionChip(
                        label = "Refresh",
                        color = colorPalette().accent,
                        modifier = Modifier,
                        onClick = { refreshToken++ }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Text("Loading logs...", style = typography().s, color = colorPalette().textSecondary)
                    return@Column
                }

                if (report.days.isEmpty()) {
                    Text("No debug or crash logs found yet.", style = typography().s, color = colorPalette().textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RescueActionChip(
                            label = "Backup DB",
                            color = colorPalette().accent,
                            icon = R.drawable.export_outline,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = exportDbDialog::export
                        )
                        RescueActionChip(
                            label = "Open GitHub",
                            color = Color(0xFF24292F),
                            icon = R.drawable.github_logo,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openGithubIssues(context) }
                        )
                    }
                    return@Column
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    report.days.forEach { day ->
                        val isSelected = selectedDate == day.date
                        RescueDateChip(
                            day = day,
                            selected = isSelected,
                            onClick = { selectedDate = day.date }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                selectedDay?.let { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RescueMetricCard("Errors", day.errorLines.size, Color(0xFFE57373), Modifier.weight(1f))
                        RescueMetricCard("Crashes", day.crashEntries.size, Color(0xFFFFB74D), Modifier.weight(1f))
                        RescueMetricCard("Signals", day.playbackSignalLines.size, colorPalette().accent, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    day.primaryCrashSummary?.let { summary ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.24f), RoundedCornerShape(18.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Latest crash", style = typography().xs, color = colorPalette().textSecondary)
                            Text(summary.title, style = typography().s, color = Color(0xFFFFCC80))
                            summary.fileHint?.let {
                                Text("File: $it", style = typography().xs, color = Color(0xFFFFE0B2))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorPalette().background2.copy(alpha = 0.8f))
                            .border(1.dp, colorPalette().textSecondary.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                            .verticalScroll(verticalScroll)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val linesToShow = day.displayLines
                        if (linesToShow.isEmpty()) {
                            Text(
                                "Nothing critical was captured for this day yet.",
                                style = typography().s,
                                color = colorPalette().textSecondary
                            )
                        } else {
                            linesToShow.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = typography().xs,
                                    color = entryColor(entry)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RescueActionChip(
                            label = "Backup DB",
                            color = colorPalette().accent,
                            icon = R.drawable.export_outline,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = exportDbDialog::export
                        )
                        RescueActionChip(
                            label = "Export ${day.date}",
                            color = colorPalette().accent,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { exportDayLog(context, day) }
                        )
                        RescueActionChip(
                            label = "GitHub Issue",
                            color = Color(0xFF24292F),
                            icon = R.drawable.github_logo,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openGithubIssues(context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RescueDateChip(
    day: RescueLogDay,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        colorPalette().accent.copy(alpha = 0.18f)
    } else {
        colorPalette().background2.copy(alpha = 0.72f)
    }
    val border = if (selected) colorPalette().accent else colorPalette().textSecondary.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(day.date, style = typography().s, color = colorPalette().text)
        Text(
            "${day.errorLines.size} errors • ${day.crashEntries.size} crashes",
            style = typography().xs,
            color = colorPalette().textSecondary
        )
    }
}

@Composable
private fun RescueMetricCard(
    title: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(title, style = typography().xs, color = colorPalette().textSecondary)
        Text(value.toString(), style = typography().m, color = color)
    }
}

@Composable
private fun RescueActionChip(
    label: String,
    color: Color,
    icon: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, style = typography().xs, color = Color.White)
    }
}

private data class RescueLogReport(val days: List<RescueLogDay>)

private data class RescueLogDay(
    val date: String,
    val errorLines: List<String>,
    val playbackSignalLines: List<String>,
    val crashEntries: List<String>,
    val runtimeLines: List<String>
) {
    val primaryCrashSummary: RescueCrashSummary?
        get() = crashEntries.lastOrNull()?.let(::extractCrashSummary)

    val displayLines: List<String>
        get() = buildList {
            addAll(errorLines)
            addAll(playbackSignalLines.filterNot { signal -> errorLines.contains(signal) })
            addAll(crashEntries)
            if (isEmpty()) addAll(runtimeLines.takeLast(40))
        }
}

private data class RescueCrashSummary(
    val title: String,
    val fileHint: String?
)

private fun loadRescueLogReport(context: Context): RescueLogReport {
    val logsDir = context.filesDir.resolve("logs")
    val runtimeFile = File(logsDir, "Cubic-Music_log.txt")
    val crashFile = File(logsDir, "Cubic-Music_crash_log.txt")

    val runtimeGroups = linkedMapOf<String, MutableList<String>>()
    val crashGroups = linkedMapOf<String, MutableList<String>>()

    if (runtimeFile.exists()) {
        runtimeFile.readLines().forEach { line ->
            val date = line.extractLogDate() ?: return@forEach
            runtimeGroups.getOrPut(date) { mutableListOf() }.add(line)
        }
    }

    if (crashFile.exists()) {
        splitCrashBlocks(crashFile.readText()).forEach { block ->
            val date = block.extractLogDate() ?: return@forEach
            crashGroups.getOrPut(date) { mutableListOf() }.add(block.trim())
        }
    }

    val allDates = (runtimeGroups.keys + crashGroups.keys).distinct().sortedDescending()
    return RescueLogReport(
        days = allDates.map { date ->
            val runtimeLines = runtimeGroups[date].orEmpty()
            RescueLogDay(
                date = date,
                errorLines = runtimeLines.filter(::isErrorLine).takeLast(80),
                playbackSignalLines = runtimeLines.filter(::isPlaybackSignalLine).takeLast(60),
                crashEntries = crashGroups[date].orEmpty().takeLast(20),
                runtimeLines = runtimeLines
            )
        }
    )
}

private fun exportDayLog(context: Context, day: RescueLogDay) {
    runCatching {
        val exportFile = File(context.cacheDir, "cubic-rescue-${day.date}.txt").apply {
            writeText(
                buildString {
                    appendLine("Cubic Music Rescue Center")
                    appendLine("Date: ${day.date}")
                    appendLine()
                    appendLine("Errors:")
                    if (day.errorLines.isEmpty()) appendLine("None")
                    day.errorLines.forEach(::appendLine)
                    appendLine()
                    appendLine("Playback signals:")
                    if (day.playbackSignalLines.isEmpty()) appendLine("None")
                    day.playbackSignalLines.forEach(::appendLine)
                    appendLine()
                    appendLine("Crash entries:")
                    if (day.crashEntries.isEmpty()) appendLine("None")
                    day.crashEntries.forEach {
                        appendLine(it)
                        appendLine()
                    }
                    appendLine("Crash summary:")
                    day.primaryCrashSummary?.let {
                        appendLine(it.title)
                        appendLine("File: ${it.fileHint ?: "Unknown"}")
                    } ?: appendLine("None")
                }
            )
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            exportFile
        )

        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Cubic Music rescue log ${day.date}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Export rescue log"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toaster.e("Couldn't export the selected log day")
    }
}

private fun openGithubIssues(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cybruGhost/Cubic-Music/issues/new"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toaster.e("Couldn't open GitHub issues")
    }
}

private fun String.extractLogDate(): String? {
    return takeIf { length >= 10 && substring(0, 10).matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        ?.substring(0, 10)
}

private fun splitCrashBlocks(content: String): List<String> {
    if (content.isBlank()) return emptyList()
    val blocks = mutableListOf<String>()
    val current = StringBuilder()
    content.lineSequence().forEach { line ->
        val startsNewBlock = line.extractLogDate() != null && current.isNotEmpty()
        if (startsNewBlock) {
            blocks += current.toString()
            current.clear()
        }
        current.appendLine(line)
    }
    if (current.isNotEmpty()) blocks += current.toString()
    return blocks
}

private fun extractCrashSummary(block: String): RescueCrashSummary {
    val lines = block.lineSequence().map(String::trim).filter { it.isNotBlank() }.toList()
    val title = lines.firstOrNull { it.contains("Exception", true) || it.contains("Error", true) }
        ?: lines.firstOrNull()
        ?: "Unknown crash"
    val fileHint = lines.firstOrNull {
        it.contains(".kt:", true) || it.contains(".java:", true)
    }?.substringAfterLast('(')?.substringBefore(')')
        ?: lines.firstOrNull {
            it.contains("at ", true) && (it.contains(".kt", true) || it.contains(".java", true))
        }
    return RescueCrashSummary(title = title, fileHint = fileHint)
}

private fun isErrorLine(line: String): Boolean {
    return line.contains(" ERROR:", true) ||
        line.contains(" WARN:", true) ||
        line.contains("PlaybackException", true) ||
        line.contains("failed", true)
}

private fun isPlaybackSignalLine(line: String): Boolean {
    return line.contains("onAudioFocusChange", true) ||
        line.contains("Audio focus", true) ||
        line.contains("onTaskRemoved", true) ||
        line.contains("onDestroy called", true) ||
        line.contains("restartForegroundOrStop", true) ||
        line.contains("Media data removed", true) ||
        line.contains("Media player removed", true) ||
        line.contains("notification removed", true)
}

private fun entryColor(entry: String): Color {
    return when {
        entry.contains("ERROR", true) || entry.contains("failed", true) || entry.contains("Exception", true) -> Color(0xFFE57373)
        entry.contains("WARN", true) || entry.contains("LOST", true) -> Color(0xFFFFB74D)
        entry.contains("Audio focus", true) || entry.contains("onTaskRemoved", true) -> Color(0xFF4DD0E1)
        else -> Color(0xFFECEFF1)
    }
}

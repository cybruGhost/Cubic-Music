package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop

@Composable
internal fun CubicProfilePage(libraryCount: Int, downloadCount: Int, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Profile", color = CubicColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CubicColors.PanelRaised).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CubicLogo(66.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Cubic Music listener", color = CubicColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Local desktop profile", color = CubicColors.TextSecondary, fontSize = 12.sp)
            }
            CubicActionTile("Settings", Icons.Rounded.Settings, onOpenSettings)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CubicStatCard("Library", "$libraryCount tracks", Icons.Rounded.LibraryMusic, Modifier.weight(1f))
            CubicStatCard("Downloaded", "$downloadCount offline", Icons.Rounded.DownloadDone, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun CubicSettingsPage(downloadCount: Int, onClearData: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", color = CubicColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        CubicSettingsCard("Appearance", Icons.Rounded.Palette) {
            Text("Dark glass", color = CubicColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Lime accent ? More appearance controls can be added here without changing playback.", color = CubicColors.TextSecondary, fontSize = 11.sp)
        }
        CubicSettingsCard("Offline music", Icons.Rounded.DownloadDone) {
            Text("$downloadCount downloaded tracks", color = CubicColors.TextSecondary, fontSize = 12.sp)
            Text(CubicDownloadStore.directory.absolutePath, color = CubicColors.TextMuted, fontSize = 11.sp)
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(CubicColors.AccentSoft).clickable {
                    runCatching { Desktop.getDesktop().open(CubicDownloadStore.directory) }
                }.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, tint = CubicColors.Accent, modifier = Modifier.size(18.dp))
                Text("Open download folder", color = CubicColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        CubicSettingsCard("Desktop data", Icons.Rounded.DeleteSweep) {
            Text("Clear listening history, favorites, playlists and offline downloads from this computer.", color = CubicColors.TextSecondary, fontSize = 11.sp)
            Text(
                "Clear desktop data",
                color = CubicColors.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CubicColors.Danger.copy(alpha = 0.10f))
                    .clickable { confirmClear = true }.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear desktop data?") },
        text = { Text("This removes Cubic Music history, favorites, playlists and downloaded files on this computer. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClearData() }) { Text("Clear", color = CubicColors.Danger) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )
}

@Composable
private fun CubicSettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CubicColors.PanelRaised).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
            Text(title, color = CubicColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun CubicStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(CubicColors.PanelRaised).padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(42.dp).background(CubicColors.AccentSoft, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CubicColors.Accent, modifier = Modifier.size(21.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = CubicColors.TextMuted, fontSize = 10.sp)
            Text(value, color = CubicColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CubicActionTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = CubicColors.Accent, modifier = Modifier.size(19.dp))
        Text(label, color = CubicColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

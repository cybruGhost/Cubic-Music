package app.it.fast4x.rimusic.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.CShareImportDialog
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.bold
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.secondary
import app.it.fast4x.rimusic.utils.semiBold
import app.kreate.android.R
import app.kreate.android.me.knighthat.component.tab.CsvImportConversionHost
import app.kreate.android.me.knighthat.component.tab.ImportSongsFromCSV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged

private const val EXPORTIFY_URL = "https://exportify.app"

@Composable
internal fun ExportifyScreen() {
    val uriHandler = LocalUriHandler.current
    val importPlaylist = ImportSongsFromCSV()
    val playlists by remember {
        Database.playlistTable.allAsPreview().distinctUntilChanged()
    }.collectAsState(emptyList(), Dispatchers.IO)
    val shareablePlaylists = remember(playlists) {
        playlists.filterNot { preview -> preview.playlist.isYoutubePlaylist }
    }

    var showCShareLibrary by remember { mutableStateOf(false) }
    var showCShareImport by remember { mutableStateOf(false) }

    if (showCShareImport) {
        CShareImportDialog(onDismiss = { showCShareImport = false })
    }
    if (showCShareLibrary) {
        CShareLibraryDialog(
            playlists = shareablePlaylists,
            onDismiss = { showCShareLibrary = false },
            onImportClick = {
                showCShareLibrary = false
                showCShareImport = true
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 20.dp,
            end = 16.dp,
            bottom = Dimensions.bottomSpacer,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "exportify_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = colorPalette().accent.copy(alpha = 0.16f),
                    contentColor = colorPalette().accent,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.export_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = stringResource(R.string.exportify_native_title),
                        style = typography().xl.bold.color(colorPalette().text),
                    )
                    BasicText(
                        text = stringResource(R.string.exportify_native_subtitle),
                        style = typography().s.secondary,
                    )
                }
            }
        }

        item(key = "exportify_open") {
            ExportifyAction(
                title = stringResource(R.string.exportify_open),
                detail = stringResource(R.string.exportify_open_detail),
                icon = R.drawable.export_outline,
                emphasized = true,
                onClick = { uriHandler.openUri(EXPORTIFY_URL) },
            )
        }

        item(key = "exportify_local_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExportifyCompactAction(
                    title = stringResource(R.string.import_playlist),
                    icon = R.drawable.import_outline,
                    onClick = importPlaylist::onShortClick,
                    modifier = Modifier.weight(1f),
                )
                ExportifyCompactAction(
                    title = stringResource(R.string.cshare_title),
                    icon = R.drawable.share_social,
                    onClick = { showCShareLibrary = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "exportify_steps") {
            ExportifyExpandableSection(
                title = stringResource(R.string.exportify_how_it_works),
                icon = R.drawable.playlist,
                initiallyExpanded = true,
            ) {
                ExportifyStep(1, stringResource(R.string.exportify_step_one))
                ExportifyStep(2, stringResource(R.string.exportify_step_two))
                ExportifyStep(3, stringResource(R.string.exportify_step_three))
                ExportifyStep(4, stringResource(R.string.exportify_step_four))
            }
        }

        item(key = "exportify_formats") {
            ExportifyExpandableSection(
                title = stringResource(R.string.exportify_import_tools),
                icon = R.drawable.import_outline,
            ) {
                BasicText(
                    text = stringResource(R.string.exportify_import_tools_detail),
                    style = typography().s.color(colorPalette().textSecondary),
                )
            }
        }
    }

    CsvImportConversionHost()
}

@Composable
private fun ExportifyAction(
    title: String,
    detail: String,
    @DrawableRes icon: Int,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val container = if (emphasized) colorPalette().accent else colorPalette().background1
    val content = if (emphasized) colorPalette().background0 else colorPalette().text
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = title, style = typography().m.semiBold.color(content))
                BasicText(text = detail, style = typography().xs.color(content.copy(alpha = 0.72f)))
            }
        }
    }
}

@Composable
private fun ExportifyCompactAction(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = colorPalette().background1,
        contentColor = colorPalette().text,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colorPalette().accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = title,
                style = typography().s.semiBold.color(colorPalette().text),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ExportifyExpandableSection(
    title: String,
    @DrawableRes icon: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Surface(
        color = colorPalette().background1,
        contentColor = colorPalette().text,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colorPalette().accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicText(
                    text = title,
                    style = typography().m.semiBold.color(colorPalette().text),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.chevron_up else R.drawable.chevron_down
                    ),
                    contentDescription = null,
                    tint = colorPalette().textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ExportifyStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = colorPalette().accent.copy(alpha = 0.15f),
            contentColor = colorPalette().accent,
            shape = RoundedCornerShape(6.dp),
        ) {
            BasicText(
                text = number.toString(),
                style = typography().xs.bold.color(colorPalette().accent),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = text,
            style = typography().s.color(colorPalette().textSecondary),
            modifier = Modifier.weight(1f),
        )
    }
}

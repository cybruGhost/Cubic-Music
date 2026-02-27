package app.kreate.android.me.knighthat.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.kreate.android.me.knighthat.component.dialog.InputDialogConstraints
import app.kreate.android.me.knighthat.component.dialog.TextInputDialog

abstract class RenameDialog(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>
): TextInputDialog(InputDialogConstraints.ALL), MenuIcon, Descriptive {

    override var isActive: Boolean by activeState
    override var value: TextFieldValue by valueState

    override fun onShortClick() = showDialog()

    @Composable
    override fun LeadingIcon() = Icon(
        painter = icon,
        tint = colorPalette().text,
        contentDescription = "Rename dialog text box icon",
        modifier = Modifier.size( 20.dp )
    )
}
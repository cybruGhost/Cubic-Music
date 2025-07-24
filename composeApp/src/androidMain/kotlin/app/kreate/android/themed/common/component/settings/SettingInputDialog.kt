package app.kreate.android.themed.common.component.settings

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import app.kreate.android.Preferences
import me.knighthat.component.dialog.TextInputDialog

class SettingInputDialog<T>(
    constraint: String,
    private val preference: Preferences<T>,
    private val title: String,
    private val onValueSet: (String) -> Unit,
    override val keyboardOption: KeyboardOptions
) : TextInputDialog(constraint) {

    override val dialogTitle: String
        @Composable
        get() = title

    override var value: TextFieldValue by mutableStateOf( TextFieldValue(preference.value.toString()) )
    override var isActive: Boolean by mutableStateOf(false)

    override fun onSet( newValue: String ) {
        super.onSet(newValue)
        if( errorMessage.isNotBlank() )
            return

        onValueSet( newValue )

        hideDialog()
    }

    override fun hideDialog() {
        super.hideDialog()
        // Some processing might have happened after the value is set,
        // setting this back to match preference's value is best idea
        value = TextFieldValue(preference.value.toString())
        errorMessage = ""
    }
}
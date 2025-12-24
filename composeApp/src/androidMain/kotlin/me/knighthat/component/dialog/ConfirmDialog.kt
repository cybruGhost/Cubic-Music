package me.knighthat.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette

interface ConfirmDialog: InteractiveDialog {

    fun onConfirm()

    @Composable
    override fun Buttons() = Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = ::hideDialog,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorPalette().background2,
                contentColor = colorPalette().text
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            InteractiveDialog.CancelButton(
                onCancel = ::hideDialog
            )
        }
        
        Button(
            onClick = ::onConfirm,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorPalette().accent,
                contentColor = colorPalette().textSecondary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            InteractiveDialog.ConfirmButton(
                onConfirm = ::onConfirm
            )
        }
    }
}
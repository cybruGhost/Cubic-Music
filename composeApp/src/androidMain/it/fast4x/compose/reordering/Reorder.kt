package it.fast4x.compose.reordering

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput

private fun Modifier.reorder(
    reorderingState: ReorderingState,
    index: Int,
    detectDragGestures: DetectDragGestures
) = this.pointerInput(reorderingState) {
    with(detectDragGestures) {
        detectDragGestures(
            onDragStart = { reorderingState.onDragStart(index) },
            onDrag = reorderingState::onDrag,
            onDragEnd = reorderingState::onDragEnd,
            onDragCancel = reorderingState::onDragEnd
        )
    }
}

fun Modifier.reorder(
    reorderingState: ReorderingState,
    index: Int
) = this.reorder(
    reorderingState = reorderingState,
    index = index,
    detectDragGestures = PointerInputScope::detectDragGestures
)

private fun interface DetectDragGestures {
    suspend fun PointerInputScope.detectDragGestures(
        onDragStart: (Offset) -> Unit,
        onDragEnd: () -> Unit,
        onDragCancel: () -> Unit,
        onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
    )
}
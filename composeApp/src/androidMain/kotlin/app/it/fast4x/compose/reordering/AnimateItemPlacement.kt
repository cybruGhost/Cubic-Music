package app.it.fast4x.compose.reordering

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

@ExperimentalFoundationApi
fun LazyItemScope.localAnimateItemPlacement(
    modifier: Modifier,
    reorderingState: ReorderingState
) = if (reorderingState.draggingIndex == -1) modifier.animateItem() else modifier

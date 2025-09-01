package me.knighthat.component.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.utils.isRecommendationEnabledKey
import it.fast4x.rimusic.utils.rememberPreference

class SmartShuffle private constructor(
    private val isRecommendationEnabled: () -> Boolean,
    private val isRecommendationsLoading: () -> Boolean,
    private val onToggleRecommendation: () -> Unit
): MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke(
            isRecommendationEnabled: () -> Boolean,
            isRecommendationsLoading: () -> Boolean,
            onToggleRecommendation: () -> Unit
        ): SmartShuffle = SmartShuffle(
            isRecommendationEnabled,
            isRecommendationsLoading,
            onToggleRecommendation
        )
    }

    override val iconId: Int = R.drawable.smart_shuffle
    override val messageId: Int = R.string.info_smart_recommendation
    override val menuIconTitle: String
        @Composable
        get() = stringResource(R.string.info_smart_recommendation)

    override fun onShortClick() {
        onToggleRecommendation()
    }

    @Composable
    override fun ToolBarButton() {
        Box(
            modifier = Modifier.size(48.dp), // Standard IconButton size
            contentAlignment = Alignment.Center
        ) {
            if (isRecommendationsLoading()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colorPalette().text,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = { onToggleRecommendation() }) {
                    Icon(
                        painter = painterResource(R.drawable.smart_shuffle),
                        contentDescription = stringResource(R.string.info_smart_recommendation),
                        tint = if (isRecommendationEnabled()) colorPalette().text else colorPalette().textDisabled
                    )
                }
            }
        }
    }
} 
package me.knighthat.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.BuildConfig
import app.kreate.android.R
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.bold
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.navigationBarPositionKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import it.fast4x.rimusic.ui.styling.ModernBlackColorPalette
import it.fast4x.rimusic.utils.colorPaletteModeKey
import java.util.stream.Stream

class ChangelogsDialog(
    seenChangelogVersionState: MutableState<String>,
) {

    var seenChangelogVersion: String by seenChangelogVersionState
    var isActive: Boolean by mutableStateOf( true )

    fun hideDialog() {
        isActive = false
        seenChangelogVersion = BuildConfig.VERSION_NAME
    }

    private fun parseReleaseNotes( lines: Stream<String>): List<Section> {
        val sections = mutableListOf<Section>()
        var currentTitle: String? = null
        val currentChanges = mutableListOf<String>()

        fun packSection( title: String = currentTitle!! ) {
            // Because [currentChanges] is a mutable list, passing it here
            // will only pass the reference, any subsequent changes will be
            // updated to this list.
            sections.add( Section(title, currentChanges.toList()) )
            currentChanges.clear()
        }

        lines.forEach {  line ->
            when {
                line.endsWith( ":" ) -> {
                    // If [currentTitle] is not null, it means another section is reached.
                    // Therefore, pack last section to a [Section]
                    currentTitle?.let( ::packSection )

                    currentTitle = line.removeSuffix(":")
                }
                line.trim().startsWith("-") -> {
                    if( line.isNotBlank() )
                        currentChanges.add( line.trim() )
                }
            }
        }
        packSection()

        return sections
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun Render() {
        if (!isActive) return
        
        var navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)
        var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.System)
        
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { hideDialog() }
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val sections = remember {
                parseReleaseNotes(
                    appContext().resources
                                .openRawResource( R.raw.release_notes )
                                .bufferedReader( Charsets.UTF_8 )
                                .lines()
                )
            }
            var selectedTab by remember { mutableIntStateOf(0) }

            // Header with title and version
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    animationSpec = tween(300),
                    initialScale = 0.9f
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                            Color(0xFF1A1A1A) // Gray dark for pitch black themes
                        } else {
                            colorPalette().background1
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.changelog_list),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicText(
                                text = stringResource(R.string.update_changelogs, BuildConfig.VERSION_NAME),
                                style = typography().l.bold.copy(color = colorPalette().text)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicText(
                                text = stringResource(R.string.view_changelog_settings_text),
                                style = typography().xs.copy(color = colorPalette().textSecondary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs with animations
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                    animationSpec = tween(400),
                    initialScale = 0.9f
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                            Color(0xFF1A1A1A) // Gray dark for pitch black themes
                        } else {
                            colorPalette().background1
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = colorPalette().text,
                        indicator = { tabPositions ->
                            val selectedPosition by remember {
                                derivedStateOf { tabPositions[selectedTab] }
                            }
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset( selectedPosition ),
                                color = colorPalette().accent,
                                width = selectedPosition.width,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sections.forEachIndexed { index, section ->
                            Tab(
                                selected = index == selectedTab,
                                onClick = { selectedTab = index }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                                                         // Icône pour chaque section
                                     Icon(
                                         painter = painterResource(
                                             when (section.title.lowercase()) {
                                                 "new" -> R.drawable.add
                                                 "changed" -> R.drawable.title_edit
                                                 "improved" -> R.drawable.refresh_circle
                                                 "fixed" -> R.drawable.alert
                                                 else -> R.drawable.information
                                             }
                                         ),
                                        contentDescription = null,
                                        tint = if (index == selectedTab) colorPalette().accent else colorPalette().textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicText(
                                        text = section.title,
                                        style = typography().s.semiBold.copy( 
                                            color = if (index == selectedTab) colorPalette().accent else colorPalette().text 
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // content with animations
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    animationSpec = tween(500),
                    initialScale = 0.9f
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                            Color(0xFF1A1A1A) // Gray dark for pitch black themes
                        } else {
                            colorPalette().background1
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    val configuration = LocalWindowInfo.current
                                         val changesBoxHeight by remember {
                         derivedStateOf {
                             val maxHeight = (configuration.containerSize.height * .8f).toInt()
                             val calculatedHeight = sections.maxOf { it.changes.size } * 32
                             calculatedHeight.coerceAtMost(maxHeight)
                         }
                     }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(changesBoxHeight.dp)
                    ) {
                        items(
                            items = sections[selectedTab].changes,
                            key = null
                        ) { change ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                // Puce colored according to the section
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when (sections[selectedTab].title.lowercase()) {
                                                "new" -> Color(0xFF4CAF50) // Vert
                                                "changed" -> Color(0xFFFF9800) // Orange
                                                "improved" -> Color(0xFF2196F3) // Bleu
                                                "fixed" -> Color(0xFFF44336) // Rouge
                                                else -> colorPalette().accent
                                            }
                                        )
                                        .padding(top = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                BasicText(
                                    text = change.removePrefix("- "),
                                    style = typography().xs.copy(color = colorPalette().text),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button with animation
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    animationSpec = tween(600),
                    initialScale = 0.9f
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hideDialog() },
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette().accent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.checkmark),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicText(
                                text = stringResource(R.string.i_understand),
                                style = typography().s.semiBold.copy(color = Color.White)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    data class Section( val title: String, val changes: List<String> )
}
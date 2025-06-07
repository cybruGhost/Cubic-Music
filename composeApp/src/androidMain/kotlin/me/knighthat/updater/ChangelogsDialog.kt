package me.knighthat.updater

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.BuildConfig
import app.kreate.android.R
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.bold
import me.knighthat.component.dialog.Dialog
import java.util.stream.Stream

class ChangelogsDialog(
    seenChangelogVersionState: MutableState<String>,
): Dialog {

    var seenChangelogVersion: String by seenChangelogVersionState
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.update_changelogs, BuildConfig.VERSION_NAME )

    // Automatically enable dialog when this class is init
    // This is an assumption, a check is required before creating this object
    override var isActive: Boolean by mutableStateOf( true )

    override fun hideDialog() {
        super.hideDialog()
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

    @Composable
    override fun DialogBody() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
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

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colorPalette().background0,
                contentColor = colorPalette().text,
                indicator = { tabPositions ->
                    val selectedPosition by remember {
                        derivedStateOf { tabPositions[selectedTab] }
                    }
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset( selectedPosition ),
                        color = colorPalette().accent,
                        width = selectedPosition.width
                    )
                },
                modifier = Modifier.fillMaxWidth( .9f )
            ) {
                sections.forEachIndexed { index, section ->
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index }
                    ) {
                        BasicText(
                            text = section.title,
                            style = typography().s.bold.copy( color = colorPalette().text )
                        )
                    }
                }
            }

            val configuration = LocalWindowInfo.current
            val changesBoxHeight by remember {
                derivedStateOf {
                    val aThirdScreenHeight = (configuration.containerSize.height * .3f).toInt()

                    sections.maxOf { it.changes.size }
                            // Make-up number, seems to be appropriate for the height of `xs`
                            .times( 22 )
                            // Ensure box's height can't go above 30% of screen
                            .coerceAtMost( aThirdScreenHeight )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues( top = 10.dp ),
                modifier = Modifier.fillMaxWidth( .9f )
                    .requiredHeight( changesBoxHeight.dp )
            ) {
                items(
                    items = sections[selectedTab].changes,
                    key = null      // Lines aren't going to change so key should be null
                ) {
                    BasicText(
                        text = it,
                        style = typography().xs
                    )
                }
            }
        }
    }

    data class Section( val title: String, val changes: List<String> )
}
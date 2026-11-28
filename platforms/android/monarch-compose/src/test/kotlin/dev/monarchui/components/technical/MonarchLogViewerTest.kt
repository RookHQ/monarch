package dev.monarchui.components.technical

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.technical.logviewer.MonarchLogLevel
import dev.monarchui.components.technical.logviewer.MonarchLogLine
import dev.monarchui.components.technical.logviewer.MonarchLogViewer
import dev.monarchui.components.technical.logviewer.MonarchLogViewerLabels
import dev.monarchui.components.technical.logviewer.MonarchLogViewerLogic
import dev.monarchui.components.technical.logviewer.MonarchLogViewerSearchMode
import dev.monarchui.components.technical.logviewer.MonarchLogViewerTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Start = 1_763_100_000_000L

private val Lines = listOf(
    MonarchLogLine("1", "Server listening on :8080", time = Start, source = "api"),
    MonarchLogLine("2", "Cache warmed", MonarchLogLevel.Debug, Start + 40, "cache"),
    MonarchLogLine("3", "Slow query on invoices", MonarchLogLevel.Warn, Start + 900, "db"),
    MonarchLogLine("4", "Payment provider timed out", MonarchLogLevel.Error, Start + 1_200, "billing"),
    MonarchLogLine("5", "Retrying payment", time = Start + 1_300, source = "billing"),
)

private fun many(count: Int): List<MonarchLogLine> = (1..count).map { MonarchLogLine("m$it", "Request $it served") }

internal object MonarchLogViewerFixtures : ComponentFixtures {
    override val id = "log-viewer"
    override val fixtures = listOf(
        fixture("following, timestamps") { MonarchLogViewer(lines = Lines) },
        fixture("highlight, line numbers, wrapped") {
            MonarchLogViewer(
                lines = Lines,
                query = "payment",
                searchMode = MonarchLogViewerSearchMode.Highlight,
                wrapped = true,
                showLineNumbers = true,
            )
        },
        fixture("paused with new lines") { MonarchLogViewer(lines = many(40), following = false) },
        fixture("filtered to nothing") { MonarchLogViewer(lines = Lines, query = "zebra") },
        fixture("loading") { MonarchLogViewer(lines = emptyList(), loading = true) },
        fixture("empty, with an action") {
            MonarchLogViewer(lines = emptyList(), showTimestamps = false) {
                TextButton(onClick = {}) { Text("Download") }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchLogViewerTest {
    @get:Rule
    val rule = createComposeRule()

    private val chip = { label: String -> rule.onNode(hasText(label) and hasClickAction()) }
    private val tall = Modifier.height(2000.dp)

    private fun announcement(): String? = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        .fetchSemanticsNodes().firstNotNullOfOrNull {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        }

    @Test
    fun eachLineIsReadAsLevelTimeSourceAndMessage() {
        rule.setMonarchContent { MonarchLogViewer(lines = Lines.take(1), modifier = tall) }
        val time = MonarchLogViewerLogic.formatTime(Start, TimeZone.getDefault())
        rule.onNodeWithContentDescription("Info, $time, api, Server listening on :8080").assertExists()
        rule.onNodeWithContentDescription("Log").assertExists()
        rule.onNodeWithText("1 line", useUnmergedTree = true).assertExists()
    }

    @Test
    fun levelFiltersShowCountsAndHideLines() {
        val changes = mutableListOf<Set<MonarchLogLevel>>()
        rule.setMonarchContent {
            MonarchLogViewer(lines = Lines, modifier = tall, showTimestamps = false, onLevelsChange = { changes += it })
        }
        chip("Info 2").assertIsSelected()
        chip("Error 1").assertIsSelected()
        chip("Info 2").performClick().assertIsNotSelected()
        assertEquals(setOf(MonarchLogLevel.Debug, MonarchLogLevel.Warn, MonarchLogLevel.Error), changes.single())
        rule.onNodeWithContentDescription("Info, api, Server listening on :8080").assertDoesNotExist()
        rule.onNodeWithText("3 of 5 lines", useUnmergedTree = true).assertExists()
        rule.onNode(hasContentDescription("Levels")).assertExists()
    }

    @Test
    fun searchFiltersOrHighlightsAndClearFiltersResetsBoth() {
        var mode by mutableStateOf(MonarchLogViewerSearchMode.Filter)
        rule.setMonarchContent {
            MonarchLogViewer(lines = Lines, modifier = tall, searchMode = mode, showTimestamps = false)
        }
        rule.onNode(hasSetTextAction()).performTextInput("PAYMENT")
        rule.onNodeWithText("2 of 5 lines", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Info, cache, Cache warmed").assertDoesNotExist()
        mode = MonarchLogViewerSearchMode.Highlight
        rule.waitForIdle()
        rule.onNodeWithText("5 lines", useUnmergedTree = true).assertExists()
        mode = MonarchLogViewerSearchMode.Filter
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No lines match the filters.", useUnmergedTree = true).assertExists()
        chip("Debug 1").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Clear filters").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("5 lines", useUnmergedTree = true).assertExists()
        chip("Debug 1").assertIsSelected()
    }

    @Test
    fun followingKeepsTheNewestLineInViewAndADragPausesIt() {
        val lines = mutableStateListOf(*many(60).toTypedArray())
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchLogViewer(
                lines = lines,
                modifier = Modifier.height(400.dp),
                onFollowingChange = { changes += it },
            )
        }
        rule.onNodeWithContentDescription("Info, Request 60 served").assertExists()
        lines += MonarchLogLine("new", "Request 61 served")
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Info, Request 61 served").assertExists()
        rule.onNodeWithContentDescription("Log").performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertEquals(listOf(false), changes)
        assertEquals("Following paused", announcement())
        chip("Follow").assertIsNotSelected()
        lines += MonarchLogLine("newer", "Request 62 served")
        lines += MonarchLogLine("newest", "Request 63 served")
        rule.waitForIdle()
        rule.onNodeWithText("2 new lines").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf(false, true), changes)
        assertEquals("Following new lines", announcement())
        rule.onNodeWithContentDescription("Info, Request 63 served").assertExists()
    }

    @Test
    fun wrapAndFollowAreTogglesTheAppCanControl() {
        var wrapped by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchLogViewer(lines = Lines, modifier = tall, wrapped = wrapped, onWrappedChange = { wrapped = it })
        }
        chip("Wrap lines").assertIsNotSelected().performClick()
        assertTrue(wrapped)
        chip("Wrap lines").assertIsSelected()
        chip("Follow").assertIsSelected().performClick().assertIsNotSelected()
        rule.onNodeWithText("Jump to latest").assertExists()
    }

    @Test
    fun loadingAndEmptyStates() {
        var loading by mutableStateOf(true)
        rule.setMonarchContent { MonarchLogViewer(lines = emptyList(), modifier = tall, loading = loading) }
        rule.onNodeWithText("Loading log", substring = true, useUnmergedTree = true).assertExists()
        loading = false
        rule.waitForIdle()
        rule.onNodeWithText("No log lines yet.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theToolbarFollowsTheDirectionTheLinesStayLeftToRightAndTextFits() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchLogViewer(lines = Lines.take(2), showTimestamps = false) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            chip("Debug 1").assertTouchTargetAtLeast48dp(rule.density).assertIsBefore(chip("Info 1"), direction)
            chip("Follow").assertIsBefore(chip("Wrap lines"), direction)
        }
        environment.layoutDirection = LayoutDirection.Ltr
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        rule.onNodeWithText("2 lines", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLogViewer(
                lines = Lines.take(1),
                modifier = tall,
                showTimestamps = false,
                labels = MonarchLogViewerLabels(
                    log = "Journal",
                    levelName = { it.name.uppercase(Locale.ROOT) },
                    follow = "Suivre",
                    wrap = "Retour à la ligne",
                    count = { shown, total -> "$shown/$total" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Journal").assertExists()
        rule.onNodeWithContentDescription("INFO, api, Server listening on :8080").assertExists()
        chip("INFO 1").assertExists()
        chip("Suivre").assertExists()
        chip("Retour à la ligne").assertExists()
        rule.onNodeWithText("1/1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLogicAndColoursComeFromTheTheme() {
        val t = MonarchLogViewerTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, t.levelColor(MonarchLogLevel.Error, colors))
            assertEquals(colors.warningStrong, t.levelColor(MonarchLogLevel.Warn, colors))
            assertEquals(colors.infoStrong, t.levelColor(MonarchLogLevel.Info, colors))
            assertEquals(colors.textMuted, t.levelColor(MonarchLogLevel.Debug, colors))
            assertEquals(colors.dangerSubtle, t.lineBackground(MonarchLogLevel.Error, colors))
            assertEquals(colors.accentSubtle, t.matchBackground(colors))
            assertEquals(colors.bgSubtle, t.surface(colors))
        }
        assertEquals("00:00:01.234", MonarchLogViewerLogic.formatTime(1_234L, TimeZone.getTimeZone("UTC")))
        assertEquals("02:00:01.234", MonarchLogViewerLogic.formatTime(1_234L, TimeZone.getTimeZone("GMT+02:00")))
        assertEquals(listOf(0..2, 8..10), MonarchLogViewerLogic.matchRanges("Foo bar foo", "foo"))
        assertEquals(emptyList(), MonarchLogViewerLogic.matchRanges("Foo", ""))
        val l = MonarchLogViewerLogic
        val filter = MonarchLogViewerSearchMode.Filter
        assertEquals(listOf(3, 4), l.visible(Lines, t.AllLevels, " payment ", filter))
        assertEquals(listOf(3), l.visible(Lines, setOf(MonarchLogLevel.Error), "", filter))
        assertEquals((0..4).toList(), l.visible(Lines, t.AllLevels, "zzz", MonarchLogViewerSearchMode.Highlight))
        assertEquals(2, MonarchLogViewerLogic.counts(Lines)[MonarchLogLevel.Info])
        val both = setOf(MonarchLogLevel.Info, MonarchLogLevel.Debug)
        assertEquals(setOf(MonarchLogLevel.Info), MonarchLogViewerLogic.toggled(both, MonarchLogLevel.Debug))
        assertEquals("   7", MonarchLogViewerLogic.lineNumber(6))
        assertEquals("1,234", MonarchLogViewerLogic.count(1234, Locale.US))
        assertEquals(0, MonarchLogViewerLogic.newLines(3, 5))
    }
}

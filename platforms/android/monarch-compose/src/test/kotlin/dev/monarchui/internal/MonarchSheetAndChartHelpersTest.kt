package dev.monarchui.internal

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/** The search list sheet, the single-choice sheet, the chart formats, the drag auto scroll and the clock pause. */
@RunWith(RobolectricTestRunner::class)
class MonarchSheetAndChartHelpersTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSearchListSheetShowsTheTitleAsAHeadingTheSearchTheRowsAndTheButtons() {
        var query by mutableStateOf("")
        var dismissed = 0
        rule.setMonarchContent {
            MonarchSearchListSheet(
                title = "Assignee",
                query = query,
                onQueryChange = { query = it },
                onDismiss = { dismissed++ },
                searchPlaceholder = "Search people",
                searching = false,
                status = { Text("2 chosen") },
                buttons = { MonarchButton(text = "Done", onClick = { dismissed++ }) },
            ) {
                items(listOf("Ada", "Grace").filter { it.contains(query, ignoreCase = true) }) { Text(it) }
            }
        }
        rule.onNodeWithText("Assignee").assert(isHeading())
        rule.onNodeWithText("2 chosen").assertExists()
        rule.onNodeWithText("Ada").assertExists()
        rule.runOnUiThread { query = "gr" }
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()
        rule.onNodeWithText("Ada").assertDoesNotExist()
        rule.onNodeWithText("Grace").assertExists()
        rule.onNodeWithText("Done").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theSingleChoiceSheetMarksTheChosenRowAndReportsTheChoice() {
        var chosen: String? = "b"
        var closed = false
        rule.setMonarchContent {
            MonarchSingleChoiceSheet(
                texts = MonarchChoiceSheetTexts("Workspace", "Search", "Loading", "No matches", "Cancel"),
                query = "",
                onQueryChange = {},
                entries = listOf(
                    MonarchChoiceSheetEntry("a", "Acme", null, "Acme workspace") {},
                    MonarchChoiceSheetEntry("b", "Beta", "@beta", "Beta workspace") {},
                ),
                selected = chosen,
                loading = false,
                actions = listOf(MonarchChoiceSheetAction("New workspace") {}),
                onChoose = { chosen = it },
                onDismiss = { closed = true },
            )
        }
        rule.onNodeWithContentDescription("Beta workspace")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        rule.onNodeWithContentDescription("Acme workspace").performClick()
        assertEquals("a", chosen)
        rule.onNodeWithText("New workspace").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(true, closed)
    }

    @Test
    fun theSingleChoiceSheetSaysWhenNothingMatches() {
        rule.setMonarchContent {
            MonarchSingleChoiceSheet(
                texts = MonarchChoiceSheetTexts("Workspace", "Search", "Loading", "No matches", "Cancel"),
                query = "zz",
                onQueryChange = {},
                entries = emptyList(),
                selected = null,
                loading = false,
                actions = emptyList(),
                onChoose = {},
                onDismiss = {},
            )
        }
        rule.onNodeWithText("No matches").assertExists()
    }

    @Test
    fun theChartFormatsWriteValuesCompactValuesAndPercentagesInTheLocale() {
        assertEquals("1,234.567", MonarchChartFormats.value(Locale.US)(1234.5671))
        assertEquals("1.2K", MonarchChartFormats.value(Locale.US, compact = true)(1200.0))
        assertEquals("1.234,5", MonarchChartFormats.value(Locale.GERMANY)(1234.5))
        assertEquals("12.5%", MonarchChartFormats.percent(Locale.US, 1)(0.125))
        assertEquals("13%", MonarchChartFormats.percent(Locale.US, 0)(0.126))
        assertEquals("13%", MonarchChartFormats.percent(Locale.US, -1)(0.126))
    }

    @Test
    fun theDragAutoScrollMeasuresTheOverscrollInTheDirectionOfTheMoveAndCapsTheStep() {
        assertEquals(20f, MonarchDragAutoScroll.overscroll(900f, 1020f, 0, 1000, direction = 1f))
        assertEquals(0f, MonarchDragAutoScroll.overscroll(900f, 1020f, 0, 1000, direction = -1f))
        assertEquals(-30f, MonarchDragAutoScroll.overscroll(-30f, 90f, 0, 1000, direction = -1f))
        assertEquals(0f, MonarchDragAutoScroll.overscroll(-30f, 90f, 0, 1000, direction = 0f))
        assertEquals(12f, MonarchDragAutoScroll.step(40f, maxStep = 12f))
        assertEquals(-5f, MonarchDragAutoScroll.step(-5f, maxStep = 12f))
    }

    @Test
    fun theClockStopsWhileNotRunningAndKeepsTheLastTime() {
        val minute = MonarchClock.MINUTE_MILLIS
        val start = 1_800_000_000_000L
        var time = start
        var read = 0L
        var running by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { read = rememberMonarchClock(running = running, currentTime = { time }) }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnUiThread { running = false }
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
        time = start + minute
        rule.mainClock.advanceTimeBy(2 * minute)
        rule.mainClock.advanceTimeByFrame()
        assertEquals(start, read)
        rule.runOnUiThread { running = true }
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(start + minute, read)
    }
}

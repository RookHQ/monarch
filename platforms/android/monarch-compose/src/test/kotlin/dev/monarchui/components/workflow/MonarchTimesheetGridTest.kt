@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGrid
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridDay
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridLabels
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridLayout
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridRow
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridSummaryRow
import dev.monarchui.components.workflow.timesheetgrid.MonarchTimesheetGridTokens
import dev.monarchui.components.workflow.timesheetgrid.TimesheetGridCommit
import dev.monarchui.components.workflow.timesheetgrid.TimesheetGridLayoutRules
import dev.monarchui.components.workflow.timesheetgrid.TimesheetGridRules
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Monday 12 January 2026, 00:00 UTC. */
private const val Monday = 1_768_176_000_000L
private const val Tuesday = Monday + 86_400_000L
private const val Wednesday = Tuesday + 86_400_000L

private val Week = listOf(
    MonarchTimesheetGridDay(Monday, lockReason = "Approved", expectedMinutes = 480),
    MonarchTimesheetGridDay(Tuesday, today = true, expectedMinutes = 480),
)

private val Atlas = MonarchTimesheetGridRow(
    id = "atlas",
    label = "Atlas",
    description = "Billable",
    cells = mapOf(Monday to 240, Tuesday to 90),
)
private val Timer = MonarchTimesheetGridRow("timer", "Timer", cells = mapOf(Tuesday to 75), readOnly = true)

@Composable
private fun Grid(
    days: List<MonarchTimesheetGridDay> = Week,
    rows: List<MonarchTimesheetGridRow> = listOf(Atlas),
    layout: MonarchTimesheetGridLayout = MonarchTimesheetGridLayout.Grid,
    readOnly: Boolean = false,
    maximum: Int = 1440,
    summaryRows: List<MonarchTimesheetGridSummaryRow> = emptyList(),
    labels: MonarchTimesheetGridLabels = MonarchTimesheetGridLabels(),
    rowActions: ((MonarchTimesheetGridRow) -> List<MonarchDataTableRowAction>)? = null,
    onDayActivate: ((Long) -> Unit)? = null,
    onCellActivate: ((String, Long) -> Unit)? = null,
    onCellChange: (suspend (String, Long, Int) -> Unit)? = { _, _, _ -> },
) {
    MonarchTimesheetGrid(
        days = days,
        rows = rows,
        summaryRows = summaryRows,
        maximumMinutesPerDay = maximum,
        layout = layout,
        readOnly = readOnly,
        labels = labels,
        rowActions = rowActions,
        onDayActivate = onDayActivate,
        onCellActivate = onCellActivate,
        onCellChange = onCellChange,
    )
}

internal object MonarchTimesheetGridFixtures : ComponentFixtures {
    override val id = "timesheet-grid"
    override val fixtures = listOf(
        fixture("day, editable, today") { Grid(layout = MonarchTimesheetGridLayout.Day) },
        fixture("day, locked day, row actions") {
            Grid(
                days = Week.take(1),
                layout = MonarchTimesheetGridLayout.Day,
                rowActions = { listOf(MonarchDataTableRowAction("Clear row", danger = true) {}) },
            )
        },
        fixture("day, read only, open entries, summary") {
            Grid(
                rows = listOf(Timer),
                layout = MonarchTimesheetGridLayout.Day,
                onCellActivate = { _, _ -> },
                onDayActivate = {},
                summaryRows = listOf(MonarchTimesheetGridSummaryRow("in", "Attendance", mapOf(Tuesday to "7h"))),
            )
        },
        fixture("grid, editable, one day") { Grid(days = Week.takeLast(1)) },
        fixture("grid, read only, over limit, open entries") {
            Grid(days = Week.take(1), readOnly = true, maximum = 120, onCellActivate = { _, _ -> }, onDayActivate = {})
        },
        fixture("grid, read only row, non-working day with a note") {
            Grid(
                days = listOf(MonarchTimesheetGridDay(Wednesday, nonWorking = true, note = "Holiday")),
                rows = listOf(Timer),
                onCellChange = null,
            )
        },
        fixture("no rows") { Grid(rows = emptyList(), layout = MonarchTimesheetGridLayout.Day) },
        fixture("no rows, grid") { Grid(days = Week.take(1), rows = emptyList()) },
        fixture("no days") { Grid(days = emptyList(), layout = MonarchTimesheetGridLayout.Day) },
        fixture("long names") {
            Grid(
                days = Week.takeLast(1),
                rows = listOf(
                    MonarchTimesheetGridRow(
                        "long",
                        "Customer onboarding programme for the northern region",
                        description = "Implementation · Billable at the standard rate",
                    ),
                ),
                layout = MonarchTimesheetGridLayout.Day,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchTimesheetGridTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
    private val atlasTuesday get() = rule.onNodeWithContentDescription("Atlas, Tue, Jan 13")

    @Test
    fun cellsAreKeyedByDayAndNegativeMinutesCountAsZero() {
        val row = MonarchTimesheetGridRow(
            "a",
            "A",
            cells = mapOf(Monday + 3_600_000L to 30, Monday + 7_200_000L to 15, Tuesday to -40),
        )
        assertEquals(45, row.minutes(Monday))
        assertEquals(45, row.minutes(Monday + 86_399_999L))
        assertEquals(0, row.minutes(Tuesday))
        assertEquals(0, row.minutes(Wednesday))
        assertEquals(Monday, TimesheetGridRules.dayKey(Monday + 1))
    }

    @Test
    fun totalsIgnoreDaysThatAreNotShown() {
        val row = MonarchTimesheetGridRow("a", "A", cells = mapOf(Monday to 60, Tuesday to 30, Wednesday to 999))
        assertEquals(90, TimesheetGridRules.rowTotal(row, Week))
        assertEquals(300, TimesheetGridRules.dayTotal(listOf(Atlas, row), Week[0]))
        assertEquals(330 + 90, TimesheetGridRules.grandTotal(listOf(Atlas, row), Week))
        assertTrue(TimesheetGridRules.over(1441, 1440))
        assertFalse(TimesheetGridRules.over(1440, 1440))
    }

    @Test
    fun aCommitReadsTheTextAgainstTheSavedValueAndTheMaximum() {
        assertEquals(TimesheetGridCommit.Save(90), TimesheetGridRules.commit("1:30", 0, 1440))
        assertEquals(TimesheetGridCommit.Save(90), TimesheetGridRules.commit("1.5h", 0, 1440))
        assertEquals(TimesheetGridCommit.Save(0), TimesheetGridRules.commit("  ", 60, 1440))
        assertEquals(TimesheetGridCommit.Unchanged, TimesheetGridRules.commit("1h", 60, 1440))
        assertEquals(TimesheetGridCommit.Invalid, TimesheetGridRules.commit("soon", 0, 1440))
        assertEquals(TimesheetGridCommit.TooLong, TimesheetGridRules.commit("25h", 0, 1440))
    }

    @Test
    fun aReasonLocksTheDayAndTheFirstPageIsToday() {
        assertTrue(TimesheetGridRules.locked(MonarchTimesheetGridDay(Monday, lockReason = "Approved")))
        assertFalse(TimesheetGridRules.cellEditable(true, Atlas, Week[0]))
        assertTrue(TimesheetGridRules.cellEditable(true, Atlas, Week[1]))
        assertFalse(TimesheetGridRules.cellEditable(true, Timer, Week[1]))
        assertFalse(TimesheetGridRules.editable(readOnly = true, canSave = true))
        assertEquals(1, TimesheetGridRules.initialDayIndex(Week))
        assertEquals(0, TimesheetGridRules.initialDayIndex(Week.take(1)))
    }

    @Test
    fun theAutomaticLayoutShowsTheGridOnlyWhenEveryColumnFits() {
        val tokens = TimesheetGridLayoutRules
        assertFalse(tokens.fitsGrid(320.dp, 7, 1f))
        assertTrue(tokens.fitsGrid(1000.dp, 7, 1f))
        assertFalse(tokens.fitsGrid(1000.dp, 7, 2f))
        assertEquals(MonarchTimesheetGridLayout.Day, tokens.resolve(MonarchTimesheetGridLayout.Automatic, false))
        assertEquals(MonarchTimesheetGridLayout.Grid, tokens.resolve(MonarchTimesheetGridLayout.Automatic, true))
        assertEquals(MonarchTimesheetGridLayout.Grid, tokens.resolve(MonarchTimesheetGridLayout.Grid, false))
        rule.setMonarchContent { Grid(layout = MonarchTimesheetGridLayout.Automatic) }
        rule.onNodeWithContentDescription("Days").assertExists()
    }

    @Test
    fun aTypedDurationIsSavedOnTheNextKey() {
        val saved = mutableListOf<Triple<String, Long, Int>>()
        rule.setMonarchContent { Grid(onCellChange = { row, day, minutes -> saved += Triple(row, day, minutes) }) }
        atlasTuesday.performTextReplacement("2:15")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf(Triple("atlas", Tuesday, 135)), saved)
        // The app did not pass the new value, so the typed text stays.
        rule.onNodeWithText("2:15").assertExists()
    }

    @Test
    fun emptyTextClearsAndTheSameValueIsNotSaved() {
        val saved = mutableListOf<Int>()
        rule.setMonarchContent { Grid(onCellChange = { _, _, minutes -> saved += minutes }) }
        atlasTuesday.performTextReplacement("90m")
        atlasTuesday.performImeAction()
        atlasTuesday.performTextReplacement("")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf(0), saved)
    }

    @Test
    fun textThatIsNotADurationIsKeptMarkedAndAnnounced() {
        var calls = 0
        rule.setMonarchContent { Grid(onCellChange = { _, _, _ -> calls++ }) }
        atlasTuesday.performTextReplacement("soon")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        assertEquals(0, calls)
        atlasTuesday.assert(hasError)
        rule.onNodeWithText("Use a time such as 1:30, 1.5 or 90m.")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithText("soon").assertExists()
    }

    @Test
    fun aValueAboveTheMaximumIsRefused() {
        var calls = 0
        rule.setMonarchContent { Grid(maximum = 480, onCellChange = { _, _, _ -> calls++ }) }
        atlasTuesday.performTextReplacement("9h")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        assertEquals(0, calls)
        rule.onNodeWithText("A day has at most 8h.").assertExists()
    }

    @Test
    fun aCellIsDisabledAndReadAsSavingUntilTheSaveReturns() {
        val pending = CompletableDeferred<Unit>()
        rule.setMonarchContent { Grid(onCellChange = { _, _, _ -> pending.await() }) }
        atlasTuesday.performTextReplacement("3h")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        atlasTuesday.assertIsNotEnabled()
        assertEquals("Saving", atlasTuesday.stateDescription())
        pending.complete(Unit)
        rule.waitForIdle()
        assertEquals(null, atlasTuesday.stateDescription())
    }

    @Test
    fun aFailedSaveShowsItsMessage() {
        rule.setMonarchContent {
            Grid(onCellChange = { _, _, _ -> error("Payroll is closed for this week.") })
        }
        atlasTuesday.performTextReplacement("3h")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        rule.onNodeWithText("Payroll is closed for this week.").assertExists()
        atlasTuesday.assert(hasError)
        rule.onNodeWithText("3h").assertExists()
    }

    @Test
    fun aSavedValueShowsOnceTheAppPassesIt() {
        rule.setMonarchContent {
            var cells by remember { mutableStateOf(Atlas.cells) }
            Grid(
                rows = listOf(MonarchTimesheetGridRow("atlas", "Atlas", cells = cells)),
                onCellChange = { _, day, minutes -> cells = cells + (day to minutes) },
            )
        }
        atlasTuesday.performTextReplacement("120m")
        atlasTuesday.performImeAction()
        rule.waitForIdle()
        rule.onNodeWithText("2h").assertExists()
        rule.onNodeWithContentDescription("Atlas, Total").assert(hasState("6 hours"))
    }

    @Test
    fun readOnlyAndLockedCellsAreTextReadWithTheirTimeAndReason() {
        rule.setMonarchContent { Grid(readOnly = true) }
        assertTrue(rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        atlasTuesday.assertHasNoClickAction()
        assertEquals("1 hour, 30 minutes", atlasTuesday.stateDescription())
        assertEquals("4 hours, Approved", rule.onNodeWithContentDescription("Atlas, Mon, Jan 12").stateDescription())
    }

    @Test
    fun cellsWithTimeOpenTheirEntries() {
        val opened = mutableListOf<Pair<String, Long>>()
        rule.setMonarchContent { Grid(readOnly = true, onCellActivate = { row, day -> opened += row to day }) }
        atlasTuesday.assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("atlas" to Tuesday), opened)
    }

    @Test
    fun dayHeadingsAreHeadingsReadInFullAndCanBeButtons() {
        val opened = mutableListOf<Long>()
        rule.setMonarchContent { Grid(onDayActivate = { opened += it }) }
        rule.onNodeWithContentDescription("Mon, Jan 12, Approved").assert(isHeading)
        rule.onNodeWithContentDescription("Tue, Jan 13, Today")
            .assert(isHeading)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf(Tuesday), opened)
    }

    @Test
    fun dayTotalsReadTheExpectedTimeAndTheLimit() {
        rule.setMonarchContent { Grid(maximum = 120) }
        assertEquals(
            "4 hours, of 8 hours, Over limit",
            rule.onNodeWithContentDescription("Day total, Mon, Jan 12").stateDescription(),
        )
        assertEquals(
            "1 hour, 30 minutes, of 8 hours",
            rule.onNodeWithContentDescription("Day total, Tue, Jan 13").stateDescription(),
        )
        rule.onNodeWithText("Over limit", useUnmergedTree = true).assertExists()
        assertEquals("5 hours, 30 minutes", rule.onNodeWithContentDescription("Day total, Total").stateDescription())
    }

    @Test
    fun theDayLayoutOpensOnTodayAndSavesFromADurationField() {
        val saved = mutableListOf<Triple<String, Long, Int>>()
        rule.setMonarchContent {
            Grid(
                layout = MonarchTimesheetGridLayout.Day,
                onCellChange = { row, day, minutes -> saved += Triple(row, day, minutes) },
            )
        }
        tab("Tue, Jan 13, Today").assertIsSelected()
        val field = rule.onNode(hasSetTextAction())
        field.performTextReplacement("2h")
        field.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf(Triple("atlas", Tuesday, 120)), saved)
    }

    @Test
    fun aLockedDayPageShowsValuesWithItsReason() {
        rule.setMonarchContent { Grid(layout = MonarchTimesheetGridLayout.Day) }
        tab("Mon, Jan 12, Approved").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertTrue(rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        assertEquals("4 hours, Approved", rule.onNodeWithContentDescription("Atlas, Mon, Jan 12").stateDescription())
    }

    @Test
    fun rowActionsOpenAMenu() {
        var cleared = 0
        rule.setMonarchContent {
            Grid(rowActions = { listOf(MonarchDataTableRowAction("Clear row") { cleared++ }) })
        }
        rule.onNodeWithContentDescription("Actions for Atlas")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Clear row").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun summaryRowsAndTheEmptyMessage() {
        rule.setMonarchContent {
            Grid(
                rows = emptyList(),
                summaryRows = listOf(MonarchTimesheetGridSummaryRow("in", "Attendance", mapOf(Monday to "7h 55m"))),
            )
        }
        rule.onNodeWithText("No time logged for this period.").assertExists()
        assertEquals("7h 55m", rule.onNodeWithContentDescription("Attendance, Mon, Jan 12").stateDescription())
        assertEquals("No time", rule.onNodeWithContentDescription("Attendance, Tue, Jan 13").stateDescription())
    }

    @Test
    fun builtInStringsAndFormatsCanBeReplaced() {
        rule.setMonarchContent {
            Grid(
                readOnly = true,
                labels = MonarchTimesheetGridLabels(
                    rowHeader = "Proyecto",
                    today = "Hoy",
                    cellLabel = { row, day -> "$row el $day" },
                    longDay = { if (it == Monday) "lunes" else "martes" },
                    spokenDuration = { "$it minutos" },
                    duration = { "$it min" },
                ),
            )
        }
        rule.onNodeWithText("Proyecto").assert(isHeading)
        rule.onNodeWithText("90 min", useUnmergedTree = true).assertExists()
        assertEquals("90 minutos", rule.onNodeWithContentDescription("Atlas el martes").stateDescription())
        rule.onNodeWithContentDescription("martes, Hoy").assertExists()
    }

    @Test
    fun rowNamesComeBeforeTheDaysRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Grid(readOnly = true) }
        rule.onNodeWithText("Atlas", useUnmergedTree = true)
            .assertIsBefore(atlasTuesday, LayoutDirection.Rtl)
        rule.onNodeWithContentDescription("Atlas, Mon, Jan 12")
            .assertIsBefore(atlasTuesday, LayoutDirection.Rtl)
    }

    @Test
    fun longRowNamesWrapAtTheLargestFontScale() {
        val name = "Customer onboarding programme for the northern region"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Grid(
                days = Week.takeLast(1),
                rows = listOf(MonarchTimesheetGridRow("long", name, readOnly = true)),
                layout = MonarchTimesheetGridLayout.Day,
            )
        }
        rule.onNodeWithText(name, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromRoles() {
        val tokens = MonarchTimesheetGridTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrong, tokens.dayColor(colors, today = true))
            assertEquals(colors.textDefault, tokens.dayColor(colors, today = false))
            assertEquals(colors.accentSolid, tokens.todayBar(colors))
            assertEquals(colors.bgSubtle, tokens.columnContainer(colors, locked = true, nonWorking = false))
            assertEquals(colors.bgSubtle, tokens.columnContainer(colors, locked = false, nonWorking = true))
            assertEquals(
                androidx.compose.ui.graphics.Color.Transparent,
                tokens.columnContainer(colors, locked = false, nonWorking = false),
            )
            assertEquals(colors.textMuted, tokens.valueColor(colors, locked = true))
            assertEquals(colors.textDefault, tokens.valueColor(colors, locked = false))
            assertEquals(colors.dangerStrong, tokens.totalColor(colors, over = true))
            assertEquals(colors.textDefault, tokens.totalColor(colors, over = false))
            assertEquals(colors.bgSubtle, tokens.headerContainer(colors))
            assertEquals(colors.textMuted, tokens.muted(colors))
            assertEquals(colors.dangerStrong, tokens.error(colors))
        }
    }

    private fun tab(name: String) =
        rule.onNode(hasContentDescription(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))

    private fun hasState(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)
}

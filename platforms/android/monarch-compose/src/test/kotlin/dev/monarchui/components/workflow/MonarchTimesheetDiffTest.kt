package dev.monarchui.components.workflow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiff
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffChange
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffEntry
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffFields
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffLabels
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffResult
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffTokens
import dev.monarchui.components.workflow.timesheetdiff.MonarchTimesheetDiffTotals
import dev.monarchui.components.workflow.timesheetdiff.TimesheetDiffKind
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Monday 12 January 2026, 00:00 UTC. */
private const val Monday = 1_768_176_000_000L
private const val Day = 86_400_000L

private val Before = listOf(
    MonarchTimesheetDiffEntry("1", Monday, 120, project = "Pulse", activity = "Design"),
    MonarchTimesheetDiffEntry("2", Monday, 45, project = "Pulse", billable = false),
)
private val After = listOf(
    MonarchTimesheetDiffEntry("1", Monday, 150, project = "Atlas", activity = "Design"),
    MonarchTimesheetDiffEntry("3", Monday + Day, 30, task = "ENG-142"),
)

internal object MonarchTimesheetDiffFixtures : ComponentFixtures {
    override val id = "timesheet-diff"
    override val fixtures = listOf(
        fixture("added and removed") {
            MonarchTimesheetDiff(
                diff = MonarchTimesheetDiffResult(added = After.takeLast(1), removed = Before.takeLast(1)),
                subtitle = "since version 3",
            )
        },
        fixture("changed, no billable totals") {
            MonarchTimesheetDiff(before = Before.take(1), after = After.take(1), showBillable = false)
        },
        fixture("empty without totals") {
            MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(), showTotals = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTimesheetDiffTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun entriesAreMatchedByIdAndComparedFieldByField() {
        val diff = MonarchTimesheetDiffResult.between(Before, After)
        assertEquals(listOf("3"), diff.added.map { it.id })
        assertEquals(listOf("2"), diff.removed.map { it.id })
        assertEquals(listOf("1"), diff.changed.map { it.id })
        assertEquals(
            listOf(MonarchTimesheetDiffFields.MINUTES, MonarchTimesheetDiffFields.PROJECT),
            diff.changed.single().fields,
        )
    }

    @Test
    fun aMissingValueAndAnEmptyOneAreDifferentButTheSameDayIsNot() {
        val a = MonarchTimesheetDiffEntry("1", Monday, 60, note = null)
        val b = MonarchTimesheetDiffEntry("1", Monday + 3_600_000L, 60, note = "")
        val diff = MonarchTimesheetDiffResult.between(listOf(a), listOf(b))
        assertEquals(listOf(MonarchTimesheetDiffFields.NOTE), diff.changed.single().fields)
        assertTrue(MonarchTimesheetDiffResult.between(listOf(a), listOf(a)).isEmpty)
    }

    @Test
    fun totalsCountAnUnknownBillableAsBillable() {
        val diff = MonarchTimesheetDiffResult.between(Before, After)
        // +30 changed, −45 removed (non-billable), +30 added.
        assertEquals(MonarchTimesheetDiffTotals(total = 15, billable = 60), diff.totals)
    }

    @Test
    fun durationsAreCompactForTheEyeAndInWordsForTalkBack() {
        val tokens = MonarchTimesheetDiffTokens
        assertEquals("1h 30m", tokens.compact(90))
        assertEquals("2h", tokens.compact(120))
        assertEquals("45m", tokens.compact(45))
        assertEquals("+1h 30m", tokens.signed(90))
        assertEquals("−45m", tokens.signed(-45))
        assertEquals("0m", tokens.signed(0))
        assertEquals("1 hour, 30 minutes", tokens.spoken(90, Locale.US))
        assertEquals("45 minutes", tokens.spoken(-45, Locale.US))
        assertEquals("0 minutes", tokens.spoken(0, Locale.US))
    }

    @Test
    fun eachLineIsOneElementReadInWords() {
        rule.setMonarchContent { MonarchTimesheetDiff(before = Before, after = After) }
        rule.onNodeWithContentDescription(
            "Removed, minus 45 minutes, Mon, Jan 12, 45 minutes, Pulse, non-billable",
            substring = false,
        ).assertHasNoClickAction()
        rule.onNodeWithContentDescription(
            "Added, plus 30 minutes, Tue, Jan 13, 30 minutes, No project, ENG-142",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "Changed time, project, plus 30 minutes, " +
                "Before: Mon, Jan 12, 2 hours, Pulse, Design, After: Mon, Jan 12, 2 hours, 30 minutes, Atlas, Design",
        ).assertExists()
    }

    @Test
    fun theTitleIsAHeadingAndTheTotalsAreReadInWords() {
        rule.setMonarchContent { MonarchTimesheetDiff(before = Before, after = After) }
        rule.onNodeWithText("What changed").assert(isHeading)
        rule.onNodeWithText("Total +15m · billable +1h").assertExists()
        rule.onNodeWithContentDescription("Total plus 15 minutes · billable plus 1 hour").assertExists()
    }

    @Test
    fun emptyShowsNoChangesAndBillableCanBeLeftOut() {
        rule.setMonarchContent { MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(), showBillable = false) }
        rule.onNodeWithText("No changes.").assertExists()
        rule.onNodeWithText("Total 0m").assertExists()
    }

    @Test
    fun aChangeWithoutATimeChangeShowsNoDelta() {
        val before = MonarchTimesheetDiffEntry("1", Monday, 60, project = "A")
        val after = MonarchTimesheetDiffEntry("1", Monday, 60, project = "B")
        rule.setMonarchContent { MonarchTimesheetDiff(before = listOf(before), after = listOf(after)) }
        rule.onNodeWithContentDescription(
            "Changed project, Before: Mon, Jan 12, 1 hour, A, After: Mon, Jan 12, 1 hour, B",
        ).assertExists()
    }

    @Test
    fun builtInStringsAndFormatsCanBeReplaced() {
        val change = MonarchTimesheetDiffChange(
            id = "1",
            fields = listOf("minutes", "rate"),
            before = Before.first(),
            after = After.first(),
        )
        rule.setMonarchContent {
            MonarchTimesheetDiff(
                diff = MonarchTimesheetDiffResult(changed = listOf(change)),
                title = "Cambios",
                labels = MonarchTimesheetDiffLabels(
                    changed = { "Cambiado: ${it.joinToString(" y ")}" },
                    fields = mapOf("minutes" to "tiempo"),
                    before = "Antes",
                    after = "Después",
                    delta = { "$it min" },
                    totals = { total, billable -> "Total $total ($billable)" },
                    entry = { "${it.project} ${it.minutes}" },
                ),
            )
        }
        rule.onNodeWithText("Cambios").assertExists()
        rule.onNodeWithContentDescription("Cambiado: tiempo y rate, 30 min, Antes: Pulse 120, Después: Atlas 150")
            .assertExists()
        rule.onNodeWithText("Total 30 min (30 min)").assertExists()
    }

    @Test
    fun theKindComesBeforeTheDeltaRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(added = After.takeLast(1)), showTotals = false)
        }
        rule.onNodeWithText("Added", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("+30m", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun linesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(removed = Before.takeLast(1)), showTotals = false)
        }
        rule.onNodeWithText("Mon, Jan 12 · 45m · Pulse · non-billable", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheToneOfEachKind() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val added = MonarchTimesheetDiffTokens.colors(colors, TimesheetDiffKind.Added)
            val removed = MonarchTimesheetDiffTokens.colors(colors, TimesheetDiffKind.Removed)
            val changed = MonarchTimesheetDiffTokens.colors(colors, TimesheetDiffKind.Changed)
            assertEquals(colors.successSubtle, added.container)
            assertEquals(colors.successSolid, added.bar)
            assertEquals(colors.successStrong, added.delta)
            assertEquals(colors.dangerSubtle, removed.container)
            assertEquals(colors.warningSubtle, changed.container)
            assertEquals(colors.textMuted, removed.struck)
            assertEquals(colors.textDefault, changed.text)
        }
    }
}

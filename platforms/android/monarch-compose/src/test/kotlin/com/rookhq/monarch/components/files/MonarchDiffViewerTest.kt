package com.rookhq.monarch.components.files

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.files.diffviewer.DiffViewerRow
import com.rookhq.monarch.components.files.diffviewer.DiffViewerRowTexts
import com.rookhq.monarch.components.files.diffviewer.DiffViewerSegment
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewer
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerGranularity
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerLabels
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerLine
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerLineKind as Kind
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerMode
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerTokens
import com.rookhq.monarch.components.files.diffviewer.diffViewerDefaultMode
import com.rookhq.monarch.components.files.diffviewer.diffViewerMarker
import com.rookhq.monarch.components.files.diffviewer.diffViewerNext
import com.rookhq.monarch.components.files.diffviewer.diffViewerPrevious
import com.rookhq.monarch.components.files.diffviewer.diffViewerRows
import com.rookhq.monarch.components.files.diffviewer.diffViewerSequences
import com.rookhq.monarch.components.files.diffviewer.diffViewerSpokenPair
import com.rookhq.monarch.components.files.diffviewer.diffViewerStats
import com.rookhq.monarch.components.files.diffviewer.diffViewerWords
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Before = "Ship on Friday.\nTell support.\nKeep the old banner."
private const val After = "Ship on Monday.\nTell support.\nUpdate the status page."
private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
private val longBefore = (1..12).map { "Line $it" }
private val longAfter = longBefore.mapIndexed { i, line -> if (i == 0 || i == 11) "$line, edited" else line }

internal object MonarchDiffViewerFixtures : ComponentFixtures {
    override val id = "diff-viewer"
    override val fixtures = listOf(
        fixture("unified with changed words") { MonarchDiffViewer(before = Before, after = After) },
        fixture("no changes") { MonarchDiffViewer(before = "Same text", after = "Same text") },
        fixture("folded unchanged run") { MonarchDiffViewer(before = longBefore, after = longAfter, context = 1) },
        fixture("whole lines, no numbers") {
            MonarchDiffViewer(
                before = Before,
                after = After,
                granularity = MonarchDiffViewerGranularity.Line,
                showLineNumbers = false,
            )
        },
        fixture("side by side, named sides") {
            MonarchDiffViewer(
                before = Before,
                after = After,
                mode = MonarchDiffViewerMode.Split,
                beforeLabel = "Version 3",
                afterLabel = "Version 4",
            )
        },
        fixture("everything added") { MonarchDiffViewer(before = "", after = "A new first line\n\nAnd a third") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDiffViewerTest {
    @get:Rule
    val rule = createComposeRule()

    private val texts = DiffViewerRowTexts("Before", "After", "Added", "Removed", "Blank line")

    @Test
    fun theLineDiffIsNumberedOnBothSidesWithRemovalsFirst() {
        val lines = MonarchDiffViewerLine.diff(listOf("a", "b", "c"), listOf("a", "x", "c"))
        assertEquals(
            listOf(
                MonarchDiffViewerLine(Kind.Same, "a", 1, 1),
                MonarchDiffViewerLine(Kind.Removed, "b", oldNumber = 2),
                MonarchDiffViewerLine(Kind.Added, "x", newNumber = 2),
                MonarchDiffViewerLine(Kind.Same, "c", 3, 3),
            ),
            lines,
        )
    }

    @Test
    fun textsSplitOnEveryLineBreak() {
        assertEquals(listOf("a", "b", "c", ""), MonarchDiffViewerLine.lines("a\r\nb\rc\n"))
        assertEquals(emptyList(), MonarchDiffViewerLine.lines(""))
    }

    @Test
    fun tooManyPairsFallBackToRemoveAllThenAddAll() {
        val ops = diffViewerSequences(listOf("a", "b"), listOf("a", "c"), maxPairs = 3)
        assertEquals(listOf(Kind.Removed, Kind.Removed, Kind.Added, Kind.Added), ops.map { it.kind })
    }

    @Test
    fun theWordDiffKeepsTheUnchangedWords() {
        assertEquals(
            listOf(
                DiffViewerSegment(Kind.Same, "Ship on "),
                DiffViewerSegment(Kind.Removed, "Friday"),
                DiffViewerSegment(Kind.Added, "Monday"),
                DiffViewerSegment(Kind.Same, "."),
            ),
            diffViewerWords("Ship on Friday.", "Ship on Monday."),
        )
    }

    @Test
    fun longUnchangedRunsFoldAroundTheChanges() {
        val lines = MonarchDiffViewerLine.diff(longBefore, longAfter)
        val folded = diffViewerRows(lines, split = false, context = 1, expanded = emptySet())
        assertEquals(2, folded.hunks)
        val gap = folded.rows.filterIsInstance<DiffViewerRow.Gap>().single()
        assertEquals(8, gap.count)
        val open = diffViewerRows(lines, split = false, context = 1, expanded = setOf(gap.id))
        assertTrue(open.rows.none { it is DiffViewerRow.Gap })
        val never = diffViewerRows(lines, split = false, context = null, expanded = emptySet())
        assertTrue(never.rows.none { it is DiffViewerRow.Gap })
        // Ten unchanged lines with a context of 5 on each side: one line more than shown is not folded.
        val almost = diffViewerRows(lines, split = false, context = 5, expanded = emptySet())
        assertTrue(almost.rows.none { it is DiffViewerRow.Gap })
    }

    @Test
    fun sideBySidePairsTheKthRemovedLineWithTheKthAddedLine() {
        val lines = MonarchDiffViewerLine.diff(listOf("a", "b"), listOf("x"))
        val rows = diffViewerRows(lines, split = true, context = 3, expanded = emptySet()).rows
        assertEquals(listOf(DiffViewerRow.Pair(0, 2, 0), DiffViewerRow.Pair(1, null, null)), rows)
    }

    @Test
    fun aSideBySideRowReadsEachSideOrTheUnchangedLineOnce() {
        val same = MonarchDiffViewerLine(Kind.Same, "Tell support.", 2, 2)
        assertEquals("Tell support.", diffViewerSpokenPair(same, same, texts))
        val old = MonarchDiffViewerLine(Kind.Removed, "", oldNumber = 1)
        val new = MonarchDiffViewerLine(Kind.Added, "Hi", newNumber = 1)
        assertEquals("Before: Removed: Blank line. After: Added: Hi", diffViewerSpokenPair(old, new, texts))
    }

    @Test
    fun eachLineIsReadWithItsKindAndTheTotalsAreRead() {
        rule.setMonarchContent { MonarchDiffViewer(before = Before, after = After) }
        rule.onNodeWithContentDescription("Removed: Ship on Friday.").assertExists()
        rule.onNodeWithContentDescription("Added: Ship on Monday.").assertExists()
        rule.onNodeWithContentDescription("Tell support.").assertExists()
        rule.onNodeWithContentDescription("2 added, 2 removed").assertExists()
        rule.onNodeWithContentDescription("Changes from Before to After").assertExists()
        val stats = diffViewerStats(MonarchDiffViewerLine.diff(Before.lines(), After.lines()))
        assertEquals(2, stats.added)
        assertEquals(2, stats.removed)
    }

    @Test
    fun nextAndPreviousMoveBetweenChangesAndAnnounceThePosition() {
        rule.setMonarchContent { MonarchDiffViewer(before = Before, after = After) }
        rule.onNodeWithText("Next change").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(polite and hasContentDescription("Change 1 of 2")).assertExists()
        rule.onNodeWithText("Next change").performClick()
        rule.onNode(polite and hasContentDescription("Change 2 of 2")).assertExists()
        rule.onNodeWithText("Next change").performClick()
        rule.onNode(polite and hasContentDescription("Change 1 of 2")).assertExists()
        rule.onNodeWithText("Previous change").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(polite and hasContentDescription("Change 2 of 2")).assertExists()
    }

    @Test
    fun oneChangeHasNoPreviousOrNext() {
        rule.setMonarchContent { MonarchDiffViewer(before = "a", after = "b") }
        rule.onNodeWithText("Next change").assertDoesNotExist()
        rule.onNodeWithText("Previous change").assertDoesNotExist()
    }

    @Test
    fun aFoldedRunOpensWhenItsButtonIsPressed() {
        rule.setMonarchContent { MonarchDiffViewer(before = longBefore, after = longAfter, context = 1) }
        rule.onNodeWithContentDescription("Line 5").assertDoesNotExist()
        rule.onNodeWithText("Show 8 unchanged lines").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Show 8 unchanged lines").assertDoesNotExist()
        rule.onNodeWithContentDescription("Line 5").assertExists()
    }

    @Test
    fun aChangeOfTheTextsStartsOver() {
        var after by mutableStateOf(longAfter)
        rule.setMonarchContent { MonarchDiffViewer(before = longBefore, after = after, context = 1) }
        rule.onNodeWithText("Show 8 unchanged lines").performClick()
        rule.onNodeWithText("Next change").performClick()
        after = longAfter.map { "$it." }
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Change", substring = true)).assertDoesNotExist()
    }

    @Test
    fun sameTextsSayNoChanges() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchDiffViewer(before = "Same", after = "Same") }
        rule.onNodeWithText("No changes.").assertTextFullyVisible()
        rule.onNodeWithContentDescription("0 added, 0 removed").assertExists()
    }

    @Test
    fun sideBySideReadsTheNamedSides() {
        rule.setMonarchContent {
            MonarchDiffViewer(
                before = Before,
                after = After,
                mode = MonarchDiffViewerMode.Split,
                beforeLabel = "Version 3",
                afterLabel = "Version 4",
            )
        }
        rule.onNodeWithContentDescription("Version 3: Removed: Ship on Friday. Version 4: Added: Ship on Monday.")
            .assertExists()
        rule.onNodeWithContentDescription("Changes from Version 3 to Version 4").assertExists()
    }

    @Test
    fun onAPhoneTheLinesStartUnifiedAndTheSwitchIsOffered() {
        rule.setMonarchContent { MonarchDiffViewer(before = Before, after = After) }
        rule.onNodeWithText("Side by side").assertExists()
        rule.onNodeWithContentDescription("Added: Ship on Monday.").assertExists()
    }

    @Test
    fun atAWideWidthTheViewerIsSideBySideAndTheSwitchChangesIt() {
        val picked = mutableListOf<MonarchDiffViewerMode>()
        rule.setMonarchContent {
            MonarchDiffViewer(
                before = Before,
                after = After,
                modifier = Modifier.requiredWidth(MonarchDiffViewerTokens.SplitMinWidth + 40.dp),
                onModeChange = { picked += it },
            )
        }
        rule.onNodeWithText("Side by side").assertIsSelected()
        rule.onNodeWithContentDescription("Layout").assertExists()
        rule.onNodeWithText("Unified").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(MonarchDiffViewerMode.Unified), picked)
        rule.onNodeWithText("Unified").assertIsSelected()
        rule.onNodeWithContentDescription("Added: Ship on Monday.").assertExists()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchDiffViewer(
                before = longBefore,
                after = longAfter,
                context = 1,
                labels = MonarchDiffViewerLabels(
                    caption = { a, b -> "$a → $b" },
                    stats = { a, r -> "plus $a minus $r" },
                    added = "New",
                    expand = { "Reveal $it" },
                    next = "Forward",
                    position = { i, n -> "$i/$n" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Before → After").assertExists()
        rule.onNodeWithContentDescription("plus 2 minus 2").assertExists()
        rule.onNodeWithContentDescription("New: Line 1, edited").assertExists()
        rule.onNodeWithText("Reveal 8").assertExists()
        rule.onNodeWithText("Forward").performClick()
        rule.onNode(polite and hasContentDescription("1/2")).assertExists()
    }

    @Test
    fun theTotalsComeBeforeTheButtonsRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDiffViewer(before = Before, after = After)
        }
        rule.onNodeWithContentDescription("2 added, 2 removed")
            .assertIsBefore(rule.onNodeWithText("Previous change"), LayoutDirection.Rtl)
    }

    @Test
    fun theNavigationRulesWrapRound() {
        assertEquals(0, diffViewerNext(-1, 3))
        assertEquals(0, diffViewerNext(2, 3))
        assertEquals(2, diffViewerPrevious(-1, 3))
        assertEquals(2, diffViewerPrevious(0, 3))
        assertEquals(1, diffViewerPrevious(2, 3))
        assertEquals(-1, diffViewerNext(0, 0))
        assertEquals(MonarchDiffViewerMode.Unified, diffViewerDefaultMode(320.dp))
        val wide = MonarchDiffViewerTokens.SplitMinWidth
        assertEquals(MonarchDiffViewerMode.Split, diffViewerDefaultMode(wide))
    }

    @Test
    fun theColoursComeFromTheSuccessAndDangerRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchDiffViewerTokens
            assertEquals(colors.successSubtle, t.lineFill(Kind.Added, colors), theme.id)
            assertEquals(colors.dangerSubtle, t.lineFill(Kind.Removed, colors), theme.id)
            assertEquals(Color.Transparent, t.lineFill(Kind.Same, colors), theme.id)
            assertEquals(colors.bgSubtle, t.lineFill(null, colors), theme.id)
            assertEquals(colors.successBorder, t.wordFill(Kind.Added, colors), theme.id)
            assertEquals(colors.dangerBorder, t.wordFill(Kind.Removed, colors), theme.id)
            assertEquals(colors.successStrong, t.markerColor(Kind.Added, colors), theme.id)
            assertEquals(colors.dangerStrong, t.markerColor(Kind.Removed, colors), theme.id)
            assertEquals(colors.textSubtle, t.numberColor(colors), theme.id)
            assertEquals(colors.accentSolid, t.currentBar(colors), theme.id)
            assertEquals(colors.borderSubtle, t.border(colors), theme.id)
            assertEquals(colors.textMuted, t.emptyText(colors), theme.id)
        }
        assertEquals("+", diffViewerMarker(Kind.Added))
        assertEquals("−", diffViewerMarker(Kind.Removed))
    }
}

package dev.monarchui.components.datagrid

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeDialog
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeDialogLabels
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeDialogTokens
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeField
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeRecord
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeSelection
import dev.monarchui.components.feedback.consequencelist.MonarchConsequence
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceKind
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
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val acme = MonarchMergeRecord(
    id = "a",
    title = "Acme Ltd",
    description = "Created 3 Mar 2025",
    values = mapOf("email" to "hello@acme.com", "phone" to " ", "city" to "London"),
)
private val acmeLimited = MonarchMergeRecord(
    id = "b",
    title = "ACME Limited",
    values = mapOf("email" to "info@acme.com", "phone" to "+44 20 7946 0000", "city" to "London "),
)
private val mergeFields = listOf(
    MonarchMergeField("email", "Email"),
    MonarchMergeField("phone", "Phone"),
    MonarchMergeField("city", "City"),
)

internal object MonarchMergeDialogFixtures : ComponentFixtures {
    override val id = "merge-dialog"
    override val fixtures = listOf(
        fixture("two records with a cancel button") {
            MonarchMergeDialog(records = listOf(acme, acmeLimited), fields = mergeFields, onMerge = {}, onCancel = {})
        },
        fixture("blocked with consequences") {
            MonarchMergeDialog(
                records = listOf(acme, acmeLimited),
                fields = mergeFields,
                onMerge = {},
                consequences = listOf(MonarchConsequence(MonarchConsequenceKind.Changed, "invoices move", count = 12)),
                blockers = listOf("ACME Limited has an open contract."),
            )
        },
        fixture("merging") {
            MonarchMergeDialog(
                records = listOf(acme, acmeLimited),
                fields = mergeFields,
                onMerge = {},
                merging = true,
                onCancel = {},
            )
        },
        fixture("failed") {
            MonarchMergeDialog(records = listOf(acme, acmeLimited), fields = mergeFields, onMerge = {}, failed = true)
        },
        fixture("one record") { MonarchMergeDialog(records = listOf(acme), fields = mergeFields, onMerge = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMergeDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private var merged: MonarchMergeSelection? = null

    private fun show(
        merging: Boolean = false,
        failed: Boolean = false,
        blockers: List<String> = emptyList(),
        onCancel: (() -> Unit)? = null,
        labels: MonarchMergeDialogLabels = MonarchMergeDialogLabels(),
        records: List<MonarchMergeRecord> = listOf(acme, acmeLimited),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontScale: Float = 1f,
    ) {
        rule.setMonarchContent(fontScale = fontScale, layoutDirection = layoutDirection) {
            MonarchMergeDialog(
                records = records,
                fields = mergeFields,
                onMerge = { merged = it },
                blockers = blockers,
                merging = merging,
                failed = failed,
                onCancel = onCancel,
                labels = labels,
            )
        }
    }

    @Test
    fun theDefaultsKeepTheFirstRecordAndFillItsEmptyValues() {
        show()
        rule.onNodeWithText("Merge records").assert(isHeading())
        rule.onNodeWithText("Merge 2 records").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(
            MonarchMergeSelection("a", listOf("b"), mapOf("email" to "a", "phone" to "b", "city" to "a")),
            merged,
        )
    }

    @Test
    fun keepingAnotherRecordMovesTheChoicesThatWereNotPicked() {
        show()
        rule.onNodeWithText("hello@acme.com").performScrollTo().performClick()
        rule.onNodeWithText("ACME Limited").performScrollTo().performClick()
        rule.onNodeWithText("Merge 2 records").performClick()
        assertEquals("b", merged?.primaryId)
        assertEquals(listOf("a"), merged?.mergedIds)
        assertEquals(mapOf("email" to "a", "phone" to "b", "city" to "b"), merged?.fields)
    }

    @Test
    fun differingFieldsAreChoicesAndMatchingFieldsAreListedOnce() {
        show()
        rule.onNodeWithText("Values to keep").assert(isHeading())
        rule.onNodeWithText("Email").assert(isHeading())
        rule.onNode(hasText("hello@acme.com") and hasText("from Acme Ltd")).assertIsSelected()
        rule.onNode(hasText("Empty") and hasText("from Acme Ltd")).assertExists()
        rule.onNode(hasText("+44 20 7946 0000") and hasText("from ACME Limited")).assertIsSelected()
        rule.onNodeWithText("Same in every record").assertExists()
        rule.onNodeWithText("City").assertExists()
        rule.onNodeWithText("London").assertExists()
    }

    @Test
    fun theKeptBadgeIsHiddenBecauseTheSelectionSaysIt() {
        show()
        rule.onNodeWithText("Kept").assertDoesNotExist()
        rule.onNode(hasText("Acme Ltd") and hasText("Created 3 Mar 2025")).assertIsSelected()
    }

    @Test
    fun blockersAndASingleRecordDisableTheMerge() {
        show(blockers = listOf("ACME Limited has an open contract."))
        rule.onNodeWithText("ACME Limited has an open contract.").performScrollTo().assertExists()
        rule.onNodeWithText("Merge 2 records").assertIsNotEnabled().performClick()
        assertNull(merged)
    }

    @Test
    fun aSingleRecordCannotBeMerged() {
        show(records = listOf(acme))
        rule.onNodeWithText("Merge 1 record").assertIsNotEnabled()
    }

    @Test
    fun whileMergingTheChoicesAndCancelAreDisabled() {
        rule.mainClock.autoAdvance = false
        show(merging = true, onCancel = {})
        rule.mainClock.advanceTimeByFrame()
        val merge = rule.onNodeWithText("Merge 2 records")
        assertEquals("Loading", merge.stateDescription())
        merge.performClick()
        assertNull(merged)
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        rule.onNode(hasText("Acme Ltd") and hasText("Created 3 Mar 2025")).assertIsNotEnabled()
    }

    @Test
    fun aFailureIsShownAndAnnouncedPolitely() {
        show(failed = true)
        val message = "The records could not be merged. Nothing was changed."
        rule.onNodeWithText(message).performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Merge 2 records").assertIsEnabled()
    }

    @Test
    fun cancelCallsBack() {
        var cancelled = 0
        show(onCancel = { cancelled++ })
        rule.onNodeWithText("Cancel").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        show(
            labels = MonarchMergeDialogLabels(
                title = "Merge companies",
                from = { "in $it" },
                confirm = { "Combine $it" },
                empty = "None",
            ),
        )
        rule.onNodeWithText("Merge companies").assertExists()
        rule.onNode(hasText("None") and hasText("in Acme Ltd")).assertExists()
        rule.onNodeWithText("Combine 2").assertExists()
    }

    @Test
    fun cancelComesBeforeMergeRightToLeft() {
        show(onCancel = {}, layoutDirection = LayoutDirection.Rtl)
        rule.onNodeWithText("Cancel").assertIsBefore(rule.onNodeWithText("Merge 2 records"), LayoutDirection.Rtl)
    }

    @Test
    fun theHeadingWrapsAtTheLargestFontSize() {
        show(fontScale = LargestFontScale)
        rule.onNodeWithText("Merge records").assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheMerge() {
        val t = MonarchMergeDialogTokens
        val records = listOf(acme, acmeLimited)
        val split = t.split(mergeFields, records)
        assertEquals(listOf("email", "phone"), split.differing.map { it.id })
        assertEquals(listOf("city"), split.same.map { it.id })
        assertEquals("b", t.defaultChoice("phone", records, "a"))
        assertEquals("a", t.defaultChoice("email", records, "a"))
        assertEquals("a", t.defaultChoice("missing", records, "a"))
        assertEquals("b", t.choice("email", records, "a", mapOf("email" to "b")))
        assertEquals("a", t.choice("email", records, "a", mapOf("email" to "gone")))
        assertEquals("a", t.primary(records, "gone")?.id)
        assertTrue(t.canMerge(recordCount = 2, blockerCount = 0, merging = false))
        assertFalse(t.canMerge(recordCount = 1, blockerCount = 0, merging = false))
        assertFalse(t.canMerge(recordCount = 2, blockerCount = 1, merging = false))
        assertFalse(t.canMerge(recordCount = 2, blockerCount = 0, merging = true))
        assertTrue(t.isEmpty("  "))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchMergeDialogTokens.headingColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchMergeDialogTokens.mutedColor(colors), theme.id)
        }
    }
}

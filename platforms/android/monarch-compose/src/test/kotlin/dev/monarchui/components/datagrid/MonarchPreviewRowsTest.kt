package dev.monarchui.components.datagrid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRow
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowIssue
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowStatus
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRows
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowsColumn
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowsLabels
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowsTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val contactColumns = listOf(
    MonarchPreviewRowsColumn("name", "Name"),
    MonarchPreviewRowsColumn("seats", "Seats", numeric = true),
)
private val ready = MonarchPreviewRow(1, mapOf("name" to "Ada Lovelace", "seats" to "3"))
private val failing = MonarchPreviewRow(
    2,
    mapOf("name" to "Alan", "seats" to null),
    errors = listOf(MonarchPreviewRowIssue("Required", field = "Seats")),
)
private val warned = MonarchPreviewRow(
    3,
    mapOf("name" to "Grace"),
    warnings = listOf(MonarchPreviewRowIssue("Name looks short")),
)
private val duplicate = MonarchPreviewRow(
    4,
    mapOf("name" to "Ada Lovelace", "seats" to "3"),
    status = MonarchPreviewRowStatus.Duplicate,
    note = "Matches the contact created on 2 May",
)

internal object MonarchPreviewRowsFixtures : ComponentFixtures {
    override val id = "preview-rows"
    override val fixtures = listOf(
        fixture("every outcome with tabs") {
            MonarchPreviewRows(
                rows = listOf(failing, warned, duplicate),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
            )
        },
        fixture("all ready, no tabs") {
            MonarchPreviewRows(
                rows = listOf(ready),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
            )
        },
        fixture("filtered to errors") {
            MonarchPreviewRows(
                rows = listOf(ready, failing),
                columns = contactColumns,
                label = "Preview",
                filter = MonarchPreviewRowStatus.Error,
                onFilterChange = {},
            )
        },
        fixture("filter matches nothing") {
            MonarchPreviewRows(
                rows = listOf(ready),
                columns = contactColumns,
                label = "Preview",
                filter = MonarchPreviewRowStatus.Duplicate,
                onFilterChange = {},
            )
        },
        fixture("no rows") {
            MonarchPreviewRows(
                rows = emptyList(),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
            )
        },
        fixture("long values") {
            MonarchPreviewRows(
                rows = listOf(
                    MonarchPreviewRow(
                        12,
                        mapOf("name" to "Maximilian Alexander von Hohenberg-Lichtenstein the Third"),
                        warnings = listOf(
                            MonarchPreviewRowIssue("Longer than the 40 characters a name may have", "Name"),
                        ),
                    ),
                ),
                columns = contactColumns.take(1),
                label = "Preview",
                filter = null,
                onFilterChange = {},
                filterable = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPreviewRowsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRowIsReadAsOneElementWithItsOutcomeValuesAndProblems() {
        rule.setMonarchContent {
            MonarchPreviewRows(
                rows = listOf(failing),
                columns = contactColumns,
                label = "Preview of contacts.csv",
                filter = null,
                onFilterChange = {},
            )
        }
        rule.onNode(hasContentDescription("Preview of contacts.csv")).assertExists()
        rule.onNode(
            hasText("Row 2, Error") and hasText("Alan") and hasText("No value") and
                hasText("Error: Seats: Required"),
        ).assertExists()
    }

    @Test
    fun theTabsFilterByOutcomeWithCounts() {
        var filter by mutableStateOf<MonarchPreviewRowStatus?>(null)
        rule.setMonarchContent {
            MonarchPreviewRows(
                rows = listOf(ready, failing, warned),
                columns = contactColumns,
                label = "Preview",
                filter = filter,
                onFilterChange = { filter = it },
            )
        }
        rule.onNode(hasContentDescription("Show rows")).assertExists()
        rule.onNode(hasText("All") and hasText("3")).assertIsSelected()
        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        val errors = rule.onNode(hasText("Error") and hasText("1") and isTab)
        errors.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchPreviewRowStatus.Error, filter)
        rule.onNodeWithText("Row 2", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Row 1", substring = true, useUnmergedTree = true).assertDoesNotExist()
        // Duplicates have no rows, so they have no tab.
        rule.onNodeWithText("Duplicate", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aFilterThatMatchesNothingOffersToClearIt() {
        var filter by mutableStateOf<MonarchPreviewRowStatus?>(MonarchPreviewRowStatus.Duplicate)
        rule.setMonarchContent {
            MonarchPreviewRows(
                rows = listOf(ready, failing),
                columns = contactColumns,
                label = "Preview",
                filter = filter,
                onFilterChange = { filter = it },
                labels = MonarchPreviewRowsLabels(filteredTitle = "No duplicates"),
            )
        }
        rule.onNodeWithText("No duplicates", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        assertEquals(null, filter)
    }

    @Test
    fun anEmptyFileSaysSo() {
        rule.setMonarchContent {
            MonarchPreviewRows(
                rows = emptyList(),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
            )
        }
        rule.onNodeWithText("No rows to preview", useUnmergedTree = true).assertExists()
        rule.onNode(hasContentDescription("Show rows")).assertDoesNotExist()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchPreviewRows(
                rows = listOf(duplicate),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
                filterable = false,
                labels = MonarchPreviewRowsLabels(duplicate = "Already exists", row = { "Line $it" }),
            )
        }
        rule.onNodeWithText("Line 4, Already exists", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Matches the contact created on 2 May", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theFieldNameComesFirstRightToLeftAndValuesWrap() {
        val long = MonarchPreviewRow(
            7,
            mapOf("name" to "Maximilian Alexander von Hohenberg-Lichtenstein"),
            warnings = listOf(MonarchPreviewRowIssue("Name looks long")),
        )
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchPreviewRows(
                rows = listOf(long),
                columns = contactColumns,
                label = "Preview",
                filter = null,
                onFilterChange = {},
                filterable = false,
            )
        }
        rule.onNodeWithText("Row 7, Warning", useUnmergedTree = true).assertExists()
        val value = rule.onNodeWithText("Maximilian", substring = true, useUnmergedTree = true)
        // A cell is its name over its value, as on the web, in either reading direction.
        val name = rule.onNodeWithText("Name", useUnmergedTree = true)
        assertTrue(name.getUnclippedBoundsInRoot().top < value.getUnclippedBoundsInRoot().top)
        value.assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheOutcome() {
        val t = MonarchPreviewRowsTokens
        assertEquals(MonarchPreviewRowStatus.Ready, ready.outcome)
        assertEquals(MonarchPreviewRowStatus.Error, failing.outcome)
        assertEquals(MonarchPreviewRowStatus.Warning, warned.outcome)
        assertEquals(MonarchPreviewRowStatus.Duplicate, duplicate.outcome)
        val counts = t.counts(listOf(ready, failing, failing))
        assertEquals(2, counts[MonarchPreviewRowStatus.Error])
        assertEquals(0, counts[MonarchPreviewRowStatus.Duplicate])
        assertEquals(listOf(MonarchPreviewRowStatus.Error, MonarchPreviewRowStatus.Ready), t.tabs(counts, null))
        assertTrue(MonarchPreviewRowStatus.Duplicate in t.tabs(counts, MonarchPreviewRowStatus.Duplicate))
        assertFalse(t.showsFilter(true, t.counts(listOf(ready)), null))
        assertTrue(t.showsFilter(true, t.counts(listOf(ready)), MonarchPreviewRowStatus.Ready))
        assertFalse(t.showsFilter(false, counts, null))
        val shown = t.shown(listOf(ready, failing, failing), MonarchPreviewRowStatus.Error)
        assertEquals(listOf(failing, failing), shown)
        assertEquals(MonarchTone.Info, t.tone(MonarchPreviewRowStatus.Duplicate))
        assertEquals(MonarchTone.Neutral, t.countTone(null))
        assertEquals(MonarchTone.Danger, t.countTone(MonarchPreviewRowStatus.Error))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchPreviewRowsTokens
            assertEquals(c.dangerStrong, t.issueColor(true, c), theme.id)
            assertEquals(c.warningStrong, t.issueColor(false, c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.textDefault, t.valueColor(c), theme.id)
        }
    }
}

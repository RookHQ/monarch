package dev.monarchui.components.datagrid

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels
import dev.monarchui.components.datagrid.tabletoolbar.MonarchTableToolbar
import dev.monarchui.components.datagrid.tabletoolbar.MonarchTableToolbarLabels
import dev.monarchui.components.datagrid.tabletoolbar.MonarchTableToolbarTokens
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

@Composable
private fun ToolbarFilters(onActive: () -> Unit = {}) {
    MonarchChip(text = "Active", selected = false, onSelectedChange = { onActive() })
    MonarchChip(text = "Owner: me", selected = true, onSelectedChange = {})
}

@Composable
private fun ToolbarControls(onColumns: () -> Unit = {}) {
    MonarchSegmentedControl(size = MonarchSegmentedControlSize.Sm) {
        Segment(selected = true, onClick = {}, label = "Table")
        Segment(selected = false, onClick = {}, label = "Board")
    }
    MonarchButton(text = "Columns", onClick = onColumns, size = MonarchButtonSize.Sm)
}

internal object MonarchTableToolbarFixtures : ComponentFixtures {
    override val id = "table-toolbar"
    override val fixtures = listOf(
        fixture("filters, count and controls") {
            MonarchTableToolbar(resultCount = 1204, filters = { ToolbarFilters() }) { ToolbarControls() }
        },
        fixture("count only") { MonarchTableToolbar(resultCount = 1) },
        fixture("rows selected") {
            MonarchTableToolbar(
                resultCount = 40,
                selectedCount = 3,
                totalCount = 40,
                bulkActions = listOf(MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {}),
                onClearSelection = {},
                filters = { ToolbarFilters() },
            ) { MonarchButton(text = "Columns", onClick = {}, size = MonarchButtonSize.Sm) }
        },
        fixture("long count text") {
            MonarchTableToolbar(
                resultCount = 12,
                labels = MonarchTableToolbarLabels(results = { "$it invoices match the filters for this quarter" }),
            ) { MonarchButton(text = "Export", onClick = {}, size = MonarchButtonSize.Sm) }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTableToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCountIsFormattedForTheLocaleAndIsALiveRegion() {
        rule.setMonarchContent { MonarchTableToolbar(resultCount = 1204) }
        rule.onNode(polite and hasText("1,204 results")).assertExists()
        rule.onNodeWithContentDescription("Table controls").assertExists()
    }

    @Test
    fun oneResultIsSingular() {
        rule.setMonarchContent { MonarchTableToolbar(resultCount = 1) }
        rule.onNodeWithText("1 result").assertExists()
    }

    @Test
    fun noCountIsShownWithoutAResultCount() {
        rule.setMonarchContent { MonarchTableToolbar { MonarchButton(text = "Export", onClick = {}) } }
        rule.onNodeWithText("results", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Export").assertIsDisplayed()
    }

    @Test
    fun theFiltersAndControlsAreShownAndFire() {
        var active = 0
        var columns = 0
        rule.setMonarchContent {
            MonarchTableToolbar(resultCount = 3, filters = { ToolbarFilters(onActive = { active++ }) }) {
                ToolbarControls(onColumns = { columns++ })
            }
        }
        rule.onNodeWithText("Active").performClick()
        rule.onNodeWithText("Columns").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, active)
        assertEquals(1, columns)
    }

    @Test
    fun aSelectionReplacesTheSearchAndFiltersWithTheBulkBar() {
        var selected by mutableIntStateOf(2)
        rule.setMonarchContent {
            MonarchTableToolbar(
                resultCount = 10,
                selectedCount = selected,
                totalCount = 10,
                bulkActions = listOf(MonarchBulkAction(id = "archive", title = "Archive") {}),
                onClearSelection = { selected = 0 },
                search = { Text("Search field") },
                filters = { ToolbarFilters() },
            ) { MonarchButton(text = "Columns", onClick = {}) }
        }
        rule.onNodeWithText("2 of 10 selected").assertExists()
        rule.onNodeWithText("Archive").assertExists()
        rule.onNodeWithText("Search field").assertDoesNotExist()
        rule.onNodeWithText("Active").assertDoesNotExist()
        rule.onNodeWithText("Columns").assertExists()
        rule.onNodeWithText("Clear selection").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(0, selected)
        rule.onNodeWithText("Search field").assertExists()
        rule.onNodeWithText("Active").assertExists()
        rule.onNode(polite and hasContentDescription("Selection cleared")).assertExists()
    }

    @Test
    fun withoutClearSelectionThereIsNoBulkBar() {
        rule.setMonarchContent {
            MonarchTableToolbar(selectedCount = 4, filters = { ToolbarFilters() })
        }
        rule.onNodeWithText("Active").assertExists()
        rule.onNodeWithText("4 selected").assertDoesNotExist()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchTableToolbar(
                resultCount = 5,
                selectedCount = 1,
                onClearSelection = {},
                labels = MonarchTableToolbarLabels(
                    toolbar = "Invoice controls",
                    results = { "$it invoices" },
                    bulkActionBar = MonarchBulkActionBarLabels(clear = "Deselect"),
                ),
            )
        }
        rule.onNodeWithContentDescription("Invoice controls").assertExists()
        rule.onNodeWithText("5 invoices").assertExists()
        rule.onNodeWithText("Deselect").assertExists()
    }

    @Test
    fun onAPhoneTheControlsTakeASecondLine() {
        rule.setMonarchContent {
            MonarchTableToolbar(resultCount = 3, filters = { ToolbarFilters() }) { ToolbarControls() }
        }
        val filter = rule.onNodeWithText("Active").getUnclippedBoundsInRoot()
        val columns = rule.onNodeWithText("Columns").getUnclippedBoundsInRoot()
        assertTrue(columns.top >= filter.bottom, "The controls are under the filters")
    }

    @Test
    fun atAWideWidthEverythingSharesOneLine() = assertWideLine(LayoutDirection.Ltr)

    @Test
    fun atAWideWidthEverythingSharesOneLineRightToLeft() = assertWideLine(LayoutDirection.Rtl)

    private fun assertWideLine(direction: LayoutDirection) {
        rule.setMonarchContent(layoutDirection = direction) {
            MonarchTableToolbar(
                modifier = Modifier.requiredWidth(MonarchTableToolbarTokens.WideMinWidth + 100.dp),
                resultCount = 3,
                filters = { ToolbarFilters() },
            ) { MonarchButton(text = "Columns", onClick = {}, size = MonarchButtonSize.Sm) }
        }
        val filter = rule.onNodeWithText("Active")
        val columns = rule.onNodeWithText("Columns")
        assertTrue(columns.getUnclippedBoundsInRoot().top < filter.getUnclippedBoundsInRoot().bottom)
        filter.assertIsBefore(columns, direction)
    }

    @Test
    fun theControlsComeAfterTheCountRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTableToolbar(resultCount = 3) { MonarchButton(text = "Columns", onClick = {}) }
        }
        rule.onNodeWithText("3 results").assertIsBefore(rule.onNodeWithText("Columns"), LayoutDirection.Rtl)
    }

    @Test
    fun theWideRuleNeedsTheBreakpointAndRoomForTheStart() {
        val wide = MonarchTableToolbarTokens.WideMinWidth
        assertTrue(MonarchTableToolbarTokens.wide(wide, wide / 2))
        assertFalse(MonarchTableToolbarTokens.wide(wide - 1.dp, 10.dp))
        assertFalse(MonarchTableToolbarTokens.wide(wide, wide / 2 + 1.dp))
    }

    @Test
    fun theBulkBarNeedsASelectionAndAClearAction() {
        assertTrue(MonarchTableToolbarTokens.selecting(1, canClear = true))
        assertFalse(MonarchTableToolbarTokens.selecting(0, canClear = true))
        assertFalse(MonarchTableToolbarTokens.selecting(3, canClear = false))
    }

    @Test
    fun theCountUsesTheMutedRoleInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchTableToolbarTokens.countText(colors))
        }
    }

    @Test
    fun aLongCountIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTableToolbar(
                resultCount = 12,
                labels = MonarchTableToolbarLabels(results = { "$it invoices match the filters for this quarter" }),
            ) { MonarchButton(text = "Export", onClick = {}) }
        }
        rule.onNodeWithText("12 invoices match the filters for this quarter").assertTextFullyVisible()
    }

    @Test
    fun aChangedCountIsShown() {
        var count by mutableStateOf(3)
        rule.setMonarchContent { MonarchTableToolbar(resultCount = count) }
        count = 2
        rule.waitForIdle()
        rule.onNode(polite and hasText("2 results")).assertExists()
    }
}

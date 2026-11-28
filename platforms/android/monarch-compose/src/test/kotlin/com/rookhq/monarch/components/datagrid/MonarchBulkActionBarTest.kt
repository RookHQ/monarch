package com.rookhq.monarch.components.datagrid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datagrid.bulkactionbar.BulkActionBarAnnouncement
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBar
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBarTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

private fun bulkActions(onArchive: () -> Unit = {}, onDelete: () -> Unit = {}) = listOf(
    MonarchBulkAction(id = "archive", title = "Archive", onClick = onArchive),
    MonarchBulkAction(id = "tag", title = "Add tag") {},
    MonarchBulkAction(id = "export", title = "Export", enabled = false) {},
    MonarchBulkAction(id = "delete", title = "Delete", destructive = true, onClick = onDelete),
)

internal object MonarchBulkActionBarFixtures : ComponentFixtures {
    override val id = "bulk-action-bar"
    override val fixtures = listOf(
        fixture("count of a total with two actions") {
            MonarchBulkActionBar(count = 3, onClear = {}, totalCount = 120, actions = bulkActions().take(2))
        },
        fixture("overflow menu with a destructive action") {
            MonarchBulkActionBar(count = 1, onClear = {}, actions = bulkActions(), maxVisibleActions = 1)
        },
        fixture("an action running") {
            MonarchBulkActionBar(count = 2, onClear = {}, actions = bulkActions().take(2), busyActionId = "archive")
        },
        fixture("busy") {
            MonarchBulkActionBar(count = 2, onClear = {}, actions = bulkActions(), maxVisibleActions = 1, busy = true)
        },
        fixture("no actions, long count") {
            MonarchBulkActionBar(
                count = 12,
                onClear = {},
                labels = MonarchBulkActionBarLabels(selected = { n, _ -> "$n invoices selected for the next run" }),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBulkActionBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCountTheActionsAndClearAreShownAndFire() {
        var archived = 0
        var cleared = 0
        rule.setMonarchContent {
            MonarchBulkActionBar(
                count = 3,
                onClear = { cleared++ },
                totalCount = 120,
                actions = bulkActions(onArchive = { archived++ }),
            )
        }
        rule.onNodeWithText("3 of 120 selected").assertExists()
        rule.onNodeWithContentDescription("Bulk actions").assertExists()
        rule.onNodeWithText("Archive").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Clear selection").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, archived)
        assertEquals(1, cleared)
    }

    @Test
    fun actionsBeyondTheLimitAreInTheMoreMenu() {
        var deleted = 0
        rule.setMonarchContent {
            MonarchBulkActionBar(
                count = 1,
                onClear = {},
                actions = bulkActions(onDelete = { deleted++ }),
                maxVisibleActions = 1,
            )
        }
        rule.onNodeWithText("Delete").assertDoesNotExist()
        rule.onNodeWithText("More").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Export").assertIsNotEnabled()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(1, deleted)
        rule.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun aRunningActionShowsProgressAndBlocksTheRest() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchBulkActionBar(count = 2, onClear = {}, actions = bulkActions(), busyActionId = "archive")
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Loading", rule.onNodeWithText("Archive").stateDescription())
        rule.onNodeWithText("Add tag").assertIsNotEnabled()
        rule.onNodeWithText("More").assertIsNotEnabled()
        rule.onNodeWithText("Clear selection").assertIsNotEnabled()
    }

    @Test
    fun busyBlocksEveryControl() {
        var archived = 0
        rule.setMonarchContent {
            MonarchBulkActionBar(
                count = 2,
                onClear = {},
                actions = bulkActions(onArchive = { archived++ }),
                busy = true,
            )
        }
        rule.onNodeWithText("Archive").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Clear selection").assertIsNotEnabled()
        assertEquals(0, archived)
    }

    @Test
    fun nothingIsDrawnWithoutASelection() {
        rule.setMonarchContent { MonarchBulkActionBar(count = 0, onClear = {}, actions = bulkActions()) }
        rule.onNodeWithText("Clear selection").assertDoesNotExist()
        rule.onNodeWithText("Archive").assertDoesNotExist()
    }

    @Test
    fun theCountIsAnnouncedWhenItAppearsAndChangesAndClearingIsAnnounced() {
        var count by mutableIntStateOf(0)
        rule.setMonarchContent { MonarchBulkActionBar(count = count, onClear = { count = 0 }, totalCount = 4) }
        rule.onNode(polite and hasContentDescription("selected", substring = true)).assertDoesNotExist()
        count = 2
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("2 of 4 selected")).assertExists()
        count = 3
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("3 of 4 selected")).assertExists()
        rule.onNodeWithText("Clear selection").performClick()
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Selection cleared")).assertExists()
    }

    @Test
    fun aBarComposedWithASelectionAnnouncesIt() {
        rule.setMonarchContent { MonarchBulkActionBar(count = 5, onClear = {}) }
        rule.onNode(polite and hasContentDescription("5 selected")).assertExists()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchBulkActionBar(
                count = 2,
                onClear = {},
                actions = bulkActions(),
                maxVisibleActions = 0,
                labels = MonarchBulkActionBarLabels(
                    selected = { n, _ -> "$n chosen" },
                    clear = "Deselect",
                    more = "Actions",
                    bar = "Selection tools",
                ),
            )
        }
        rule.onNodeWithText("2 chosen").assertExists()
        rule.onNodeWithText("Deselect").assertIsEnabled()
        rule.onNodeWithText("Actions").assertExists()
        rule.onNodeWithText("Archive").assertDoesNotExist()
        rule.onNodeWithContentDescription("Selection tools").assertExists()
    }

    @Test
    fun theCountIsOnTheFirstLineRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchBulkActionBar(count = 1, onClear = {}, actions = bulkActions().take(1))
        }
        // The count is on the first line, above the actions, in either direction; the clear action shares its line.
        val count = rule.onNodeWithText("1 selected").getUnclippedBoundsInRoot()
        val action = rule.onNodeWithText("Archive").getUnclippedBoundsInRoot()
        assertTrue(count.top < action.top)
        assertTrue(count.right >= action.right)
    }

    @Test
    fun aLongCountWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBulkActionBar(
                count = 12,
                onClear = {},
                labels = MonarchBulkActionBarLabels(selected = { n, _ -> "$n invoices selected for the next run" }),
            )
        }
        rule.onNodeWithText("12 invoices", substring = true).assertTextFullyVisible()
        rule.onNodeWithText("Clear selection").assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheBar() {
        val t = MonarchBulkActionBarTokens
        val actions = bulkActions()
        assertEquals(listOf("archive", "tag"), t.split(actions, 2).visible.map { it.id })
        assertEquals(listOf("export", "delete"), t.split(actions, 2).overflow.map { it.id })
        assertEquals(4, t.split(actions, -1).overflow.size)
        assertEquals(true, t.actionEnabled(actions[0], busy = false, busyActionId = "archive"))
        assertEquals(false, t.actionEnabled(actions[1], busy = false, busyActionId = "archive"))
        assertEquals(false, t.actionEnabled(actions[0], busy = true, busyActionId = null))
        assertEquals(false, t.actionEnabled(actions[2], busy = false, busyActionId = null))
        assertEquals(MonarchButtonVariant.Danger, t.variant(actions[3]))
        assertEquals(MonarchButtonVariant.Secondary, t.variant(actions[0]))
        assertEquals(BulkActionBarAnnouncement.Count, t.announcement(0, 2))
        assertEquals(BulkActionBarAnnouncement.Cleared, t.announcement(2, 0))
        assertEquals(BulkActionBarAnnouncement.None, t.announcement(0, 0))
        assertEquals(BulkActionBarAnnouncement.None, t.announcement(3, 3))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchBulkActionBarTokens
            assertEquals(colors.bgRaised, t.container(colors), theme.id)
            assertEquals(colors.borderSubtle, t.borderColor(colors), theme.id)
            assertEquals(colors.textDefault, t.countText(colors), theme.id)
        }
    }
}

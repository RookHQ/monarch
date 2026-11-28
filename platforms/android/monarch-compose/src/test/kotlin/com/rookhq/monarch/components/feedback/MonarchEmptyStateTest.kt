package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchEmptyStateFixtures : ComponentFixtures {
    override val id = "empty-state"
    override val fixtures = MonarchEmptyStateKind.entries.flatMap { kind ->
        MonarchEmptyStateSize.entries.map { size ->
            fixture("${kind.name}, ${size.name}") {
                MonarchEmptyState(
                    title = "No invoices yet",
                    kind = kind,
                    size = size,
                    message = "Invoices you create appear here.",
                    icon = { MonarchIcon(TestIcon, contentDescription = null) },
                    tip = "Tip: import invoices from a file.",
                    onClearFilters = {},
                    actions = {
                        MonarchButton(text = "Create invoice", onClick = {}, variant = MonarchButtonVariant.Primary)
                    },
                )
            }
        }
    } + listOf(
        fixture("title only") { MonarchEmptyState(title = "Nothing here") },
        fixture("long text") {
            MonarchEmptyState(
                title = "No tickets match the filters you have chosen for this queue",
                message = "Remove a filter or search for something else to see more tickets in this queue.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEmptyStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheIconIsHidden() {
        rule.setMonarchContent {
            MonarchEmptyState(
                title = "No invoices yet",
                message = "Invoices you create appear here.",
                icon = { Box(Modifier.testTag("icon").size(20.dp)) },
            )
        }
        rule.onNodeWithText("No invoices yet").assert(isHeading())
        rule.onNodeWithText("Invoices you create appear here.").assertExists()
        rule.onNodeWithTag("icon").assertDoesNotExist()
    }

    @Test
    fun aFilteredStateOffersClearFiltersBeforeTheActions() {
        var cleared = 0
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEmptyState(
                title = "No matching tickets",
                kind = MonarchEmptyStateKind.Filtered,
                onClearFilters = { cleared++ },
                actions = { MonarchButton(text = "New ticket", onClick = {}) },
            )
        }
        val clear = rule.onNodeWithText("Clear filters")
        clear.assertTouchTargetAtLeast48dp(rule.density)
        clear.assertIsBefore(rule.onNodeWithText("New ticket"), LayoutDirection.Rtl)
        clear.performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun clearFiltersIsOnlyOfferedForAFilteredState() {
        rule.setMonarchContent { MonarchEmptyState(title = "No tickets yet", onClearFilters = {}) }
        rule.onNodeWithText("Clear filters").assertDoesNotExist()
    }

    @Test
    fun theClearFiltersLabelReplacesTheBuiltInString() {
        rule.setMonarchContent {
            MonarchEmptyState(
                title = "No results",
                kind = MonarchEmptyStateKind.Filtered,
                onClearFilters = {},
                clearFiltersLabel = "Reset search",
            )
        }
        rule.onNodeWithText("Reset search").assertExists()
    }

    @Test
    fun theActionsAreButtons() {
        var created = 0
        rule.setMonarchContent {
            MonarchEmptyState(
                title = "No invoices yet",
                actions = { MonarchButton(text = "Create invoice", onClick = { created++ }) },
            )
        }
        rule.onNodeWithText("Create invoice").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, created)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEmptyState(
                title = "No tickets match the filters of this queue",
                message = "Remove a filter to see more tickets.",
                tip = "Filters are saved per queue.",
            )
        }
        rule.onNodeWithText("No tickets match", substring = true).assertTextFullyVisible()
        rule.onNodeWithText("Remove a filter", substring = true).assertTextFullyVisible()
        rule.onNodeWithText("Filters are saved", substring = true).assertTextFullyVisible()
    }

    @Test
    fun theTileTakesTheAccentOrTheSuccessTintInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val first = MonarchEmptyStateTokens.tileColors(MonarchEmptyStateKind.FirstRun, colors)
            assertEquals(colors.accentSubtle, first.container, theme.id)
            assertEquals(colors.accentStrong, first.content, theme.id)
            val success = MonarchEmptyStateTokens.tileColors(MonarchEmptyStateKind.Success, colors)
            assertEquals(colors.successSubtle, success.container, theme.id)
            assertEquals(colors.successStrong, success.content, theme.id)
        }
    }

    @Test
    fun theSmallSizeWrapsAndIsNotCutAtTheLargestFontSize() {
        // Regression (C-044): the small title was cut at font scale 2.0.
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row {
                MonarchEmptyState(
                    title = "No comments",
                    size = MonarchEmptyStateSize.Sm,
                    message = "Comments on this page appear here.",
                    tip = "Mention someone with @.",
                    modifier = Modifier.width(140.dp),
                )
                MonarchEmptyState(title = "Empty", size = MonarchEmptyStateSize.Sm)
            }
        }
        val title = rule.onNodeWithText("No comments", useUnmergedTree = true).assertTextFullyVisible()
        assertTrue(title.textLayout().lineCount >= 1)
        rule.onNodeWithText("Comments on this page", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Mention someone", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Empty", useUnmergedTree = true).assertTextFullyVisible()
    }
}

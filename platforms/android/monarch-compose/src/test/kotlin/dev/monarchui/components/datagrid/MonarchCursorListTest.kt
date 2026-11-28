package dev.monarchui.components.datagrid

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.datagrid.cursorlist.CursorListBody
import dev.monarchui.components.datagrid.cursorlist.MonarchCursorList
import dev.monarchui.components.datagrid.cursorlist.MonarchCursorListLabels
import dev.monarchui.components.datagrid.cursorlist.MonarchCursorListTokens
import dev.monarchui.components.datagrid.loadmore.MonarchLoadMoreLabels
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun tickets(count: Int) = List(count) { "Ticket ${it + 1}" }

internal object MonarchCursorListFixtures : ComponentFixtures {
    override val id = "cursor-list"
    override val fixtures = listOf(
        fixture("ready with a count") {
            MonarchCursorList(items = tickets(3), label = "Tickets", hasMore = true, onLoadMore = {}, totalCount = 9) {
                MonarchText(text = it)
            }
        },
        fixture("loading more, not divided") {
            MonarchCursorList(
                items = tickets(2),
                label = "Tickets",
                hasMore = true,
                onLoadMore = {},
                loading = true,
                divided = false,
            ) { MonarchText(text = it) }
        },
        fixture("failed later") {
            MonarchCursorList(items = tickets(2), label = "Tickets", hasMore = true, onLoadMore = {}, failed = true) {
                MonarchText(text = it)
            }
        },
        fixture("end") {
            MonarchCursorList(items = tickets(2), label = "Tickets", hasMore = false, onLoadMore = {}) {
                MonarchText(text = it)
            }
        },
        fixture("first page loading") {
            MonarchCursorList(
                items = emptyList<String>(),
                label = "Tickets",
                hasMore = true,
                onLoadMore = {},
                loading = true,
            ) {}
        },
        fixture("first page failed") {
            MonarchCursorList(
                items = emptyList<String>(),
                label = "Tickets",
                hasMore = true,
                onLoadMore = {},
                failed = true,
            ) {}
        },
        fixture("empty") {
            MonarchCursorList(items = emptyList<String>(), label = "Tickets", hasMore = false, onLoadMore = {}) {}
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCursorListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theItemsTheCountAndTheNameOfTheListAreShown() {
        rule.setMonarchContent {
            MonarchCursorList(items = tickets(2), label = "Tickets", hasMore = true, onLoadMore = {}, totalCount = 40) {
                MonarchText(text = it)
            }
        }
        rule.onNode(hasContentDescription("Tickets")).assertExists()
        rule.onNodeWithText("Ticket 2").assertExists()
        rule.onNodeWithText("Showing 2 of 40 items").assertExists()
    }

    @Test
    fun loadMoreLoadsAndFocusMovesToTheFirstNewItem() {
        var items by mutableStateOf(tickets(2))
        var loading by mutableStateOf(false)
        var loads = 0
        rule.setMonarchContent {
            MonarchCursorList(
                items = items,
                label = "Tickets",
                hasMore = true,
                onLoadMore = {
                    loads++
                    loading = true
                },
                loading = loading,
                modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
            ) { MonarchText(text = it) }
        }
        rule.onNodeWithText("Load more").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, loads)
        rule.waitForIdle()
        items = tickets(4)
        loading = false
        rule.waitForIdle()
        rule.onNode(isFocused() and hasAnyDescendant(hasText("Ticket 3"))).assertExists()
    }

    @Test
    fun autoLoadLoadsWithoutMovingFocus() {
        var items by mutableStateOf(tickets(2))
        var loads = 0
        rule.setMonarchContent {
            MonarchCursorList(
                items = items,
                label = "Tickets",
                hasMore = items.size < 4,
                onLoadMore = {
                    loads++
                    items = tickets(4)
                },
                autoLoad = true,
            ) { MonarchText(text = it) }
        }
        rule.waitForIdle()
        assertEquals(1, loads)
        rule.onNodeWithText("Ticket 4").assertExists()
        rule.onNode(isFocused()).assertDoesNotExist()
        rule.onNodeWithText("That's everything").assertExists()
    }

    @Test
    fun aFailedFirstPageShowsTheErrorStateWithRetry() {
        var retries = 0
        var loads = 0
        rule.setMonarchContent {
            MonarchCursorList(
                items = emptyList<String>(),
                label = "Tickets",
                hasMore = true,
                onLoadMore = { loads++ },
                failed = true,
                onRetry = { retries++ },
            ) {}
        }
        rule.onNodeWithText("This list could not be loaded", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Retry").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
        assertEquals(0, loads)
    }

    @Test
    fun aLaterFailureKeepsTheItemsAndRetriesFromTheFooter() {
        var loads = 0
        rule.setMonarchContent {
            MonarchCursorList(
                items = tickets(2),
                label = "Tickets",
                hasMore = true,
                onLoadMore = { loads++ },
                failed = true,
            ) { MonarchText(text = it) }
        }
        rule.onNodeWithText("Ticket 1").assertExists()
        rule.onNodeWithText("Couldn't load more.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, loads)
    }

    @Test
    fun theFirstPagePlaceholderIsNamedLoading() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchCursorList(
                items = emptyList<String>(),
                label = "Tickets",
                hasMore = true,
                onLoadMore = {},
                loading = true,
                labels = MonarchCursorListLabels(loading = "Loading tickets"),
            ) {}
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasContentDescription("Loading tickets", substring = true)).assertExists()
    }

    @Test
    fun anEmptyListShowsTheEmptyStateOrTheAppsOwn() {
        var custom by mutableStateOf(false)
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCursorList(
                items = emptyList<String>(),
                label = "Tickets",
                hasMore = false,
                onLoadMore = {},
                empty = if (custom) ({ MonarchEmptyState(title = "No tickets yet") }) else null,
            ) {}
        }
        rule.onNodeWithText("Items appear here once they are added.", useUnmergedTree = true).assertTextFullyVisible()
        custom = true
        rule.onNodeWithText("No tickets yet", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLabelsReplaceTheFootersStrings() {
        rule.setMonarchContent {
            MonarchCursorList(
                items = tickets(1),
                label = "Tickets",
                hasMore = true,
                onLoadMore = {},
                failed = true,
                labels = MonarchCursorListLabels(
                    retry = "Try again",
                    footer = MonarchLoadMoreLabels(failed = "Offline"),
                ),
            ) { MonarchText(text = it) }
        }
        rule.onNodeWithText("Try again").assertExists()
        rule.onNodeWithText("Offline").assertExists()
    }

    @Test
    fun theRulesOfTheList() {
        val t = MonarchCursorListTokens
        assertEquals(CursorListBody.Error, t.body(count = 0, hasMore = true, loading = true, failed = true))
        assertEquals(CursorListBody.Loading, t.body(count = 0, hasMore = true, loading = true, failed = false))
        assertEquals(CursorListBody.Items, t.body(count = 0, hasMore = true, loading = false, failed = false))
        assertEquals(CursorListBody.Empty, t.body(count = 0, hasMore = false, loading = false, failed = false))
        assertEquals(CursorListBody.Items, t.body(count = 3, hasMore = false, loading = false, failed = true))
        assertEquals(2, t.focusTarget(pendingFrom = 2, count = 4, loading = false))
        assertNull(t.focusTarget(pendingFrom = 2, count = 4, loading = true))
        assertNull(t.focusTarget(pendingFrom = 2, count = 2, loading = false))
        assertNull(t.focusTarget(pendingFrom = null, count = 4, loading = false))
    }
}

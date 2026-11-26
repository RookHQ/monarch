package com.rookhq.monarch.components.collaboration

import android.icu.util.TimeZone
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.notificationcenter.MonarchNotificationCenter
import com.rookhq.monarch.components.collaboration.notificationcenter.MonarchNotificationCenterFilter
import com.rookhq.monarch.components.collaboration.notificationcenter.MonarchNotificationCenterItem
import com.rookhq.monarch.components.collaboration.notificationcenter.MonarchNotificationCenterLabels
import com.rookhq.monarch.components.collaboration.notificationcenter.MonarchNotificationCenterTokens
import com.rookhq.monarch.components.collaboration.notificationcenter.NotificationCenterDayName
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/** 14 November 2026, 14:00 UTC. */
private const val Now = 1_794_664_800_000L
private const val Hour = 3_600_000L
private const val Day = 24 * Hour

private val Mention = MonarchNotificationCenterItem(
    id = "mention",
    title = "Ana mentioned you",
    time = Now - Hour,
    message = "Can you review the header?",
    actorName = "Ana Ruiz",
)
private val Paid = MonarchNotificationCenterItem(
    id = "paid",
    title = "Invoice paid",
    time = Now - 2 * Hour,
    read = true,
)
private val Invite = MonarchNotificationCenterItem(id = "invite", title = "Ben invited you", time = Now - Day)
private val Old = MonarchNotificationCenterItem(id = "old", title = "Plan renewed", time = Now - 5 * Day, read = true)
private val Items = listOf(Mention, Paid, Invite, Old)

internal object MonarchNotificationCenterFixtures : ComponentFixtures {
    override val id = "notification-center"
    override val fixtures = listOf(
        fixture("grouped by day, mark all read") {
            MonarchNotificationCenter(items = Items, now = Now, onMarkRead = {}, onMarkAllRead = {}, onSelect = {})
        },
        fixture("unread tab") {
            MonarchNotificationCenter(
                items = Items,
                now = Now,
                initialFilter = MonarchNotificationCenterFilter.Unread,
                onMarkAllRead = {},
            )
        },
        fixture("loading") { MonarchNotificationCenter(items = emptyList(), now = Now, loading = true) },
        fixture("error with retry") {
            MonarchNotificationCenter(
                items = emptyList(),
                now = Now,
                errorMessage = "Notifications did not load",
                onRetry = {},
            )
        },
        fixture("empty, nothing to mark") {
            MonarchNotificationCenter(items = emptyList(), now = Now, onMarkAllRead = {})
        },
        fixture("empty unread tab") {
            MonarchNotificationCenter(
                items = listOf(Paid),
                now = Now,
                initialFilter = MonarchNotificationCenterFilter.Unread,
            )
        },
        fixture("more to load, not grouped") {
            MonarchNotificationCenter(
                items = listOf(Mention),
                now = Now,
                groupByDay = false,
                hasMore = true,
                onLoadMore = {},
            )
        },
        fixture("loading more, server count above 99") {
            MonarchNotificationCenter(
                items = listOf(Invite),
                now = Now,
                unreadCount = 120,
                hasMore = true,
                loadingMore = true,
                onLoadMore = {},
            )
        },
        fixture("load more failed") {
            MonarchNotificationCenter(
                items = listOf(Paid),
                now = Now,
                hasMore = true,
                loadMoreFailed = true,
                onLoadMore = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchNotificationCenterTest {
    @get:Rule
    val rule = createComposeRule()

    private var savedZone: java.util.TimeZone? = null

    @Before
    fun pinTheTimeZone() {
        savedZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTheTimeZone() {
        java.util.TimeZone.setDefault(savedZone)
    }

    private val utc: TimeZone get() = TimeZone.getTimeZone("UTC")

    @Test
    fun theTabsFilterAndTheUnreadTabReadsItsCount() {
        val filters = mutableListOf<MonarchNotificationCenterFilter>()
        rule.setMonarchContent {
            MonarchNotificationCenter(items = Items, now = Now, onFilterChange = { filters += it })
        }
        rule.onNodeWithText("All").assertIsSelected()
        rule.onNodeWithContentDescription("Invoice paid", substring = true).assertExists()
        rule.onNodeWithContentDescription("Unread, 2 unread")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Unread, 2 unread").assertIsSelected()
        assertEquals(listOf(MonarchNotificationCenterFilter.Unread), filters)
        rule.onAllNodesWithContentDescription("Invoice paid", substring = true).assertCountEquals(0)
        rule.onNodeWithContentDescription("Ben invited you", substring = true).assertExists()
    }

    @Test
    fun itemsAreGroupedUnderDayHeadings() {
        rule.setMonarchContent { MonarchNotificationCenter(items = listOf(Paid, Invite, Old), now = Now) }
        rule.onNodeWithText("Today").assert(isHeading())
        rule.onNodeWithText("Yesterday").assert(isHeading())
        rule.onNodeWithText("Monday, November 9").assert(isHeading())
        rule.onNodeWithText("Notifications").assert(isHeading())
    }

    @Test
    fun openingAnUnreadItemMarksItRead() {
        val read = mutableListOf<String>()
        val selected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchNotificationCenter(
                items = Items,
                now = Now,
                onMarkRead = { read += it },
                onSelect = { selected += it.id },
            )
        }
        rule.onNodeWithContentDescription("Ana mentioned you", substring = true).performClick()
        rule.onNodeWithContentDescription("Invoice paid", substring = true).performClick()
        assertEquals(listOf("mention"), read)
        assertEquals(listOf("mention", "paid"), selected)
    }

    @Test
    fun withoutMarkReadOnSelectOpeningOnlySelects() {
        val read = mutableListOf<String>()
        var selected = 0
        rule.setMonarchContent {
            MonarchNotificationCenter(
                items = Items,
                now = Now,
                markReadOnSelect = false,
                onMarkRead = { read += it },
                onSelect = { selected++ },
            )
        }
        rule.onNodeWithContentDescription("Ana mentioned you", substring = true).performClick()
        assertEquals(emptyList(), read)
        assertEquals(1, selected)
    }

    @Test
    fun unreadRowsOfferMarkAsReadAsATalkBackAction() {
        val read = mutableListOf<String>()
        rule.setMonarchContent { MonarchNotificationCenter(items = Items, now = Now, onMarkRead = { read += it }) }
        val unread = rule.onNodeWithContentDescription("Ana mentioned you", substring = true).fetchSemanticsNode()
        val actions = unread.config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Mark as read"), actions.map { it.label })
        actions.single().action()
        assertEquals(listOf("mention"), read)
        val readRow = rule.onNodeWithContentDescription("Invoice paid", substring = true).fetchSemanticsNode()
        assertEquals(emptyList(), readRow.config.getOrNull(SemanticsActions.CustomActions).orEmpty())
    }

    @Test
    fun markAllAsReadIsOffWhenNothingIsUnread() {
        rule.setMonarchContent {
            MonarchNotificationCenter(items = listOf(Paid, Old), now = Now, onMarkAllRead = {})
        }
        rule.onNodeWithText("Mark all as read").assertIsNotEnabled()
    }

    @Test
    fun markAllAsReadRunsWhenSomethingIsUnread() {
        var marked = 0
        rule.setMonarchContent { MonarchNotificationCenter(items = Items, now = Now, onMarkAllRead = { marked++ }) }
        rule.onNodeWithText("Mark all as read")
            .assertIsEnabled()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, marked)
    }

    @Test
    fun theFirstLoadShowsPlaceholders() {
        rule.setMonarchContent { MonarchNotificationCenter(items = emptyList(), loading = true) }
        rule.onNodeWithContentDescription("Loading notifications", substring = true).assertExists()
    }

    @Test
    fun aFailedFirstLoadOffersRetry() {
        var retried = 0
        rule.setMonarchContent {
            MonarchNotificationCenter(items = emptyList(), errorMessage = "Did not load", onRetry = { retried++ })
        }
        rule.onNodeWithText("Did not load", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun emptyListsSayWhy() {
        rule.setMonarchContent { MonarchNotificationCenter(items = listOf(Paid), now = Now) }
        rule.onAllNodesWithText("You're all caught up", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Unread").performClick()
        rule.onNodeWithText("No unread notifications", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("All").performClick()
        rule.onNodeWithContentDescription("Invoice paid", substring = true).assertExists()
    }

    @Test
    fun anEmptyCenterIsCaughtUp() {
        rule.setMonarchContent { MonarchNotificationCenter(items = emptyList(), now = Now) }
        rule.onNodeWithText("You're all caught up", useUnmergedTree = true).assertExists()
    }

    @Test
    fun loadMoreEndsTheList() {
        var loads = 0
        rule.setMonarchContent {
            MonarchNotificationCenter(items = listOf(Mention), now = Now, hasMore = true, onLoadMore = { loads++ })
        }
        rule.onNodeWithText("Load more").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, loads)
    }

    @Test
    fun theServerCountIsShownAs99Plus() {
        rule.setMonarchContent { MonarchNotificationCenter(items = listOf(Mention), now = Now, unreadCount = 120) }
        rule.onNodeWithContentDescription("Unread, 120 unread").assertExists()
        rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchNotificationCenter(
                items = listOf(Mention),
                now = Now,
                onMarkAllRead = {},
                labels = MonarchNotificationCenterLabels(
                    title = "Avisos",
                    all = "Todos",
                    unreadTab = { "No leídos, $it" },
                    markAllRead = "Marcar todo",
                    today = "Hoy",
                ),
            )
        }
        rule.onNodeWithText("Avisos").assertExists()
        rule.onNodeWithText("Todos").assertExists()
        rule.onNodeWithContentDescription("No leídos, 1").assertExists()
        rule.onNodeWithText("Marcar todo").assertExists()
        rule.onNodeWithText("Hoy").assertExists()
    }

    @Test
    fun theTitleComesBeforeMarkAllInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchNotificationCenter(items = Items, now = Now, onMarkAllRead = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Notifications").assertIsBefore(rule.onNodeWithText("Mark all as read"), direction)
        }
    }

    @Test
    fun theHeadingFitsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchNotificationCenter(items = Items, now = Now, onMarkAllRead = {})
        }
        rule.onNodeWithText("Notifications").assertTextFullyVisible()
        rule.onNodeWithText("Today").assertTextFullyVisible()
    }

    @Test
    fun theLogicCountsFiltersAndGroups() {
        val t = MonarchNotificationCenterTokens
        assertEquals(2, t.unreadTotal(Items, null))
        assertEquals(40, t.unreadTotal(Items, 40))
        assertEquals(0, t.unreadTotal(Items, -1))
        assertEquals("99+", t.countText(100))
        assertEquals("99", t.countText(99))
        assertEquals(listOf(Mention, Invite), t.visible(Items, MonarchNotificationCenterFilter.Unread))
        val days = t.groupByDay(Items, utc)
        assertEquals(listOf(listOf(Mention, Paid), listOf(Invite), listOf(Old)), days.map { it.items })
        assertEquals(NotificationCenterDayName.Today, t.dayName(days[0].day, Now, utc))
        assertEquals(NotificationCenterDayName.Yesterday, t.dayName(days[1].day, Now, utc))
        assertEquals(NotificationCenterDayName.Date, t.dayName(days[2].day, Now, utc))
        assertEquals("Monday, November 9", t.dateHeading(Old.time, Now, Locale.US, utc))
        assertEquals("Friday, November 14, 2025", t.dateHeading(Now - 365 * Day, Now, Locale.US, utc))
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals(t.day(Now, utc), t.day(Now, tokyo))
        assertEquals(t.day(Now, tokyo) + 1, t.day(Now + 2 * Hour, tokyo))
    }
}

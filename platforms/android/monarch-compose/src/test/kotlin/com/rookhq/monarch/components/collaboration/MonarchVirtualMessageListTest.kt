package com.rookhq.monarch.components.collaboration

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageList
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageListLabels
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageListTokens
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageListTokens.Entry
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 14 November 2026, 14:00 UTC, a Saturday. */
private const val ListNow = 1_794_664_800_000L
private const val ListMinute = 60_000L
private const val ListDay = 24 * 60 * ListMinute

private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private class ListMessage(val id: String, val author: String, val text: String, val time: Long)

private val conversation = listOf(
    ListMessage("m1", "ana", "Morning! Is the build green?", ListNow - ListDay - 30 * ListMinute),
    ListMessage("m2", "ben", "Yes, since last night.", ListNow - ListDay - 20 * ListMinute),
    ListMessage("m3", "ana", "Shipping at noon then.", ListNow - 40 * ListMinute),
    ListMessage("m4", "ana", "Release notes are in the doc.", ListNow - 38 * ListMinute),
    ListMessage("m5", "ben", "Looks good.", ListNow - 5 * ListMinute),
)

private fun more(count: Int, from: Int = 0) = (from until from + count).map { i ->
    ListMessage("x$i", if (i % 2 == 0) "ana" else "ben", "Message number $i", ListNow - (100 - i) * ListMinute)
}

@Composable
private fun ConversationList(
    items: List<ListMessage>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    firstUnreadKey: Any? = null,
    hasOlder: Boolean = false,
    loadingOlder: Boolean = false,
    olderFailed: Boolean = false,
    loading: Boolean = false,
    labels: MonarchVirtualMessageListLabels = MonarchVirtualMessageListLabels(),
    onLoadOlder: (() -> Unit)? = null,
    onRetryOlder: (() -> Unit)? = null,
    onAtBottomChange: ((Boolean) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
) {
    MonarchVirtualMessageList(
        items = items,
        key = { it.id },
        time = { it.time },
        modifier = modifier.height(320.dp),
        state = state,
        authorId = { it.author },
        firstUnreadKey = firstUnreadKey,
        hasOlder = hasOlder,
        loadingOlder = loadingOlder,
        olderFailed = olderFailed,
        loading = loading,
        now = ListNow,
        locale = Locale.US,
        timeZone = Utc.id,
        labels = labels,
        onLoadOlder = onLoadOlder,
        onRetryOlder = onRetryOlder,
        onAtBottomChange = onAtBottomChange,
        header = header,
        footer = footer,
        status = status,
        error = error,
    ) { message, context ->
        MonarchMessageRow(
            author = MonarchMessageRowAuthor(if (message.author == "ana") "Ana Ruiz" else "Ben Okafor"),
            time = message.time,
            now = ListNow,
            grouped = context.grouped,
        ) { MonarchText(message.text) }
    }
}

internal object MonarchVirtualMessageListFixtures : ComponentFixtures {
    override val id = "virtual-message-list"
    override val fixtures = listOf(
        fixture("two days, grouped, new messages divider") {
            ConversationList(items = conversation, firstUnreadKey = "m5")
        },
        fixture("older history loading") {
            ConversationList(items = conversation.takeLast(2), hasOlder = true, loadingOlder = true)
        },
        fixture("older history failed") {
            ConversationList(items = conversation.takeLast(2), olderFailed = true, onLoadOlder = {})
        },
        fixture("more older history, footer and status") {
            ConversationList(
                items = conversation.takeLast(1),
                hasOlder = true,
                onLoadOlder = {},
                footer = { MonarchText("Seen by Ben") },
                status = { MonarchText("Ben is typing…") },
            )
        },
        fixture("first load") {
            ConversationList(items = emptyList(), loading = true)
        },
        fixture("empty with a header") {
            ConversationList(items = emptyList(), header = { MonarchText("This is the start of your chat with Ben.") })
        },
        fixture("first load failed") {
            ConversationList(items = emptyList(), error = { MonarchText("Couldn't load messages.") })
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchVirtualMessageListTest {
    @get:Rule
    val rule = createComposeRule()

    private val t = MonarchVirtualMessageListTokens

    private fun reads(text: String) = SemanticsMatcher("reads $text") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == text } ||
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it == text }
    }

    private val politeAnnouncement = SemanticsMatcher("an announcement") { node ->
        node.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite &&
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().isNotEmpty()
    }

    @Test
    fun theRowsAreNewestFirstWithDaysUnreadAndGroups() {
        val entries = t.entries(
            keys = conversation.map { it.id },
            times = conversation.map { it.time },
            authors = conversation.map { it.author },
            firstUnreadIndex = 4,
            showOlder = false,
            hasHeader = true,
            hasFooter = true,
            groupWindowMillis = t.GROUP_WINDOW_MILLIS,
            timeZone = Utc,
        )
        val today = t.startOfDay(ListNow, Utc)
        val yesterday = t.startOfDay(ListNow - ListDay, Utc)
        assertEquals(
            listOf(
                Entry.Footer,
                Entry.Message(4, false, "m5"),
                Entry.Unread,
                Entry.Message(3, true, "m4"),
                Entry.Message(2, false, "m3"),
                Entry.Day(today),
                Entry.Message(1, false, "m2"),
                Entry.Message(0, false, "m1"),
                Entry.Day(yesterday),
                Entry.Header,
            ),
            entries,
        )
        val older = t.entries(listOf("a"), listOf(ListNow), listOf(null), -1, true, true, false, 1L, Utc)
        assertEquals(listOf(Entry.Message(0, false, "a"), Entry.Day(today), Entry.Older), older)
    }

    @Test
    fun groupingNeedsTheSameKnownAuthorWithinTheWindow() {
        assertTrue(t.grouped(0L, "ana", 60_000L, "ana", 300_000L))
        assertTrue(!t.grouped(0L, "ana", 301_000L, "ana", 300_000L))
        assertTrue(!t.grouped(0L, "ana", 1_000L, "ben", 300_000L))
        assertTrue(!t.grouped(0L, null, 1_000L, null, 300_000L))
        assertTrue(!t.grouped(10_000L, "ana", 0L, "ana", 300_000L))
    }

    @Test
    fun dayLabelsAreTodayYesterdayThenTheDate() {
        val label = { day: Long -> t.dayLabel(t.startOfDay(day, Utc), ListNow, Locale.US, Utc, "Today", "Yesterday") }
        assertEquals("Today", label(ListNow))
        assertEquals("Yesterday", label(ListNow - ListDay))
        assertEquals("Thursday, November 12", label(ListNow - 2 * ListDay))
        assertEquals("Friday, November 14, 2025", label(ListNow - 365 * ListDay))
    }

    @Test
    fun theScrollRulesArePure() {
        assertTrue(t.atBottom(0, 10, 200))
        assertTrue(!t.atBottom(0, 300, 200))
        assertTrue(!t.atBottom(1, 0, 200))
        assertTrue(t.shouldLoadOlder(8, 10, 1, hasOlder = true, loadingOlder = false, olderFailed = false))
        assertTrue(!t.shouldLoadOlder(7, 10, 1, hasOlder = true, loadingOlder = false, olderFailed = false))
        assertTrue(!t.shouldLoadOlder(9, 10, 1, hasOlder = true, loadingOlder = true, olderFailed = false))
        assertTrue(!t.shouldLoadOlder(9, 10, 1, hasOlder = true, loadingOlder = false, olderFailed = true))
        assertTrue(!t.shouldLoadOlder(9, 10, 1, hasOlder = false, loadingOlder = false, olderFailed = false))
        assertEquals(2, t.arrived("b", listOf("a", "b", "c", "d")))
        assertEquals(0, t.arrived(null, listOf("a")))
        assertEquals(0, t.arrived("z", listOf("a")))
    }

    @Test
    fun separatorsAreHeadingsAndTheListIsNamed() {
        rule.setMonarchContent { ConversationList(items = conversation, firstUnreadKey = "m5") }
        rule.onNodeWithContentDescription("Messages").assertExists()
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        rule.onNodeWithText("Today").assert(heading)
        rule.onNodeWithText("New messages").assert(heading)
        rule.onNodeWithText("Looks good.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aNewMessageAtTheBottomStaysInViewAndIsAnnounced() {
        var items by mutableStateOf(conversation)
        val state = LazyListState()
        rule.setMonarchContent { ConversationList(items = items, state = state) }
        rule.onAllNodes(politeAnnouncement).assertCountEquals(0)
        items = items + ListMessage("m6", "ben", "One more thing.", ListNow)
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
        rule.onNodeWithText("One more thing.", useUnmergedTree = true).assertExists()
        rule.onNode(politeAnnouncement).assert(reads("1 new message"))
        rule.onAllNodesWithText("Jump to latest").assertCountEquals(0)
    }

    @Test
    fun scrolledUpTheJumpButtonCountsArrivalsAndReturnsToTheBottom() {
        var items by mutableStateOf(more(30))
        val state = LazyListState()
        val bottom = mutableListOf<Boolean>()
        rule.setMonarchContent {
            ConversationList(items = items, state = state, onAtBottomChange = { bottom += it })
        }
        rule.runOnIdle { state.requestScrollToItem(20) }
        rule.waitForIdle()
        rule.onNodeWithText("Jump to latest").assertTouchTargetAtLeast48dp(rule.density)
        items = items + more(2, from = 30)
        rule.waitForIdle()
        rule.onNodeWithText("2 new messages").performClick()
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
        rule.onAllNodesWithText("Jump to latest").assertCountEquals(0)
        assertEquals(listOf(true, false, true), bottom)
    }

    @Test
    fun olderHistoryIsAskedForNearTheTop() {
        var asked = 0
        rule.setMonarchContent {
            ConversationList(items = conversation.takeLast(2), hasOlder = true, onLoadOlder = { asked++ })
        }
        rule.waitForIdle()
        assertEquals(1, asked)
        rule.onNodeWithText("Load older messages").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(2, asked)
    }

    @Test
    fun aFailedOlderPageOffersRetry() {
        var retried = 0
        var loaded = 0
        rule.setMonarchContent {
            ConversationList(
                items = conversation.takeLast(2),
                hasOlder = true,
                olderFailed = true,
                onLoadOlder = { loaded++ },
                onRetryOlder = { retried++ },
            )
        }
        rule.onNodeWithText("Couldn’t load older messages.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
        assertEquals(0, loaded)
    }

    @Test
    fun theFirstLoadEmptyAndErrorStatesReplaceTheList() {
        var loading by mutableStateOf(true)
        var failed by mutableStateOf(false)
        rule.setMonarchContent {
            ConversationList(
                items = emptyList(),
                loading = loading,
                header = { MonarchText("Start of the chat") },
                error = if (failed) ({ MonarchText("Couldn't load") }) else null,
            )
        }
        rule.onNodeWithContentDescription("Loading messages", substring = true).assertExists()
        loading = false
        rule.onNodeWithText("Start of the chat").assertExists()
        failed = true
        rule.onNodeWithText("Couldn't load").assertExists()
        rule.onAllNodesWithText("Start of the chat").assertCountEquals(0)
    }

    @Test
    fun theStatusIsInAPoliteLiveRegion() {
        rule.setMonarchContent {
            ConversationList(items = conversation, status = { MonarchText("Ben is typing…") })
        }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(polite and hasText("Ben is typing…")).assertExists()
    }

    @Test
    fun separatorsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            ConversationList(items = listOf(ListMessage("old", "ana", "Hi", ListNow - 2 * ListDay)))
        }
        rule.onNodeWithText("Thursday, November 12", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            ConversationList(
                items = conversation,
                firstUnreadKey = "m3",
                labels = MonarchVirtualMessageListLabels(
                    label = "Mensajes",
                    today = "Hoy",
                    newMessages = "Nuevos",
                    yesterday = "Ayer",
                ),
            )
        }
        rule.onNodeWithContentDescription("Mensajes").assertExists()
        rule.onNodeWithText("Hoy").assertExists()
        rule.onNodeWithText("Nuevos").assertExists()
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, t.dayColor(c), theme.id)
            assertEquals(c.accentStrong, t.unreadColor(c), theme.id)
            assertEquals(c.accentBorder, t.unreadLineColor(c), theme.id)
        }
    }
}

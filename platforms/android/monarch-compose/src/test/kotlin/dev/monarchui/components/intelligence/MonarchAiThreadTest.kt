@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import android.icu.util.TimeZone
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationError
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationStatus
import dev.monarchui.components.intelligence.aiconversation.MonarchAiErrorPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aithread.AiThreadEnding
import dev.monarchui.components.intelligence.aithread.AiThreadNoticeKind
import dev.monarchui.components.intelligence.aithread.AiThreadSpeech
import dev.monarchui.components.intelligence.aithread.AiThreadSpeechState
import dev.monarchui.components.intelligence.aithread.MonarchAiThread
import dev.monarchui.components.intelligence.aithread.MonarchAiThreadLabels
import dev.monarchui.components.intelligence.aithread.MonarchAiThreadMessageContext
import dev.monarchui.components.intelligence.aithread.MonarchAiThreadTokens
import dev.monarchui.components.intelligence.aithread.MonarchAiThreadTokens.Entry
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 14 November 2026, 14:00 UTC, a Saturday. */
private const val ThreadNow = 1_794_664_800_000L
private const val ThreadMinute = 60_000L
private const val ThreadDay = 24 * 60 * ThreadMinute

private val ThreadUtc: TimeZone = TimeZone.getTimeZone("UTC")

private fun threadMessage(
    id: String,
    role: MonarchAiRole,
    text: String,
    at: Long = ThreadNow,
    streaming: Boolean = false,
    extra: List<MonarchAiPart> = emptyList(),
) = MonarchAiConversationMessage(
    id = id,
    role = role,
    parts = listOf(
        MonarchAiTextPart(text, state = if (streaming) MonarchAiPartState.Streaming else MonarchAiPartState.Done),
    ) + extra,
    createdAt = MonarchAiConversationMessage.timestamp(at),
)

private val TwoDays = listOf(
    threadMessage("u1", MonarchAiRole.User, "Is invoice 42 paid?", ThreadNow - ThreadDay - 30 * ThreadMinute),
    threadMessage("a1", MonarchAiRole.Assistant, "Not yet.", ThreadNow - ThreadDay - 29 * ThreadMinute),
    threadMessage("u2", MonarchAiRole.User, "Remind them today.", ThreadNow - 5 * ThreadMinute),
)

private fun many(count: Int, from: Int = 0) = (from until from + count).map { i ->
    threadMessage(
        "m$i",
        if (i % 2 == 0) MonarchAiRole.User else MonarchAiRole.Assistant,
        "Message number $i",
        ThreadNow - (100 - i) * ThreadMinute,
    )
}

/** The thread under test, as plain text rows so the tests do not depend on MonarchAiMessage's layout. */
@Suppress("LongParameterList")
@Composable
private fun TestThread(
    messages: List<MonarchAiConversationMessage>,
    state: LazyListState = rememberLazyListState(),
    status: MonarchAiConversationStatus = MonarchAiConversationStatus.Idle,
    error: MonarchAiConversationError? = null,
    offline: Boolean = false,
    queuedCount: Int = 0,
    slow: Boolean = false,
    hasEarlier: Boolean = false,
    loadingEarlier: Boolean = false,
    announces: Boolean = true,
    labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
    onStop: (() -> Unit)? = null,
    onLoadEarlier: (() -> Unit)? = null,
    jumpIcon: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    row: (@Composable (MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Unit)? = null,
) {
    MonarchAiThread(
        messages = messages,
        modifier = Modifier.height(260.dp),
        state = state,
        status = status,
        error = error,
        offline = offline,
        queuedCount = queuedCount,
        slow = slow,
        hasEarlier = hasEarlier,
        loadingEarlier = loadingEarlier,
        timeZone = "UTC",
        nowMillis = ThreadNow,
        announces = announces,
        labels = labels,
        onStop = onStop,
        onLoadEarlier = onLoadEarlier,
        jumpIcon = jumpIcon,
        empty = empty,
    ) { message, context ->
        if (row != null) row(message, context) else MonarchText(message.text)
    }
}

internal object MonarchAiThreadFixtures : ComponentFixtures {
    override val id = "ai-thread"
    override val fixtures = listOf(
        fixture("two days with the default messages") {
            MonarchAiThread(
                messages = TwoDays,
                modifier = Modifier.height(260.dp),
                timeZone = "UTC",
                nowMillis = ThreadNow,
            )
        },
        fixture("submitted, thinking") {
            TestThread(messages = TwoDays.takeLast(1), status = MonarchAiConversationStatus.Submitted)
        },
        fixture("load earlier, and loading") {
            TestThread(messages = TwoDays.takeLast(1), hasEarlier = true, onLoadEarlier = {})
        },
        fixture("loading earlier") {
            TestThread(messages = TwoDays.takeLast(1), hasEarlier = true, loadingEarlier = true, onLoadEarlier = {})
        },
        fixture("scrolled up, jump to latest") {
            TestThread(
                messages = many(20),
                state = rememberLazyListState(initialFirstVisibleItemIndex = 10),
                jumpIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("offline with a queued message") {
            TestThread(messages = TwoDays.takeLast(1), offline = true, queuedCount = 1)
        },
        fixture("sending queued messages") {
            TestThread(messages = TwoDays.takeLast(1), queuedCount = 2)
        },
        fixture("slow reply") {
            TestThread(
                messages = TwoDays.takeLast(1),
                status = MonarchAiConversationStatus.Streaming,
                slow = true,
            )
        },
        fixture("empty") {
            TestThread(messages = emptyList(), empty = { MonarchText("Ask anything about your invoices.") })
        },
    )
}

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiThreadTest {
    @get:Rule
    val rule = createComposeRule()

    private val t = MonarchAiThreadTokens
    private val s = AiThreadSpeech

    private fun announcement(mode: LiveRegionMode, text: String) = SemanticsMatcher("$mode announcement $text") {
        it.config.getOrNull(SemanticsProperties.LiveRegion) == mode &&
            it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { d -> d == text }
    }

    private val anyPolite = SemanticsMatcher("a polite announcement") {
        it.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite &&
            it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().isNotEmpty()
    }

    @Test
    fun theRowsAreNewestFirstWithDaysThinkingAndEarlier() {
        val entries = t.entries(
            ids = TwoDays.map { it.id },
            times = TwoDays.map { it.createdAtMillis },
            daySeparators = true,
            thinking = true,
            showEarlier = true,
            timeZone = ThreadUtc,
        )
        val today = t.startOfDay(ThreadNow, ThreadUtc)
        val yesterday = t.startOfDay(ThreadNow - ThreadDay, ThreadUtc)
        assertEquals(
            listOf(
                Entry.Thinking,
                Entry.Message(2, "u2"),
                Entry.Day(today, "u2"),
                Entry.Message(1, "a1"),
                Entry.Message(0, "u1"),
                Entry.Day(yesterday, "u1"),
                Entry.Earlier,
            ),
            entries,
        )
        val plain = t.entries(listOf("a", "b"), listOf(null, ThreadNow), false, false, false, ThreadUtc)
        assertEquals(listOf(Entry.Message(1, "b"), Entry.Message(0, "a")), plain)
        val noTime = t.entries(listOf("a", "b"), listOf(null, ThreadNow), true, false, false, ThreadUtc)
        assertEquals(listOf(Entry.Message(1, "b"), Entry.Day(today, "b"), Entry.Message(0, "a")), noTime)
    }

    @Test
    fun dayLabelsAreTodayYesterdayThenTheDateInTheZone() {
        val label = { day: Long, zone: TimeZone ->
            t.dayLabel(t.startOfDay(day, zone), ThreadNow, Locale.US, zone, "Today", "Yesterday")
        }
        assertEquals("Today", label(ThreadNow, ThreadUtc))
        assertEquals("Yesterday", label(ThreadNow - ThreadDay, ThreadUtc))
        assertEquals("Thursday, November 12", label(ThreadNow - 2 * ThreadDay, ThreadUtc))
        assertEquals("Friday, November 14, 2025", label(ThreadNow - 365 * ThreadDay, ThreadUtc))
        // 23:30 UTC on Friday is already Saturday in Tokyo, so it is today there.
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        val lateFriday = ThreadNow - 14 * 60 * ThreadMinute - 30 * ThreadMinute
        assertEquals("Yesterday", label(lateFriday, ThreadUtc))
        assertEquals("Today", label(lateFriday, tokyo))
    }

    @Test
    fun theScrollNoticeAndErrorRulesArePure() {
        assertTrue(t.atBottom(0, 10, 100))
        assertFalse(t.atBottom(0, 300, 100))
        assertFalse(t.atBottom(1, 0, 100))
        assertEquals(2, t.newCount(listOf("a", "b", "c"), "a"))
        assertEquals(0, t.newCount(listOf("a"), null))
        assertEquals(0, t.newCount(listOf("a"), "gone"))
        assertEquals(AiThreadNoticeKind.Offline, t.notice(offline = true, queuedCount = 2, slow = true, busy = true))
        assertEquals(AiThreadNoticeKind.Sending, t.notice(offline = false, queuedCount = 2, slow = true, busy = true))
        assertEquals(AiThreadNoticeKind.Slow, t.notice(offline = false, queuedCount = 0, slow = true, busy = true))
        assertNull(t.notice(offline = false, queuedCount = 0, slow = true, busy = false))
        assertEquals(MonarchTone.Warning, t.noticeTone(AiThreadNoticeKind.Offline))
        assertEquals(MonarchTone.Warning, t.noticeTone(AiThreadNoticeKind.Sending))
        assertEquals(MonarchTone.Neutral, t.noticeTone(AiThreadNoticeKind.Slow))
        val errorPart = MonarchAiErrorPart("network", "Down", retryable = true)
        val failed = threadMessage("a", MonarchAiRole.Assistant, "", extra = listOf(errorPart))
        assertTrue(t.announcesError(true, TwoDays.last()))
        assertTrue(t.announcesError(true, null))
        assertFalse(t.announcesError(true, failed))
        assertFalse(t.announcesError(false, TwoDays.last()))
        assertTrue(t.showsEmpty(0, thinking = false, showEarlier = false, hasEmpty = true))
        assertFalse(t.showsEmpty(0, thinking = true, showEarlier = false, hasEmpty = true))
        assertFalse(t.showsEmpty(0, thinking = false, showEarlier = true, hasEmpty = true))
        assertFalse(t.showsEmpty(1, thinking = false, showEarlier = false, hasEmpty = true))
        assertFalse(t.showsEmpty(0, thinking = false, showEarlier = false, hasEmpty = false))
        assertTrue(t.busy(MonarchAiConversationStatus.Submitted))
        assertTrue(t.busy(MonarchAiConversationStatus.Streaming))
        assertFalse(t.busy(MonarchAiConversationStatus.AwaitingApproval))
    }

    private fun reply(text: String, streaming: Boolean = false) =
        threadMessage("a", MonarchAiRole.Assistant, text, streaming = streaming)

    @Test
    fun aReplyIsReadInSentencesThenCompleted() {
        val streaming = MonarchAiConversationStatus.Streaming
        val first = s.speak(null, reply("Hello there. How", streaming = true), "Hello there. How", streaming)
        assertEquals("Hello there.", first.text)
        assertEquals(AiThreadSpeechState("a", 12, live = true), first.state)
        val more = "Hello there. How can"
        val none = s.speak(first.state, reply(more, streaming = true), more, streaming)
        assertEquals("", none.text)
        val whole = "Hello there. How can I help?"
        val done = s.speak(none.state, reply(whole), whole, MonarchAiConversationStatus.Idle)
        assertEquals(" How can I help?", done.text)
        assertTrue(done.finished)
        assertEquals(AiThreadEnding.Complete, done.ending)
        val cut = "Hello there. How"
        val stopped = s.speak(first.state, reply(cut), cut, MonarchAiConversationStatus.Stopped)
        assertEquals(AiThreadEnding.Stopped, stopped.ending)
        val failed = s.speak(first.state, reply(cut), cut, MonarchAiConversationStatus.Error)
        assertTrue(failed.finished)
        assertNull(failed.ending)
        val waiting = s.speak(first.state, reply(cut), cut, MonarchAiConversationStatus.AwaitingApproval)
        assertFalse(waiting.finished)
    }

    @Test
    fun aReplyFirstSeenFinishedOrThePersonsMessageIsNotRead() {
        val history = s.speak(null, TwoDays[1], "Not yet.", MonarchAiConversationStatus.Idle)
        assertEquals("", history.text)
        assertFalse(history.finished)
        assertEquals(AiThreadSpeechState("a1", 8, live = false), history.state)
        val user = s.speak(null, TwoDays[0], "Is invoice 42 paid?", MonarchAiConversationStatus.Idle)
        assertNull(user.state)
        assertEquals(-1, s.lastBoundary("No end yet", 0))
        assertEquals(11, s.lastBoundary("First line\nSecond.", 0))
    }

    @Test
    fun codeBlocksAreNamedAndMarksDropped() {
        val spoken = s.tidy(s.spoken("## Steps\nRun **this**:\n```sh\nrm -rf build\n```\nThen `done`.", "Code block"))
        assertEquals("Steps Run this: Code block. Then done.", spoken)
    }

    @Test
    fun theListIsNamedAndShowsDayLabels() {
        rule.setMonarchContent { TestThread(messages = TwoDays) }
        rule.onNodeWithContentDescription("Conversation").assertExists()
        rule.onNodeWithContentDescription("Today").assertExists()
        rule.onNodeWithContentDescription("Yesterday").assertExists()
        rule.onNodeWithText("Remind them today.").assertExists()
    }

    @Test
    fun aNewMessageAtTheBottomStaysInView() {
        var messages by mutableStateOf(many(20))
        val state = LazyListState()
        rule.setMonarchContent { TestThread(messages = messages, state = state) }
        messages = messages + threadMessage("new", MonarchAiRole.User, "One more question.", ThreadNow)
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
        rule.onNodeWithText("One more question.").assertExists()
        rule.onAllNodesWithText("Jump to latest").assertCountEquals(0)
    }

    @Test
    fun scrolledUpTheJumpButtonCountsNewMessagesAndReturnsToTheBottom() {
        var messages by mutableStateOf(many(30))
        val state = LazyListState()
        rule.setMonarchContent { TestThread(messages = messages, state = state) }
        rule.runOnIdle { state.requestScrollToItem(20) }
        rule.waitForIdle()
        rule.onNodeWithText("Jump to latest").assertTouchTargetAtLeast48dp(rule.density)
        messages = messages + many(2, from = 30)
        rule.waitForIdle()
        assertTrue(state.firstVisibleItemIndex > 0)
        rule.onNodeWithText("2 new messages", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jump to latest").performClick()
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
        rule.onAllNodesWithText("Jump to latest").assertCountEquals(0)
    }

    @Test
    fun theJumpIconComesBeforeTheLabelInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            TestThread(
                messages = many(20),
                state = rememberLazyListState(initialFirstVisibleItemIndex = 10),
                jumpIcon = { MonarchIcon(TestIcon, contentDescription = "Down") },
            )
        }
        rule.onNodeWithContentDescription("Down", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Jump to latest", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun loadEarlierCallsBackAndShowsItsProgress() {
        var asked = 0
        var loading by mutableStateOf(false)
        rule.setMonarchContent {
            TestThread(
                messages = TwoDays.takeLast(1),
                hasEarlier = true,
                loadingEarlier = loading,
                onLoadEarlier = { asked++ },
            )
        }
        rule.onNodeWithText("Load earlier messages").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, asked)
        loading = true
        rule.onNodeWithText("Load earlier messages").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading earlier messages…"),
        )
    }

    @Test
    fun withoutACallbackThereIsNoLoadButton() {
        rule.setMonarchContent { TestThread(messages = TwoDays, hasEarlier = true) }
        rule.onAllNodesWithText("Load earlier messages").assertCountEquals(0)
    }

    @Test
    fun aSubmittedReplyShowsAndAnnouncesThinking() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { TestThread(messages = TwoDays, status = MonarchAiConversationStatus.Submitted) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // Only the thread announces: the indicator is told not to, so it is said once.
        rule.onAllNodes(announcement(LiveRegionMode.Polite, "Thinking…")).assertCountEquals(1)
        rule.onNodeWithText("Thinking…", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theThinkingIndicatorAnnouncesItselfWithoutTheThreadAnnouncer() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            TestThread(messages = TwoDays, status = MonarchAiConversationStatus.Submitted, announces = false)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // The indicator's own live region is the only one.
        rule.onAllNodes(announcement(LiveRegionMode.Polite, "Thinking…")).assertCountEquals(1)
        rule.onAllNodes(anyPolite).assertCountEquals(1)
    }

    @Test
    fun aStreamingReplyIsAnnouncedBySentenceThenComplete() {
        var messages by mutableStateOf(TwoDays)
        var status by mutableStateOf(MonarchAiConversationStatus.Idle)
        rule.setMonarchContent { TestThread(messages = messages, status = status) }
        rule.onAllNodes(anyPolite).assertCountEquals(0)
        status = MonarchAiConversationStatus.Streaming
        messages = TwoDays + threadMessage("a2", MonarchAiRole.Assistant, "Done. I sent", streaming = true)
        rule.waitForIdle()
        rule.onNode(announcement(LiveRegionMode.Polite, "Done.")).assertExists()
        status = MonarchAiConversationStatus.Idle
        messages = TwoDays + threadMessage("a2", MonarchAiRole.Assistant, "Done. I sent the reminder.")
        rule.waitForIdle()
        rule.onNode(announcement(LiveRegionMode.Polite, "I sent the reminder. Response complete")).assertExists()
    }

    @Test
    fun anErrorNoMessageShowsIsAnnouncedAssertively() {
        var messages by mutableStateOf(TwoDays)
        rule.setMonarchContent {
            TestThread(messages = messages, error = MonarchAiConversationError("network", "Connection lost", true))
        }
        rule.onNode(announcement(LiveRegionMode.Assertive, "Error: Connection lost")).assertExists()
        messages = TwoDays + threadMessage(
            "a2",
            MonarchAiRole.Assistant,
            "",
            extra = listOf(MonarchAiErrorPart("network", "Connection lost", retryable = true)),
        )
        rule.onAllNodes(announcement(LiveRegionMode.Assertive, "Error: Connection lost")).assertCountEquals(0)
    }

    @Test
    fun withoutTheThreadAnnouncerMessagesAnnounceThemselves() {
        val seen = mutableListOf<Boolean>()
        rule.setMonarchContent {
            TestThread(
                messages = TwoDays.takeLast(1),
                announces = false,
                row = { m, c ->
                    seen += c.announces
                    MonarchText(m.text)
                },
            )
        }
        rule.waitForIdle()
        assertTrue(seen.isNotEmpty() && seen.all { it })
        rule.onAllNodes(anyPolite).assertCountEquals(0)
    }

    @Test
    fun theThreadAnnouncerSilencesTheMessagesAndGivesTheirPlace() {
        val contexts = mutableListOf<MonarchAiThreadMessageContext>()
        rule.setMonarchContent {
            TestThread(
                messages = TwoDays,
                row = { m, c ->
                    contexts += c
                    MonarchText(m.text)
                },
            )
        }
        rule.waitForIdle()
        assertTrue(contexts.none { it.announces })
        assertTrue(contexts.any { it.index == 2 && it.isLast })
        assertTrue(contexts.any { it.index == 0 && !it.isLast })
    }

    @Test
    fun noticesAreInAPoliteLiveRegionAndNameTheQueue() {
        var offline by mutableStateOf(true)
        var slow by mutableStateOf(false)
        rule.setMonarchContent {
            TestThread(
                messages = TwoDays,
                offline = offline,
                queuedCount = if (slow) 0 else 2,
                slow = slow,
                status = MonarchAiConversationStatus.Streaming,
            )
        }
        val offlineText = "You are offline. 2 messages will send when you are back online."
        rule.onNode(announcement(LiveRegionMode.Polite, offlineText)).assertExists()
        rule.onNodeWithText(offlineText, substring = true).assertExists()
        offline = false
        rule.onNode(announcement(LiveRegionMode.Polite, "Back online. Sending 2 queued messages…")).assertExists()
        slow = true
        rule.onNode(announcement(LiveRegionMode.Polite, "This is taking longer than usual. Still working…"))
            .assertExists()
    }

    @Test
    fun noticesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            TestThread(messages = TwoDays.takeLast(1), offline = true)
        }
        val text = "You are offline. Messages you send will wait until you are back online."
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun escapeStopsARunningReply() {
        var stopped = 0
        var status by mutableStateOf(MonarchAiConversationStatus.Streaming)
        rule.setMonarchContent {
            TestThread(
                messages = TwoDays.takeLast(1),
                status = status,
                onStop = { stopped++ },
                row = { _, _ -> MonarchTextField(value = "", onValueChange = {}, label = "Note") },
            )
        }
        // A text field takes focus in touch mode too; the thread sees the key before it.
        val button = rule.onNode(hasSetTextAction())
        button.performSemanticsAction(SemanticsActions.RequestFocus)
        button.performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, stopped)
        status = MonarchAiConversationStatus.Idle
        rule.waitForIdle()
        button.performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, stopped)
    }

    @Test
    fun theEmptyContentShowsWithoutMessages() {
        rule.setMonarchContent {
            TestThread(messages = emptyList(), empty = { MonarchText("Ask anything about your invoices.") })
        }
        rule.onNodeWithText("Ask anything about your invoices.").assertExists()
        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Conversation")))
            .assertCountEquals(0)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            TestThread(
                messages = many(20),
                state = rememberLazyListState(initialFirstVisibleItemIndex = 10),
                offline = true,
                labels = MonarchAiThreadLabels(
                    label = "Conversación",
                    jumpToLatest = "Ir al final",
                    offline = "Sin conexión",
                ),
            )
        }
        rule.onNodeWithContentDescription("Conversación").assertExists()
        rule.onNodeWithText("Ir al final").assertExists()
        rule.onNodeWithText("Sin conexión", substring = true).assertExists()
    }
}

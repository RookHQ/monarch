package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.components.intelligence.aireasoning.AiReasoning
import com.rookhq.monarch.components.intelligence.aireasoning.MonarchAiReasoning
import com.rookhq.monarch.components.intelligence.aireasoning.MonarchAiReasoningLabels
import com.rookhq.monarch.components.intelligence.aireasoning.MonarchAiReasoningTokens
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
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val Reasoning = "Sum the monthly rows of Q3, then compare with Q2."
private const val Start = 1_800_000_000_000L

/** Long enough for a disclosure animation to finish, shorter than one tick of the timer. */
private const val FrameSettle = 600L

internal object MonarchAiReasoningFixtures : ComponentFixtures {
    override val id = "ai-reasoning"
    override val fixtures = listOf(
        fixture("finished, closed, with an expand icon") {
            MonarchAiReasoning(
                part = MonarchAiReasoningPart(text = Reasoning, durationMs = 12_000L),
                expandIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("finished, open") {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = Reasoning), initiallyExpanded = true)
        },
        fixture("streaming with text") {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = Reasoning, state = MonarchAiPartState.Streaming))
        },
        fixture("streaming, no text yet") {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = "", state = MonarchAiPartState.Streaming))
        },
        fixture("redacted with a mark") {
            MonarchAiReasoning(
                part = MonarchAiReasoningPart(text = "", redacted = true),
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiReasoningTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAiReasoningTokens

    @Test
    fun finishedReasoningIsAClosedDisclosureWithItsDuration() {
        rule.setMonarchContent {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = Reasoning, durationMs = 12_400L))
        }
        val header = rule.onNodeWithText("Thought for 12s")
        assertEquals("Collapsed", header.stateDescription())
        header.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText(Reasoning).assertDoesNotExist()
        rule.onNodeWithContentDescription("Assistant reasoning").assertExists()
        header.performClick()
        rule.onNodeWithText(Reasoning).assertExists()
    }

    @Test
    fun streamingStartsOpenTicksAndClosesWhenItFinishes() {
        var now = Start
        var part by mutableStateOf(MonarchAiReasoningPart(text = Reasoning, state = MonarchAiPartState.Streaming))
        val changes = mutableListOf<Boolean>()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            AiReasoning(
                part = part,
                modifier = Modifier,
                startedAt = null,
                initiallyExpanded = false,
                onExpandedChange = { changes += it },
                icon = null,
                expandIcon = null,
                labels = MonarchAiReasoningLabels(),
                clock = { now },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Thinking…").assertExists()
        rule.onNodeWithText(Reasoning).assertExists()
        now = Start + 5_000L
        rule.mainClock.advanceTimeBy(tokens.TICK_MILLIS)
        rule.onNodeWithText("Thinking… 5s").assertExists()
        now = Start + 65_000L
        part = part.copy(state = MonarchAiPartState.Done)
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(FrameSettle)
        rule.onNodeWithText("Thought for 1m 5s").assertExists()
        rule.onNodeWithText(Reasoning).assertDoesNotExist()
        assertEquals(listOf(false), changes)
    }

    @Test
    fun aDisclosureThePersonTouchedStaysAsTheyLeftIt() {
        var part by mutableStateOf(MonarchAiReasoningPart(text = Reasoning, state = MonarchAiPartState.Streaming))
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiReasoning(part = part) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Thinking…").performClick()
        rule.mainClock.advanceTimeBy(FrameSettle)
        rule.onNodeWithText(Reasoning).assertDoesNotExist()
        rule.onNodeWithText("Thinking…").performClick()
        rule.mainClock.advanceTimeBy(FrameSettle)
        part = part.copy(state = MonarchAiPartState.Done, durationMs = 3_000L)
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(FrameSettle)
        rule.onNodeWithText("Thought for 3s").assertExists()
        rule.onNodeWithText(Reasoning).assertExists()
    }

    @Test
    fun aRestartOpensAgain() {
        var part by mutableStateOf(MonarchAiReasoningPart(text = Reasoning, durationMs = 2_000L))
        val changes = mutableListOf<Boolean>()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiReasoning(part = part, onExpandedChange = { changes += it }) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(Reasoning).assertDoesNotExist()
        part = part.copy(state = MonarchAiPartState.Streaming)
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(FrameSettle)
        rule.onNodeWithText(Reasoning).assertExists()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun redactedAndEmptyReasoningIsOneStillElement() {
        rule.setMonarchContent {
            Column {
                MonarchAiReasoning(part = MonarchAiReasoningPart(text = "secret", redacted = true))
                MonarchAiReasoning(part = MonarchAiReasoningPart(text = ""))
            }
        }
        rule.onNodeWithContentDescription("Assistant reasoning: Reasoning hidden").assertExists()
        rule.onNodeWithContentDescription("Assistant reasoning: Thought").assertExists()
        rule.onNodeWithText("secret").assertDoesNotExist()
    }

    @Test
    fun streamingWithoutTextIsTheThinkingIndicator() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = "", state = MonarchAiPartState.Streaming))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Thinking…", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsAndTheDurationCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiReasoning(
                part = MonarchAiReasoningPart(text = Reasoning, durationMs = 9_000L),
                labels = MonarchAiReasoningLabels(
                    prefix = "Razonamiento",
                    thoughtFor = { "Pensó durante $it" },
                    duration = { "${it / 1_000} segundos" },
                ),
            )
        }
        rule.onNodeWithText("Pensó durante 9 segundos").assertExists()
        rule.onNodeWithContentDescription("Razonamiento").assertExists()
    }

    @Test
    fun theChevronComesBeforeTheHeaderAndMirrorsRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiReasoning(
                part = MonarchAiReasoningPart(text = Reasoning, durationMs = 1_000L),
                expandIcon = { MonarchIcon(TestIcon, contentDescription = "chevron") },
            )
        }
        // As the web: the chevron is the first thing of the header, at its start edge.
        rule.onNodeWithContentDescription("chevron", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Thought for 1s", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theHeaderIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiReasoning(part = MonarchAiReasoningPart(text = Reasoning, durationMs = 75_000L))
        }
        rule.onNodeWithText("Thought for 1m 15s", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun durationsRoundToWholeSecondsAndAreAtLeastOne() {
        assertEquals("1s", tokens.duration(200L, Locale.US))
        assertEquals("12s", tokens.duration(12_400L, Locale.US))
        assertEquals("1m 5s", tokens.duration(65_000L, Locale.US))
        assertEquals("2m", tokens.duration(120_000L, Locale.US))
        val part = MonarchAiReasoningPart(text = "", durationMs = 5L)
        assertEquals(5L, tokens.shownDuration(part, 9L))
        assertEquals(9L, tokens.shownDuration(part.copy(durationMs = null), 9L))
    }

    @Test
    fun theDisclosureOpensAndClosesOnlyWhenStreamingChanges() {
        assertEquals(true, tokens.expandedAfter(wasStreaming = false, streaming = true, touched = true))
        assertEquals(false, tokens.expandedAfter(wasStreaming = true, streaming = false, touched = false))
        assertNull(tokens.expandedAfter(wasStreaming = true, streaming = false, touched = true))
        assertNull(tokens.expandedAfter(wasStreaming = true, streaming = true, touched = false))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, tokens.textColor(c), theme.id)
            assertEquals(c.aiStrong, tokens.markColor(c), theme.id)
        }
    }
}

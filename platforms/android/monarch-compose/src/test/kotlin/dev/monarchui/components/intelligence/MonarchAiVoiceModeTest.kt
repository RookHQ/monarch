package dev.monarchui.components.intelligence

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceMode
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeGlyph
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeLabels
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeRules
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeSize
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeState
import dev.monarchui.components.intelligence.aivoicemode.MonarchAiVoiceModeTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun message(id: String, role: MonarchAiRole, text: String, streaming: Boolean = false) =
    MonarchAiConversationMessage(
        id = id,
        role = role,
        parts = listOf(
            MonarchAiTextPart(text, if (streaming) MonarchAiPartState.Streaming else MonarchAiPartState.Done),
        ),
        createdAt = "2025-03-10T12:00:00.000Z",
    )

internal object MonarchAiVoiceModeFixtures : ComponentFixtures {
    override val id = "ai-voice-mode"
    override val fixtures = listOf(
        fixture("compact, speaking, every control") {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Speaking,
                level = 0.6f,
                agent = MonarchAiAgentRef(name = "Ada"),
                captionsVisible = false,
                elapsedSeconds = 65,
                size = MonarchAiVoiceModeSize.Compact,
                onMuteChange = {},
                onInterrupt = {},
                onEnd = {},
            )
        },
        fixture("compact, thinking, muted") {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Thinking,
                captionsVisible = false,
                muted = true,
                size = MonarchAiVoiceModeSize.Compact,
                onMuteChange = {},
            )
        },
        fixture("compact, ended with captions") {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Ended,
                captions = listOf(
                    message("1", MonarchAiRole.User, "What is on my calendar today?"),
                    message("2", MonarchAiRole.Assistant, "You have two meetings."),
                ),
                elapsedSeconds = 245,
                size = MonarchAiVoiceModeSize.Compact,
            )
        },
        fixture("compact, error with retry") {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Error,
                size = MonarchAiVoiceModeSize.Compact,
                onRetry = {},
            )
        },
        fixture("md, connecting") {
            MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Connecting, captionsVisible = false)
        },
        fixture("md, muted") {
            MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Muted, captionsVisible = false, onMuteChange = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiVoiceModeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun liveRegions(): List<String?> = rule.onAllNodes(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() }

    private fun show(
        state: () -> MonarchAiVoiceModeState,
        captions: () -> List<MonarchAiConversationMessage> = { emptyList() },
        onMuteChange: ((Boolean) -> Unit)? = null,
        onInterrupt: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null,
    ) {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(
                state = state(),
                modifier = Modifier.testTag("voice"),
                agent = MonarchAiAgentRef(name = "Ada"),
                captions = captions(),
                elapsedSeconds = 65,
                size = MonarchAiVoiceModeSize.Compact,
                onMuteChange = onMuteChange,
                onInterrupt = onInterrupt,
                onEnd = onEnd,
            )
        }
        frames()
    }

    /** Applies state written by the test, then moves the composition on by a few frames. */
    private fun frames() {
        rule.waitForIdle()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()
    }

    private fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction {
        performClick()
        frames()
        return this
    }

    @Test
    fun theSurfaceIsAGroupNamedAfterTheAssistant() {
        show(state = { MonarchAiVoiceModeState.Listening })
        rule.onNodeWithTag("voice")
            .assertContentDescriptionEquals("Voice conversation with Ada")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun theStateAndTheClockAreWritten() {
        show(state = { MonarchAiVoiceModeState.Thinking })
        rule.onNodeWithText("Thinking…", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Call time: 1:05").assertExists()
    }

    @Test
    fun aChangeOfStateIsAnnouncedButNotTheFirstNorTurnsIntoListeningOrSpeaking() {
        var state by mutableStateOf(MonarchAiVoiceModeState.Connecting)
        show(state = { state })
        assertTrue(liveRegions().all { it == null })
        state = MonarchAiVoiceModeState.Listening
        frames()
        assertTrue(liveRegions().all { it == null })
        state = MonarchAiVoiceModeState.Thinking
        frames()
        assertTrue("Thinking…" in liveRegions())
        state = MonarchAiVoiceModeState.Speaking
        frames()
        assertFalse("Thinking…" in liveRegions())
        state = MonarchAiVoiceModeState.Error
        frames()
        assertTrue("Something went wrong: The voice conversation stopped unexpectedly." in liveRegions())
    }

    @Test
    fun finalCaptionsAreAnnouncedOnceAndInterimOnesAreHidden() {
        var captions by mutableStateOf(listOf(message("1", MonarchAiRole.User, "Hello")))
        show(state = { MonarchAiVoiceModeState.Speaking }, captions = { captions })
        // Captions final on first appearance are not announced.
        assertFalse(liveRegions().any { it?.contains("Hello") == true })
        rule.onNodeWithContentDescription("You: Hello").assertExists()
        captions = captions + message("2", MonarchAiRole.Assistant, "You have two", streaming = true)
        frames()
        rule.onNodeWithContentDescription("Ada: You have two", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Ada\nYou have two", useUnmergedTree = true).assertExists()
        captions = captions.dropLast(1) + message("2", MonarchAiRole.Assistant, "You have two meetings.")
        frames()
        assertTrue("Ada: You have two meetings." in liveRegions())
        // The caption itself and the live region that announced it.
        rule.onAllNodesWithContentDescription("Ada: You have two meetings.").assertCountEquals(2)
    }

    @Test
    fun captionsShowAHintWhileLiveAndCanBeHidden() {
        show(state = { MonarchAiVoiceModeState.Listening })
        rule.onNodeWithText("Captions appear here as you talk.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Captions").assertIsOn().assertTouchTargetAtLeast48dp(rule.density).click()
        rule.onNodeWithText("Captions").assertIsOff()
        rule.onNodeWithText("Captions appear here as you talk.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun hoistedCaptionsVisibilityCallsBack() {
        var asked: Boolean? = null
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Listening,
                captionsVisible = false,
                onCaptionsVisibleChange = { asked = it },
            )
        }
        frames()
        rule.onNodeWithText("Captions").assertIsOff().click()
        assertEquals(true, asked)
        rule.onNodeWithText("Captions").assertIsOff()
    }

    @Test
    fun muteIsAToggleThatAsksForTheOtherValue() {
        var asked: Boolean? = null
        var state by mutableStateOf(MonarchAiVoiceModeState.Listening)
        show(state = { state }, onMuteChange = { asked = it })
        rule.onNodeWithText("Mute").assertIsOff().assertTouchTargetAtLeast48dp(rule.density).click()
        assertEquals(true, asked)
        state = MonarchAiVoiceModeState.Muted
        frames()
        rule.onNodeWithText("Mute").assertIsOn()
    }

    @Test
    fun aMicrophoneMutedWhileTheAssistantSpeaksIsWritten() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Speaking, muted = true, onMuteChange = {})
        }
        frames()
        rule.onNodeWithText("Your microphone is muted", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Mute").assertIsOn()
    }

    @Test
    fun interruptAndEndShowOnlyWhenTheyApply() {
        var interrupts = 0
        var ends = 0
        var state by mutableStateOf(MonarchAiVoiceModeState.Speaking)
        show(state = { state }, onInterrupt = { interrupts++ }, onEnd = { ends++ })
        rule.onNodeWithText("Interrupt").click()
        rule.onNodeWithText("End").assertTouchTargetAtLeast48dp(rule.density).click()
        assertEquals(1, interrupts)
        assertEquals(1, ends)
        state = MonarchAiVoiceModeState.Listening
        frames()
        rule.onNodeWithText("Interrupt").assertDoesNotExist()
        state = MonarchAiVoiceModeState.Ended
        frames()
        rule.onNodeWithText("End").assertDoesNotExist()
        rule.onNodeWithText("Captions").assertDoesNotExist()
        rule.onNodeWithText("Lasted 1:05", useUnmergedTree = true).assertExists()
    }

    @Test
    fun focusMovesToMuteWhenInterruptGoesAway() {
        var state by mutableStateOf(MonarchAiVoiceModeState.Speaking)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            // Buttons take focus from a keyboard, as with a hardware keyboard or Switch Access.
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            MonarchAiVoiceMode(
                state = state,
                size = MonarchAiVoiceModeSize.Compact,
                captionsVisible = false,
                onMuteChange = {},
                onInterrupt = {},
            )
        }
        frames()
        rule.onNodeWithText("Interrupt").requestFocus()
        frames()
        rule.onNodeWithText("Interrupt").assertIsFocused()
        state = MonarchAiVoiceModeState.Listening
        frames()
        rule.onNodeWithText("Mute").assertIsFocused()
    }

    @Test
    fun anEndedCallOffersStartAgain() {
        var restarts = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Ended, onRestart = { restarts++ })
        }
        frames()
        rule.onNodeWithText("Start again").assertTouchTargetAtLeast48dp(rule.density).click()
        assertEquals(1, restarts)
    }

    @Test
    fun aFailedCallShowsItsErrorAndOffersRetry() {
        var retries = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Error,
                error = "The line dropped.",
                onRetry = { retries++ },
            )
        }
        frames()
        rule.onNodeWithText("The line dropped.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Retry").click()
        assertEquals(1, retries)
    }

    @Test
    fun everyStringCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Listening,
                modifier = Modifier.testTag("voice"),
                elapsedSeconds = 5,
                labels = MonarchAiVoiceModeLabels(
                    label = { "Call with $it" },
                    assistant = "Helper",
                    states = mapOf(MonarchAiVoiceModeState.Listening to "Go ahead"),
                    captions = "Subtitles",
                    elapsed = "Duration",
                ),
                onMuteChange = {},
            )
        }
        frames()
        rule.onNodeWithTag("voice").assertContentDescriptionEquals("Call with Helper")
        rule.onNodeWithText("Go ahead", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Subtitles").assertExists()
        rule.onNodeWithContentDescription("Duration: 0:05").assertExists()
    }

    @Test
    fun theControlsFollowTheLayoutDirection() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Listening, captionsVisible = false, onMuteChange = {})
        }
        frames()
        rule.onNodeWithText("Mute", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Captions", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theStateTextWrapsAtTheLargestFontScale() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiVoiceMode(
                state = MonarchAiVoiceModeState.Error,
                error = "The connection to the assistant was lost and could not be restored.",
                size = MonarchAiVoiceModeSize.Compact,
            )
        }
        frames()
        rule.onNodeWithText("The connection to the assistant", substring = true, useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theRulesFollowTheWebAndIos() {
        val tokens = MonarchAiVoiceModeRules
        assertEquals("4:05", tokens.duration(245, Locale.US))
        assertEquals("1:02:09", tokens.duration(3_729, Locale.US))
        assertEquals("0:00", tokens.duration(-5, Locale.US))
        assertEquals(0f, tokens.shownLevel(0.7f, MonarchAiVoiceModeState.Thinking, reducedMotion = false))
        assertEquals(0f, tokens.shownLevel(0.7f, MonarchAiVoiceModeState.Speaking, reducedMotion = true))
        assertEquals(1f, tokens.shownLevel(3f, MonarchAiVoiceModeState.Speaking, reducedMotion = false))
        assertEquals(0f, tokens.shownLevel(Float.NaN, MonarchAiVoiceModeState.Listening, reducedMotion = false))
        assertEquals(MonarchAiVoiceModeGlyph.Dots, tokens.glyph(MonarchAiVoiceModeState.Reconnecting))
        assertEquals(MonarchAiVoiceModeGlyph.Bars, tokens.glyph(MonarchAiVoiceModeState.Listening))
        assertEquals(MonarchAiVoiceModeGlyph.Error, tokens.glyph(MonarchAiVoiceModeState.Error))
        assertEquals(MonarchAiVoiceModeGlyph.Rest, tokens.glyph(MonarchAiVoiceModeState.Ended))
        assertFalse(tokens.muted(MonarchAiVoiceModeState.Ended, muted = true))
        assertTrue(tokens.muted(MonarchAiVoiceModeState.Muted, muted = null))
        assertFalse(tokens.canInterrupt(MonarchAiVoiceModeState.Speaking, hasHandler = false))
        assertFalse(tokens.announces(null, MonarchAiVoiceModeState.Thinking))
        assertFalse(tokens.announces(MonarchAiVoiceModeState.Thinking, MonarchAiVoiceModeState.Speaking))
        assertTrue(tokens.announces(MonarchAiVoiceModeState.Speaking, MonarchAiVoiceModeState.Ended))
        assertEquals(1.7f, MonarchAiVoiceModeTokens.haloScale(1f), 0.0001f)
        assertEquals(listOf(0.125f, 0.2f, 0.25f, 0.2f, 0.125f), MonarchAiVoiceModeTokens.barHeights(0f))
    }

    @Test
    fun captionsLeaveOutOtherRolesAndEmptyMessages() {
        val captions = MonarchAiVoiceModeRules.captions(
            listOf(
                message("1", MonarchAiRole.System, "Be brief"),
                message("2", MonarchAiRole.User, "  "),
                message("3", MonarchAiRole.User, " Hi "),
                message("4", MonarchAiRole.Assistant, "Hello", streaming = true),
            ),
            agentName = "Ada",
            you = "You",
        )
        assertEquals(listOf("3", "4"), captions.map { it.id })
        assertEquals("Hi", captions[0].text)
        assertFalse(captions[1].final)
        assertEquals(listOf(captions[0]), MonarchAiVoiceModeRules.unannounced(captions, emptySet()))
    }

    @Test
    fun theVisualUsesTheAssistantDangerAndNeutralRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiVoiceModeTokens
            val live = tokens.visualColors(MonarchAiVoiceModeState.Speaking, colors)
            assertEquals(colors.aiSubtle, live.orb, theme.id)
            assertEquals(colors.aiSolid, live.glyph, theme.id)
            assertEquals(colors.aiBorder, live.ring, theme.id)
            assertEquals(colors.aiSubtle, live.halo, theme.id)
            assertEquals(colors.dangerSolid, tokens.visualColors(MonarchAiVoiceModeState.Error, colors).orb, theme.id)
            assertEquals(colors.bgEmphasis, tokens.visualColors(MonarchAiVoiceModeState.Muted, colors).orb, theme.id)
            assertEquals(colors.aiStrong, tokens.speakerColor(user = false, colors), theme.id)
            assertEquals(colors.textMuted, tokens.speakerColor(user = true, colors), theme.id)
        }
    }

    @Test
    fun theOwnCaptionsStateSurvivesRecomposition() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            var state by remember { mutableStateOf(MonarchAiVoiceModeState.Listening) }
            MonarchAiVoiceMode(state = state, onMuteChange = { state = MonarchAiVoiceModeState.Muted })
        }
        frames()
        rule.onNodeWithText("Captions").click()
        rule.onNodeWithText("Mute").click()
        rule.onNodeWithText("Captions").assertIsOff()
    }
}

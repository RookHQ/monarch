@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import android.Manifest
import android.app.Application
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiSpeechError
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiSpeechEvent
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiSpeechException
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiSpeechRecognizer
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiVoiceInput
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiVoiceInputLabels
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiVoiceInputMode
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiVoiceInputState
import com.rookhq.monarch.components.intelligence.aivoiceinput.MonarchAiVoiceInputTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/** A recogniser the test drives by hand: it plays what the test sends, in order. */
private class ScriptedRecognizer(
    private val available: Boolean = true,
    override val requiredPermissions: List<String> = emptyList(),
) : MonarchAiSpeechRecognizer {
    private val script = Channel<Any>(Channel.UNLIMITED)
    var starts = 0
    var stops = 0
    var cancelled = 0
    var continuous: Boolean? = null
    var locale: Locale? = null

    fun send(event: MonarchAiSpeechEvent) {
        script.trySend(event)
    }

    fun fail(error: MonarchAiSpeechError) {
        script.trySend(error)
    }

    override fun isAvailable(locale: Locale?): Boolean = available

    override fun start(locale: Locale?, continuous: Boolean): Flow<MonarchAiSpeechEvent> = flow {
        starts++
        this@ScriptedRecognizer.continuous = continuous
        this@ScriptedRecognizer.locale = locale
        var finished = false
        try {
            while (true) {
                when (val next = script.receive()) {
                    is MonarchAiSpeechEvent -> emit(next)
                    is MonarchAiSpeechError -> throw MonarchAiSpeechException(next)
                    else -> break
                }
            }
            finished = true
        } finally {
            if (!finished) cancelled++
        }
    }

    override fun stop() {
        stops++
        script.trySend(Done)
    }

    private object Done
}

internal object MonarchAiVoiceInputFixtures : ComponentFixtures {
    override val id = "ai-voice-input"
    override val fixtures = listOf(
        fixture("toggle") { MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = ScriptedRecognizer()) },
        fixture("push to talk, small, with an icon") {
            MonarchAiVoiceInput(
                onTranscript = { _, _ -> },
                recognizer = ScriptedRecognizer(),
                mode = MonarchAiVoiceInputMode.PushToTalk,
                size = MonarchButtonSize.Sm,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("disabled, large") {
            MonarchAiVoiceInput(
                onTranscript = { _, _ -> },
                recognizer = ScriptedRecognizer(),
                size = MonarchButtonSize.Lg,
                enabled = false,
            )
        },
        fixture("long label") {
            MonarchAiVoiceInput(
                onTranscript = { _, _ -> },
                recognizer = ScriptedRecognizer(),
                labels = MonarchAiVoiceInputLabels(dictate = "Dictate your message to the assistant"),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiVoiceInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val transcripts = mutableListOf<Pair<String, Boolean>>()
    private val states = mutableListOf<MonarchAiVoiceInputState>()

    private fun show(
        recognizer: ScriptedRecognizer,
        mode: MonarchAiVoiceInputMode = MonarchAiVoiceInputMode.Toggle,
        enabled: Boolean = true,
        labels: MonarchAiVoiceInputLabels = MonarchAiVoiceInputLabels(),
        locale: Locale? = null,
    ) {
        rule.setMonarchContent {
            MonarchAiVoiceInput(
                onTranscript = { text, final -> transcripts += text to final },
                modifier = Modifier.testTag("voice"),
                recognizer = recognizer,
                locale = locale,
                mode = mode,
                enabled = enabled,
                labels = labels,
                onStateChange = { states += it },
            )
        }
    }

    private fun liveRegion() = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        useUnmergedTree = true,
    )

    @Test
    fun theToggleStartsListeningAndReadsAsOn() {
        val recognizer = ScriptedRecognizer()
        show(recognizer, locale = Locale.FRANCE)
        rule.onNodeWithText("Dictate").assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Starting microphone…", useUnmergedTree = true).assertExists()
        assertEquals(1, recognizer.starts)
        assertEquals(true, recognizer.continuous)
        assertEquals(Locale.FRANCE, recognizer.locale)
        recognizer.send(MonarchAiSpeechEvent.Started)
        rule.waitForIdle()
        rule.onNodeWithText("Dictate").assertIsOn()
        rule.onNodeWithText("Listening…", useUnmergedTree = true).assertExists()
        liveRegion().assertContentDescriptionEquals("Listening…")
        assertEquals(listOf(MonarchAiVoiceInputState.Starting, MonarchAiVoiceInputState.Listening), states)
    }

    @Test
    fun guessesAndPhrasesAreHandedBackAndShownAsTheTranscript() {
        val recognizer = ScriptedRecognizer()
        show(recognizer)
        rule.onNodeWithText("Dictate").performClick()
        recognizer.send(MonarchAiSpeechEvent.Partial("book a"))
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Transcript: book a").assertExists()
        recognizer.send(MonarchAiSpeechEvent.Final(" book a table "))
        recognizer.send(MonarchAiSpeechEvent.Partial("for two"))
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Transcript: book a table for two").assertExists()
        assertEquals(listOf("book a" to false, "book a table" to true, "for two" to false), transcripts)
    }

    @Test
    fun pressingAgainStopsAndAnnouncesIt() {
        val recognizer = ScriptedRecognizer()
        show(recognizer)
        rule.onNodeWithText("Dictate").performClick()
        recognizer.send(MonarchAiSpeechEvent.Started)
        rule.waitForIdle()
        rule.onNodeWithText("Dictate").performClick()
        rule.waitForIdle()
        assertEquals(1, recognizer.stops)
        rule.onNodeWithText("Dictate").assertIsOff()
        liveRegion().assertContentDescriptionEquals("Stopped listening.")
        assertEquals(MonarchAiVoiceInputState.Idle, states.last())
        rule.onNodeWithText("Listening…", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aRefusedPermissionShowsGuidanceWithAnOpenSettingsAction() {
        val recognizer = ScriptedRecognizer()
        show(recognizer)
        rule.onNodeWithText("Dictate").performClick()
        recognizer.fail(MonarchAiSpeechError.Denied)
        rule.waitForIdle()
        rule.onNodeWithText("Microphone access is off", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Open settings").assertTouchTargetAtLeast48dp(rule.density)
        assertEquals(MonarchAiVoiceInputState.Denied, states.last())
    }

    @Test
    fun otherFailuresShowTheirMessageWithoutTheSettingsAction() {
        val recognizer = ScriptedRecognizer()
        show(recognizer, labels = MonarchAiVoiceInputLabels(noSpeech = "Nothing heard."))
        rule.onNodeWithText("Dictate").performClick()
        recognizer.fail(MonarchAiSpeechError.NoSpeech)
        rule.waitForIdle()
        rule.onNodeWithText("Nothing heard.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Open settings").assertDoesNotExist()
        assertEquals(MonarchAiVoiceInputState.Error, states.last())
        // A new session clears the failure.
        rule.onNodeWithText("Dictate").performClick()
        rule.onNodeWithText("Nothing heard.", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(2, recognizer.starts)
    }

    @Test
    fun nothingIsDrawnWhenRecognitionIsNotAvailable() {
        show(ScriptedRecognizer(available = false))
        rule.onNodeWithText("Dictate").assertDoesNotExist()
    }

    @Test
    fun pushToTalkListensWhileHeld() {
        val recognizer = ScriptedRecognizer()
        show(recognizer, mode = MonarchAiVoiceInputMode.PushToTalk)
        val button = rule.onNodeWithText("Hold to talk").assertIsOff().assertTouchTargetAtLeast48dp(rule.density)
        button.performTouchInput { down(center) }
        rule.waitForIdle()
        assertEquals(1, recognizer.starts)
        assertEquals(false, recognizer.continuous)
        button.performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(1, recognizer.stops)
    }

    @Test
    fun pushToTalkStartsAndStopsWithTheClickActionForTalkBack() {
        val recognizer = ScriptedRecognizer()
        show(recognizer, mode = MonarchAiVoiceInputMode.PushToTalk)
        val button = rule.onNodeWithText("Hold to talk")
        button.assert(
            SemanticsMatcher("click action is labelled") {
                it.config[SemanticsActions.OnClick].label == "start or stop listening"
            },
        )
        button.performSemanticsAction(SemanticsActions.OnClick)
        recognizer.send(MonarchAiSpeechEvent.Started)
        rule.waitForIdle()
        rule.onNodeWithText("Hold to talk").assertIsOn()
        rule.onNodeWithText("Hold to talk").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(1, recognizer.stops)
        rule.onNodeWithText("Hold to talk").assertIsOff()
    }

    @Test
    fun aDisabledButtonDoesNotStart() {
        val recognizer = ScriptedRecognizer()
        show(recognizer, enabled = false)
        rule.onNodeWithText("Dictate").assertIsNotEnabled().performClick()
        assertEquals(0, recognizer.starts)
    }

    @Test
    fun disablingStopsARunningSession() {
        val recognizer = ScriptedRecognizer()
        var enabled by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = recognizer, enabled = enabled)
        }
        rule.onNodeWithText("Dictate").performClick()
        enabled = false
        rule.waitForIdle()
        assertEquals(1, recognizer.stops)
    }

    @Test
    fun leavingTheCompositionCancelsTheSession() {
        val recognizer = ScriptedRecognizer()
        var shown by mutableStateOf(true)
        rule.setMonarchContent {
            if (shown) MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = recognizer)
        }
        rule.onNodeWithText("Dictate").performClick()
        recognizer.send(MonarchAiSpeechEvent.Started)
        rule.waitForIdle()
        shown = false
        rule.waitForIdle()
        assertEquals(1, recognizer.cancelled)
    }

    @Test
    fun aMissingPermissionIsAskedForBeforeListening() {
        val recognizer = ScriptedRecognizer(requiredPermissions = listOf(Manifest.permission.RECORD_AUDIO))
        show(recognizer)
        rule.onNodeWithText("Dictate").performClick()
        rule.onNodeWithText("Starting microphone…", useUnmergedTree = true).assertExists()
        assertEquals(0, recognizer.starts)
        // Stopping while the request is open ends the session at once.
        rule.onNodeWithText("Dictate").performClick()
        rule.onNodeWithText("Dictate").assertIsOff()
        assertEquals(MonarchAiVoiceInputState.Idle, states.last())
    }

    @Test
    fun aGrantedPermissionStartsAtOnce() {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.RECORD_AUDIO)
        val recognizer = ScriptedRecognizer(requiredPermissions = listOf(Manifest.permission.RECORD_AUDIO))
        show(recognizer)
        rule.onNodeWithText("Dictate").performClick()
        assertEquals(1, recognizer.starts)
    }

    @Test
    fun theStatusFollowsTheButtonInTheLayoutDirection() {
        val recognizer = ScriptedRecognizer()
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = recognizer, showLevel = false)
        }
        rule.onNodeWithText("Dictate").performClick()
        rule.onNodeWithText("Dictate", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Starting microphone…", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theFailureMessageWrapsAtTheLargestFontScale() {
        val recognizer = ScriptedRecognizer()
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column { MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = recognizer) }
        }
        rule.onNodeWithText("Dictate").performClick()
        recognizer.fail(MonarchAiSpeechError.Unavailable)
        rule.waitForIdle()
        rule.onNodeWithText("Dictation is not available", substring = true, useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theRulesMapFailuresLevelsAndTranscripts() {
        val tokens = MonarchAiVoiceInputTokens
        assertEquals(MonarchAiSpeechError.Denied, tokens.speechError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(MonarchAiSpeechError.NoSpeech, tokens.speechError(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(MonarchAiSpeechError.NoMicrophone, tokens.speechError(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(MonarchAiSpeechError.Unavailable, tokens.speechError(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(MonarchAiSpeechError.Unavailable, tokens.speechError(12))
        assertEquals(MonarchAiSpeechError.Failed, tokens.speechError(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(0f, tokens.levelOfRms(-10f))
        assertEquals(0.5f, tokens.levelOfRms(4f))
        assertEquals(1f, tokens.levelOfRms(30f))
        assertEquals(0f, tokens.clampedLevel(Float.NaN))
        assertEquals("book a table", tokens.preview("book a", " table "))
        assertEquals("book", tokens.preview("", "book"))
        assertEquals(MonarchTone.Info, tokens.tone(MonarchAiSpeechError.NoSpeech))
        assertEquals(MonarchTone.Danger, tokens.tone(MonarchAiSpeechError.Failed))
        assertTrue(tokens.offersSettings(MonarchAiSpeechError.Denied))
        assertFalse(tokens.offersSettings(MonarchAiSpeechError.NoMicrophone))
        assertEquals(MonarchButtonVariant.Primary, tokens.buttonVariant(active = true))
        assertEquals(MonarchButtonVariant.Secondary, tokens.buttonVariant(active = false))
    }
}

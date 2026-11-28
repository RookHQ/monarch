@file:OptIn(ExperimentalTestApi::class)

package dev.monarchui.components.intelligence

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import dev.monarchui.components.intelligence.aipromptinput.AiPromptInputKey
import dev.monarchui.components.intelligence.aipromptinput.AiPromptInputKeyAction
import dev.monarchui.components.intelligence.aipromptinput.AiPromptInputLimit
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInput
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInputLabels
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInputRules
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInputSubmitOn
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInputTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val PromptFiles = listOf(
    MonarchAiAttachmentTrayFile("f1", "brief.pdf", sizeBytes = 120_000, contentType = "application/pdf"),
    MonarchAiAttachmentTrayFile("f2", "photo.png", contentType = "image/png", progress = 0.4f),
)

internal object MonarchAiPromptInputFixtures : ComponentFixtures {
    override val id = "ai-prompt-input"
    override val fixtures = listOf(
        fixture("empty with attach") {
            MonarchAiPromptInput(
                label = "Message the assistant",
                state = rememberTextFieldState(),
                onSubmit = {},
                onAttach = {},
            )
        },
        fixture("text with label, counter and tokens") {
            MonarchAiPromptInput(
                label = "Message the assistant",
                state = rememberTextFieldState("Summarize this thread"),
                onSubmit = {},
                showLabel = true,
                maxLength = 4000,
                countTokens = { it.length / 4 },
            )
        },
        fixture("near the limit") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("123456789"),
                onSubmit = {},
                maxLength = 10,
            )
        },
        fixture("over the limit") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("Too long"),
                onSubmit = {},
                maxLength = 5,
            )
        },
        fixture("busy with stop") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                busy = true,
                onStop = {},
            )
        },
        fixture("disabled with reason") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                enabled = false,
                disabledReason = "Connect a provider to start.",
                onAttach = {},
            )
        },
        fixture("attachments and icons") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                attachments = PromptFiles,
                onRemoveAttachment = {},
                onAttach = {},
                attachIcon = { Icon(TestIcon, contentDescription = null) },
                sendIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("toolbar, menu and footer") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("/sum"),
                onSubmit = {},
                triggerMenu = { value, _ -> MonarchText(text = "Commands for ${value.text}") },
                toolbar = { MonarchButton(text = "Model", onClick = {}) },
                footer = { MonarchText(text = "Answers can be wrong.") },
            )
        },
        fixture("long placeholder") {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                placeholder = "Ask about this customer, their open tickets or their last three invoices",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiPromptInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Prompt"))

    @Test
    fun theFocusRequesterFocusesTheTextFieldEvenWithAnAttachmentButtonFirst() {
        val requester = FocusRequester()
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                onAttach = {},
                focusRequester = requester,
            )
        }
        rule.onNode(field).assertIsNotFocused()
        rule.runOnIdle { requester.requestFocus() }
        rule.onNode(field).assertIsFocused()
    }

    @Test
    fun sendSubmitsTheTextAndClearsTheField() {
        val state = TextFieldState()
        val sent = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiPromptInput(label = "Prompt", state = state, onSubmit = { sent += it }) }
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNode(field).performTextInput("Summarize this")
        rule.onNodeWithText("Send").assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Summarize this"), sent)
        assertEquals("", state.text.toString())
    }

    @Test
    fun theTextComesBackWhenSubmitThrows() {
        val state = TextFieldState("Hello")
        rule.setMonarchContent {
            MonarchAiPromptInput(label = "Prompt", state = state, onSubmit = { error("offline") })
        }
        rule.onNodeWithText("Send").performClick()
        rule.waitForIdle()
        assertEquals("Hello", state.text.toString())
        rule.onNodeWithText("Send").assertIsEnabled()
    }

    @Test
    fun blankTextCannotBeSentButAttachmentsCan() {
        val sent = mutableListOf<String>()
        val removed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("   "),
                onSubmit = { sent += it },
                attachments = PromptFiles.take(1),
                onRemoveAttachment = { removed += it },
            )
        }
        rule.onNodeWithText("brief.pdf", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Send").assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(listOf("   "), sent)
        rule.onNodeWithContentDescription("Remove", substring = true).performClick()
        assertEquals(listOf("f1"), removed)
    }

    @Test
    fun whileBusySendIsStopAndStopCallsBack() {
        var stopped = 0
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("Next question"),
                onSubmit = {},
                busy = true,
                onStop = { stopped++ },
            )
        }
        rule.onAllNodesWithText("Send").assertCountEquals(0)
        rule.onNodeWithText("Stop").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, stopped)
    }

    @Test
    fun withoutOnStopTheStopButtonIsDisabled() {
        rule.setMonarchContent {
            MonarchAiPromptInput(label = "Prompt", state = rememberTextFieldState(), onSubmit = {}, busy = true)
        }
        rule.onNodeWithText("Stop").assertIsNotEnabled()
    }

    @Test
    fun hardwareEnterSendsShiftEnterDoesNotAndEscapeStops() {
        val sent = mutableListOf<String>()
        var stopped = 0
        val state = TextFieldState("Hi")
        rule.setMonarchContent {
            MonarchAiPromptInput(label = "Prompt", state = state, onSubmit = { sent += it }, onStop = { stopped++ })
        }
        rule.onNode(field).requestFocus()
        rule.onNode(field).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        rule.waitForIdle()
        assertTrue(sent.isEmpty())
        assertEquals("Hi\n", state.text.toString())
        rule.onNode(field).performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(listOf("Hi\n"), sent)
        assertEquals("", state.text.toString())
        rule.onNode(field).performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, stopped)
    }

    @Test
    fun escapeStopsWhileBusy() {
        var stopped = 0
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                busy = true,
                onStop = { stopped++ },
            )
        }
        rule.onNode(field).requestFocus()
        rule.onNode(field).performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals(1, stopped)
    }

    @Test
    fun theKeyRulesFollowTheSetting() {
        fun press(key: Key, shift: Boolean = false, command: Boolean = false, hardware: Boolean = true) =
            AiPromptInputKey(key, shift, command, hardware)
        val t = MonarchAiPromptInputRules
        val enter = MonarchAiPromptInputSubmitOn.Enter
        val ctrl = MonarchAiPromptInputSubmitOn.CtrlEnter
        assertEquals(AiPromptInputKeyAction.Submit, t.keyAction(press(Key.Enter), enter, busy = false, canStop = false))
        assertEquals(AiPromptInputKeyAction.Submit, t.keyAction(press(Key.NumPadEnter), enter, false, false))
        assertEquals(AiPromptInputKeyAction.None, t.keyAction(press(Key.Enter, shift = true), enter, false, false))
        assertEquals(AiPromptInputKeyAction.None, t.keyAction(press(Key.Enter, hardware = false), enter, false, false))
        assertEquals(AiPromptInputKeyAction.None, t.keyAction(press(Key.Enter), ctrl, false, false))
        assertEquals(AiPromptInputKeyAction.Submit, t.keyAction(press(Key.Enter, command = true), ctrl, false, false))
        assertEquals(AiPromptInputKeyAction.Submit, t.keyAction(press(Key.Enter, command = true), enter, false, false))
        assertEquals(AiPromptInputKeyAction.Stop, t.keyAction(press(Key.Escape), enter, busy = true, canStop = true))
        assertEquals(AiPromptInputKeyAction.None, t.keyAction(press(Key.Escape), enter, busy = true, canStop = false))
        assertEquals(AiPromptInputKeyAction.None, t.keyAction(press(Key.Escape), enter, busy = false, canStop = true))
    }

    @Test
    fun theCounterReadsInWordsAndOverTheLimitSendingIsOffWithAnError() {
        val state = TextFieldState("Hi")
        rule.setMonarchContent { MonarchAiPromptInput(label = "Prompt", state = state, onSubmit = {}, maxLength = 5) }
        rule.onNodeWithContentDescription("2 of 5 characters").assertExists()
        rule.onNode(field).performTextInput(" there")
        rule.onNodeWithContentDescription("8 of 5 characters").assertExists()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        val error = rule.onNode(field).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)
        assertEquals("3 characters over the limit", error)
        val region = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        val over = listOf("3 characters over the limit")
        region.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, over))
    }

    @Test
    fun reachingTheWarningShareIsAnnounced() {
        val state = TextFieldState("12345678")
        rule.setMonarchContent { MonarchAiPromptInput(label = "Prompt", state = state, onSubmit = {}, maxLength = 10) }
        rule.onNode(field).performTextInput("9")
        rule.waitForIdle()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("1 character left")))
    }

    @Test
    fun theLimitRulesHold() {
        val t = MonarchAiPromptInputTokens
        assertEquals(AiPromptInputLimit.Ok, t.limit(500, null, 0.9f))
        assertEquals(AiPromptInputLimit.Ok, t.limit(8, 10, 0.9f))
        assertEquals(AiPromptInputLimit.Warning, t.limit(9, 10, 0.9f))
        assertEquals(AiPromptInputLimit.Warning, t.limit(10, 10, 0.9f))
        assertEquals(AiPromptInputLimit.Over, t.limit(11, 10, 0.9f))
        val ok = AiPromptInputLimit.Ok
        val warn = AiPromptInputLimit.Warning
        val over = AiPromptInputLimit.Over
        assertEquals("left 1", t.limitAnnouncement(ok, warn, 9, 10, { "left $it" }, { "over $it" }))
        assertEquals("over 2", t.limitAnnouncement(warn, over, 12, 10, { "left $it" }, { "over $it" }))
        assertNull(t.limitAnnouncement(over, warn, 10, 10, { "left $it" }, { "over $it" }))
        assertNull(t.limitAnnouncement(warn, ok, 3, 10, { "left $it" }, { "over $it" }))
        assertNull(t.limitAnnouncement(ok, over, 12, null, { "left $it" }, { "over $it" }))
        fun can(
            text: String = "Hi",
            enabled: Boolean = true,
            busy: Boolean = false,
            submitting: Boolean = false,
            limit: AiPromptInputLimit = ok,
            files: Boolean = false,
        ) = t.canSubmit(text, enabled, busy, submitting, limit, files)
        assertTrue(can())
        assertFalse(can(text = " "))
        assertTrue(can(text = "", files = true))
        assertFalse(can(enabled = false))
        assertFalse(can(busy = true))
        assertFalse(can(submitting = true))
        assertFalse(can(limit = over))
    }

    @Test
    fun receivedFilesAreFilteredByType() {
        val t = MonarchAiPromptInputRules
        val photo = Uri.parse("content://media/photo/1")
        val doc = Uri.parse("content://docs/2")
        val image = ClipData(ClipDescription("photo", arrayOf("image/png")), ClipData.Item(photo))
        val pdf = ClipData(ClipDescription("doc", arrayOf("application/pdf")), ClipData.Item(doc))
        val text = ClipData.newPlainText("note", "hello")
        assertEquals(listOf(photo), t.acceptedUris(image, MonarchAiPromptInputTokens.DefaultAccept))
        assertEquals(emptyList(), t.acceptedUris(pdf, MonarchAiPromptInputTokens.DefaultAccept))
        assertEquals(listOf(doc), t.acceptedUris(pdf, listOf("application/pdf")))
        assertEquals(emptyList(), t.acceptedUris(text, listOf("image/*")))
        assertTrue(t.accepts(listOf("image/jpeg"), listOf("*/*")))
        assertFalse(t.accepts(listOf("text/plain"), listOf("image/*")))
    }

    @Test
    fun theTriggerMenuReadsAndReplacesTheText() {
        val state = TextFieldState("Please /sum")
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = state,
                onSubmit = {},
                triggerMenu = { value, onValueChange ->
                    MonarchButton(
                        text = "Pick ${value.text.substringAfterLast(' ')}",
                        onClick = { onValueChange(TextFieldValue("Please /summarize ", TextRange(18))) },
                    )
                },
            )
        }
        rule.onNodeWithText("Pick /sum").performClick()
        assertEquals("Please /summarize ", state.text.toString())
        assertEquals(TextRange(18), state.selection)
    }

    @Test
    fun disabledItCannotBeEditedOrSentAndSaysWhy() {
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState("Hi"),
                onSubmit = {},
                enabled = false,
                disabledReason = "Connect a provider to start.",
                onAttach = {},
            )
        }
        rule.onNode(field).assertIsNotEnabled()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNodeWithText("Attach files").assertIsNotEnabled()
        rule.onNodeWithText("Connect a provider to start.").assertExists()
    }

    @Test
    fun attachIsNamedAndCallsTheAppPicker() {
        var attached = 0
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                onAttach = { attached++ },
                attachIcon = { Icon(TestIcon, contentDescription = null) },
                sendIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Attach files").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, attached)
        rule.onNodeWithContentDescription("Send").assertTouchTargetAtLeast48dp(rule.density).assertIsNotEnabled()
    }

    @Test
    fun theTokenEstimateAndBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                countTokens = { 12 },
                labels = MonarchAiPromptInputLabels(
                    placeholder = "Pregunta algo",
                    send = "Enviar",
                    tokens = { "$it fichas" },
                ),
            )
        }
        rule.onNodeWithText("Pregunta algo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Enviar").assertExists()
        rule.onNodeWithText("12 fichas").assertExists()
    }

    @Test
    fun theDefaultsAreTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiPromptInput(label = "Prompt", state = rememberTextFieldState(), onSubmit = {}, countTokens = { 1 })
        }
        rule.onNodeWithText("Send a message…", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("≈ 1 token").assertExists()
    }

    @Test
    fun attachComesBeforeSendInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAiPromptInput(label = "Prompt", state = rememberTextFieldState(), onSubmit = {}, onAttach = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Attach files").assertIsBefore(rule.onNodeWithText("Send"), direction)
        }
    }

    @Test
    fun theTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiPromptInput(
                label = "Prompt",
                state = rememberTextFieldState(),
                onSubmit = {},
                showLabel = true,
                disabledReason = "Connect a provider to start using the assistant.",
                enabled = false,
            )
        }
        rule.onNodeWithText("Connect a provider to start using the assistant.").assertTextFullyVisible()
        rule.onNodeWithText("Prompt").assertTextFullyVisible()
    }

    @Test
    fun theColoursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchAiPromptInputTokens
            assertEquals(colors.borderInput, t.boxColors(true, over = false, dragging = false, colors).border, theme.id)
            assertEquals(colors.dangerBorder, t.boxColors(true, over = true, dragging = false, colors).border, theme.id)
            assertEquals(colors.accentBorder, t.boxColors(true, over = true, dragging = true, colors).border, theme.id)
            val disabled = t.boxColors(false, over = false, dragging = false, colors)
            assertEquals(colors.bgSubtle, disabled.container, theme.id)
            assertEquals(colors.textMuted, t.counterColor(AiPromptInputLimit.Ok, colors), theme.id)
            assertEquals(colors.warningStrong, t.counterColor(AiPromptInputLimit.Warning, colors), theme.id)
            assertEquals(colors.dangerStrong, t.counterColor(AiPromptInputLimit.Over, colors), theme.id)
            assertEquals(colors.textDisabled, t.textColor(false, colors), theme.id)
            assertEquals(colors.textSubtle, t.placeholderColor(colors), theme.id)
            assertEquals(colors.accentStrong, t.dropHintColor(colors), theme.id)
        }
    }
}

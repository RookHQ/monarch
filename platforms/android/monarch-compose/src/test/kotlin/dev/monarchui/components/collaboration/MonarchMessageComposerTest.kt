@file:OptIn(ExperimentalMonarchApi::class, ExperimentalTestApi::class)

package dev.monarchui.components.collaboration

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.collaboration.messagecomposer.MessageComposerKey
import dev.monarchui.components.collaboration.messagecomposer.MessageComposerKeyAction
import dev.monarchui.components.collaboration.messagecomposer.MessageComposerPicked
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposer
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerKeys
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerLabels
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerMentions
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerMessage
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerMode
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerReply
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerSendShortcut
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerTokens
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChip
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

private val AnaItem = MonarchMentionItem(id = "u_1", label = "Ana Ruiz", detail = "Design")
private val Modes = listOf(
    MonarchMessageComposerMode("reply", "Reply"),
    MonarchMessageComposerMode(
        value = "note",
        label = "Internal note",
        note = true,
        placeholder = "Only your team sees this",
        sendLabel = "Add note",
    ),
)

internal object MonarchMessageComposerFixtures : ComponentFixtures {
    override val id = "message-composer"
    override val fixtures = listOf(
        fixture("empty with placeholder and attach") {
            MonarchMessageComposer(
                label = "Message #design",
                state = rememberTextFieldState(),
                onSend = {},
                placeholder = "Message #design",
                onAttach = {},
            )
        },
        fixture("text, count and draft saved") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("See you at 3"),
                onSend = {},
                maxLength = 280,
                draftStatus = MonarchSaveStatus.Saved,
            )
        },
        fixture("replying, dismissable") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                replyTo = MonarchMessageComposerReply("Ana Ruiz", "Are we still on for the review this afternoon?"),
                onDismissReply = {},
            )
        },
        fixture("editing with cancel") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("Fixed the typo"),
                onSend = {},
                editing = true,
                onCancelEdit = {},
            )
        },
        fixture("modes, note selected") {
            MonarchMessageComposer(
                label = "Reply to customer",
                state = rememberTextFieldState(),
                onSend = {},
                modes = Modes,
                selectedMode = "note",
            )
        },
        fixture("over the limit") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("Too long"),
                onSend = {},
                maxLength = 5,
            )
        },
        fixture("sending") {
            MonarchMessageComposer(label = "Message", state = rememberTextFieldState("Hi"), onSend = {}, sending = true)
        },
        fixture("disabled with reason, draft error") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                enabled = false,
                disabledReason = "You can't post in this channel.",
                draftStatus = MonarchSaveStatus.Error,
                emojiPicker = { _, _ -> },
            )
        },
        fixture("attachments, actions and icons") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                hasAttachments = true,
                onAttach = {},
                attachIcon = { Icon(TestIcon, contentDescription = null) },
                emojiIcon = { Icon(TestIcon, contentDescription = null) },
                emojiPicker = { _, _ -> },
                attachments = { MonarchAttachmentChip(name = "report.pdf", onRemove = {}) },
                actions = { MonarchButton(text = "Schedule", onClick = {}) },
            )
        },
        fixture("mention suggestions") {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("Thanks @an"),
                onSend = {},
                mentions = MonarchMessageComposerMentions(listOf(AnaItem)) {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchMessageComposerTest {
    @get:Rule
    val rule = createComposeRule()

    private val field = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Message"))

    @Test
    fun theSendButtonSendsTheTrimmedTextAndClearsTheField() {
        val state = TextFieldState()
        val sent = mutableListOf<MonarchMessageComposerMessage>()
        rule.setMonarchContent { MonarchMessageComposer(label = "Message", state = state, onSend = { sent += it }) }
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNode(field).performTextInput("  Hello  ")
        rule.onNodeWithText("Send").assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Hello"), sent.map { it.text })
        assertNull(sent.single().mode)
        assertEquals("", state.text.toString())
    }

    @Test
    fun theTextStaysWhenSendingThrows() {
        val state = TextFieldState("Hello")
        rule.setMonarchContent {
            MonarchMessageComposer(label = "Message", state = state, onSend = { error("offline") })
        }
        rule.onNodeWithText("Send").performClick()
        rule.waitForIdle()
        assertEquals("Hello", state.text.toString())
        rule.onNodeWithText("Send").assertIsEnabled()
    }

    @Test
    fun attachmentsAllowSendingWithoutText() {
        val sent = mutableListOf<MonarchMessageComposerMessage>()
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = { sent += it },
                hasAttachments = true,
            )
        }
        rule.onNodeWithText("Send").assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(listOf(""), sent.map { it.text })
    }

    @Test
    fun theCountReadsWhatIsLeftAndOverTheLimitSendingIsOff() {
        val state = TextFieldState("Hi")
        rule.setMonarchContent { MonarchMessageComposer(label = "Message", state = state, onSend = {}, maxLength = 5) }
        rule.onNodeWithContentDescription("3 characters left").assertExists()
        rule.onNode(field).performTextInput(" there")
        rule.onNodeWithContentDescription("3 characters over the limit").assertExists()
        rule.onNodeWithText("Send").assertIsNotEnabled()
    }

    @Test
    fun theReplyContextIsOneElementWithASeparateDismissButton() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                replyTo = MonarchMessageComposerReply("Ana Ruiz", "Lunch?"),
                onDismissReply = { dismissed++ },
            )
        }
        val context = rule.onNodeWithText("Replying to Ana Ruiz").fetchSemanticsNode()
        val text = context.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Replying to Ana Ruiz", "Lunch?"), text)
        rule.onNodeWithText("Stop replying").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun editingSavesAndCancels() {
        var cancelled = 0
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("Fixed"),
                onSend = {},
                editing = true,
                onCancelEdit = { cancelled++ },
            )
        }
        rule.onNodeWithText("Editing message").assertExists()
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onAllNodesWithText("Send").assertCountEquals(0)
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun modesChangeThePlaceholderTheSendLabelAndTheMessage() {
        val sent = mutableListOf<MonarchMessageComposerMessage>()
        val changes = mutableListOf<String>()
        val state = TextFieldState()
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = state,
                onSend = { sent += it },
                modes = Modes,
                onModeChange = { changes += it },
            )
        }
        rule.onNodeWithText("Reply").assertIsSelected()
        rule.onNodeWithText("Internal note").performClick()
        assertEquals(listOf("note"), changes)
        rule.onNodeWithText("Internal note").assertIsSelected()
        rule.onNodeWithText("Only your team sees this", useUnmergedTree = true).assertExists()
        rule.onNode(field).performTextInput("Refund approved")
        rule.onNodeWithText("Add note").performClick()
        rule.waitForIdle()
        assertEquals("note", sent.single().mode)
    }

    @Test
    fun aPickedMentionIsWrittenAndSent() {
        val queries = mutableListOf<MonarchMentionQuery?>()
        val sent = mutableListOf<MonarchMessageComposerMessage>()
        val state = TextFieldState()
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = state,
                onSend = { sent += it },
                mentions = MonarchMessageComposerMentions(listOf(AnaItem)) { queries += it },
            )
        }
        rule.onNode(field).performTextInput("Thanks @an")
        rule.waitForIdle()
        assertEquals("an", queries.last()?.text)
        rule.onNodeWithText("Ana Ruiz", substring = true).performClick()
        assertEquals("Thanks @Ana Ruiz ", state.text.toString())
        rule.waitForIdle()
        assertNull(queries.last())
        rule.onNodeWithText("Send").performClick()
        rule.waitForIdle()
        assertEquals("Thanks @Ana Ruiz", sent.single().text)
        assertEquals(listOf("u_1"), sent.single().mentions.map { it.id })
    }

    @Test
    fun thePickedEmojiIsInsertedAtTheCaret() {
        val state = TextFieldState()
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = state,
                onSend = {},
                emojiPicker = { onPick, _ -> MonarchButton(text = "Smile", onClick = { onPick("🙂") }) },
            )
        }
        rule.onAllNodesWithText("Smile").assertCountEquals(0)
        rule.onNodeWithText("Add emoji").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Smile").performClick()
        assertEquals("🙂", state.text.toString())
        rule.onAllNodesWithText("Smile").assertCountEquals(0)
    }

    @Test
    fun typingIsReportedOncePerIntervalAndStopsWhenEmptied() {
        var typing = 0
        var stopped = 0
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                onTyping = { typing++ },
                onTypingStop = { stopped++ },
            )
        }
        rule.onNode(field).performTextInput("a")
        rule.waitForIdle()
        rule.onNode(field).performTextInput("b")
        rule.waitForIdle()
        assertEquals(1, typing)
        assertEquals(0, stopped)
        rule.onNode(field).performTextClearance()
        rule.waitForIdle()
        assertEquals(1, stopped)
    }

    @Test
    fun hardwareEnterSendsAndShiftEnterDoesNot() {
        val sent = mutableListOf<MonarchMessageComposerMessage>()
        rule.setMonarchContent {
            MonarchMessageComposer(label = "Message", state = rememberTextFieldState("Hi"), onSend = { sent += it })
        }
        rule.onNode(field).requestFocus()
        rule.onNode(field).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        rule.waitForIdle()
        assertTrue(sent.isEmpty())
        rule.onNode(field).performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(1, sent.size)
    }

    @Test
    fun theKeyRulesFollowTheShortcut() {
        fun press(key: Key, shift: Boolean = false, command: Boolean = false, hardware: Boolean = true) =
            MessageComposerKey(key, shift, command, hardware)
        val enter = MonarchMessageComposerSendShortcut.Enter
        val ctrl = MonarchMessageComposerSendShortcut.CtrlEnter
        fun action(
            key: MessageComposerKey,
            shortcut: MonarchMessageComposerSendShortcut = enter,
            editing: Boolean = false,
            empty: Boolean = false,
        ) = MonarchMessageComposerKeys.keyAction(key, shortcut, editing, empty, canEditLast = true)
        assertEquals(MessageComposerKeyAction.Send, action(press(Key.Enter)))
        assertEquals(MessageComposerKeyAction.Send, action(press(Key.NumPadEnter)))
        assertEquals(MessageComposerKeyAction.None, action(press(Key.Enter, shift = true)))
        assertEquals(MessageComposerKeyAction.None, action(press(Key.Enter, hardware = false)))
        assertEquals(MessageComposerKeyAction.None, action(press(Key.Enter), ctrl))
        assertEquals(MessageComposerKeyAction.Send, action(press(Key.Enter, command = true), ctrl))
        assertEquals(MessageComposerKeyAction.CancelEdit, action(press(Key.Escape), editing = true))
        assertEquals(MessageComposerKeyAction.None, action(press(Key.Escape)))
        assertEquals(MessageComposerKeyAction.EditLast, action(press(Key.DirectionUp), empty = true))
        assertEquals(MessageComposerKeyAction.None, action(press(Key.DirectionUp)))
        val noEditLast = MonarchMessageComposerKeys.keyAction(press(Key.DirectionUp), enter, false, true, false)
        assertEquals(MessageComposerKeyAction.None, noEditLast)
    }

    @Test
    fun theSendAndMentionRulesHold() {
        val t = MonarchMessageComposerTokens
        assertTrue(t.canSend("Hi", enabled = true, sending = false, hasAttachments = false, maxLength = null))
        assertFalse(t.canSend("  ", enabled = true, sending = false, hasAttachments = false, maxLength = null))
        assertTrue(t.canSend("", enabled = true, sending = false, hasAttachments = true, maxLength = null))
        assertFalse(t.canSend("Hi", enabled = false, sending = false, hasAttachments = false, maxLength = null))
        assertFalse(t.canSend("Hi", enabled = true, sending = true, hasAttachments = false, maxLength = null))
        assertFalse(t.canSend("Hello", enabled = true, sending = false, hasAttachments = false, maxLength = 4))
        assertEquals(-1, t.remaining("Hello", 4))
        assertNull(t.remaining("Hello", null))
        assertEquals("Hi @Ana Ruiz " to 13, t.insertMention("Hi @an", 3, 6, "@Ana Ruiz"))
        assertEquals("Hi @Ana Ruiz and" to 13, t.insertMention("Hi @an and", 3, 6, "@Ana Ruiz"))
        val ana = MessageComposerPicked('@', AnaItem)
        val ben = MessageComposerPicked('@', MonarchMentionItem(id = "u_2", label = "Ben"))
        assertEquals(listOf("u_1"), t.mentionsIn("Thanks @Ana Ruiz", listOf(ana, ben, ana)).map { it.id })
    }

    @Test
    fun aDisabledComposerSaysWhyAndHidesTheEmojiButton() {
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                enabled = false,
                disabledReason = "Read-only channel",
                emojiPicker = { _, _ -> },
            )
        }
        rule.onNode(field).assertIsNotEnabled()
        rule.onNodeWithText("Read-only channel").assertExists()
        rule.onAllNodesWithText("Add emoji").assertCountEquals(0)
    }

    @Test
    fun theDraftStatusIsShownUnderTheBox() {
        var status by mutableStateOf(MonarchSaveStatus.Idle)
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                draftStatus = status,
            )
        }
        rule.onAllNodesWithText("Draft saved", substring = true).assertCountEquals(0)
        status = MonarchSaveStatus.Saved
        rule.onNodeWithText("Draft saved", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun iconButtonsAreNamed() {
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                onAttach = {},
                attachIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Attach files").assertTouchTargetAtLeast48dp(rule.density)
        rule.onAllNodesWithContentDescription("Add emoji").assertCountEquals(0)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState("Hola"),
                onSend = {},
                maxLength = 10,
                replyTo = MonarchMessageComposerReply("Ana"),
                onAttach = {},
                labels = MonarchMessageComposerLabels(
                    send = "Enviar",
                    attach = "Adjuntar",
                    replyingTo = { "Respondiendo a $it" },
                    remaining = { "Quedan $it" },
                ),
            )
        }
        rule.onNodeWithText("Enviar").assertExists()
        rule.onNodeWithText("Adjuntar").assertExists()
        rule.onNodeWithText("Respondiendo a Ana").assertExists()
        rule.onNodeWithContentDescription("Quedan 6").assertExists()
    }

    @Test
    fun attachComesBeforeSendInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchMessageComposer(label = "Message", state = rememberTextFieldState(), onSend = {}, onAttach = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Attach files").assertIsBefore(rule.onNodeWithText("Send"), direction)
        }
    }

    @Test
    fun theContextFitsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMessageComposer(
                label = "Message",
                state = rememberTextFieldState(),
                onSend = {},
                replyTo = MonarchMessageComposerReply("Maria Fernanda de los Santos"),
            )
        }
        rule.onNodeWithText("Replying to Maria Fernanda de los Santos", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNode(field).assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchMessageComposerTokens
            assertEquals(c.bgSurface, t.boxColors(note = false, enabled = true, colors = c).container, theme.id)
            assertEquals(c.borderInput, t.boxColors(note = false, enabled = true, colors = c).border, theme.id)
            assertEquals(c.warningSubtle, t.boxColors(note = true, enabled = true, colors = c).container, theme.id)
            assertEquals(c.warningBorder, t.boxColors(note = true, enabled = true, colors = c).border, theme.id)
            assertEquals(c.bgSubtle, t.boxColors(note = true, enabled = false, colors = c).container, theme.id)
            assertEquals(c.dangerStrong, t.countColor(over = true, colors = c), theme.id)
            assertEquals(c.textMuted, t.countColor(over = false, colors = c), theme.id)
            assertEquals(c.textSubtle, t.placeholderColor(c), theme.id)
        }
    }
}

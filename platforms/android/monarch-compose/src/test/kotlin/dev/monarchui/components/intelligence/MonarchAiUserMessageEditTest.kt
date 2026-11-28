package dev.monarchui.components.intelligence

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiusermessageedit.MonarchAiUserMessageEdit
import dev.monarchui.components.intelligence.aiusermessageedit.MonarchAiUserMessageEditTokens
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val ORIGINAL = "Summarise the quarterly report."

internal object MonarchAiUserMessageEditFixtures : ComponentFixtures {
    override val id = "ai-user-message-edit"
    override val fixtures = listOf(
        fixture("unchanged") { MonarchAiUserMessageEdit(ORIGINAL, {}, {}, focusOnAppear = false) },
        fixture("disabled") { MonarchAiUserMessageEdit(ORIGINAL, {}, {}, enabled = false) },
        fixture("empty") { MonarchAiUserMessageEdit("", {}, {}, focusOnAppear = false) },
    )
}

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class MonarchAiUserMessageEditTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())

    private fun show(onSubmit: (String) -> Unit = {}, onCancel: () -> Unit = {}, focusOnAppear: Boolean = true) {
        rule.setMonarchContent {
            MonarchAiUserMessageEdit(ORIGINAL, onSubmit, onCancel, focusOnAppear = focusOnAppear)
        }
    }

    @Test
    fun theFieldStartsWithTheOriginalAndIsLabelled() {
        show()
        field.assert(hasContentDescription("Edit message"))
        rule.onNodeWithText(ORIGINAL).assertExists()
        rule.onNodeWithText("Editing will create a new branch. The original stays available.").assertExists()
    }

    @Test
    fun sendIsDisabledUntilTheTextChanges() {
        show()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        field.performTextInput(" ")
        rule.onNodeWithText("Send").assertIsNotEnabled()
        field.performTextInput("Add a chart.")
        rule.onNodeWithText("Send").assertIsEnabled()
    }

    @Test
    fun sendSubmitsTheTrimmedText() {
        var sent: String? = null
        show(onSubmit = { sent = it })
        field.performTextClearance()
        field.performTextInput("  Summarise it in three points.\n")
        rule.onNodeWithText("Send").performClick()
        assertEquals("Summarise it in three points.", sent)
    }

    @Test
    fun cancelCallsOnCancel() {
        var cancels = 0
        show(onCancel = { cancels++ })
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun theFieldTakesFocusWhenAskedOnly() {
        show()
        field.assertIsFocused()
    }

    @Test
    fun theFieldDoesNotTakeFocusWhenNotAsked() {
        show(focusOnAppear = false)
        field.assertIsNotFocused()
    }

    @Test
    fun ctrlEnterSendsAndEscapeCancels() {
        var sent: String? = null
        var cancels = 0
        show(onSubmit = { sent = it }, onCancel = { cancels++ })
        field.performTextInput(" Add a chart.")
        field.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) } }
        assertEquals("$ORIGINAL Add a chart.", sent)
        field.performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, cancels)
    }

    @Test
    fun aDisabledEditorCannotSendOrCancel() {
        var calls = 0
        rule.setMonarchContent { MonarchAiUserMessageEdit(ORIGINAL, { calls++ }, { calls++ }, enabled = false) }
        rule.onNode(hasContentDescription("Edit message")).assertIsNotEnabled()
        rule.onNodeWithText("Cancel").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        assertEquals(0, calls)
    }

    @Test
    fun theSubmissionRuleTrimsAndComparesWithTheOriginal() {
        assertNull(MonarchAiUserMessageEditTokens.submission("  ", "a"))
        assertNull(MonarchAiUserMessageEditTokens.submission(" a \n", "a"))
        assertEquals("b", MonarchAiUserMessageEditTokens.submission(" b ", "a"))
        assertEquals(2, MonarchAiUserMessageEditTokens.maxLines(1))
        assertEquals(10, MonarchAiUserMessageEditTokens.maxLines(10))
    }

    @Test
    fun theButtonsHaveA48dpTarget() {
        show()
        rule.onNodeWithText("Cancel").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Send").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun cancelComesBeforeSendInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchAiUserMessageEdit(ORIGINAL, {}, {}, focusOnAppear = false) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Cancel").assertIsBefore(rule.onNodeWithText("Send"), direction)
        }
    }

    @Test
    fun theNoteWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiUserMessageEdit(ORIGINAL, {}, {}, focusOnAppear = false)
        }
        rule.onNodeWithText("Editing will create a new branch. The original stays available.", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun everyBuiltInStringCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiUserMessageEdit(
                ORIGINAL,
                {},
                {},
                label = "Modifier le message",
                branchHint = "Une nouvelle branche sera créée.",
                sendLabel = "Envoyer",
                cancelLabel = "Annuler",
            )
        }
        field.assert(hasContentDescription("Modifier le message"))
        listOf(
            "Une nouvelle branche sera créée.",
            "Envoyer",
            "Annuler",
        ).forEach { rule.onNodeWithText(it).assertExists() }
    }
}

package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiCompletion
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiCompletionAcceptance
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiCompletionStatus
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiGhostText
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiGhostTextAcceptKind
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiGhostTextLabels
import com.rookhq.monarch.components.intelligence.aighosttext.MonarchAiGhostTextTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SUGGESTION = " send the figures by Friday."

internal object MonarchAiGhostTextFixtures : ComponentFixtures {
    override val id = "ai-ghost-text"
    override val fixtures = listOf(
        fixture("empty") { MonarchAiGhostText(rememberTextFieldState(), label = "Reply") { _, _ -> null } },
        fixture("with text and helper") {
            MonarchAiGhostText(
                rememberTextFieldState("Thanks for the update."),
                label = "Reply",
                helperText = "Suggestions appear as you type.",
            ) { _, _ -> null }
        },
        fixture("disabled") {
            MonarchAiGhostText(rememberTextFieldState("Thanks"), label = "Reply", enabled = false) { _, _ -> null }
        },
        fixture("no accept button") {
            MonarchAiGhostText(rememberTextFieldState(), label = "Reply", showAcceptButton = false) { _, _ -> null }
        },
    )
}

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class MonarchAiGhostTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val state = TextFieldState()
    private val accepted = mutableListOf<Pair<MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind>>()
    private var dismissals = 0
    private val field get() = rule.onNode(hasSetTextAction())
    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Composable
    private fun Field(enabled: Boolean = true, labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels()) {
        MonarchAiGhostText(
            state = state,
            label = "Reply",
            modifier = Modifier.testTag("ghost"),
            enabled = enabled,
            debounceMillis = 0L,
            labels = labels,
            onAccept = { acceptance, kind -> accepted += acceptance to kind },
            onDismiss = { dismissals++ },
        ) { text, _ -> if (text.endsWith("I will")) SUGGESTION else null }
    }

    private fun typeUntilSuggested(labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels()) {
        rule.setMonarchContent { Field(labels = labels) }
        field.performTextInput("Thanks. I will")
        val accept = labels.accept ?: "Accept suggestion"
        rule.waitUntil { rule.onAllNodes(hasText(accept)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aSuggestionShowsAfterTheTextButIsNotPartOfIt() {
        typeUntilSuggested()
        assertEquals("Thanks. I will", state.text.toString())
        val editable = field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("Thanks. I will", editable, "TalkBack reads the typed text only")
    }

    @Test
    fun aNewSuggestionIsAnnouncedPolitely() {
        typeUntilSuggested()
        rule.onNode(live and SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Suggestion: ${SUGGESTION.trim()} — use Accept suggestion, or press Tab on a keyboard."),
        )).assertExists()
    }

    @Test
    fun theAcceptButtonInsertsTheSuggestion() {
        typeUntilSuggested()
        rule.onNodeWithText("Accept suggestion").performClick()
        assertEquals("Thanks. I will$SUGGESTION", state.text.toString())
        assertEquals(TextRange(state.text.length), state.selection)
        assertEquals(MonarchAiGhostTextAcceptKind.All, accepted.single().second)
        assertEquals(SUGGESTION, accepted.single().first.inserted)
        rule.onNode(live and SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Suggestion accepted."),
        )).assertExists()
        rule.onNodeWithText("Accept suggestion").assertDoesNotExist()
    }

    @Test
    fun tabAcceptsTheSuggestion() {
        typeUntilSuggested()
        field.performKeyInput { pressKey(Key.Tab) }
        assertEquals("Thanks. I will$SUGGESTION", state.text.toString())
    }

    @Test
    fun ctrlWithTheForwardArrowAcceptsOneWordAndKeepsTheRest() {
        typeUntilSuggested()
        field.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.DirectionRight) } }
        assertEquals("Thanks. I will send", state.text.toString())
        assertEquals(MonarchAiGhostTextAcceptKind.Word, accepted.single().second)
        rule.onNodeWithText("Accept suggestion").assertExists()
    }

    @Test
    fun escapeDismissesTheSuggestion() {
        typeUntilSuggested()
        field.performKeyInput { pressKey(Key.Escape) }
        assertEquals("Thanks. I will", state.text.toString())
        assertEquals(1, dismissals)
        rule.onNodeWithText("Accept suggestion").assertDoesNotExist()
        rule.onNode(live and SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Suggestion dismissed."),
        )).assertExists()
    }

    @Test
    fun talkBackHasAcceptAndDismissActionsWhileASuggestionShows() {
        typeUntilSuggested()
        val actions = field.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Accept suggestion", "Dismiss suggestion"), actions.map { it.label })
        rule.runOnIdle { actions.first().action() }
        assertEquals("Thanks. I will$SUGGESTION", state.text.toString())
        assertFalse(field.fetchSemanticsNode().config.contains(SemanticsActions.CustomActions))
    }

    @Test
    fun nothingIsSuggestedWithTheCaretInTheMiddleOfTheText() {
        rule.setMonarchContent { Field() }
        field.performTextInput("Thanks. I will")
        rule.runOnIdle { state.edit { selection = TextRange(3) } }
        rule.waitForIdle()
        rule.onNodeWithText("Accept suggestion").assertDoesNotExist()
        assertFalse(MonarchAiGhostTextTokens.caretAtEnd("abc def", TextRange(3)))
        assertTrue(MonarchAiGhostTextTokens.caretAtEnd("abc  ", TextRange(3)))
        assertFalse(MonarchAiGhostTextTokens.caretAtEnd("abc", TextRange(0, 3)))
    }

    @Test
    fun aDisabledFieldCannotBeEdited() {
        rule.setMonarchContent { Field(enabled = false) }
        rule.onNode(hasText("Reply")).assertIsNotEnabled()
        rule.onNodeWithText("Accept suggestion").assertDoesNotExist()
    }

    @Test
    fun theAnnouncementsAndActionNamesCanBeReplaced() {
        typeUntilSuggested(
            MonarchAiGhostTextLabels(
                announce = { "Suggestion : $it" },
                accept = "Accepter",
                dismiss = "Ignorer",
                accepted = "Accepté.",
            ),
        )
        rule.onNode(live and SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Suggestion : ${SUGGESTION.trim()}"),
        )).assertExists()
        val actions = field.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Accepter", "Ignorer"), actions.map { it.label })
        rule.onNodeWithText("Accepter").performClick()
        rule.onNode(live and SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Accepté.")))
            .assertExists()
    }

    @Test
    fun theAcceptButtonHasA48dpTarget() {
        typeUntilSuggested()
        rule.onNodeWithText("Accept suggestion").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiGhostText(
                TextFieldState(),
                label = "Reply",
                helperText = "Suggestions appear as you type.",
            ) { _, _ ->
                null
            }
        }
        rule.onNodeWithText("Suggestions appear as you type.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theSuggestionUsesTheMutedTextRoleInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAiGhostTextTokens.suggestionColor(colors), theme.id)
        }
    }

    // The completion on its own, with requests run at once.
    private fun completion(
        minimumCharacters: Int = 3,
        complete: suspend (String, Int) -> String?,
    ) = MonarchAiCompletion(CoroutineScope(Dispatchers.Unconfined), 0L, minimumCharacters, true, complete)

    @Test
    fun aRequestNeedsTheMinimumCharacters() {
        val requests = mutableListOf<String>()
        val c = completion { text, _ -> requests += text; "x" }
        c.update(" a ", 3)
        assertEquals(MonarchAiCompletionStatus.Idle, c.status)
        c.update("abc", 3)
        assertEquals(listOf("abc"), requests)
        assertEquals(MonarchAiCompletionStatus.Ready, c.status)
        assertEquals("x", c.suggestion)
    }

    @Test
    fun typingThroughTheSuggestionKeepsTheRestWithoutANewRequest() {
        var requests = 0
        val c = completion { _, _ -> requests++; " world" }
        c.update("Hello", 5)
        c.update("Hello w", 7)
        assertEquals(1, requests)
        assertEquals("orld", c.suggestion)
        c.update("Hello wx", 8)
        assertEquals(2, requests)
    }

    @Test
    fun aStaleAnswerIsIgnored() {
        val first = CompletableDeferred<String?>()
        val c = completion { text, _ -> if (text == "abc") first.await() else "new" }
        c.update("abc", 3)
        assertEquals(MonarchAiCompletionStatus.Loading, c.status)
        c.update("abcd", 4)
        first.complete("old")
        assertEquals("new", c.suggestion)
        assertEquals("abcd", c.text)
    }

    @Test
    fun aFailureIsReportedAndShowsNothing() {
        var failure: Throwable? = null
        val c = completion { _, _ -> error("offline") }
        c.onError = { failure = it }
        c.update("abc", 3)
        assertEquals(MonarchAiCompletionStatus.Error, c.status)
        assertEquals("", c.suggestion)
        assertEquals("offline", failure?.message)
    }

    @Test
    fun acceptingAWordKeepsTheRestAndAcceptingAllEndsIt() {
        val c = completion { _, _ -> " send it, today" }
        c.update("I will", 6)
        val word = c.acceptWord()
        assertEquals(" send", word?.inserted)
        assertEquals("I will send", word?.text)
        assertEquals(11, word?.cursor)
        assertEquals(" it, today", c.suggestion)
        val rest = c.accept()
        assertEquals("I will send it, today", rest?.text)
        assertEquals(MonarchAiCompletionStatus.Idle, c.status)
        assertNull(c.accept())
    }

    @Test
    fun composingAndDisablingDismissAndBlockRequests() {
        var requests = 0
        val c = completion { _, _ -> requests++; "x" }
        c.update("abc", 3)
        c.setComposing(true)
        assertEquals(MonarchAiCompletionStatus.Idle, c.status)
        c.update("abcd", 4)
        assertEquals(1, requests)
        c.setComposing(false)
        c.enabled = false
        c.update("abcde", 5)
        assertEquals(1, requests)
        assertEquals(MonarchAiCompletionStatus.Idle, c.status)
    }

    @Test
    fun theNextWordTakesItsPunctuationAndLeadingSpace() {
        assertEquals(" world,", MonarchAiCompletion.nextWord(" world, again"))
        assertEquals("(hello)", MonarchAiCompletion.nextWord("(hello) there"))
        assertEquals("", MonarchAiCompletion.nextWord(""))
        assertEquals("   ", MonarchAiCompletion.nextWord("   "))
    }
}

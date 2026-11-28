package dev.monarchui.components.intelligence

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiinlineprompt.AiInlinePromptPanel
import dev.monarchui.components.intelligence.aiinlineprompt.MonarchAiInlinePrompt
import dev.monarchui.components.intelligence.aiinlineprompt.MonarchAiInlinePromptArea
import dev.monarchui.components.intelligence.aiinlineprompt.MonarchAiInlinePromptLabels
import dev.monarchui.components.intelligence.aiinlineprompt.MonarchAiInlinePromptTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val InlinePromptSelection =
    "The team shipped the new onboarding flow in March. Early numbers suggest more people finish setup " +
        "on their first visit, although the sample is still small and support tickets have not changed much yet."

internal object MonarchAiInlinePromptFixtures : ComponentFixtures {
    override val id = "ai-inline-prompt"
    override val fixtures = listOf(
        fixture("sheet") {
            MonarchAiInlinePrompt(onDismissRequest = {}, onSubmit = {}, context = "Early numbers suggest more.")
        },
        fixture("excerpt") {
            AiInlinePromptPanel(onSend = {}, context = "Early numbers suggest more people finish setup.")
        },
        fixture("long excerpt") {
            AiInlinePromptPanel(onSend = {}, context = InlinePromptSelection)
        },
        fixture("suggestions") {
            AiInlinePromptPanel(onSend = {}, suggestions = listOf("Shorten", "Make it more formal"))
        },
        fixture("initial value") {
            AiInlinePromptPanel(onSend = {}, initialValue = "Make it shorter")
        },
        fixture("pending") {
            AiInlinePromptPanel(
                onSend = {},
                suggestions = listOf("Shorten"),
                initialValue = "Make it shorter",
                pending = true,
            )
        },
    )
}

/** A selection toolbar that keeps the menu it was asked to show, so a test can read and pick its items. */
private class RecordingToolbarProvider : TextContextMenuProvider {
    var data: TextContextMenuData? by mutableStateOf(null)
    var closed = false
    private var provider: TextContextMenuDataProvider? = null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        provider = dataProvider
        data = dataProvider.data()
        awaitCancellation()
    }

    /** Asks for the menu again, as the toolbar does when it is refreshed. */
    fun rebuild() {
        data = assertNotNull(provider, "the menu was never shown").data()
    }

    fun item(label: String): TextContextMenuItem? =
        data?.components?.filterIsInstance<TextContextMenuItem>()?.firstOrNull { it.label == label }

    fun pick(label: String) {
        val item = assertNotNull(item(label), "no \"$label\" in ${data?.components}")
        item.onClick(
            object : TextContextMenuSession {
                override fun close() {
                    closed = true
                }
            },
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiInlinePromptTest {
    @get:Rule
    val rule = createComposeRule()

    private val field = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Prompt"))

    @Test
    fun theSheetIsNamedAndFocusesThePromptField() {
        rule.setMonarchContent {
            MonarchAiInlinePrompt(onDismissRequest = {}, onSubmit = {}, initialValue = "Make it shorter")
        }
        rule.waitForIdle()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Ask AI")).assertExists()
        rule.onNode(isHeading() and hasText("Ask AI")).assertExists()
        rule.onNode(field).assertIsFocused()
        val text = rule.onNode(field).fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("Make it shorter", text)
    }

    @Test
    fun sendSubmitsTheTrimmedPromptAndClosesTheSheet() {
        val sent = mutableListOf<String>()
        var open by mutableStateOf(true)
        rule.setMonarchContent {
            if (open) {
                MonarchAiInlinePrompt(onDismissRequest = { open = false }, onSubmit = { sent += it }, sendIcon = null)
            }
        }
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNode(field).performTextInput("   ")
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNode(field).performTextInput("Make it shorter  ")
        rule.onNodeWithText("Send").assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Make it shorter"), sent)
        assertEquals(false, open)
    }

    @Test
    fun withoutCloseOnSubmitTheSheetStays() {
        val sent = mutableListOf<String>()
        var dismissed = false
        rule.setMonarchContent {
            MonarchAiInlinePrompt(
                onDismissRequest = { dismissed = true },
                sendIcon = null,
                onSubmit = { sent += it },
                closeOnSubmit = false,
            )
        }
        rule.onNode(field).performTextInput("Fix spelling")
        rule.onNodeWithText("Send").performClick()
        rule.waitForIdle()
        assertEquals(listOf("Fix spelling"), sent)
        assertEquals(false, dismissed)
        rule.onNode(field).assertExists()
    }

    @Test
    fun aSuggestionSendsAtOnce() {
        val sent = mutableListOf<String>()
        rule.setMonarchContent {
            AiInlinePromptPanel(onSend = { sent += it }, suggestions = listOf("Shorten", "Make it more formal"))
        }
        rule.onNodeWithText("Make it more formal").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("Make it more formal"), sent)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Suggestions")))
            .assertExists()
    }

    @Test
    fun theExcerptIsReadWithItsLabelAndCut() {
        rule.setMonarchContent { AiInlinePromptPanel(onSend = {}, context = InlinePromptSelection) }
        val expected = InlinePromptSelection.take(MonarchAiInlinePromptTokens.EXCERPT_MAX).trimEnd() + "…"
        rule.onNodeWithText("Selection").assert(
            SemanticsMatcher("reads the excerpt with the label") { node ->
                node.config[SemanticsProperties.Text].map { it.text } == listOf("Selection", expected)
            },
        )
    }

    @Test
    fun pendingDisablesEverythingAndAnnouncesSending() {
        val sent = mutableListOf<String>()
        var pending by mutableStateOf(false)
        rule.setMonarchContent {
            AiInlinePromptPanel(
                onSend = { sent += it },
                suggestions = listOf("Shorten"),
                initialValue = "Make it shorter",
                pending = pending,
            )
        }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        val sending = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Sending…"))
        // The status region is there before anything is sent (beside the prompt input's own region), silent.
        rule.onAllNodes(polite).assertCountEquals(2)
        rule.onNode(sending).assertDoesNotExist()
        pending = true
        rule.waitForIdle()
        rule.onAllNodes(polite).assertCountEquals(2)
        rule.onNode(polite and sending).assertExists()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNodeWithText("Shorten").assertIsNotEnabled().performClick()
        rule.onNode(field).assertIsNotEnabled()
        assertTrue(sent.isEmpty())
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            AiInlinePromptPanel(
                onSend = {},
                context = "Text",
                suggestions = listOf("Shorten"),
                pending = true,
                labels = MonarchAiInlinePromptLabels(
                    label = "Demander à l’IA",
                    placeholder = "Que faire ?",
                    inputLabel = "Consigne",
                    submit = "Envoyer",
                    context = "Sélection",
                    suggestions = "Idées",
                    pending = "Envoi…",
                ),
            )
        }
        rule.onNodeWithText("Demander à l’IA").assertExists()
        rule.onNodeWithText("Que faire ?", useUnmergedTree = true).assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Consigne")))
            .assertExists()
        rule.onNodeWithText("Envoyer").assertExists()
        rule.onNodeWithText("Sélection", substring = true).assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Idées")))
            .assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Envoi…")))
            .assertExists()
    }

    @Test
    fun suggestionsFlowFromTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            AiInlinePromptPanel(onSend = {}, suggestions = listOf("One", "Two"))
        }
        rule.onNodeWithText("One").assertIsBefore(rule.onNodeWithText("Two"), LayoutDirection.Rtl)
    }

    @Test
    fun theExcerptFitsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            AiInlinePromptPanel(onSend = {}, context = "Early numbers suggest more people finish setup.")
        }
        rule.onNodeWithText("Early numbers suggest more people finish setup.", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithText("Selection", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun excerptAndPromptRules() {
        val tokens = MonarchAiInlinePromptTokens
        assertNull(tokens.excerpt(null))
        assertNull(tokens.excerpt("   "))
        assertEquals("Short", tokens.excerpt("Short"))
        assertEquals("abc…", tokens.excerpt("abc  defgh", max = 5))
        assertEquals("a".repeat(140), tokens.excerpt("a".repeat(140)))
        assertEquals("a".repeat(140) + "…", tokens.excerpt("a".repeat(141)))
        assertEquals("Shorten", tokens.prompt("  Shorten \n"))
        assertNull(tokens.prompt(" \n "))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiInlinePromptTokens
            val excerpt = tokens.excerptColors(colors)
            assertEquals(colors.bgSubtle, excerpt.container, theme.id)
            assertEquals(colors.textDefault, excerpt.content, theme.id)
            assertEquals(colors.aiBorder, excerpt.border, theme.id)
            assertEquals(colors.aiStrong, tokens.contextLabelColor(colors), theme.id)
            assertEquals(colors.textMuted, tokens.mutedColor(colors), theme.id)
        }
    }

    @Test
    fun theActionIsAddedToTheSelectionMenuOfSelectableText() {
        val toolbar = RecordingToolbarProvider()
        var clicks = 0
        rule.setMonarchContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides toolbar) {
                MonarchAiInlinePromptArea(onAskAi = { clicks++ }) {
                    SelectionContainer {
                        Text("Early numbers suggest more people finish setup.", Modifier.testTag("text"))
                    }
                }
            }
        }
        rule.onNodeWithTag("text").performTouchInput { longClick(center) }
        // Under load the toolbar can be asked for a frame after the gesture ends; wait for the menu itself.
        rule.waitUntil(timeoutMillis = 5_000) { toolbar.data?.components?.isNotEmpty() == true }
        assertNotNull(toolbar.item("Copy"), "the system items stay: ${toolbar.data?.components}")
        toolbar.pick("Ask AI")
        assertEquals(1, clicks)
        assertTrue(toolbar.closed)
    }

    @Test
    fun theTextFieldActionGivesTheSelectedText() {
        val toolbar = RecordingToolbarProvider()
        val state = TextFieldState("Early numbers suggest more")
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides toolbar) {
                MonarchAiInlinePromptArea(state = state, onAskAi = { picked += it }, label = "Rewrite") {
                    BasicTextField(state = state, modifier = Modifier.testTag("field"))
                }
            }
        }
        rule.onNodeWithTag("field").performTouchInput { longClick(center) }
        // Under load the toolbar can be asked for a frame after the gesture ends; wait for the menu itself.
        rule.waitUntil(timeoutMillis = 5_000) { toolbar.data?.components?.isNotEmpty() == true }
        val selection = state.selection
        assertTrue(!selection.collapsed, "the long press selects a word")
        toolbar.pick("Rewrite")
        assertEquals(listOf(state.text.substring(selection.min, selection.max)), picked)
        assertTrue(toolbar.closed)
    }

    @Test
    fun noActionWhenDisabled() {
        val toolbar = RecordingToolbarProvider()
        rule.setMonarchContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides toolbar) {
                MonarchAiInlinePromptArea(onAskAi = {}, enabled = false) {
                    SelectionContainer {
                        Text("Early numbers suggest more people finish setup.", Modifier.testTag("text"))
                    }
                }
            }
        }
        rule.onNodeWithTag("text").performTouchInput { longClick(center) }
        // Under load the toolbar can be asked for a frame after the gesture ends; wait for the menu itself.
        rule.waitUntil(timeoutMillis = 5_000) { toolbar.data?.components?.isNotEmpty() == true }
        assertNotNull(toolbar.item("Copy"), "the menu was shown: ${toolbar.data?.components}")
        assertNull(toolbar.item("Ask AI"))
    }

    @Test
    fun theTextFieldActionIsLeftOutWhileTheSelectionIsEmpty() {
        val toolbar = RecordingToolbarProvider()
        val state = TextFieldState("Early numbers suggest more")
        rule.setMonarchContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides toolbar) {
                MonarchAiInlinePromptArea(state = state, onAskAi = {}) {
                    BasicTextField(state = state, modifier = Modifier.testTag("field"))
                }
            }
        }
        rule.onNodeWithTag("field").performTouchInput { longClick(center) }
        // Under load the toolbar can be asked for a frame after the gesture ends; wait for the menu itself.
        rule.waitUntil(timeoutMillis = 5_000) { toolbar.data?.components?.isNotEmpty() == true }
        assertNotNull(toolbar.item("Ask AI"), "shown with a selection: ${toolbar.data?.components}")
        // The menu is built again from the field's state; with only a caret the item is left out.
        state.edit { selection = TextRange(2) }
        toolbar.rebuild()
        assertNull(toolbar.item("Ask AI"))
    }
}

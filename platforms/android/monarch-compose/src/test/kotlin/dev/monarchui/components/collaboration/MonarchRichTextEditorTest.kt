package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditor
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorDefaults
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorLabels
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorModel
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorState
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorTokens
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextMark
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextSpan
import dev.monarchui.components.collaboration.richtexteditor.RichTextEdit
import dev.monarchui.components.collaboration.richtexteditor.activeTools
import dev.monarchui.components.collaboration.richtexteditor.applyLink
import dev.monarchui.components.collaboration.richtexteditor.applyTool
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.onNodeWithTextOrDescription
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val Notes = "Release notes\nThe export now keeps formatting."
private val NotesSpans = listOf(
    MonarchRichTextSpan(MonarchRichTextMark.Heading2, 0, 13),
    MonarchRichTextSpan(MonarchRichTextMark.Bold, 18, 24),
    MonarchRichTextSpan(MonarchRichTextMark.Link, 35, 45, url = "https://example.com"),
)
private val Ana = MonarchMentionItem(id = "u1", label = "Ana Lima", detail = "Design")

private fun bold(start: Int, end: Int) = MonarchRichTextSpan(MonarchRichTextMark.Bold, start, end)

private fun heading(start: Int, end: Int) = MonarchRichTextSpan(MonarchRichTextMark.Heading1, start, end)

/** Types [typed] at the caret, as the text field reports it. */
private fun MonarchRichTextEditorState.type(typed: String, maxLength: Int? = null) {
    val at = selection.min
    val next = text.substring(0, at) + typed + text.substring(selection.max)
    onValueChange(TextFieldValue(next, TextRange(at + typed.length)), maxLength)
}

private fun MonarchRichTextEditorState.select(start: Int, end: Int = start) {
    onValueChange(TextFieldValue(text, TextRange(start, end)))
}

private fun MonarchRichTextEditorState.backspace() {
    val at = selection.min
    onValueChange(TextFieldValue(text.removeRange(at - 1, at), TextRange(at - 1)))
}

@OptIn(ExperimentalMonarchApi::class)
internal object MonarchRichTextEditorFixtures : ComponentFixtures {
    override val id = "rich-text-editor"
    override val fixtures = listOf(
        fixture("empty with a prompt and a limit") {
            MonarchRichTextEditor(
                label = "Description",
                state = remember { MonarchRichTextEditorState() },
                prompt = "What changed?",
                maxLength = 2_000,
            )
        },
        fixture("formatted document") {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState(Notes, NotesSpans) },
                tools = MonarchEditorTool.InlineGroups,
            )
        },
        fixture("over the limit, invalid") {
            MonarchRichTextEditor(
                label = "Summary",
                state = remember { MonarchRichTextEditorState("Far too long for this") },
                tools = emptyList(),
                maxLength = 10,
                invalid = true,
            )
        },
        fixture("disabled") {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState("Locked while saving") },
                enabled = false,
            )
        },
        fixture("read only") {
            // No link: an inline link's target is the height of its line, which the matrix's 48dp rule does not
            // exempt (as in MonarchAiMarkdownFixtures); readOnlyShowsTheDocumentWithoutTheBox covers links.
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState(Notes, NotesSpans.dropLast(1)) },
                readOnly = true,
            )
        },
        fixture("mention suggestions") {
            MonarchRichTextEditor(
                label = "Reply",
                state = remember { MonarchRichTextEditorState("Thanks @an") },
                tools = emptyList(),
                mentionItems = listOf(Ana),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
@OptIn(ExperimentalMonarchApi::class)
class MonarchRichTextEditorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun typingAtTheEndOfAFormatContinuesItButNotALink() {
        val state = MonarchRichTextEditorState("ab", listOf(bold(0, 2)))
        state.type("c")
        assertEquals(listOf(bold(0, 3)), state.spans)
        val link = MonarchRichTextSpan(MonarchRichTextMark.Link, 0, 2, url = "https://example.com")
        val linked = MonarchRichTextEditorState("ab", listOf(link))
        linked.type("c")
        assertEquals(listOf(link), linked.spans)
    }

    @Test
    fun spansMoveWithEditsBeforeAndInsideThem() {
        val state = MonarchRichTextEditorState("abcd", listOf(bold(1, 3)))
        state.select(0)
        state.type("x")
        assertEquals(listOf(bold(2, 4)), state.spans)
        state.select(3)
        state.type("y")
        assertEquals(listOf(bold(2, 5)), state.spans)
        state.select(2, 5)
        state.type("")
        assertTrue(state.spans.isEmpty())
    }

    @Test
    fun aRepeatedLetterIsPlacedWhereItWasTyped() {
        assertEquals(RichTextEdit(1, 0, 1), MonarchRichTextEditorModel.diff("aa", "aaa", 2))
        assertEquals(RichTextEdit(1, 1, 0), MonarchRichTextEditorModel.diff("abc", "ac", 1))
        assertNull(MonarchRichTextEditorModel.diff("same", "same", 0))
        val state = MonarchRichTextEditorState("aa", listOf(bold(0, 1)))
        state.select(1)
        state.type("a")
        assertEquals(listOf(bold(0, 2)), state.spans)
    }

    @Test
    fun aFormatTurnedOnAtTheCaretAppliesToWhatIsTypedNext() {
        val state = MonarchRichTextEditorState("ab")
        state.applyTool(MonarchEditorTool.Bold)
        assertTrue(MonarchEditorTool.Bold in state.activeTools())
        state.type("c")
        state.type("d")
        assertEquals(listOf(bold(2, 4)), state.spans)
    }

    @Test
    fun aFormatTurnedOffAtTheCaretStopsBeforeWhatIsTypedNext() {
        val state = MonarchRichTextEditorState("ab", listOf(bold(0, 2)))
        assertTrue(MonarchEditorTool.Bold in state.activeTools())
        state.applyTool(MonarchEditorTool.Bold)
        assertFalse(MonarchEditorTool.Bold in state.activeTools())
        state.type("c")
        assertEquals(listOf(bold(0, 2)), state.spans)
    }

    @Test
    fun movingTheCaretDropsPendingFormats() {
        val state = MonarchRichTextEditorState("ab")
        state.applyTool(MonarchEditorTool.Italic)
        state.select(1)
        state.type("x")
        assertTrue(state.spans.isEmpty())
    }

    @Test
    fun aSelectionTogglesAFormat() {
        val state = MonarchRichTextEditorState("abcd")
        state.select(0, 2)
        state.applyTool(MonarchEditorTool.Bold)
        assertEquals(listOf(bold(0, 2)), state.spans)
        assertTrue(MonarchEditorTool.Bold in state.activeTools())
        state.applyTool(MonarchEditorTool.Bold)
        assertTrue(state.spans.isEmpty())
        state.select(1, 4)
        state.applyTool(MonarchEditorTool.Strikethrough)
        state.select(0, 4)
        assertFalse(MonarchEditorTool.Strikethrough in state.activeTools())
    }

    @Test
    fun headingsCoverParagraphsAndSplitOnReturn() {
        val state = MonarchRichTextEditorState("Title\nBody")
        state.select(2)
        state.applyTool(MonarchEditorTool.heading(1))
        assertEquals(listOf(heading(0, 5)), state.spans)
        assertTrue(MonarchEditorTool.heading(1) in state.activeTools())
        state.select(5)
        state.type("\n")
        assertEquals(listOf(heading(0, 5)), state.spans)
        assertEquals(setOf(MonarchEditorTool.Paragraph), state.activeTools())
        state.select(2)
        state.type("\n")
        assertEquals(listOf(heading(0, 2), heading(3, 6)), state.spans)
        state.select(0)
        state.type("A ")
        assertEquals(heading(0, 4), state.spans.first())
    }

    @Test
    fun aHeadingMergedIntoTheParagraphBeforeIsDropped() {
        val state = MonarchRichTextEditorState("ab\nTitle", listOf(heading(3, 8)))
        state.select(3)
        state.backspace()
        assertEquals("abTitle", state.text)
        assertTrue(state.spans.isEmpty())
    }

    @Test
    fun anEmptyHeadingKeepsItsStyleForWhatIsTyped() {
        val state = MonarchRichTextEditorState()
        state.applyTool(MonarchEditorTool.heading(1))
        state.type("Hi")
        assertEquals(listOf(heading(0, 2)), state.spans)
        state.applyTool(MonarchEditorTool.Paragraph)
        assertTrue(state.spans.isEmpty())
    }

    @Test
    fun listsAreTogglesOverTheSelectedParagraphs() {
        val state = MonarchRichTextEditorState("one\ntwo")
        state.select(0, 7)
        state.applyTool(MonarchEditorTool.BulletList)
        assertEquals("• one\n• two", state.text)
        assertTrue(MonarchEditorTool.BulletList in state.activeTools())
        state.select(0, state.text.length)
        state.applyTool(MonarchEditorTool.OrderedList)
        assertEquals("1. one\n2. two", state.text)
        state.select(0, state.text.length)
        state.applyTool(MonarchEditorTool.OrderedList)
        assertEquals("one\ntwo", state.text)
    }

    @Test
    fun returnContinuesAListAndEndsItOnAnEmptyItem() {
        val state = MonarchRichTextEditorState("• one")
        state.type("\n")
        assertEquals("• one\n• ", state.text)
        assertEquals(TextRange(8), state.selection)
        state.type("\n")
        assertEquals("• one\n", state.text)
        val ordered = MonarchRichTextEditorState("1. one")
        ordered.type("\n")
        assertEquals("1. one\n2. ", ordered.text)
    }

    @Test
    fun undoAndRedoRestoreTextAndFormats() {
        val state = MonarchRichTextEditorState()
        assertFalse(state.canUndo)
        state.type("a")
        state.type("b")
        state.type(" ")
        state.select(0, 2)
        state.applyTool(MonarchEditorTool.Bold)
        state.undo()
        assertTrue(state.spans.isEmpty())
        state.undo()
        assertEquals("ab", state.text)
        state.undo()
        assertEquals("", state.text)
        assertFalse(state.canUndo)
        state.redo()
        assertEquals("ab", state.text)
        assertTrue(state.canRedo)
    }

    @Test
    fun typingStopsAtTheLimitAndPastingIsCut() {
        val state = MonarchRichTextEditorState("abcd")
        state.type("efgh", maxLength = 6)
        assertEquals("abcdef", state.text)
        assertEquals(TextRange(6), state.selection)
        state.type("x", maxLength = 6)
        assertEquals("abcdef", state.text)
        assertTrue(MonarchRichTextEditorDefaults.isOverLimit("abcdefg", 6))
        assertFalse(MonarchRichTextEditorDefaults.isOverLimit("abcdefg", null))
    }

    @Test
    fun linksApplyToTheSelectionOrAreWrittenAtTheCaret() {
        val state = MonarchRichTextEditorState("Read the docs")
        state.select(9, 13)
        state.applyLink("https://example.com/docs")
        assertEquals(
            listOf(MonarchRichTextSpan(MonarchRichTextMark.Link, 9, 13, "https://example.com/docs")),
            state.spans,
        )
        state.select(11)
        assertTrue(MonarchEditorTool.Link in state.activeTools())
        state.applyLink(null)
        assertTrue(state.spans.isEmpty())
        val empty = MonarchRichTextEditorState("See ")
        empty.applyLink("https://a.example")
        assertEquals("See https://a.example", empty.text)
        assertEquals(MonarchRichTextSpan(MonarchRichTextMark.Link, 4, 21, "https://a.example"), empty.spans.single())
    }

    @Test
    fun normalizingJoinsTouchingSpansAndClampsThem() {
        val spans = MonarchRichTextEditorModel.normalize(listOf(bold(0, 2), bold(2, 4), bold(3, 99)), "abcdef")
        assertEquals(listOf(bold(0, 6)), spans)
        assertEquals(
            listOf(MonarchRichTextSpan(MonarchRichTextMark.Heading2, 0, 3)),
            MonarchRichTextEditorModel.normalize(
                listOf(MonarchRichTextSpan(MonarchRichTextMark.Heading2, 0, 1)),
                "abc",
            ),
        )
    }

    @Test
    fun theStateIsSavedWithItsSpansAndSelection() {
        val state = MonarchRichTextEditorState(Notes, NotesSpans)
        state.select(2, 5)
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val saved = with(MonarchRichTextEditorState.Saver) { scope.save(state) }!!
        val restored = MonarchRichTextEditorState.Saver.restore(saved)!!
        assertEquals(Notes, restored.text)
        assertEquals(NotesSpans, restored.spans)
        assertEquals(TextRange(2, 5), restored.selection)
    }

    @Test
    fun theTextAreaIsNamedAndTheCountIsRead() {
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Description",
                state = remember { MonarchRichTextEditorState("Ship") },
                maxLength = 2_000,
            )
        }
        rule.onNode(hasContentDescription("Description") and hasSetTextAction()).assertIsEnabled()
        rule.onNodeWithContentDescription("1,996 characters left").assert(hasText("4 / 2,000"))
        rule.onNodeWithContentDescription("Formatting").assertExists()
        rule.onNodeWithTextOrDescription("Undo").assertIsNotEnabled()
    }

    @Test
    fun pastTheLimitTheFieldHasAnError() {
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Summary",
                state = remember { MonarchRichTextEditorState("Twelve chars") },
                maxLength = 10,
            )
        }
        rule.onNode(hasContentDescription("Summary") and hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "2 characters over the limit"))
        rule.onNodeWithContentDescription("2 characters over the limit").assert(hasText("12 / 10"))
    }

    @Test
    fun theToolbarFormatsTheSelection() {
        val state = MonarchRichTextEditorState("Ship it")
        state.select(0, 4)
        rule.setMonarchContent { MonarchRichTextEditor(label = "Notes", state = state) }
        rule.onNode((hasText("Bold") or hasContentDescription("Bold")) and hasClickAction())
            .performClick()
        assertEquals(listOf(bold(0, 4)), state.spans)
        rule.onNodeWithTextOrDescription("Undo").assertIsEnabled().performClick()
        assertTrue(state.spans.isEmpty())
    }

    @Test
    fun customToolsGoToTheApp() {
        var pressed: MonarchEditorTool? = null
        val mention = MonarchEditorTool.custom("mention", "Mention")
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState() },
                tools = listOf(listOf(MonarchEditorTool.Bold, MonarchEditorTool.CodeBlock, mention)),
                onTool = { pressed = it },
            )
        }
        rule.onNodeWithTextOrDescription("Mention").performClick()
        assertEquals(mention, pressed)
        // Built-in tools the editor does not apply (a code block) are left out of its toolbar.
        rule.onNodeWithTextOrDescription("Code block").assertDoesNotExist()
    }

    @Test
    fun readOnlyShowsTheDocumentWithoutTheBox() {
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState(Notes, NotesSpans) },
                readOnly = true,
            )
        }
        rule.onNodeWithTextOrDescription(Notes).assertExists()
        val link = rule.onNodeWithTextOrDescription(Notes).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single()
            .getLinkAnnotations(0, Notes.length).single().item
        assertEquals("https://example.com", (link as LinkAnnotation.Url).url)
        rule.onNodeWithContentDescription("Formatting").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun aDisabledEditorCannotBeUsed() {
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState("Saving") },
                enabled = false,
            )
        }
        rule.onNodeWithContentDescription("Notes").assertIsNotEnabled()
        rule.onNode((hasText("Bold") or hasContentDescription("Bold")) and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun aChosenMentionIsWrittenIntoTheText() {
        val state = MonarchRichTextEditorState("Thanks @an")
        val queries = mutableListOf<MonarchMentionQuery?>()
        var chosen: MonarchMentionItem? = null
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Reply",
                state = state,
                tools = emptyList(),
                mentionItems = listOf(Ana),
                onMentionSearch = { queries += it },
                onMention = { chosen = it },
            )
        }
        assertEquals("an", queries.last()?.text)
        rule.onNodeWithTextOrDescription("Ana Lima").performClick()
        assertEquals("Thanks @Ana Lima ", state.text)
        assertEquals(Ana, chosen)
        rule.waitForIdle()
        assertNull(queries.last())
    }

    @Test
    fun thePromptWrapsAtTheLargestFontScale() {
        val prompt = "Describe what changed in this release and why"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState() },
                prompt = prompt,
                tools = emptyList(),
            )
        }
        rule.onNodeWithTextOrDescription(prompt, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theCountSitsAtTheEndInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Column {
                MonarchRichTextEditor(
                    label = "Notes",
                    state = remember { MonarchRichTextEditorState("Hi") },
                    tools = emptyList(),
                    showCharacterCount = true,
                )
            }
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val field = rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
            val count = rule.onNodeWithContentDescription("2 characters").fetchSemanticsNode().boundsInRoot
            if (direction == LayoutDirection.Ltr) {
                assertTrue(count.left > field.center.x, "$direction: $count in $field")
            } else {
                assertTrue(count.right < field.center.x, "$direction: $count in $field")
            }
        }
    }

    @Test
    fun theCountStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRichTextEditor(
                label = "Notes",
                state = remember { MonarchRichTextEditorState("Hola") },
                maxLength = 10,
                labels = MonarchRichTextEditorLabels(
                    countOfLimit = { count, limit -> "$count de $limit" },
                    remaining = { "quedan $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("quedan 6").assert(hasText("4 de 10"))
    }

    @Test
    fun theColoursAndStylesComeFromTheTheme() {
        val typography = MonarchTypography.Default
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchRichTextEditorTokens
            assertEquals(c.bgSurface, t.box(c, enabled = true, error = false).container, theme.id)
            assertEquals(c.borderInput, t.box(c, enabled = true, error = false).border, theme.id)
            assertEquals(c.dangerStrong, t.box(c, enabled = true, error = true).border, theme.id)
            assertEquals(c.bgSubtle, t.box(c, enabled = false, error = true).container, theme.id)
            assertEquals(c.borderDefault, t.box(c, enabled = false, error = true).border, theme.id)
            assertEquals(c.textDisabled, t.box(c, enabled = false, error = false).content, theme.id)
            assertEquals(c.textSubtle, t.prompt(c), theme.id)
            assertEquals(c.textMuted, t.count(c, over = false), theme.id)
            assertEquals(c.dangerStrong, t.count(c, over = true), theme.id)
            assertEquals(c.accentStrong, t.cursor(c, error = false), theme.id)
            assertEquals(c.textLink, t.style(MonarchRichTextMark.Link, typography, c).color, theme.id)
            assertEquals(c.bgMuted, t.style(MonarchRichTextMark.Code, typography, c).background, theme.id)
        }
        val colors = MonarchColors(AllThemes.first().colors)
        val boldStyle = MonarchRichTextEditorTokens.style(MonarchRichTextMark.Bold, typography, colors)
        assertEquals(FontWeight.Bold, boldStyle.fontWeight)
        assertEquals(
            typography.title.fontSize,
            MonarchRichTextEditorTokens.style(MonarchRichTextMark.Heading1, typography, colors).fontSize,
        )
    }
}

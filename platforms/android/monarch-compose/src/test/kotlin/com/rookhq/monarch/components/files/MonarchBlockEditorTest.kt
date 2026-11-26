@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.files

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextMark
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextSpan
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuItem
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuQuery
import com.rookhq.monarch.components.files.blockeditor.BlockEditorActions
import com.rookhq.monarch.components.files.blockeditor.BlockEditorModel
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditor
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlock
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlock.Text as TextBlock
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlockType
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorLabels
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorState
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorTextStyle as Style
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorTokens
import com.rookhq.monarch.components.files.blockeditor.rememberMonarchBlockEditorState
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool
import com.rookhq.monarch.components.files.tableblock.MonarchTableBlockData
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.onNodeWithTextOrDescription
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val Plan = listOf(
    TextBlock("title", Style.Heading1, "Launch plan"),
    TextBlock(
        "intro",
        text = "Ship the export on Friday.",
        spans = listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 0, 4)),
    ),
    TextBlock("b1", Style.BulletedList, "Write the notes"),
    TextBlock("b2", Style.BulletedList, "Tell support"),
)

private val Mixed = listOf(
    TextBlock("n1", Style.NumberedList, "Freeze the branch"),
    TextBlock("n2", Style.NumberedList, "Tag the release"),
    TextBlock("c1", Style.Checklist, "QA sign-off", checked = true),
    TextBlock("q", Style.Quote, "Small releases, often."),
    TextBlock("code", Style.Code, "./gradlew release\n./gradlew publish"),
)

private val Notes = listOf(
    MonarchBlockEditorBlock.Callout("note", "The export keeps formatting.", tone = MonarchTone.Warning),
    MonarchBlockEditorBlock.Toggle("faq", "Why Friday?", body = "Support is staffed on the weekend.", expanded = true),
)

internal object MonarchBlockEditorFixtures : ComponentFixtures {
    override val id = "block-editor"
    override val fixtures = listOf(
        fixture("headings, formats and a list") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan),
                modifier = Modifier.height(400.dp),
            )
        },
        fixture("numbered list, checklist, quote and code") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Mixed),
                modifier = Modifier.height(400.dp),
                tools = emptyList(),
            )
        },
        fixture("callout and toggle") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Notes),
                modifier = Modifier.height(400.dp),
                tools = emptyList(),
            )
        },
        fixture("table") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(
                    listOf(
                        MonarchBlockEditorBlock.Table(
                            "t",
                            MonarchTableBlockData.of(listOf(listOf("Step", "Owner"), listOf("QA", "Ana"))),
                        ),
                    ),
                ),
                modifier = Modifier.height(400.dp),
                tools = emptyList(),
            )
        },
        fixture("a new embed asking for its link") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(listOf(MonarchBlockEditorBlock.Embed("e"))),
                modifier = Modifier.height(400.dp),
                tools = emptyList(),
            )
        },
        fixture("empty document with its placeholder") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(),
                modifier = Modifier.height(300.dp),
            )
        },
        fixture("read only") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(
                    Plan.take(2) + Notes + MonarchBlockEditorBlock.Embed("link", "https://example.com/report"),
                ),
                modifier = Modifier.height(400.dp),
                readOnly = true,
            )
        },
        fixture("disabled") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Mixed.take(3)),
                modifier = Modifier.height(400.dp),
                enabled = false,
            )
        },
        fixture("without handles") {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan),
                modifier = Modifier.height(400.dp),
                dragHandles = false,
                tools = emptyList(),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchBlockEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun texts(state: MonarchBlockEditorState) = state.blocks.map { BlockEditorModel.textOf(it) }

    private fun styles(state: MonarchBlockEditorState) = state.blocks.map { (it as? TextBlock)?.style }

    private fun type(actions: BlockEditorActions, state: MonarchBlockEditorState, id: String, text: String) {
        val editor = state.editor(state.block(id)!!)
        val caret = editor.selection.end
        val value = editor.value
        val next = value.text.substring(0, caret) + text + value.text.substring(caret)
        actions.onText(id, TextFieldValue(next, TextRange(caret + text.length)))
    }

    @Test
    fun linesSlicesAndJoinsKeepTheFormats() {
        val bold = MonarchRichTextSpan(MonarchRichTextMark.Bold, 2, 8)
        val segments = BlockEditorModel.lines("one\ntwo three", listOf(bold))
        assertEquals(listOf("one", "two three"), segments.map { it.text })
        assertEquals(listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 2, 3)), segments[0].spans)
        assertEquals(listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 0, 4)), segments[1].spans)
        assertEquals(1 to 2, BlockEditorModel.caretIn(segments, 6))
        assertEquals(0 to 3, BlockEditorModel.caretIn(segments, 3))
        val italic = listOf(MonarchRichTextSpan(MonarchRichTextMark.Italic, 0, 2))
        val (text, spans) = BlockEditorModel.joined("ab", emptyList(), "cd", italic)
        assertEquals("abcd", text)
        assertEquals(listOf(MonarchRichTextSpan(MonarchRichTextMark.Italic, 2, 4)), spans)
        assertEquals(Style.Heading2, BlockEditorModel.shortcut("##"))
        assertEquals(Style.Checklist, BlockEditorModel.shortcut("[ ]"))
        assertEquals(null, BlockEditorModel.shortcut("####"))
        assertEquals(2, BlockEditorModel.number(Mixed, 1))
        assertEquals(1, BlockEditorModel.number(Mixed, 0))
        val callout = BlockEditorModel.convert(Plan[1], MonarchBlockEditorBlockType.Callout)
        assertIs<MonarchBlockEditorBlock.Callout>(callout)
        assertEquals(Plan[1].spans, callout.spans)
        assertEquals(null, BlockEditorModel.convert(Plan[1], MonarchBlockEditorBlockType.Table))
        val code = BlockEditorModel.convert(Plan[1], MonarchBlockEditorBlockType.Code) as TextBlock
        assertEquals(emptyList(), code.spans)
    }

    @Test
    fun theStateInsertsMovesDuplicatesDeletesAndUndoes() {
        val state = MonarchBlockEditorState(Plan)
        assertFalse(state.canUndo)
        state.moveBlock("b2", -1)
        assertEquals(listOf("title", "intro", "b2", "b1"), state.blocks.map { it.id })
        state.duplicateBlock("title")
        assertEquals(5, state.blocks.size)
        assertEquals("Launch plan", BlockEditorModel.textOf(state.blocks[1]))
        assertNotEquals("title", state.blocks[1].id)
        state.deleteBlock("intro")
        state.insertBlock(TextBlock("title", text = "Same id"), afterId = "b1")
        assertEquals("Same id", BlockEditorModel.textOf(state.blocks.last()))
        assertNotEquals("title", state.blocks.last().id)
        state.undo()
        state.undo()
        state.undo()
        state.undo()
        assertEquals(Plan, state.blocks)
        assertFalse(state.canUndo)
        state.redo()
        assertEquals(listOf("title", "intro", "b2", "b1"), state.blocks.map { it.id })
        state.setBlocks(emptyList())
        assertEquals(1, state.blocks.size)
        assertFalse(state.canRedo)
        val only = MonarchBlockEditorState(listOf(TextBlock("x", text = "Last")))
        only.deleteBlock("x")
        assertEquals(listOf(""), texts(only))
    }

    @Test
    fun theSaverKeepsEveryKindOfBlock() {
        val all = Plan + Mixed + Notes +
            MonarchBlockEditorBlock.Table("t", MonarchTableBlockData.of(listOf(listOf("A", "B")), false)) +
            MonarchBlockEditorBlock.Embed("e", "https://example.com", "Report")
        val state = MonarchBlockEditorState(all)
        val scope = SaverScope { true }
        val saved = with(MonarchBlockEditorState.Saver) { scope.save(state) }!!
        val restored = MonarchBlockEditorState.Saver.restore(saved)!!
        assertEquals(all, restored.blocks)
    }

    @Test
    fun returnSplitsABlockAndEndsAnEmptyList() {
        val state = MonarchBlockEditorState(Plan)
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        val editor = state.editor(state.block("intro")!!)
        actions.onText("intro", editor.value.copy(selection = TextRange(8)))
        type(actions, state, "intro", "\n")
        assertEquals("Ship the", BlockEditorModel.textOf(state.blocks[1]))
        assertEquals(" export on Friday.", BlockEditorModel.textOf(state.blocks[2]))
        assertEquals(Style.Paragraph, (state.blocks[2] as TextBlock).style)
        assertEquals(listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 0, 4)), (state.blocks[1] as TextBlock).spans)
        assertEquals(state.blocks[2].id, state.focusRequest?.id)
        assertEquals(0, state.focusRequest?.caret)

        type(actions, state, "b2", "\n")
        val added = state.blocks.last() as TextBlock
        assertEquals(Style.BulletedList, added.style)
        assertEquals("", added.text)
        type(actions, state, added.id, "\n")
        assertEquals(Style.Paragraph, (state.blocks.last() as TextBlock).style)
        assertEquals(6, state.blocks.size)
        state.undo()
        assertEquals(Style.BulletedList, (state.blocks.last() as TextBlock).style)
    }

    @Test
    fun markdownPrefixesTurnAParagraphIntoAnotherKind() {
        val state = MonarchBlockEditorState(listOf(TextBlock("p")))
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        "## ".forEach { type(actions, state, "p", it.toString()) }
        assertEquals(Style.Heading2, (state.blocks.single() as TextBlock).style)
        assertEquals("", BlockEditorModel.textOf(state.blocks.single()))
        val limited = MonarchBlockEditorState(listOf(TextBlock("p")))
        val noQuotes = BlockEditorActions(limited, setOf(MonarchBlockEditorBlockType.Paragraph))
        "> ".forEach { type(noQuotes, limited, "p", it.toString()) }
        assertEquals(Style.Paragraph, (limited.blocks.single() as TextBlock).style)
        assertEquals("> ", BlockEditorModel.textOf(limited.blocks.single()))
    }

    @Test
    fun backspaceAtTheStartTurnsIntoAParagraphThenJoins() {
        val state = MonarchBlockEditorState(Plan)
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        state.editor(state.block("b1")!!).let { actions.onText("b1", it.value.copy(selection = TextRange(0))) }
        assertTrue(actions.onBackspaceAtStart("b1"))
        assertEquals(Style.Paragraph, (state.block("b1") as TextBlock).style)
        state.editor(state.block("b1")!!).let { actions.onText("b1", it.value.copy(selection = TextRange(0))) }
        assertTrue(actions.onBackspaceAtStart("b1"))
        assertEquals("Ship the export on Friday.Write the notes", BlockEditorModel.textOf(state.block("intro")!!))
        assertEquals(null, state.block("b1"))
        assertEquals("intro", state.focusRequest?.id)
        assertEquals(26, state.focusRequest?.caret)
        state.editor(state.block("title")!!).let { actions.onText("title", it.value.copy(selection = TextRange(3))) }
        assertFalse(actions.onBackspaceAtStart("title"))
    }

    @Test
    fun typingIsUndoneOneWordAtATime() {
        val state = MonarchBlockEditorState(listOf(TextBlock("p")))
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        "Hi there".forEach { type(actions, state, "p", it.toString()) }
        assertEquals("Hi there", BlockEditorModel.textOf(state.blocks.single()))
        state.undo()
        assertEquals("Hi ", BlockEditorModel.textOf(state.blocks.single()))
        state.undo()
        assertEquals("Hi", BlockEditorModel.textOf(state.blocks.single()))
        state.undo()
        assertEquals("", BlockEditorModel.textOf(state.blocks.single()))
        assertEquals("", state.editor(state.blocks.single()).text)
    }

    @Test
    fun theToolbarTurnsTheFocusedBlockAndFormatsItsSelection() {
        val state = MonarchBlockEditorState(Plan)
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        state.focusedBlockId = "intro"
        actions.applyTool(MonarchEditorTool.Blockquote, null)
        assertEquals(Style.Quote, (state.block("intro") as TextBlock).style)
        assertTrue(MonarchEditorTool.Blockquote in actions.activeTools())
        actions.applyTool(MonarchEditorTool.Blockquote, null)
        assertEquals(Style.Paragraph, (state.block("intro") as TextBlock).style)
        actions.applyTool(MonarchEditorTool.heading(2), null)
        assertEquals(Style.Heading2, (state.block("intro") as TextBlock).style)
        state.editor(state.block("intro")!!).let {
            actions.onText("intro", it.value.copy(selection = TextRange(5, 11)))
        }
        actions.applyTool(MonarchEditorTool.Italic, null)
        assertTrue(
            MonarchRichTextSpan(MonarchRichTextMark.Italic, 5, 11) in (state.block("intro") as TextBlock).spans,
        )
        assertTrue(MonarchEditorTool.Italic in actions.activeTools())
        var custom: MonarchEditorTool? = null
        actions.applyTool(MonarchEditorTool.custom("comment", "Comment"), onTool = { custom = it })
        assertEquals("custom:comment", custom?.id)
        state.focusedBlockId = null
        val disabled = actions.disabledTools(listOf(listOf(MonarchEditorTool.Undo, MonarchEditorTool.Paragraph)))
        assertTrue(MonarchEditorTool.Paragraph in disabled)
        assertTrue(MonarchEditorTool.Bold in disabled)
        assertFalse(MonarchEditorTool.Undo in disabled)
    }

    @Test
    fun theSlashMenuTurnsOrInsertsBlocks() {
        val state = MonarchBlockEditorState(listOf(TextBlock("p", text = "Notes"), TextBlock("q")))
        val actions = BlockEditorActions(state, MonarchBlockEditorBlockType.entries.toSet())
        state.focusedBlockId = "q"
        state.hasFocus = true
        type(actions, state, "q", "/hea")
        val (id, query) = actions.slashQuery()!!
        assertEquals("q" to "hea", id to query.text)
        actions.pickSlash(id, query, MonarchSlashMenuItem("heading-3", "Heading 3"), null)
        assertEquals(Style.Heading3, (state.block("q") as TextBlock).style)
        assertEquals("", BlockEditorModel.textOf(state.block("q")!!))

        state.focusedBlockId = "p"
        state.editor(state.block("p")!!).let { actions.onText("p", it.value.copy(selection = TextRange(5))) }
        type(actions, state, "p", "/")
        assertEquals(null, actions.slashQuery())
        actions.onText("p", TextFieldValue("/", TextRange(1)))
        val table = actions.slashQuery()!!.second
        actions.pickSlash("p", table, MonarchSlashMenuItem("table", "Table"), null)
        assertIs<MonarchBlockEditorBlock.Table>(state.blocks.first())

        var picked: MonarchSlashMenuItem? = null
        type(actions, state, "q", "/me")
        state.focusedBlockId = "q"
        val meeting = MonarchSlashMenuItem("meeting", "Meeting notes")
        actions.pickSlash("q", MonarchSlashMenuQuery('/', "me", 0, 3), meeting) { picked = it }
        assertEquals("meeting", picked?.id)
        assertEquals("", BlockEditorModel.textOf(state.block("q")!!))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchBlockEditorTokens.text(colors, enabled = true))
            assertEquals(colors.textDisabled, MonarchBlockEditorTokens.text(colors, enabled = false))
            assertEquals(colors.textMuted, MonarchBlockEditorTokens.quote(colors, enabled = true))
            assertEquals(colors.borderStrong, MonarchBlockEditorTokens.quoteBar(colors))
            assertEquals(colors.bgSubtle, MonarchBlockEditorTokens.codeFill(colors))
            assertEquals(colors.accentBorder, MonarchBlockEditorTokens.dragged(colors))
            assertNotEquals(MonarchBlockEditorTokens.text(colors, true), MonarchBlockEditorTokens.placeholder(colors))
        }
    }

    @Test
    fun blocksAreNamedAndHeadingsAreHeadings() {
        rule.setMonarchContent {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan),
                modifier = Modifier.height(460.dp),
            )
        }
        rule.onNode(hasContentDescription("Heading 1") and hasSetTextAction()).assert(isHeading())
        rule.onNodeWithContentDescription("Document").assertExists()
        rule.onNodeWithContentDescription("Block actions: Heading 1").assertTouchTargetAtLeast48dp(rule.density)
        rule.onAllNodes(hasContentDescription("Bulleted list") and hasSetTextAction()).assertCountEquals(2)
    }

    @Test
    fun typingReturnStartsANewBlockAndUndoRemovesIt() {
        val state = MonarchBlockEditorState(listOf(TextBlock("p", text = "First")))
        rule.setMonarchContent {
            MonarchBlockEditor(label = "Document", state = state, modifier = Modifier.height(460.dp))
        }
        rule.onNodeWithTextOrDescription("Undo").assertIsNotEnabled()
        rule.onNode(hasText("First") and hasSetTextAction()).performTextInput("\nSecond")
        rule.waitForIdle()
        assertEquals(listOf("First", "Second"), texts(state))
        rule.onNodeWithTextOrDescription("Undo").performClick()
        rule.waitForIdle()
        assertEquals(1, state.blocks.size)
    }

    @Test
    fun theSlashMenuOpensAndTurnsTheBlock() {
        val state = MonarchBlockEditorState(listOf(TextBlock("p")))
        rule.setMonarchContent {
            MonarchBlockEditor(label = "Document", state = state, modifier = Modifier.height(460.dp))
        }
        rule.onNode(hasSetTextAction()).performTextInput("/check")
        rule.waitForIdle()
        rule.onNodeWithTextOrDescription("Items to tick off", useUnmergedTree = true).assertExists()
        rule.onNodeWithTextOrDescription("Checklist").performClick()
        rule.waitForIdle()
        assertEquals(listOf(Style.Checklist), styles(state))
        rule.onNodeWithTextOrDescription("Items to tick off", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theHandleOpensTheBlockMenuAndMovesTheBlock() {
        val state = MonarchBlockEditorState(Plan)
        rule.setMonarchContent {
            MonarchBlockEditor(label = "Document", state = state, modifier = Modifier.height(460.dp))
        }
        val handle = rule.onNodeWithContentDescription("Block actions: Heading 1")
        val actions = handle.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Move up", "Move down"), actions.map { it.label })
        actions.last().action()
        rule.waitForIdle()
        assertEquals(listOf("intro", "title", "b1", "b2"), state.blocks.map { it.id })
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(polite and hasContentDescription("Heading 1 moved to position 2 of 4.")).assertExists()
        rule.onNodeWithContentDescription("Block actions: Heading 1").performClick()
        rule.waitForIdle()
        rule.onNodeWithTextOrDescription("Turn into").assert(isHeading())
        rule.onNodeWithTextOrDescription("Callout").performScrollTo().performClick()
        rule.waitForIdle()
        assertIs<MonarchBlockEditorBlock.Callout>(state.block("title"))
        rule.onAllNodes(hasContentDescription("Block actions: Bulleted list"))[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithTextOrDescription("Delete block").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(listOf("intro", "title", "b2"), state.blocks.map { it.id })
    }

    @Test
    fun theHandleIsOneFocusStopAndEnterOpensItsMenu() {
        val state = MonarchBlockEditorState(Plan)
        lateinit var focus: FocusManager
        rule.setMonarchContent {
            focus = LocalFocusManager.current
            MonarchBlockEditor(label = "Document", state = state, modifier = Modifier.height(460.dp))
        }
        val title = rule.onNode(hasSetTextAction() and hasText("Launch plan"))
        val intro = rule.onNode(hasSetTextAction() and hasText("Ship the export", substring = true))
        title.requestFocus()
        // The first stop after the title is the next block's handle (its semantics hide the focus state).
        rule.runOnIdle { focus.moveFocus(FocusDirection.Next) }
        title.assertIsNotFocused()
        intro.assertIsNotFocused()
        // The next stop is the block's text, not a second stop on the same handle.
        rule.runOnIdle { focus.moveFocus(FocusDirection.Next) }
        intro.assertIsFocused()
        rule.runOnIdle { focus.moveFocus(FocusDirection.Previous) }
        intro.assertIsNotFocused()
        rule.onRoot().performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        rule.onNodeWithTextOrDescription("Turn into").assertExists()
    }

    @Test
    fun aChecklistItemIsTickedByItsCheckbox() {
        val state = MonarchBlockEditorState(listOf(TextBlock("c", Style.Checklist, "QA sign-off")))
        rule.setMonarchContent {
            MonarchBlockEditor(
                label = "Document",
                state = state,
                modifier = Modifier.height(460.dp),
                tools = emptyList(),
            )
        }
        val toggleable = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)
        val box = rule.onNode(hasContentDescription("QA sign-off") and toggleable)
        box.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertTrue((state.blocks.single() as TextBlock).checked)
        box.assertIsOn()
    }

    @Test
    fun readOnlyShowsTextWithoutFieldsOrHandles() {
        rule.setMonarchContent {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan + Notes),
                modifier = Modifier.height(460.dp),
                readOnly = true,
            )
        }
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        rule.onAllNodes(hasContentDescription("Block actions", substring = true)).assertCountEquals(0)
        rule.onNodeWithTextOrDescription("Launch plan").assert(isHeading())
        rule.onNodeWithTextOrDescription("Undo").assertDoesNotExist()
    }

    @Test
    fun aDisabledEditorHasNoToolbarAndNoHandles() {
        rule.setMonarchContent {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan),
                modifier = Modifier.height(460.dp),
                enabled = false,
            )
        }
        val field = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
        rule.onNode(hasText("Launch plan") and field).assertIsNotEnabled()
        rule.onNodeWithTextOrDescription("Undo").assertDoesNotExist()
        rule.onAllNodes(hasContentDescription("Block actions", substring = true)).assertCountEquals(0)
    }

    @Test
    fun theHandleComesBeforeTheTextRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(Plan.take(1)),
                modifier = Modifier.height(300.dp),
                tools = emptyList(),
            )
        }
        rule.onNodeWithContentDescription("Block actions: Heading 1")
            .assertIsBefore(rule.onNode(hasSetTextAction()), LayoutDirection.Rtl)
    }

    @Test
    fun theTextFitsAtTheLargestFontSizeAndStringsCanBeReplaced() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBlockEditor(
                label = "Document",
                state = rememberMonarchBlockEditorState(),
                modifier = Modifier.height(460.dp),
                tools = emptyList(),
                labels = MonarchBlockEditorLabels(
                    placeholder = "Écrivez ici",
                    handle = { "Actions : $it" },
                    names = mapOf(MonarchBlockEditorBlockType.Paragraph to "Texte"),
                ),
            )
        }
        rule.onNodeWithTextOrDescription("Écrivez ici", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithContentDescription("Actions : Texte").assertExists()
    }
}

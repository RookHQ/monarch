package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.monarchui.R
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorState
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorTokens
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock as Block

/** The inline formats the text selection menu offers, with their built-in names. */
private val SelectionTools = listOf(
    MonarchEditorTool.Bold to R.string.monarch_editortoolbar_bold,
    MonarchEditorTool.Italic to R.string.monarch_editortoolbar_italic,
    MonarchEditorTool.Strikethrough to R.string.monarch_editortoolbar_strikethrough,
    MonarchEditorTool.Code to R.string.monarch_editortoolbar_code,
)

/** Keeps the block with the caret and whether the editor has focus. */
internal fun blockEditorFocusChanged(state: MonarchBlockEditorState, id: String, focused: Boolean) {
    if (focused) {
        state.focusedBlockId = id
        state.hasFocus = true
    } else if (state.focusedBlockId == id) {
        state.hasFocus = false
    }
}

/** Backspace at the start of a block, and Alt+Shift+Up or Down to move it. Returns whether the key was used. */
internal fun blockEditorKey(event: KeyEvent, id: String, actions: BlockEditorActions): Boolean {
    val moving = event.isAltPressed && event.isShiftPressed
    val offset = when {
        !moving -> 0
        event.key == Key.DirectionUp -> -1
        event.key == Key.DirectionDown -> 1
        else -> 0
    }
    return when {
        event.type != KeyEventType.KeyDown -> false
        event.key == Key.Backspace -> actions.onBackspaceAtStart(id)
        offset != 0 -> {
            actions.move(id, offset)
            true
        }
        else -> false
    }
}

/** Adds the inline formats to the system menu of selected text in block [id]. */
private fun Modifier.blockEditorSelectionMenu(
    id: String,
    editor: MonarchRichTextEditorState,
    actions: BlockEditorActions,
    tools: List<Pair<MonarchEditorTool, String>>,
): Modifier = appendTextContextMenuComponents {
    if (!editor.selection.collapsed) {
        separator()
        tools.forEach { (tool, label) ->
            item(key = tool.id, label = label) {
                actions.format(id, tool)
                close()
            }
        }
    }
}

/**
 * The text of a block: a `BasicTextField` over the block's
 * `MonarchRichTextEditorState`, with its formats drawn by a visual
 * transformation, Backspace at the start and Alt+Shift+Up or Down handled,
 * and the inline formats added to the system selection menu.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun BlockEditorTextField(
    block: Block,
    name: String,
    textStyle: TextStyle,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    heading: Boolean = false,
) {
    val editor = state.editor(block)
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val requester = remember { FocusRequester() }
    val spans = editor.spans
    val transformation = remember(spans, typography, colors) {
        if (spans.isEmpty()) {
            VisualTransformation.None
        } else {
            VisualTransformation { text ->
                TransformedText(
                    MonarchRichTextEditorTokens.annotated(text.text, spans, typography, colors),
                    OffsetMapping.Identity,
                )
            }
        }
    }
    val request = state.focusRequest
    LaunchedEffect(request) {
        if (request != null && request.id == block.id) {
            requester.requestFocus()
            request.caret?.let { blockEditorCaret(editor, it) }
            state.focusRequest = null
        }
    }
    val toolNames = SelectionTools.map { (tool, resource) ->
        tool to (config.labels.toolbar.tools[tool] ?: stringResource(resource))
    }
    val selectionMenu = if (config.bubbleMenu && actions.formats(block)) {
        Modifier.blockEditorSelectionMenu(block.id, editor, actions, toolNames)
    } else {
        Modifier
    }
    BasicTextField(
        value = editor.value,
        onValueChange = { actions.onText(block.id, it) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MonarchBlockEditorTokens.MinBlockHeight)
            .focusRequester(requester)
            .onFocusChanged { focus -> blockEditorFocusChanged(state, block.id, focus.isFocused) }
            .onPreviewKeyEvent { event -> config.editable && blockEditorKey(event, block.id, actions) }
            .then(selectionMenu)
            .semantics {
                contentDescription = name
                if (heading) heading()
            },
        enabled = config.enabled,
        readOnly = !config.editable,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = transformation,
        cursorBrush = SolidColor(MonarchBlockEditorTokens.cursor(colors)),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MonarchBlockEditorTokens.MinBlockHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (editor.text.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = MonarchBlockEditorTokens.placeholder(colors)),
                    )
                }
                inner()
            }
        },
    )
}

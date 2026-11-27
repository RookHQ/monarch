package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rookhq.monarch.components.collaboration.richtexteditor.applyLink
import com.rookhq.monarch.components.collaboration.richtexteditor.activeTools
import com.rookhq.monarch.components.collaboration.richtexteditor.applyTool
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuItem
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuQuery
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.components.files.blockeditor.BlockEditorModel as Model
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlock as Block
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlockType as Type
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorTextStyle as Style

/** What the editor's rows, toolbar and menus do to the document. Holds no state of its own. */
@Suppress("TooManyFunctions")
internal class BlockEditorActions(private val state: MonarchBlockEditorState, private val offered: Set<Type>) {
    private val blocks: List<Block> get() = state.blocks

    /** Takes a change from the text field of block [id]: typing, Return, a pasted text or a shortcut. */
    fun onText(id: String, value: TextFieldValue) {
        val block = state.block(id) ?: return
        val editor = state.editor(block)
        val old = editor.value
        if (old.text == value.text) {
            editor.onValueChange(value)
            return
        }
        val typed = typedChar(old.text, value)
        state.record(typingIn = if (typed != null && !typed.isWhitespace()) id else null)
        editor.onValueChange(value)
        when {
            block is Block.Text && block.style != Style.Code && '\n' in editor.text -> split(block)
            block is Block.Text && block.style == Style.Paragraph && typed == ' ' -> shortcut(block)
            block is Block.Toggle && '\n' in editor.text -> {
                val at = editor.text.indexOf('\n')
                editor.replace(at, at + 1, "", record = false)
                state.syncFromEditor(id)
                state.block(id)?.let { state.replaceQuietly(replaced(it, (it as Block.Toggle).copy(expanded = true))) }
            }
        }
        state.syncFromEditor(id)
    }

    /** The character typed at the caret when the change is one insertion, or `null`. */
    private fun typedChar(old: String, value: TextFieldValue): Char? {
        val caret = value.selection.end
        if (!value.selection.collapsed || value.text.length != old.length + 1 || caret == 0) return null
        return if (value.text.removeRange(caret - 1, caret) == old) value.text[caret - 1] else null
    }

    /** Cuts a text block at its line breaks into blocks; Return on an empty list item ends the list. */
    private fun split(block: Block.Text) {
        val editor = state.editor(block)
        val segments = Model.lines(editor.text, editor.spans)
        if (block.style.isList && segments.size == 2 && segments.all { it.text.isEmpty() }) {
            editor.setDocument("")
            state.replaceQuietly(replaced(block, block.copy(style = Style.Paragraph, text = "", spans = emptyList())))
            state.focusRequest = BlockEditorFocus(block.id, 0)
            return
        }
        val (index, caret) = Model.caretIn(segments, editor.selection.end)
        val first = segments.first()
        editor.setDocument(first.text, first.spans)
        val style = Model.nextStyle(block.style)
        val added = segments.drop(1).map { Block.Text(MonarchBlockEditorState.newId(), style, it.text, it.spans) }
        val at = blocks.indexOfFirst { it.id == block.id }
        val next = blocks.toMutableList()
        next[at] = block.copy(text = first.text, spans = first.spans)
        next.addAll(at + 1, added)
        state.replaceQuietly(next)
        state.focusRequest = BlockEditorFocus(if (index == 0) block.id else added[index - 1].id, caret)
    }

    /** Turns "## " typed at the start of a paragraph into a heading 2, and the other Markdown prefixes. */
    private fun shortcut(block: Block.Text) {
        val editor = state.editor(block)
        val caret = editor.selection.end
        val style = Model.shortcut(editor.text.substring(0, caret - 1)) ?: return
        if (style.type !in offered) return
        editor.replace(0, caret, "", record = false)
        val current = state.block(block.id) as? Block.Text ?: return
        state.replaceQuietly(replaced(current, current.copy(style = style, text = editor.text, spans = editor.spans)))
    }

    /** Backspace with the caret at the start of block [id]. Returns whether it was used. */
    fun onBackspaceAtStart(id: String): Boolean {
        val block = state.block(id) ?: return false
        if (state.editor(block).selection != TextRange(0)) return false
        return when {
            block is Block.Text && block.style != Style.Paragraph -> {
                turnInto(id, Type.Paragraph)
                true
            }
            block is Block.Callout || (block is Block.Toggle && block.body.isEmpty()) -> {
                turnInto(id, Type.Paragraph)
                true
            }
            block is Block.Text -> joinWithPrevious(block)
            else -> false
        }
    }

    private fun joinWithPrevious(block: Block.Text): Boolean {
        val at = blocks.indexOfFirst { it.id == block.id }
        val previous = blocks.getOrNull(at - 1) ?: return false
        val previousText = Model.textOf(previous)
        return when {
            previousText != null -> {
                val (text, spans) = Model.joined(previousText, Model.spansOf(previous), block.text, block.spans)
                state.change {
                    blocks.filterNot { it.id == block.id }
                        .map { if (it.id == previous.id) Model.withText(it, text, spans) else it }
                }
                state.focusRequest = BlockEditorFocus(previous.id, previousText.length)
                true
            }
            block.text.isEmpty() -> {
                state.change { blocks.filterNot { it.id == block.id } }
                true
            }
            else -> false
        }
    }

    /** Turns block [id] into [type], keeping its text. */
    fun turnInto(id: String, type: Type) {
        val block = state.block(id) ?: return
        val next = Model.convert(block, type) ?: return
        state.change { replaced(block, next) }
        state.focusRequest = BlockEditorFocus(id, null)
    }

    /** Ticks or clears a checklist item. */
    fun setChecked(id: String, checked: Boolean) {
        val block = state.block(id) as? Block.Text ?: return
        state.change { replaced(block, block.copy(checked = checked)) }
    }

    /** Sets the tone of a callout. */
    fun setTone(id: String, tone: MonarchTone) {
        val block = state.block(id) as? Block.Callout ?: return
        state.change { replaced(block, block.copy(tone = tone)) }
    }

    /** Shows or hides the content of a toggle. */
    fun setExpanded(id: String, expanded: Boolean) {
        val block = state.block(id) as? Block.Toggle ?: return
        state.change { replaced(block, block.copy(expanded = expanded)) }
    }

    /** Sets the content of a toggle, grouping typing into words. */
    fun setBody(id: String, body: String) {
        val block = state.block(id) as? Block.Toggle ?: return
        if (block.body == body) return
        val typing = body.length == block.body.length + 1 && !body.last().isWhitespace()
        state.record(typingIn = if (typing) "$id:body" else null)
        state.replaceQuietly(replaced(block, block.copy(body = body)))
    }

    /** Moves block [id] by [offset] places. */
    fun move(id: String, offset: Int) {
        state.moveBlock(id, offset)
        state.focusRequest = BlockEditorFocus(id, null)
    }

    /** Copies block [id] below itself. */
    fun duplicate(id: String) {
        state.duplicateBlock(id)
    }

    /** Deletes block [id]. */
    fun delete(id: String) {
        state.deleteBlock(id)
    }

    /** Replaces any block, such as a table after an edit or an embed once its link is known. */
    fun update(block: Block) {
        state.updateBlock(block)
    }

    /** The block the toolbar acts on: the focused one. */
    private fun focused(): Block? = state.focusedBlockId?.let(state::block)

    /** Applies a toolbar tool to the focused block. Custom tools go to [onTool]. */
    fun applyTool(tool: MonarchEditorTool, onTool: ((MonarchEditorTool) -> Unit)?) {
        val style = Model.style(tool)
        val block = focused()
        when {
            tool == MonarchEditorTool.Undo -> state.undo()
            tool == MonarchEditorTool.Redo -> state.redo()
            tool.id.startsWith(CUSTOM) -> onTool?.invoke(tool)
            block == null -> Unit
            style != null -> {
                val current = (block as? Block.Text)?.style
                val toggledOff = current == style && style != Style.Paragraph && !style.isHeading
                val target = if (toggledOff) Style.Paragraph else style
                turnInto(block.id, target.type)
            }
            tool in Model.InlineTools && tool != MonarchEditorTool.Link && formats(block) -> {
                val editor = state.editor(block)
                state.record()
                editor.applyTool(tool)
                state.syncFromEditor(block.id)
                state.focusRequest = BlockEditorFocus(block.id, null)
            }
        }
    }

    /** Applies the link form's address to the selection of the focused block; `null` removes the link. */
    fun applyLink(url: String?) {
        val block = focused() ?: return
        if (!formats(block)) return
        state.record()
        state.editor(block).applyLink(url)
        state.syncFromEditor(block.id)
    }

    /** Applies an inline format from the selection menu of block [id]. */
    fun format(id: String, tool: MonarchEditorTool) {
        val block = state.block(id) ?: return
        if (!formats(block)) return
        state.record()
        state.editor(block).applyTool(tool)
        state.syncFromEditor(id)
    }

    /** Whether a block takes inline formats: it has text and is not code. */
    fun formats(block: Block): Boolean =
        Model.textOf(block) != null && (block as? Block.Text)?.style != Style.Code

    /** The toolbar tools that are on for the focused block. */
    fun activeTools(): Set<MonarchEditorTool> {
        val block = focused() ?: return emptySet()
        val style = (block as? Block.Text)?.let { Model.tool(it.style) }
        val inline = if (formats(block)) {
            state.editor(block).activeTools().filterTo(mutableSetOf()) { it in Model.InlineTools }
        } else {
            emptySet()
        }
        return inline + listOfNotNull(style)
    }

    /** The toolbar tools that cannot be used now. */
    fun disabledTools(groups: List<List<MonarchEditorTool>>): Set<MonarchEditorTool> {
        val block = focused()
        val all = groups.flatten()
        return buildSet {
            if (!state.canUndo) add(MonarchEditorTool.Undo)
            if (!state.canRedo) add(MonarchEditorTool.Redo)
            if (block == null || Model.textOf(block) == null) addAll(all.filter { Model.style(it) != null })
            if (block == null || !formats(block)) addAll(Model.InlineTools)
        }
    }

    /** The / command typed in the focused text block, with the block's id, or `null`. */
    fun slashQuery(): Pair<String, MonarchSlashMenuQuery>? {
        val block = (focused() as? Block.Text)?.takeIf { state.hasFocus && it.style != Style.Code }
        return block?.let { MonarchSlashMenuQuery.find(state.editor(it).value) }?.let { block.id to it }
    }

    /** Runs a / menu entry in block [id]: removes the typed command, then turns or inserts the block. */
    fun pickSlash(
        id: String,
        query: MonarchSlashMenuQuery,
        item: MonarchSlashMenuItem,
        custom: ((MonarchSlashMenuItem) -> Unit)?,
    ) {
        val block = state.block(id) ?: return
        val editor = state.editor(block)
        state.record()
        editor.onValueChange(query.replacing(editor.value))
        state.syncFromEditor(id)
        val type = BlockEditorSlash.typeOf(item.id)
        val current = state.block(id)
        val converted = if (type != null && current != null) Model.convert(current, type) else null
        when {
            type == null -> custom?.invoke(item)
            current == null -> Unit
            converted != null -> {
                state.replaceQuietly(replaced(current, converted))
                state.focusRequest = BlockEditorFocus(id, null)
            }
            else -> insertFromSlash(id, current, type)
        }
    }

    private fun insertFromSlash(id: String, current: Block, type: Type) {
        val newId = MonarchBlockEditorState.newId()
        val inserted: Block = if (type == Type.Table) Block.Table(newId) else Block.Embed(newId)
        val at = blocks.indexOfFirst { it.id == id }
        val next = blocks.toMutableList()
        if (Model.textOf(current).isNullOrEmpty()) next[at] = inserted else next.add(at + 1, inserted)
        state.replaceQuietly(next)
        state.focusedBlockId = inserted.id
        if (inserted is Block.Embed) state.focusRequest = BlockEditorFocus(inserted.id, null)
    }

    private fun replaced(old: Block, new: Block): List<Block> = blocks.map { if (it.id == old.id) new else it }

    private companion object {
        const val CUSTOM = "custom:"
    }
}

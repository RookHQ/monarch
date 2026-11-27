package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextEditorState
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextMark
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextSpan
import com.rookhq.monarch.components.files.tableblock.MonarchTableBlockData
import com.rookhq.monarch.foundation.MonarchTone
import java.util.UUID
import com.rookhq.monarch.components.files.blockeditor.BlockEditorModel as Model
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlock as Block
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorTextStyle as Style

/** A block the editor should focus once it is on screen, with the caret at [caret] (`null` keeps it). */
internal class BlockEditorFocus(val id: String, val caret: Int?)

/**
 * The document of a `MonarchBlockEditor`: its [blocks], the block being
 * edited and the undo history.
 *
 * Create it with [rememberMonarchBlockEditorState] so it survives
 * configuration changes, read [blocks] to save the document and call
 * [setBlocks] to load another. The editing functions ([insertBlock],
 * [updateBlock], [moveBlock], [duplicateBlock], [deleteBlock]) are what the
 * editor itself uses; each is one undoable change. Typing in one block is
 * grouped into one change per word.
 *
 * @param blocks the initial document. An empty list starts with one empty paragraph; repeated ids are replaced.
 */
@Stable
@Suppress("TooManyFunctions")
public class MonarchBlockEditorState(blocks: List<Block> = emptyList()) {
    private var document: List<Block> by mutableStateOf(normalized(blocks))
    private var undoStack: List<List<Block>> by mutableStateOf(emptyList())
    private var redoStack: List<List<Block>> by mutableStateOf(emptyList())
    private var lastTyping: String? = null
    private val editors = mutableMapOf<String, MonarchRichTextEditorState>()

    /** The blocks of the document, in order. */
    public val blocks: List<Block> get() = document

    /** The id of the block that has, or last had, the caret; the toolbar and the / menu act on it. */
    public var focusedBlockId: String? by mutableStateOf(null)
        internal set

    /** Whether the caret is in a text of the editor now. */
    internal var hasFocus: Boolean by mutableStateOf(false)

    /** A block to focus once it is on screen. */
    internal var focusRequest: BlockEditorFocus? by mutableStateOf(null)

    /** Whether there is a change to undo. */
    public val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** Whether there is an undone change to redo. */
    public val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Replaces the document and clears the undo history. */
    public fun setBlocks(blocks: List<Block>) {
        replaceDocument(normalized(blocks))
        undoStack = emptyList()
        redoStack = emptyList()
        lastTyping = null
        focusRequest = null
    }

    /** Undoes the last change: typing in a block, a format, a new, moved or deleted block. */
    public fun undo() {
        val last = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + listOf(document)).takeLast(HISTORY)
        undoStack = undoStack.dropLast(1)
        replaceDocument(last)
        lastTyping = null
    }

    /** Redoes the last undone change. */
    public fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + listOf(document)).takeLast(HISTORY)
        redoStack = redoStack.dropLast(1)
        replaceDocument(next)
        lastTyping = null
    }

    /**
     * Inserts [block] after the block [afterId], or at the end when it is
     * `null` or not found, and moves the caret into it when it has text. A
     * block whose id is already used gets a new id.
     */
    public fun insertBlock(block: Block, afterId: String? = focusedBlockId) {
        val fresh = if (document.any { it.id == block.id }) withId(block, newId()) else block
        val at = document.indexOfFirst { it.id == afterId }.let { if (it < 0) document.size else it + 1 }
        change { document.toMutableList().apply { add(at, fresh) } }
        if (Model.textOf(fresh) != null) focusRequest = BlockEditorFocus(fresh.id, 0)
    }

    /** Replaces the block with the same id as [block]. Nothing happens when there is none. */
    public fun updateBlock(block: Block) {
        val at = document.indexOfFirst { it.id == block.id }
        if (at < 0 || document[at] == block) return
        change { document.toMutableList().apply { set(at, block) } }
        editors[block.id]?.let { syncEditor(it, block) }
    }

    /** Moves the block [id] by [offset] places, up when negative. The move stops at either end. */
    public fun moveBlock(id: String, offset: Int) {
        val from = document.indexOfFirst { it.id == id }
        if (from < 0) return
        moveBlockTo(from, from + offset)
    }

    /** Moves the block at [from] to [to]. */
    internal fun moveBlockTo(from: Int, to: Int) {
        val target = to.coerceIn(0, document.lastIndex)
        if (from !in document.indices || from == target) return
        change { Model.moved(document, from, target) }
    }

    /** Inserts a copy of the block [id], with a new id, right after it. */
    public fun duplicateBlock(id: String) {
        val block = document.firstOrNull { it.id == id } ?: return
        insertBlock(withId(block, newId()), id)
    }

    /** Deletes the block [id]. The last block of a document is emptied to a paragraph instead. */
    public fun deleteBlock(id: String) {
        val at = document.indexOfFirst { it.id == id }
        if (at < 0) return
        change {
            if (document.size == 1) listOf(Block.Text(newId())) else document.filterNot { it.id == id }
        }
        if (focusedBlockId == id) focusedBlockId = null
    }

    /* ------------------------------------------------------------ editing, used by the editor */

    /** The block [id], or `null`. */
    internal fun block(id: String): Block? = document.firstOrNull { it.id == id }

    /** The text state of a block that has text, kept in step with the block. */
    internal fun editor(block: Block): MonarchRichTextEditorState = editors.getOrPut(block.id) {
        MonarchRichTextEditorState(Model.textOf(block).orEmpty(), Model.spansOf(block))
    }

    /** Records the document before a change, grouping typing in one block into words. */
    internal fun record(typingIn: String? = null) {
        if (typingIn == null || typingIn != lastTyping) push()
        lastTyping = typingIn
    }

    /** Applies [transform] as one undoable change. */
    internal fun change(transform: () -> List<Block>) {
        val next = transform()
        if (next == document) return
        push()
        lastTyping = null
        replaceDocument(next)
    }

    /** Writes the text of the editor of [id] back into its block, without recording. */
    internal fun syncFromEditor(id: String) {
        val at = document.indexOfFirst { it.id == id }
        val editor = editors[id]
        if (at < 0 || editor == null) return
        val block = document[at]
        val next = Model.withText(block, editor.text, editor.spans)
        if (next != block) document = document.toMutableList().apply { set(at, next) }
    }

    /** Replaces blocks without recording; the caller recorded first. */
    internal fun replaceQuietly(next: List<Block>) {
        replaceDocument(next)
    }

    private fun push() {
        undoStack = (undoStack + listOf(document)).takeLast(HISTORY)
        redoStack = emptyList()
    }

    private fun replaceDocument(next: List<Block>) {
        document = next
        val ids = next.mapTo(mutableSetOf()) { it.id }
        editors.keys.retainAll(ids)
        next.forEach { block -> editors[block.id]?.let { syncEditor(it, block) } }
    }

    private fun syncEditor(editor: MonarchRichTextEditorState, block: Block) {
        val text = Model.textOf(block) ?: return
        val spans = Model.spansOf(block)
        if (editor.text != text || editor.spans != spans) editor.setDocument(text, spans)
    }

    /** Saves the blocks; the undo history and the caret are not kept. */
    public companion object {
        private const val HISTORY = 100

        /** A new block id: a random UUID. */
        public fun newId(): String = UUID.randomUUID().toString()

        /** The saver of `rememberSaveable`. */
        public val Saver: Saver<MonarchBlockEditorState, Any> = listSaver<MonarchBlockEditorState, List<Any?>>(
            save = { state -> state.blocks.map(BlockEditorSaving::save) },
            restore = { saved -> MonarchBlockEditorState(saved.map(BlockEditorSaving::restore)) },
        )

        private fun normalized(blocks: List<Block>): List<Block> {
            val seen = mutableSetOf<String>()
            val unique = blocks.map { block ->
                if (seen.add(block.id)) block else withId(block, newId()).also { seen.add(it.id) }
            }
            return unique.ifEmpty { listOf(Block.Text(newId())) }
        }

        private fun withId(block: Block, id: String): Block = when (block) {
            is Block.Text -> block.copy(id = id)
            is Block.Callout -> block.copy(id = id)
            is Block.Toggle -> block.copy(id = id)
            is Block.Table -> block.copy(id = id)
            is Block.Embed -> block.copy(id = id)
        }
    }
}

/** Turns blocks into lists of plain values for the saved instance state, and back. */
internal object BlockEditorSaving {
    private const val SPAN_FIELDS = 4

    fun save(block: Block): List<Any?> = when (block) {
        is Block.Text -> listOf("text", block.id, block.style.name, block.text, saveSpans(block.spans), block.checked)
        is Block.Callout -> listOf("callout", block.id, block.tone.name, block.text, saveSpans(block.spans))
        is Block.Toggle -> listOf("toggle", block.id, block.body, block.text, saveSpans(block.spans), block.expanded)
        is Block.Table -> {
            val rows = ArrayList(block.data.rows.map { ArrayList(it) })
            listOf("table", block.id, block.data.hasHeaderRow, rows)
        }
        is Block.Embed -> listOf("embed", block.id, block.url, block.caption)
    }

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    fun restore(saved: List<Any?>): Block {
        val id = saved[1] as String
        return when (saved[0]) {
            "text" -> Block.Text(
                id = id,
                style = Style.valueOf(saved[2] as String),
                text = saved[3] as String,
                spans = restoreSpans(saved[4] as List<Any?>),
                checked = saved[5] as Boolean,
            )
            "callout" -> Block.Callout(
                id = id,
                text = saved[3] as String,
                spans = restoreSpans(saved[4] as List<Any?>),
                tone = MonarchTone.valueOf(saved[2] as String),
            )
            "toggle" -> Block.Toggle(
                id = id,
                text = saved[3] as String,
                spans = restoreSpans(saved[4] as List<Any?>),
                body = saved[2] as String,
                expanded = saved[5] as Boolean,
            )
            "table" -> Block.Table(id, MonarchTableBlockData.of(saved[3] as List<List<String>>, saved[2] as Boolean))
            else -> Block.Embed(id, saved[2] as String, saved[3] as String?)
        }
    }

    private fun saveSpans(spans: List<MonarchRichTextSpan>): List<Any?> =
        ArrayList(spans.flatMap { listOf(it.mark.name, it.start, it.end, it.url) })

    private fun restoreSpans(saved: List<Any?>): List<MonarchRichTextSpan> = saved.chunked(SPAN_FIELDS).map { fields ->
        MonarchRichTextSpan(
            mark = MonarchRichTextMark.valueOf(fields[0] as String),
            start = fields[1] as Int,
            end = fields[2] as Int,
            url = fields[3] as String?,
        )
    }
}

/**
 * Creates and remembers a [MonarchBlockEditorState] that survives
 * configuration changes and process death.
 *
 * @param blocks the initial document.
 */
@Composable
public fun rememberMonarchBlockEditorState(
    blocks: List<Block> = emptyList(),
): MonarchBlockEditorState =
    rememberSaveable(saver = MonarchBlockEditorState.Saver) { MonarchBlockEditorState(blocks) }

/** The caret of [editor] moved to [caret]. */
internal fun blockEditorCaret(editor: MonarchRichTextEditorState, caret: Int) {
    editor.onValueChange(editor.value.copy(selection = TextRange(caret.coerceIn(0, editor.text.length))))
}

package dev.monarchui.components.collaboration.richtexteditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorLists as Lists
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorModel as Model

/** The text, spans and selection at one point in the history of a document. */
private data class RichTextSnapshot(val value: TextFieldValue, val spans: List<MonarchRichTextSpan>)

/**
 * The document of a `MonarchRichTextEditor`: plain [text], formatted
 * [spans] and the selection, with the editor's undo history.
 *
 * Create it with [rememberMonarchRichTextEditorState] so it survives
 * configuration changes, read [text] and [spans] to save the document, and
 * call [setDocument] to load another. Lists are text prefixes ("• ", "1. ")
 * in [text]; headings are spans over whole paragraphs.
 *
 * @param text the initial text.
 * @param spans the initial formatting. Spans outside the text are clamped, touching spans are joined.
 */
@Stable
public class MonarchRichTextEditorState(text: String = "", spans: List<MonarchRichTextSpan> = emptyList()) {
    internal var value: TextFieldValue by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        private set

    /** The formatting of [text], sorted by start. */
    public var spans: List<MonarchRichTextSpan> by mutableStateOf(Model.normalize(spans, text))
        private set

    /** Formats chosen at a caret before typing; `null` when typing continues the formats around it. */
    internal var pending: Set<MonarchRichTextMark>? by mutableStateOf(null)
        private set

    private var undoStack by mutableStateOf(listOf<RichTextSnapshot>())
    private var redoStack by mutableStateOf(listOf<RichTextSnapshot>())
    private var typingEnd: Int? = null

    /** The plain text of the document. */
    public val text: String get() = value.text

    /** The selection, or the caret when it is collapsed. */
    public val selection: TextRange get() = value.selection

    /** Whether there is a change to undo. */
    public val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** Whether there is an undone change to redo. */
    public val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Replaces the document and clears the undo history. The caret goes to the end. */
    public fun setDocument(text: String, spans: List<MonarchRichTextSpan> = emptyList()) {
        value = TextFieldValue(text, TextRange(text.length))
        this.spans = Model.normalize(spans, text)
        pending = null
        undoStack = emptyList()
        redoStack = emptyList()
        typingEnd = null
    }

    /** Undoes the last change: typing, a format or a list. */
    public fun undo() {
        val last = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + snapshot()).takeLast(HISTORY)
        undoStack = undoStack.dropLast(1)
        restore(last)
    }

    /** Redoes the last undone change. */
    public fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + snapshot()).takeLast(HISTORY)
        redoStack = redoStack.dropLast(1)
        restore(next)
    }

    private fun snapshot() = RichTextSnapshot(value, spans)

    private fun restore(snapshot: RichTextSnapshot) {
        value = snapshot.value
        spans = snapshot.spans
        pending = null
        typingEnd = null
    }

    private fun pushHistory() {
        undoStack = (undoStack + snapshot()).takeLast(HISTORY)
        redoStack = emptyList()
    }

    /**
     * Takes a change from the text field: moves the spans, continues or ends
     * a list on Return, applies pending formats to typed text and stops at
     * [maxLength].
     */
    internal fun onValueChange(new: TextFieldValue, maxLength: Int? = null) {
        val old = value
        val edit = Model.diff(old.text, new.text, new.selection.end)
        if (edit == null) {
            if (new.selection != old.selection) pending = null
            value = new
            return
        }
        val limited = limit(old.text, new, edit, maxLength) ?: return
        val result = continueList(old.text, limited.first, limited.second)
        val finalEdit = result.edit
        val typing = finalEdit.removed == 0 && finalEdit.inserted == 1 && !result.text[finalEdit.start].isWhitespace()
        if (!(typing && typingEnd == finalEdit.start)) pushHistory()
        typingEnd = if (typing) finalEdit.start + 1 else null
        val composition = if (result.changed || limited.first.text != new.text) null else new.composition
        commit(old.text, result.text, finalEdit, TextFieldValue(result.text, result.selection, composition))
    }

    private fun limit(
        old: String,
        new: TextFieldValue,
        edit: RichTextEdit,
        maxLength: Int?,
    ): Pair<TextFieldValue, RichTextEdit>? {
        if (maxLength == null || new.text.length <= maxLength || new.text.length <= old.length) return new to edit
        val allowed = (maxLength - (old.length - edit.removed)).coerceIn(0, edit.inserted)
        if (allowed == 0 && edit.removed == 0) return null
        val inserted = new.text.substring(edit.start, edit.start + allowed)
        val text = old.substring(0, edit.start) + inserted + old.substring(edit.end)
        return TextFieldValue(text, TextRange(edit.start + allowed)) to edit.copy(inserted = allowed)
    }

    private data class ListResult(
        val text: String,
        val edit: RichTextEdit,
        val selection: TextRange,
        val changed: Boolean,
    )

    private fun continueList(old: String, new: TextFieldValue, edit: RichTextEdit): ListResult {
        val paragraph = Model.paragraph(old, edit.start)
        val isReturn = edit.removed == 0 && edit.inserted == 1 && new.text[edit.start] == '\n'
        val prefix = if (isReturn) Lists.listPrefix(old, paragraph) else null
        if (prefix == null || edit.start < paragraph.first + prefix.length) {
            return ListResult(new.text, edit, new.selection, false)
        }
        val emptyItem = paragraph.last + 1 - paragraph.first == prefix.length && edit.start == paragraph.last + 1
        return if (emptyItem) {
            val text = old.removeRange(paragraph.first, paragraph.first + prefix.length)
            ListResult(text, RichTextEdit(paragraph.first, prefix.length, 0), TextRange(paragraph.first), true)
        } else {
            val next = Lists.nextPrefix(prefix)
            val text = new.text.substring(0, edit.start + 1) + next + new.text.substring(edit.start + 1)
            val caret = TextRange(edit.start + 1 + next.length)
            ListResult(text, RichTextEdit(edit.start, 0, 1 + next.length), caret, true)
        }
    }

    private fun commit(oldText: String, text: String, edit: RichTextEdit, newValue: TextFieldValue) {
        var next = MonarchRichTextEditorShift.shift(spans, edit, oldText.substring(edit.start, edit.end))
        val formats = pending
        if (formats != null && edit.inserted > 0) {
            val end = edit.start + edit.inserted
            Model.Inline.forEach { mark ->
                next = Model.remove(next, mark, edit.start, end)
                if (mark in formats) next = next + MonarchRichTextSpan(mark, edit.start, end)
            }
        }
        pending = null
        spans = Model.normalize(next, text)
        value = newValue
    }

    /** Replaces the text from [start] to [end] with [replacement], as one undoable change unless [record] is off. */
    internal fun replace(start: Int, end: Int, replacement: String, record: Boolean = true) {
        if (record) pushHistory()
        typingEnd = null
        val old = value.text
        val text = old.substring(0, start) + replacement + old.substring(end)
        val edit = RichTextEdit(start, end - start, replacement.length)
        val selection = TextRange(edit.map(value.selection.start), edit.map(value.selection.end))
        pending = null
        commit(old, text, edit, TextFieldValue(text, selection))
    }

    /** Replaces the whole value, such as a completed mention, as one undoable change. */
    internal fun replaceValue(new: TextFieldValue) {
        val edit = Model.diff(value.text, new.text, new.selection.end) ?: return
        pushHistory()
        typingEnd = null
        pending = null
        commit(value.text, new.text, edit, new)
    }

    /** Replaces the spans, as one undoable change unless [record] is off. */
    internal fun updateSpans(next: List<MonarchRichTextSpan>, record: Boolean = true) {
        if (record) pushHistory()
        typingEnd = null
        spans = Model.normalize(next, value.text)
    }

    /** Sets the formats that typing at the caret will use. */
    internal fun updatePending(formats: Set<MonarchRichTextMark>) {
        pending = formats
    }

    /** Saves the text, the spans and the selection; the undo history is not kept. */
    public companion object {
        private const val HISTORY = 100
        private const val FIELDS_PER_SPAN = 4
        private const val SAVED_HEADER = 3

        /** The saver of `rememberSaveable`. */
        public val Saver: Saver<MonarchRichTextEditorState, Any> = listSaver<MonarchRichTextEditorState, Any?>(
            save = { state ->
                listOf<Any?>(state.text, state.selection.start, state.selection.end) +
                    state.spans.flatMap { listOf(it.mark.name, it.start, it.end, it.url) }
            },
            restore = { saved ->
                val text = saved[0] as String
                val spans = saved.drop(SAVED_HEADER).chunked(FIELDS_PER_SPAN).map { fields ->
                    MonarchRichTextSpan(
                        mark = MonarchRichTextMark.valueOf(fields[0] as String),
                        start = fields[1] as Int,
                        end = fields[2] as Int,
                        url = fields.last() as String?,
                    )
                }
                MonarchRichTextEditorState(text, spans).apply {
                    val start = (saved[1] as Int).coerceIn(0, text.length)
                    val end = (saved[2] as Int).coerceIn(0, text.length)
                    value = value.copy(selection = TextRange(start, end))
                }
            },
        )
    }
}

/**
 * Creates and remembers a [MonarchRichTextEditorState] that survives
 * configuration changes and process death.
 *
 * @param text the initial text.
 * @param spans the initial formatting.
 */
@Composable
public fun rememberMonarchRichTextEditorState(
    text: String = "",
    spans: List<MonarchRichTextSpan> = emptyList(),
): MonarchRichTextEditorState = rememberSaveable(saver = MonarchRichTextEditorState.Saver) {
    MonarchRichTextEditorState(text, spans)
}

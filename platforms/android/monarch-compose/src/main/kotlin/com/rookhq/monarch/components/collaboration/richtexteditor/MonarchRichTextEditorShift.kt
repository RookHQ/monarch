package com.rookhq.monarch.components.collaboration.richtexteditor

/** How MonarchRichTextEditor moves spans when the text changes. */
internal object MonarchRichTextEditorShift {
    /**
     * [spans] moved by [edit]. Text typed at the end of a bold, italic,
     * struck-through or code span continues it; text typed inside any span
     * joins it; a heading also takes text typed at its start. [removedText]
     * is the text the edit removed.
     */
    fun shift(spans: List<MonarchRichTextSpan>, edit: RichTextEdit, removedText: String): List<MonarchRichTextSpan> =
        spans.mapNotNull { span -> shiftSpan(span, edit, removedText) }

    private fun shiftSpan(span: MonarchRichTextSpan, edit: RichTextEdit, removedText: String): MonarchRichTextSpan? {
        if (removesHeading(span, edit, removedText)) return null
        val includes = includesInsertion(span, edit)
        val newStart = newStart(span.start, edit, includes)
        val newEnd = newEnd(span.end, edit, includes)
        return when {
            newEnd < newStart -> null
            newEnd == newStart && !span.mark.isHeading -> null
            else -> span.copy(start = newStart, end = newEnd)
        }
    }

    /** A heading removed together with a line break goes; one emptied inside its paragraph stays for typing. */
    private fun removesHeading(span: MonarchRichTextSpan, edit: RichTextEdit, removedText: String): Boolean {
        val inside = span.start >= edit.start && span.end <= edit.end
        return span.mark.isHeading && inside && edit.removed > 0 && '\n' in removedText
    }

    /** Whether the inserted text joins [span]. */
    private fun includesInsertion(span: MonarchRichTextSpan, edit: RichTextEdit): Boolean {
        val s = span.start
        val e = span.end
        val heading = span.mark.isHeading
        return edit.inserted > 0 && when {
            s < edit.start && e > edit.start -> true
            s < edit.start && e == edit.start -> span.mark in MonarchRichTextEditorModel.Continuing || heading
            heading -> s in edit.start..edit.end
            else -> false
        }
    }

    private fun newStart(s: Int, edit: RichTextEdit, includes: Boolean): Int = when {
        s < edit.start -> s
        includes -> edit.start
        s < edit.end -> edit.start + edit.inserted
        else -> s + edit.inserted - edit.removed
    }

    private fun newEnd(e: Int, edit: RichTextEdit, includes: Boolean): Int = when {
        e > edit.end -> e + edit.inserted - edit.removed
        e > edit.start -> edit.start + if (includes) edit.inserted else 0
        includes && e == edit.start -> e + edit.inserted
        else -> e
    }
}

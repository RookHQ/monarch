package com.rookhq.monarch.components.collaboration.richtexteditor

/** The list rules of MonarchRichTextEditor: lists are text prefixes at the start of paragraphs. */
internal object MonarchRichTextEditorLists {
    /** The text prefix of a bulleted list item. */
    const val BULLET: String = "• "

    /** The text prefix of an unticked task item. */
    const val TASK: String = "\u2610 "

    /** The text prefix of a ticked task item. */
    const val TASK_DONE: String = "\u2611 "

    /** The text prefix of a quoted paragraph: a bar, as the web draws at the start of a quote. */
    const val QUOTE: String = "\u258E "

    private val OrderedPrefix = Regex("^(\\d{1,4})\\. ")

    /** The list prefix that starts [paragraph] of [text], "• " or "3. ", or `null`. */
    fun listPrefix(text: String, paragraph: IntRange): String? {
        val line = text.substring(paragraph.first, paragraph.last + 1)
        return when {
            line.startsWith(BULLET) -> BULLET
            line.startsWith(TASK) -> TASK
            line.startsWith(TASK_DONE) -> TASK_DONE
            line.startsWith(QUOTE) -> QUOTE
            else -> OrderedPrefix.find(line)?.value
        }
    }

    /** The prefix of the item after one that starts with [prefix]: the same bullet or the next number. */
    fun nextPrefix(prefix: String): String =
        OrderedPrefix.find(prefix)?.groupValues?.get(1)?.toIntOrNull()?.let { "${it + 1}. " } ?: prefix

    /** Whether [prefix] is a numbered list prefix. */
    fun isOrdered(prefix: String?): Boolean = prefix != null && OrderedPrefix.matches(prefix)
}

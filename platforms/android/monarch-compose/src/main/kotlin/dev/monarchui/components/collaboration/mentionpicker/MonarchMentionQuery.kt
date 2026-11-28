package dev.monarchui.components.collaboration.mentionpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The mention being typed: from the trigger character to the caret.
 *
 * Find it with [find] as the text or the caret changes, pass it to
 * `MonarchMentionPicker`, and complete it with [replacing] when a suggestion
 * is chosen. The range belongs to the text it was found in; find it again
 * after the text changes.
 *
 * @property trigger the character that started it, `@` or `#`.
 * @property text what has been typed after the trigger.
 * @property start the index of the trigger in the text.
 * @property end the index of the caret, the end of the query.
 */
@Immutable
public class MonarchMentionQuery(
    public val trigger: Char,
    public val text: String,
    public val start: Int,
    public val end: Int,
) {
    /**
     * [text] with this query replaced by [replacement], usually an item's
     * token followed by a space. When [replacement] ends with a space and the
     * text after the query starts with one, that space is removed, so a
     * mention typed mid-sentence does not leave two. A query whose range does
     * not fit [text] leaves it unchanged.
     */
    public fun replacing(text: String, replacement: String): String {
        if (start < 0 || end > text.length || start > end) return text
        return text.substring(0, start) + replacement + rest(text, replacement)
    }

    /**
     * [value] with this query replaced by [replacement], and the caret placed
     * after it. See the other overload.
     */
    public fun replacing(value: TextFieldValue, replacement: String): TextFieldValue {
        val replaced = replacing(value.text, replacement)
        if (replaced == value.text) return value
        return value.copy(text = replaced, selection = TextRange(start + replacement.length))
    }

    private fun rest(text: String, replacement: String): String {
        val after = text.substring(end)
        return if (replacement.endsWith(' ') && after.startsWith(' ')) after.substring(1) else after
    }

    override fun equals(other: Any?): Boolean = other is MonarchMentionQuery &&
        other.trigger == trigger && other.text == text && other.start == start && other.end == end

    override fun hashCode(): Int = listOf(trigger, text, start, end).hashCode()

    override fun toString(): String = "MonarchMentionQuery($trigger$text, $start..$end)"

    /** Finds the mention being typed. */
    public companion object {
        private const val MAX_WORD = 40

        /**
         * The mention being typed before [caret] in [text], as on the web: a
         * trigger at the start or after white space or "(", followed by at
         * most two words of at most 40 characters. A trigger inside a word,
         * such as in an email address, is not a mention. `null` when there is
         * none.
         *
         * @param text the text of the field.
         * @param caret the index of the caret; the end of the text by default.
         * @param triggers the characters that open the picker; add `#` for channels.
         */
        public fun find(
            text: String,
            caret: Int = text.length,
            triggers: Set<Char> = setOf('@'),
        ): MonarchMentionQuery? {
            if (triggers.isEmpty() || caret < 0 || caret > text.length) return null
            val alternatives = triggers.joinToString("|") { Regex.escape(it.toString()) }
            val stop = triggers.joinToString("") { classEscape(it) }
            val word = "[^\\s$stop]{0,$MAX_WORD}"
            val pattern = Regex("(?:^|[\\s(])($alternatives)($word(?: $word)?)$")
            val match = pattern.find(text.substring(0, caret)) ?: return null
            val trigger = match.groupValues[1].single()
            val typed = match.groupValues[2]
            return MonarchMentionQuery(trigger, typed, caret - typed.length - 1, caret)
        }

        /**
         * The mention being typed in [value], at its caret. `null` while a
         * range of text is selected.
         */
        public fun find(value: TextFieldValue, triggers: Set<Char> = setOf('@')): MonarchMentionQuery? =
            if (value.selection.collapsed) find(value.text, value.selection.end, triggers) else null

        private fun classEscape(char: Char): String = if (char in "\\^-[]") "\\$char" else char.toString()
    }
}

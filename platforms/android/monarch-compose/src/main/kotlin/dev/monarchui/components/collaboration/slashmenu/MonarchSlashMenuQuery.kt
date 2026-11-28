package dev.monarchui.components.collaboration.slashmenu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The slash command being typed: from the trigger to the caret.
 *
 * Find it with [find] as the text or the caret changes, pass it to
 * `MonarchSlashMenu`, and replace it with [replacing] when an entry is
 * chosen. The range belongs to the text it was found in; find it again after
 * the text changes.
 *
 * @property trigger the character that started it, `/` by default.
 * @property text what has been typed after the trigger.
 * @property start the index of the trigger in the text.
 * @property end the index of the caret, the end of the query.
 */
@Immutable
public class MonarchSlashMenuQuery(
    public val trigger: Char,
    public val text: String,
    public val start: Int,
    public val end: Int,
) {
    /**
     * [text] with this query, trigger included, replaced by [replacement]:
     * the markup or text the chosen entry inserts, or `""` to remove the
     * command. A query whose range does not fit [text] leaves it unchanged.
     */
    public fun replacing(text: String, replacement: String = ""): String {
        if (start < 0 || end > text.length || start > end) return text
        return text.substring(0, start) + replacement + text.substring(end)
    }

    /**
     * [value] with this query replaced by [replacement], and the caret placed
     * after it. See the other overload.
     */
    public fun replacing(value: TextFieldValue, replacement: String = ""): TextFieldValue {
        if (start < 0 || end > value.text.length || start > end) return value
        return value.copy(text = replacing(value.text, replacement), selection = TextRange(start + replacement.length))
    }

    override fun equals(other: Any?): Boolean = other is MonarchSlashMenuQuery &&
        other.trigger == trigger && other.text == text && other.start == start && other.end == end

    override fun hashCode(): Int = listOf(trigger, text, start, end).hashCode()

    override fun toString(): String = "MonarchSlashMenuQuery($trigger$text, $start..$end)"

    /** Finds the slash command being typed. */
    public companion object {
        /** The longest command that keeps the menu open, as on the web. */
        public const val DEFAULT_MAX_LENGTH: Int = 24

        /**
         * The command being typed before [caret] in [text], as the web's
         * findSlashQuery: [trigger] followed by letters, digits or hyphens.
         * A space, punctuation or more than [maxLength] characters after the
         * trigger ends it. `null` when there is none.
         *
         * @param text the text of the field.
         * @param caret the index of the caret; the end of the text by default.
         * @param trigger the character that opens the menu.
         * @param position where the trigger may be typed.
         * @param maxLength the longest command that keeps the menu open.
         */
        public fun find(
            text: String,
            caret: Int = text.length,
            trigger: Char = '/',
            position: MonarchSlashMenuTriggerPosition = MonarchSlashMenuTriggerPosition.LineStart,
            maxLength: Int = DEFAULT_MAX_LENGTH,
        ): MonarchSlashMenuQuery? {
            if (caret < 0 || caret > text.length || maxLength < 0) return null
            val lineStart = text.lastIndexOf('\n', caret - 1) + 1
            val line = text.substring(lineStart, caret)
            val lead = if (position == MonarchSlashMenuTriggerPosition.LineStart) "^" else "(?:^|\\s)"
            val pattern = Regex("$lead${Regex.escape(trigger.toString())}([\\p{L}\\p{N}-]{0,$maxLength})$")
            val match = pattern.find(line) ?: return null
            val typed = match.groupValues[1]
            return MonarchSlashMenuQuery(trigger, typed, caret - typed.length - 1, caret)
        }

        /**
         * The command being typed in [value], at its caret. `null` while a
         * range of text is selected.
         */
        public fun find(
            value: TextFieldValue,
            trigger: Char = '/',
            position: MonarchSlashMenuTriggerPosition = MonarchSlashMenuTriggerPosition.LineStart,
            maxLength: Int = DEFAULT_MAX_LENGTH,
        ): MonarchSlashMenuQuery? = if (value.selection.collapsed) {
            find(value.text, value.selection.end, trigger, position, maxLength)
        } else {
            null
        }
    }
}

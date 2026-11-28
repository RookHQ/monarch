package dev.monarchui.components.files.pageoutline

import androidx.compose.runtime.Immutable

/**
 * The word count and reading time of a document, shown under a
 * `MonarchPageOutline`.
 *
 * @property words the number of words.
 * @property minutes the reading time in minutes.
 */
@Immutable
public class MonarchPageOutlineStats(public val words: Int, public val minutes: Int) {
    /** Counting from text. */
    public companion object {
        /** The reading speed used by [fromText], in words per minute, as on the web. */
        public const val WORDS_PER_MINUTE: Int = 220

        /**
         * Counts the words of [text] (runs of characters between white space)
         * and the minutes to read them at [WORDS_PER_MINUTE], at least one.
         *
         * @param text the document's plain text.
         */
        public fun fromText(text: String): MonarchPageOutlineStats {
            val words = text.split(Regex("\\s+")).count { it.isNotEmpty() }
            val minutes = maxOf(1, Math.round(words.toFloat() / WORDS_PER_MINUTE))
            return MonarchPageOutlineStats(words, minutes)
        }
    }
}

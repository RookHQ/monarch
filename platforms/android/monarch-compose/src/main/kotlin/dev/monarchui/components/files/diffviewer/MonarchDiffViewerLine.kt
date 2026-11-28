package dev.monarchui.components.files.diffviewer

import androidx.compose.runtime.Immutable

/**
 * One line of a line diff, with its number on each side. Build the lines of two
 * texts with [diff]; `MonarchDiffViewer` does this itself.
 *
 * @property kind whether the line is in both texts, only the later or only the earlier.
 * @property text the text of the line, without the line break.
 * @property oldNumber the line number in the earlier text, from 1: set for same and removed lines.
 * @property newNumber the line number in the later text, from 1: set for same and added lines.
 */
@Immutable
public class MonarchDiffViewerLine(
    public val kind: MonarchDiffViewerLineKind,
    public val text: String,
    public val oldNumber: Int? = null,
    public val newNumber: Int? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchDiffViewerLine &&
            other.kind == kind &&
            other.text == text &&
            other.oldNumber == oldNumber &&
            other.newNumber == newNumber

    override fun hashCode(): Int = ((kind.hashCode() * HASH + text.hashCode()) * HASH + (oldNumber ?: 0)) * HASH +
        (newNumber ?: 0)

    override fun toString(): String = "MonarchDiffViewerLine($kind, \"$text\", old=$oldNumber, new=$newNumber)"

    /** The line diff and the splitting of a text into lines. */
    public companion object {
        private const val HASH = 31

        /**
         * The line diff from [before] to [after] along a longest common subsequence, numbered on both sides.
         * When a removal and an addition are both possible, the removal comes first. Inputs of more than a
         * million line pairs show every line of [before] removed and every line of [after] added.
         *
         * @param before the lines of the earlier text.
         * @param after the lines of the later text.
         */
        public fun diff(before: List<String>, after: List<String>): List<MonarchDiffViewerLine> {
            var old = 0
            var new = 0
            return diffViewerSequences(before, after, DiffViewerMaxLinePairs).map { op ->
                when (op.kind) {
                    MonarchDiffViewerLineKind.Same ->
                        MonarchDiffViewerLine(op.kind, op.value, oldNumber = ++old, newNumber = ++new)
                    MonarchDiffViewerLineKind.Removed -> MonarchDiffViewerLine(op.kind, op.value, oldNumber = ++old)
                    MonarchDiffViewerLineKind.Added -> MonarchDiffViewerLine(op.kind, op.value, newNumber = ++new)
                }
            }
        }

        /**
         * The lines of [text]: split on `\n`, `\r\n` and `\r`. An empty text has no lines.
         *
         * @param text the text to split.
         */
        public fun lines(text: String): List<String> =
            if (text.isEmpty()) emptyList() else text.replace(LineBreak, "\n").split('\n')

        private val LineBreak = Regex("\r\n?")
    }
}

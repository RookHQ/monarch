package dev.monarchui.components.intelligence.aiinlinediff

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.monarchui.theme.MonarchColors

/** The styled text of a MonarchAiInlineDiff and the character range of each change in it. */
internal class AiInlineDiffText(val text: AnnotatedString, val ranges: List<IntRange>) {
    /** The index of the change at character [offset], or `null` when the offset is unchanged text. */
    fun changeAt(offset: Int): Int? = ranges.indexOfFirst { offset in it }.takeIf { it >= 0 }

    companion object {
        /** Styles [segments]: removed and added runs get their span style; records each change's range. */
        fun of(
            segments: List<MonarchAiDiffSegment>,
            changes: List<MonarchAiInlineDiffChange>,
            colors: MonarchColors,
        ): AiInlineDiffText {
            val owner = HashMap<Int, Int>()
            changes.forEachIndexed { change, item -> item.segmentIndexes.forEach { index -> owner[index] = change } }
            val starts = IntArray(changes.size) { -1 }
            val ends = IntArray(changes.size)
            val text = buildAnnotatedString {
                segments.forEachIndexed { index, segment ->
                    val change = owner[index]
                    if (change != null && starts[change] < 0) starts[change] = length
                    if (segment.type == MonarchAiDiffSegmentType.Equal) {
                        append(segment.text)
                    } else {
                        withStyle(MonarchAiInlineDiffTokens.span(segment.type, colors)) { append(segment.text) }
                    }
                    if (change != null) ends[change] = length
                }
            }
            val ranges = changes.indices.map { starts[it].coerceAtLeast(0) until ends[it] }
            return AiInlineDiffText(text, ranges)
        }

        /** The text as TalkBack reads it: removed and added runs after "Deleted:" and "Inserted:". */
        fun spoken(segments: List<MonarchAiDiffSegment>, deleted: String, inserted: String): String =
            segments.joinToString("") { segment ->
                when (segment.type) {
                    MonarchAiDiffSegmentType.Equal -> segment.text
                    MonarchAiDiffSegmentType.Delete -> " $deleted ${segment.text} "
                    MonarchAiDiffSegmentType.Insert -> " $inserted ${segment.text} "
                }
            }.replace(Spaces, " ").trim()

        /** The active change after a move by [step] from [active] among [total] changes, wrapping at the ends. */
        fun move(active: Int, step: Int, total: Int): Int = if (total == 0) 0 else (active + step).mod(total)

        private val Spaces = Regex("\\s+")
    }
}

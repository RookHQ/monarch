package com.rookhq.monarch.components.intelligence.aiinlinediff

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerLineKind
import com.rookhq.monarch.components.files.diffviewer.diffViewerWords

/**
 * A run of text in a suggestion: unchanged, added or removed. A list of
 * segments in reading order is the input of `MonarchAiInlineDiff`.
 *
 * @property type whether the text stays, is added or is removed.
 * @property text the text of the run.
 * @property id groups neighbouring added and removed runs into one change. `null` groups neighbours that also
 * have no id.
 */
@Immutable
public data class MonarchAiDiffSegment(
    public val type: MonarchAiDiffSegmentType,
    public val text: String,
    public val id: String? = null,
) {
    /** Builders and helpers for lists of segments. */
    public companion object {
        /**
         * The word diff of two texts as segments: words and spaces that stay are [MonarchAiDiffSegmentType.Equal],
         * the others are removed or added.
         *
         * @param before the current text.
         * @param after the suggested text.
         */
        public fun diffWords(before: String, after: String): List<MonarchAiDiffSegment> =
            diffViewerWords(before, after).map { segment ->
                val type = when (segment.kind) {
                    MonarchDiffViewerLineKind.Same -> MonarchAiDiffSegmentType.Equal
                    MonarchDiffViewerLineKind.Added -> MonarchAiDiffSegmentType.Insert
                    MonarchDiffViewerLineKind.Removed -> MonarchAiDiffSegmentType.Delete
                }
                MonarchAiDiffSegment(type, segment.text)
            }

        /**
         * Groups segments into changes. A change is a run of neighbouring removed and added segments that share
         * an id (or all have none); unchanged text ends a change.
         *
         * @param segments the segments in reading order.
         */
        public fun changes(segments: List<MonarchAiDiffSegment>): List<MonarchAiInlineDiffChange> {
            val out = ArrayList<MonarchAiInlineDiffChange>()
            var open = false
            var openId: String? = null
            segments.forEachIndexed { index, segment ->
                if (segment.type == MonarchAiDiffSegmentType.Equal) {
                    open = false
                    return@forEachIndexed
                }
                if (!open || segment.id != openId) {
                    open = true
                    openId = segment.id
                    out += MonarchAiInlineDiffChange(segment.id ?: index.toString(), emptyList(), "", "")
                }
                val last = out.last()
                out[out.lastIndex] = if (segment.type == MonarchAiDiffSegmentType.Delete) {
                    last.copy(segmentIndexes = last.segmentIndexes + index, deleted = last.deleted + segment.text)
                } else {
                    last.copy(segmentIndexes = last.segmentIndexes + index, inserted = last.inserted + segment.text)
                }
            }
            return out
        }

        /**
         * Applies a decision to one change and returns the new segments: accepting keeps the added text and
         * drops the removed text, rejecting does the opposite. Neighbouring unchanged text without an id is
         * joined. Use it to update the segments from `onAccept` and `onReject`.
         *
         * @param segments the current segments.
         * @param id the id of the change, as in [MonarchAiInlineDiffChange.id].
         * @param accept `true` to accept the change, `false` to reject it.
         */
        public fun resolve(
            segments: List<MonarchAiDiffSegment>,
            id: String,
            accept: Boolean,
        ): List<MonarchAiDiffSegment> {
            val change = changes(segments).firstOrNull { it.id == id } ?: return segments
            val indexes = change.segmentIndexes.toSet()
            val keep = if (accept) MonarchAiDiffSegmentType.Insert else MonarchAiDiffSegmentType.Delete
            val out = ArrayList<MonarchAiDiffSegment>()
            segments.forEachIndexed { index, segment ->
                val next = when {
                    index !in indexes -> segment
                    segment.type == keep -> MonarchAiDiffSegment(MonarchAiDiffSegmentType.Equal, segment.text)
                    else -> null
                }
                if (next == null || next.text.isEmpty()) return@forEachIndexed
                val last = out.lastOrNull()
                val joins = next.type == MonarchAiDiffSegmentType.Equal &&
                    last?.type == MonarchAiDiffSegmentType.Equal && last.id == null && next.id == null
                if (joins) out[out.lastIndex] = last.copy(text = last.text + next.text) else out += next
            }
            return out
        }
    }
}

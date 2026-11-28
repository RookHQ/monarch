package dev.monarchui.components.intelligence.aiinlinediff

import androidx.compose.runtime.Immutable

/**
 * One suggested change of a `MonarchAiInlineDiff`: a run of neighbouring removed and added segments.
 *
 * @property id the segments' [MonarchAiDiffSegment.id], or the index of the change's first segment as a string.
 * @property segmentIndexes the indexes of the change's segments in the list it was made from.
 * @property deleted the removed text, joined.
 * @property inserted the added text, joined.
 */
@Immutable
public data class MonarchAiInlineDiffChange(
    public val id: String,
    public val segmentIndexes: List<Int>,
    public val deleted: String,
    public val inserted: String,
)

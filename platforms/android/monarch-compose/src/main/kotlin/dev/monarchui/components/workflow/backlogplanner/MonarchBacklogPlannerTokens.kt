package dev.monarchui.components.workflow.backlogplanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchBacklogPlanner: sizes, colours and the keys of its lazy entries. */
internal object MonarchBacklogPlannerTokens {
    /** The space between rows, and between a header and its rows. */
    val RowGap: Dp = MonarchTokens.Space.space0.dp

    /** The space above a bucket's header, after the previous bucket. */
    val BucketGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between the parts of a row or a header line. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The height of the capacity bar. */
    val BarHeight: Dp = MonarchTokens.Space.space1.dp

    /** The space between the lines of a header. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The space inside a row at the start and end. */
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space1.dp

    /** The space inside a row above and below. */
    val RowPaddingVertical: Dp = MonarchTokens.Space.space1.dp

    /** The space inside the text of an empty bucket. */
    val EmptyPadding: Dp = MonarchTokens.Space.space3.dp

    /** The border width of a row. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The most the list scrolls in one frame while a dragged row is held past its edge. */
    val AutoScrollStep: Dp = MonarchTokens.Space.space4.dp

    /** The z order of the dragged row, above the others. */
    const val DRAGGED_Z: Float = 1f

    /** The angle of the collapse icon while the bucket is open. */
    const val EXPANDED_ROTATION: Float = 90f

    /** The separator of the parts of a summary. */
    const val SEPARATOR: String = " · "

    /** The fill of a row. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The border of a row: the accent border while dragged, so the state is not the shadow alone. */
    fun border(dragged: Boolean, colors: MonarchColors): Color =
        if (dragged) colors.accentBorder else colors.borderSubtle

    /** The colour of quiet text: subtitles, summaries, points and the empty text. */
    fun meta(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the over-capacity note, which is also written as text. */
    fun overCapacity(colors: MonarchColors): Color = colors.dangerStrong

    /** The key of a bucket's header in the lazy list. */
    fun headerKey(bucketId: String): String = "h:$bucketId"

    /** The key of an item's row in the lazy list. */
    fun itemKey(key: String): String = "i:$key"

    /** The key of an empty bucket's text in the lazy list. */
    fun emptyKey(bucketId: String): String = "e:$bucketId"

    /** The key of a bucket's footer in the lazy list. */
    fun footerKey(bucketId: String): String = "f:$bucketId"

    /** The filled fraction of the capacity meter, from zero to one. */
    fun fill(points: Int, capacity: Int): Float = (points.toFloat() / capacity.coerceAtLeast(1)).coerceIn(0f, 1f)
}

package com.rookhq.monarch.components.files.columnsblock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and layout logic of MonarchColumnsBlock, built from primitives only. */
internal object MonarchColumnsBlockTokens {
    /** The gap between columns side by side. */
    val ColumnGap: Dp = MonarchTokens.Space.space6.dp

    /** The gap between stacked columns. */
    val StackedGap: Dp = MonarchTokens.Space.space3.dp

    /** The web stacks below 30rem; one rem is the base type size. */
    private const val STACK_BELOW_REM: Float = 30f

    /** The width under which the columns stack, at the default font size. */
    val StackBelow: Dp = (MonarchTokens.FontSize.base * STACK_BELOW_REM).dp

    const val MIN_RATIO: Float = 0.2f
    const val MAX_RATIO: Float = 0.8f
    const val DEFAULT_RATIO: Float = 0.5f

    /** Keeps the share of the first of two columns between 0.2 and 0.8; 0.5 when it is not a number. */
    fun clampRatio(ratio: Float): Float = if (ratio.isFinite()) ratio.coerceIn(MIN_RATIO, MAX_RATIO) else DEFAULT_RATIO

    /**
     * Whether the columns stack. The threshold grows with the font size
     * setting, as 30rem grows with the browser's text size on the web.
     */
    fun stacks(availableWidth: Dp, fontScale: Float): Boolean = availableWidth < StackBelow * maxOf(1f, fontScale)

    /**
     * The widths of side-by-side columns in pixels. Two columns share the
     * width minus the gap at [ratio]; any other count is equal.
     */
    fun widths(count: Int, total: Int, gap: Int, ratio: Float): List<Int> {
        if (count <= 0) return emptyList()
        val content = (total - gap * (count - 1)).coerceAtLeast(0)
        if (count == 2) {
            val first = (content * clampRatio(ratio)).toInt()
            return listOf(first, content - first)
        }
        val each = content / count
        return List(count) { index -> if (index == count - 1) content - each * (count - 1) else each }
    }
}

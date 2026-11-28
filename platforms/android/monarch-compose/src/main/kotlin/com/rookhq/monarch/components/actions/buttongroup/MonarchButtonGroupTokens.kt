package com.rookhq.monarch.components.actions.buttongroup

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchButtonGroup, built from primitives only. */
internal object MonarchButtonGroupTokens {
    private val Corner = CornerSize(MonarchTokens.Radius.md.dp)
    private val Square = CornerSize(0)
    private val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The gap between buttons that are not attached. */
    val DetachedGap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The shape of the button at [index] of [count]: the outer corners of the
     * group are rounded and the inner ones square. Start and end follow the
     * layout direction. Detached buttons keep all their corners.
     */
    fun shape(index: Int, count: Int, orientation: Orientation, attached: Boolean): RoundedCornerShape {
        if (!attached) return RoundedCornerShape(Corner)
        val first = index == 0
        val last = index == count - 1
        return if (orientation == Orientation.Horizontal) {
            RoundedCornerShape(
                topStart = if (first) Corner else Square,
                topEnd = if (last) Corner else Square,
                bottomEnd = if (last) Corner else Square,
                bottomStart = if (first) Corner else Square,
            )
        } else {
            RoundedCornerShape(
                topStart = if (first) Corner else Square,
                topEnd = if (first) Corner else Square,
                bottomEnd = if (last) Corner else Square,
                bottomStart = if (last) Corner else Square,
            )
        }
    }

    /**
     * The space between neighbours. Attached secondary buttons overlap by
     * their border, so they share one line; attached buttons without a border
     * keep a hairline gap; detached buttons keep an 8dp gap.
     */
    fun spacing(variant: MonarchButtonVariant, attached: Boolean): Dp = when {
        !attached -> DetachedGap
        variant == MonarchButtonVariant.Secondary || variant == MonarchButtonVariant.DangerSubtle -> -BorderWidth
        else -> BorderWidth
    }

    /**
     * The widths of the buttons of a row. With [fill], or when the natural
     * widths do not fit in [available], the width is shared equally, so long
     * labels wrap inside their share.
     */
    fun rowWidths(natural: List<Int>, available: Int, gap: Int, fill: Boolean): List<Int> {
        val count = natural.size
        val room = available - gap * (count - 1)
        val fits = available == Int.MAX_VALUE || natural.sum() <= room
        if (!fill && fits) return natural
        val share = (room / count).coerceAtLeast(0)
        return List(count) { share }
    }
}

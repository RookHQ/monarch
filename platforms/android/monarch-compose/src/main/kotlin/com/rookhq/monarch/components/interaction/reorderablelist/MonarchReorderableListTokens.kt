package com.rookhq.monarch.components.interaction.reorderablelist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure reordering rules of MonarchReorderableList. */
internal object MonarchReorderableListTokens {
    /** The space between rows. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The space between the handle, the content and the move buttons. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a row at the start (the handle brings its own 48dp target). */
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space1.dp

    /** The space after the content: a control at the end (a switch) keeps the compact-row gap to the border. */
    val RowPaddingEnd: Dp = MonarchTokens.Space.space3.dp

    /** The space inside a row above and below. */
    val RowPaddingVertical: Dp = MonarchTokens.Space.space1.dp

    /** The border width of a row. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The most the list scrolls in one frame while a dragged row is held past its edge. */
    val AutoScrollStep: Dp = MonarchTokens.Space.space4.dp

    /** The z order of the dragged row, above the others. */
    const val DRAGGED_Z: Float = 1f

    /** The fill of a row. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The border of a row: the accent border while dragged, so the state is not the shadow alone. */
    fun border(dragged: Boolean, colors: MonarchColors): Color =
        if (dragged) colors.accentBorder else colors.borderSubtle

    /** A copy of [list] with the item at [from] moved to [to], clamped to the list. */
    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        return list.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** One row as laid out: its index, the offset of its top and its height, in pixels. */
    data class Slot(val index: Int, val offset: Int, val size: Int)

    /**
     * The index the dragged row moves to: the row whose span holds the middle
     * of the dragged row at [draggedTop], or `null` to stay.
     */
    fun target(dragged: Slot, draggedTop: Float, slots: List<Slot>): Int? {
        val middle = draggedTop + dragged.size / 2f
        return slots.firstOrNull { it.index != dragged.index && middle >= it.offset && middle <= it.offset + it.size }
            ?.index
    }
}

package com.rookhq.monarch.components.interaction.sortablegrid

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleAxis
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure reordering rules of MonarchSortableGrid. */
internal object MonarchSortableGridTokens {
    /** The smallest tile width for adaptive columns, before font scaling. Two columns on most phones. */
    val MinItemWidth: Dp = (MonarchTokens.Space.space20 + MonarchTokens.Space.space20).dp

    /** The space between tiles, across and down. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a tile. */
    val TilePadding: Dp = MonarchTokens.Space.space1.dp

    /** The space between the handle and the content of a tile. */
    val PartGap: Dp = MonarchTokens.Space.space1.dp

    /** The border width of a tile. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The most the grid scrolls in one frame while a dragged tile is held past its edge. */
    val AutoScrollStep: Dp = MonarchTokens.Space.space4.dp

    /** The z order of the dragged tile, above the others. */
    const val DRAGGED_Z: Float = 1f

    /** Fixed [columns] when given, otherwise as many columns of [minItemWidth], grown with [fontScale], as fit. */
    fun cells(columns: Int?, minItemWidth: Dp, fontScale: Float): GridCells =
        columns?.let { GridCells.Fixed(it.coerceAtLeast(1)) } ?: GridCells.Adaptive(minItemWidth * fontScale)

    /** The fill of a tile. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The border of a tile: the accent border while dragged, so the state is not the shadow alone. */
    fun border(dragged: Boolean, colors: MonarchColors): Color =
        if (dragged) colors.accentBorder else colors.borderSubtle

    /** A copy of [list] with the item at [from] moved to [to], clamped to the list. */
    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        return list.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** The handle's axis: up and down only in one column, every direction otherwise. */
    fun axis(columns: Int): MonarchDragHandleAxis =
        if (columns <= 1) MonarchDragHandleAxis.Vertical else MonarchDragHandleAxis.Both

    /**
     * The index a tile at [index] moves to one step in [direction], in a grid
     * of [columns] columns and [count] tiles, or `null` when that is past the
     * first or last place. Backward and forward follow the reading order.
     */
    fun step(index: Int, direction: MonarchDragHandleDirection, columns: Int, count: Int): Int? {
        val across = columns.coerceAtLeast(1)
        val to = when (direction) {
            MonarchDragHandleDirection.Up -> index - across
            MonarchDragHandleDirection.Down -> index + across
            MonarchDragHandleDirection.Backward -> index - 1
            MonarchDragHandleDirection.Forward -> index + 1
        }
        return to.takeIf { it in 0 until count }
    }

    /** One tile as laid out: its index, the top-left corner and the size, in pixels. */
    data class Slot(val index: Int, val x: Int, val y: Int, val width: Int, val height: Int) {
        /** Whether [point] is inside the tile. */
        fun contains(point: Offset): Boolean =
            point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height
    }

    /** The index the dragged tile moves to: the tile under its centre at [draggedCorner], or `null` to stay. */
    fun target(dragged: Slot, draggedCorner: Offset, slots: List<Slot>): Int? {
        val centre = draggedCorner + Offset(dragged.width / 2f, dragged.height / 2f)
        return slots.firstOrNull { it.index != dragged.index && it.contains(centre) }?.index
    }
}

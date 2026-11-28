package dev.monarchui.components.interaction.sortablegrid

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.monarchui.internal.MonarchDragAutoScroll

/**
 * The drag in progress in a `MonarchSortableGrid`: which tile is held, the
 * order shown while it moves, and how far it is from where the layout put it.
 * Positions are those of `LazyGridLayoutInfo`, the same space as the pointer.
 * The order is committed once, when the drag ends.
 */
@Stable
internal class MonarchSortableGridDragState(private val gridState: LazyGridState) {
    /** The key of the held tile, or `null` when nothing is dragged. */
    var key: Any? by mutableStateOf(null)
        private set

    /** The order shown while dragging, as indices into the items, or `null`. */
    var order: List<Int>? by mutableStateOf(null)
        private set

    /** The index of the held tile before the drag. */
    var from: Int = -1
        private set

    private var delta by mutableStateOf(Offset.Zero)
    private var startCorner = Offset.Zero

    /** Whether a tile is held. */
    val active: Boolean get() = key != null

    /** Where the held tile is in the order shown. */
    val position: Int get() = order?.indexOf(from) ?: from

    private fun heldInfo(): LazyGridItemInfo? = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    private fun LazyGridItemInfo.slot() =
        MonarchSortableGridTokens.Slot(index, offset.x, offset.y, size.width, size.height)

    /** How far the held tile is drawn from its slot, so it follows the finger. */
    fun translation(): Offset {
        val held = heldInfo() ?: return Offset.Zero
        return startCorner + delta - Offset(held.offset.x.toFloat(), held.offset.y.toFloat())
    }

    /** Starts a drag on the tile at [point] when [movable] allows it. Returns whether a drag started. */
    fun start(point: Offset, count: Int, movable: (Int) -> Boolean): Boolean {
        val tile = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.slot().contains(point) }
        if (tile == null || tile.index >= count || !movable(tile.index)) return false
        key = tile.key
        from = tile.index
        order = List(count) { it }
        startCorner = Offset(tile.offset.x.toFloat(), tile.offset.y.toFloat())
        delta = Offset.Zero
        return true
    }

    /** Moves the held tile by [amount] and reorders the tiles it passes. */
    fun drag(amount: Offset) {
        delta += amount
        retarget()
    }

    /** Moves the held tile into the slot under its centre, once the layout shows the last move. */
    fun retarget() {
        val held = heldInfo()
        val shown = order
        // Waits for the layout to show the last move before the next one.
        if (held == null || shown == null || held.index != shown.indexOf(from)) return
        val slots = gridState.layoutInfo.visibleItemsInfo.map { it.slot() }
        val target = MonarchSortableGridTokens.target(held.slot(), startCorner + delta, slots) ?: return
        val first = gridState.firstVisibleItemIndex
        if (held.index == first || target == first) {
            // Keeps the grid where it is: by default it follows the key of its first visible tile.
            gridState.requestScrollToItem(first, gridState.firstVisibleItemScrollOffset)
        }
        order = MonarchSortableGridTokens.moved(shown, held.index, target)
    }

    /** How far the held tile is past the top or bottom edge of the grid in the direction it moves. */
    fun overscroll(): Float {
        val held = heldInfo() ?: return 0f
        val top = startCorner.y + delta.y
        val info = gridState.layoutInfo
        return MonarchDragAutoScroll.overscroll(
            top = top,
            bottom = top + held.size.height,
            viewportStart = info.viewportStartOffset,
            viewportEnd = info.viewportEndOffset,
            direction = delta.y,
        )
    }

    /** Ends the drag and returns the index the held tile was dropped at. */
    fun end(): Int {
        val to = position
        reset()
        return to
    }

    /** Ends the drag without a move. */
    fun reset() {
        key = null
        order = null
        delta = Offset.Zero
    }
}

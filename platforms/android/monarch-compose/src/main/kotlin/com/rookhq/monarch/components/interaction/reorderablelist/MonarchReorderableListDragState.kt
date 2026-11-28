package com.rookhq.monarch.components.interaction.reorderablelist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rookhq.monarch.internal.MonarchDragAutoScroll

/**
 * The drag in progress in a `MonarchReorderableList`: which row is held, the
 * order shown while it moves, and how far it is from where the layout put it.
 * The order is committed once, when the drag ends.
 */
@Stable
internal class MonarchReorderableListDragState(private val listState: LazyListState) {
    /** The key of the held row, or `null` when nothing is dragged. */
    var key: Any? by mutableStateOf(null)
        private set

    /** The order shown while dragging, as indices into the items, or `null`. */
    var order: List<Int>? by mutableStateOf(null)
        private set

    /** The index of the held row before the drag. */
    var from: Int = -1
        private set

    private var delta by mutableFloatStateOf(0f)
    private var startOffset = 0

    /** Whether a row is held. */
    val active: Boolean get() = key != null

    /** Where the held row is in the order shown. */
    val position: Int get() = order?.indexOf(from) ?: from

    private fun heldInfo(): LazyListItemInfo? = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** How far the held row is drawn from its slot, so it follows the finger. */
    fun translation(): Float = heldInfo()?.let { startOffset + delta - it.offset } ?: 0f

    /** Starts a drag on the row at [y] when [movable] allows it. Returns whether a drag started. */
    fun start(y: Float, count: Int, movable: (Int) -> Boolean): Boolean {
        val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y <= it.offset + it.size }
        if (row == null || row.index >= count || !movable(row.index)) return false
        key = row.key
        from = row.index
        order = List(count) { it }
        startOffset = row.offset
        delta = 0f
        return true
    }

    /** Moves the held row by [dy] and reorders the rows it passes. */
    fun drag(dy: Float) {
        delta += dy
        retarget()
    }

    /** Moves the held row into the slot under its middle, once the layout shows the last move. */
    fun retarget() {
        val held = heldInfo()
        val shown = order
        // Waits for the layout to show the last move before the next one.
        if (held == null || shown == null || held.index != shown.indexOf(from)) return
        val slots = listState.layoutInfo.visibleItemsInfo.map {
            MonarchReorderableListTokens.Slot(it.index, it.offset, it.size)
        }
        val heldSlot = MonarchReorderableListTokens.Slot(held.index, held.offset, held.size)
        val target = MonarchReorderableListTokens.target(heldSlot, startOffset + delta, slots) ?: return
        val first = listState.firstVisibleItemIndex
        if (held.index == first || target == first) {
            // Keeps the list where it is: by default it follows the key of its first visible row.
            listState.requestScrollToItem(first, listState.firstVisibleItemScrollOffset)
        }
        order = MonarchReorderableListTokens.moved(shown, held.index, target)
    }

    /** How far the held row is past the edge of the list in the direction it moves. */
    fun overscroll(): Float {
        val held = heldInfo() ?: return 0f
        val top = startOffset + delta
        val info = listState.layoutInfo
        return MonarchDragAutoScroll.overscroll(
            top = top,
            bottom = top + held.size,
            viewportStart = info.viewportStartOffset,
            viewportEnd = info.viewportEndOffset,
            direction = delta,
        )
    }

    /** Ends the drag and returns the index the held row was dropped at. */
    fun end(): Int {
        val to = position
        reset()
        return to
    }

    /** Ends the drag without a move. */
    fun reset() {
        key = null
        order = null
        delta = 0f
    }
}

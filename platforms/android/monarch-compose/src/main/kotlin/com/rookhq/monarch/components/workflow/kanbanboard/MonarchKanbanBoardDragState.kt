package com.rookhq.monarch.components.workflow.kanbanboard

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

/**
 * The drag in progress on a `MonarchKanbanBoard`: the held card, where the
 * finger is on the board, and where the card would land. The board does not
 * change until the drop.
 */
@Stable
internal class MonarchKanbanBoardDragState<T> {
    /** The key of the held card, or `null` when nothing is dragged. */
    var key: String? by mutableStateOf(null)
        private set

    /** The held card. */
    var item: T? by mutableStateOf(null)
        private set

    /** Where the held card was before the drag. */
    var origin: MonarchKanbanBoardMoves.Location? by mutableStateOf(null)
        private set

    /** The finger, in board coordinates. */
    var pointer: Offset by mutableStateOf(Offset.Zero)

    /** The finger's offset inside the card when it was picked up. */
    var grab: Offset = Offset.Zero
        private set

    /** The size of the held card when it was picked up, in pixels. */
    var size: IntSize by mutableStateOf(IntSize.Zero)
        private set

    /** Where the card would land, or `null` when the finger is over no column that takes it. */
    var target: MonarchKanbanBoardMoves.Location? by mutableStateOf(null)

    /** The column under the finger that refuses the card, or `null`. */
    var refusing: String? by mutableStateOf(null)

    /** Whether a card is held. */
    val active: Boolean get() = key != null

    /** Holds [item], picked up at [pointer] from its card at [bounds]. */
    fun start(key: String, item: T, origin: MonarchKanbanBoardMoves.Location, pointer: Offset, bounds: Rect) {
        this.key = key
        this.item = item
        this.origin = origin
        this.pointer = pointer
        grab = pointer - bounds.topLeft
        size = IntSize(bounds.width.toInt(), bounds.height.toInt())
        target = origin
        refusing = null
    }

    /** Ends the drag. */
    fun reset() {
        key = null
        item = null
        origin = null
        target = null
        refusing = null
    }
}

/** Where the board, each card and each column's list are, so a finger position can be read as a place. */
internal class MonarchKanbanBoardGeometry {
    /** The board, the coordinates of every position the drag uses. */
    var board: LayoutCoordinates? = null

    /** Each card shown, by key. */
    val cards: MutableMap<String, LayoutCoordinates> = mutableMapOf()

    /** Each column's list of cards, by column id. */
    val lists: MutableMap<String, LayoutCoordinates> = mutableMapOf()

    /** The bounds of [coordinates] on the board, clipped to what is visible, or `null`. */
    fun bounds(coordinates: LayoutCoordinates?): Rect? {
        val parent = board ?: return null
        if (coordinates == null || !coordinates.isAttached || !parent.isAttached) return null
        return parent.localBoundingBoxOf(coordinates).takeIf { it.width > 0f && it.height > 0f }
    }

    /** The key of the card under [point], or `null`. */
    fun cardAt(point: Offset): String? = cards.entries.firstOrNull { bounds(it.value)?.contains(point) == true }?.key

    /** The id of the column whose list is under [point] or level with it, or `null`. */
    fun columnAt(point: Offset): String? = lists.entries.firstOrNull { (_, coordinates) ->
        bounds(coordinates)?.let { point.x >= it.left && point.x <= it.right } == true
    }?.key
}

package com.rookhq.monarch.components.workflow.kanbanboard

import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection

/** The pure moving rules of MonarchKanbanBoard: finding, moving and stepping cards, and drop positions. */
internal object MonarchKanbanBoardMoves {
    /** A place on the board: a column and an index in it. */
    data class Location(val columnId: String, val index: Int)

    /** Where a card is: its column's position, its index in the column and the card. */
    data class Found<T>(val column: Int, val index: Int, val item: T)

    /** Finds the card with [key], or `null`. */
    fun <T> locate(columns: List<MonarchKanbanColumn<T>>, key: String, keyOf: (T) -> String): Found<T>? =
        columns.withIndex().firstNotNullOfOrNull { (c, column) ->
            column.items.indexOfFirst { keyOf(it) == key }.takeIf { it != -1 }?.let { Found(c, it, column.items[it]) }
        }

    /** Where the card with [key] is, as a location, or `null`. */
    fun <T> where(columns: List<MonarchKanbanColumn<T>>, key: String, keyOf: (T) -> String): Location? =
        locate(columns, key, keyOf)?.let { Location(columns[it.column].id, it.index) }

    /** A copy of [columns] with the card [key] at [to], clamped. Unknown cards or columns leave it unchanged. */
    fun <T> move(
        columns: List<MonarchKanbanColumn<T>>,
        key: String,
        to: Location,
        keyOf: (T) -> String,
    ): List<MonarchKanbanColumn<T>> {
        val from = locate(columns, key, keyOf) ?: return columns
        if (columns.none { it.id == to.columnId }) return columns
        val source = columns[from.column].id
        return columns.map { column ->
            if (column.id != source && column.id != to.columnId) return@map column
            val items = column.items.toMutableList()
            if (column.id == source) items.removeAt(from.index)
            if (column.id == to.columnId) items.add(to.index.coerceIn(0, items.size), from.item)
            column.withItems(items)
        }
    }

    /**
     * Whether a column refuses a card that comes from another column: [canMove]
     * says no, or [enforceWipLimits] is on and the column is at or over its limit.
     */
    fun <T> refuses(
        column: MonarchKanbanColumn<T>,
        item: T,
        fromColumnId: String,
        canMove: ((T, String, String) -> Boolean)?,
        enforceWipLimits: Boolean,
    ): Boolean {
        if (column.id == fromColumnId) return false
        val full = enforceWipLimits && column.wipLimit?.let { column.items.size >= it } == true
        return full || canMove?.invoke(item, column.id, fromColumnId) == false
    }

    /**
     * The place one step from the card [key] in [direction]: up and down
     * within its column, backward and forward to the same position in the
     * nearest column that is not [blocked]. `null` when there is none.
     */
    fun <T> step(
        columns: List<MonarchKanbanColumn<T>>,
        key: String,
        direction: MonarchDragHandleDirection,
        keyOf: (T) -> String,
        blocked: (String) -> Boolean,
    ): Location? {
        val at = locate(columns, key, keyOf) ?: return null
        val column = columns[at.column]
        return when (direction) {
            MonarchDragHandleDirection.Up -> (at.index - 1).takeIf { it >= 0 }?.let { Location(column.id, it) }
            MonarchDragHandleDirection.Down ->
                (at.index + 1).takeIf { it < column.items.size }?.let { Location(column.id, it) }
            MonarchDragHandleDirection.Backward, MonarchDragHandleDirection.Forward -> {
                val sign = if (direction == MonarchDragHandleDirection.Forward) 1 else -1
                var c = at.column + sign
                while (c in columns.indices && blocked(columns[c].id)) c += sign
                columns.getOrNull(c)?.let { Location(it.id, at.index.coerceAtMost(it.items.size)) }
            }
        }
    }

    /** A card as laid out in its column: its index among the other cards and the middle of its span, in pixels. */
    data class Slot(val index: Int, val middle: Float)

    /**
     * The index a card dropped at [y] gets among the [slots] of the other
     * cards shown in the column, or after them; [count] is the number of other
     * cards when none is shown.
     */
    fun indexAt(y: Float, slots: List<Slot>, count: Int): Int =
        slots.firstOrNull { y < it.middle }?.index ?: slots.lastOrNull()?.let { it.index + 1 } ?: count

    /** The page step for a finger at [x] on a board [width] wide: -1 near the start, 1 near the end, else 0. */
    fun edge(x: Float, width: Float, zone: Float, rtl: Boolean): Int {
        val physical = when {
            x < zone -> -1
            x > width - zone -> 1
            else -> 0
        }
        return if (rtl) -physical else physical
    }


    /** How many columns of at least [minWidth] fit in [available] pixels with [spacing] between them. */
    fun columnsThatFit(available: Int, minWidth: Int, spacing: Int): Int =
        ((available + spacing) / (minWidth + spacing).coerceAtLeast(1)).coerceAtLeast(1)
}

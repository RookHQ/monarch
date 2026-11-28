package dev.monarchui.components.datagrid.datatable

import androidx.compose.runtime.Immutable

/**
 * The sort of a `MonarchDataTable`: one column and a direction.
 *
 * @property columnId the id of the sorted column.
 * @property direction ascending or descending.
 */
@Immutable
public class MonarchDataTableSort(
    public val columnId: String,
    public val direction: MonarchDataTableSortDirection = MonarchDataTableSortDirection.Ascending,
) {
    /** The sort after the person taps this column's header again: the other direction. */
    public fun toggled(): MonarchDataTableSort = MonarchDataTableSort(
        columnId,
        if (direction == MonarchDataTableSortDirection.Ascending) {
            MonarchDataTableSortDirection.Descending
        } else {
            MonarchDataTableSortDirection.Ascending
        },
    )

    override fun equals(other: Any?): Boolean =
        other is MonarchDataTableSort && other.columnId == columnId && other.direction == direction

    override fun hashCode(): Int = columnId.hashCode() * HASH + direction.hashCode()

    override fun toString(): String = "MonarchDataTableSort(columnId=$columnId, direction=$direction)"

    private companion object {
        const val HASH = 31
    }
}

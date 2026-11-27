package com.rookhq.monarch.components.files.tableblock

import androidx.compose.runtime.Immutable

/**
 * The content of a `MonarchTableBlock`: a rectangle of text cells and whether
 * the first row is a header.
 *
 * The table always has at least one row and one column, every row has the
 * same number of cells, and it never grows past [MAXIMUM_ROW_COUNT] rows or
 * [MAXIMUM_COLUMN_COUNT] columns, the same limits as on the web and iOS.
 *
 * The value is immutable: every change returns a new value, for state
 * hoisting with `onDataChange`. A change that is not possible (a full table,
 * the last row, an index outside the table) returns the same value.
 *
 * @property rows the cells, row by row. Every row has [columnCount] cells.
 * @property hasHeaderRow whether the first row is drawn and read as column headers.
 */
@Immutable
public class MonarchTableBlockData private constructor(
    public val rows: List<List<String>>,
    public val hasHeaderRow: Boolean,
) {
    /** The number of rows, header included. */
    public val rowCount: Int get() = rows.size

    /** The number of columns. */
    public val columnCount: Int get() = rows.first().size

    /** Whether another row fits. */
    public val canAddRow: Boolean get() = rowCount < MAXIMUM_ROW_COUNT

    /** Whether another column fits. */
    public val canAddColumn: Boolean get() = columnCount < MAXIMUM_COLUMN_COUNT

    /** Whether a row can be deleted: the last row cannot. */
    public val canDeleteRow: Boolean get() = rowCount > 1

    /** Whether a column can be deleted: the last column cannot. */
    public val canDeleteColumn: Boolean get() = columnCount > 1

    /** The text of a cell, or an empty string outside the table. */
    public operator fun get(row: Int, column: Int): String = rows.getOrNull(row)?.getOrNull(column).orEmpty()

    /** Whether [row] is the header row. */
    public fun isHeader(row: Int): Boolean = hasHeaderRow && row == 0

    /** A copy with the text of one cell replaced. Outside the table nothing changes. */
    public fun withCell(row: Int, column: Int, text: String): MonarchTableBlockData {
        if (!contains(row, column)) return this
        return copy(rows.mapIndexed { r, cells -> if (r == row) cells.replaced(column, text) else cells })
    }

    /** A copy with the header row switched on or off. */
    public fun withHeaderRow(hasHeaderRow: Boolean): MonarchTableBlockData =
        if (hasHeaderRow == this.hasHeaderRow) this else MonarchTableBlockData(rows, hasHeaderRow)

    /** A copy with an empty row below [after]; unchanged when the table is full or [after] is outside it. */
    public fun addRow(after: Int): MonarchTableBlockData {
        if (!canAddRow || after !in rows.indices) return this
        return copy(rows.toMutableList().apply { add(after + 1, List(columnCount) { "" }) })
    }

    /** A copy with an empty column after [after]; unchanged when the table is full or [after] is outside it. */
    public fun addColumn(after: Int): MonarchTableBlockData {
        if (!canAddColumn || after !in 0 until columnCount) return this
        return copy(rows.map { it.toMutableList().apply { add(after + 1, "") } })
    }

    /** A copy without row [at]; unchanged for the last remaining row or a row outside the table. */
    public fun deleteRow(at: Int): MonarchTableBlockData {
        if (!canDeleteRow || at !in rows.indices) return this
        return copy(rows.filterIndexed { index, _ -> index != at })
    }

    /** A copy without column [at]; unchanged for the last remaining column or a column outside the table. */
    public fun deleteColumn(at: Int): MonarchTableBlockData {
        if (!canDeleteColumn || at !in 0 until columnCount) return this
        return copy(rows.map { cells -> cells.filterIndexed { index, _ -> index != at } })
    }

    override fun equals(other: Any?): Boolean =
        other is MonarchTableBlockData && other.rows == rows && other.hasHeaderRow == hasHeaderRow

    override fun hashCode(): Int = java.util.Objects.hash(rows, hasHeaderRow)

    private fun contains(row: Int, column: Int): Boolean = row in rows.indices && column in 0 until columnCount

    private fun copy(rows: List<List<String>>): MonarchTableBlockData = MonarchTableBlockData(rows, hasHeaderRow)

    private fun List<String>.replaced(index: Int, text: String): List<String> =
        mapIndexed { i, cell -> if (i == index) text else cell }

    /** Creates tables and holds the limits. */
    public companion object {
        /** The most rows a table can have. */
        public const val MAXIMUM_ROW_COUNT: Int = 50

        /** The most columns a table can have. */
        public const val MAXIMUM_COLUMN_COUNT: Int = 20

        private const val DEFAULT_SIZE = 3

        /**
         * A table with the given cells. Short rows are padded with empty
         * cells, rows and columns past the limits are dropped, and an empty
         * input gives one empty cell.
         *
         * @param rows the cells, row by row.
         * @param hasHeaderRow whether the first row holds column headers.
         */
        public fun of(rows: List<List<String>>, hasHeaderRow: Boolean = true): MonarchTableBlockData {
            val kept = rows.take(MAXIMUM_ROW_COUNT)
            val width = (kept.maxOfOrNull { it.size } ?: 1).coerceIn(1, MAXIMUM_COLUMN_COUNT)
            val normalized = kept.map { row -> List(width) { row.getOrNull(it).orEmpty() } }
            return MonarchTableBlockData(normalized.ifEmpty { listOf(List(width) { "" }) }, hasHeaderRow)
        }

        /**
         * An empty table. The counts are kept between 1 and the limits.
         *
         * @param rowCount the number of rows, header included.
         * @param columnCount the number of columns.
         * @param hasHeaderRow whether the first row holds column headers.
         */
        public fun empty(
            rowCount: Int = DEFAULT_SIZE,
            columnCount: Int = DEFAULT_SIZE,
            hasHeaderRow: Boolean = true,
        ): MonarchTableBlockData {
            val height = rowCount.coerceIn(1, MAXIMUM_ROW_COUNT)
            val width = columnCount.coerceIn(1, MAXIMUM_COLUMN_COUNT)
            return MonarchTableBlockData(List(height) { List(width) { "" } }, hasHeaderRow)
        }
    }
}

package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.datagrid.columnchooser.MonarchColumnChooserColumn

/**
 * One column of a `MonarchDataGrid`: its header, the text of a row's cell, its width, its pin, and how it sorts,
 * hides and edits.
 *
 * @param T the row type.
 * @property title the header, already localised. Also the field name in the list layout and the detail editor.
 * @property id the stable id used by the sort, the hidden set, the order, the pins and the widths. The title by
 * default.
 * @property width the column's width in the grid layout. [Dp.Unspecified] uses the default width; the app's
 * `columnWidths` replace it.
 * @property numeric aligns the cells to the end with tabular figures, and sorts by number.
 * @property sortable whether the column menu offers sorting by this column.
 * @property hideable whether the column menu or a column chooser can hide it.
 * @property pinnable whether the column menu offers pinning this column.
 * @property pinned the edge the column is pinned to while the app passes no `pinning`.
 * @property comparator the ascending order of rows for this column. By default the cell text: by number for a
 * numeric column, otherwise alphabetically ignoring case; empty cells sort last in both directions.
 * @property edit makes the column's cells editable in place, or `null` for a read-only column.
 * @property value the text of a row's cell. An empty string is read as "No value".
 */
public class MonarchDataGridColumn<T>(
    public val title: String,
    public val id: String = title,
    public val width: Dp = Dp.Unspecified,
    public val numeric: Boolean = false,
    public val sortable: Boolean = true,
    public val hideable: Boolean = true,
    public val pinnable: Boolean = true,
    public val pinned: MonarchDataGridPin? = null,
    public val comparator: Comparator<T>? = null,
    public val edit: MonarchDataGridEdit<T, *>? = null,
    public val value: (T) -> String,
) {
    /** This column as a `MonarchColumnChooser` offers it. */
    public val chooserColumn: MonarchColumnChooserColumn
        get() = MonarchColumnChooserColumn(label = title, id = id, hideable = hideable)

    override fun toString(): String = "MonarchDataGridColumn(id=$id, title=$title)"
}

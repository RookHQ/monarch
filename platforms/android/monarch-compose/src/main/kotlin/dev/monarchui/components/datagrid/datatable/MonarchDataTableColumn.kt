package dev.monarchui.components.datagrid.datatable

import androidx.compose.runtime.Composable
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooserColumn
import dev.monarchui.foundation.MonarchTone

/**
 * One column of a `MonarchDataTable`: its header, how a row's cell reads, and
 * how it sorts, hides and sizes.
 *
 * @param T the row type.
 * @property title the header, already localised. Also the field name in the list layout.
 * @property id the stable id used by the sort, the hidden set and the order. The title by default.
 * @property numeric aligns the cells to the end with tabular figures, and sorts by number.
 * @property sortable whether the header sorts the rows.
 * @property hideable whether a column chooser can hide it.
 * @property weight the column's share of the row's width in the table layout.
 * @property comparator the ascending order of rows for this column. By default the cell text: by number for a
 * numeric column, otherwise alphabetically ignoring case; empty cells sort last in both directions.
 * @property tone the tone of a badge drawn around a row's cell text, or `null` for plain text, such as a status.
 * @property cell draws a row's cell in the table layout instead of its text, for an avatar, a badge or a link. The
 * text of [value] still names the cell for sorting, the list layout and TalkBack.
 * @property value the text of a row's cell. An empty string is read as "No value".
 */
public class MonarchDataTableColumn<T>(
    public val title: String,
    public val id: String = title,
    public val numeric: Boolean = false,
    public val sortable: Boolean = true,
    public val hideable: Boolean = true,
    public val weight: Float = 1f,
    public val comparator: Comparator<T>? = null,
    public val tone: ((T) -> MonarchTone?)? = null,
    public val cell: (@Composable (T) -> Unit)? = null,
    public val value: (T) -> String,
) {
    /** This column as a `MonarchColumnChooser` offers it. */
    public val chooserColumn: MonarchColumnChooserColumn
        get() = MonarchColumnChooserColumn(label = title, id = id, hideable = hideable)

    override fun toString(): String = "MonarchDataTableColumn(id=$id, title=$title)"
}

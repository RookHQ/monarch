package dev.monarchui.components.datagrid.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCell
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellEditor
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellLabels

/**
 * How the cells of a `MonarchDataGrid` column edit in place, with a `MonarchEditableCell`: the editor, the typed
 * value of a row and how a new value is saved.
 *
 * The value is hoisted: [onCommit] saves the new value and the app passes the changed rows back to the grid.
 * While it runs the cell shows a progress indicator; when it throws, the cell keeps the editor open with the
 * error message.
 *
 * @param T the row type.
 * @param V the value type, fixed by [editor]: `String`, `Double`, an option value or epoch milliseconds in UTC.
 * @property editor the editor a cell opens: text, number, a choice or a date.
 * @property value the saved value of a row's cell, or `null` for an empty cell.
 * @property onCommit saves a row's new value. Throw to keep the editor open with the error.
 * @property editable whether a row's cell can be edited; a cell that cannot is read-only text.
 * @property required refuses an empty value.
 * @property placeholder shown in an empty editable cell.
 * @property validate returns a message to refuse a row's new value before it is saved, or `null` to allow it.
 */
@Immutable
public class MonarchDataGridEdit<T, V : Any>(
    public val editor: MonarchEditableCellEditor<V>,
    public val value: (T) -> V?,
    public val onCommit: suspend (row: T, value: V?) -> Unit,
    public val editable: (T) -> Boolean = { true },
    public val required: Boolean = false,
    public val placeholder: String? = null,
    public val validate: ((row: T, value: V?) -> String?)? = null,
) {
    /** The editable cell of [row], showing [display] (the column's text) while it is not editing. */
    @Composable
    internal fun Cell(
        row: T,
        label: String,
        display: String,
        modifier: Modifier,
        startEditing: Boolean,
        labels: MonarchEditableCellLabels,
    ) {
        val check = validate
        MonarchEditableCell(
            value = value(row),
            label = label,
            editor = editor,
            onCommit = { onCommit(row, it) },
            modifier = modifier,
            required = required,
            placeholder = placeholder,
            startEditing = startEditing,
            format = if (display.isBlank()) null else { _ -> display },
            validate = check?.let { { next -> it(row, next) } },
            labels = labels,
            bare = false,
        )
    }

    override fun toString(): String = "MonarchDataGridEdit(editor=$editor)"
}

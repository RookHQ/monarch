package dev.monarchui.components.datagrid.columnchooser

import androidx.compose.runtime.Immutable

/**
 * One column offered by a `MonarchColumnChooser`. `MonarchDataTableColumn.chooserColumn` gives one for a
 * column of a `MonarchDataTable`.
 *
 * @property label the column's name, as in the table header.
 * @property id the column's stable id, used in the hidden set and the order. The label by default.
 * @property hideable whether the person can hide it. A column that cannot be hidden is always shown.
 */
@Immutable
public class MonarchColumnChooserColumn(
    public val label: String,
    public val id: String = label,
    public val hideable: Boolean = true,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchColumnChooserColumn && other.label == label && other.id == id && other.hideable == hideable

    override fun hashCode(): Int = (label.hashCode() * HASH + id.hashCode()) * HASH + hideable.hashCode()

    override fun toString(): String = "MonarchColumnChooserColumn(label=$label, id=$id, hideable=$hideable)"

    private companion object {
        const val HASH = 31
    }
}

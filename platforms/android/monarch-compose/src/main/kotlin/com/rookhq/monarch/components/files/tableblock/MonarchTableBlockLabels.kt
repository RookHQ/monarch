package com.rookhq.monarch.components.files.tableblock

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTableBlock`. Every
 * `null` uses the built-in string named after it in `strings_tableblock.xml`.
 *
 * @property region the spoken name of a table without a caption or a label: "Table".
 * @property controls the label of the button that opens the editing menu: "Table".
 * @property addRow the menu item that inserts a row below: "Add row below".
 * @property addColumn the menu item that inserts a column after: "Add column after".
 * @property deleteRow the menu item that deletes the row: "Delete row".
 * @property deleteColumn the menu item that deletes the column: "Delete column".
 * @property headerRow the menu item that switches the header row: "Header row".
 * @property deleteTable the menu item that deletes the table: "Delete table".
 * @property emptyCell what TalkBack reads for an empty read-only cell: "Empty".
 * @property cellPosition writes the spoken position of an editable cell from its row and column,
 * counted from 1: "Row 2, column 1".
 */
@Immutable
public class MonarchTableBlockLabels(
    public val region: String? = null,
    public val controls: String? = null,
    public val addRow: String? = null,
    public val addColumn: String? = null,
    public val deleteRow: String? = null,
    public val deleteColumn: String? = null,
    public val headerRow: String? = null,
    public val deleteTable: String? = null,
    public val emptyCell: String? = null,
    public val cellPosition: ((row: Int, column: Int) -> String)? = null,
)

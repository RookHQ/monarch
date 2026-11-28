package com.rookhq.monarch.components.datagrid.bulkactionbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchBulkActionBar`. Every
 * `null` uses the built-in string named after it in `strings_bulkactionbar.xml`.
 *
 * @property selected writes the count from the selected items and the total, when known: "3 of 120 selected".
 * Shown and announced.
 * @property cleared announced when the selection is emptied: "Selection cleared".
 * @property clear the label of the button that empties the selection: "Clear selection".
 * @property more the label of the button that opens the menu of the other actions: "More".
 * @property bar the TalkBack name of the bar: "Bulk actions".
 */
@Immutable
public class MonarchBulkActionBarLabels(
    public val selected: ((count: Int, total: Int?) -> String)? = null,
    public val cleared: String? = null,
    public val clear: String? = null,
    public val more: String? = null,
    public val bar: String? = null,
)

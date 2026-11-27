package com.rookhq.monarch.components.datagrid.tabletoolbar

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels

/**
 * Replacements for the built-in strings of a `MonarchTableToolbar`. Every
 * `null` uses the built-in string named after it in `strings_tabletoolbar.xml`.
 *
 * @property toolbar the TalkBack name of the toolbar: "Table controls".
 * @property results writes the result count: "1,204 results". Shown and announced when it changes.
 * @property bulkActionBar the strings of the bulk selection bar shown while rows are selected.
 */
@Immutable
public class MonarchTableToolbarLabels(
    public val toolbar: String? = null,
    public val results: ((count: Int) -> String)? = null,
    public val bulkActionBar: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
)

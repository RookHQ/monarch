package dev.monarchui.components.files.filelist

import androidx.compose.runtime.Immutable
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels
import dev.monarchui.components.files.filecard.MonarchFileCardLabels

/**
 * Replacements for the built-in strings of a `MonarchFileList`. Every `null`
 * uses the built-in string named after it in `strings_filelist.xml`.
 *
 * @property selectAll the label of the select all checkbox: "Select all files".
 * @property empty the title of the empty state: "No files yet".
 * @property card the strings of each `MonarchFileCard`.
 * @property bulkActionBar the strings of the bar shown while files are selected.
 */
@Immutable
public class MonarchFileListLabels(
    public val selectAll: String? = null,
    public val empty: String? = null,
    public val card: MonarchFileCardLabels = MonarchFileCardLabels(),
    public val bulkActionBar: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
)

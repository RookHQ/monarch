package dev.monarchui.components.datagrid.previewrows

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPreviewRows`. Every
 * `null` uses the built-in string named after it in `strings_previewrows.xml`.
 *
 * @property filter the TalkBack name of the filter tabs: "Show rows".
 * @property all the tab that shows every row: "All".
 * @property ready the name of the Ready status, in badges and tabs: "Ready".
 * @property warning the name of the Warning status: "Warning".
 * @property error the name of the Error status: "Error".
 * @property duplicate the name of the Duplicate status: "Duplicate".
 * @property row writes the heading of a row from its number: "Row 4".
 * @property emptyTitle the title when the file has no data rows: "No rows to preview".
 * @property emptyMessage the message when the file has no data rows: "The file has a header row but no data rows.".
 * @property filteredTitle the title when the chosen filter matches no row: "No rows with this result".
 * @property noValue what TalkBack reads for a missing value, shown as a dash: "No value".
 * @property result the caption before a row's outcome badge, "Result".
 * @property issues the caption before a row's problems, "Issues".
 */
@Immutable
public class MonarchPreviewRowsLabels(
    public val filter: String? = null,
    public val all: String? = null,
    public val ready: String? = null,
    public val warning: String? = null,
    public val error: String? = null,
    public val duplicate: String? = null,
    public val row: ((row: Int) -> String)? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val filteredTitle: String? = null,
    public val noValue: String? = null,
    public val result: String? = null,
    public val issues: String? = null,
)

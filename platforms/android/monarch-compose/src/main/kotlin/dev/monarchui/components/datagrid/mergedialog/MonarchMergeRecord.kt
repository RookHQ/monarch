package dev.monarchui.components.datagrid.mergedialog

import androidx.compose.runtime.Immutable

/**
 * One of the records a `MonarchMergeDialog` merges.
 *
 * @property id identifies the record in the [MonarchMergeSelection].
 * @property title the name of the record, such as the company name.
 * @property description a second line, such as "Created 3 Mar 2025 · 12 invoices".
 * @property values the field values by field id, already written for display. A missing or blank value
 * counts as empty.
 */
@Immutable
public class MonarchMergeRecord(
    public val id: String,
    public val title: String,
    public val description: String? = null,
    public val values: Map<String, String> = emptyMap(),
)

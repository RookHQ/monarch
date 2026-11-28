package dev.monarchui.components.datagrid.columnmapping

import androidx.compose.runtime.Immutable

/**
 * What still blocks a mapping, from `MonarchColumnMappingDefaults.issues`.
 *
 * @property missing the required fields that no column is mapped to, in field order.
 * @property duplicates each field id chosen for more than one column, with those columns.
 */
@Immutable
public data class MonarchColumnMappingIssues(
    public val missing: List<MonarchColumnMappingField>,
    public val duplicates: Map<String, List<String>>,
) {
    /** Whether the mapping can be used: nothing missing and nothing chosen twice. */
    public val isEmpty: Boolean get() = missing.isEmpty() && duplicates.isEmpty()
}

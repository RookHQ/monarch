package com.rookhq.monarch.components.datagrid.mergedialog

import androidx.compose.runtime.Immutable

/**
 * What the person chose in a `MonarchMergeDialog`.
 *
 * @property primaryId the record that is kept.
 * @property mergedIds the other records, merged into the kept one.
 * @property fields for each field id, the id of the record whose value is kept.
 */
@Immutable
public data class MonarchMergeSelection(
    public val primaryId: String,
    public val mergedIds: List<String>,
    public val fields: Map<String, String>,
)

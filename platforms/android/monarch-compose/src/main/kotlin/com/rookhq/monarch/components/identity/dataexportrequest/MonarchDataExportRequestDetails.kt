package com.rookhq.monarch.components.identity.dataexportrequest

import androidx.compose.runtime.Immutable

/**
 * What the person chose when requesting an export.
 *
 * @property scope the ids of the chosen scope options, in option order. Empty when there is no scope.
 * @property format the chosen format value, or `null` when no formats are offered.
 */
@Immutable
public data class MonarchDataExportRequestDetails(
    public val scope: List<String>,
    public val format: String?,
)

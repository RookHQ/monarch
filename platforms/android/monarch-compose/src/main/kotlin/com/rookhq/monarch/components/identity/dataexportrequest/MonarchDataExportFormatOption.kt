package com.rookhq.monarch.components.identity.dataexportrequest

import androidx.compose.runtime.Immutable

/**
 * One file format the person can choose for an export.
 *
 * @property value the value passed in [MonarchDataExportRequestDetails.format], such as `json`.
 * @property label the visible label, such as "JSON (machine-readable)". Already localised.
 */
@Immutable
public class MonarchDataExportFormatOption(
    public val value: String,
    public val label: String,
)

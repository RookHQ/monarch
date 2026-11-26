package com.rookhq.monarch.components.datagrid.previewrows

import androidx.compose.runtime.Immutable

/**
 * A column of the file shown by a `MonarchPreviewRows`.
 *
 * @property id the key of the column in `MonarchPreviewRow.values`.
 * @property label the name of the column, shown before each value.
 * @property numeric draws the value with tabular figures, so numbers line up.
 */
@Immutable
public class MonarchPreviewRowsColumn(
    public val id: String,
    public val label: String,
    public val numeric: Boolean = false,
)

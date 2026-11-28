package dev.monarchui.components.datagrid.mergedialog

import androidx.compose.runtime.Immutable

/**
 * A field whose kept value a `MonarchMergeDialog` asks for.
 *
 * @property id the key of the field in [MonarchMergeRecord.values].
 * @property label the visible name of the field.
 */
@Immutable
public class MonarchMergeField(
    public val id: String,
    public val label: String,
)

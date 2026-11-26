package com.rookhq.monarch.components.datagrid.columnmapping

import androidx.compose.runtime.Immutable

/**
 * A column of the imported data, such as a CSV header, with a few example
 * values. The mapping is keyed by [name].
 *
 * @property name the header text. A blank header is shown as its position, "#3".
 * @property samples example values from the first rows; the first three that are not blank are shown.
 */
@Immutable
public data class MonarchColumnMappingSource(
    public val name: String,
    public val samples: List<String> = emptyList(),
)

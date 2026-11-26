package com.rookhq.monarch.components.datagrid.columnmapping

import androidx.compose.runtime.Immutable

/**
 * A field of the product that a column can be mapped to.
 *
 * @property id the stable id, the value of the mapping.
 * @property label the name in the picker.
 * @property required whether some column must be mapped to it. Shown as "(required)" in the picker.
 * @property description help, such as the expected format, shown under the picker once chosen.
 * @property aliases other header names that mean this field ("E-mail", "Mail"), for suggestions.
 */
@Immutable
public data class MonarchColumnMappingField(
    public val id: String,
    public val label: String,
    public val required: Boolean = false,
    public val description: String? = null,
    public val aliases: List<String> = emptyList(),
)

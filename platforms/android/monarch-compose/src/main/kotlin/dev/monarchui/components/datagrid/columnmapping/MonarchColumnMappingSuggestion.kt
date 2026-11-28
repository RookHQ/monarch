package dev.monarchui.components.datagrid.columnmapping

import androidx.compose.runtime.Immutable

/**
 * The field suggested for a column by `MonarchColumnMappingDefaults.suggest`.
 *
 * @property fieldId the id of the suggested field.
 * @property confidence an exact or a close match.
 * @property score the similarity, from 0 to 1, rounded to two decimals.
 */
@Immutable
public data class MonarchColumnMappingSuggestion(
    public val fieldId: String,
    public val confidence: MonarchColumnMappingConfidence,
    public val score: Double,
)

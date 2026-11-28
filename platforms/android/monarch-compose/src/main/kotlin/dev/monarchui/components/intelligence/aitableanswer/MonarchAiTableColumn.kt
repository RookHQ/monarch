package dev.monarchui.components.intelligence.aitableanswer

import androidx.compose.runtime.Immutable

/**
 * A column of a `MonarchAiTableAnswer`: which key of the rows it shows and,
 * optionally, its header, type and alignment. Every `null` is decided from
 * the data.
 *
 * @property key the key of the value in each row.
 * @property label the header. `null` uses the key.
 * @property type how the values compare and format. `null` infers it from the values.
 * @property alignment where the values sit. `null` puts numbers at the end and the rest at the start.
 */
@Immutable
public data class MonarchAiTableColumn(
    public val key: String,
    public val label: String? = null,
    public val type: MonarchAiTableColumnType? = null,
    public val alignment: MonarchAiTableColumnAlignment? = null,
)

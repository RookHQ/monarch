package dev.monarchui.components.charts.heatmap

import androidx.compose.runtime.Immutable

/**
 * One value of a `MonarchHeatmap`. A row and column pair without a cell is
 * drawn and read as no data.
 *
 * @property row the key of the row.
 * @property column the key of the column.
 * @property value the value; `null` or a value that is not finite is no data.
 * @property valueLabel replaces the formatted value in the cell, the table and what TalkBack reads, such as
 * "7h 30m".
 * @property note a short note read and listed after the value, such as "Holiday".
 */
@Immutable
public class MonarchHeatmapCell(
    public val row: String,
    public val column: String,
    public val value: Double?,
    public val valueLabel: String? = null,
    public val note: String? = null,
)

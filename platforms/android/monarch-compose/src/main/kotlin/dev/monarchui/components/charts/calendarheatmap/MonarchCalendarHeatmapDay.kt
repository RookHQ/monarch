package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.runtime.Immutable

/**
 * One day of a `MonarchCalendarHeatmap`.
 *
 * @property date the day, as epoch milliseconds in UTC. Any time of that UTC day counts as the day.
 * @property value the day's value, or `null` for no data. A value that is not finite counts as no data.
 * @property valueLabel replaces the formatted value wherever it is written or read, such as "7h 30m".
 * @property note a short note after the value, such as "Holiday" or "Leave".
 * @property inactive a non-working or out-of-scope day, such as a weekend: drawn hollow unless it has a value
 * above zero, and read as such.
 * @property marker flags the day with a small dot; explain why in [note].
 */
@Immutable
public class MonarchCalendarHeatmapDay(
    public val date: Long,
    public val value: Double?,
    public val valueLabel: String? = null,
    public val note: String? = null,
    public val inactive: Boolean = false,
    public val marker: Boolean = false,
)

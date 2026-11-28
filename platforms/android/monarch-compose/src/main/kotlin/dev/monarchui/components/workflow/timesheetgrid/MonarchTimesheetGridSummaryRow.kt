package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.runtime.Immutable

/**
 * A display-only footer row of a `MonarchTimesheetGrid`, such as attendance.
 *
 * @property id a stable id.
 * @property label the row's name.
 * @property cells the text per day, keyed by any time on the day in UTC (epoch milliseconds).
 * @property total the text in the total column, or `null` for none.
 */
@Immutable
public class MonarchTimesheetGridSummaryRow(
    public val id: String,
    public val label: String,
    public val cells: Map<Long, String> = emptyMap(),
    public val total: String? = null,
) {
    private val byDay: Map<Long, String> = cells.mapKeys { TimesheetGridRules.dayKey(it.key) }

    /**
     * The text of this row on a day.
     *
     * @param on any time on the day in UTC, as epoch milliseconds.
     * @return the text, or `null` when the day has none.
     */
    public fun text(on: Long): String? = byDay[TimesheetGridRules.dayKey(on)]

    override fun toString(): String = "MonarchTimesheetGridSummaryRow(id=$id)"
}

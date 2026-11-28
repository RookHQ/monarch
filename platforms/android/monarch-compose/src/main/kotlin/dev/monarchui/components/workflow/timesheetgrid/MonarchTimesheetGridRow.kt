package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.runtime.Immutable

/**
 * One row of a `MonarchTimesheetGrid`: a project, task or issue with its minutes per day.
 *
 * @property id a stable id, passed back to `onCellChange` and `onCellActivate`.
 * @property label the row's name, such as a project name. Also names each cell for TalkBack.
 * @property description secondary text under the name, such as the activity or "Billable".
 * @property cells minutes per day, keyed by any time on the day in UTC (epoch milliseconds). Missing days are
 * empty; negative minutes count as 0; two keys on the same day are added together.
 * @property readOnly the row never takes typing, for example time from a timer or an import.
 */
@Immutable
public class MonarchTimesheetGridRow(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val cells: Map<Long, Int> = emptyMap(),
    public val readOnly: Boolean = false,
) {
    private val byDay: Map<Long, Int> = cells.entries
        .groupingBy { TimesheetGridRules.dayKey(it.key) }
        .fold(0) { total, entry -> total + maxOf(0, entry.value) }

    /**
     * The minutes of this row on a day.
     *
     * @param on any time on the day in UTC, as epoch milliseconds.
     * @return the minutes, 0 when the day is empty.
     */
    public fun minutes(on: Long): Int = byDay[TimesheetGridRules.dayKey(on)] ?: 0

    override fun toString(): String = "MonarchTimesheetGridRow(id=$id)"
}

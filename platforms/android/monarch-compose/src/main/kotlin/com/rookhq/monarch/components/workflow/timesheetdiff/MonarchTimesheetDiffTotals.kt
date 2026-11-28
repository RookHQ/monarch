package com.rookhq.monarch.components.workflow.timesheetdiff

import androidx.compose.runtime.Immutable

/**
 * The minutes gained (positive) or lost (negative) between two versions of
 * a timesheet.
 *
 * @property total every entry.
 * @property billable billable entries only; an entry whose `billable` is `null` counts as billable.
 */
@Immutable
public class MonarchTimesheetDiffTotals(public val total: Int, public val billable: Int) {
    override fun equals(other: Any?): Boolean =
        other is MonarchTimesheetDiffTotals && other.total == total && other.billable == billable

    override fun hashCode(): Int = 31 * total + billable

    override fun toString(): String = "MonarchTimesheetDiffTotals(total=$total, billable=$billable)"
}

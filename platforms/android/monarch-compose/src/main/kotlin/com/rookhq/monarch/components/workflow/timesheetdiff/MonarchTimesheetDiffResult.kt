package com.rookhq.monarch.components.workflow.timesheetdiff

import androidx.compose.runtime.Immutable

/**
 * The difference between two versions of a timesheet, shown by
 * `MonarchTimesheetDiff`. Build it with [between], or pass the product's own
 * difference.
 *
 * @property added entries only in the new version, in its order.
 * @property removed entries only in the old version, in its order.
 * @property changed entries in both with different values, in the new version's order.
 */
@Immutable
public class MonarchTimesheetDiffResult(
    public val added: List<MonarchTimesheetDiffEntry> = emptyList(),
    public val removed: List<MonarchTimesheetDiffEntry> = emptyList(),
    public val changed: List<MonarchTimesheetDiffChange> = emptyList(),
) {
    /** Whether nothing was added, removed or changed. */
    public val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty() && changed.isEmpty()

    /** The minutes gained or lost in total and for billable entries. */
    public val totals: MonarchTimesheetDiffTotals
        get() {
            fun billable(entry: MonarchTimesheetDiffEntry) = if (entry.billable == false) 0 else entry.minutes
            val total = added.sumOf { it.minutes } - removed.sumOf { it.minutes } +
                changed.sumOf { it.after.minutes - it.before.minutes }
            val bill = added.sumOf(::billable) - removed.sumOf(::billable) +
                changed.sumOf { billable(it.after) - billable(it.before) }
            return MonarchTimesheetDiffTotals(total, bill)
        }

    /** Compares two versions. */
    public companion object {
        /**
         * Compares [before] and [after] by entry id: entries only in [after]
         * are added, only in [before] removed, and in both with a different
         * date, minutes, project, activity, task, billable or note changed. A
         * missing value and an empty one are different.
         */
        public fun between(
            before: List<MonarchTimesheetDiffEntry>,
            after: List<MonarchTimesheetDiffEntry>,
        ): MonarchTimesheetDiffResult {
            val old = before.associateBy { it.id }
            val new = after.associateBy { it.id }
            val changed = after.mapNotNull { entry ->
                val previous = old[entry.id] ?: return@mapNotNull null
                val fields = TimesheetDiffCompare.changedFields(previous, entry)
                if (fields.isEmpty()) null else MonarchTimesheetDiffChange(entry.id, fields, previous, entry)
            }
            return MonarchTimesheetDiffResult(
                added = after.filter { it.id !in old },
                removed = before.filter { it.id !in new },
                changed = changed,
            )
        }
    }
}

/** The field comparison of [MonarchTimesheetDiffResult.between]. */
internal object TimesheetDiffCompare {
    private const val DAY_MILLIS = 86_400_000L

    /** The names of the compared fields whose values differ, in the web's order. */
    fun changedFields(before: MonarchTimesheetDiffEntry, after: MonarchTimesheetDiffEntry): List<String> {
        val fields = MonarchTimesheetDiffFields
        return listOfNotNull(
            fields.DATE.takeIf { Math.floorDiv(before.date, DAY_MILLIS) != Math.floorDiv(after.date, DAY_MILLIS) },
            fields.MINUTES.takeIf { before.minutes != after.minutes },
            fields.PROJECT.takeIf { before.project != after.project },
            fields.ACTIVITY.takeIf { before.activity != after.activity },
            fields.TASK.takeIf { before.task != after.task },
            fields.BILLABLE.takeIf { before.billable != after.billable },
            fields.NOTE.takeIf { before.note != after.note },
        )
    }
}

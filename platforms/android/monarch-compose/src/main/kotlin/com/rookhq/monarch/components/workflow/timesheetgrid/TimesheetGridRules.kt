package com.rookhq.monarch.components.workflow.timesheetgrid

import com.rookhq.monarch.components.forms.durationinput.DurationInputMath
import com.rookhq.monarch.components.forms.durationinput.DurationInputParse
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInputTokens

/** What committing the text of a cell does. */
internal sealed interface TimesheetGridCommit {
    /** The value is the saved one: nothing to save. */
    data object Unchanged : TimesheetGridCommit

    /** Save [minutes]; 0 clears the cell. */
    data class Save(val minutes: Int) : TimesheetGridCommit

    /** The text is not a duration. */
    data object Invalid : TimesheetGridCommit

    /** The value is above the daily maximum. */
    data object TooLong : TimesheetGridCommit
}

/** The pure rules of MonarchTimesheetGrid: day keys, totals, what can be edited and what a commit does. */
internal object TimesheetGridRules {
    /** Milliseconds in a UTC day. */
    const val DAY_MILLIS: Long = 86_400_000L

    /** The UTC midnight of the day [millis] falls on. */
    fun dayKey(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** Whether the day is locked: [MonarchTimesheetGridDay.locked] or a lock reason. */
    fun locked(day: MonarchTimesheetGridDay): Boolean = day.locked || day.lockReason != null

    /** Whether the grid takes typing at all: a save callback and not read-only. */
    fun editable(readOnly: Boolean, canSave: Boolean): Boolean = !readOnly && canSave

    /** Whether one cell takes typing. */
    fun cellEditable(editable: Boolean, row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay): Boolean =
        editable && !row.readOnly && !locked(day)

    /** A row's total over [days]; days not in [days] are ignored. */
    fun rowTotal(row: MonarchTimesheetGridRow, days: List<MonarchTimesheetGridDay>): Int =
        days.sumOf { row.minutes(it.date) }

    /** A day's total over [rows]. */
    fun dayTotal(rows: List<MonarchTimesheetGridRow>, day: MonarchTimesheetGridDay): Int =
        rows.sumOf { it.minutes(day.date) }

    /** The total of every row over [days]. */
    fun grandTotal(rows: List<MonarchTimesheetGridRow>, days: List<MonarchTimesheetGridDay>): Int =
        rows.sumOf { rowTotal(it, days) }

    /** Whether a day total is above the daily maximum. */
    fun over(total: Int, maximum: Int): Boolean = total > maximum

    /** The day page that opens first: today, otherwise the first day. */
    fun initialDayIndex(days: List<MonarchTimesheetGridDay>): Int = days.indexOfFirst { it.today }.coerceAtLeast(0)

    /** Reads typed [text] against the [saved] value: empty clears, a duration above [maximum] is rejected. */
    fun commit(text: String, saved: Int, maximum: Int): TimesheetGridCommit {
        val parsed = MonarchDurationInputTokens.read(text, null, DurationInputMath.MINUTES_PER_DAY)
        val minutes = when (parsed) {
            DurationInputParse.Empty -> 0
            DurationInputParse.Invalid -> return TimesheetGridCommit.Invalid
            is DurationInputParse.Minutes -> parsed.minutes
        }
        return when {
            minutes > maximum -> TimesheetGridCommit.TooLong
            minutes == saved -> TimesheetGridCommit.Unchanged
            else -> TimesheetGridCommit.Save(minutes)
        }
    }
}

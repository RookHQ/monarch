package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.runtime.Immutable

/**
 * One day of a `MonarchTimesheetGrid`: a column of the grid, or a page of the day layout.
 *
 * @property date the day: any time on that date in UTC, as epoch milliseconds, as the Material 3
 * `DatePickerState` gives it. The same day key is used in each row's cells.
 * @property label replaces the built-in short heading ("Mon 12").
 * @property today marks the day as today: the heading uses the accent colour and says "Today".
 * @property nonWorking shades the day, for a weekend or a holiday. Entry is still allowed unless the day is locked.
 * @property locked makes the day's cells read-only, for example after approval or when payroll is closed.
 * @property lockReason a short reason shown under the heading and read with each cell. A reason locks the day even
 * when [locked] is `false`.
 * @property note a short note under the heading, such as a holiday or leave name.
 * @property expectedMinutes the minutes expected that day, shown as "of 8h" under the day total.
 */
@Immutable
public class MonarchTimesheetGridDay(
    public val date: Long,
    public val label: String? = null,
    public val today: Boolean = false,
    public val nonWorking: Boolean = false,
    public val locked: Boolean = false,
    public val lockReason: String? = null,
    public val note: String? = null,
    public val expectedMinutes: Int? = null,
) {
    override fun toString(): String = "MonarchTimesheetGridDay(date=$date)"
}

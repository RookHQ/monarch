package dev.monarchui.components.charts.burndownchart

import androidx.compose.runtime.Immutable

/**
 * One day (or other step) of a `MonarchBurndownChart`: a date or a label, the work left and the scope.
 *
 * @property label the day as written by the app, such as "Mon", or `null` for a dated day.
 * @property date the day as epoch milliseconds (UTC), written by the chart's date format, or `null` for a labelled
 * day.
 * @property remaining the work left at the end of the day. `null` for a day not reached yet; a value that is not
 * finite is a gap.
 * @property scope the total committed work that day. A change from the day before is drawn as a scope change.
 */
@Immutable
public class MonarchBurndownChartPoint private constructor(
    public val label: String?,
    public val date: Long?,
    public val remaining: Double?,
    public val scope: Double?,
) {
    /**
     * A day written as [label].
     *
     * @param label the day as written by the app, already localised, such as "Mon" or "Day 3".
     * @param remaining the work left at the end of the day, or `null` for a day not reached yet.
     * @param scope the total committed work that day, or `null` when the sprint has no scope.
     */
    public constructor(label: String, remaining: Double? = null, scope: Double? = null) :
        this(label, null, remaining, scope)

    /**
     * A day at [date].
     *
     * @param date the day as epoch milliseconds (UTC), as in the Material 3 `DatePickerState`.
     * @param remaining the work left at the end of the day, or `null` for a day not reached yet.
     * @param scope the total committed work that day, or `null` when the sprint has no scope.
     */
    public constructor(date: Long, remaining: Double? = null, scope: Double? = null) :
        this(null, date, remaining, scope)
}

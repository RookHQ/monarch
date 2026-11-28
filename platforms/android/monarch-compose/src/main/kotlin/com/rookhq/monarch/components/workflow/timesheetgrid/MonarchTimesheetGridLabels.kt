package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings and formats of a `MonarchTimesheetGrid`. Every `null` keeps the built-in
 * string `monarch_timesheetgrid_<key>` or the built-in format.
 *
 * @property caption the spoken name of the grid: "Time per row and day".
 * @property rowHeader the heading of the first column: "Project".
 * @property total the heading of the total column: "Total".
 * @property dayTotal the name of the day totals row: "Day total".
 * @property days the spoken name of the day tabs: "Days".
 * @property rowActions the spoken name of a row's actions button from the row's name: "Actions for Atlas".
 * @property actionsText the visible text of the actions button when no icon is given: "Actions".
 * @property cellLabel the name of a cell from the row's name and the day: "Atlas, Mon, Jan 12".
 * @property locked read with a locked day without a reason: "Locked".
 * @property noTime read for an empty cell or total: "No time".
 * @property invalid the message for text that is not a duration: "Use a time such as 1:30, 1.5 or 90m.".
 * @property tooLong the message for a value above the daily maximum, from the maximum written as a duration:
 * "A day has at most 24h.".
 * @property saveFailed the message when a save throws without a message: "That time could not be saved.".
 * @property expected the text under a day total from the expected duration: "of 8h".
 * @property overLimit shown and read with a day total above the daily maximum: "Over limit".
 * @property empty the message shown without rows: "No time logged for this period.".
 * @property today shown and read with today: "Today".
 * @property saving read with a cell while it is saved: "Saving".
 * @property periodTotal a row's total in the day layout from the duration: "Total 7h 30m".
 * @property duration writes minutes for the eye. By default "1h 30m".
 * @property spokenDuration writes minutes for TalkBack. By default the locale's words, "1 hour, 30 minutes".
 * @property shortDay writes a day key for a heading. By default "Mon 12" in the locale, in UTC.
 * @property longDay writes a day key for names. By default "Mon, Jan 12" in the locale, in UTC.
 */
@Immutable
public class MonarchTimesheetGridLabels(
    public val caption: String? = null,
    public val rowHeader: String? = null,
    public val total: String? = null,
    public val dayTotal: String? = null,
    public val days: String? = null,
    public val rowActions: ((row: String) -> String)? = null,
    public val actionsText: String? = null,
    public val cellLabel: ((row: String, day: String) -> String)? = null,
    public val locked: String? = null,
    public val noTime: String? = null,
    public val invalid: String? = null,
    public val tooLong: ((maximum: String) -> String)? = null,
    public val saveFailed: String? = null,
    public val expected: ((duration: String) -> String)? = null,
    public val overLimit: String? = null,
    public val empty: String? = null,
    public val today: String? = null,
    public val saving: String? = null,
    public val periodTotal: ((duration: String) -> String)? = null,
    public val duration: ((minutes: Int) -> String)? = null,
    public val spokenDuration: ((minutes: Int) -> String)? = null,
    public val shortDay: ((day: Long) -> String)? = null,
    public val longDay: ((day: Long) -> String)? = null,
)

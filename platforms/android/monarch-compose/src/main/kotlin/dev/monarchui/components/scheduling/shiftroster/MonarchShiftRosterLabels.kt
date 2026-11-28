package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchShiftRoster`. Every `null` keeps the built-in string
 * `monarch_shiftroster_<key>`.
 *
 * @property roster the name of the roster read by TalkBack, "Shift roster".
 * @property person the header of the people column in the grid and the name of the person tabs, "Person".
 * @property day the name of the day tabs, "Day".
 * @property off a day off, "Off".
 * @property unassigned a day with no assignment, "Not assigned".
 * @property clear the menu item that removes an assignment, "Clear".
 * @property menuTitle the heading of a cell's menu, from the person's name and the day: "Alex Kim, Wednesday,
 * November 4".
 * @property cell what TalkBack reads for a cell, from the person's name, the day and the assignment: "Alex Kim,
 * Wednesday, November 4: Morning, 6:00 AM to 2:00 PM".
 * @property range a shift's start and end as TalkBack reads them: "6:00 AM to 2:00 PM".
 * @property overnight added to a shift that ends the next day, "overnight".
 * @property hours the header of the hours column and the name of a person's total, "Hours".
 * @property hoursValue a total of hours, already rounded to one decimal: "37.5 h".
 * @property working the label of the grid's working-count row, "Working".
 * @property workingValue the number of people working on a day: "3 working".
 * @property today the marker of today, "Today".
 * @property emptyTitle the title of the empty state, "No one to schedule".
 * @property emptyDescription the message of the empty state.
 * @property loading what TalkBack reads while the roster loads, "Loading roster".
 */
@Immutable
public class MonarchShiftRosterLabels(
    public val roster: String? = null,
    public val person: String? = null,
    public val day: String? = null,
    public val off: String? = null,
    public val unassigned: String? = null,
    public val clear: String? = null,
    public val menuTitle: ((name: String, day: String) -> String)? = null,
    public val cell: ((name: String, day: String, assignment: String) -> String)? = null,
    public val range: ((start: String, end: String) -> String)? = null,
    public val overnight: String? = null,
    public val hours: String? = null,
    public val hoursValue: ((hours: String) -> String)? = null,
    public val working: String? = null,
    public val workingValue: ((count: Int) -> String)? = null,
    public val today: String? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val loading: String? = null,
)

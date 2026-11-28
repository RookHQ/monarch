package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTeamCalendar`. Every `null` keeps the built-in string
 * `monarch_teamcalendar_<key>`.
 *
 * @property person the header of the people column, "Person".
 * @property grid the name of the grid read by TalkBack, given the range: "Team calendar, November 2026".
 * @property today read with the date of today, "Today".
 * @property holiday read with a holiday's day, given its name: "Holiday: Thanksgiving".
 * @property range the days of a bar that spans several days, given the first and the last: "Monday, November 2
 * to Friday, November 6".
 * @property firstHalf read for a half day in the first half, "first half".
 * @property secondHalf read for a half day in the second half, "second half".
 * @property pending read for an entry waiting for approval, "pending".
 * @property emptyTitle the title of the empty state when there are no members, "Nobody to show".
 * @property emptyDescription the message of the empty state, "People and their time away appear here.".
 * @property legend the line under the grid that explains shading, half bars and faded bars.
 * @property loading read by TalkBack while the placeholder is shown, "Loading team calendar".
 */
@Immutable
public class MonarchTeamCalendarLabels(
    public val person: String? = null,
    public val grid: ((String) -> String)? = null,
    public val today: String? = null,
    public val holiday: ((String) -> String)? = null,
    public val range: ((String, String) -> String)? = null,
    public val firstHalf: String? = null,
    public val secondHalf: String? = null,
    public val pending: String? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val legend: String? = null,
    public val loading: String? = null,
)

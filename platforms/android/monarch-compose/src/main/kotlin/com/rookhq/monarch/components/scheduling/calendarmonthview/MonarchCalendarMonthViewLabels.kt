package com.rookhq.monarch.components.scheduling.calendarmonthview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCalendarMonthView`.
 * Every `null` keeps the built-in string `monarch_calendarmonthview_<key>`.
 *
 * @property today added to what TalkBack reads for today, "Today".
 * @property day what TalkBack reads for a day, given its full date and its number of events: "Wednesday,
 * November 4, 2026, 3 events".
 * @property more the overflow line of a wide cell, given the number of hidden events: "+2 more".
 * @property moreShort the overflow mark after the dots of a narrow cell: "+2".
 * @property noEvents shown under the selected day when it has no events, "No events".
 * @property allDay the time of an all-day event, and of a day a longer event covers whole, "All day".
 * @property until what TalkBack reads for the last day of an event that started earlier: "until 10:00".
 * @property range what TalkBack reads for an event's start and end: "9:00 to 10:00".
 * @property moveDayEarlier the TalkBack action that moves an event one day earlier.
 * @property moveDayLater the TalkBack action that moves an event one day later.
 * @property moveWeekEarlier the TalkBack action that moves an event one week earlier.
 * @property moveWeekLater the TalkBack action that moves an event one week later.
 */
@Immutable
public class MonarchCalendarMonthViewLabels(
    public val today: String? = null,
    public val day: ((date: String, count: Int) -> String)? = null,
    public val more: ((Int) -> String)? = null,
    public val moreShort: ((Int) -> String)? = null,
    public val noEvents: String? = null,
    public val allDay: String? = null,
    public val until: ((String) -> String)? = null,
    public val range: ((String, String) -> String)? = null,
    public val moveDayEarlier: String? = null,
    public val moveDayLater: String? = null,
    public val moveWeekEarlier: String? = null,
    public val moveWeekLater: String? = null,
)

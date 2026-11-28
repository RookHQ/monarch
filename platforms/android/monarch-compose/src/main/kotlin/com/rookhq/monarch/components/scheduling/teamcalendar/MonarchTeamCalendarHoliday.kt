package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.runtime.Immutable

/**
 * A non-working day for everyone in a `MonarchTeamCalendar`, such as a public holiday. Its column is shaded and
 * its name is read with the day and shown in a tooltip on a long press of the day.
 *
 * @property date any time on the day, as epoch milliseconds read in UTC.
 * @property name the holiday's name.
 */
@Immutable
public class MonarchTeamCalendarHoliday(
    public val date: Long,
    public val name: String,
)

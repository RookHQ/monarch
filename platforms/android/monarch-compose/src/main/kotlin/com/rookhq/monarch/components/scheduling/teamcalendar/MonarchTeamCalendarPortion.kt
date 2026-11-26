package com.rookhq.monarch.components.scheduling.teamcalendar

/** Which part of a day an entry of a `MonarchTeamCalendar` covers. */
public enum class MonarchTeamCalendarPortion {
    /** The whole day. */
    Full,

    /** The first half of the day, drawn as a half bar at the start of the day. */
    FirstHalf,

    /** The second half of the day, drawn as a half bar at the end of the day. */
    SecondHalf,
}

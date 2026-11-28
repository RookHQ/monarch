package com.rookhq.monarch.components.scheduling.weeklyhourseditor

/** Why the intervals of one day of a `MonarchWeeklyHoursEditor` value are invalid. */
public enum class MonarchWeeklyHoursError {
    /** A time falls outside the day: a start before 0 or an end after 1440 minutes. */
    Range,

    /** An interval does not end after it starts. */
    Order,

    /** Two intervals of the day overlap. */
    Overlap,
}

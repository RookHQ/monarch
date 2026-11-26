package com.rookhq.monarch.components.scheduling.calendarmonthview

import androidx.compose.runtime.Immutable

/**
 * How the app marks one day of a `MonarchCalendarMonthView`: time off, a
 * closure, a holiday.
 *
 * Hatching is decoration; say what it means in [badge], which is shown in the
 * cell and read by TalkBack with the day.
 *
 * @property hatched draws diagonal lines over the day, for days off or closures.
 * @property badge a short note under the date, such as "Holiday". Also added to what TalkBack reads for the day.
 */
@Immutable
public class MonarchCalendarMonthViewDayState(
    public val hatched: Boolean = false,
    public val badge: String? = null,
)

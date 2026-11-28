package dev.monarchui.components.scheduling.timegrid

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTimeGrid`. Every `null` keeps the built-in string
 * `monarch_timegrid_<key>`.
 *
 * @property allDay the label of the all-day row and the time of an all-day event, "All day".
 * @property createEvent what TalkBack reads for an empty hour, from the day and the time:
 * "Create event, Wednesday, November 4, 9:00 AM".
 * @property today added to today's heading for TalkBack, "Today".
 * @property openDay the click label of a day heading that opens the day, from the day: "Open Wednesday, November 4".
 * @property range what TalkBack reads for an event's start and end: "9:30 AM to 10:15 AM".
 * @property until what TalkBack reads on a later day of an event that started earlier: "until 2:00 AM".
 * @property moveEarlier the TalkBack action that moves an event earlier by the snap interval, "Move earlier".
 * @property moveLater the TalkBack action that moves an event later by the snap interval, "Move later".
 * @property moveToPreviousDay the TalkBack action that moves an event to the previous column, "Move to previous day".
 * @property moveToNextDay the TalkBack action that moves an event to the next column, "Move to next day".
 */
@Immutable
public class MonarchTimeGridLabels(
    public val allDay: String? = null,
    public val createEvent: ((String) -> String)? = null,
    public val today: String? = null,
    public val openDay: ((String) -> String)? = null,
    public val range: ((String, String) -> String)? = null,
    public val until: ((String) -> String)? = null,
    public val moveEarlier: String? = null,
    public val moveLater: String? = null,
    public val moveToPreviousDay: String? = null,
    public val moveToNextDay: String? = null,
)

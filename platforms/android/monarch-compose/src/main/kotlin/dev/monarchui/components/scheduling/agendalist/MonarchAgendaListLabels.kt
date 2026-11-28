package dev.monarchui.components.scheduling.agendalist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAgendaList`. Every
 * `null` keeps the built-in string `monarch_agendalist_<key>`.
 *
 * @property today the marker in the heading of today, "Today".
 * @property allDay the time of an all-day event, and of a day a longer event covers whole, "All day".
 * @property count the number of events in a day heading: "3 events".
 * @property emptyTitle the title of the empty state, "Nothing scheduled".
 * @property until what TalkBack reads for the last day of a longer event: "until 02:00".
 * @property range what TalkBack reads for a start and an end: "09:30 to 10:15".
 */
@Immutable
public class MonarchAgendaListLabels(
    public val today: String? = null,
    public val allDay: String? = null,
    public val count: ((Int) -> String)? = null,
    public val emptyTitle: String? = null,
    public val until: ((String) -> String)? = null,
    public val range: ((String, String) -> String)? = null,
)

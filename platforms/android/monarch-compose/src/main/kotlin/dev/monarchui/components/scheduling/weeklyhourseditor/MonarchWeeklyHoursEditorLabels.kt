package dev.monarchui.components.scheduling.weeklyhourseditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchWeeklyHoursEditor`. Every `null` keeps the built-in string
 * `monarch_weeklyhourseditor_<key>`. `day` is the localized name of the weekday, such as "Monday".
 *
 * @property title the heading of the editor, "Weekly hours".
 * @property unavailable shown for a day that is switched off, "Unavailable".
 * @property start the label of an interval's start menu, "Start".
 * @property end the label of an interval's end menu, "End".
 * @property add the visible text of a day's add button, "Add interval".
 * @property addItem the TalkBack name of the add button, from the day; keep [add] in it.
 * @property remove the visible text of an interval's remove button without an icon, "Remove".
 * @property removeItem the TalkBack name of the remove button, from the day and the interval's position.
 * @property copyToAll the visible text of a day's copy button, "Copy to all".
 * @property copyToAllItem the TalkBack name of the copy button, from the day; keep [copyToAll] in it.
 * @property copied announced after a copy, from the day.
 * @property range one interval as text in the read-only view, from its start and end: "9:00 AM – 5:00 PM".
 * @property midnight the end of the day on a 12-hour clock, "Midnight". A 24-hour clock shows "24:00".
 * @property rangeError the message under a day with a time outside the day.
 * @property orderError the message under a day with an interval that ends before it starts.
 * @property overlapError the message under a day with overlapping intervals.
 */
@Immutable
public class MonarchWeeklyHoursEditorLabels(
    public val title: String? = null,
    public val unavailable: String? = null,
    public val start: String? = null,
    public val end: String? = null,
    public val add: String? = null,
    public val addItem: ((day: String) -> String)? = null,
    public val remove: String? = null,
    public val removeItem: ((day: String, position: Int) -> String)? = null,
    public val copyToAll: String? = null,
    public val copyToAllItem: ((day: String) -> String)? = null,
    public val copied: ((day: String) -> String)? = null,
    public val range: ((start: String, end: String) -> String)? = null,
    public val midnight: String? = null,
    public val rangeError: String? = null,
    public val orderError: String? = null,
    public val overlapError: String? = null,
)

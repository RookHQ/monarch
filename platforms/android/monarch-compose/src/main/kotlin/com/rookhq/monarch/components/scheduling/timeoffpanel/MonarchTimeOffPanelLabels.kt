package com.rookhq.monarch.components.scheduling.timeoffpanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTimeOffPanel`. Every
 * `null` keeps the built-in string `monarch_timeoffpanel_<key>`.
 *
 * @property title the panel's heading, "Time off".
 * @property description the line under the title.
 * @property add the add button and the save button of the sheet, "Add time off".
 * @property includePast the switch that shows past entries, "Include past".
 * @property closure the badge of an entry for everyone, "Closure".
 * @property everyone the name of a closure and its choice in Who, "Everyone".
 * @property past the badge of a past entry, "Past".
 * @property allDay after the dates of an all-day entry, "all day".
 * @property allDayLabel the all-day switch of the sheet, "All day".
 * @property remove the visible text of the remove button without an icon, "Remove".
 * @property removeEntry the spoken name of an entry's remove button, from who and when.
 * @property removeTitle the title of the confirmation, "Remove this time off?".
 * @property removeDescription the consequence in the confirmation.
 * @property removeConfirm the confirming button, "Remove".
 * @property emptyTitle the empty state's title, "No time off planned".
 * @property emptyDescription the empty state's message.
 * @property loading the spoken name of the placeholders, "Loading time off".
 * @property retry the retry button of the error banner, "Try again".
 * @property sheetDescription the line under the sheet's title, from the zone's name.
 * @property who the person select, "Who".
 * @property type the type select, "Type".
 * @property closureNotice the notice when Everyone is chosen.
 * @property from the first day of an all-day entry, "From".
 * @property until the last day of an all-day entry, "Until (inclusive)".
 * @property start the start of a timed entry, "Start".
 * @property end the end of a timed entry, "End".
 * @property reason the reason field, "Reason (optional)".
 * @property reasonDescription the helper of the reason field, "Visible to your team."
 * @property cancel the buttons that close the sheet, the confirmation and the pickers, "Cancel".
 * @property confirmPick the button that takes a date or time from a picker, "OK".
 * @property endBeforeStart the error when the end is before the start.
 * @property failed the error when adding or removing threw without a message.
 */
@Immutable
public class MonarchTimeOffPanelLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val add: String? = null,
    public val includePast: String? = null,
    public val closure: String? = null,
    public val everyone: String? = null,
    public val past: String? = null,
    public val allDay: String? = null,
    public val allDayLabel: String? = null,
    public val remove: String? = null,
    public val removeEntry: ((who: String, time: String) -> String)? = null,
    public val removeTitle: String? = null,
    public val removeDescription: String? = null,
    public val removeConfirm: String? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val loading: String? = null,
    public val retry: String? = null,
    public val sheetDescription: ((zone: String) -> String)? = null,
    public val who: String? = null,
    public val type: String? = null,
    public val closureNotice: String? = null,
    public val from: String? = null,
    public val until: String? = null,
    public val start: String? = null,
    public val end: String? = null,
    public val reason: String? = null,
    public val reasonDescription: String? = null,
    public val cancel: String? = null,
    public val confirmPick: String? = null,
    public val endBeforeStart: String? = null,
    public val failed: String? = null,
)

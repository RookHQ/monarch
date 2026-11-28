package dev.monarchui.components.scheduling.businesshourseditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchBusinessHoursEditor`. Every `null` keeps the built-in
 * string `monarch_businesshourseditor_<key>`. The strings of the weekly hours go in `weeklyHoursLabels`.
 *
 * @property name the label of the name field, "Name".
 * @property namePlaceholder the placeholder of the name field, "Office hours".
 * @property nameRequired the error of a missing name, "Enter a name.".
 * @property timeZone the label of the time zone picker, "Time zone".
 * @property timeZoneDescription the help under the time zone picker.
 * @property noOpenHours the error when no interval is open and one is required, "Open at least one interval.".
 * @property effectiveFrom the label of the first effective day, "Effective from".
 * @property effectiveTo the label of the last effective day, "Effective until".
 * @property effectiveOrder the error when the last day is before the first.
 * @property exceptions the heading of the holidays, "Holidays and exceptions".
 * @property exceptionsDescription the help under that heading.
 * @property noExceptions shown when there are none, "No holidays or exceptions.".
 * @property addException the button that adds a date, "Add date".
 * @property exceptionDate the label of the date of the nth row, "Date 1".
 * @property exceptionName the label of the name of the nth row, "Name of date 1".
 * @property exceptionNamePlaceholder the placeholder of a row's name, "Public holiday".
 * @property exceptionKind the label of the menu of the nth row, "Hours on date 1".
 * @property closed the option for a date closed all day, "Closed all day".
 * @property specialHours the option for a date open at other times, "Open at other times".
 * @property exceptionStart the label of the opening time of the nth row, "Opening time of date 1".
 * @property exceptionEnd the label of the closing time of the nth row, "Closing time of date 1".
 * @property remove the visible text of a row's remove button without an icon, "Remove".
 * @property removeException the TalkBack name of the remove button of the nth row, given its trimmed name:
 * "Remove New Year's Day", or "Remove date 1" without a name.
 * @property exceptionError the message of a row's problem.
 * @property default the label of the default checkbox, "Default".
 * @property defaultDescription the help of the default checkbox.
 * @property endOfDay the closing time at the end of the day, "End of day".
 * @property chooseDate the text of a date button with no date, "Choose a date".
 * @property clearDate the button that clears an effective date, "Clear".
 * @property confirm the confirm button of the date dialog, "OK".
 * @property cancel the cancel button of the date dialog, "Cancel".
 */
@Immutable
public class MonarchBusinessHoursEditorLabels(
    public val name: String? = null,
    public val namePlaceholder: String? = null,
    public val nameRequired: String? = null,
    public val timeZone: String? = null,
    public val timeZoneDescription: String? = null,
    public val noOpenHours: String? = null,
    public val effectiveFrom: String? = null,
    public val effectiveTo: String? = null,
    public val effectiveOrder: String? = null,
    public val exceptions: String? = null,
    public val exceptionsDescription: String? = null,
    public val noExceptions: String? = null,
    public val addException: String? = null,
    public val exceptionDate: ((position: Int) -> String)? = null,
    public val exceptionName: ((position: Int) -> String)? = null,
    public val exceptionNamePlaceholder: String? = null,
    public val exceptionKind: ((position: Int) -> String)? = null,
    public val closed: String? = null,
    public val specialHours: String? = null,
    public val exceptionStart: ((position: Int) -> String)? = null,
    public val exceptionEnd: ((position: Int) -> String)? = null,
    public val remove: String? = null,
    public val removeException: ((position: Int, name: String) -> String)? = null,
    public val exceptionError: ((MonarchBusinessHoursExceptionError) -> String)? = null,
    public val default: String? = null,
    public val defaultDescription: String? = null,
    public val endOfDay: String? = null,
    public val chooseDate: String? = null,
    public val clearDate: String? = null,
    public val confirm: String? = null,
    public val cancel: String? = null,
)

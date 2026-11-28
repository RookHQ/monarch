package com.rookhq.monarch.components.scheduling.slotpicker

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.scheduling.minimonth.MonarchMiniMonthLabels
import com.rookhq.monarch.components.scheduling.timezoneselect.MonarchTimezoneSelectLabels

/**
 * Replacements for the built-in strings of a `MonarchSlotPicker`. Every
 * `null` keeps the built-in string `monarch_slotpicker_<key>`.
 *
 * @property chooseDay shown before a day is chosen, "Choose a day to see available times."
 * @property empty shown when the day has no times, "No times are available on this day. Try another day."
 * @property loading announced while times load, "Loading available times".
 * @property loaded announced when times arrive, from the number of open times and the day: "6 times on
 * Wednesday, November 4".
 * @property times the spoken name of the group of times, from the day: "Available times on Wednesday, November 4".
 * @property seatsLeft under a time with limited places, from the number: "3 left".
 * @property timeZone the label of the time zone picker, "Time zone".
 * @property shownIn shown when the zone cannot be changed, from the zone's name: "Times shown in Central European
 * Standard Time".
 * @property convertedFrom shown when the business works in another zone, from its name: "Times are converted
 * from Eastern Standard Time."
 * @property notOpen the warning when the selection is not one of the day's open times, from the time: "9:30 AM
 * is not an open time on this day."
 * @property chooseOutside the button of `onChooseOutside`, "Choose another time".
 * @property retry the button of `onRetry`, "Try again".
 * @property miniMonth the strings of the month calendar.
 * @property timezoneSelect the strings of the time zone picker.
 */
@Immutable
public class MonarchSlotPickerLabels(
    public val chooseDay: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val loaded: ((count: Int, day: String) -> String)? = null,
    public val times: ((day: String) -> String)? = null,
    public val seatsLeft: ((count: Int) -> String)? = null,
    public val timeZone: String? = null,
    public val shownIn: ((zone: String) -> String)? = null,
    public val convertedFrom: ((zone: String) -> String)? = null,
    public val notOpen: ((time: String) -> String)? = null,
    public val chooseOutside: String? = null,
    public val retry: String? = null,
    public val miniMonth: MonarchMiniMonthLabels = MonarchMiniMonthLabels(),
    public val timezoneSelect: MonarchTimezoneSelectLabels = MonarchTimezoneSelectLabels(),
)

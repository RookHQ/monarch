package dev.monarchui.components.scheduling.calendartoolbar

/** The views a `MonarchCalendarToolbar` switches between. Each also names the unit the arrows move by. */
public enum class MonarchCalendarToolbarView {
    /** One day; the arrows move by a day. */
    Day,

    /** One week; the arrows move by a week. */
    Week,

    /** One month; the arrows move by a month. */
    Month,

    /** A list of what is coming; the arrows move by a period the app defines. */
    Agenda,
}

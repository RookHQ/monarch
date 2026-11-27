package com.rookhq.monarch.components.datadisplay.periodselector

/**
 * A named period of a `MonarchPeriodSelector`. Rolling periods
 * ([Last7Days] and the others) end today and count today.
 */
public enum class MonarchPeriodPreset {
    /** Today only. */
    Today,

    /** Yesterday only. */
    Yesterday,

    /** The last 7 days, today included. */
    Last7Days,

    /** The last 14 days, today included. */
    Last14Days,

    /** The last 30 days, today included. */
    Last30Days,

    /** The last 90 days, today included. */
    Last90Days,

    /** The last 180 days, today included. */
    Last180Days,

    /** The last 365 days, today included; shown as "Last 12 months". */
    Last365Days,

    /** From the first day of this week to today. */
    ThisWeek,

    /** From the first day of this month to today. */
    ThisMonth,

    /** The whole of last month. */
    LastMonth,

    /** From the first day of this quarter to today. */
    ThisQuarter,

    /** The whole of last quarter. */
    LastQuarter,

    /** From the first of January to today. */
    ThisYear,

    /** Dates the person picks in a date range picker. */
    Custom,
}

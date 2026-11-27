package com.rookhq.monarch.components.scheduling.shiftroster

/** How a `MonarchShiftRoster` lays out people and days. */
public enum class MonarchShiftRosterLayout {
    /** The grid when every day column fits the width and the text is not enlarged much, otherwise [ByDay]. */
    Automatic,

    /** People down the side and days across, scrolled sideways when it does not fit. For tablets. */
    Grid,

    /** One day at a time, picked with tabs, with a row per person. The phone layout. */
    ByDay,

    /** One person at a time, picked with tabs, with a row per day. */
    ByPerson,
}

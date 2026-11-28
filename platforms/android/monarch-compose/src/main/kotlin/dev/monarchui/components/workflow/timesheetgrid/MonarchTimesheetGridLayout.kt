package dev.monarchui.components.workflow.timesheetgrid

/** How a `MonarchTimesheetGrid` lays out rows and days. */
public enum class MonarchTimesheetGridLayout {
    /** The grid when every day column fits the width and the text is not enlarged much, otherwise [Day]. */
    Automatic,

    /** Rows down the side and days across, scrolled sideways when it does not fit. For tablets. */
    Grid,

    /** One day at a time, picked with tabs, with a duration field or value per row. The phone layout. */
    Day,
}

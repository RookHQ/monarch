package dev.monarchui.components.datagrid.datagrid

/**
 * The row spacing of a `MonarchDataGrid`. Both keep 48dp touch targets; compact only removes padding around
 * the cells, so more rows fit on a tablet.
 */
public enum class MonarchDataGridDensity {
    /** Padding above and below each cell. The default. */
    Comfortable,

    /** Less padding above and below each cell. */
    Compact,
}

package com.rookhq.monarch.components.datagrid.datagrid

/** The edge a `MonarchDataGrid` column is pinned to: it stays visible while the other columns scroll sideways. */
public enum class MonarchDataGridPin {
    /** Kept at the start edge, before the scrolling columns. */
    Start,

    /** Kept at the end edge, after the scrolling columns. */
    End,
}

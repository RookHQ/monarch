package com.rookhq.monarch.components.datagrid.datatable

/** How a `MonarchDataTable` lays out its rows. */
public enum class MonarchDataTableLayout {
    /**
     * A table below font scale 1.5 (it scrolls sideways when the columns need more room), a list of records
     * otherwise.
     */
    Automatic,

    /** Always a table with a header row and a cell per column. */
    Table,

    /** Always a list of records with a labelled line per field. */
    List,
}

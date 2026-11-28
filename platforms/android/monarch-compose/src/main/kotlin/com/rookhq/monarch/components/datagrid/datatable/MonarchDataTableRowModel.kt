package com.rookhq.monarch.components.datagrid.datatable

/** How one row is drawn and what it can do, shared by the table and the list layouts. */
internal class MonarchDataTableRowModel(
    val name: String,
    val selected: Boolean?,
    val onSelectedChange: ((Boolean) -> Unit)?,
    val actions: List<MonarchDataTableRowAction>,
    val onOpen: (() -> Unit)?,
)

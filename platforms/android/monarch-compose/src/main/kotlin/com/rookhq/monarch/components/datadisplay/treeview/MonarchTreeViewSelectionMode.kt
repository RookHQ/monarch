package com.rookhq.monarch.components.datadisplay.treeview

/** How items of a `MonarchTreeView` are selected. The same names exist on every platform. */
public enum class MonarchTreeViewSelectionMode {
    /** No selection, for navigation trees such as an outline. Tapping a parent expands it. */
    None,

    /** One item at a time. The default. */
    Single,

    /** Any number of items; tapping an item adds or removes it. */
    Multiple,
}

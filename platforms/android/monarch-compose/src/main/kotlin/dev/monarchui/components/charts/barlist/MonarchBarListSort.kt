package dev.monarchui.components.charts.barlist

/** The order of the rows of a `MonarchBarList`. Rows without a value always go last. */
public enum class MonarchBarListSort {
    /** The largest value first. The default. */
    Descending,

    /** The smallest value first. */
    Ascending,

    /** The order the items were given in. */
    None,
}

package dev.monarchui.components.charts.deltaindicator

/** Which way a value moved. Passed to the icon slot of `MonarchDeltaIndicator`, so it can draw the right arrow. */
public enum class MonarchDeltaIndicatorDirection {
    /** The value increased. */
    Up,

    /** The value decreased. */
    Down,

    /** The change is zero, inside the flat threshold, or written the same as zero. */
    Flat,
}

package dev.monarchui.components.datadisplay.insightstrip

/** How a `MonarchInsightStrip` lays out its cards. The same names exist on every platform. */
public enum class MonarchInsightStripLayout {
    /** One row that scrolls sideways; each card is most of the width. The default. */
    Scroll,

    /** One column. */
    Stack,

    /** One to three columns, by the width available. */
    Grid,
}

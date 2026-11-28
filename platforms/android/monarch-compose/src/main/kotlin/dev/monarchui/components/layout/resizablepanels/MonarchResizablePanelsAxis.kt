package dev.monarchui.components.layout.resizablepanels

/** The direction of a `MonarchResizablePanels`. The same names exist on every platform. */
public enum class MonarchResizablePanelsAxis {
    /** Side by side: the first panel at the start, a vertical separator between them. The default. */
    Horizontal,

    /** Stacked: the first panel on top, a horizontal separator between them. Needs a bounded height. */
    Vertical,
}

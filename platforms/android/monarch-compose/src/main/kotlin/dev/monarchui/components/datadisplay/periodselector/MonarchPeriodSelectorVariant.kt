package dev.monarchui.components.datadisplay.periodselector

/** How a `MonarchPeriodSelector` shows its presets. */
public enum class MonarchPeriodSelectorVariant {
    /** Every preset as a segment of a Material 3 segmented button row, which scrolls sideways when narrow. */
    Segmented,

    /** A `MonarchSelect` field with a menu of the presets, for toolbars and narrow screens. */
    Select,
}

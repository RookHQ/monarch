package dev.monarchui.components.datadisplay.statusbadge

/** The emphasis of a `MonarchStatusBadge`. The same names exist on every platform. */
public enum class MonarchStatusBadgeVariant {
    /** A tinted pill with strong text. The default. */
    Subtle,

    /** No fill, a border in the tone. For dense tables. */
    Outline,

    /** A strong fill. For one prominent status on a screen. */
    Solid,

    /** A coloured dot and plain text, without a pill. The quietest form. */
    Dot,
}

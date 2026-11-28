package dev.monarchui.components.datadisplay.statcard

/** Whether a `MonarchStatCard` draws its own surface. */
public enum class MonarchStatCardVariant {
    /** A card with the surface fill, a border and padding. */
    Card,

    /** No surface, border or padding, for a figure inside another surface. */
    Plain,
}

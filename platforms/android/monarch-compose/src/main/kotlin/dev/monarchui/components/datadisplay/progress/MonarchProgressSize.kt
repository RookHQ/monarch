package dev.monarchui.components.datadisplay.progress

/** The size of a `MonarchProgress`: the thickness of a bar, the diameter of a ring. */
public enum class MonarchProgressSize {
    /** A 4dp bar, a 24dp ring: in a list row or next to a label. */
    Sm,

    /** The default: an 8dp bar like the web, a 40dp ring. */
    Md,

    /** A 12dp bar, a 64dp ring: the main progress of a screen, with room for the value inside the ring. */
    Lg,

    /** An extra large ring (96dp) with the value inside; a bar is 16dp. */
    Xl,
}

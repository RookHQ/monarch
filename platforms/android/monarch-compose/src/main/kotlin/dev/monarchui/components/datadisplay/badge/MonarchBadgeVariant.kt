package dev.monarchui.components.datadisplay.badge

/** The emphasis of a `MonarchBadge`. The same three names exist on every platform. */
public enum class MonarchBadgeVariant {
    /** A tinted fill. For status in lists and tables. The default. */
    Subtle,

    /** A strong fill. For counts and states that must stand out. */
    Solid,

    /** An outline without a fill. For metadata. */
    Outline,
}

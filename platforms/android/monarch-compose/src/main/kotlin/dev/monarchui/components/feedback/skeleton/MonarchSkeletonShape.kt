package dev.monarchui.components.feedback.skeleton

/** The outline of a `MonarchSkeleton`. The same three names exist on the web. */
public enum class MonarchSkeletonShape {
    /** A line of text: the full width given and three quarters of the body font size high. */
    Text,

    /** A block with 8dp corners, for a picture or a card: the full width given, at least 40dp high. The default. */
    Rect,

    /** A circle, for an avatar. 40dp unless `Modifier.size` sets another size. */
    Circle,
}

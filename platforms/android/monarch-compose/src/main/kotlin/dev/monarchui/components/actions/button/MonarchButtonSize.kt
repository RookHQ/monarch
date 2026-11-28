package dev.monarchui.components.actions.button

/**
 * The size of a `MonarchButton`. The same three names exist on every platform.
 *
 * The size sets the visible height, the same numbers as the web. The touch target is at least 48dp at every
 * size: the layout reserves it around the visible button and a tap in the reserved space presses the button.
 */
public enum class MonarchButtonSize {
    /** 32dp high, 12sp label. For dense layouts. */
    Sm,

    /** 36dp high, 14sp label. The default. */
    Md,

    /** 40dp high, 14sp label. For the main action of a screen. */
    Lg,
}

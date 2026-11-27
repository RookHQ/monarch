package com.rookhq.monarch.components.datadisplay.tag

/**
 * The size of a `MonarchTag`. The same two names exist on every platform.
 *
 * The size sets the text style and the visible height of a read-only tag. A
 * tag the user can act on is a Material 3 chip, 32dp high at both sizes, with
 * a 48dp touch target.
 */
public enum class MonarchTagSize {
    /** Caption text; 24dp high when read-only. For dense rows. */
    Sm,

    /** Label text; 28dp high when read-only. The default. */
    Md,
}

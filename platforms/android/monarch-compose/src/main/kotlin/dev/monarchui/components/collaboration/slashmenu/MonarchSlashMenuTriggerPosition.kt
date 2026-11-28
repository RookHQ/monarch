package dev.monarchui.components.collaboration.slashmenu

/** Where the trigger of a `MonarchSlashMenu` may be typed. */
public enum class MonarchSlashMenuTriggerPosition {
    /** Only at the start of a line, so paths and fractions such as 1/2 never open the menu. The default. */
    LineStart,

    /** At the start of a line or after white space. */
    WordStart,
}

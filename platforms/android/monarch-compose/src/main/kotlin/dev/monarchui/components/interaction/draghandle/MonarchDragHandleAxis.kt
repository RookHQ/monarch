package dev.monarchui.components.interaction.draghandle

/** Which way the item of a `MonarchDragHandle` moves. It selects the grip pattern and the move actions. */
public enum class MonarchDragHandleAxis {
    /** Up and down, as in a list. The default. */
    Vertical,

    /** Backward and forward in reading order, as in a row of tabs. */
    Horizontal,

    /** All four directions, as in a grid. */
    Both,
}

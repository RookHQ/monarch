package dev.monarchui.components.interaction.draghandle

/** Where an item is in a drag, shown by `MonarchDragHandle`. The same names exist on every platform. */
public enum class MonarchDragHandleState {
    /** At rest. The default. */
    Idle,

    /** Picked up and being moved. */
    Grabbed,

    /** Just dropped: a brief confirmation before the app returns to [Idle]. */
    Dropped,
}

package com.rookhq.monarch.components.interaction.draghandle

/**
 * One step a `MonarchDragHandle` asks the app to move its item.
 *
 * [Backward] and [Forward] are positions in reading order, so they need no change for right-to-left layouts.
 */
public enum class MonarchDragHandleDirection {
    /** One step up. */
    Up,

    /** One step down. */
    Down,

    /** One step back in reading order: left in a left-to-right layout, right in a right-to-left one. */
    Backward,

    /** One step forward in reading order. */
    Forward,
}

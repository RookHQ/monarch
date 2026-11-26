package com.rookhq.monarch.components.files.filecard

/** How a `MonarchFileCard` is laid out. The same names exist on every platform. */
public enum class MonarchFileCardLayout {
    /** A 4:3 preview above the name, for grids. The default. */
    Card,

    /** A small preview beside the name on one line, for lists. */
    Row,
}

package com.rookhq.monarch.components.scheduling.eventchip

/** The layout of a `MonarchEventChip`. The same three names exist on every platform. */
public enum class MonarchEventChipVariant {
    /** A colour dot, the time and the title on one line, for a month cell or a popover list. The default. */
    Dot,

    /** A tinted row with a colour bar at the start edge and room for a detail line, for agenda and all-day rows. */
    Bar,

    /** A tinted block that fills the size it is given, for an event placed in a time grid. */
    Block,
}

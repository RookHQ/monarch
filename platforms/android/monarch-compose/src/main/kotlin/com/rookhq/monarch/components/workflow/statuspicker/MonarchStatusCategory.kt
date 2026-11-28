package com.rookhq.monarch.components.workflow.statuspicker

/**
 * The workflow stage a status belongs to. It decides the status's glyph and
 * colour, and the order of the groups in a `MonarchStatusPicker` menu is the
 * order of these constants.
 */
public enum class MonarchStatusCategory {
    /** Not planned yet. Drawn as a dashed ring in the subtle text colour. */
    Backlog,

    /** Planned but not begun. Drawn as a ring in the muted text colour. */
    Unstarted,

    /** Being worked on. Drawn as a ring with its end half filled, in the strong warning colour. */
    Started,

    /** Finished. Drawn as a filled disc with a check mark, in the strong success colour. */
    Completed,

    /** Stopped without finishing. Drawn as a filled disc with a cross, in the subtle text colour. */
    Cancelled,
    ;

    /** Whether work on a status in this category is over: [Completed] or [Cancelled]. */
    public val isClosed: Boolean get() = this == Completed || this == Cancelled
}

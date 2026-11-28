package com.rookhq.monarch.components.workflow.dependencypicker

/** How another item relates to this one. The constants are in display order. */
public enum class MonarchDependencyRelation {
    /** The other item must finish first. */
    BlockedBy,

    /** This item must finish first. */
    Blocks,

    /** Linked without order. */
    Relates,

    /** This item is the same work as the other. */
    Duplicates,

    /** The other item is the same work as this one. */
    DuplicatedBy,
}

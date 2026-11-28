package dev.monarchui.components.files.pagetree

/** What a `MonarchPageTreeMove` moved. The same names exist on every platform. */
public enum class MonarchPageTreeMoveKind {
    /** A page, within its group or to another group. */
    Page,

    /** A group among the groups. */
    Group,
}

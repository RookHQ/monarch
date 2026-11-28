package dev.monarchui.components.workflow.orgchart

/** What a card in a `MonarchOrgChart` stands for. */
public enum class MonarchOrgChartNodeKind {
    /** A person, with an avatar. */
    Person,

    /** An open position: a muted card with a dashed border, read as "Open position". It cannot move or take people. */
    Vacancy,
}

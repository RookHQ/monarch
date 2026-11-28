package dev.monarchui.components.workflow.escalationpolicyeditor

/** Why a level of an escalation policy cannot be saved. */
public enum class MonarchEscalationLevelProblem {
    /** The level notifies nobody. */
    Targets,

    /** With cumulative timing, the level is not later than the one before. */
    Order,
}

package dev.monarchui.components.intelligence.aiplan

/** What the person decided about a `MonarchAiPlan`. */
public enum class MonarchAiPlanDecision {
    /** Not decided yet: the plan can be edited, approved or rejected. */
    Pending,

    /** Approved: the agent works through the steps. */
    Approved,

    /** Rejected: the agent does not take the steps. */
    Rejected,
}

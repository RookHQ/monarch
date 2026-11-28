package dev.monarchui.components.workflow.escalationpolicyeditor

/** How the times of a `MonarchEscalationPolicyEditor` are measured. */
public enum class MonarchEscalationTiming {
    /** Every time is measured from the start, so each level must come later than the one before. */
    Cumulative,

    /** Each time is measured from the previous level. */
    Interval,
}

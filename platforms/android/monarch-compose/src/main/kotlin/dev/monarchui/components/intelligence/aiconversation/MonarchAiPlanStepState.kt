package dev.monarchui.components.intelligence.aiconversation

/** Where a step of a plan is. */
public enum class MonarchAiPlanStepState {
    /** Not started. */
    Todo,

    /** In progress. */
    Active,

    /** Finished. */
    Done,

    /** Left out. */
    Skipped,

    /** Failed. */
    Failed,
}

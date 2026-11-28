package dev.monarchui.components.workflow.stagejourney

/** How much of a `MonarchStageJourney` is shown. */
public enum class MonarchStageJourneyVariant {
    /** The stage strip, the current-stage card and the moves. */
    Full,

    /** The stage strip only, for a list row or a header. */
    Strip,
}

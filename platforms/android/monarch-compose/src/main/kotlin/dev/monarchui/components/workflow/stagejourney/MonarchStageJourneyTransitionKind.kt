package dev.monarchui.components.workflow.stagejourney

/** What a move does. */
public enum class MonarchStageJourneyTransitionKind {
    /** One of the next steps; the first is the primary button. */
    Forward,

    /** Leaves the pipeline early (withdrawn, lost); shown quietly. */
    Exit,

    /** Undoes the last move; shown as "Back to …". Only the first is shown. */
    Back,
}

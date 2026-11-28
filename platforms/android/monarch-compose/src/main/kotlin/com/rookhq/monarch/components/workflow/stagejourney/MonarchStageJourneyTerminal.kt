package com.rookhq.monarch.components.workflow.stagejourney

/** How the current stage ends the journey. */
public enum class MonarchStageJourneyTerminal {
    /** The journey ended well: granted, won, hired. */
    Success,

    /** The journey ended early: withdrawn, lost. */
    Closed,
}

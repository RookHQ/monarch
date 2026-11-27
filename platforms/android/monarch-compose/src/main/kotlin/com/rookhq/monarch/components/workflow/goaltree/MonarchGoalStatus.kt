package com.rookhq.monarch.components.workflow.goaltree

/** How a goal in a `MonarchGoalTree` is doing. Each status is shown in words, with a tone. */
public enum class MonarchGoalStatus {
    /** Work has not begun. Neutral tone. */
    NotStarted,

    /** Expected to be met. Info tone. */
    OnTrack,

    /** May be missed. Warning tone. */
    AtRisk,

    /** Expected to be missed. Danger tone. */
    OffTrack,

    /** Met. Success tone. */
    Done,
}

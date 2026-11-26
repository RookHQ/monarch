package com.rookhq.monarch.components.workflow.stepper

/** When a `MonarchStepper` shows the compact "Step 2 of 5" summary with a progress bar instead of every step. */
public enum class MonarchStepperCompactMode {
    /** A horizontal stepper becomes compact when its steps do not fit the width on one line each. */
    Automatic,

    /** Always the compact summary, in either orientation. */
    Always,

    /** Never compact. A horizontal stepper whose names do not fit on one line each is drawn as a column. */
    Never,
}

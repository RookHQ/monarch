package com.rookhq.monarch.components.collaboration.callstage

/** Where a `MonarchCallStage` is shown. The same names exist on every platform. */
public enum class MonarchCallStageSurface {
    /** The full call screen, with Pin and full-screen buttons on the tiles. The default. */
    Stage,

    /** A compact panel docked over the app: at most two columns, no tile buttons, Expand and Minimize. */
    Docked,

    /** A picture in picture window: at most two columns, no tile buttons and no picture in picture button. */
    Pip,
}

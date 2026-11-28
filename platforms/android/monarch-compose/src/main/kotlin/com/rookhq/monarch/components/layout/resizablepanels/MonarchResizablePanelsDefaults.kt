package com.rookhq.monarch.components.layout.resizablepanels

/** The default values of `MonarchResizablePanels`. */
public object MonarchResizablePanelsDefaults {
    /** The default range of the share of the first panel: from 10% to 90%. */
    public val Range: ClosedFloatingPointRange<Float> = 0.1f..0.9f

    /** The default step of one arrow key press or one TalkBack adjustment: 5%. */
    public const val Step: Float = 0.05f
}

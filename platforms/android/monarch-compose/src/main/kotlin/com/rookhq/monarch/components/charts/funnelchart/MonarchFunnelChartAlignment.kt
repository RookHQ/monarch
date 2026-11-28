package com.rookhq.monarch.components.charts.funnelchart

/** Where the bars of a `MonarchFunnelChart` sit in their track. */
public enum class MonarchFunnelChartAlignment {
    /** Each bar is centred, so the stack reads as a funnel. */
    Center,

    /** Each bar starts at the start edge, which makes lengths easier to compare. Mirrored right to left. */
    Start,
}

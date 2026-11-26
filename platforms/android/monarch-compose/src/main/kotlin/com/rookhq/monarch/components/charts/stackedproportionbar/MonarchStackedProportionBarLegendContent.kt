package com.rookhq.monarch.components.charts.stackedproportionbar

/** What each legend entry of a `MonarchStackedProportionBar` shows after its label. */
public enum class MonarchStackedProportionBarLegendContent {
    /** The value and the share: "1,204 · 34%". */
    Both,

    /** The value only. */
    Value,

    /** The share only. */
    Percent,

    /** The label only. TalkBack still reads the value and the share. */
    None,
}

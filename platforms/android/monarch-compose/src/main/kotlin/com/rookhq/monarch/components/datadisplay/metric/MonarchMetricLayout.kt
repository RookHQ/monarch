package com.rookhq.monarch.components.datadisplay.metric

/** How a `MonarchMetric` places its label, value and change. The same names exist on every platform. */
public enum class MonarchMetricLayout {
    /** The label above the value, the change under it. The default. */
    Stacked,

    /** Label, value and change on one line that wraps, for toolbars and table footers. */
    Inline,
}

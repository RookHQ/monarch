package dev.monarchui.components.charts.chartlegend

/**
 * A palette slot of a chart series: the theme's five data colours, then a
 * neutral one. Charts and legends that use the same slot show the same colour.
 */
public enum class MonarchChartColor {
    /** The first data colour, `MonarchTheme.colors.data1`. */
    Data1,

    /** The second data colour, `MonarchTheme.colors.data2`. */
    Data2,

    /** The third data colour, `MonarchTheme.colors.data3`. */
    Data3,

    /** The fourth data colour, `MonarchTheme.colors.data4`. */
    Data4,

    /** The fifth data colour, `MonarchTheme.colors.data5`. */
    Data5,

    /** A neutral series such as a target or "Other": the subtle text colour. */
    Neutral,
}

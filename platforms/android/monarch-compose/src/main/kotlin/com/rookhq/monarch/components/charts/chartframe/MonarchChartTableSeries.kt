package com.rookhq.monarch.components.charts.chartframe

import androidx.compose.runtime.Immutable

/**
 * One series of a `MonarchChartTable`: a column of the data table.
 *
 * @property name the name of the series, as in the legend. Already localised.
 * @property values one value per category, in category order. `null` is a gap, shown as a dash and read as
 * "No value".
 * @property formattedValues the values as the chart shows them ("$1,200", "12%"), in category order. `null`
 * formats the numbers for the current locale.
 */
@Immutable
public class MonarchChartTableSeries(
    public val name: String,
    public val values: List<Double?>,
    public val formattedValues: List<String>? = null,
)

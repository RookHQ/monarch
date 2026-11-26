package com.rookhq.monarch.components.charts.chartframe

import androidx.compose.runtime.Immutable

/**
 * The data of a chart as a table: categories by series. A `MonarchChartFrame`
 * with a table offers "View as table", the text alternative of the chart.
 *
 * @property categoryTitle the heading of the category column, such as "Week". Already localised.
 * @property categories the categories, one row each, such as the weeks.
 * @property series the series, one column each.
 */
@Immutable
public class MonarchChartTable(
    public val categoryTitle: String,
    public val categories: List<String>,
    public val series: List<MonarchChartTableSeries>,
)

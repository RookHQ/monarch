package com.rookhq.monarch.components.charts.linechart

/**
 * The spoken and visible text of the plot.
 *
 * @property title the chart's name: the spoken name of the plot.
 * @property summary the spoken summary: the plot's state for TalkBack.
 * @property describe one x value as read: "Mon: This week 820, Last week 700".
 * @property detailsAction the TalkBack action name of one x value: "Details, Mon".
 * @property details the drill-in button: "Details".
 * @property clear the button that ends the inspection: "Clear".
 */
internal class MonarchLineChartPlotText(
    val title: String,
    val summary: String,
    val describe: (Int) -> String,
    val detailsAction: (Int) -> String,
    val details: String,
    val clear: String,
)

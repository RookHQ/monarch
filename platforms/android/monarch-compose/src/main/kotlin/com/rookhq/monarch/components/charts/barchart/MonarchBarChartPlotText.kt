package com.rookhq.monarch.components.charts.barchart

/**
 * The spoken and visible text of the plot.
 *
 * @property title the chart's name: the spoken name of the plot.
 * @property summary the spoken summary: the plot's state for TalkBack.
 * @property describe one category as read: "Jan: Web 1,200, iOS 800".
 * @property detailsAction the TalkBack action name of one category: "Details, Jan".
 * @property details the drill-in button: "Details".
 * @property clear the button that ends the inspection: "Clear".
 * @property highlighted the state of the highlighted category: "Highlighted".
 */
internal class MonarchBarChartPlotText(
    val title: String,
    val summary: String,
    val describe: (Int) -> String,
    val detailsAction: (Int) -> String,
    val details: String,
    val clear: String,
    val highlighted: String,
)

package com.rookhq.monarch.components.geo.worldmap

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchWorldMap`. Every `null`
 * keeps the built-in string. Sentences with values are format strings with
 * `%1$s`-style placeholders, in the order given for each.
 *
 * @property frame the strings of the chart frame. Its `legend` also names the Less to More legend, and its
 * `noValue` is the summary of a map without values.
 * @property country the table heading of the country column: "Country".
 * @property value the table heading of the value column: "Value".
 * @property extras the spoken name of the tiles under the map: "Other values".
 * @property less the start of the legend: "Less" (shared with the heatmap).
 * @property more the end of the legend: "More" (shared with the heatmap).
 * @property rangeBelow the range of the lightest step, from its upper bound: "Below %1$s".
 * @property range the range of a middle step, from its bounds: "%1$s to %2$s".
 * @property rangeAbove the range of the darkest step, from its lower bound: "%1$s or more".
 * @property select the TalkBack action that selects a country when the map has `onSelect`: "Select".
 * @property unplaced the note for codes that are not on the map, from their names and their formatted total.
 * By default "Not on the map: <names> (<total>)".
 * @property summary the generated summary, from the number of countries and the top three as text. By default
 * "5 countries. Highest: United States 1,200, Germany 300, India 150.".
 */
@Immutable
public class MonarchWorldMapLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val country: String? = null,
    public val value: String? = null,
    public val extras: String? = null,
    public val less: String? = null,
    public val more: String? = null,
    public val rangeBelow: String? = null,
    public val range: String? = null,
    public val rangeAbove: String? = null,
    public val select: String? = null,
    public val unplaced: ((names: String, total: String) -> String)? = null,
    public val summary: ((countries: Int, top: String) -> String)? = null,
)

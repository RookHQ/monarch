package dev.monarchui.components.charts.uptimebar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchUptimeBar`. Every `null` uses the built-in string named
 * after it in `strings_uptimebar.xml`.
 *
 * @property statusNames the names of the states, in the details, the legend, the summary and TalkBack. Missing
 * states keep "Operational", "Degraded", "Partial outage", "Down", "Maintenance" and "No data".
 * @property uptime the name of the uptime value in the details and the summary: "Uptime". It also names the bar
 * when there is no label.
 * @property overall writes the overall line under the bar from the formatted percentage: "99.83% uptime".
 * @property legend the spoken name of the legend: "Legend".
 * @property periods the number of periods at the start of the summary: "Periods".
 * @property open the button in the details and the TalkBack action that call `onSegmentSelect`: "Open".
 * @property summary writes the whole spoken summary from the number of periods, the count of each state present
 * and the formatted overall uptime (`null` when nothing was measured).
 */
@Immutable
public class MonarchUptimeBarLabels(
    public val statusNames: Map<MonarchUptimeBarStatus, String> = emptyMap(),
    public val uptime: String? = null,
    public val overall: ((String) -> String)? = null,
    public val legend: String? = null,
    public val periods: String? = null,
    public val open: String? = null,
    public val summary: ((Int, Map<MonarchUptimeBarStatus, Int>, String?) -> String)? = null,
)

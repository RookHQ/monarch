package dev.monarchui.components.charts.funnelchart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchFunnelChart`. Every
 * `null` uses the built-in string of `strings_funnelchart.xml`. Sentences
 * with values are format strings with `%1$s`-style placeholders, in the order
 * given for each.
 *
 * @property frame the strings of the chart frame: View as table, loading, empty and error.
 * @property stage the table heading of the stage column: "Stage".
 * @property value the table heading of the count column: "Count".
 * @property fromPrevious the table heading of the share of the previous stage: "From previous".
 * @property ofFirst the table heading of the share of the first stage: "Of first".
 * @property step the line between two stages, from the share that continued and the number dropped:
 * "%1$s continued · %2$s dropped".
 * @property stages writes the first sentence of the summary from the number of stages and the first and last
 * labels: "4 stages from Visited to Paid".
 * @property overall the sentence of the overall conversion: "Overall conversion %1$s".
 * @property largestDrop the sentence of the largest drop, from the two labels and the share lost:
 * "Largest drop: %1$s to %2$s, %3$s lost".
 * @property describeFirst what TalkBack reads for the first stage, from its label and value: "%1$s: %2$s".
 * @property describe what TalkBack reads for a later stage, from its label, value, share of the previous and
 * share of the first: "%1$s: %2$s, %3$s of previous, %4$s of first".
 */
@Immutable
public class MonarchFunnelChartLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val stage: String? = null,
    public val value: String? = null,
    public val fromPrevious: String? = null,
    public val ofFirst: String? = null,
    public val step: String? = null,
    public val stages: ((count: Int, first: String, last: String) -> String)? = null,
    public val overall: String? = null,
    public val largestDrop: String? = null,
    public val describeFirst: String? = null,
    public val describe: String? = null,
)

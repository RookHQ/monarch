package com.rookhq.monarch.components.intelligence.aicontextwindowmeter

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiContextWindowMeter`. Every `null` uses the built-in
 * string named after it in `strings_aicontextwindowmeter.xml`.
 *
 * @property label the heading and the TalkBack name of the bar: "Context window".
 * @property amount the summary, from the formatted count, limit and share: "45,000 of 128,000 tokens (35%)".
 * @property approximate wraps the summary of an estimated count: "About 45,000 of 128,000 tokens (35%)".
 * @property legend the TalkBack name of the legend: "What fills the context window".
 * @property other the legend name of the tokens no part covers: "Other".
 * @property tokens a part's amount in the legend, from the formatted count: "1,204 tokens".
 * @property warning the notice at or above the warning share.
 * @property full the notice at or over the limit.
 * @property summarize the button that calls `onSummarize`: "Summarize earlier messages".
 * @property showDetails what activating the compact meter does, read by TalkBack: "Show what fills the context
 * window".
 */
@Immutable
public class MonarchAiContextWindowMeterLabels(
    public val label: String? = null,
    public val amount: ((used: String, limit: String, percent: String) -> String)? = null,
    public val approximate: ((amount: String) -> String)? = null,
    public val legend: String? = null,
    public val other: String? = null,
    public val tokens: ((formatted: String) -> String)? = null,
    public val warning: String? = null,
    public val full: String? = null,
    public val summarize: String? = null,
    public val showDetails: String? = null,
)

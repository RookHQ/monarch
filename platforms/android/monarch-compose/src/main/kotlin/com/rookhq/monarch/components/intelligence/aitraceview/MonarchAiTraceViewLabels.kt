package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiTraceView`, and its
 * duration format. Every `null` keeps the built-in string
 * `monarch_aitraceview_<key>`.
 *
 * @property label the heading and spoken name of the trace, "Run trace".
 * @property filterLabel the spoken name of the filter, "Show".
 * @property filter the name of a filter: "All", "Errors only", "Slowest first".
 * @property kind the name of a span kind: "Model", "Tool", "Retrieval", "Agent", "Guardrail", "Other".
 * @property status the name of a status: "OK", "Error", "Cancelled", "Running".
 * @property totalDuration the total time of the run, "Total time".
 * @property totalTokens the tokens of the run, "Tokens".
 * @property totalCost the cost of the run, "Cost".
 * @property errors the number of failed spans, "Errors".
 * @property notAvailable shown for an unknown value, "–".
 * @property empty shown without spans, "No spans recorded".
 * @property noErrors shown when Errors only finds nothing, "No errors in this run".
 * @property tokens the tokens of a span in its facts line, "1,380 tokens", given the count.
 * @property formatDuration writes a duration in milliseconds, such as "1.42 s".
 * @property timing the spoken timing of a span, given its start offset and its duration (`null` while running):
 * "Starts at +1.4 s, takes 600 ms" or "Starts at +0 ms, still running".
 * @property closeDetails the button that closes the details, "Close details".
 * @property openPart the button that shows the span's part in the conversation, "Show in conversation".
 * @property start the start in the details, "Start".
 * @property end the end in the details, "End".
 * @property duration the duration in the details, "Duration".
 * @property inputTokens "Input tokens".
 * @property outputTokens "Output tokens".
 * @property model "Model".
 * @property statusName the status in the details, "Status".
 * @property kindName the kind in the details, "Kind".
 * @property error the error in the details, "Error".
 * @property attributes the heading of the attributes, "Attributes".
 * @property input the heading of the input, "Input".
 * @property output the heading of the output, "Output".
 * @property expand the spoken name of the button that opens a span, "Expand search_documents".
 * @property collapse the spoken name of the button that closes a span, "Collapse search_documents".
 */
@Immutable
public class MonarchAiTraceViewLabels(
    public val label: String? = null,
    public val filterLabel: String? = null,
    public val filter: ((MonarchAiTraceFilter) -> String)? = null,
    public val kind: ((MonarchAiTraceSpanKind) -> String)? = null,
    public val status: ((MonarchAiTraceSpanStatus) -> String)? = null,
    public val totalDuration: String? = null,
    public val totalTokens: String? = null,
    public val totalCost: String? = null,
    public val errors: String? = null,
    public val notAvailable: String? = null,
    public val empty: String? = null,
    public val noErrors: String? = null,
    public val tokens: ((count: Long) -> String)? = null,
    public val formatDuration: ((millis: Long) -> String)? = null,
    public val timing: ((start: String, duration: String?) -> String)? = null,
    public val closeDetails: String? = null,
    public val openPart: String? = null,
    public val start: String? = null,
    public val end: String? = null,
    public val duration: String? = null,
    public val inputTokens: String? = null,
    public val outputTokens: String? = null,
    public val model: String? = null,
    public val statusName: String? = null,
    public val kindName: String? = null,
    public val error: String? = null,
    public val attributes: String? = null,
    public val input: String? = null,
    public val output: String? = null,
    public val expand: ((name: String) -> String)? = null,
    public val collapse: ((name: String) -> String)? = null,
)

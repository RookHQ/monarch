package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.runtime.Immutable

/**
 * One step of an agent run in a `MonarchAiTraceView`. Times are
 * milliseconds from the start of the run.
 *
 * @property id a stable id, unique in the trace.
 * @property parentId the span this one ran inside. `null` or an unknown id makes a top-level span.
 * @property kind what ran.
 * @property name what ran, such as a tool name or a step name.
 * @property status how it ended, or `Running`.
 * @property start when it started, in milliseconds from the start of the run.
 * @property end when it ended, in milliseconds from the start of the run. `null` while it runs.
 * @property inputTokens the tokens this span itself read. Do not roll children up into parents: totals add every span.
 * @property outputTokens the tokens this span itself wrote.
 * @property costMicros this span's own cost in millionths of [currency] (1 000 000 is 1.00).
 * @property currency the ISO 4217 currency of [costMicros]. `null` uses the view's currency.
 * @property model the model that served a model call.
 * @property error why it failed, shown as plain text.
 * @property attributes extra facts, shown as a description list in the details.
 * @property input what went in, as plain text.
 * @property output what came out, as plain text.
 * @property partId the id of the conversation part this span produced or served.
 * @property messageId the id of the conversation message this span belongs to.
 */
@Immutable
public data class MonarchAiTraceSpan(
    public val id: String,
    public val kind: MonarchAiTraceSpanKind,
    public val name: String,
    public val status: MonarchAiTraceSpanStatus,
    public val start: Long,
    public val end: Long? = null,
    public val parentId: String? = null,
    public val inputTokens: Long? = null,
    public val outputTokens: Long? = null,
    public val costMicros: Long? = null,
    public val currency: String? = null,
    public val model: String? = null,
    public val error: String? = null,
    public val attributes: Map<String, String> = emptyMap(),
    public val input: String? = null,
    public val output: String? = null,
    public val partId: String? = null,
    public val messageId: String? = null,
)

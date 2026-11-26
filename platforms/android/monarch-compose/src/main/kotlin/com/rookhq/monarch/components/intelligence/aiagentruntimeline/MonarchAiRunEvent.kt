package com.rookhq.monarch.components.intelligence.aiagentruntimeline

import androidx.compose.runtime.Immutable

/**
 * One event of an agent run: a model call, a tool call, a hand-off or an error.
 *
 * @property id a stable id.
 * @property kind what the event is.
 * @property name the model, tool or agent, such as `search_invoices`.
 * @property startedAt when it started, in epoch milliseconds (UTC).
 * @property endedAt when it ended, in epoch milliseconds; `null` while it runs.
 * @property inputTokens the tokens read, when known.
 * @property outputTokens the tokens written, when known.
 * @property costMicros the cost in millionths of the currency, when known.
 * @property detail a text shown in the code style when the event is open, such as the tool's arguments.
 * @property partId the conversation part the event produced.
 * @property messageId the conversation message the event belongs to.
 */
@Immutable
public data class MonarchAiRunEvent(
    public val id: String,
    public val kind: MonarchAiRunEventKind,
    public val name: String,
    public val startedAt: Long,
    public val endedAt: Long? = null,
    public val inputTokens: Long? = null,
    public val outputTokens: Long? = null,
    public val costMicros: Long? = null,
    public val detail: String? = null,
    public val partId: String? = null,
    public val messageId: String? = null,
)

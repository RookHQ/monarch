package dev.monarchui.components.intelligence.aisubagenttree

import androidx.compose.runtime.Immutable

/**
 * One agent of a `MonarchAiSubagentTree`: the lead, or a sub-agent it handed
 * work to, with the agents it handed work to in turn.
 *
 * @property id a stable id, unique across the whole tree.
 * @property name the display name, such as "Researcher".
 * @property role its role or assignment, such as "Find the three latest filings".
 * @property status where the agent is.
 * @property activity what it is doing now, shown while it is working.
 * @property stepsDone steps finished so far.
 * @property stepsTotal steps planned in total; `null` when unknown.
 * @property toolCalls tool calls made so far.
 * @property startedAt when it started, in epoch milliseconds.
 * @property endedAt when it ended, in epoch milliseconds.
 * @property summary what it produced or why it ended, shown once it is done, failed or stopped.
 * @property children the agents it handed work to, nested to any depth.
 * @property partId the id of the conversation part that started this agent, such as the hand-off's tool call id.
 * @property messageId the id of the conversation message this agent belongs to.
 */
@Immutable
public data class MonarchAiSubagent(
    public val id: String,
    public val name: String,
    public val status: MonarchAiSubagentStatus,
    public val role: String? = null,
    public val activity: String? = null,
    public val stepsDone: Int? = null,
    public val stepsTotal: Int? = null,
    public val toolCalls: Int? = null,
    public val startedAt: Long? = null,
    public val endedAt: Long? = null,
    public val summary: String? = null,
    public val children: List<MonarchAiSubagent> = emptyList(),
    public val partId: String? = null,
    public val messageId: String? = null,
)

package dev.monarchui.components.intelligence.aisubagenttree

/** Which parts and actions of MonarchAiSubagentTree apply to an agent. Every function is pure. */
internal object MonarchAiSubagentTreeRules {
    /** Whether the summary of an agent is shown: once it has ended. */
    fun showsSummary(status: MonarchAiSubagentStatus): Boolean =
        status == MonarchAiSubagentStatus.Done ||
            status == MonarchAiSubagentStatus.Failed ||
            status == MonarchAiSubagentStatus.Stopped

    /** Whether Stop is offered for the selected agent. */
    fun canStop(agent: MonarchAiSubagent): Boolean =
        agent.status == MonarchAiSubagentStatus.Waiting || agent.status == MonarchAiSubagentStatus.Working

    /** Whether Respond is offered for the selected agent. */
    fun canRespond(agent: MonarchAiSubagent): Boolean = agent.status == MonarchAiSubagentStatus.NeedsInput

    /** Whether Show in conversation is offered for the selected agent. */
    fun canOpenPart(agent: MonarchAiSubagent): Boolean = agent.partId != null || agent.messageId != null
}

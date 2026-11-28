package com.rookhq.monarch.components.intelligence.aimultiagentroster

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState

/**
 * Replacements for the built-in strings of a `MonarchAiMultiAgentRoster`.
 * Every `null` keeps the built-in string `monarch_aimultiagentroster_<key>`.
 *
 * @property label names the list for TalkBack, "Agents".
 * @property agentKind read after each agent's name, "AI agent".
 * @property state the name of a state: "Idle", "Working", "Waiting for you", "Error".
 * @property summary the summary line from the number of agents and the count of each state, "3 agents ·
 * 2 working".
 * @property changed announced when an agent's state changes, from its name and the state's name,
 * "Research agent: Error".
 * @property empty shown when there are no agents, "No agents are working on this."
 */
@Immutable
public class MonarchAiMultiAgentRosterLabels(
    public val label: String? = null,
    public val agentKind: String? = null,
    public val state: ((MonarchAiAgentState) -> String)? = null,
    public val summary: ((total: Int, counts: Map<MonarchAiAgentState, Int>) -> String)? = null,
    public val changed: ((name: String, state: String) -> String)? = null,
    public val empty: String? = null,
)

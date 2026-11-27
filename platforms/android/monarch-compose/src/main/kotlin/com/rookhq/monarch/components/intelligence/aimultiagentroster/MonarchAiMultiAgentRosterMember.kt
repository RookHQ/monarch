package com.rookhq.monarch.components.intelligence.aimultiagentroster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef

/**
 * One agent in a `MonarchAiMultiAgentRoster` and what it is doing.
 *
 * @property agent the agent. Keep [MonarchAiAgentRef.id] (or the name) stable between updates: changes are
 * found by it.
 * @property state what the agent is doing.
 * @property task what it is working on, for example "Comparing three sources". Already localized.
 * @property image the agent's picture, loaded by the app. `null` shows initials.
 */
@Immutable
public class MonarchAiMultiAgentRosterMember(
    public val agent: MonarchAiAgentRef,
    public val state: MonarchAiAgentState = MonarchAiAgentState.Idle,
    public val task: String? = null,
    public val image: Painter? = null,
) {
    /** The key changes are found by: the agent's id, else its name. */
    public val id: String get() = agent.id ?: agent.name
}

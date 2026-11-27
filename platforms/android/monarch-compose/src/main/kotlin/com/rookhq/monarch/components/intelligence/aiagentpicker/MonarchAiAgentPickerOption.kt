package com.rookhq.monarch.components.intelligence.aiagentpicker

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentTool
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef

/**
 * One agent a person can choose in a `MonarchAiAgentPicker`.
 *
 * @property agent the agent. Its `id`, or its `name` without an id, is the option's [key].
 * @property description one line on what the agent is for.
 * @property capabilities short capability phrases, shown as badges.
 * @property tools the agent's tools; the option shows how many.
 * @property state what the agent is doing now, shown on its avatar.
 * @property image the agent's picture, loaded by the app; `null` shows the initials.
 * @property enabled whether the agent can be chosen now, such as not enabled in this space.
 */
@Stable
public class MonarchAiAgentPickerOption(
    public val agent: MonarchAiAgentRef,
    public val description: String? = null,
    public val capabilities: List<String> = emptyList(),
    public val tools: List<MonarchAiAgentTool> = emptyList(),
    public val state: MonarchAiAgentState = MonarchAiAgentState.Idle,
    public val image: Painter? = null,
    public val enabled: Boolean = true,
) {
    /** The value of the option: the agent's `id`, or its `name` when it has no id. */
    public val key: String get() = agent.id ?: agent.name
}

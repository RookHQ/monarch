package com.rookhq.monarch.components.intelligence.aiagentavatar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiAgentAvatar`. Every
 * `null` keeps the built-in string `monarch_aiagentavatar_<key>`.
 *
 * @property kind read after the agent's name so it is never taken for a person, "AI agent".
 * @property state the name of a state, read and shown with `showState`: "Idle", "Working", "Waiting for you",
 * "Error".
 */
@Immutable
public class MonarchAiAgentAvatarLabels(
    public val kind: String? = null,
    public val state: ((MonarchAiAgentState) -> String)? = null,
)

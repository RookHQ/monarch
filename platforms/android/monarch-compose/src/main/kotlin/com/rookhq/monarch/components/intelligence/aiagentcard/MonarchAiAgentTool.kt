package com.rookhq.monarch.components.intelligence.aiagentcard

import androidx.compose.runtime.Immutable

/**
 * A tool an AI agent can call, as people see it in a `MonarchAiAgentCard`.
 *
 * @property name the tool's name, such as "Search documents".
 * @property description what the tool does, in plain words.
 */
@Immutable
public data class MonarchAiAgentTool(
    public val name: String,
    public val description: String? = null,
)

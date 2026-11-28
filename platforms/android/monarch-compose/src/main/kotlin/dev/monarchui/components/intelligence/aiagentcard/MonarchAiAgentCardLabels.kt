package dev.monarchui.components.intelligence.aiagentcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiAgentCard` supplies. Each `null`
 * keeps the built-in English string `monarch_aiagentcard_<key>`.
 *
 * @property kind written after the role, so the card never reads as a person's profile. Default "AI agent".
 * @property capabilities the heading of the capabilities. Default "What it can do".
 * @property tools the heading of the tools. Default "Tools".
 * @property dataAccess the heading of the data the agent can see. Default "Data it can see".
 * @property owner the label of the owner. Default "Owner".
 * @property toolCount the compact card's tool count from the number of tools. Default "3 tools".
 */
@Immutable
public class MonarchAiAgentCardLabels(
    public val kind: String? = null,
    public val capabilities: String? = null,
    public val tools: String? = null,
    public val dataAccess: String? = null,
    public val owner: String? = null,
    public val toolCount: ((count: Int) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiAgentCardLabels && other.kind == kind && other.capabilities == capabilities &&
            other.tools == tools && other.dataAccess == dataAccess && other.owner == owner &&
            other.toolCount == toolCount

    override fun hashCode(): Int = listOf(kind, capabilities, tools, dataAccess, owner, toolCount).hashCode()
}

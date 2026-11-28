package dev.monarchui.components.intelligence.aiagentpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiAgentPicker` supplies. Each `null`
 * keeps the built-in English string `monarch_aiagentpicker_<key>`.
 *
 * @property label the name of the group, read by TalkBack. Default "Choose an agent".
 * @property empty shown when there are no agents. Default "No agents are available."
 */
@Immutable
public class MonarchAiAgentPickerLabels(
    public val label: String? = null,
    public val empty: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiAgentPickerLabels && other.label == label && other.empty == empty

    override fun hashCode(): Int = listOf(label, empty).hashCode()
}

package dev.monarchui.components.intelligence.aiassistantpanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiAssistantPanel` supplies. Each
 * `null` keeps the built-in English string `monarch_aiassistantpanel_<key>`.
 *
 * @property close the close button. Default "Close assistant".
 * @property minimize the button that collapses the pane. Default "Minimize assistant".
 * @property expand the button that opens a collapsed pane. Default "Expand assistant".
 * @property inputLabel the TalkBack name of the prompt input. Default "Message the assistant".
 * @property context the TalkBack name of the context chips. Default "Context".
 * @property welcome the greeting while the conversation is empty. Default "How can I help?".
 * @property starters the TalkBack name of the suggested prompts. Default "Suggested prompts".
 * @property assistant the name of the avatar when there is no agent. Default "Assistant".
 */
@Immutable
public class MonarchAiAssistantPanelLabels(
    public val close: String? = null,
    public val minimize: String? = null,
    public val expand: String? = null,
    public val inputLabel: String? = null,
    public val context: String? = null,
    public val welcome: String? = null,
    public val starters: String? = null,
    public val assistant: String? = null,
) {
    private val parts: List<String?>
        get() = listOf(close, minimize, expand, inputLabel, context, welcome, starters, assistant)

    override fun equals(other: Any?): Boolean = other is MonarchAiAssistantPanelLabels && other.parts == parts

    override fun hashCode(): Int = parts.hashCode()
}

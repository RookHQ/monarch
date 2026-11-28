package com.rookhq.monarch.components.intelligence.aiinlineprompt

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiInlinePrompt`.
 * Every `null` keeps the built-in string `monarch_aiinlineprompt_<key>`.
 *
 * @property label the title and pane title of the sheet: "Ask AI".
 * @property placeholder shown while the prompt is empty: "Ask AI to edit or write…".
 * @property inputLabel the accessible name of the prompt field: "Prompt".
 * @property submit the Send button: "Send".
 * @property context the label before the excerpt of the selection: "Selection".
 * @property suggestions the name of the group of quick prompts: "Suggestions".
 * @property pending shown and announced while the prompt is being sent: "Sending…".
 */
@Immutable
public class MonarchAiInlinePromptLabels(
    public val label: String? = null,
    public val placeholder: String? = null,
    public val inputLabel: String? = null,
    public val submit: String? = null,
    public val context: String? = null,
    public val suggestions: String? = null,
    public val pending: String? = null,
)

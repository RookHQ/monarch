package com.rookhq.monarch.components.intelligence.aireasoning

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiReasoning`. Every
 * `null` keeps the built-in string `monarch_aireasoning_<key>`.
 *
 * @property prefix spoken before the header so it is known as the assistant's reasoning, "Assistant reasoning".
 * @property thinking the header while the model reasons, "Thinking…".
 * @property thoughtFor the header once done, given the formatted duration: "Thought for 12s".
 * @property thought the header once done when no duration is known, "Thought".
 * @property reasoningHidden the header of reasoning whose text is withheld, "Reasoning hidden".
 * @property duration formats a duration in milliseconds. `null` uses the narrow unit format of the current
 * locale: "12s", "1m 5s".
 */
@Immutable
public class MonarchAiReasoningLabels(
    public val prefix: String? = null,
    public val thinking: String? = null,
    public val thoughtFor: ((String) -> String)? = null,
    public val thought: String? = null,
    public val reasoningHidden: String? = null,
    public val duration: ((Long) -> String)? = null,
)

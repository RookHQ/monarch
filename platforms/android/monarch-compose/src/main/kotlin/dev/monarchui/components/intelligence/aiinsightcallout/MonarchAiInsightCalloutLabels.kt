package dev.monarchui.components.intelligence.aiinsightcallout

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiInsightCallout`.
 * Every `null` keeps the built-in string `monarch_aiinsightcallout_<key>`.
 *
 * @property tone the tone name TalkBack reads before the title, such as "Warning". An empty string reads the
 * title alone.
 * @property ai the origin label of a model-made insight, "AI insight".
 * @property rule the origin label of a rule-made insight, "Rule-based".
 * @property evidence the title of the evidence disclosure, "Show evidence".
 * @property dismiss the TalkBack name of the dismiss button, "Dismiss insight".
 */
@Immutable
public data class MonarchAiInsightCalloutLabels(
    public val tone: String? = null,
    public val ai: String? = null,
    public val rule: String? = null,
    public val evidence: String? = null,
    public val dismiss: String? = null,
)

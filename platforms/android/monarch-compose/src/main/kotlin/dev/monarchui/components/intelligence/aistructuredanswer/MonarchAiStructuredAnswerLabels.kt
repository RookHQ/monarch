package dev.monarchui.components.intelligence.aistructuredanswer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiStructuredAnswer`.
 * Every `null` keeps the built-in string `monarch_aistructuredanswer_<key>`.
 *
 * @property aiAnswer the visible provenance label above the answer, "AI answer".
 * @property pending read for a field that has not arrived while the answer streams, "Still generating".
 * @property empty read for a field without a value once the answer is complete, "Not provided".
 * @property complete announced once when streaming ends, "Response complete."
 */
@Immutable
public class MonarchAiStructuredAnswerLabels(
    public val aiAnswer: String? = null,
    public val pending: String? = null,
    public val empty: String? = null,
    public val complete: String? = null,
)

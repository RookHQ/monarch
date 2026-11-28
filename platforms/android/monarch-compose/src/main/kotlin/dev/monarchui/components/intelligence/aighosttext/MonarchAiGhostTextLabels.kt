package dev.monarchui.components.intelligence.aighosttext

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of `MonarchAiGhostText`. Every value is
 * `null` by default, which keeps the built-in string.
 *
 * @property announce announced when a suggestion appears, from the suggestion. Say how to accept it.
 * Built in: `monarch_aighosttext_announce`.
 * @property accept the title of the accept button and the name of the TalkBack action. Built in:
 * "Accept suggestion".
 * @property dismiss the name of the TalkBack action that dismisses the suggestion. Built in:
 * "Dismiss suggestion".
 * @property accepted announced when the suggestion is accepted. Built in: "Suggestion accepted."
 * @property dismissed announced when the suggestion is dismissed. Built in: "Suggestion dismissed."
 */
@Immutable
public class MonarchAiGhostTextLabels(
    public val announce: ((String) -> String)? = null,
    public val accept: String? = null,
    public val dismiss: String? = null,
    public val accepted: String? = null,
    public val dismissed: String? = null,
)

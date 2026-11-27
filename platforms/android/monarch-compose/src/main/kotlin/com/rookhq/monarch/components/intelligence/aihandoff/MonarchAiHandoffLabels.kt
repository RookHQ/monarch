package com.rookhq.monarch.components.intelligence.aihandoff

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiHandoff`. Every `null`
 * keeps the built-in string `monarch_aihandoff_<key>`.
 *
 * @property label read first, so the marker is known as a handoff, "Handoff".
 * @property handedOff the sentence, from the two names and whether the work went to a person: "Research
 * agent handed off to Sam Lee". A function, so a translation can put the names in any order. The names are
 * shown in a stronger weight wherever they appear in the result.
 * @property agentKind read after an agent's name, "AI agent".
 * @property reason read before the reason, "Reason:".
 */
@Immutable
public class MonarchAiHandoffLabels(
    public val label: String? = null,
    public val handedOff: ((from: String, to: String, toPerson: Boolean) -> String)? = null,
    public val agentKind: String? = null,
    public val reason: String? = null,
)

package dev.monarchui.components.datadisplay.insightstrip

import androidx.compose.runtime.Immutable
import dev.monarchui.foundation.MonarchTone

/**
 * Replacements for the built-in strings of a `MonarchInsightStrip`. Every
 * `null` keeps the built-in string `monarch_insightstrip_<key>`.
 *
 * @property showMore the button that shows the hidden insights, given how many are hidden.
 * @property showLess the button that hides them again.
 * @property empty shown when there are no insights.
 * @property loading what TalkBack reads for the placeholder cards.
 * @property retry the button that calls `onRetry` after an error.
 * @property dismiss the button that dismisses an insight.
 * @property evidence the header of the evidence section.
 * @property ruleOrigin the badge of an insight a rule raised.
 * @property aiOrigin the badge of an insight a model raised.
 * @property toneLabel the word shown and read for each tone.
 */
@Immutable
public class MonarchInsightStripLabels(
    public val showMore: ((Int) -> String)? = null,
    public val showLess: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val retry: String? = null,
    public val dismiss: String? = null,
    public val evidence: String? = null,
    public val ruleOrigin: String? = null,
    public val aiOrigin: String? = null,
    public val toneLabel: ((MonarchTone) -> String)? = null,
)

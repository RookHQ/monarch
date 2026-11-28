package dev.monarchui.components.intelligence.aiusagemeter

import androidx.compose.runtime.Immutable
import dev.monarchui.components.billing.usagemeter.MonarchUsageMeterLabels

/**
 * Replacements for the built-in strings of a `MonarchAiUsageMeter`. Every
 * `null` uses the built-in string named after it in `strings_aiusagemeter.xml`.
 *
 * @property label the TalkBack name of the meter when it has no title: "AI usage".
 * @property unit writes the unit for a count: "tokens", "credit". The count is the limit, or the amount used
 * when there is no limit.
 * @property nearLimit announced when a scope reaches the warning share, from its label and the formatted share:
 * "This month: 84% of the limit used".
 * @property overLimit announced when a scope goes over its limit, from its label: "This month: over the limit".
 * @property meter the strings of the underlying `MonarchUsageMeter`: the figures, the share, the levels, the
 * reset date, loading and retry.
 */
@Immutable
public class MonarchAiUsageMeterLabels(
    public val label: String? = null,
    public val unit: ((unit: MonarchAiUsageUnit, count: Double) -> String)? = null,
    public val nearLimit: ((label: String, percent: String) -> String)? = null,
    public val overLimit: ((label: String) -> String)? = null,
    public val meter: MonarchUsageMeterLabels = MonarchUsageMeterLabels(),
)

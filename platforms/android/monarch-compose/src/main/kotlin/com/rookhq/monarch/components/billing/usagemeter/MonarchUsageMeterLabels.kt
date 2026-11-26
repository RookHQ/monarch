package com.rookhq.monarch.components.billing.usagemeter

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchUsageMeter`. Every
 * `null` uses the built-in string named after it in `strings_usagemeter.xml`.
 *
 * @property amount writes the figures from the used amount and the limit, already formatted: "7,200 of 10,000".
 * @property percent writes the share of the limit from a formatted percentage: "42% used".
 * @property nearLimit the level at or above the warning share: "Near the limit".
 * @property overLimit the level above the limit: "Over the limit".
 * @property resets writes when the quota resets from a formatted date: "Resets 1 Dec 2026".
 * @property loading the loading text: "Loading usage".
 * @property retry the label of the retry button: "Try again".
 */
@Immutable
public class MonarchUsageMeterLabels(
    public val amount: ((used: String, limit: String) -> String)? = null,
    public val percent: ((percent: String) -> String)? = null,
    public val nearLimit: String? = null,
    public val overLimit: String? = null,
    public val resets: ((date: String) -> String)? = null,
    public val loading: String? = null,
    public val retry: String? = null,
)

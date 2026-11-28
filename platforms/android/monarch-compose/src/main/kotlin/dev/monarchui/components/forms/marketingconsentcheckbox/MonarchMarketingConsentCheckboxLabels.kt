package dev.monarchui.components.forms.marketingconsentcheckbox

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchMarketingConsentCheckbox`.
 * Every `null` or missing entry keeps the built-in string
 * `monarch_marketingconsentcheckbox_<key>`.
 *
 * @property unsubscribeNote replaces "You can unsubscribe at any time."
 * @property channelLabels replaces the labels of the channel checkboxes shown when there are several channels:
 * "Email", "Text message (SMS)", "Phone", "Post".
 */
@Immutable
public class MonarchMarketingConsentCheckboxLabels(
    public val unsubscribeNote: String? = null,
    public val channelLabels: Map<MonarchMarketingChannel, String> = emptyMap(),
)

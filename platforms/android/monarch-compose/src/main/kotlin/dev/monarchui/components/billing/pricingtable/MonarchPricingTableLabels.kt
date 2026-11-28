package dev.monarchui.components.billing.pricingtable

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPricingTable`. Every
 * `null` uses the built-in string named after it in `strings_pricingtable.xml`.
 *
 * @property caption the spoken name of the table: "Plan comparison".
 * @property included the word of an included feature: "Included".
 * @property notIncluded the spoken text of a feature a plan does not have: "Not included".
 * @property currentPlan the badge of the current plan: "Current plan".
 * @property billingPeriod the spoken name of the period picker: "Billing period".
 * @property plan the spoken name of the plan picker on a phone: "Plan".
 */
@Immutable
public class MonarchPricingTableLabels(
    public val caption: String? = null,
    public val included: String? = null,
    public val notIncluded: String? = null,
    public val currentPlan: String? = null,
    public val billingPeriod: String? = null,
    public val plan: String? = null,
)

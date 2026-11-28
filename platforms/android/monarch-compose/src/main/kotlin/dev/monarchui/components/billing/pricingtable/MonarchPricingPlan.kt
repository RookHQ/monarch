package dev.monarchui.components.billing.pricingtable

import androidx.compose.runtime.Immutable

/**
 * One plan of a `MonarchPricingTable`: a page on a phone, a column in the grid.
 *
 * Prices are shown exactly as given; Monarch does no currency formatting or
 * arithmetic.
 *
 * @property id the stable id, used as the key in every feature's values.
 * @property name the plan name. Already localised.
 * @property description one short line on who the plan is for.
 * @property prices the price per billing period, keyed by the period id, such as "$12".
 * @property price the price used when [prices] has none for the period, such as "Contact us". It is shown
 * without the period's suffix and note.
 * @property priceSuffix replaces the period's price suffix for this plan, such as "per seat / month".
 * @property badge a short label for a highlighted plan, such as "Recommended".
 * @property highlighted emphasises the plan as the recommended one.
 * @property current marks the organisation's current plan with a "Current plan" badge.
 */
@Immutable
public class MonarchPricingPlan(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val prices: Map<String, String> = emptyMap(),
    public val price: String? = null,
    public val priceSuffix: String? = null,
    public val badge: String? = null,
    public val highlighted: Boolean = false,
    public val current: Boolean = false,
)

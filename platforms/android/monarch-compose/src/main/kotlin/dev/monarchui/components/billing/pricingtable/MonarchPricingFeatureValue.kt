package dev.monarchui.components.billing.pricingtable

import androidx.compose.runtime.Immutable

/** What a plan offers for one feature of a `MonarchPricingTable`. */
@Immutable
public sealed interface MonarchPricingFeatureValue {
    /** The plan has the feature: the included icon, or the word "Included". */
    public data object Included : MonarchPricingFeatureValue

    /** The plan does not have the feature: a dash, read as "Not included". */
    public data object NotIncluded : MonarchPricingFeatureValue

    /**
     * A value written out, such as "3", "Unlimited" or "Email and chat".
     *
     * @property text the value. Already localised.
     */
    public data class Text(val text: String) : MonarchPricingFeatureValue
}

package dev.monarchui.components.layout.pricingsection

import androidx.compose.runtime.Immutable
import dev.monarchui.components.billing.plancard.MonarchPlanCardFeature

/**
 * One plan of a [MonarchPricingSection].
 *
 * @property id identifies the plan: the key of `comparison` values and of the action.
 * @property name the name of the plan.
 * @property description a muted line under the price.
 * @property prices the price for each period id, shown exactly as given.
 * @property price the price when the plan has none for the chosen period, such as "Contact us".
 * @property badge a short badge beside the name, such as "Recommended".
 * @property highlighted draws the card with the accent border.
 * @property featuresLabel a line above the features, such as "Everything in Starter, plus".
 * @property features what the plan includes; a feature with `included = false` is shown muted.
 */
@Immutable
public class MonarchPricingSectionPlan(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val prices: Map<String, String> = emptyMap(),
    public val price: String? = null,
    public val badge: String? = null,
    public val highlighted: Boolean = false,
    public val featuresLabel: String? = null,
    public val features: List<MonarchPlanCardFeature> = emptyList(),
)

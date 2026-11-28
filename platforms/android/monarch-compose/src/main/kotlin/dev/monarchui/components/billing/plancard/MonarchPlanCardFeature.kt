package dev.monarchui.components.billing.plancard

import androidx.compose.runtime.Immutable

/**
 * One line in the feature list of a `MonarchPlanCard`.
 *
 * @property label what the plan offers, such as "Unlimited projects". Already localised.
 * @property hint a short detail after the label, such as "up to 10 seats".
 * @property included `false` lists a feature the plan does not have: muted, with a dash, and read with
 * "Not included:" before it.
 */
@Immutable
public class MonarchPlanCardFeature(
    public val label: String,
    public val hint: String? = null,
    public val included: Boolean = true,
)

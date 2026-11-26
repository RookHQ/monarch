package com.rookhq.monarch.components.billing.pricingtable

import androidx.compose.runtime.Immutable

/**
 * One feature compared across the plans of a `MonarchPricingTable`: a row of the grid, a line on each page.
 *
 * @property id the stable id of the feature.
 * @property label the feature, such as "Single sign-on". Already localised.
 * @property description a short explanation under the label.
 * @property group features with the same group are shown under one group heading, in first-seen order.
 * @property values the value per plan id. A plan missing from the map does not have the feature.
 */
@Immutable
public class MonarchPricingFeature(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val group: String? = null,
    public val values: Map<String, MonarchPricingFeatureValue> = emptyMap(),
)

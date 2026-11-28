package com.rookhq.monarch.components.layout.comparisonsection

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeatureValue

/**
 * One compared capability of a [MonarchComparisonSection].
 *
 * @property id identifies the row.
 * @property label the capability, such as "Change history".
 * @property values the value for each column id: `Included`, `NotIncluded` or `Text`. A column missing from the
 * map does not have the capability.
 * @property group the group heading the row is listed under, such as "Access".
 * @property description a muted line under the label.
 */
@Immutable
public class MonarchComparisonRow(
    public val id: String,
    public val label: String,
    public val values: Map<String, MonarchPricingFeatureValue>,
    public val group: String? = null,
    public val description: String? = null,
)

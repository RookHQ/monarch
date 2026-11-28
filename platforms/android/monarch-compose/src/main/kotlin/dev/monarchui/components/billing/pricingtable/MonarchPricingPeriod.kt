package dev.monarchui.components.billing.pricingtable

import androidx.compose.runtime.Immutable

/**
 * A billing period of a `MonarchPricingTable`, such as Monthly or Yearly.
 *
 * @property id the key of the period in every plan's prices.
 * @property label the text of the period picker, such as "Yearly". Already localised.
 * @property suffix a short line under the picker while the period is chosen, such as "2 months included".
 * Keep it factual.
 * @property priceSuffix the text after every price in this period, such as "/ month".
 * @property priceNote a line under every price in this period, such as "Billed yearly".
 */
@Immutable
public class MonarchPricingPeriod(
    public val id: String,
    public val label: String,
    public val suffix: String? = null,
    public val priceSuffix: String? = null,
    public val priceNote: String? = null,
)

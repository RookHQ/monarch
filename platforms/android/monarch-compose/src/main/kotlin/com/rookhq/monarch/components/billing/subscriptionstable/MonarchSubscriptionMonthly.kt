package com.rookhq.monarch.components.billing.subscriptionstable

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/** A subscription's price normalised to one month in the reporting currency (MRR). */
@Immutable
public sealed interface MonarchSubscriptionMonthly {
    /**
     * The monthly figure.
     *
     * @property amount the amount in major units.
     * @property currencyCode the ISO 4217 code of the reporting currency.
     */
    @Immutable
    public class Amount(public val amount: BigDecimal, public val currencyCode: String) : MonarchSubscriptionMonthly

    /**
     * No exchange rate was available. A live subscription (active or past due)
     * shows "Not converted"; any other shows the empty dash.
     */
    public data object NotConverted : MonarchSubscriptionMonthly
}

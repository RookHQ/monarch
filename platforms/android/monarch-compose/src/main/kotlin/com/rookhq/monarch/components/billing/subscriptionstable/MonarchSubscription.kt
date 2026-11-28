package com.rookhq.monarch.components.billing.subscriptionstable

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * One recurring agreement, shown as a row of `MonarchSubscriptionsTable`.
 * Dates are epoch milliseconds (UTC).
 *
 * @property id a stable key of the subscription.
 * @property name the subscription's name, the row's main text.
 * @property status the status key, looked up in the table's registry, such as "active" or "past_due".
 * @property amount the price per billing period in major units, or `null` when it has none (an empty dash).
 * @property currencyCode the ISO 4217 code of [amount].
 * @property plan the plan or product, under the name. `null` shows "Custom price".
 * @property quantity seats or units, written "3 ×" before the plan when above 1.
 * @property customer the customer it belongs to.
 * @property interval the unit of the billing period, or `null` for a one-time charge.
 * @property intervalCount the number of intervals per billing period, such as 3 months.
 * @property monthly the price normalised to one month, or `null` for the empty dash.
 * @property renewalDateMillis the next renewal or end date, or `null` when it has no end ("Evergreen").
 * @property renewsAutomatically `true` renews on [renewalDateMillis]; `false` ends on it.
 * @property cancelsAtMillis a scheduled cancellation, shown under the name.
 * @property changesAtMillis a scheduled plan or price change, shown under the name.
 */
@Immutable
public class MonarchSubscription(
    public val id: String,
    public val name: String,
    public val status: String,
    public val amount: BigDecimal?,
    public val currencyCode: String,
    public val plan: String? = null,
    public val quantity: Int = 1,
    public val customer: String? = null,
    public val interval: MonarchSubscriptionInterval? = MonarchSubscriptionInterval.Month,
    public val intervalCount: Int = 1,
    public val monthly: MonarchSubscriptionMonthly? = null,
    public val renewalDateMillis: Long? = null,
    public val renewsAutomatically: Boolean = true,
    public val cancelsAtMillis: Long? = null,
    public val changesAtMillis: Long? = null,
)

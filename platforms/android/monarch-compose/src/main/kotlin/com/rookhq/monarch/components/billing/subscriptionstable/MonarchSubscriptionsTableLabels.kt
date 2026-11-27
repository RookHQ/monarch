package com.rookhq.monarch.components.billing.subscriptionstable

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSubscriptionsTable`.
 * Every `null` uses the built-in string named after it in
 * `strings_subscriptionstable.xml`.
 *
 * @property caption the name TalkBack gives the table: "Subscriptions".
 * @property subscription the column heading of names: "Subscription".
 * @property customer the column heading of customers: "Customer".
 * @property status the column heading of statuses: "Status".
 * @property price the column heading of prices: "Price".
 * @property monthly the column heading and spoken label of the monthly figure: "Monthly".
 * @property renewal the column heading of renewals: "Renewal".
 * @property customPrice shown in place of a missing plan: "Custom price".
 * @property quantity the quantity before the plan: "3 ×".
 * @property interval the billing period: "month", "quarter", "3 weeks", "one time".
 * @property priceSuffix written after the price: "/ month".
 * @property spokenPrice what TalkBack reads for a recurring price, given the price and the period: "$49.00 per month".
 * @property spokenOneTimePrice what TalkBack reads for a one-time price: "$49.00, one time".
 * @property cancels a scheduled cancellation, given the date: "cancels Mar 1, 2027".
 * @property changes a scheduled change, given the date: "changes Mar 1, 2027".
 * @property renews a renewal, given the date: "Renews Jul 1, 2026".
 * @property ends an end without renewal, given the date: "Ends Jul 1, 2026".
 * @property evergreen no renewal date: "Evergreen".
 * @property notConverted a live subscription without a monthly figure: "Not converted".
 * @property notConvertedHint read after "Not converted" to explain it.
 * @property noValue what TalkBack reads for an empty dash: "No value".
 * @property emptyTitle the title of the empty state: "No subscriptions".
 * @property emptyMessage the message of the empty state.
 */
@Immutable
public class MonarchSubscriptionsTableLabels(
    public val caption: String? = null,
    public val subscription: String? = null,
    public val customer: String? = null,
    public val status: String? = null,
    public val price: String? = null,
    public val monthly: String? = null,
    public val renewal: String? = null,
    public val customPrice: String? = null,
    public val quantity: ((Int) -> String)? = null,
    public val interval: ((MonarchSubscriptionInterval?, Int) -> String)? = null,
    public val priceSuffix: ((String) -> String)? = null,
    public val spokenPrice: ((String, String) -> String)? = null,
    public val spokenOneTimePrice: ((String) -> String)? = null,
    public val cancels: ((String) -> String)? = null,
    public val changes: ((String) -> String)? = null,
    public val renews: ((String) -> String)? = null,
    public val ends: ((String) -> String)? = null,
    public val evergreen: String? = null,
    public val notConverted: String? = null,
    public val notConvertedHint: String? = null,
    public val noValue: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
)

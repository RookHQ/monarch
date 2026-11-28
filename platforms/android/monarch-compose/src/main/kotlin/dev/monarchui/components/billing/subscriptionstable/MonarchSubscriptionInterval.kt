package dev.monarchui.components.billing.subscriptionstable

/** The unit of a subscription's billing period. A `null` interval is a one-time charge. */
public enum class MonarchSubscriptionInterval {
    /** Billed every day, or every few days. */
    Day,

    /** Billed every week, or every few weeks. */
    Week,

    /** Billed every month, or every few months (3 months is written "quarter"). */
    Month,

    /** Billed every year, or every few years. */
    Year,
}

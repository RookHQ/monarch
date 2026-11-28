package dev.monarchui.components.billing.subscriptionstable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.internal.monarchString
import java.util.Locale

/** The words of one row of a `MonarchSubscriptionsTable`, written and spoken. */
internal class SubscriptionsTableRowText(
    val meta: String,
    val customer: String?,
    val priceSuffix: String,
    val monthly: String?,
    val monthlyMuted: Boolean,
    val renewal: String,
    val spoken: String,
)

/** The months in a quarter, written "quarter" rather than "3 months". */
private const val MonthsInQuarter = 3

/** The billing period: "month", "quarter", "3 weeks", "one time". */
@Composable
internal fun subscriptionsTableInterval(
    interval: MonarchSubscriptionInterval?,
    count: Int,
    labels: MonarchSubscriptionsTableLabels,
): String {
    labels.interval?.let { return it(interval, count) }
    return when {
        interval == null -> stringResource(R.string.monarch_subscriptionstable_one_time)
        interval == MonarchSubscriptionInterval.Month && count == MonthsInQuarter ->
            stringResource(R.string.monarch_subscriptionstable_quarter)
        else -> pluralStringResource(
            when (interval) {
                MonarchSubscriptionInterval.Day -> R.plurals.monarch_subscriptionstable_days
                MonarchSubscriptionInterval.Week -> R.plurals.monarch_subscriptionstable_weeks
                MonarchSubscriptionInterval.Month -> R.plurals.monarch_subscriptionstable_months
                MonarchSubscriptionInterval.Year -> R.plurals.monarch_subscriptionstable_years
            },
            count,
            count,
        )
    }
}

/** The line under the name: "3 × Team plan · cancels Mar 1, 2027". */
@Composable
private fun subscriptionsTableMeta(
    subscription: MonarchSubscription,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
): String {
    val tokens = MonarchSubscriptionsTableTokens
    val quantity = subscription.quantity.takeIf { it > 1 }?.let {
        labels.quantity?.invoke(it) ?: stringResource(R.string.monarch_subscriptionstable_quantity, it)
    }
    val plan = subscription.plan ?: monarchString(labels.customPrice, R.string.monarch_subscriptionstable_custom_price)
    val cancels = subscription.cancelsAtMillis?.let { tokens.dateText(it, locale) }?.let {
        labels.cancels?.invoke(it) ?: stringResource(R.string.monarch_subscriptionstable_cancels, it)
    }
    val changes = subscription.changesAtMillis?.let { tokens.dateText(it, locale) }?.let {
        labels.changes?.invoke(it) ?: stringResource(R.string.monarch_subscriptionstable_changes, it)
    }
    return tokens.line(listOf(listOfNotNull(quantity, plan).joinToString(" "), cancels, changes))
}

/** "Renews Jul 1, 2026", "Ends Jul 1, 2026" or "Evergreen". */
@Composable
private fun subscriptionsTableRenewal(
    subscription: MonarchSubscription,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
): String {
    val date = subscription.renewalDateMillis?.let { MonarchSubscriptionsTableTokens.dateText(it, locale) }
        ?: return monarchString(labels.evergreen, R.string.monarch_subscriptionstable_evergreen)
    return if (subscription.renewsAutomatically) {
        labels.renews?.invoke(date) ?: stringResource(R.string.monarch_subscriptionstable_renews, date)
    } else {
        labels.ends?.invoke(date) ?: stringResource(R.string.monarch_subscriptionstable_ends, date)
    }
}

/** What TalkBack reads for the price: "$49.00 per month", "$49.00, one time" or "No value". */
@Composable
private fun subscriptionsTableSpokenPrice(
    subscription: MonarchSubscription,
    interval: String,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
): String {
    val price = subscription.amount?.let { MonarchMoneyFormat.format(it, subscription.currencyCode, locale) }
        ?: return monarchString(labels.noValue, R.string.monarch_subscriptionstable_no_value)
    return if (subscription.interval == null) {
        labels.spokenOneTimePrice?.invoke(price)
            ?: stringResource(R.string.monarch_subscriptionstable_spoken_one_time_price, price)
    } else {
        labels.spokenPrice?.invoke(price, interval)
            ?: stringResource(R.string.monarch_subscriptionstable_spoken_price, price, interval)
    }
}

/** The monthly figure as written: the amount, "Not converted" for a live subscription, or `null`. */
@Composable
private fun subscriptionsTableMonthly(
    subscription: MonarchSubscription,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
): String? = when (val figure = subscription.monthly) {
    is MonarchSubscriptionMonthly.Amount -> MonarchMoneyFormat.format(figure.amount, figure.currencyCode, locale)
    else -> monarchString(labels.notConverted, R.string.monarch_subscriptionstable_not_converted)
        .takeIf { MonarchSubscriptionsTableTokens.notConverted(subscription) }
}

/** Every string of [subscription]'s row, from the labels or the built-in strings. */
@Composable
internal fun subscriptionsTableRowText(
    subscription: MonarchSubscription,
    status: MonarchStatusDefinition,
    showCustomer: Boolean,
    showMonthly: Boolean,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
): SubscriptionsTableRowText {
    val notConverted = MonarchSubscriptionsTableTokens.notConverted(subscription)
    val meta = subscriptionsTableMeta(subscription, locale, labels)
    val renewal = subscriptionsTableRenewal(subscription, locale, labels)
    val interval = subscriptionsTableInterval(subscription.interval, subscription.intervalCount, labels)
    val suffix = labels.priceSuffix?.invoke(interval)
        ?: stringResource(R.string.monarch_subscriptionstable_price_suffix, interval)
    val spokenPrice = subscriptionsTableSpokenPrice(subscription, interval, locale, labels)
    val monthly = subscriptionsTableMonthly(subscription, locale, labels)
    val monthlyLabel = monarchString(labels.monthly, R.string.monarch_subscriptionstable_monthly)
    val hint = monarchString(labels.notConvertedHint, R.string.monarch_subscriptionstable_not_converted_hint)
    val noValue = monarchString(labels.noValue, R.string.monarch_subscriptionstable_no_value)
    val spokenMonthly = when {
        !showMonthly -> null
        notConverted -> "$monthlyLabel $monthly. $hint"
        else -> "$monthlyLabel ${monthly ?: noValue}"
    }
    val customer = subscription.customer?.takeIf { showCustomer && it.isNotBlank() }
    return SubscriptionsTableRowText(
        meta = meta,
        customer = customer,
        priceSuffix = suffix,
        monthly = monthly,
        monthlyMuted = notConverted,
        renewal = renewal,
        spoken = listOfNotNull(subscription.name, meta, customer, status.label, spokenPrice, spokenMonthly, renewal)
            .joinToString(", "),
    )
}

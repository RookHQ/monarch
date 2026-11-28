package dev.monarchui.components.billing.subscriptionstable

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.math.BigDecimal
import java.util.Locale

private const val PreviewRenewal = 1_782_864_000_000L

@MonarchPreviews
@Composable
internal fun MonarchSubscriptionsTablePreview() {
    PreviewFrame {
        MonarchSubscriptionsTable(
            subscriptions = listOf(
                MonarchSubscription(
                    id = "1",
                    name = "Acme workspace",
                    status = "active",
                    amount = BigDecimal("49.00"),
                    currencyCode = "USD",
                    plan = "Team plan",
                    quantity = 3,
                    customer = "Acme Ltd",
                    monthly = MonarchSubscriptionMonthly.Amount(BigDecimal("49.00"), "USD"),
                    renewalDateMillis = PreviewRenewal,
                ),
                MonarchSubscription(
                    id = "2",
                    name = "Globex support",
                    status = "past_due",
                    amount = BigDecimal("1200.00"),
                    currencyCode = "EUR",
                    interval = MonarchSubscriptionInterval.Year,
                    monthly = MonarchSubscriptionMonthly.NotConverted,
                ),
            ),
            locale = Locale.US,
            onSelect = {},
        )
        MonarchSubscriptionsTable(subscriptions = emptyList())
    }
}

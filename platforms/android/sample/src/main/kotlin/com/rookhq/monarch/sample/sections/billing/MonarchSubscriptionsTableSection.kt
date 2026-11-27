package com.rookhq.monarch.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.billing.subscriptionstable.MonarchSubscription
import com.rookhq.monarch.components.billing.subscriptionstable.MonarchSubscriptionInterval
import com.rookhq.monarch.components.billing.subscriptionstable.MonarchSubscriptionMonthly
import com.rookhq.monarch.components.billing.subscriptionstable.MonarchSubscriptionsTable
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of SubscriptionsTable. */
object MonarchSubscriptionsTableSection : CataloguePage {
    override val title = "SubscriptionsTable"

    private const val JULY_1 = 1_782_864_000_000L
    private const val MARCH_1 = 1_803_859_200_000L
    private const val SEATS = 3

    private val subscriptions = listOf(
        MonarchSubscription(
            id = "acme",
            name = "Acme workspace",
            status = "active",
            amount = BigDecimal("49.00"),
            currencyCode = "USD",
            plan = "Team plan",
            quantity = SEATS,
            customer = "Acme Ltd",
            monthly = MonarchSubscriptionMonthly.Amount(BigDecimal("49.00"), "USD"),
            renewalDateMillis = JULY_1,
        ),
        MonarchSubscription(
            id = "globex",
            name = "Globex support",
            status = "past_due",
            amount = BigDecimal("1200.00"),
            currencyCode = "EUR",
            plan = "Premium support",
            customer = "Globex GmbH",
            interval = MonarchSubscriptionInterval.Year,
            monthly = MonarchSubscriptionMonthly.NotConverted,
            renewalDateMillis = JULY_1,
            renewsAutomatically = false,
        ),
        MonarchSubscription(
            id = "initech",
            name = "Initech analytics",
            status = "trial",
            amount = BigDecimal("300.00"),
            currencyCode = "USD",
            plan = "Analytics",
            customer = "Initech",
            intervalCount = SEATS,
            monthly = MonarchSubscriptionMonthly.Amount(BigDecimal("100.00"), "USD"),
            cancelsAtMillis = MARCH_1,
        ),
    )

    @Composable
    override fun Content() {
        var showCustomer by rememberSaveable { mutableStateOf(true) }
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        MonarchSwitchRow(checked = showCustomer, onCheckedChange = { showCustomer = it }, label = "Show customer")
        CatalogueCaption("Rows open a subscription")
        MonarchSubscriptionsTable(
            subscriptions = subscriptions,
            showCustomer = showCustomer,
            onSelect = { opened = it.name },
        )
        opened?.let { MonarchText(text = "Opened $it") }
        CatalogueCaption("No subscriptions")
        MonarchSubscriptionsTable(subscriptions = emptyList())
    }
}

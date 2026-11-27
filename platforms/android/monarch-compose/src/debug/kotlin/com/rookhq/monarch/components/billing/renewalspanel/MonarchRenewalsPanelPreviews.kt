@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.math.BigDecimal
import java.util.Locale

private const val PreviewNow = 1_782_864_000_000L
private const val PreviewDay = 86_400_000L

@MonarchPreviews
@Composable
internal fun MonarchRenewalsPanelPreview() {
    PreviewFrame {
        MonarchRenewalsPanel(
            items = listOf(
                MonarchRenewalItem(
                    id = "acme",
                    name = "Acme support contract",
                    status = "open",
                    renewalDateMillis = PreviewNow + 12 * PreviewDay,
                    description = "Contract · Acme Ltd",
                    amount = BigDecimal("12000"),
                    currencyCode = "USD",
                    amountNote = "per year",
                    autoRenew = false,
                    owner = "Ana Silva",
                    nextAction = "Send a quote",
                    nextActionOverdue = true,
                ),
                MonarchRenewalItem(id = "dom", name = "example.com", status = "renewed", renewalDateMillis = null),
            ),
            filters = listOf(MonarchRenewalFilter("30", "30 days", 2), MonarchRenewalFilter("all", "All")),
            totals = listOf(MonarchRenewalTotal("USD", BigDecimal("12000"), 1)),
            unpricedCount = 1,
            locale = Locale.US,
            now = PreviewNow,
            onItemAction = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchRenewalsPanelStatesPreview() {
    PreviewFrame {
        MonarchRenewalsPanel(items = emptyList())
        MonarchRenewalsPanel(items = emptyList(), error = "", onRetry = {})
    }
}

@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.billing.renewalspanel.MonarchRenewalFilter
import dev.monarchui.components.billing.renewalspanel.MonarchRenewalItem
import dev.monarchui.components.billing.renewalspanel.MonarchRenewalTotal
import dev.monarchui.components.billing.renewalspanel.MonarchRenewalsPanel
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of RenewalsPanel. */
object MonarchRenewalsPanelSection : CataloguePage {
    override val title = "RenewalsPanel"

    private const val DAY = 86_400_000L
    private const val SOON = 9L
    private const val LATER = 45L
    private const val LIKELY = 60

    private fun items(now: Long) = listOf(
        MonarchRenewalItem(
            id = "acme",
            name = "Acme support contract",
            status = "open",
            renewalDateMillis = now + SOON * DAY,
            description = "Contract · Acme Ltd",
            amount = BigDecimal("12000"),
            currencyCode = "USD",
            amountNote = "per year",
            autoRenew = false,
            owner = "Ana Silva",
            nextAction = "Send a quote",
            nextActionOverdue = true,
        ),
        MonarchRenewalItem(
            id = "lic",
            name = "Design tool licenses",
            status = "in_progress",
            renewalDateMillis = now + LATER * DAY,
            noticeDeadlineMillis = now + SOON * DAY,
            amount = BigDecimal("2400"),
            currencyCode = "EUR",
            autoRenew = true,
            probability = LIKELY,
            owner = "Bruno Costa",
        ),
        MonarchRenewalItem(
            id = "dom",
            name = "example.com",
            status = "renewed",
            renewalDateMillis = now - SOON * DAY,
            outcome = "Renewed for two years",
        ),
    )

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var filter by rememberSaveable { mutableStateOf("open") }
        var updated by rememberSaveable { mutableStateOf(setOf<String>()) }
        val all = items(now).map {
            if (it.id in updated) {
                MonarchRenewalItem(it.id, it.name, "renewed", it.renewalDateMillis, outcome = "Marked as renewed")
            } else {
                it
            }
        }
        val shown = if (filter == "open") all.filter { it.status == "open" || it.status == "in_progress" } else all
        CatalogueCaption("Filters, totals and a row action")
        MonarchRenewalsPanel(
            items = shown,
            description = "Contracts, licenses and domains that renew in the next 90 days.",
            filters = listOf(
                MonarchRenewalFilter("open", "Open", all.count { it.status != "renewed" }),
                MonarchRenewalFilter("all", "All", all.size),
            ),
            selectedFilter = filter,
            totals = listOf(
                MonarchRenewalTotal("USD", BigDecimal("12000"), 1),
                MonarchRenewalTotal("EUR", BigDecimal("2400"), 1, estimated = BigDecimal("2400")),
            ),
            unpricedCount = 1,
            now = now,
            onFilterChange = { filter = it },
            onItemAction = { updated = updated + it.id },
            canAct = { it.status == "open" || it.status == "in_progress" },
        )
        CatalogueCaption("Loading, failed and empty")
        MonarchRenewalsPanel(items = emptyList(), loading = true)
        MonarchRenewalsPanel(items = emptyList(), error = "Check your connection.", onRetry = {})
        MonarchRenewalsPanel(items = emptyList())
    }
}

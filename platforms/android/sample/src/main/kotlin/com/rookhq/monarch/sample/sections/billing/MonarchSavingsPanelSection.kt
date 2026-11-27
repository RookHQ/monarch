@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsAmount
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsCategory
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsDecisionCount
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsItem
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsPanel
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of SavingsPanel. */
object MonarchSavingsPanelSection : CataloguePage {
    override val title = "SavingsPanel"

    private const val JULY_1 = 1_782_864_000_000L
    private const val UNUSED = 7
    private const val PARKED = 4
    private const val NO_WEBSITE = 3

    private val registry = MonarchStatusRegistry(
        mapOf(
            "parked" to MonarchStatusDefinition("Parked", MonarchTone.Warning),
            "no_website" to MonarchStatusDefinition("No website", MonarchTone.Neutral),
            "keep" to MonarchStatusDefinition("Keep", MonarchTone.Success),
            "let_lapse" to MonarchStatusDefinition("Let lapse", MonarchTone.Danger),
        ),
    )

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Unused domains with decisions")
        MonarchSavingsPanel(
            unusedCount = UNUSED,
            costs = listOf(
                MonarchSavingsAmount(BigDecimal("184.00"), "USD"),
                MonarchSavingsAmount(BigDecimal("42.00"), "EUR"),
            ),
            unpricedCount = 1,
            categories = listOf(
                MonarchSavingsCategory("parked", PARKED),
                MonarchSavingsCategory("no_website", NO_WEBSITE),
            ),
            decisions = listOf(
                MonarchSavingsDecisionCount("keep", "Keep", 2),
                MonarchSavingsDecisionCount("let_lapse", "Let lapse", NO_WEBSITE),
                MonarchSavingsDecisionCount("undecided", "Undecided", 2),
            ),
            potentialSavings = listOf(MonarchSavingsAmount(BigDecimal("96.00"), "USD")),
            items = listOf(
                MonarchSavingsItem(
                    id = "net",
                    name = "example.net",
                    category = "parked",
                    dateMillis = JULY_1,
                    amount = MonarchSavingsAmount(BigDecimal("32.00"), "USD"),
                    decision = "let_lapse",
                ),
                MonarchSavingsItem(id = "org", name = "example.org", category = "no_website", unpriced = true),
                MonarchSavingsItem(id = "io", name = "example.io", category = "parked", decision = "keep"),
            ),
            maxItems = 2,
            registry = registry,
            onCategoryClick = { opened = it.id },
            onItemClick = { opened = it.name },
        )
        opened?.let { MonarchText(text = "Opened $it") }
        CatalogueCaption("Nothing unused, loading and failed")
        MonarchSavingsPanel(unusedCount = 0)
        MonarchSavingsPanel(unusedCount = UNUSED, loading = true)
        MonarchSavingsPanel(unusedCount = UNUSED, error = "", onRetry = {})
    }
}

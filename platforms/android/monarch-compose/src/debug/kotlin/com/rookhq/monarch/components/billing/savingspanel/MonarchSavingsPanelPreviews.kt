@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.billing.savingspanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.math.BigDecimal
import java.util.Locale

private const val PreviewDate = 1_782_864_000_000L

@MonarchPreviews
@Composable
internal fun MonarchSavingsPanelPreview() {
    PreviewFrame {
        MonarchSavingsPanel(
            unusedCount = 3,
            costs = listOf(MonarchSavingsAmount(BigDecimal("120"), "USD")),
            unpricedCount = 1,
            categories = listOf(MonarchSavingsCategory("parked", 2), MonarchSavingsCategory("no_website", 1)),
            decisions = listOf(MonarchSavingsDecisionCount("keep", "Keep", 1)),
            potentialSavings = listOf(MonarchSavingsAmount(BigDecimal("80"), "USD")),
            items = listOf(
                MonarchSavingsItem(
                    id = "a",
                    name = "example.net",
                    category = "parked",
                    dateMillis = PreviewDate,
                    amount = MonarchSavingsAmount(BigDecimal("40"), "USD"),
                    decision = "let_lapse",
                ),
            ),
            registry = MonarchStatusRegistry(
                mapOf("let_lapse" to MonarchStatusDefinition("Let lapse", MonarchTone.Warning)),
            ),
            locale = Locale.US,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchSavingsPanelStatesPreview() {
    PreviewFrame {
        MonarchSavingsPanel(unusedCount = 0)
        MonarchSavingsPanel(unusedCount = 0, error = "", onRetry = {})
    }
}

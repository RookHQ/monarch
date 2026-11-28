@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aispendcap

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiSpendCapPreview() {
    PreviewFrame {
        MonarchAiSpendCap(
            spent = 1_240,
            limit = 5_000,
            currencyCode = "GBP",
            period = MonarchAiSpendCapPeriod.Month,
            breakdown = listOf(MonarchAiSpendCapBreakdownItem("research", "Research agent", 820)),
        )
        MonarchAiSpendCap(
            spent = 5_000,
            limit = 5_000,
            currencyCode = "USD",
            period = MonarchAiSpendCapPeriod.Day,
            canEdit = false,
            onRequestIncrease = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiSpendCapEditingPreview() {
    PreviewFrame {
        MonarchAiSpendCap(
            spent = 4_200,
            limit = 5_000,
            currencyCode = "EUR",
            period = MonarchAiSpendCapPeriod.Week,
            editing = true,
        )
    }
}

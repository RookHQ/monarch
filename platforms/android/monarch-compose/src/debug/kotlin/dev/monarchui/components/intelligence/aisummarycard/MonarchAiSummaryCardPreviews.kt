@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aisummarycard

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val Now = 1_793_750_400_000L
private const val Hour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchAiSummaryCardPreview() {
    PreviewFrame {
        MonarchAiSummaryCard(
            title = "Account summary",
            summary = "**Acme** renewed in March. Two open tickets mention *slow exports*.",
            generatedAt = Now - 3 * Hour,
            now = Now,
            sources = listOf(MonarchAiSourcePart("t1", "Ticket 4821", null, MonarchAiSourceKind.Record)),
            onRefresh = {},
        )
        MonarchAiSummaryCard(title = "Deal summary", summary = "", status = MonarchAiSummaryCardStatus.Generating)
        MonarchAiSummaryCard(
            title = "Deal summary",
            summary = "",
            status = MonarchAiSummaryCardStatus.Error,
            error = "The model did not answer.",
            onRefresh = {},
        )
    }
}

package dev.monarchui.components.datadisplay.insightstrip

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewInsights = listOf(
    MonarchInsight(
        id = "renewals",
        title = "3 domains expire in 30 days",
        why = "Auto-renew is off for all three.",
        origin = MonarchInsightOrigin.Rule,
        tone = MonarchTone.Warning,
        evidence = "example.com, example.org, example.net",
        actionLabel = "Review renewals",
        onAction = {},
    ),
    MonarchInsight(
        id = "churn",
        title = "Renewal at risk",
        why = "Usage fell by half in the last month.",
        origin = MonarchInsightOrigin.Ai,
        tone = MonarchTone.Danger,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchInsightStripPreview() {
    PreviewFrame {
        MonarchInsightStrip(insights = previewInsights, title = "Needs attention", onDismiss = {})
        MonarchInsightStrip(insights = previewInsights, layout = MonarchInsightStripLayout.Stack, limit = 1)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchInsightStripStatesPreview() {
    PreviewFrame {
        MonarchInsightStrip(insights = emptyList(), loading = true)
        MonarchInsightStrip(insights = emptyList(), errorText = "Insights could not be loaded.", onRetry = {})
        MonarchInsightStrip(insights = emptyList())
    }
}

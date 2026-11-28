package dev.monarchui.components.intelligence.aiinsightcallout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiInsightCalloutPreview() {
    PreviewFrame {
        MonarchAiInsightCallout(
            title = "Renewal at risk",
            why = "Usage fell 40% in the last 30 days.",
            origin = MonarchAiInsightOrigin.Ai,
            tone = MonarchTone.Warning,
            actionLabel = "Plan outreach",
            onAction = {},
            onDismiss = {},
        ) { Text("Weekly active users: 120 to 72") }
        MonarchAiInsightCallout(
            title = "Invoice overdue",
            why = "Unpaid for 30 days.",
            origin = MonarchAiInsightOrigin.Rule,
            tone = MonarchTone.Danger,
        )
    }
}

package dev.monarchui.components.intelligence.aiusagemeter

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiUsageMeterPreview() {
    PreviewFrame {
        MonarchAiUsageMeter(
            scopes = listOf(
                MonarchAiUsageScope("This turn", used = 1_850.0),
                MonarchAiUsageScope("Conversation", used = 42_000.0, limit = 50_000.0),
                MonarchAiUsageScope("This month", used = 1_020.0, limit = 1_000.0, unit = MonarchAiUsageUnit.Credits),
            ),
            title = "AI usage",
        )
    }
}

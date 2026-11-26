package com.rookhq.monarch.components.intelligence.aiusagemeter

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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

package com.rookhq.monarch.components.intelligence.aiconversationlist

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme
import java.util.TimeZone

private const val PreviewNow = 1_763_121_600_000L
private const val Day = 86_400_000L

@MonarchPreviews
@Composable
internal fun MonarchAiConversationListPreview() {
    PreviewFrame {
        MonarchAiConversationList(
            conversations = listOf(
                MonarchAiConversationSummary("a", "Quarterly plan", PreviewNow - Day * 40, pinned = true),
                MonarchAiConversationSummary("b", "Trip to Lisbon", PreviewNow - 1_000, preview = "Day two: Belém"),
                MonarchAiConversationSummary("c", "", PreviewNow - Day * 3),
            ),
            onSelect = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 4),
            activeId = "b",
            nowMillis = PreviewNow,
            timeZone = "UTC",
            onNew = {},
            onRename = { _, _ -> },
            onPin = { _, _ -> },
            onDelete = {},
        )
    }
}

package com.rookhq.monarch.components.intelligence.aimemorychip

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiMemoryChipPreview() {
    PreviewFrame {
        MonarchAiMemoryAction.entries.forEach { action ->
            MonarchAiMemoryChip(
                memory = MonarchAiMemoryPart("m-${action.name}", action, "Prefers summaries under 200 words", "user"),
                onEdit = { _, _ -> },
                onForget = {},
            )
        }
        MonarchAiMemoryChip(
            memory = MonarchAiMemoryPart("m-row", MonarchAiMemoryAction.Write, "Works in the Lisbon office"),
            variant = MonarchAiMemoryChipVariant.Row,
            onForget = {},
        )
    }
}

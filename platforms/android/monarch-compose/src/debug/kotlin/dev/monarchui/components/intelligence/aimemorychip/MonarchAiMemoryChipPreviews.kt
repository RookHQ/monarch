package dev.monarchui.components.intelligence.aimemorychip

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryAction
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryPart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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

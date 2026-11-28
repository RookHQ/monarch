package dev.monarchui.components.intelligence.aisuggestedactions

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@OptIn(ExperimentalMonarchApi::class)
@MonarchPreviews
@Composable
internal fun MonarchAiSuggestedActionsPreview() {
    PreviewFrame {
        MonarchAiSuggestedActions(
            actions = listOf(
                MonarchAiSuggestedAction(
                    "a",
                    "Send the renewal reminder",
                    description = "The contract ends in 14 days.",
                ),
                MonarchAiSuggestedAction("b", "Create a follow-up task", risk = MonarchAiToolCallRisk.Medium),
                MonarchAiSuggestedAction("c", "Close 4 stale tickets", risk = MonarchAiToolCallRisk.Destructive),
                MonarchAiSuggestedAction("d", "Log the call", status = MonarchAiSuggestedActionStatus.Done),
                MonarchAiSuggestedAction("e", "Update the forecast", status = MonarchAiSuggestedActionStatus.Failed),
            ),
            onRun = {},
            onDismiss = {},
        )
        MonarchAiSuggestedActions(actions = emptyList(), onRun = {}, origin = MonarchAiSuggestedActionsOrigin.Rule)
    }
}

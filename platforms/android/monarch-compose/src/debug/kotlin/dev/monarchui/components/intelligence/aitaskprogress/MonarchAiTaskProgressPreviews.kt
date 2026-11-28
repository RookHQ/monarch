package dev.monarchui.components.intelligence.aitaskprogress

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_763_100_000_000L
private const val FiveMinutes = 300_000L
private const val Eta = 180_000L

@MonarchPreviews
@Composable
internal fun MonarchAiTaskProgressPreview() {
    PreviewFrame {
        MonarchAiTaskProgress(
            title = "Summarising 40 documents",
            status = MonarchAiTaskStatus.Running,
            startedAt = PreviewNow - FiveMinutes,
            steps = listOf(
                MonarchAiPlanStep("read", "Read the documents", MonarchAiPlanStepState.Done),
                MonarchAiPlanStep("draft", "Draft the summary", MonarchAiPlanStepState.Active),
            ),
            etaMillis = Eta,
            now = PreviewNow,
            onNotifyingChange = {},
            onMinimize = {},
            onCancel = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiTaskProgressEndedPreview() {
    PreviewFrame {
        MonarchAiTaskProgress(
            title = "Export to CSV",
            status = MonarchAiTaskStatus.Failed,
            startedAt = PreviewNow - FiveMinutes,
            progress = 0.4f,
            now = PreviewNow,
        )
        MonarchAiTaskProgress(
            title = "Index the wiki",
            status = MonarchAiTaskStatus.Done,
            startedAt = PreviewNow - FiveMinutes,
            now = PreviewNow,
            onMinimize = {},
        )
    }
}
